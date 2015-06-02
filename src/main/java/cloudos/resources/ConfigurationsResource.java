package cloudos.resources;

import cloudos.appstore.model.app.AppConfigDef;
import cloudos.appstore.model.app.AppManifest;
import cloudos.dao.AppDAO;
import cloudos.dao.SessionDAO;
import cloudos.dao.SslCertificateDAO;
import cloudos.model.Account;
import cloudos.model.app.CloudOsApp;
import cloudos.model.support.SslCertificateRequest;
import cloudos.model.support.UnlockRequest;
import cloudos.server.CloudOsConfiguration;
import cloudos.service.RootyService;
import com.qmino.miredot.annotations.ReturnType;
import edu.emory.mathcs.backport.java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.http.HttpStatusCodes;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.wizard.resources.ResourceUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rooty.RootyMessage;
import rooty.toots.service.ServiceKeyRequest;
import rooty.toots.vendor.VendorSettingDisplayValue;
import rooty.toots.vendor.VendorSettingHandler;
import rooty.toots.vendor.VendorSettingUpdateRequest;
import rooty.toots.vendor.VendorSettingsListRequest;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static cloudos.resources.ApiConstants.H_API_KEY;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.wizard.resources.ResourceUtil.*;
import static rooty.toots.service.ServiceKeyRequest.Operation.ALLOW_SSH;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path(ApiConstants.CONFIGS_ENDPOINT)
@Service @Slf4j
public class ConfigurationsResource {

    public static final String SYSTEM_APP = "system";
    public static final long ROOTY_TIMEOUT = TimeUnit.SECONDS.toMillis(30);

    @Autowired private CloudOsConfiguration configuration;
    @Autowired private AppDAO appDAO;
    @Autowired private SessionDAO sessionDAO;
    @Autowired private SslCertificateDAO certificateDAO;
    @Autowired private RootyService rooty;

    /**
     * Get all configuration groups. Must be admin
     * @param apiKey The session ID
     * @return a Set of Strings, each representing a configuration group
     * @statuscode 403 if caller is not admin
     */
    @GET
    @ReturnType("java.util.List<java.lang.String>")
    public Response getConfigurations (@HeaderParam(H_API_KEY) String apiKey) {

        final Account admin = sessionDAO.find(apiKey);
        if (admin == null) return notFound(apiKey);
        if (!admin.isAdmin()) return forbidden();

        final Set<String> configs = new HashSet<>();
        try {
            configs.addAll(Arrays.asList(getAllConfigurations()));
            configs.add(SYSTEM_APP);
            return ok(configs);

        } catch (Exception e) {
            log.error("Error getting configurations: "+e, e);
            return serverError();
        }
    }

    private String[] getAllConfigurations() throws Exception {
        final RootyMessage request = rooty.request(new VendorSettingsListRequest(), ROOTY_TIMEOUT);
        return JsonUtil.fromJson(request.getResults(), String[].class);
    }

    /**
     * Get all configuration options for a configuration group. Must be admin
     * @param apiKey The session ID
     * @param app Name of the configuration group (usually the name of the CloudOs app, or the special 'system' category)
     * @return an array of VendorSettingDisplayValues
     * @statuscode 403 if caller is not admin
     */
    @GET
    @Path("/{app}")
    @ReturnType("java.util.List<rooty.toots.vendor.VendorSettingDisplayValue>")
    public Response getConfigurationOptions (@HeaderParam(H_API_KEY) String apiKey,
                                             @PathParam("app") String app) {

        final Account admin = sessionDAO.find(apiKey);
        if (admin == null) return notFound(apiKey);
        if (!admin.isAdmin()) return forbidden();

        if (empty(app) || app.equals("undefined") || app.equals(SYSTEM_APP)) {
            return ok(getSystemOptions());
        }

        try {
            return ok(getConfiguration(app));
        } catch (Exception e) {
            log.error("Error getting options for app "+app+": "+e, e);
            return serverError();
        }
    }

    private VendorSettingDisplayValue[] getSystemOptions() {
        return new VendorSettingDisplayValue[] {
                new VendorSettingDisplayValue("mxrecord", "mx."+configuration.getHostname(), true),
                new VendorSettingDisplayValue("allowssh", getAllowSsh().toString(), true)
        };
    }

    private Boolean getAllowSsh() {
        return Boolean.valueOf(rooty.request(new ServiceKeyRequest(ALLOW_SSH), ROOTY_TIMEOUT).getResults());
    }

    private VendorSettingDisplayValue[] getConfiguration(String app) throws Exception {
        final CloudOsApp installedApp = appDAO.findInstalledByName(app);
        List<String> fields = null;
        if (installedApp != null) {
            final AppManifest appManifest = installedApp.getManifest();
            fields = toFieldList(appManifest);
        }
        final RootyMessage request = rooty.request(new VendorSettingsListRequest().setCookbook(app).setFields(fields), ROOTY_TIMEOUT);
        return JsonUtil.fromJson(request.getResults(), VendorSettingDisplayValue[].class);
    }

