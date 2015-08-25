package cloudos.service;

import cloudos.appstore.model.CloudOsAccount;
import cloudos.appstore.model.app.AppConfigDef;
import cloudos.appstore.model.app.AppLayout;
import cloudos.appstore.model.app.AppManifest;
import cloudos.appstore.model.app.AppMetadata;
import cloudos.appstore.model.app.config.AppConfiguration;
import cloudos.appstore.model.support.AppListing;
import cloudos.dao.AppDAO;
import cloudos.dao.SessionDAO;
import cloudos.databag.PortsDatabag;
import cloudos.model.support.AppDownloadRequest;
import cloudos.model.support.AppInstallRequest;
import cloudos.server.CloudOsConfiguration;
import cloudos.service.task.CloudOsTaskResult;
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
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.cobbzilla.util.io.FileUtil.abs;

/**
 * Installs an application from your cloudstead's app library onto your cloudos.
 */
@Accessors(chain=true) @Slf4j
public class AppInstallTask extends CloudOsTaskBase {

    private static final int DEFAULT_TTL = 3600;

    private static final long INSTALL_TIMEOUT = TimeUnit.MINUTES.toMillis(20);

    @Getter @Setter private RootyService rootyService;
    @Getter @Setter private CloudOsAccount account;
    @Getter @Setter private AppInstallRequest request;
    @Getter @Setter private AppDAO appDAO;
    @Getter @Setter private SessionDAO sessionDAO;
    @Getter @Setter private CloudOsAppConfigValidationResolver resolver;
    @Getter @Setter private CloudOsConfiguration configuration;

