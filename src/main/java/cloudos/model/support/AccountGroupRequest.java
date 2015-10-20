package cloudos.model.support;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.util.string.StringUtil;

import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@NoArgsConstructor @Accessors(chain=true)
public class AccountGroupRequest {

    @Size(min=2, max=100, message="err.name.length")
    @Setter private String name;

    public AccountGroupRequest(String groupName, String description) {
        setName(groupName);
        setDescription(description);
    }

    public String getName () { return name == null ? null : name.toLowerCase(); }

    // comma or space separated list of groups to mirror members from
    @Getter @Setter private String mirrors;
    public boolean hasMirror() { return !empty(mirrors); }
    public List<String> getMirrorList () { return empty(mirrors) ? new ArrayList<String>() : StringUtil.split(mirrors, " ,"); }

    @Getter @Setter private List<String> recipients = new ArrayList<>();
    public AccountGroupRequest addRecipient (String r) { recipients.add(r.toLowerCase()); return this; }

    @JsonIgnore public List<String> getRecipientsLowercase () {
        final List<String> list = new ArrayList<>();
        for (String r : recipients) list.add(r.toLowerCase());
        return list;
    }

    @Getter @Setter private String description;
    @Getter @Setter private String storageQuota;


}
