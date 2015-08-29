package cloudos.server;

import cloudos.dns.service.mock.MockDnsManager;
import lombok.NoArgsConstructor;
import org.cobbzilla.util.bean.BeanMerger;
import org.cobbzilla.util.dns.DnsManager;

@NoArgsConstructor
public class MockCloudOsConfiguration extends CloudOsConfiguration {

    private DnsManager mockDnsManager = new MockDnsManager();

    public MockCloudOsConfiguration(CloudOsConfiguration configuration) {
        BeanMerger.mergeNotNullProperties(this, configuration);
    }

    @Override public DnsManager getDnsManager() { return mockDnsManager; }

}