    @Override public CloudOsTaskResult execute() {

        description("{appInstall.installingApp}", request.toString());

        // Find the app version to install
        AppLayout appLayout = null;
        if (request.hasVersion()) {
            appLayout = configuration.getAppLayout(request.getName(), request.getVersion());
        }
        if (appLayout == null || !appLayout.exists()) {
            // is it a public app?
            try {
                final AppListing listing;
                if (request.hasVersion()) {
                    listing = configuration.getAppStoreClient().findAppListing(request.getPublisher(), request.getName(), request.getVersion());
                } else {
                    // get latest published version
                    listing = configuration.getAppStoreClient().findAppListing(request.getPublisher(), request.getName());
                }
                if (listing == null) {
                    error("{appInstall.versionNotFound}", "not a directory and no public version found in app store");
                    return null;
                }

                // download it but turn off auto-install... we're done
                final CloudOsTaskResult downloadResult = new AppDownloadTask()
                        .setResolver(getResolver())
                        .setAccount(getAccount())
                        .setAppDAO(getAppDAO())
                        .setConfiguration(getConfiguration())
                        .setRootyService(getRootyService())
                        .setRequest(new AppDownloadRequest()
                                .setAutoInstall(false)
                                .setOverwrite(request.isForce())
                                .setUrl(listing.getBundleUrl())
                                .setSha(listing.getBundleUrlSha()))
                        .setTaskId(getTaskId())
                        .call();

                appLayout = configuration.getAppLayout(request.getName(), listing.getVersion());
                if (downloadResult == null || !downloadResult.isSuccess() || !downloadResult.isComplete() || appLayout == null || !appLayout.exists()) {
                    final String msg = "error installing from app store";
                    error("{appInstall.error.installFromAppStoreFailed}", msg);
                    log.error(msg+": "+downloadResult);
                    return null;
                }
            } catch (Exception e) {
                final String msg = "error installing from app store";
                error("{appInstall.error.installFromAppStoreFailed}", msg);
                log.error(msg+": "+e, e);
                return null;
            }
        }

        // Validate databags. If any violations are related to missing passwords, pick a random password
        // and email it to the caller.
        final AppManifest manifest = AppManifest.load(appLayout.getVersionDir());
        final AppConfiguration appConfig = AppConfiguration.readAppConfiguration(manifest, appLayout.getDatabagsDir(), configuration.getSystemLocale());

        final AppLayout currentLayout = configuration.getAppLayout(request.getName());
        final File currentVersionDir = currentLayout.getAppActiveVersionDir();
        if (currentVersionDir != null && currentVersionDir.exists()) {
            // copy over settings from previous installation
            final AppConfiguration currentConfig = AppConfiguration.readAppConfiguration(AppManifest.load(currentVersionDir), currentLayout.getDatabagsDir(), configuration.getSystemLocale());
            if (currentConfig != null) {
                appConfig.merge(currentConfig);
                appConfig.writeAppConfiguration(manifest, appLayout.getDatabagDirForApp());
            }
        }

        final List<ConstraintViolationBean> validationErrors = appConfig.validate(resolver);

        if (!validationErrors.isEmpty()) {
            error("appInstall.err.validation", validationErrors);
            return null;
        }

        // write manifest file to data_bags/app-name directory
        try {
            FileUtil.toFile(appLayout.getDatabagFile(AppManifest.CLOUDOS_MANIFEST), JsonUtil.toJson(manifest));
        } catch (Exception e) {
            error("{appInstall.err.manifestDatabag}", "Error creating "+AppManifest.CLOUDOS_MANIFEST_JSON+" databag");
            return null;
        }

        // does it want its own hostname?
        if (manifest.hasHostname()) {
            addEvent("{appInstall.creatingHostname}");
            try {
                final String hostname = manifest.getHostname();
                if (hostname != null && !hostname.equals(AppManifest.ROOT_HOSTNAME)) {
                    createDnsRecord(DnsType.A, hostname + "-" + configuration.getHostname(), configuration.getPublicIp());
                    createDnsRecord(DnsType.A, hostname + "." + configuration.getHostname(), configuration.getPublicIp());
                }

            } catch (Exception e) {
                error("{appInstall.error.creatingHostname}", e);
                return null;
            }
        }

        // collect cookbooks and recipes, build chef
        addEvent("{appInstall.verifyingChefCookbooks}");
        final ChefMessage chefMessage = new ChefMessage(ChefOperation.ADD).setForceApply(request.isForce());
        final File chefDir = appLayout.getChefDir();
        if (!chefDir.exists()) {
            error("{appInstall.error.chefDir.notFound", "chefDir not found: "+abs(chefDir));
            return null;
        }
        chefMessage.setChefDir(abs(chefDir));
        chefMessage.setCookbook(manifest.getName());

        // generate a new ports databag every time
        final PortsDatabag ports = PortsDatabag.pick();

        try {
            FileUtil.toFile(appLayout.getDatabagFile(PortsDatabag.ID), JsonUtil.toJson(ports));
        } catch (Exception e) {
            error("{appInstall.error.writingPortsDataBag}", e);
            return null;
        }

        // ensure app-repository remains readable to rooty group
        try {
            if (configuration.getRootyGroup() != null) {
                CommandShell.chgrp(configuration.getRootyGroup(), configuration.getAppRepository(), true);
            }
            CommandShell.chmod(configuration.getAppRepository(), "g+r", true);
        } catch (Exception e) {
            error("{appInstall.error.perms}", "Error setting ownership/permissions on "+abs(configuration.getAppRepository())+": "+e);
            return null;
        }

        // notify the chef-user that we have some new recipes to add to the run list
        addEvent("{appInstall.runningChef}");
        final RootyMessage status;
        try {
            result.setRootyUuid(chefMessage.initUuid());
            status = rootyService.request(chefMessage, INSTALL_TIMEOUT);
        } catch (Exception e) {
            error("{appInstall.error.notifyingChefToRun}", e);
            return result;
        }

        if (status.isSuccess() && !status.hasError()) {
            addEvent("{appInstall.recordingInstallation}");
            final AppMetadata metadata = new AppMetadata()
                    .setInstalled_by(account.getName())
                    .setActive_version(request.getVersion())
                    .setInteractive(manifest.isInteractive());
            metadata.write(appLayout.getAppDir());
            appDAO.resetApps();
            result.setSuccess(true);

        } else {
            result.setError(status.getLastError());
        }
        return result;
    }

    private ConstraintViolationBean missingConfig(AppConfigDef databag, String item) {
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
