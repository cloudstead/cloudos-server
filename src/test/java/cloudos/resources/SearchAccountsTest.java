package cloudos.resources;

import cloudos.dao.AccountDAO;
import cloudos.model.Account;
import cloudos.model.auth.LoginRequest;
import cloudos.model.support.AccountRequest;
import org.cobbzilla.wizard.dao.SearchResults;
import org.cobbzilla.wizard.model.NamedEntity;
import org.cobbzilla.wizard.model.ResultPage;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.junit.Assert.*;

public class SearchAccountsTest extends ApiClientTestBase {

    private static final String DOC_TARGET = "Search: Accounts";
    public static final Comparator<Account> SORT_BY_NAME = new Comparator<Account>() {
        @Override public int compare(Account a1, Account a2) { return a1.getName().compareToIgnoreCase(a2.getName()); }
    };

    public static final int NUM_ACCOUNTS = 22;

    protected static Map<String, AccountRequest> accountRequests = new HashMap<>();
    protected static final List<Account> accounts = new ArrayList<>();
    protected static final List<Account> accountsByName = new ArrayList<>();
    protected static final List<String> accountNames = new ArrayList<>();

    @Before
    public void seed () throws Exception {

        reset();

        final AccountDAO accountDAO = getBean(AccountDAO.class);
        final String accountNameBase = randomAlphanumeric(10).toLowerCase();
        for (int i=0; i<NUM_ACCOUNTS; i++) {
            final String accountName = accountNameBase + "_" + i;
            final String password = randomAlphanumeric(10);
            final AccountRequest request = newAccountRequest(accountName, password, false);
            accounts.add(accountDAO.create(request));
            accountRequests.put(accountName, request);
        }

        final Set<Account> sorted = new TreeSet<>(SORT_BY_NAME);
        sorted.addAll(accounts);
        accountsByName.addAll(sorted);
        for (Account a : accountsByName) accountNames.add(a.getName());
    }

    public void reset() {
        // start fresh
        final AccountDAO accountDAO = getBean(AccountDAO.class);

        for (Account account : accountDAO.findAll()) {
            accountDAO.delete(account.getName());
        }
        accountRequests.clear();
        accounts.clear();
        accountsByName.clear();
        accountNames.clear();
    }

    @Test
    public void testSearchAccountsByName() throws Exception {
        apiDocs.startRecording(DOC_TARGET, "basic name-sorted default search. should return the first 10 accounts by name");
        final ResultPage page = new ResultPage()
                .setPageNumber(0).setPageSize(10)
                .setSortField("name").setSortOrder(ResultPage.SortOrder.ASC);
        expectResults(page, NUM_ACCOUNTS, accountNames);
    }

    @Test
    public void testSearchActiveAccounts() throws Exception {

        apiDocs.startRecording(DOC_TARGET, "search accounts in various ways");

        final ResultPage page = new ResultPage().setPageNumber(0).setPageSize(10).setSortField("name").setSortOrder(ResultPage.SortOrder.ASC);;
        SearchResults<Account> found;

        apiDocs.addNote("search all accounts, should find the " + NUM_ACCOUNTS + " that were created/invited during test setup");
        found = searchAccounts(page);
        assertEquals(NUM_ACCOUNTS, found.size());

        apiDocs.addNote("search invited accounts, should find the "+NUM_ACCOUNTS+" that were created/invited during test setup");
        page.setBound(Account.STATUS, Account.Status.invited.name());
        found = searchAccounts(page);
        assertEquals(NUM_ACCOUNTS, found.size());

        // activate some accounts
        final Set<String> activated = new TreeSet<>();
        final Set<String> unactivated = new TreeSet<>();
        final int numActivated = 5;
        for (int i=0; i<NUM_ACCOUNTS; i++) {
            final String accountName = accounts.get(i).getName();
            if (i < numActivated) {
                final LoginRequest loginRequest = new LoginRequest()
                        .setName(accountName)
                        .setPassword(accountRequests.get(accountName).getPassword());
                assertEquals(200, login(loginRequest).status);
                activated.add(accountName);
            } else {
                unactivated.add(accountName);
            }
        }
        List<String> expected;
        expected = new ArrayList<>(activated);

        // after all those logins, let's flush the api tokens and push the admin token back on
        flushTokens(); pushToken(adminToken);

        page.setBound(Account.STATUS, Account.Status.active.name());
        apiDocs.addNote("we activated "+numActivated+" accounts. search active accounts, should find the activated accounts");
        expectResults(page, numActivated, expected);

        apiDocs.addNote("search invited accounts, now there should be "+(NUM_ACCOUNTS-numActivated)+" invited since "+numActivated+" were activated");
        expected = new ArrayList<>(unactivated);
        page.setBound(Account.STATUS, Account.Status.invited.name());
        expectResults(page, NUM_ACCOUNTS-numActivated, expected);

        apiDocs.addNote("search all accounts, should find the " + NUM_ACCOUNTS + " that were created/invited during test setup");
        page.unsetBound(Account.STATUS);
        found = searchAccounts(page);
        assertEquals(NUM_ACCOUNTS, found.size());
    }

