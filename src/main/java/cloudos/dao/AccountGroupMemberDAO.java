package cloudos.dao;

import cloudos.model.AccountGroupMember;
import org.cobbzilla.wizard.dao.AbstractCRUDDAO;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class AccountGroupMemberDAO extends AbstractCRUDDAO<AccountGroupMember> {

    public List<AccountGroupMember> findByGroup(String groupUuid) { return findByField("groupUuid", groupUuid); }
    public List<AccountGroupMember> findByGroupName(String groupName) { return findByField("groupName", groupName); }

    public AccountGroupMember findByGroupAndMember(String groupUuid, String memberUuid) {
        return uniqueResult(criteria().add(
                Restrictions.and(
                        Restrictions.eq("groupUuid", groupUuid),
                        Restrictions.eq("memberUuid", memberUuid)
                )
        ));
    }

    public Map<String, Integer> findAllMembershipCounts() {
        // todo: cache this?
        final List<Object[]> results = hibernateTemplate.findByCriteria(criteria().setProjection(Projections.projectionList().add(Projections.groupProperty("groupUuid")).add(Projections.rowCount())));
        final HashMap<String, Integer> map = new HashMap<>();
        for (Object[] result : results) {
            map.put((String) result[0], ((Number) result[1]).intValue());
        }
        return map;
    }
}
