package cloudos.server;

import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.server.RestServerBase;
import org.cobbzilla.wizard.server.config.factory.ConfigurationSource;

import java.util.List;

@Slf4j
public class CloudOsServer extends RestServerBase<CloudOsConfiguration> {

    private static final String[] API_CONFIG_YML = {"cloudos-config.yml"};

    @Override protected String getListenAddress() { return LOCALHOST; }

    // args are ignored, config is loaded from the classpath
    public static void main(String[] args) throws Exception {

        final List<ConfigurationSource> configSources = getStreamConfigurationSources(CloudOsServer.class, API_CONFIG_YML);

        main(CloudOsServer.class, new CloudOsServerListener(), configSources);

    }

}
