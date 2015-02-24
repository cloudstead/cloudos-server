package cloudos.dao;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum AccountGroupMemberType {

    account, group;

    @JsonCreator public AccountGroupMemberType fromString (String s) { return AccountGroupMemberType.valueOf(s.toLowerCase()); }

}
