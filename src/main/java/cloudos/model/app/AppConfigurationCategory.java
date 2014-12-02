package cloudos.model.app;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Corresponds to a single databag in the app manifest.
 */
@NoArgsConstructor
public class AppConfigurationCategory {

    @Getter @Setter private String name;
    @Getter @Setter private List<String> items = new ArrayList<>();
    @Getter @Setter private Map<String, String> values = new HashMap<>();

    public AppConfigurationCategory(String name) { this.name = name; }

    public void add (String item) { items.add(item); }

    public void set (String item, String value) {
        if (!items.contains(item)) throw new IllegalArgumentException("Invalid config item: "+item);
        values.put(item, value);
    }

    public String get (String item) { return values.get(item); }

}
