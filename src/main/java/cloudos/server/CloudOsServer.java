package cloudos.server;

import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.system.CommandShell;
import org.cobbzilla.wizard.server.RestServerBase;
import org.cobbzilla.wizard.server.config.factory.ConfigurationSource;

import java.util.List;

@Slf4j
public class CloudOsServer extends RestServerBase<CloudOsConfiguration> {

    public static final String[] API_CONFIG_YML = {"cloudos-config.yml"};

    @Override protected String getListenAddress() { return LOCALHOST; }

    // args are ignored, config is loaded from the classpath
    public static void main(String[] args) throws Exception {

        log.info("starting with ulimits: \n" + CommandShell.execScript("ulimit -a"));

        final List<ConfigurationSource> configSources = getConfigurationSources();
        main(CloudOsServer.class, configSources);
    }

    public static List<ConfigurationSource> getConfigurationSources() {
        return getStreamConfigurationSources(CloudOsServer.class, API_CONFIG_YML);
    }
}
