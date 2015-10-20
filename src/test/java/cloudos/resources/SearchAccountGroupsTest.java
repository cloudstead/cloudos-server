package cloudos.resources;

import cloudos.dao.AccountGroupDAO;
import cloudos.model.AccountGroup;
import cloudos.model.support.AccountRequest;
import org.apache.commons.lang3.RandomUtils;
import org.cobbzilla.wizard.model.ResultPage;
import org.junit.Before;
import org.junit.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.*;

import static cloudos.resources.ApiConstants.ACCOUNTS_ENDPOINT;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.cobbzilla.util.json.JsonUtil.toJson;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SearchAccountGroupsTest extends SearchTestBase {

    private static final String DOC_TARGET = "Search: Account Groups";
    public static final Comparator<AccountGroup> GROUPS_BY_NAME = new Comparator<AccountGroup>() {
        @Override public int compare(AccountGroup g1, AccountGroup g2) { return g1.getName().compareToIgnoreCase(g2.getName()); }
    };

    private static final int NUM_GROUPS = 21;
    protected static final List<AccountGroup> groups = new ArrayList<>();
    protected static final List<AccountGroup> groupsByName = new ArrayList<>();
    protected static final List<String> groupNames = new ArrayList<>();

    @Before public void seedGroups() throws Exception {

        resetGroups();
        super.seedAccounts();

        final AccountGroupDAO groupDAO = getBean(AccountGroupDAO.class);

        final String groupNameBase = "aaa"+ randomAlphanumeric(10).toLowerCase();
        for (int i=0; i<NUM_GROUPS; i++) {
            final String groupName = groupNameBase + "_" + i;
            final AccountGroup group = (AccountGroup) new AccountGroup().setLdapContext(ldap()).setName(groupName);
            groups.add(groupDAO.create(group));

            for (int j=0; j<NUM_ACCOUNTS/2; j++) {
                try {
                    group.addMember(accounts.get(RandomUtils.nextInt(0, NUM_ACCOUNTS)).getDn());
                } catch (DataIntegrityViolationException dive) {
                    // this can happen if the random roller above tries to add the same account to a group a second time
                    // just ignore it, the group will have one less member than it otherwise would.
                }
            }
        }

        final Set<AccountGroup> sorted = new TreeSet<>(GROUPS_BY_NAME);
        sorted.addAll(groups);
        groupsByName.addAll(sorted);
        for (AccountGroup g : groupsByName) groupNames.add(g.getName());
    }

    public void resetGroups () {

        final AccountGroupDAO groupDAO = getBean(AccountGroupDAO.class);

        for (AccountGroup g : groupDAO.findAll()) {
            if (!AccountGroupDAO.isDefaultGroup(g.getName())) groupDAO.delete(g.getUuid());
        }
        groups.clear();
        groupNames.clear();
        groupsByName.clear();

        super.resetAccounts();
    }

    @Test public void testSearchGroupsByName() throws Exception {
        apiDocs.startRecording(DOC_TARGET, "basic name-sorted default search. should return the first 10 account groups by name (plus the 2 default groups)");

        apiDocs.addNote("Create an admin user to ensure both built-in groups exist");
        final AccountRequest request = newAccountRequest(ldap(), randomAlphanumeric(10), randomAlphanumeric(10), true);
        put(ACCOUNTS_ENDPOINT + "/" + request.getName(), toJson(request));

        final ResultPage page = new ResultPage()
                .setPageNumber(0).setPageSize(10)
                .setSortField("name").setSortOrder(ResultPage.ASC);

        final List<String> expected = new ArrayList<>(groupNames);
        expected.add(AccountGroup.DEFAULT_GROUP_NAME);
        expectResults(page, NUM_GROUPS + 2, expected, searchAccountGroups(page));
    }

    @Test public void testDownloadCsv () throws Exception {
        apiDocs.startRecording(DOC_TARGET, "download a CSV of the results of a search");
        apiDocs.addNote("download all groups as a CSV");
        final String csv = downloadAccountGroups(ResultPage.INFINITE_PAGE);
        assertNotNull(csv);
        for (String name : groupNames) assertTrue(csv.contains(name));
    }

}