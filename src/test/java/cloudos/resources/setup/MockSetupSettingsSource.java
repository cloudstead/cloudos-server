package cloudos.resources.setup;

import cloudos.model.support.SetupRequest;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.security.bcrypt.BCryptUtil;
import org.cobbzilla.wizard.validation.SimpleViolationException;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.cobbzilla.wizardtest.RandomUtil.randomEmail;

@Slf4j
public class MockSetupSettingsSource extends SetupSettingsSource {

    public static final String MOCK_BACKUP_KEY = "mock-backup-key";

    @Getter private final String password = randomAlphanumeric(10);
    @Getter private SetupSettings mockSettings
            = new SetupSettings(randomAlphanumeric(10), randomEmail(), BCryptUtil.hash(getPassword()), MOCK_BACKUP_KEY);

    @Override public boolean canSetup() { return mockSettings != null; }

    @Override protected SetupSettings getSettings() { return getMockSettings(); }

    @Override
    public String validateFirstTimeSetup(SetupRequest request) {
        if (!canSetup()) throw new SimpleViolationException("{error.setup.alreadySetup}");
        validateSecrets(request, mockSettings);
        return MOCK_BACKUP_KEY;
    }

    public void firstTimeSetupCompleted() { mockSettings = null; }

}
