package cloudos.main;

import cloudos.dns.main.DnsOperation;
import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.util.daemon.ZillaRuntime;
import org.cobbzilla.util.dns.DnsType;
import org.kohsuke.args4j.Option;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

public class CloudOsDnsMainOptions extends CloudOsMainOptions {

    public static final String USAGE_OPERATION = "The operation to perform. Can be add, remove or list. Default is list.";
    public static final String OPT_OPERATION = "-p";
    public static final String LONGOPT_OPERATION = "--operation";
    @Option(name=OPT_OPERATION, aliases=LONGOPT_OPERATION, usage=USAGE_OPERATION, required=false)
    @Getter @Setter private DnsOperation operation = DnsOperation.list;

    public static final String USAGE_TYPE = "The type of DNS record. Required for 'add' and 'remove' operations.";
    public static final String OPT_TYPE = "-r";
    public static final String LONGOPT_TYPE = "--record";
    @Option(name=OPT_TYPE, aliases=LONGOPT_TYPE, usage=USAGE_TYPE, required=false)
    @Getter @Setter private DnsType type;

    public static final String USAGE_FQDN = "The FQDN of the DNS record. Required for 'add' and 'remove' operations.";
    public static final String OPT_FQDN = "-f";
    public static final String LONGOPT_FQDN = "--fqdn";
    @Option(name=OPT_FQDN, aliases=LONGOPT_FQDN, usage=USAGE_FQDN, required=false)
    @Getter @Setter private String fqdn;

    public static final String USAGE_SUBDOMAIN = "The subdomain to limit records to.";
    public static final String OPT_SUBDOMAIN = "-S";
    public static final String LONGOPT_SUBDOMAIN = "--subdomain";
    @Option(name=OPT_SUBDOMAIN, aliases=LONGOPT_SUBDOMAIN, usage=USAGE_SUBDOMAIN, required=false)
    @Getter @Setter private String subdomain;
    public boolean hasSubdomain() { return !empty(subdomain); }

    public static final String USAGE_VALUE = "The value of the DNS record. Required for 'add' operations.";
    public static final String OPT_VALUE = "-v";
    public static final String LONGOPT_VALUE = "--value";
    @Option(name=OPT_VALUE, aliases=LONGOPT_VALUE, usage=USAGE_VALUE, required=false)
    @Getter @Setter private String value;

    public static final String USAGE_TTL = "The TTL of the DNS record, in seconds. Default is 1 day (86400).";
    public static final String OPT_TTL = "-t";
    public static final String LONGOPT_TTL = "--ttl";
    @Option(name=OPT_TTL, aliases=LONGOPT_TTL, usage=USAGE_TTL, required=false)
    @Getter @Setter private int ttl = (int) TimeUnit.DAYS.toSeconds(1);

    public static final String USAGE_OPTIONS = "Options for the DNS record. Optional. Must be comma-separated list of key=value. Enclose with quotes if using spaces.";
    public static final String OPT_OPTIONS = "-o";
    public static final String LONGOPT_OPTIONS = "--options";
    @Option(name=OPT_OPTIONS, aliases=LONGOPT_OPTIONS, usage=USAGE_OPTIONS, required=false)
    @Getter @Setter private String options;

    public Map<String, String> getOptionsMap() {
        if (options == null || options.isEmpty()) return Collections.emptyMap();

        final Map<String, String> map = new HashMap<>();
        for (String kvPair : options.split(",")) {
            int eqPos = kvPair.indexOf("=");
            if (eqPos == kvPair.length()) throw new IllegalArgumentException("Option cannot end in '=' character");
            if (eqPos == -1) {
                map.put(kvPair.trim(), "true");
            } else {
                map.put(kvPair.substring(0, eqPos).trim(), kvPair.substring(eqPos+1).trim());
            }
        }

        return map;
    }
}
