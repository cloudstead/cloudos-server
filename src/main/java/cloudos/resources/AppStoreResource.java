package cloudos.resources;

import cloudos.appstore.client.AppStoreApiClient;
import cloudos.appstore.model.support.AppInstallStatus;
import cloudos.appstore.model.support.AppListing;
import cloudos.appstore.model.support.AppStoreQuery;
import cloudos.dao.AppDAO;
import cloudos.dao.SessionDAO;
import cloudos.model.Account;
import cloudos.model.app.CloudOsApp;
import cloudos.server.CloudOsConfiguration;
import com.qmino.miredot.annotations.ReturnType;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.dao.SearchResults;
import org.cobbzilla.wizard.model.SemanticVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static cloudos.resources.ApiConstants.H_API_KEY;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.wizard.resources.ResourceUtil.*;

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
    public Response queryAppStore (@HeaderParam(H_API_KEY) String apiKey, AppStoreQuery query) {

        final Account admin = sessionDAO.find(apiKey);
        if (admin == null) return notFound(apiKey);

        final AppStoreApiClient client = configuration.getAppStoreClient();
        final SearchResults<AppListing> results;

        try {
            results = client.searchAppStore(query);
        } catch (Exception e) {
            log.error("Error searching app store: "+e, e);
            return serverError();
        }

        if (empty(results) || !results.hasResults()) return notFound();

        updateWithLocalInfo(results);

        return ok(results);
    }

    protected void updateWithLocalInfo(SearchResults<AppListing> results) {
        for (AppListing listing : results.getResults()) {
            updateWithLocalInfo(listing);
        }
    }

    private AppListing updateWithLocalInfo(AppListing listing) {
        CloudOsApp found = appDAO.findInstalledByName(listing.getName());
        final SemanticVersion semanticVersion = listing.getSemanticVersion();
        if (found != null) {
            if (semanticVersion.compareTo(found.getMetadata().getSemanticVersion()) > 0) {
                listing.setInstallStatus(AppInstallStatus.upgrade_available_installed);
            } else {
                listing.setInstallStatus(AppInstallStatus.installed);
            }
        } else {
            found = appDAO.findLatestVersionByName(listing.getName());
            if (found != null) {
                if (semanticVersion.compareTo(found.getManifest().getSemanticVersion()) > 0) {
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
        return listing;
    }

    /**
     * View details for an app
     * @param apiKey The session ID
     * @param publisher The name of the publisher
     * @param app The name of the app
     * @return The CloudApp object if found
     */
    @GET
    @Path("/{publisher}/{app}")
    @ReturnType("cloudos.appstore.model.support.AppListing")
    public Response viewAppDetails (@HeaderParam(H_API_KEY) String apiKey,
                                    @PathParam("publisher") String publisher,
                                    @PathParam("app") String app) {

        final Account admin = sessionDAO.find(apiKey);
        if (admin == null) return notFound(apiKey);

        final AppStoreApiClient client = configuration.getAppStoreClient();
        final AppListing appListing;
        try {
            appListing = updateWithLocalInfo(client.findAppListing(publisher, app));
        } catch (Exception e) {
            log.error("viewAppDetails: client.findAppListing API call failed: "+e, e);
            return serverError();
        }

        return ok(appListing);
    }

    /**
     * Find details about a particular app version
     * @param apiKey The session ID
     * @param publisher The name of the publisher
     * @param app The name of the app
     * @param version the version of the app
     * @return a single AppListing, will also include the "availableVersions" field
     */
    @GET
    @Path("/{publisher}/{app}/{version}")
    @ReturnType("cloudos.appstore.model.support.AppListing")
    public Response viewAppDetails (@HeaderParam(H_API_KEY) String apiKey,
                                    @PathParam("publisher") String publisher,
                                    @PathParam("app") String app,
                                    @PathParam("version") String version) {

        final Account admin = sessionDAO.find(apiKey);
        if (admin == null) return notFound(apiKey);

        final AppStoreApiClient client = configuration.getAppStoreClient();
        final AppListing appListing;
        try {
            appListing = updateWithLocalInfo(client.findAppListing(publisher, app, version));
        } catch (Exception e) {
            log.error("viewAppDetails: client.findAppListing API call failed: "+e, e);
            return serverError();
        }

        return ok(appListing);
    }

}
