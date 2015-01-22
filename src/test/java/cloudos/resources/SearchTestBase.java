package cloudos.resources;

import cloudos.dao.AccountDAO;
import cloudos.model.Account;
import cloudos.model.support.AccountRequest;
import org.cobbzilla.wizard.dao.SearchResults;
import org.cobbzilla.wizard.model.NamedEntity;
import org.cobbzilla.wizard.model.ResultPage;
import org.junit.Before;

import java.util.*;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.junit.Assert.assertEquals;

public class SearchTestBase extends ApiClientTestBase {

    public static final Comparator<Account> ACCOUNTS_BY_NAME = new Comparator<Account>() {
        @Override public int compare(Account a1, Account a2) { return a1.getName().compareToIgnoreCase(a2.getName()); }
    };

    public static final int NUM_ACCOUNTS = 22;

    protected static Map<String, AccountRequest> accountRequests = new HashMap<>();
    protected static final List<Account> accounts = new ArrayList<>();
    protected static final List<Account> accountsByName = new ArrayList<>();
    protected static final List<String> accountNames = new ArrayList<>();

    @Before
    public void seedAccounts () throws Exception {

        resetAccounts();

        final AccountDAO accountDAO = getBean(AccountDAO.class);
        final String accountNameBase = randomAlphanumeric(10).toLowerCase();
        for (int i=0; i<NUM_ACCOUNTS; i++) {
            final String accountName = accountNameBase + "_" + i;
            final String password = randomAlphanumeric(10);
            final AccountRequest request = newAccountRequest(accountName, password, false);
            accounts.add(accountDAO.create(request));
            accountRequests.put(accountName, request);
        }

        final Set<Account> sorted = new TreeSet<>(ACCOUNTS_BY_NAME);
        sorted.addAll(accounts);
        accountsByName.addAll(sorted);
        for (Account a : accountsByName) accountNames.add(a.getName());
    }

    public void resetAccounts () {
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

    protected void expectResults(ResultPage page, int expectedTotalCount, List<String> expected) throws Exception {
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
