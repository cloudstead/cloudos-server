package cloudos.resources;

import cloudos.dao.AccountGroupDAO;
import cloudos.dao.AccountGroupMemberDAO;
import cloudos.dao.SessionDAO;
import cloudos.model.Account;
import cloudos.model.AccountGroup;
import cloudos.model.AccountGroupMember;
import cloudos.model.support.AccountGroupRequest;
import cloudos.model.support.AccountGroupView;
import cloudos.service.RootyService;
import com.qmino.miredot.annotations.ReturnType;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.resources.ResourceUtil;
import org.cobbzilla.wizard.validation.SimpleViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rooty.events.email.EmailAliasEvent;
import rooty.events.email.NewEmailAliasEvent;
import rooty.events.email.RemoveEmailAliasEvent;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path(ApiConstants.GROUPS_ENDPOINT)
@Service @Slf4j
public class AccountGroupsResource {

    @Autowired private AccountGroupDAO groupDAO;
    @Autowired private AccountGroupMemberDAO memberDAO;
    @Autowired private SessionDAO sessionDAO;
    @Autowired private RootyService rooty;

    /**
     * Find all groups. Must be admin.
     * @param apiKey The session ID
     * @return a List of AccountGroupViews
     * @statuscode 403 caller is not an admin
     */
    @GET
    @ReturnType("java.util.List<cloudos.model.support.AccountGroupView>")
    public Response findAll(@HeaderParam(ApiConstants.H_API_KEY) String apiKey) {

        final Account admin = sessionDAO.find(apiKey);
        if (admin == null) return ResourceUtil.notFound(apiKey);
        if (!admin.isAdmin()) return ResourceUtil.forbidden();

        final List<AccountGroup> groups = groupDAO.findAll();
        final List<AccountGroupView> views = new ArrayList<>();
        for (AccountGroup g : groups) views.add(buildAccountGroupView(memberDAO, g, null));
        return Response.ok(groups).build();
    }

    /**
     * Create a new AccountGroup
     * @param apiKey The session ID
     * @param groupName name of the new group
     * @param groupRequest The GroupRequest
     * @return an AccountGroupView representing the newly created group
     * @statuscode 403 if the caller is not an admin
     * @statuscode 422 if the group cannot be created due to validation errors
     */
    @PUT
    @Path("/{group}")
    @ReturnType("cloudos.model.support.AccountGroupView")
    public Response create(@HeaderParam(ApiConstants.H_API_KEY) String apiKey,
                           @PathParam("group") String groupName,
                           AccountGroupRequest groupRequest) {

        final Account admin = sessionDAO.find(apiKey);
        if (admin == null) return ResourceUtil.notFound(apiKey);

        // todo: more sophisticated authz check (perhaps for "create group" permission?)
        if (!admin.isAdmin()) return ResourceUtil.forbidden();

        groupName = groupName.toLowerCase();
        final List<String> recipients = groupRequest.getRecipientsLowercase();
        if (recipients.isEmpty()) {
            log.warn("Cannot create empty group "+groupName+", adding caller ("+admin.getName()+") as sole member");
            recipients.add(admin.getName());
        }

        if (!groupName.equalsIgnoreCase(groupRequest.getName())) {
            throw new SimpleViolationException("{err.name.mismatch}", "group name in json was different from uri");
        }

        final AccountGroup created = groupDAO.create(groupRequest, recipients);
        if (created == null) throw new IllegalStateException("create: createAccountGroup returned null!"); //should never happen

        // build view
        final AccountGroupView view = buildAccountGroupView(memberDAO, created, created.getMembers());

        // tell rooty. this will create email mailbox and other per-user stuffs. apps can listen on this MQ as well.
        announce(groupName, recipients);

        return Response.ok(view).build();
    }

