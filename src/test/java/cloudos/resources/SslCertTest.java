package cloudos.resources;

import cloudos.model.SslCertificate;
import cloudos.model.support.SslCertificateRequest;
import cloudos.service.MockRootySender;
import org.apache.commons.lang3.RandomStringUtils;
import org.cobbzilla.util.http.HttpStatusCodes;
import org.cobbzilla.util.security.ShaUtil;
import org.cobbzilla.wizard.util.RestResponse;
import org.junit.Test;
import rooty.RootyMessage;
import rooty.toots.ssl.InstallSslCertData;
import rooty.toots.ssl.InstallSslCertMessage;

import java.util.List;

import static cloudos.resources.ApiConstants.CERTS_ENDPOINT;
import static org.cobbzilla.util.json.JsonUtil.fromJson;
import static org.cobbzilla.util.json.JsonUtil.toJson;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SslCertTest extends ApiClientTestBase {

    public static final String DOC_TARGET = "SSL Certificate Management";

    @Test
    public void testInstallSslCert () throws Exception {
        apiDocs.startRecording(DOC_TARGET, "install an SSL cert");

        final String sslKey = getTestKey();
        final String sslPem = getTestPem();
        final String sslPemSha = ShaUtil.sha256_hex(sslPem);
        final String name = RandomStringUtils.randomAlphanumeric(10);
        final InstallSslCertData certData = new InstallSslCertData().setKey(sslKey).setPem(sslPem);

        final SslCertificateRequest request = new SslCertificateRequest();
        request.setName(name);
        request.setKey(sslKey);
        request.setPem(sslPem);

        final MockRootySender sender = getRootySender();
        sender.flush();

        apiDocs.addNote("install a new SSL cert");
        final RestResponse response = post(CERTS_ENDPOINT+"/"+name, toJson(request));
        assertEquals(HttpStatusCodes.OK, response.status);

        final List<RootyMessage> sent = sender.getSent();
        assertEquals(1, sent.size());
        final InstallSslCertMessage rootyMessage = (InstallSslCertMessage) sent.get(0);
        assertEquals(certData, rootyMessage.getData());

        apiDocs.addNote("list all certs, verify the cert we created is there");
        SslCertificate[] certs = fromJson(get(CERTS_ENDPOINT).json, SslCertificate[].class);
        assertTrue(certs.length >= 2);
        boolean found = false;
        for (SslCertificate cert : certs) {
            if (cert.getPemSha().equals(sslPemSha)) {
                found = true; break;
            }
        }
        assertTrue(found);
    }

}
