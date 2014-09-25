package cloudos.model.support;

import cloudos.model.AccountGroupInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Accessors(chain=true)
public class AccountGroupRequest {

    @Setter private String name;
    public String getName () { return name == null ? null : name.toLowerCase(); }

    @Getter @Setter private List<String> recipients = new ArrayList<>();
    public AccountGroupRequest addRecipient (String r) { recipients.add(r.toLowerCase()); return this; }

    @JsonIgnore public List<String> getRecipientsLowercase () {
        final List<String> list = new ArrayList<>();
        for (String r : recipients) list.add(r.toLowerCase());
        return list;
    }

    @Getter @Setter private AccountGroupInfo info;
    public boolean hasInfo () { return info != null; }

}
