package cloudos.resources;

import cloudos.dao.SessionDAO;
import cloudos.dao.SslCertificateDAO;
import cloudos.model.Account;
import cloudos.model.support.SslCertificateRequest;
import cloudos.model.support.UnlockRequest;
import cloudos.server.CloudOsConfiguration;
import cloudos.service.RootyService;
import edu.emory.mathcs.backport.java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.http.HttpStatusCodes;
import org.cobbzilla.util.json.JsonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rooty.RootyMessage;
import rooty.toots.service.ServiceKeyRequest;
import rooty.toots.vendor.VendorSettingDisplayValue;
import rooty.toots.vendor.VendorSettingUpdateRequest;
import rooty.toots.vendor.VendorSettingsListRequest;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static cloudos.resources.ApiConstants.H_API_KEY;
import static org.cobbzilla.wizard.resources.ResourceUtil.forbidden;
import static org.cobbzilla.wizard.resources.ResourceUtil.notFound;
import static rooty.toots.service.ServiceKeyRequest.Operation.ALLOW_SSH;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path(ApiConstants.CONFIGS_ENDPOINT)
@Service @Slf4j
public class ConfigurationsResource {

    public static final String SYSTEM_APP = "system";

    @Autowired private CloudOsConfiguration configuration;
    @Autowired private SessionDAO sessionDAO;
    @Autowired private SslCertificateDAO certificateDAO;
    @Autowired private RootyService rooty;

    @GET
    public Response getConfigurations (@HeaderParam(H_API_KEY) String apiKey) {

        final Account admin = sessionDAO.find(apiKey);
        if (admin == null) return notFound(apiKey);
        if (!admin.isAdmin()) return forbidden();

        final Set<String> configs = new HashSet<>();
        try {
            configs.addAll(Arrays.asList(getAllConfigurations()));
            configs.add(SYSTEM_APP);
            return Response.ok(configs).build();

        } catch (Exception e) {
            log.error("Error getting configurations: "+e, e);
            return Response.serverError().build();
        }
    }

    private String[] getAllConfigurations() throws Exception {
        return JsonUtil.fromJson(rooty.request(new VendorSettingsListRequest()).getResults(), String[].class);
    }

    @GET
    @Path("/{app}")
    public Response getConfigurationOptions (@HeaderParam(H_API_KEY) String apiKey,
                                             @PathParam("app") String app) {

        final Account admin = sessionDAO.find(apiKey);
        if (admin == null) return notFound(apiKey);
        if (!admin.isAdmin()) return forbidden();

        if (app.equals(SYSTEM_APP)) {
            return Response.ok(getSystemOptions()).build();
        }

        try {
            return Response.ok(getConfiguration(app)).build();
        } catch (Exception e) {
            log.error("Error getting options for app "+app+": "+e, e);
            return Response.serverError().build();
        }
    }

    private VendorSettingDisplayValue[] getSystemOptions() {
        return new VendorSettingDisplayValue[] {
                new VendorSettingDisplayValue("mxrecord", "mx."+configuration.getHostname()),
                new VendorSettingDisplayValue("allowssh", Boolean.valueOf(rooty.request(new ServiceKeyRequest(ALLOW_SSH)).getResults()).toString())
        };
    }

    private VendorSettingDisplayValue[] getConfiguration(String app) throws Exception {
        final RootyMessage request = rooty.request(new VendorSettingsListRequest().setCookbook(app));
        return JsonUtil.fromJson(request.getResults(), VendorSettingDisplayValue[].class);
    }

    @GET
    @Path("/{app}/{option}")
    public Response getConfigurationOption (@HeaderParam(H_API_KEY) String apiKey,
                                            @PathParam("app") String app,
                                            @PathParam("option") String option) {

        final Account admin = sessionDAO.find(apiKey);
        if (admin == null) return notFound(apiKey);
        if (!admin.isAdmin()) return forbidden();

        final VendorSettingDisplayValue value;
        if (app.equals(SYSTEM_APP)) {
            value = getOption(option, getSystemOptions());
        } else {
            try {
                value = getOption(option, getConfiguration(app));
            } catch (Exception e) {
                log.error("Error reading config: " + e, e);
                return Response.serverError().build();
            }
        }
        return value == null ? notFound(option) : Response.ok(value).build();
    }

    private VendorSettingDisplayValue getOption(String name, VendorSettingDisplayValue[] values) {
        for (VendorSettingDisplayValue v : values) {
            if (v.getPath().equals(name)) return v;
        }
        return null;
    }

    @POST
    @Path("/{app}/{option}")
    public Response setConfigurationOption (@HeaderParam(H_API_KEY) String apiKey,
                                            @PathParam("app") String app,
                                            @PathParam("option") String option,
                                            String value) {

        final Account admin = sessionDAO.find(apiKey);
        if (admin == null) return notFound(apiKey);
        if (!admin.isAdmin()) return forbidden();

        try {
            return Response.ok(Boolean.valueOf(updateConfig(app, option, value).getResults())).build();

        } catch (Exception e) {
            log.error("Error handling setConfig ("+ app +"): "+e, e);
            return Response.serverError().build();
        }
    }

    public RootyMessage updateConfig(String app, String option, String value) {
        return rooty.request(new VendorSettingUpdateRequest(option, value).setCookbook(app));
    }

    @PUT
    @Path("/unlock")
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
                updateConfig("cloudos", name, setting.getValue());
            } catch (Exception e) {
                log.error("Error handling setConfig ("+name+"): "+e, e);
                return Response.serverError().build();
            }
        }

        return Response.ok().build();
    }

}
