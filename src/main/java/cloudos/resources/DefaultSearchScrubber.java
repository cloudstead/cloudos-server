package cloudos.resources;

import cloudos.model.Account;
import cloudos.model.AccountGroup;
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
//                scrubGroup((AccountGroup) o);
            }
        }
        return results;
    }

    private void scrubAccount(Account a) {
        a.remove(a.ldap().getExternal_id());
        a.remove(a.ldap().getUser_twoFactorAuthId());
        a.remove(a.ldap().getUser_lastLogin());
        a.remove(a.ldap().getUser_password());
        a.remove(a.ldap().getUser_email());
        a.remove(a.ldap().getUser_twoFactor());
        a.remove(a.ldap().getUser_admin());
    }

//    private void scrubGroup(AccountGroup group) {
//        group.setUuid(null);
//        if (group.hasMembers()) {
//            for (AccountGroupMember m : group.getMembers()) {
//                m.setD(null);
//                m.setMemberUuid(null);
//                m.setGroupUuid(null);
//            }
//        }
//    }

}
