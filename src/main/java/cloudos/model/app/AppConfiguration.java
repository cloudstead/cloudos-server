package cloudos.model.app;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the configuration for a given app+version
 */
public class AppConfiguration {

    @Getter @Setter private List<AppConfigurationCategory> categories = new ArrayList<>();

    public AppConfigurationCategory getCategory(String name) {
        for (AppConfigurationCategory cat : categories) {
            if (cat.getName().equals(name)) return cat;
        }
        return null;
    }

    public void add(AppConfigurationCategory category) { categories.add(category); }

}
