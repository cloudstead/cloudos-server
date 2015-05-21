package cloudos.resources;

import cloudos.appstore.client.AppStoreApiClient;
import cloudos.appstore.model.PublishedApp;
import cloudos.appstore.model.support.AppInstallStatus;
import cloudos.appstore.model.support.AppListing;
import cloudos.dao.AppDAO;
import cloudos.dao.SessionDAO;
import cloudos.model.Account;
import cloudos.model.app.CloudOsApp;
import cloudos.server.CloudOsConfiguration;
import com.qmino.miredot.annotations.ReturnType;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.dao.SearchResults;
import org.cobbzilla.wizard.model.ResultPage;
import org.cobbzilla.wizard.resources.ResourceUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static cloudos.resources.ApiConstants.H_API_KEY;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path(ApiConstants.APPSTORE_ENDPOINT)
@Service @Slf4j
public class AppStoreResource {

    @Autowired private SessionDAO sessionDAO;
    @Autowired private AppDAO appDAO;
    @Autowired private CloudOsConfiguration configuration;

    /**
     * Search the app store
     * @param apiKey The session ID
     * @param query The app store query
     * @return the search results
     */
    @POST
    @ReturnType("org.cobbzilla.wizard.dao.SearchResults<cloudos.appstore.model.support.AppListing>")
    public Response queryAppStore (@HeaderParam(H_API_KEY) String apiKey, ResultPage query) {

        final Account admin = sessionDAO.find(apiKey);
        if (admin == null) return ResourceUtil.notFound(apiKey);

        final AppStoreApiClient client = configuration.getAppStoreClient();
        final SearchResults<AppListing> results;

        try {
            results = client.searchAppStore(query);

        } catch (Exception e) {
            log.error("Error searching app store: "+e, e);
            return Response.serverError().build();
        }

        for (AppListing listing : results.getResults()) {
            final PublishedApp app = listing.getApp();
            CloudOsApp found = appDAO.findInstalledByName(app.getAppName());
            if (found != null) {
                if (app.getSemanticVersion().compareTo(found.getMetadata().getSemanticVersion()) > 0) {
                    listing.setInstallStatus(AppInstallStatus.upgrade_available_installed);
                } else {
                    listing.setInstallStatus(AppInstallStatus.installed);
                }
            } else {
                found = appDAO.findLatestVersionByName(app.getAppName());
                if (found != null) {
                    if (app.getSemanticVersion().compareTo(found.getManifest().getSemanticVersion()) > 0) {
                        listing.setInstallStatus(AppInstallStatus.upgrade_available_not_installed);
                    } else {
                        listing.setInstallStatus(AppInstallStatus.available_local);
                    }
                } else {
                    // default -- app has not been downloaded to local app repo
                    listing.setInstallStatus(AppInstallStatus.available_appstore);
                }
            }
            // todo: check for apps that are actively installing, set status=installing
        }

        return Response.ok(results).build();
    }

}
