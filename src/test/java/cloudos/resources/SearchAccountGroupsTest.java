package cloudos.resources;

import cloudos.dao.AccountGroupDAO;
import cloudos.dao.AccountGroupMemberDAO;
import cloudos.model.AccountGroup;
import cloudos.model.AccountGroupMember;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.cobbzilla.wizard.model.ResultPage;
import org.junit.Before;
import org.junit.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.*;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SearchAccountGroupsTest extends SearchAccountsTest {

    private static final String DOC_TARGET = "Search: Account Groups";
    public static final Comparator<AccountGroup> SORT_BY_NAME = new Comparator<AccountGroup>() {
        @Override public int compare(AccountGroup g1, AccountGroup g2) { return g1.getName().compareToIgnoreCase(g2.getName()); }
    };

    private static final int NUM_GROUPS = 21;
    protected static final List<AccountGroup> groups = new ArrayList<>();
    protected static final List<AccountGroup> groupsByName = new ArrayList<>();
    protected static final List<String> groupNames = new ArrayList<>();

    @Before public void seed() throws Exception {

        super.seed();

        final AccountGroupMemberDAO memberDAO = getBean(AccountGroupMemberDAO.class);
        final AccountGroupDAO groupDAO = getBean(AccountGroupDAO.class);

        final String groupNameBase = RandomStringUtils.randomAlphanumeric(10).toLowerCase();
        for (int i=0; i<NUM_GROUPS; i++) {
            final String groupName = groupNameBase + "_" + i;
            final AccountGroup group = (AccountGroup) new AccountGroup().setName(groupName);
            groups.add(groupDAO.create(group));

            for (int j=0; j<NUM_ACCOUNTS/2; j++) {
                final AccountGroupMember member = new AccountGroupMember(group, accounts.get(RandomUtils.nextInt(NUM_ACCOUNTS)));
                try {
                    group.addMember(memberDAO.create(member));
                } catch (DataIntegrityViolationException dive) {
                    // this can happen if the random roller above tries to add the same account to a group a second time
                    // just ignore it, the group will have one less member than it otherwise would.
                }
            }
        }

        final Set<AccountGroup> sorted = new TreeSet<>(SORT_BY_NAME);
        sorted.addAll(groups);
        groupsByName.addAll(sorted);
        for (AccountGroup g : groupsByName) groupNames.add(g.getName());
    }

    @Override public void reset() {

        final AccountGroupMemberDAO memberDAO = getBean(AccountGroupMemberDAO.class);
        final AccountGroupDAO groupDAO = getBean(AccountGroupDAO.class);

        for (AccountGroupMember m : memberDAO.findAll()) memberDAO.delete(m.getUuid());
        for (AccountGroup g : groupDAO.findAll()) groupDAO.delete(g.getUuid());
        groups.clear();
        groupNames.clear();
        groupsByName.clear();

        super.reset();
    }

    @Test
    public void testSearchGroupsByName() throws Exception {
        apiDocs.startRecording(DOC_TARGET, "basic name-sorted default search. should return the first 10 account groups by name");
        final ResultPage page = new ResultPage()
                .setPageNumber(0).setPageSize(10)
                .setSortField("name").setSortOrder(ResultPage.SortOrder.ASC);
        expectResults(page, NUM_GROUPS, groupNames, searchAccountGroups(page));
    }

    @Test
    public void testDownloadCsv () throws Exception {
        apiDocs.startRecording(DOC_TARGET, "download a CSV of the results of a search");
        apiDocs.addNote("download all groups as a CSV");
        final String csv = downloadAccountGroups(ResultPage.INFINITE_PAGE);
        assertNotNull(csv);
        for (String name : groupNames) assertTrue(csv.contains(name));
    }

}