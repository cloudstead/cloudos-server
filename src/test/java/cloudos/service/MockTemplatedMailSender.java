package cloudos.service;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.cobbzilla.mail.TemplatedMail;
import org.cobbzilla.mail.TemplatedMailSender;

import java.util.ArrayList;
import java.util.List;

public class MockTemplatedMailSender extends TemplatedMailSender {

    @Getter @Setter private List<TemplatedMail> messages = new ArrayList<>();

    public int messageCount () { return messages.size(); }
    public void reset () { messages.clear(); }
    public TemplatedMail first() { return messages.isEmpty() ? null : messages.get(0); }

    @Override public void deliverMessage(TemplatedMail mail) throws Exception { messages.add(mail); }

}
