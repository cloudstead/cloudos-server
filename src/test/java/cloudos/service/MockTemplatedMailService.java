package cloudos.service;

import org.cobbzilla.mail.TemplatedMailSender;

public class MockTemplatedMailService extends TemplatedMailService {

    @Override protected TemplatedMailSender initMailSender() { return new MockTemplatedMailSender(); }

}
