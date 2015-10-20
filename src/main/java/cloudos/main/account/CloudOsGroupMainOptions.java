package cloudos.main.account;

import cloudos.main.CloudOsMainOptions;
import cloudos.model.AccountGroupInfo;
import cloudos.model.support.AccountGroupRequest;
import edu.emory.mathcs.backport.java.util.Collections;
import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.wizard.api.CrudOperation;
import org.kohsuke.args4j.Option;

import java.util.List;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.string.StringUtil.split;

public class CloudOsGroupMainOptions extends CloudOsMainOptions {

    public static final String USAGE_NAME = "Name of the group (if omitted all groups will be listed). Required for write operations.";
    public static final String OPT_NAME = "-n";
    public static final String LONGOPT_NAME = "--name";
    @Option(name=OPT_NAME, aliases=LONGOPT_NAME, usage=USAGE_NAME)
    @Getter @Setter private String name;
    public boolean hasName () { return !empty(name); }

    public static final String USAGE_DESCRIPTION = "Description of the group (for new groups)";
    public static final String OPT_DESCRIPTION = "-d";
    public static final String LONGOPT_DESCRIPTION = "--description";
    @Option(name=OPT_DESCRIPTION, aliases=LONGOPT_DESCRIPTION, usage=USAGE_DESCRIPTION)
    @Getter @Setter private String description;

    public static final String USAGE_QUOTA = "Storage quota for the group";
    public static final String OPT_QUOTA = "-q";
    public static final String LONGOPT_QUOTA = "--quota";
    @Option(name=OPT_QUOTA, aliases=LONGOPT_QUOTA, usage=USAGE_QUOTA)
    @Getter @Setter private String quota;

    public static final String USAGE_MEMBERS = "Members to add/remove";
    public static final String OPT_MEMBERS = "-m";
    public static final String LONGOPT_MEMBERS = "--members";
    @Option(name=OPT_MEMBERS, aliases=LONGOPT_MEMBERS, usage=USAGE_MEMBERS)
    @Getter @Setter private String members;

    public static final String USAGE_MIRROR = "Mirror other groups";
    public static final String OPT_MIRROR = "-M";
    public static final String LONGOPT_MIRROR = "--mirrors";
    @Option(name=OPT_MIRROR, aliases=LONGOPT_MIRROR, usage=USAGE_MIRROR)
    @Getter @Setter private String mirrors;

    public static final String USAGE_OPERATION = "The operation to perform";
    public static final String OPT_OPERATION = "-o";
    public static final String LONGOPT_OPERATION = "--operation";
    @Option(name=OPT_OPERATION, aliases=LONGOPT_OPERATION, usage=USAGE_OPERATION)
    @Getter @Setter private CrudOperation operation = CrudOperation.read;

    public AccountGroupInfo getInfo() {
        return new AccountGroupInfo().setDescription(description).setStorageQuota(quota);
    }

    public List<String> getRecipients() { return empty(members) ? Collections.emptyList() : split(members, ", \t"); }

    public AccountGroupRequest getGroupRequest() {
        return new AccountGroupRequest()
                .setName(getName())
                .setDescription(getDescription())
                .setMirrors(getMirrors())
                .setRecipients(getRecipients());
    }

}
