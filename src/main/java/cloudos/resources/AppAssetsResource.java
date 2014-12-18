package cloudos.resources;

import cloudos.appstore.model.app.AppLayout;
import cloudos.server.CloudOsConfiguration;
import com.qmino.miredot.annotations.ReturnType;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.cobbzilla.wizard.resources.ResourceUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.*;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.*;
import java.net.URLConnection;

@Consumes(MediaType.WILDCARD)
@Produces(MediaType.WILDCARD)
@Path(ApiConstants.APP_ASSETS_ENDPOINT)
@Service @Slf4j
public class AppAssetsResource {

    @Autowired private CloudOsConfiguration configuration;

    /**
     * Get an application asset.
     * @param app The app name
     * @param asset The name of the asset.
     * @return The asset data along with an appropriate Content-Type header
     */
    @GET
    @Path("{app}/{asset}")
    @ReturnType("javax.ws.rs.core.StreamingOutput")
    @Produces(MediaType.WILDCARD)
    public Response getAsset (@PathParam("app") String app,
                              @PathParam("asset") String asset) {

        final AppLayout layout = configuration.getAppLayout(app);
        if (!layout.getVersionDir().exists()) return ResourceUtil.notFound(asset);

        final File assetFile = layout.findDefaultAsset(asset);
        if (assetFile == null || !assetFile.exists()) return ResourceUtil.notFound(asset);

        return Response.ok(new StreamingOutput() {
            @Override public void write(OutputStream out) throws IOException, WebApplicationException {
                @Cleanup final InputStream in = new FileInputStream(assetFile);
                IOUtils.copy(in, out);
            }
        })
                .header(HttpHeaders.CONTENT_TYPE, URLConnection.guessContentTypeFromName(assetFile.getName()))
                .header(HttpHeaders.CONTENT_LENGTH, assetFile.length())
                .build();
    }

}
