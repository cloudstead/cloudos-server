package cloudos.resources;

import cloudos.dao.AccountGroupMemberType;
import cloudos.model.Account;
import cloudos.model.AccountGroup;
import cloudos.model.AccountGroupInfo;
import cloudos.model.support.AccountGroupMemberView;
import cloudos.model.support.AccountGroupRequest;
import cloudos.model.support.AccountGroupView;
import cloudos.model.support.AccountRequest;
import org.apache.commons.lang3.RandomUtils;
import org.cobbzilla.util.http.HttpStatusCodes;
import org.cobbzilla.wizard.model.ResultPage;
import org.cobbzilla.wizard.util.RestResponse;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static cloudos.resources.ApiConstants.GROUPS_ENDPOINT;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.cobbzilla.util.http.HttpStatusCodes.OK;
import static org.cobbzilla.util.http.HttpStatusCodes.UNPROCESSABLE_ENTITY;
import static org.cobbzilla.util.json.JsonUtil.fromJson;
import static org.cobbzilla.util.json.JsonUtil.toJson;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AccountGroupsResourceTest extends ApiClientTestBase {

    public static final String DOC_TARGET = "Account Group Management";

    public static final int NUM_ACCOUNTS = 5;
    private List<Account> testAccounts;

    @Before public void createTestAccounts() throws Exception {
        RestResponse response;
        testAccounts = new ArrayList<>();

        // create some users
        for (int i=0; i<NUM_ACCOUNTS; i++) {
            final String accountName = randomAlphanumeric(10);
            final AccountRequest request = newAccountRequest(accountName);
            apiDocs.addNote("create user #"+i);
            response = put(ApiConstants.ACCOUNTS_ENDPOINT + "/" + accountName, toJson(request));
            assertEquals(200, response.status);
            testAccounts.add(fromJson(response.json, Account.class));
        }
    }

    @Test public void testGroupCrud() throws Exception {
        apiDocs.startRecording(DOC_TARGET, "create, read, update, delete");

        // get users, expect to see the ones we've created, plus ourselves
        final List<Account> accounts = searchAccounts(ResultPage.INFINITE_PAGE).getResults();
        for (Account created : testAccounts) {
            boolean exists = false;
            for (Account found : accounts) {
                if (found.getName().equals(created.getName())) {
                    exists = true;
                    break;
                }
            }
            assertTrue("Account was not created: "+created.getName(), exists);
        }

        AccountGroupView created;
        RestResponse response;

        // create "group1" with 2 users, illustrate info fields too
        final AccountGroupRequest group1 = new AccountGroupRequest()
                .addRecipient(testAccounts.get(0).getName())
                .addRecipient(testAccounts.get(1).getName())
                .setName(randomAlphanumeric(10).toLowerCase())
                .setInfo(new AccountGroupInfo()
                        .setDescription(randomAlphanumeric(100))
                        .setStorageQuota((10 + RandomUtils.nextInt(0, 21)) + "gb"));
        apiDocs.addNote("create a group with 2 users, a random description and a storage quota of "+group1.getInfo().getStorageQuota());
        response = put(GROUPS_ENDPOINT +"/"+group1.getName(), toJson(group1));
        assertEquals(HttpStatusCodes.OK, response.status);

        // validate that the group created contains everything we expect
        created = fromJson(response.json, AccountGroupView.class);
        assertEquals(group1.getName(), created.getName());
        assertEquals(group1.getRecipients().size(), created.getMembers().size());
        for (AccountGroupMemberView m : created.getMembers()) {
            assertTrue(group1.getRecipients().contains(m.getName()));
            assertEquals(AccountGroupMemberType.account, m.getType());
        }

        // create "group2" with 2 other users and group1
        final AccountGroupRequest group2 = new AccountGroupRequest()
                .addRecipient(testAccounts.get(2).getName())
                .addRecipient(testAccounts.get(3).getName())
                .addRecipient(group1.getName())
                .setName(randomAlphanumeric(10).toLowerCase());
        apiDocs.addNote("create another group with 2 other users, plus the previous group");
        response = put(GROUPS_ENDPOINT +"/"+group2.getName(), toJson(group2));
        assertEquals(HttpStatusCodes.OK, response.status);

        // validate that the group created contains everything we expect
        created = fromJson(response.json, AccountGroupView.class);
        assertEquals(group2.getName(), created.getName());
        assertEquals(group2.getRecipients().size(), created.getMembers().size());
        for (AccountGroupMemberView m : created.getMembers()) {
            assertTrue(group2.getRecipients().contains(m.getName()));
            if (m.getName().equals(group1.getName())) {
                assertEquals(AccountGroupMemberType.group, m.getType());
            } else {
                assertEquals(AccountGroupMemberType.account, m.getType());
            }
        }

        // create "group3" with group2 and group1 (should fail with circular dependency)
        final AccountGroupRequest group3 = new AccountGroupRequest()
                .addRecipient(group1.getName())
                .addRecipient(group2.getName())
                .setName(randomAlphanumeric(10).toLowerCase());
        apiDocs.addNote("create a third group with both previous groups, should fail due to circular dependency");
        response = doPut(GROUPS_ENDPOINT +"/"+group3.getName(), toJson(group3));
        assertEquals(UNPROCESSABLE_ENTITY, response.status);

        apiDocs.addNote("fetch all groups, there should be 4 (the 2 we created + the 2 default groups)");
        AccountGroup[] groups = fromJson(get(GROUPS_ENDPOINT).json, AccountGroup[].class);
        assertEquals(4, groups.length);

        // update group2, keep one user, add another user, remove everyone else
        group2.getRecipients().remove(testAccounts.get(2).getName());
        group2.getRecipients().remove(group1.getName());
        group2.addRecipient(testAccounts.get(4).getName());
        apiDocs.addNote("update the second group. keep one user, add another, remove everyone else");
        response = doPost(GROUPS_ENDPOINT +"/"+group2.getName(), toJson(group2));
        assertEquals(HttpStatusCodes.OK, response.status);

        // refetch group2 and validate
        apiDocs.addNote("fetch second group, ensure members are correct");
        AccountGroupView updated = fromJson(doGet(GROUPS_ENDPOINT +"/"+group2.getName()).json, AccountGroupView.class);
        assertEquals(group2.getName(), updated.getName());
        assertEquals(2, updated.getMembers().size());
        for (AccountGroupMemberView m : updated.getMembers()) {
            assertTrue(group2.getRecipients().contains(m.getName()));
            assertEquals(AccountGroupMemberType.account, m.getType());
        }

        // delete all groups
        apiDocs.addNote("delete first group");  delete(GROUPS_ENDPOINT + "/" + group1.getName());
        apiDocs.addNote("delete second group"); delete(GROUPS_ENDPOINT + "/" + group2.getName());

        apiDocs.addNote("fetch all groups, there should be only the 2 default groups");
        groups = fromJson(get(GROUPS_ENDPOINT).json, AccountGroup[].class);
        assertEquals(2, groups.length);
        assertEquals(AccountGroup.ADMIN_GROUP_NAME, groups[0].getName());
        assertEquals(AccountGroup.DEFAULT_GROUP_NAME, groups[1].getName());

        // looking up a single one should return 404
        apiDocs.addNote("fetch one of the old groups, should get a 404");
        assertEquals(HttpStatusCodes.NOT_FOUND, doGet(GROUPS_ENDPOINT +"/"+group2.getName()).status);
    }

    @Test public void testMirrorGroup () throws Exception {

        RestResponse response;
        final String sourceName = randomAlphanumeric(10).toLowerCase();
        final String mirrorName = sourceName + "_mirror";

        apiDocs.startRecording(DOC_TARGET, "mirror groups: create, read, update, delete");

        apiDocs.addNote("create a source group with some members");
        final AccountGroupRequest sourceGroupRequest = new AccountGroupRequest()
                .addRecipient(testAccounts.get(0).getName())
                .addRecipient(testAccounts.get(1).getName())
                .addRecipient(testAccounts.get(2).getName())
                .setName(sourceName);
        response = put(GROUPS_ENDPOINT +"/"+ sourceName, toJson(sourceGroupRequest));
        assertEquals(HttpStatusCodes.OK, response.status);

        apiDocs.addNote("create a mirror");
        final AccountGroupRequest mirrorGroupRequest = new AccountGroupRequest()
                .setMirror(sourceName)
                .setName(mirrorName);
        response = put(GROUPS_ENDPOINT +"/"+mirrorName, toJson(mirrorGroupRequest));
        assertEquals(HttpStatusCodes.OK, response.status);

        apiDocs.addNote("verify members are the same");
        AccountGroupView mirrorGroup = fetchGroupView(mirrorName);
        assertEquals(sourceGroupRequest.getRecipients().size(), mirrorGroup.getMemberCount());

        apiDocs.addNote("add an account to the source group");
        sourceGroupRequest.addRecipient(testAccounts.get(3).getName());
        assertEquals(4, sourceGroupRequest.getRecipients().size());
        AccountGroupView sourceGroup = updateGroup(sourceGroupRequest);
        assertEquals(sourceGroupRequest.getRecipients().size(), sourceGroup.getMemberCount());

        apiDocs.addNote("verify mirror contains new account");
        mirrorGroup = fetchGroupView(mirrorName);
        assertEquals(sourceGroupRequest.getRecipients().size(), mirrorGroup.getMemberCount());

        apiDocs.addNote("remove two accounts from the source group");
        sourceGroupRequest.getRecipients().remove(0);
        sourceGroupRequest.getRecipients().remove(0);
        assertEquals(2, sourceGroupRequest.getRecipients().size());
        sourceGroup = updateGroup(sourceGroupRequest);
        assertEquals(sourceGroupRequest.getRecipients().size(), sourceGroup.getMemberCount());

        apiDocs.addNote("verify mirror has account removed");
        mirrorGroup = fetchGroupView(mirrorName);
        assertEquals(sourceGroupRequest.getRecipients().size(), mirrorGroup.getMemberCount());

        apiDocs.addNote("remove source group, should fail since mirror still exists");
        assertEquals(UNPROCESSABLE_ENTITY, doDelete(GROUPS_ENDPOINT+"/"+sourceName).status);

        apiDocs.addNote("remove mirror");
        assertEquals(OK, doDelete(GROUPS_ENDPOINT + "/" + mirrorName).status);

        apiDocs.addNote("remove source group, should now succeed");
        assertEquals(OK, doDelete(GROUPS_ENDPOINT + "/" + sourceName).status);
    }

    public AccountGroupView updateGroup(AccountGroupRequest request) throws Exception {
        assertEquals(OK, post(GROUPS_ENDPOINT +"/"+ request.getName(), toJson(request)).status);
        return fetchGroupView(request.getName());
    }

    public AccountGroupView fetchGroupView(String groupName) throws Exception {
        return fromJson(get(GROUPS_ENDPOINT+"/"+groupName).json, AccountGroupView.class);
    }

}