    @Test
    public void testSearchSuspendedAccounts () throws Exception {

        apiDocs.startRecording(DOC_TARGET, "suspend some accounts and verify searches return correctly");

        final ResultPage page = new ResultPage().setPageNumber(0).setPageSize(10).setSortField("name").setSortOrder(ResultPage.SortOrder.ASC);;
        SearchResults<Account> found;

        apiDocs.addNote("search all accounts, should find the " + NUM_ACCOUNTS + " that were created/invited during test setup");
        found = searchAccounts(page);
        assertEquals(NUM_ACCOUNTS, found.size());

        // login every account we created
        apiDocs.addNote("login all accounts, so that all accounts are active");
        for (AccountRequest request : accountRequests.values()) {
            login(new LoginRequest().setName(request.getAccountName()).setPassword(request.getPassword()));
        }
        flushTokens(); pushToken(adminToken);

        apiDocs.addNote("search active accounts, should find the " + NUM_ACCOUNTS + " that just logged in");
        page.setBound(Account.STATUS, Account.Status.active.name());
        expectResults(page, NUM_ACCOUNTS, accountNames);

        // suspend some accounts
        final Set<String> suspended = new TreeSet<>();
        final Set<String> unsuspended = new TreeSet<>();
        final int numSuspended = 5;
        for (int i=0; i<NUM_ACCOUNTS; i++) {
            final String accountName = accounts.get(i).getName();
            if (i < numSuspended) {
                suspend(accountRequests.get(accountName));
                suspended.add(accountName);
            } else {
                unsuspended.add(accountName);
            }
        }
        List<String> expected;
        expected = new ArrayList<>(suspended);

        apiDocs.addNote("search suspended accounts, should find the " + numSuspended + " that were just suspended");
        page.setBound(Account.STATUS, Account.Status.suspended.name());
        expectResults(page, numSuspended, expected);

        apiDocs.addNote("search active accounts, should find the " + (NUM_ACCOUNTS-numSuspended) + " that are not suspended");
        page.setBound(Account.STATUS, Account.Status.active.name());
        expected = new ArrayList<>(unsuspended);
        expectResults(page, NUM_ACCOUNTS - numSuspended, expected);
    }

    @Test
    public void testSearchAdminsAndNonAdmins () throws Exception {
        apiDocs.startRecording(DOC_TARGET, "update some accounts and make them admins, verify searches return correctly");

        final ResultPage page = new ResultPage().setPageNumber(0).setPageSize(10).setSortField("name").setSortOrder(ResultPage.SortOrder.ASC);;
        SearchResults<Account> found;

        apiDocs.addNote("search all accounts, should find the " + NUM_ACCOUNTS + " that were created/invited during test setup");
        found = searchAccounts(page);
        assertEquals(NUM_ACCOUNTS, found.size());

        apiDocs.addNote("search admin accounts, should not find any");
        page.setBound(Account.STATUS, Account.Status.admins.name());
        expectResults(page, 0, Collections.EMPTY_LIST);

        // make some accounts admins
        final Set<String> admins = new TreeSet<>();
        final Set<String> nonadmins = new TreeSet<>();
        final int numAdmins = 5;
        for (int i=0; i<NUM_ACCOUNTS; i++) {
            final String accountName = accounts.get(i).getName();
            if (i < numAdmins) {
                apiDocs.addNote("update account "+accountName+": make an admin");
                update((AccountRequest) accountRequests.get(accountName).setAdmin(true));
                admins.add(accountName);
            } else {
                nonadmins.add(accountName);
            }
        }
        List<String> expected;
        expected = new ArrayList<>(admins);

        apiDocs.addNote("search admin accounts, should find the " + numAdmins + " that were just made admins");
        page.setBound(Account.STATUS, Account.Status.admins.name());
        expectResults(page, numAdmins, expected);

        apiDocs.addNote("search non-admin accounts, should find the " + (NUM_ACCOUNTS - numAdmins) + " that are not admins");
        page.setBound(Account.STATUS, Account.Status.non_admins.name());
        expected = new ArrayList<>(nonadmins);
        expectResults(page, NUM_ACCOUNTS - numAdmins, expected);
    }

    @Test
    public void testSearchAsNonAdminAccount () throws Exception {

        apiDocs.startRecording(DOC_TARGET, "as a non-admin, search accounts. sensitive fields should be hidden.");

        apiDocs.addNote("login as a non-admin user");
        final String accountName = accounts.get(0).getName();
        login(new LoginRequest().setName(accountName).setPassword(accountRequests.get(accountName).getPassword()));

        apiDocs.addNote("search all accounts, should find the " + NUM_ACCOUNTS + " that were created/invited during test setup. " +
                "\nVerify that the results do not include uuids, recovery emails, or authids");

        final SearchResults<Account> found = searchAccounts(ResultPage.INFINITE_PAGE);
        assertEquals(NUM_ACCOUNTS, found.size());
        assertEquals(NUM_ACCOUNTS, found.getResults().size());

        for (Account a : found.getResults()) {
            assertNull(a.getUuid());
            assertNull(a.getEmail());
            assertNull(a.getAuthId());
            assertFalse(a.isAdmin());
            assertFalse(a.isTwoFactor());
        }
    }

    @Test
    public void testDownloadCsv () throws Exception {
        apiDocs.startRecording(DOC_TARGET, "download a CSV of the results of a search");
        apiDocs.addNote("download all accounts as a CSV");
        final String csv = downloadAccounts(ResultPage.INFINITE_PAGE);
        assertNotNull(csv);
        for (String name : accountNames) assertTrue(csv.contains(name));
    }

    private void expectResults(ResultPage page, int expectedTotalCount, List<String> expected) throws Exception {
        expectResults(page, expectedTotalCount, expected, searchAccounts(page));
    }

    protected void expectResults(ResultPage page, int expectedTotalCount, List<String> expected, SearchResults<? extends NamedEntity> found) {
        assertEquals(expectedTotalCount, found.size());
        final int limit = Math.min(expectedTotalCount, page.getPageSize());
        for (int i=0; i<limit; i++) {
            assertEquals(found.getResult(i).getName(), expected.get(i));
        }
    }

}
