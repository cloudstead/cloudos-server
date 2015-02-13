package cloudos.model.app;

import cloudos.appstore.model.app.AppManifest;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

public class AppRepositoryState {

    @Getter @Setter private SortedSet<String> appNames = new TreeSet<>();
    @Getter @Setter private Map<String, List<CloudOsAppVersion>> appVersions = new HashMap<>();

    public void addApp (AppManifest manifest, boolean active) {
        final String appName = manifest.getName();
        final String version = manifest.getVersion();
        final String parent = manifest.getParent();

        appNames.add(appName);
        List<CloudOsAppVersion> versions = appVersions.get(appName);
        if (versions == null) {
            versions = new ArrayList<>();
            appVersions.put(appName, versions);
        }
        versions.add(new CloudOsAppVersion(appName, version, parent, active));
    }

}
