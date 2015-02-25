package cloudos.model.support;

import cloudos.model.AccountGroupInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

import static org.cobbzilla.util.string.StringUtil.empty;

@NoArgsConstructor @Accessors(chain=true)
public class AccountGroupRequest {

    @Size(min=2, max=100, message="err.name.length")
    @Setter private String name;

    public AccountGroupRequest(String groupName, String description) {
        setName(groupName);
        setInfo(new AccountGroupInfo().setDescription(description));
    }

    public String getName () { return name == null ? null : name.toLowerCase(); }

    // name of a group to mirror members from
    @Size(min=2, max=100, message="err.mirror.length")
    @Getter @Setter private String mirror;
    public boolean hasMirror() { return !empty(mirror); }

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
