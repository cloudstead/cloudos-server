package cloudos.main;

import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.util.daemon.ZillaRuntime;
import org.kohsuke.args4j.Option;
import rooty.toots.service.ServiceKeyRequest;

import static org.cobbzilla.util.daemon.ZillaRuntime.*;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

public class ServiceKeyMainOptions extends CloudOsMainOptions {

    public static final String USAGE_OPERATION = "The operation to perform. If omitted, all keys will be listed";
    public static final String OPT_OPERATION = "-o";
    public static final String LONGOPT_OPERATION = "--operation";
    @Option(name=OPT_OPERATION, aliases=LONGOPT_OPERATION, usage=USAGE_OPERATION)
    @Getter @Setter private ServiceKeyRequest.Operation operation;

    public static final String USAGE_NAME = "The name of the valet key to add/remove";
    public static final String OPT_NAME = "-n";
    public static final String LONGOPT_NAME = "--name";
    @Option(name=OPT_NAME, aliases=LONGOPT_NAME, usage=USAGE_NAME)
    @Getter @Setter private String name;

    public boolean hasName () { return !empty(name); }

    public static final String USAGE_RECIPIENT = "The recipient of the valet key";
    public static final String OPT_RECIPIENT = "-r";
    public static final String LONGOPT_RECIPIENT = "--recipient";
    @Option(name=OPT_RECIPIENT, aliases=LONGOPT_RECIPIENT, usage=USAGE_RECIPIENT)
    @Getter @Setter private ServiceKeyRequest.Recipient recipient = ServiceKeyRequest.Recipient.VENDOR;

}