    private List<String> toFieldList(AppManifest appManifest) {
        final List<String> fields = new ArrayList<>();
        final AppConfigDef[] databags = appManifest.getConfig();
        if (databags != null && databags.length > 0) {
            for (AppConfigDef def : databags) {
                for (String item : def.getItems()) {
                    fields.add(def.getName() + "/" + item);
                }
            }
        }
        return fields;
    }

    /**
     * Get the value for a single configuration option
     * @param apiKey The session ID
     * @param app Name of the configuration group (usually the name of the CloudOs app, or the special 'system' category)
     * @param category name of the configuration category
     * @param option name of the configuration option
     * @return the VendorSettingDisplayValue
     * @statuscode 403 if caller is not admin
     * @statuscode 404 if the configuration option was not found
     */
    @GET
    @Path("/{app}/{category}/{option}")
    @ReturnType("rooty.toots.vendor.VendorSettingDisplayValue")
    public Response getConfigurationOption (@HeaderParam(H_API_KEY) String apiKey,
                                            @PathParam("app") String app,
                                            @PathParam("category") String category,
                                            @PathParam("option") String option) {

        final Account admin = sessionDAO.find(apiKey);
        if (admin == null) return notFound(apiKey);
        if (!admin.isAdmin()) return forbidden();

        final VendorSettingDisplayValue value;
        if (app.equals(SYSTEM_APP)) {
            value = getOption(option, getSystemOptions());
        } else {
            try {
                value = getOption(category+"/"+option, getConfiguration(app));
            } catch (Exception e) {
                log.error("Error reading config: " + e, e);
                return serverError();
            }
        }
        return value == null ? notFound(option) : ok(value);
    }

    private VendorSettingDisplayValue getOption(String name, VendorSettingDisplayValue[] values) {
        for (VendorSettingDisplayValue v : values) {
            if (v.getPath().equals(name)) return v;
        }
        return null;
    }

    /**
     * Set a single configuration option
     * @param apiKey The session ID
     * @param app Name of the configuration group (usually the name of the CloudOs app, or the special 'system' category)
     * @param category name of the configuration category
     * @param option name of the configuration option
     * @param value the value to set
     * @return "true" if the settings was written successfully, "false" if it was not written
     * @statuscode 500 if an error occurred writing the option
     */
    @POST
    @Path("/{app}/{category}/{option}")
    @ReturnType("java.lang.Boolean")
    public Response setConfigurationOption (@HeaderParam(H_API_KEY) String apiKey,
                                            @PathParam("app") String app,
                                            @PathParam("category") String category,
                                            @PathParam("option") String option,
                                            String value) {

        final Account admin = sessionDAO.find(apiKey);
        if (admin == null) return notFound(apiKey);
        if (!admin.isAdmin()) return forbidden();

        if (value.equals(VendorSettingHandler.VENDOR_DEFAULT)) return ResourceUtil.invalid("{err.setConfig.invalidValue}");

        try {
            return ok(Boolean.valueOf(updateConfig(app, category+"/"+option, value).getResults()));

        } catch (Exception e) {
            log.error("Error handling setConfig ("+ app +"): "+e, e);
            return serverError();
        }
    }

    public RootyMessage updateConfig(String app, String option, String value) {
        return rooty.request(new VendorSettingUpdateRequest(option, value).setCookbook(app), ROOTY_TIMEOUT);
    }

    /**
     * Unlock the cloudstead
     * @param apiKey The session ID
     * @param unlockRequest The unlock request
     * @return HTTP status 200 if the unlock succeeded, or 422 if the cloudstead remains locked
     */
    @PUT
    @Path("/unlock")
    @ReturnType("java.lang.Void")
    public Response unlockCloudstead(@HeaderParam(H_API_KEY) String apiKey, UnlockRequest unlockRequest) {

        final Account admin = sessionDAO.find(apiKey);
        if (admin == null) return notFound(apiKey);
        if (!admin.isAdmin()) return forbidden();

        final SslCertificateRequest cert = unlockRequest.getCert();
        final Response certResponse = SslCertificatesResource.addOrOverwriteCert(certificateDAO, rooty, cert.getName(), cert);
        if (certResponse.getStatus() != HttpStatusCodes.OK) return certResponse;

        for (Map.Entry<String, String> setting : unlockRequest.getSettings().entrySet()) {
            final String name = setting.getKey();
            try {
                final RootyMessage response = updateConfig("cloudos", name, setting.getValue());
                if (!response.getBooleanResult()) {
                    log.warn("unlockCloudstead: updateConfig returned false");
                    return ResourceUtil.invalid("{err.unlock.stillLocked}");
                }

            } catch (Exception e) {
                log.error("Error handling setConfig ("+name+"): "+e, e);
                return serverError();
            }
        }

        return getAllowSsh() ? ok() : ResourceUtil.invalid("{err.unlock.stillLocked}");
    }

}
