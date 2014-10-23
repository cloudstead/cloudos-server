package cloudos.main;

import cloudos.model.auth.AuthResponse;
import cloudos.model.auth.CloudOsAuthResponse;
import cloudos.model.auth.LoginRequest;
import cloudos.model.support.AppInstallUrlRequest;
import cloudos.service.task.TaskId;
import cloudos.service.task.TaskResult;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.client.ApiClientBase;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import static cloudos.resources.ApiConstants.*;
import static org.cobbzilla.util.json.JsonUtil.fromJson;
import static org.cobbzilla.util.json.JsonUtil.toJson;

@Slf4j
public class InstallAppMain {

    private final InstallAppOptions options = new InstallAppOptions();
    private final CmdLineParser parser = new CmdLineParser(options);

    @Getter private String[] args;
    public void setArgs(String[] args) throws CmdLineException {
        parser.parseArgument(args);
    }


    public static void main (String[] args) throws Exception {
        InstallAppMain m = new InstallAppMain();
        m.setArgs(args);
        m.install();
    }

    public void install () throws Exception {

        final ApiClientBase api = new ApiClientBase(options.getApiBase()) {
            @Override protected String getTokenHeader() { return H_API_KEY; }
        };

        // login
        log.info("logging in "+options.getAccount()+" ...");
        final LoginRequest loginRequest = new LoginRequest().setName(options.getAccount()).setPassword(options.getPassword());
        final AuthResponse authResponse = fromJson(api.post(ACCOUNTS_ENDPOINT, toJson(loginRequest)).json, CloudOsAuthResponse.class);
        api.pushToken(authResponse.getSessionId());

        // install app
        log.info("installing app from "+options.getUrl()+" ...");
        final AppInstallUrlRequest request = new AppInstallUrlRequest().setUrl(options.getUrl());
        final TaskId taskId = fromJson(api.post(APPS_ENDPOINT, toJson(request)).json, TaskId.class);

        // monitor status
        TaskResult taskResult;
        while (true) {
            Thread.sleep(options.getPollInterval());
            log.info("checking progress of installation...");
            taskResult = fromJson(api.get(TASKS_ENDPOINT + "/" + taskId.getUuid()).json, TaskResult.class);
            if (taskResult.isSuccess()) break;
            log.info(toJson(taskResult));
        }

        log.info("installation successful");
    }


}
