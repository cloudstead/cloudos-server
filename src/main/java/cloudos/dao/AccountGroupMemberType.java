package cloudos.dao;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum AccountGroupMemberType {

    account, group;

    @JsonCreator public AccountGroupMemberType create(String s) { return AccountGroupMemberType.valueOf(s.toLowerCase()); }

}
