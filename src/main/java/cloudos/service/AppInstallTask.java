package cloudos.service;

import cloudos.appstore.model.CloudOsAccount;
import cloudos.appstore.model.app.AppDatabagDef;
import cloudos.appstore.model.app.AppManifest;
import cloudos.dao.AppDAO;
import cloudos.databag.PortsDatabag;
import cloudos.model.app.AppMetadata;
import cloudos.model.app.CloudOsAppLayout;
import cloudos.model.support.AppInstallRequest;
import cloudos.server.CloudOsConfiguration;
import cloudos.service.task.TaskBase;
import cloudos.service.task.TaskResult;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.dns.DnsRecord;
import org.cobbzilla.util.dns.DnsType;
import org.cobbzilla.util.io.FileUtil;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.util.system.CommandShell;
import org.cobbzilla.wizard.validation.ConstraintViolationBean;
import rooty.RootyMessage;
import rooty.toots.chef.ChefMessage;
import rooty.toots.chef.ChefOperation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Installs an application from your cloudstead's app library onto your cloudos.
 */
@Accessors(chain=true) @Slf4j
public class AppInstallTask extends TaskBase {

    private static final int DEFAULT_TTL = 3600;

    private static final long INSTALL_TIMEOUT = TimeUnit.MINUTES.toMillis(5);

    @Getter @Setter private RootyService rootyService;
    @Getter @Setter private CloudOsAccount account;
    @Getter @Setter private AppInstallRequest request;
    @Getter @Setter private AppDAO appDAO;
    @Getter @Setter private CloudOsConfiguration configuration;

    @Override
    public TaskResult call() {

        description("{appInstall.installingApp}", request.toString());

        // Find the app version to install
        final CloudOsAppLayout appLayout = configuration.getAppLayout();
        final File appDir = appLayout.getAppDir(request.getName());
        final File appVersionDir = appLayout.getAppVersionDir(request.getName(), request.getVersion());

        if (!appVersionDir.exists() || !appVersionDir.isDirectory()) {
            error("{appInstall.versionNotFound}", "not a directory: "+appVersionDir.getAbsolutePath());
            return null;
        }

        final AppManifest manifest = AppManifest.load(appVersionDir);
        final String name = manifest.getId();
        final List<ConstraintViolationBean> validationErrors = new ArrayList<>();

        // do we have all the required configuration?
        if (manifest.hasDatabags()) {
            for (AppDatabagDef databag : manifest.getDatabags()) {
                final JsonNode node = appLayout.getDatabag(appVersionDir, databag.getName());
                for (String item : databag.getItems()) {
                    if (node == null) {
                        validationErrors.add(missingConfig(databag, item));
                    } else {
                        try {
                            // validate that all fields are present and have values.
                            if (JsonUtil.toString(JsonUtil.findNode(node, item)) == null) {
                                validationErrors.add(missingConfig(databag, item));
                            }
                        } catch (Exception e) {
                            validationErrors.add(missingConfig(databag, item));
                        }
                    }
                }
            }
        }
        if (!validationErrors.isEmpty()) {
            error("err.validation", validationErrors);
            return null;
        }

        // write manifest file to data_bags/app-name directory
        try {
            FileUtil.toFile(appLayout.getDatabagFile(appVersionDir, "cloudos-manifest"), JsonUtil.toJson(manifest));
        } catch (Exception e) {
            error("err.manifestDatabag", "Error creating cloudos-manifest.json databag");
            return null;
        }

        // does it want its own hostname?
        if (manifest.hasHostname()) {
            addEvent("{appInstall.creatingHostname}");
            try {
                createDnsRecord(DnsType.A, manifest.getHostname() + "-" + configuration.getHostname(), configuration.getPublicIp());
                createDnsRecord(DnsType.A, manifest.getHostname() + "." + configuration.getHostname(), configuration.getPublicIp());

            } catch (Exception e) {
                error("{appInstall.error.creatingHostname}", e);
                return null;
            }
        }

        // collect cookbooks and recipes, build chef
        addEvent("{appInstall.verifyingChefCookbooks}");
        final ChefMessage chefMessage = new ChefMessage(ChefOperation.ADD).setForceApply(request.isForce());
        final File chefDir = appLayout.getChefDir(appVersionDir);
        if (!chefDir.exists()) {
            error("{appInstall.error.chefDir.notFound", "chefDir not found: "+chefDir.getAbsolutePath());
            return null;
        }
        chefMessage.setChefDir(chefDir.getAbsolutePath());

        for (String recipe : manifest.getChefInstallRunlist()) {
            chefMessage.addRecipe(recipe.trim());
        }

        // generate a new ports databag every time
        final PortsDatabag ports = PortsDatabag.pick();

        try {
            FileUtil.toFile(appLayout.getDatabagFile(appVersionDir, PortsDatabag.ID), JsonUtil.toJson(ports));
        } catch (Exception e) {
            error("{appInstall.error.writingPortsDataBag}", e);
            return null;
        }

        // ensure app-repository remains readable to rooty group
        try {
            if (configuration.getRootyGroup() != null) {
                CommandShell.chgrp(configuration.getRootyGroup(), configuration.getAppRepository(), true);
            }
            CommandShell.chmod(configuration.getAppRepository(), "750", true);
        } catch (Exception e) {
            error("{appInstall.error.perms", "Error setting ownership/permissions on "+configuration.getAppRepository().getAbsolutePath()+": "+e);
            return null;
        }

        // notify the chef-user that we have some new recipes to add to the run list
        addEvent("{appInstall.notifyingChefToRun}");
        final RootyMessage status;
        try {
            result.setRootyUuid(chefMessage.initUuid());
            status = rootyService.request(chefMessage, INSTALL_TIMEOUT);
        } catch (Exception e) {
            error("{appInstall.error.notifyingChefToRun}", e);
            return null;
        }

        if (status.isSuccess()) {
            addEvent("{appInstall.recordingInstallation}");
            final AppMetadata metadata = new AppMetadata()
                    .setInstalled_by(account.getName())
                    .setActive_version(request.getVersion())
                    .setInteractive(manifest.isInteractive());
            metadata.write(appDir);
            appDAO.resetApps();
            result.setSuccess(true);

        } else {
            result.setError(status.getLastError());
        }
        return result;
    }

    private ConstraintViolationBean missingConfig(AppDatabagDef databag, String item) {
        return new ConstraintViolationBean("err."+databag.getName()+"."+item+".empty");
    }

    protected void createDnsRecord(DnsType type, String fqdn, String value) throws Exception {
        configuration.getDnsManager().write((DnsRecord) new DnsRecord()
                .setTtl(DEFAULT_TTL)
                .setType(type)
                .setFqdn(fqdn)
                .setValue(value));
    }

}
