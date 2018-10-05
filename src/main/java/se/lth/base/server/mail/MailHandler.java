package se.lth.base.server.mail;

import org.simplejavamail.MailException;
import org.simplejavamail.email.Email;
import org.simplejavamail.mailer.Mailer;
import org.simplejavamail.mailer.MailerBuilder;
import org.simplejavamail.mailer.config.TransportStrategy;
import se.lth.base.server.data.DriveWrap;
import se.lth.base.server.data.User;

import java.io.IOException;
import java.util.List;

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
	
	private void sendMail(Email email) throws MailException {
		mailer.sendMail(email);
	}

    /**
     * Send an email to the driver for the last passenger added
     *
     * @param user User to welcome
     */
    public void welcomeUser(User user) throws IOException {
        sendMail(new CustomEmail(user,EmailType.WELCOME).getEmail());
    }

    /**
     * Send an email to the driver for the last passenger added
     *
     * @param driveWrap DriveWrap DTO object
     */
	public void notifyDriverNewPassengerOnTrip(DriveWrap driveWrap) throws IOException {
	    sendMail(new CustomEmail(driveWrap,EmailType.NEW_PASSENGER_ON_TRIP).getEmail());
    }

    /**
     * Send an email to a specified user
     *
     * @param driveWrap DriveWrap DTO object
     * @param user Passenger to be notified
     */
    public void notifyPassengerBookingConfirmed(DriveWrap driveWrap, User user) throws IOException {
        sendMail(new CustomEmail(driveWrap,EmailType.BOOKING_CONFIRMED, user).getEmail());
    }

    /**
     * Sends an email to all users in drive to rate the drive
     *
     * @param driveWrap DriveWrap DTO
     */
    public void sendLinkToRatingScreen(DriveWrap driveWrap) throws IOException {
        sendMail(new CustomEmail(driveWrap,EmailType.RATING).getEmail());
    }

    // TODO: Uncomment when SearchFilter DTO implemented
    /*public void notifyUserSearchFilterMatch(DriveWrap driveWrap, SearchFilter searchFilter) {
        sendMail(new CustomEmail(driveWrap, CustomEmail.FILTER_MATCH_EMAIL, searchFilter).getEmail());
    }*/

    /**
     * Sends an email to a driver that passenger has cancelled trip
     *
     * @param driveWrap DriveWrap DTO
     * @param passenger Passenger that has been removed from drive
     */
    public void notifyDriverPassengerCancelledTrip(DriveWrap driveWrap, User passenger) throws IOException {
        sendMail(new CustomEmail(driveWrap, EmailType.PASSENGER_CANCELLED_TRIP, passenger).getEmail());
    }

    /**
     * Sends an email to passenger that the driver has removed him/her
     *
     * @param driveWrap DriveWrap DTO
     * @param passenger Passenger that has been removed from drive
     */
    public void notifyPassengerDriverRemovedPassenger(DriveWrap driveWrap, User passenger) throws IOException {
        sendMail(new CustomEmail(driveWrap, EmailType.DRIVER_REMOVED_PASSENGER, passenger).getEmail());
    }

    /**
     * Sends an email to all passengers that the drive is cancelled
     *
     * @param driveWrap DriveWrap DTO
     */
    public void notifyPassengersDriverCancelledDrive(DriveWrap driveWrap) throws IOException {
        sendMail(new CustomEmail(driveWrap, EmailType.DRIVER_CANCELLED_DRIVE).getEmail());
    }

    /**
     * Sends an email to a user that he has been warned by an admin
     *
     * @param user User to be warned
     */
    public void notifyUserHasBeenWarned(User user) throws IOException {
        sendMail(new CustomEmail(user, EmailType.WARNING).getEmail());
    }
}
