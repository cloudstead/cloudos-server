package cloudos.model.app;

import cloudos.appstore.model.app.AppManifest;
import cloudos.appstore.model.app.AppMetadata;
import cloudos.databag.PortsDatabag;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.json.JsonUtil;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j @Accessors(chain=true)
public class CloudOsApp {

    @Getter @Setter private File appRepository;
    @Getter @Setter private String name;
    @Getter @Setter private AppMetadata metadata;
    @Getter @Setter private AppManifest manifest;
    @Getter @Setter private Map<String, JsonNode> databags = new HashMap<>();

    public void addDatabag(String name, JsonNode databag) { databags.put(name, databag); }
    public <T> T getDatabag(String name) {
        final JsonNode node = databags.get(name);
        if (node == null) return null;
        try {
            return JsonUtil.FULL_MAPPER.reader().readValue(node);
        } catch (IOException e) {
            throw new IllegalStateException("Error reading databag: "+e, e);
        }
    }

    public String getLocalBaseUri() {
        final AppManifest m = getManifest();
        if (!m.hasWeb()) return null;

        try {
            final PortsDatabag ports = getDatabag(PortsDatabag.ID);
            if (ports == null) return null;
            return "http://127.0.0.1:"+ ports.getPrimary();

        } catch (Exception e) {
            log.warn("error loading ports databag: "+e);
            return null;
        }
    }

}
