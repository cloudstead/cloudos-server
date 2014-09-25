package cloudos.resources;

import cloudos.model.Account;
import cloudos.model.AccountGroup;
import cloudos.model.AccountGroupMember;
import org.cobbzilla.wizard.model.SearchScrubber;

import java.util.List;

public class DefaultSearchScrubber implements SearchScrubber {

    public static final DefaultSearchScrubber DEFAULT_SEARCH_SCRUBBER = new DefaultSearchScrubber();

    @Override
    public List scrub(List results) {
        if (results == null) return null;
        for (Object o : results) {
            if (o instanceof Account) {
                scrubAccount((Account) o);

            } else if (o instanceof AccountGroup) {
                scrubGroup((AccountGroup) o);
            }
        }
        return results;
    }

    private void scrubAccount(Account a) {
        a.setUuid(null);
        a.setAuthId(null);
        a.setLastLogin(null);
        a.setPassword(null);
        a.setRecoveryEmail(null);
        a.setTwoFactor(false);
        a.setAdmin(false);
    }

    private void scrubGroup(AccountGroup group) {
        group.setUuid(null);
        if (group.hasMembers()) {
            for (AccountGroupMember m : group.getMembers()) {
                m.setUuid(null);
                m.setMemberUuid(null);
                m.setGroupUuid(null);
            }
        }
    }

}
