package cloudos.main;

import cloudos.model.auth.AuthResponse;
import cloudos.model.auth.CloudOsAuthResponse;
import cloudos.model.auth.LoginRequest;
import cloudos.service.task.TaskId;
import cloudos.service.task.TaskResult;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.client.ApiClientBase;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import java.util.concurrent.TimeUnit;

import static cloudos.resources.ApiConstants.*;
import static org.cobbzilla.util.json.JsonUtil.fromJson;
import static org.cobbzilla.util.json.JsonUtil.toJson;
import static org.cobbzilla.util.system.Sleep.sleep;

@Slf4j
public abstract class CloudOsMainBase<OPT extends CloudOsMainOptions> {

    public static final long TIMEOUT = TimeUnit.MINUTES.toMillis(20);

    @Getter private final OPT options = initOptions();
    protected abstract OPT initOptions();

    private final CmdLineParser parser = new CmdLineParser(getOptions());

    @Getter private String[] args;
    public void setArgs(String[] args) throws CmdLineException {
        this.args = args;
        parser.parseArgument(args);
    }

    @Getter(value=AccessLevel.PROTECTED, lazy=true) private final ApiClientBase apiClient = initApiClient();

    private ApiClientBase initApiClient() {
        return new ApiClientBase(getOptions().getApiBase()) {
            @Override protected String getTokenHeader() { return H_API_KEY; }
        };
    }

    protected static void main(Class<? extends CloudOsMainBase> clazz, String[] args) {
        try {
            CloudOsMainBase m = clazz.newInstance();
            m.setArgs(args);
            if (m.getOptions().requireAccount()) m.login();
            m.run();
        } catch (Exception e) {
            log.error("Unexpected error: "+e, e);
        }
    }

    protected abstract void run() throws Exception;

    protected void login () {
        log.info("logging in "+options.getAccount()+" ...");
        try {
            final LoginRequest loginRequest = new LoginRequest().setName(options.getAccount()).setPassword(options.getPassword());
            final ApiClientBase api = getApiClient();
            final AuthResponse authResponse = fromJson(api.post(ACCOUNTS_ENDPOINT, toJson(loginRequest)).json, CloudOsAuthResponse.class);
            api.pushToken(authResponse.getSessionId());
        } catch (Exception e) {
            throw new IllegalStateException("Error logging in: "+e, e);
        }
    }

    protected TaskResult awaitTaskResult(TaskId taskId) throws Exception {

        final ApiClientBase api = getApiClient();
        final long start = System.currentTimeMillis();
        final long pollInterval = 1000 * getOptions().getPollInterval();
        final String taskStatusUri = TASKS_ENDPOINT + "/" + taskId.getUuid();

        TaskResult result = fromJson(api.get(taskStatusUri).json, TaskResult.class);

        while (!result.isComplete() && System.currentTimeMillis() - start < TIMEOUT) {
            sleep(pollInterval, "waiting for task ("+taskId+") to complete");
            final String json = api.get(taskStatusUri).json;
            result = fromJson(json, TaskResult.class);
        }

        return result;
    }

}
