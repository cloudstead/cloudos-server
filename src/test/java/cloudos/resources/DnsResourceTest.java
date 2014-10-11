package cloudos.resources;

import cloudos.dns.service.mock.MockDnsManager;
import org.cobbzilla.util.dns.DnsRecord;
import org.cobbzilla.util.dns.DnsRecordBase;
import org.cobbzilla.util.dns.DnsRecordMatch;
import org.cobbzilla.util.dns.DnsType;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static cloudos.resources.ApiConstants.DNS_ENDPOINT;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.cobbzilla.util.json.JsonUtil.fromJson;
import static org.cobbzilla.util.json.JsonUtil.toJson;
import static org.junit.Assert.*;

public class DnsResourceTest extends ApiClientTestBase {

    private static final String DOC_TARGET = "DNS Management";

    @Before public void initDns () throws Exception {

        final MockDnsManager dnsManager = getDnsManager();
        dnsManager.reset();

        final String host = getConfiguration().getHostname();
        DnsRecord record;

        record = (DnsRecord) new DnsRecord().setType(DnsType.A).setFqdn(host).setValue("127.0.0.1");
        dnsManager.write(record);

        record = (DnsRecord) new DnsRecord().setType(DnsType.A).setFqdn("mx."+host).setValue("127.0.0.1");
        dnsManager.write(record);

        record = (DnsRecord) new DnsRecord().setType(DnsType.MX).setFqdn(host).setValue("mx."+host);
        dnsManager.write(record);
    }

    @Test public void dnsRecordCrud () throws Exception {

        apiDocs.startRecording(DOC_TARGET, "List, create and remove DNS records");
        final String host = getConfiguration().getHostname();
        Set<DnsRecord> recordSet;

        apiDocs.addNote("List all DNS records for our subdomain");
        recordSet = toSet(fromJson(get(DNS_ENDPOINT).json, DnsRecord[].class));
        assertEquals(3, recordSet.size());

        apiDocs.addNote("Add a few random TXT records to the subdomain");
        final int numTxtRecords = 5;
        final DnsRecordBase[] txtRecords = new DnsRecordBase[numTxtRecords];
        for (int i=0; i<numTxtRecords; i++) {
            txtRecords[i] = new DnsRecord()
                    .setType(DnsType.TXT)
                    .setFqdn(host)
                    .setValue(randomAlphanumeric(10));
            assertTrue("TXT record was not created", Boolean.valueOf(post(DNS_ENDPOINT, toJson(txtRecords[i])).json));
        }

        apiDocs.addNote("Try to add the same record twice. API call should succeed but return false, indicating record was not added");
        assertFalse("TXT record was created twice", Boolean.valueOf(post(DNS_ENDPOINT, toJson(txtRecords[0])).json));

        apiDocs.addNote("List all DNS records for subdomain, verify new records were added");
        recordSet = toSet(fromJson(get(DNS_ENDPOINT).json, DnsRecord[].class));
        assertEquals(3 + numTxtRecords, recordSet.size());
        for (int i=0; i<numTxtRecords; i++) {
            assertTrue("Didn't find TXT record", recordSet.contains(txtRecords[i]));
        }

        DnsRecordMatch match;

        apiDocs.addNote("Remove one TXT record");
        match = new DnsRecordMatch(txtRecords[0]);
        assertEquals(1, deleteFromDns(match));

        apiDocs.addNote("List all DNS records for subdomain, verify one TXT record was removed and all other records remain");
        recordSet = toSet(fromJson(get(DNS_ENDPOINT).json, DnsRecord[].class));
        assertEquals(3 + numTxtRecords - 1, recordSet.size());
        assertFalse("Expected TXT record to be deleted", recordSet.contains(txtRecords[0]));

        apiDocs.addNote("Remove all TXT records");
        match = (DnsRecordMatch) new DnsRecordMatch().setType(DnsType.TXT);
        assertEquals(numTxtRecords - 1, deleteFromDns(match));

        apiDocs.addNote("List all DNS records for subdomain, verify all TXT records were removed");
        recordSet = toSet(fromJson(get(DNS_ENDPOINT).json, DnsRecord[].class));
        for (int i=0; i<numTxtRecords; i++) {
            assertFalse("Expected TXT record to be deleted", recordSet.contains(txtRecords[i]));
        }
        assertEquals(3, recordSet.size());

        apiDocs.addNote("Remove all records for subdomain");
        assertEquals(getDnsManager().size(), deleteFromDns(new DnsRecordMatch()));

        apiDocs.addNote("List all DNS records for subdomain, verify there are none");
        recordSet = toSet(fromJson(get(DNS_ENDPOINT).json, DnsRecord[].class));
        assertTrue("Expected all records to be deleted", recordSet.isEmpty());
    }

    private HashSet<DnsRecord> toSet(DnsRecord[] records) { return new HashSet<>(Arrays.asList(records)); }

    private int deleteFromDns(DnsRecordMatch match) throws Exception {
        return fromJson(post(DNS_ENDPOINT+DnsRecordsResource.EP_DELETE, toJson(match)).json, Integer.class);
    }

}
