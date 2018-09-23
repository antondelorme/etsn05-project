package se.lth.base.server.mail;

import org.simplejavamail.MailException;
import org.simplejavamail.email.Email;
import org.simplejavamail.mailer.Mailer;
import org.simplejavamail.mailer.MailerBuilder;
import org.simplejavamail.mailer.config.TransportStrategy;

/**
 * Wrapper class for SimpleMail mail handler
 * 
 * @author Group 1 ETSN05 2018
 *
 */
public class MailHandler {
	private static final String DEFAULT_HOST = "smtp.gmail.com";
	private static final int DEFAULT_PORT = 587;
	private static final String DEFAULT_USERNAME = "yourcommutecompanion@gmail.com";
	private static final String DEFAULT_PASSWORD = "+++commute95";
	public static final String DEFAULT_SENDER = DEFAULT_USERNAME;
	
	private Mailer mailer;
	
	public MailHandler() {
		mailer = MailerBuilder
				.withSMTPServer(
						DEFAULT_HOST, 
						DEFAULT_PORT, 
						DEFAULT_USERNAME, 
						DEFAULT_PASSWORD)
				.withTransportStrategy(TransportStrategy.SMTP_TLS)
				.withSessionTimeout(10 * 1000)
				.buildMailer();
	}
	
	public void sendMail(Email email) throws MailException {
		mailer.sendMail(email);
	}
}
