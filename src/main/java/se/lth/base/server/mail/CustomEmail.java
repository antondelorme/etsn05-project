package se.lth.base.server.mail;

import org.simplejavamail.email.Email;
import org.simplejavamail.email.EmailBuilder;

/**
 * Creates a SimpleEmail object based on a standard predefined email type
 * 
 * @author Group 1 ETSN05 2018
 *
 */
public class CustomEmail {
	public static final int WELCOME_EMAIL = 1;
	
	private String firstName;
	private String lastName;
	private String email;
	private int emailType;

	public CustomEmail(String firstName, String lastName, String email, int emailType) {
		this.firstName = firstName;
		this.lastName = lastName;
		this.email = email;
		this.emailType = emailType;
	}
	
	public Email getEmail() {
		return EmailBuilder.startingBlank()
				.from(MailHandler.DEFAULT_SENDER)
				.to(firstName + " " + lastName, email)
				.withSubject(getSubject())
				.withHTMLText(getMessage())
		        .withPlainText("Please view this email in a modern email client!")
		        .buildEmail(); 	
	}
	
	public String getSubject() {		
		switch (emailType) {
		case WELCOME_EMAIL: return  getWelcomeSubject();
		default: return "INVALID EMAIL TYPE";
		}
	}
	
	public String getMessage() {
		switch (emailType) {
		case WELCOME_EMAIL: return  getWelcomeMessage();
		default: return "INVALID EMAIL TYPE";
		}
	}
	
	// ---------- STANDARD EMAILS BELOW --------------
	
	private String getWelcomeSubject() {
		return "Welcome to CommuteCompanion " + firstName;
	}
	
	private String getWelcomeMessage() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("<h1>Hi ").append(firstName).append("</h1>");
		sb.append("<p>Welcome to CommuteCompanion!</p>");
		
		return sb.toString();
	}
}
