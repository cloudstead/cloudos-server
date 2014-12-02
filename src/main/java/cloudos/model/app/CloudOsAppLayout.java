package cloudos.model.app;

import cloudos.appstore.model.app.AppManifest;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.util.io.FileUtil;
import org.cobbzilla.util.json.JsonUtil;

import java.io.File;

@AllArgsConstructor
public class CloudOsAppLayout {

    public static final String BUNDLE_TARBALL = "bundle.tar.gz";
    public static final String CHEF_DIR = "chef";
    public static final String DATABAGS_DIR = "data_bags";
    public static final String ASSET_PLUGIN_JAR = "plugin.jar";

    @Getter @Setter private File appRepository;

    public File getAppDir (String name) {
        return new File(appRepository, AppManifest.scrubDirname(name));
    }

    public File getAppActiveVersionDir (String name) {
        final File appDir = getAppDir(name);
        final AppMetadata metadata = AppMetadata.fromJson(appDir);
        if (metadata.isActive()) {
            final File versionDir = new File(appDir, AppManifest.scrubDirname(metadata.getActive_version()));
            return versionDir.exists() && versionDir.isDirectory() ? versionDir : null;
        }
        return null;
    }

    public File getAppActiveVersionDir(AppManifest manifest) {
        return getAppVersionDir(manifest.getName(), manifest.getVersion());
    }

    public File getAppVersionDir(String name, String version) {
        final File appDir = getAppDir(name);
        return new File(appDir, AppManifest.scrubDirname(version));
    }

    public File getBundleFile (File appVersionDir) { return new File(appVersionDir, CloudOsAppLayout.BUNDLE_TARBALL); }
    public File getChefDir    (File appVersionDir) { return new File(appVersionDir, CloudOsAppLayout.CHEF_DIR); }
    public File getManifest   (File appVersionDir) { return new File(appVersionDir, AppManifest.CLOUDOS_MANIFEST_JSON);  }
    public File getPluginJar  (File appVersionDir) { return new File(appVersionDir, CloudOsAppLayout.ASSET_PLUGIN_JAR); }

    public File getDatabagsDir(File appVersionDir) { return new File(getChefDir(appVersionDir), CloudOsAppLayout.DATABAGS_DIR); }

    public File getDatabagFile(File appVersionDir, String databagName) {
        return new File(new File(getDatabagsDir(appVersionDir), appVersionDir.getParentFile().getName()), databagName+".json");
    }

    public JsonNode getDatabag(File appVersionDir, String databagName) {
        final File databagFile = getDatabagFile(appVersionDir, databagName);
        if (!databagFile.exists()) return null;
        return JsonUtil.fromJsonOrDie(FileUtil.toStringOrDie(databagFile), JsonNode.class);
    }
}
