package cloudos.service;

import cloudos.appstore.model.AppRuntime;
import cloudos.appstore.model.CloudOsAccount;
import cloudos.appstore.model.app.AppManifest;
import cloudos.dao.AppDAO;
import cloudos.databag.PortsDatabag;
import cloudos.model.support.AppInstallUrlRequest;
import cloudos.server.CloudOsConfiguration;
import cloudos.service.task.TaskBase;
import cloudos.service.task.TaskResult;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.dns.DnsRecord;
import org.cobbzilla.util.dns.DnsType;
import org.cobbzilla.util.http.HttpUtil;
import org.cobbzilla.util.io.FileUtil;
import org.cobbzilla.util.io.Tarball;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.util.system.PortPicker;
import rooty.RootyConfiguration;
import rooty.toots.chef.ChefMessage;
import rooty.toots.chef.ChefOperation;

import java.io.File;

@Accessors(chain=true) @Slf4j
public class AppInstallTask extends TaskBase {

    public static final String PLUGIN_JAR = "plugin.jar";
    public static final String CLOUDOS_MANIFEST_JSON = "cloudos-manifest.json";

    private static final int DEFAULT_TTL = 3600;

    @Getter @Setter private CloudOsAccount account;
    @Getter @Setter private AppInstallUrlRequest request;
    @Getter @Setter private AppDAO appDAO;
    @Getter @Setter private CloudOsConfiguration configuration;

    @Override
    public TaskResult call() {

        final RootyConfiguration rooty = configuration.getRooty();

        // initial description of task (we'll refine this when we know what is being installed)
        description("{appInstall.installingPackage}", request.getUrl());

        // download the tarball to a tempfile
        addEvent("{appInstall.downloadingTarball}");
        final String suffix = request.getUrl().substring(request.getUrl().lastIndexOf('.'));
        final File tarball;
        try {
            tarball = File.createTempFile("app-tarball", suffix);
            HttpUtil.url2file(request.getUrl(), tarball);
        } catch (Exception e) {
            error("{appInstall.error.downloadingTarball", e);
            return null;
        }

        // unroll the tarball to a temp dir
        addEvent("{appInstall.unpackingTarball}");
        final File tempDir;
        try {
            tempDir = Tarball.unroll(tarball);
        } catch (Exception e) {
            error("{appInstall.error.unpackingTarball}", e);
            return null;
        }

        // validate the manifest
        addEvent("{appInstall.readingManifest}");
        final AppManifest manifest;
        try {
            final File manifestFile = new File(tempDir, CLOUDOS_MANIFEST_JSON);
            manifest = JsonUtil.fromJson(FileUtil.toString(manifestFile), AppManifest.class);
        } catch (Exception e) {
            error("{appInstall.error.readingManifest}", e);
            return null;
        }

        // update description, we now know the app name
        final String name = manifest.getName();
        description("{appInstall.installingApp}", name);

        // load the AppRuntime class
        addEvent("{appInstall.loadingPlugin}");
        final File pluginJar = new File(tempDir, PLUGIN_JAR);
        try {
            final Class<? extends AppRuntime> pluginClass = appDAO.loadPluginClass(pluginJar, manifest.getPlugin());
        } catch (Exception e) {
            error("{appInstall.error.loadingPlugin}", e);
            return null;
        }

        // does it want its own hostname?
        // todo: maintain a registry of hostnames to avoid conflicts (or could we just peek at /etc/tinydns/root/data ?)
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

        // collect cookbooks and recipes, build delta command
        addEvent("{appInstall.verifyingChefCookbooks}");
        final ChefMessage chefMessage = new ChefMessage(ChefOperation.ADD);
        for (String recipe : manifest.getChefInstallRunlist()) {
            chefMessage.addRecipe(recipe.trim());
        }

        // pick a port to listen on and write data bag
        final PortsDatabag ports = new PortsDatabag()
                .setPrimary(PortPicker.pickOrDie())
                .setAdmin(PortPicker.pickOrDie());
        final File chefDir = new File(tempDir, "chef");
        final File databagDir = new File(chefDir.getAbsolutePath() + "/data_bags/" + name);
        if (!databagDir.mkdirs()) {
            error("{appInstall.error.creatingDatabagDir}", new IllegalStateException("error creating "+databagDir.getAbsolutePath()));
            return null;
        }
        try {
            FileUtil.toFile(new File(databagDir, "ports.json"), JsonUtil.toJson(ports));
        } catch (Exception e) {
            error("{appInstall.error.writingPortsDataBag}", e);
            return null;
        }

        // notify the chef-user that we have some new recipes to add to the run list
        addEvent("{appInstall.notifyingChefToRun}");
        try {
            configuration.getChefHandler().write(chefMessage, rooty.getSecret(), chefDir);
        } catch (Exception e) {
            error("{appInstall.error.notifyingChefToRun}", e);
            return null;
        }

        // todo: check for messages*.properties and populate localized string tables for app

        // todo: find a way to monitor progress of chef installation

        addEvent("{appInstall.recordingInstallation}");
        try {
            appDAO.install(account, manifest, pluginJar, tarball, ports.getPrimary());
        } catch (Exception e) {
            error("{appInstall.error.recordingInstallation}", e);
            return null;
        }

        result.setSuccess(true);
        return result;
    }

    protected void createDnsRecord(DnsType type, String fqdn, String value) throws Exception {
        configuration.getDnsManager().write((DnsRecord) new DnsRecord()
                .setTtl(DEFAULT_TTL)
                .setType(type)
                .setFqdn(fqdn)
                .setValue(value));
    }

}
