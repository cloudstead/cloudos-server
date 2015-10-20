package cloudos.main;

import cloudos.model.auth.CloudOsAuthResponse;
import cloudos.model.auth.LoginRequest;
import cloudos.resources.ApiConstants;
import cloudos.service.task.CloudOsTaskResult;
import com.fasterxml.jackson.databind.InjectableValues;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.wizard.client.ApiClientBase;
import org.cobbzilla.wizard.main.MainApiBase;
import org.cobbzilla.wizard.model.ldap.LdapEntity;
import org.cobbzilla.wizard.task.TaskId;
import org.cobbzilla.wizard.util.RestResponse;
import rooty.toots.vendor.VendorSettingDisplayValue;

import java.util.concurrent.TimeUnit;

import static cloudos.resources.ApiConstants.*;
import static org.cobbzilla.util.json.JsonUtil.fromJson;
import static org.cobbzilla.util.system.Sleep.sleep;

@Slf4j
public abstract class CloudOsMainBase<OPT extends CloudOsMainOptions> extends MainApiBase<OPT> {

    public CloudOsMainBase () {
        JsonUtil.NOTNULL_MAPPER.setInjectableValues(new InjectableValues.Std()
                .addValue(LdapEntity.LDAP_CONTEXT, getOptions().getLdap()));
        JsonUtil.FULL_MAPPER.setInjectableValues(new InjectableValues.Std()
                .addValue(LdapEntity.LDAP_CONTEXT, getOptions().getLdap()));
        JsonUtil.PUBLIC_MAPPER.setInjectableValues(new InjectableValues.Std()
                .addValue(LdapEntity.LDAP_CONTEXT, getOptions().getLdap()));
    }

    @Override protected String getApiHeaderTokenName() { return H_API_KEY; }

    @Override protected Object buildLoginRequest(OPT options) {
        return new LoginRequest().setName(options.getAccount()).setPassword(options.getPassword());
    }

    @Override protected String getLoginUri(String account) { return ACCOUNTS_ENDPOINT; }

    @Override protected String getSessionId(RestResponse response) throws Exception {
        return fromJson(response.json, CloudOsAuthResponse.class).getSessionId();
    }

    @Override protected void setSecondFactor(Object loginRequest, String token) {
        ((LoginRequest) loginRequest).setSecondFactor(token);
    }

    public static final long TIMEOUT = TimeUnit.MINUTES.toMillis(20);
    protected long getTimeout() { return TIMEOUT; }

    protected CloudOsTaskResult awaitTaskResult(TaskId taskId) throws Exception {

        final ApiClientBase api = getApiClient();
        final long start = System.currentTimeMillis();
        final long pollInterval = 1000 * getOptions().getPollInterval();
        final String taskStatusUri = TASKS_ENDPOINT + "/" + taskId.getUuid();

        CloudOsTaskResult result = fromJson(api.get(taskStatusUri).json, CloudOsTaskResult.class);

        while (!result.isComplete() && System.currentTimeMillis() - start < getTimeout()) {
            sleep(pollInterval, "waiting for task (" + taskId + ") to complete");
            final String json = api.get(taskStatusUri).json;
            result = fromJson(json, CloudOsTaskResult.class);
            out(result.toString());
        }

        return result;
    }

    public Boolean getAllowSsh() throws Exception {
        final RestResponse response = getApiClient().doGet(ApiConstants.CONFIGS_ENDPOINT + "/system/allowssh");
        return Boolean.valueOf(fromJson(response.json, VendorSettingDisplayValue.class).getValue());
    }

}
