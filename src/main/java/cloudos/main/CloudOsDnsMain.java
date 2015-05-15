package cloudos.main;

import org.cobbzilla.util.dns.DnsRecord;
import org.cobbzilla.util.dns.DnsRecordBase;
import org.cobbzilla.util.dns.DnsRecordMatch;
import org.cobbzilla.wizard.client.ApiClientBase;

import static cloudos.resources.ApiConstants.DNS_ENDPOINT;
import static org.cobbzilla.util.json.JsonUtil.fromJson;
import static org.cobbzilla.util.json.JsonUtil.toJson;

public class CloudOsDnsMain extends CloudOsMainBase<CloudOsDnsMainOptions> {

    public static void main (String[] args) { main(CloudOsDnsMain.class, args); }

    @Override protected void run() throws Exception {
        final CloudOsDnsMainOptions options = getOptions();
        final ApiClientBase api = getApiClient();

        final DnsRecordBase record;
        switch (options.getOperation()) {
            case add:
                if (options.hasSubdomain()) throw new IllegalArgumentException("subdomain option is invalid for 'add' operations");
                record = new DnsRecord()
                        .setTtl(options.getTtl())
                        .setOptions(options.getOptionsMap())
                        .setType(options.getType())
                        .setFqdn(options.getFqdn())
                        .setValue(options.getValue());
                // do not add if there is already an identical record
                boolean newlyAdded = Boolean.parseBoolean(api.post(DNS_ENDPOINT, toJson(record)).json);
                out(newlyAdded ? "Successfully added record: "+record : "Success: record already existed: "+record);
                break;

            case remove:
                record = new DnsRecordMatch()
                        .setSubdomain(options.getSubdomain())
                        .setType(options.getType())
                        .setFqdn(options.getFqdn())
                        .setValue(options.getValue());
                int numDeleted = Integer.parseInt(api.post(DNS_ENDPOINT+"/delete", toJson(record)).json);
                out("Successfully deleted "+numDeleted+" records that matched: "+record);
                break;

            case list:
                final DnsRecord[] found = fromJson(api.get(DNS_ENDPOINT).json, DnsRecord[].class);
                out("Successfully found "+found.length+" records");
                for (DnsRecord rec : found) {
                    out(rec.toString());
                }
                break;
        }
    }
}
