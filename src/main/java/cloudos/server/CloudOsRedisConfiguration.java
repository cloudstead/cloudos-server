package cloudos.server;

import lombok.AllArgsConstructor;
import org.cobbzilla.wizard.cache.redis.RedisConfiguration;

@AllArgsConstructor
public class CloudOsRedisConfiguration extends RedisConfiguration {

    private final CloudOsConfiguration configuration;

    @Override public String getKey() { return configuration.getCloudConfig().getDataKey(); }

}
