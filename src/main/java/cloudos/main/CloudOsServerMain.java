package cloudos.main;

import cloudos.dao.AccountGroupDAO;
import cloudos.main.account.CloudOsGroupMainOptions;
import cloudos.model.AccountGroup;
import cloudos.server.CloudOsConfiguration;
import cloudos.server.CloudOsServer;
import org.cobbzilla.util.collection.ArrayUtil;
import org.cobbzilla.wizard.server.RestServerHarness;
import org.kohsuke.args4j.CmdLineParser;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.File;
import java.util.Arrays;
import java.util.Map;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.io.FileUtil.abs;
import static org.cobbzilla.util.system.CommandShell.isRoot;
import static org.cobbzilla.util.system.CommandShell.loadShellExports;

public class CloudOsServerMain {

    private static final File SYSTEM_ENV_FILE = new File("/etc/apache2/envvars");

    public static void main (String[] args) throws Exception {

        if (!isRoot()) die("must be root");

        if (args.length >= 3 && args[0].equals("--command")) {

            final Map<String, String> env = loadShellExports(SYSTEM_ENV_FILE);

            final File envFile = findEnvFile();
            if (!envFile.exists() || !envFile.canRead()) die("env file is unreadable: "+abs(envFile));
            env.putAll(loadShellExports(envFile));

            final String command = args[1];

            // shift args by 2 to remove --command <command-name>
            args = ArrayUtil.remove(args, 0);
            args = ArrayUtil.remove(args, 0);
            handleCommand(command, args, env);

        } else {
            die("Invalid arguments: "+Arrays.toString(args));
        }

    }

    private static File findEnvFile() {
        File userHome;
        for (String cp : System.getProperty("java.class.path").split(File.pathSeparator)) {
            if (cp.matches(".+/cloudos-server-.+\\.jar$")) {
                userHome = new File(cp).getParentFile();
                while (userHome != null && !new File(userHome, ".cloudos.env").exists()) {
                    userHome = userHome.getParentFile();
                }
                if (userHome != null) {
                    final File envFile = new File(userHome, ".cloudos.env");
                    if (envFile.exists()) return envFile;
                }
                break;
            }
        }
        return die(".cloudos.env file not found");
    }

    private static void handleCommand(String command, String[] args, Map<String, String> env) throws Exception {

        // Build the server, but do not connect any web components.
        // We only need the Spring container, properly configured
        final RestServerHarness<CloudOsConfiguration, CloudOsServer> harness = new RestServerHarness<>(CloudOsServer.class);
        final ConfigurableApplicationContext applicationContext = harness.springServer(CloudOsServer.getConfigurationSources(), env);

        // avoid parser error for required options
        args = ArrayUtil.append(args, CloudOsMainOptions.OPT_ACCOUNT);
        args = ArrayUtil.append(args, "__no_account__");
        args = ArrayUtil.append(args, CloudOsMainOptions.OPT_API_BASE);
        args = ArrayUtil.append(args, "__no_api_base__");

        switch (command) {
            case "group":
                final CloudOsGroupMainOptions options = new CloudOsGroupMainOptions() {
                    @Override protected boolean requireAccount() { return false; }
                };
                final CmdLineParser parser = new CmdLineParser(options);
                parser.parseArgument(args);

                final AccountGroupDAO groupDAO = applicationContext.getBean(AccountGroupDAO.class);
                final String name = options.getName();
                AccountGroup group = groupDAO.findByName(name);

                switch (options.getOperation()) {
                    case read:
                        System.out.println(group);
                        break;

                    case create:
                    case update:
                        if (group == null) {
                            group = groupDAO.create(options.getGroupRequest(), options.getRecipients());
                            System.out.println("Created group: " + group);
                        } else {
                            group = groupDAO.update(options.getGroupRequest());
                            System.out.println("Updated group: " + groupDAO.findByName(name).setMembers(groupDAO.buildGroupMemberList(group)));
                        }
                        break;

                    case delete:
                        if (group == null) die("No group found: "+name);
                        groupDAO.delete(group.getUuid());
                        System.out.println("Deleted group: "+group);
                        break;

                    default:
                        throw new UnsupportedOperationException("operation "+options.getOperation()+" not supported");
                }
                break;

            default:
                die("Invalid command: "+command);
        }
    }

}
