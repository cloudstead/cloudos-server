package cloudos.model.support;

import cloudos.model.Account;
import cloudos.model.auth.CloudOsAuthResponse;
import cloudos.server.CloudOsConfiguration;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.cobbzilla.util.string.Base64;

import static org.cobbzilla.util.string.StringUtil.UTF8cs;

@NoArgsConstructor
public class SetupResponse extends CloudOsAuthResponse {

    public SetupResponse(String sessionId, Account account, CloudOsConfiguration configuration, String backupKey) {
        super(sessionId, account);
        restoreKey = generateRestoreKey(configuration, backupKey);
    }

    public static String generateRestoreKey(CloudOsConfiguration configuration, String backupKey) {
        return Base64.encodeBytes(new StringBuilder()
                .append("\nexport AWS_ACCESS_KEY_ID=").append(configuration.getCloudConfig().getAWSAccessKeyId())
                .append("\nexport AWS_SECRET_ACCESS_KEY=").append(configuration.getCloudConfig().getAWSSecretKey())
                .append("\nexport AWS_IAM_USER=").append(configuration.getCloudConfig().getUsername())
                .append("\nexport S3_BUCKET=").append(configuration.getCloudConfig().getBucket())
                .append("\nexport BACKUP_KEY=").append(backupKey).append("\n")
                .append("\nexport FOR_CLOUDOS_HOSTNAME=").append(configuration.getHostname()).append("\n")
                .toString().getBytes(UTF8cs));
    }

    @Getter @Setter private String restoreKey;
}
