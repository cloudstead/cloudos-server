package cloudos.resources.setup;

import cloudos.model.support.SetupRequest;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.cobbzilla.util.security.bcrypt.BCryptUtil;
import org.cobbzilla.wizard.validation.SimpleViolationException;
import org.cobbzilla.wizardtest.RandomUtil;

@Slf4j
public class MockSetupSettingsSource extends SetupSettingsSource {

    @Getter private final String password = RandomStringUtils.randomAlphanumeric(10);
    @Getter private SetupSettings mockSettings = new SetupSettings(RandomStringUtils.randomAlphanumeric(10), RandomUtil.randomEmail(), BCryptUtil.hash(getPassword()));

    @Override protected SetupSettings getSettings() { return getMockSettings(); }

    @Override
    public void validateFirstTimeSetup(SetupRequest request) {
        if (mockSettings == null) throw new SimpleViolationException("{error.setup.alreadySetup}");
        validateSecrets(request, mockSettings);
    }

    public void firstTimeSetupCompleted() { mockSettings = null; }

}
