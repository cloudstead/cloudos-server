package cloudos.model.app;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @JsonIgnore
    public Map<String, Object> getDatabagMap() {
        final Map<String, Object> databags = new HashMap<>();
        for (AppConfigurationCategory cat : getCategories()) {
            final Map<String, String> values = new HashMap<>();
            if (cat.hasValues()) {
                final Map<String, String> databagValues = cat.getValues();
                for (String key : databagValues.keySet()) {
                    final String value = databagValues.get(key);
                    if (key.contains(".")) values.put(key.replace(".", "_"), value);
                    values.put(key, value);
                }
            }
            databags.put(cat.getName(), values);
        }
        return databags;
    }

}