    /**
     * Update an AccountGroup
     * @param apiKey The session ID
     * @param groupName name of the group to update
     * @param groupRequest The GroupRequest
     * @return the updated AccountGroup
     * @statuscode 403 if the caller is not an admin
     * @statuscode 404 group not found
     * @statuscode 422 if the group cannot be created due to validation errors
     */
    @POST
    @Path("/{group}")
    @ReturnType("cloudos.model.AccountGroup")
    public Response update(@HeaderParam(ApiConstants.H_API_KEY) String apiKey,
                           @PathParam("group") String groupName,
                           AccountGroupRequest groupRequest) {

        final Account admin = sessionDAO.find(apiKey);
        if (admin == null) return ResourceUtil.notFound(apiKey);
        if (!admin.isAdmin()) return ResourceUtil.forbidden();

        groupName = groupName.toLowerCase();
        final List<String> recipients = groupRequest.getRecipientsLowercase();

        if (!groupName.equalsIgnoreCase(groupRequest.getName())) {
            throw new SimpleViolationException("{err.name.mismatch}", "group name in json was different from uri");
        }

        final AccountGroup group = groupDAO.findByName(groupName.toLowerCase());
        if (group == null) return ResourceUtil.notFound(groupName);

        // update if quota/description changed
        if (!group.sameInfo(groupRequest.getInfo())) {
            group.setInfo(groupRequest.getInfo());
            groupDAO.update(group);
        }

        if (!recipients.isEmpty()) {
            if (groupDAO.createsCircularReference(groupName, recipients)) {
                throw new SimpleViolationException("{err.group.circularReference}", "group cannot contain a circular reference");
            }

            // update members and announce change
            groupDAO.mergeMembers(groupRequest, group);
            announce(groupName, recipients);
        }

        return Response.ok(group).build();
    }

    /**
     * @param group The group to base the view upon
     * @param members The members of the group. If null, no members are added to the view. \
     *                If non-null, the elements must have been initialized with populateBy[Name|Uuid] so that \
     *                their account or accountGroup property is set.
     * @return The view of the group
     */
    public static AccountGroupView buildAccountGroupView(AccountGroupMemberDAO memberDAO, AccountGroup group, List<AccountGroupMember> members) {

        final Map<String, Integer> counts = memberDAO.findAllMembershipCounts();

        final AccountGroupView view = new AccountGroupView(group);
        view.resetMembers();
        if (members != null) view.addMembers(members);

        view.setMemberCount(counts.get(group.getUuid()));

        return view;
    }

    /**
     * Find an AccountGroup
     * @param apiKey The session ID
     * @param groupName name of the group to find
     * @return the AccountGroupView
     * @statuscode 403 if the caller is not an admin
     * @statuscode 404 group not found
     */
    @GET
    @Path("/{group}")
    @ReturnType("cloudos.model.support.AccountGroupView")
    public Response find(@HeaderParam(ApiConstants.H_API_KEY) String apiKey,
                         @PathParam("group") String groupName) {

        final Account admin = sessionDAO.find(apiKey);
        if (admin == null) return ResourceUtil.notFound(apiKey);
        if (!admin.isAdmin()) return ResourceUtil.forbidden();

        groupName = groupName.toLowerCase();

        final AccountGroup group = groupDAO.findByName(groupName);
        if (group == null) return ResourceUtil.notFound(groupName);

        final List<AccountGroupMember> members = groupDAO.buildGroupMemberList(group);
        final AccountGroupView view = buildAccountGroupView(memberDAO, group, members);

        return Response.ok(view).build();
    }

    /**
     * Delete an AccountGroup
     * @param apiKey The session ID
     * @param groupName name of the group to delete
     * @return Just an HTTP status code
     */
    @DELETE
    @Path("/{group}")
    @ReturnType("java.lang.Void")
    public Response remove(@HeaderParam(ApiConstants.H_API_KEY) String apiKey,
                           @PathParam("group") String groupName) {

        final Account admin = sessionDAO.find(apiKey);
        if (admin == null) return ResourceUtil.notFound(apiKey);
        if (!admin.isAdmin()) return ResourceUtil.forbidden();

        groupName = groupName.toLowerCase();

        final AccountGroup group = groupDAO.findByName(groupName);
        if (group == null) return ResourceUtil.notFound(groupName);

        // delete members
        for (AccountGroupMember m : memberDAO.findByGroup(group.getUuid())) {
            memberDAO.delete(m.getUuid());
        }
        // delete group
        groupDAO.delete(group.getUuid());

        // Announce removed alias on the event bus
        announce(new RemoveEmailAliasEvent(groupName));

        return Response.ok(Boolean.TRUE).build();
    }

    private void announce(String groupName, List<String> recipients) {
        final EmailAliasEvent event = new NewEmailAliasEvent()
                .setRecipients(recipients)
                .setName(groupName);
        announce(event);
    }

    private void announce(EmailAliasEvent event) { rooty.getSender().write(event); }

}
