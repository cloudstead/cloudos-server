package cloudos.server;

import cloudos.appstore.client.AppStoreApiClient;
import cloudos.dns.DnsClient;
import cloudos.dns.server.DynDnsConfiguration;
import cloudos.dns.service.DynDnsManager;
import cloudos.service.TwoFactorAuthService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.mail.sender.SmtpMailConfig;
import org.cobbzilla.util.dns.DnsManager;
import org.cobbzilla.util.http.ApiConnectionInfo;
import org.cobbzilla.util.system.CommandShell;
import org.cobbzilla.wizard.server.config.DatabaseConfiguration;
import org.cobbzilla.wizard.server.config.HasDatabaseConfiguration;
import org.cobbzilla.wizard.server.config.RestServerConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import rooty.RootyConfiguration;
import rooty.toots.chef.ChefHandler;
import rooty.toots.postfix.PostfixHandler;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Configuration @Slf4j
public class CloudOsConfiguration extends RestServerConfiguration implements HasDatabaseConfiguration {

    public static final String DEFAULT_ADMIN = "admin";

    @Setter private DatabaseConfiguration database;
    @Bean public DatabaseConfiguration getDatabase() { return database; }

    @Getter @Setter private CloudStorageConfiguration cloudConfig = new CloudStorageConfiguration();

    @Getter @Setter private SmtpMailConfig smtpMailConfig;
    @Getter @Setter private String emailTemplateRoot;

    @Getter @Setter private ApiConnectionInfo appStore;
    @Getter(lazy=true) private final AppStoreApiClient appStoreApiClient = initAppStoreApiClient();
    private AppStoreApiClient initAppStoreApiClient() { return new AppStoreApiClient(appStore); }

    @Getter @Setter private ApiConnectionInfo authy;
    public TwoFactorAuthService getTwoFactorAuthService () { return new TwoFactorAuthService(authy); }

    @Getter @Setter private String kadminPassword;
    @Getter @Setter private String defaultAdmin = DEFAULT_ADMIN;

    @Getter @Setter private RootyConfiguration rooty;
    public ChefHandler getChefHandler () { return rooty.getHandler(ChefHandler.class); }
    public PostfixHandler getPostfixHandler () { return rooty.getHandler(PostfixHandler.class); }

    @Getter @Setter private DynDnsConfiguration dns;
    @Setter private DnsManager dnsManager;
    public DnsManager getDnsManager() {
        if (dnsManager == null) {
            dnsManager = dns.isDynDns() ? new DynDnsManager(dns) : new DnsClient(dns);
        }
        return dnsManager;
    }

    @Getter(lazy=true) private final String hostname = initHostname();
    private String initHostname() { return CommandShell.toString("hostname"); }

    @Getter(lazy=true) private final String shortHostname = initShortHostname();
    private String initShortHostname () {
        final String h = getHostname();
        final int dotPos = h.indexOf(".");
        return dotPos == -1 ? h : h.substring(0, dotPos);
    }

    @Getter(lazy=true) private final String publicIp = initPublicIp();

    private String initPublicIp() {
        try {
            final String ip = InetAddress.getLocalHost().getHostAddress();
            log.info("initPublicIp: returning ip="+ip);
            return ip;
        } catch (UnknownHostException e) {
            throw new IllegalStateException("Error getting public ip: "+e ,e);
        }
    }

}
