package cloudos.main;

import lombok.Getter;
import lombok.Setter;
import org.kohsuke.args4j.Option;

import java.io.File;

public class UnlockMainOptions extends CloudOsMainOptions {

    public static final String USAGE_PEM = "The PEM file (public key) that will replace the default https certificate's PEM file";
    public static final String OPT_PEM = "-p";
    public static final String LONGOPT_PEM = "--pem";
    @Option(name=OPT_PEM, aliases=LONGOPT_PEM, usage=USAGE_PEM, required=true)
    @Getter @Setter private File pem;

    public static final String USAGE_KEY = "The private key file that will replace the default https certificate's private key";
    public static final String OPT_KEY = "-k";
    public static final String LONGOPT_KEY = "--key";
    @Option(name=OPT_KEY, aliases=LONGOPT_KEY, usage=USAGE_KEY, required=true)
    @Getter @Setter private File key;

    public static final String USAGE_AUTHY_USER = "The name of the Authy user that will replace the built-in Authy user for 2-factor authentication";
    public static final String OPT_AUTHY_USER = "-A";
    public static final String LONGOPT_AUTHY_USER = "--authy-user";
    @Option(name=OPT_AUTHY_USER, aliases=LONGOPT_AUTHY_USER, usage=USAGE_AUTHY_USER, required=true)
    @Getter @Setter private String authy;

    public static final String USAGE_FORCE = "Re-unlock the system with a new default https certificate and Authy user, even if the system already appears to be unlocked";
    public static final String OPT_FORCE = "-f";
    public static final String LONGOPT_FORCE = "--force";
    @Option(name=OPT_FORCE, aliases=LONGOPT_FORCE, usage=USAGE_FORCE)
    @Getter @Setter private boolean force = false;


}
