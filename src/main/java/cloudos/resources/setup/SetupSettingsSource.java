package cloudos.resources.setup;

import cloudos.model.support.SetupRequest;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.io.FileUtil;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.util.security.bcrypt.BCrypt;
import org.cobbzilla.wizard.validation.SimpleViolationException;
import org.springframework.stereotype.Service;

import java.io.File;

import static org.cobbzilla.util.io.FileUtil.abs;
import static org.cobbzilla.util.io.FileUtil.toStringOrDie;

@Service @Slf4j
public class SetupSettingsSource {

    public static final String USER_HOME = System.getProperty("user.home");

    public static final File SETUP_KEY_FILE = new File(USER_HOME, ".first_time_setup");

    public boolean canSetup() { return SETUP_KEY_FILE.exists(); }

    protected SetupSettings getSettings() {
        return JsonUtil.fromJsonOrDie(toStringOrDie(SETUP_KEY_FILE), SetupSettings.class);
    }

    public String validateFirstTimeSetup(SetupRequest request) {
        if (!canSetup()) throw new SimpleViolationException("{error.setup.alreadySetup}");
        final SetupSettings settings = getSettings();
        if (settings == null) throw new SimpleViolationException("{error.setup.alreadySetup}");
        validateSecrets(request, settings);
        return settings.getBackupKey();
    }

    protected void validateSecrets(SetupRequest request, SetupSettings settings) {
        if (!settings.getSecret().equals(request.getSetupKey())) throw new SimpleViolationException("{error.setup.key.invalid}");

        // compare password...
        if (!BCrypt.checkpw(request.getInitialPassword(), settings.getPasswordHash())) {
            throw new SimpleViolationException("{error.setup.initialPassword.invalid}");
        }
    }

    public void firstTimeSetupCompleted() {
        // truncate the setup file, since the system is now setup
        try {
            FileUtil.truncate(SETUP_KEY_FILE);
        } catch (Exception e) {
            log.error("Error truncating setup file: "+abs(SETUP_KEY_FILE)+": "+e, e);
        }
    }

}
