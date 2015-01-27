package cloudos.resources;

import cloudos.dao.SessionDAO;
import cloudos.dao.SslCertificateDAO;
import cloudos.model.Account;
import cloudos.model.SslCertificate;
import cloudos.model.support.SslCertificateRequest;
import cloudos.service.RootyService;
import com.qmino.miredot.annotations.ReturnType;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.openssl.PEMParser;
import org.cobbzilla.util.string.StringUtil;
import org.cobbzilla.wizard.resources.ResourceUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rooty.toots.ssl.InstallSslCertData;
import rooty.toots.ssl.InstallSslCertMessage;
import rooty.toots.ssl.RemoveSslCertMessage;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.StringReader;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path(ApiConstants.EP_CERTS)
@Service @Slf4j
public class SslCertificatesResource {

    public static final long SSL_CERT_TIMEOUT = TimeUnit.SECONDS.toMillis(60);

    public static final String CN_PREFIX = "CN=";

    @Autowired private SslCertificateDAO certificateDAO;
    @Autowired private SessionDAO sessionDAO;
    @Autowired private RootyService rooty;

    /**
     * Find all SSL certificates. Must be admin
     * @param apiKey The session ID
     * @return a List of SslCertificates
     * @statuscode 403 if caller is not an admin
     */
    @GET
    public Response findCertificates (@HeaderParam(ApiConstants.H_API_KEY) String apiKey) {

        final Account admin = sessionDAO.find(apiKey);
        if (admin == null) return ResourceUtil.notFound(apiKey);
        if (!admin.isAdmin()) return ResourceUtil.forbidden();

        final List<SslCertificate> certs = certificateDAO.findAll();
        return Response.ok(certs).build();
    }

    /**
     * Find a single SSL certificate. Must be admin
     * @param apiKey The session ID
     * @param name The name of the certificate
     * @return an SslCertificates
     * @statuscode 403 if caller is not an admin
     * @statuscode 404 no certificate with that name
     */
    @GET
    @Path("/{name}")
    @ReturnType("cloudos.model.SslCertificate")
    public Response findCertificate (@HeaderParam(ApiConstants.H_API_KEY) String apiKey,
                                     @PathParam("name") String name) {

        final Account admin = sessionDAO.find(apiKey);
        if (admin == null) return ResourceUtil.notFound(apiKey);
        if (!admin.isAdmin()) return ResourceUtil.forbidden();

        SslCertificate found = certificateDAO.findByName(name);
        if (found == null) return ResourceUtil.notFound(name);

        return Response.ok(found).build();
    }

    /**
     * Create or over-write an SSL certificate
     * @param apiKey The session ID
     * @param name The name of the certificate
     * @param request
     * @return The Certificate that was added/updated
     * @statuscode 403 if caller is not an admin
     */
    @POST
    @Path("/{name}")
    @ReturnType("cloudos.model.SslCertificate")
    public Response addOrOverwriteCert(@HeaderParam(ApiConstants.H_API_KEY) String apiKey,
                                       @PathParam("name") String name,
                                       SslCertificateRequest request) {

        final Account admin = sessionDAO.find(apiKey);
        if (admin == null) return ResourceUtil.notFound(apiKey);
        if (!admin.isAdmin()) return ResourceUtil.forbidden();

        if (!name.equals(request.getName())) return ResourceUtil.invalid();

        return addOrOverwriteCert(certificateDAO, rooty, name, request);
    }

    public static Response addOrOverwriteCert(SslCertificateDAO certificateDAO, RootyService rooty,
                                              String name, SslCertificateRequest request) {

        final SslCertificate found = certificateDAO.findByName(name);
        if (found != null) {
            log.warn("certificate will be overwritten: "+name);
        }

        // validate PEM
        String commonName = "";
        final PEMParser pemParser = new PEMParser(new StringReader(request.getPem()));
        try {
            Object thing;
            while ((thing = pemParser.readObject()) != null) {
                if (thing instanceof X509CertificateHolder) {
                    final String subject = ((X509CertificateHolder) thing).getSubject().toString();
                    if (subject != null && subject.startsWith(CN_PREFIX) && subject.length() > CN_PREFIX.length()) {
                        commonName = subject.substring(CN_PREFIX.length());
                    }
                }
            }

        } catch (Exception e) {
            log.warn("Error parsing PEM: "+e);
            return Response.serverError().build();
        }
        if (StringUtil.empty(commonName)) {
            log.warn("{err.cert.pem.invalid}", "The PEM data did not contain a valid Common Name");
        }

        final SslCertificate cert = (SslCertificate) new SslCertificate()
                .setCommonName(commonName)
                .setDescription(request.getDescription())
                .setPem(request.getPem())
                .setKey(request.getKey())
                .setName(name);
        final SslCertificate dbCert;
        if (found == null) {
            dbCert = certificateDAO.create(cert);
        } else {
            cert.setUuid(found.getUuid());
            dbCert = certificateDAO.update(cert);
        }

        final InstallSslCertData data = new InstallSslCertData().setKey(request.getKey()).setPem(request.getPem());
        rooty.request(new InstallSslCertMessage(data).setName(name), SSL_CERT_TIMEOUT);

        return Response.ok(dbCert).build();
    }

    /**
     * Delete an SSL certificate
     * @param apiKey The session ID
     * @param name The name of the certificate
     * @return Just an HTTP status code
     * @statuscode 403 if caller is not an admin
     */
    @DELETE
    @Path("/{name}")
    @ReturnType("java.lang.Void")
    public Response removeCertificate (@HeaderParam(ApiConstants.H_API_KEY) String apiKey,
                                       @PathParam("name") String name) {

        final Account admin = sessionDAO.find(apiKey);
        if (admin == null) return ResourceUtil.notFound(apiKey);
        if (!admin.isAdmin()) return ResourceUtil.forbidden();

        final SslCertificate found = certificateDAO.findByName(name);
        if (found == null) return ResourceUtil.notFound(name);

        rooty.getSender().write(new RemoveSslCertMessage(name));

        certificateDAO.delete(found.getUuid());
        return Response.ok().build();
    }
}
