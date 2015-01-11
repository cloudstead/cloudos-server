package cloudos.resources;

import cloudos.model.support.RestoreRequest;
import cloudos.resources.setup.MockSetupSettingsSource;
import cloudos.service.task.TaskId;
import org.cobbzilla.wizard.util.RestResponse;
import org.junit.Test;

import static org.cobbzilla.util.json.JsonUtil.fromJson;
import static org.cobbzilla.util.json.JsonUtil.toJson;
import static org.cobbzilla.wizardtest.RandomUtil.randomEmail;
import static org.junit.Assert.assertEquals;

public class SetupResourceTest extends ApiClientTestBase {

    private static final String DOC_TARGET = "First-time Setup and Restore from Backup";

    @Override protected boolean skipAdminCreation() { return true; }

    @Test
    public void testFirstTimeSetup () throws Exception {
        apiDocs.startRecording(DOC_TARGET, "First-time setup of a new cloudstead");
        apiDocs.addNote("Send setup request, creates initial admin account");
        doFirstTimeSetup();
    }

    @Test
    public void testRestoreFromBackup () throws Exception {
        apiDocs.startRecording(DOC_TARGET, "Restore a cloudstead from a backup");
        final MockSetupSettingsSource setupSource  = server.getApplicationContext().getBean(MockSetupSettingsSource.class);

        // Build restore request
        final RestoreRequest request = new RestoreRequest()
                .setRestoreKey(MockSetupSettingsSource.MOCK_BACKUP_KEY)
                .setNotifyEmail(randomEmail())
                .setSetupKey(setupSource.getMockSettings().getSecret())
                .setInitialPassword(setupSource.getPassword());

        apiDocs.addNote("Send restore request");
        final RestResponse response = post(ApiConstants.SETUP_ENDPOINT+"/restore", toJson(request));
        assertEquals(200, response.status);

        final TaskId taskId = fromJson(response.json, TaskId.class);

        // todo: check on status of restore?
    }
}
