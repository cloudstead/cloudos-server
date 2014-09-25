package cloudos.resources;

import org.cobbzilla.util.dns.DnsManager;
import org.cobbzilla.util.dns.DnsRecord;
import org.cobbzilla.util.dns.DnsRecordMatch;

import java.util.*;

public class MockDnsManager implements DnsManager {

    private Set<DnsRecord> database = new HashSet<>();

    public void reset () { database.clear(); }

    public int size () { return database.size(); }

    @Override public List<DnsRecord> list(DnsRecordMatch match) throws Exception {
        final List<DnsRecord> matches = new ArrayList<>();
        for (DnsRecord r : database) {
            if (r.match(match)) matches.add(r);
        }
        return matches;
    }

    @Override public boolean write(DnsRecord record) throws Exception {
        return database.add(record);
    }

    @Override public void publish() throws Exception {}

    @Override public int remove(DnsRecordMatch match) throws Exception {
        int count = 0;
        for (Iterator<DnsRecord> iter = database.iterator(); iter.hasNext(); ) {
            if (iter.next().match(match)) {
                iter.remove();
                count++;
            }
        }
        return count;
    }

}
