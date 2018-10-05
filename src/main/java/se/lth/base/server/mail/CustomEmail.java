package se.lth.base.server.mail;

import org.simplejavamail.email.Email;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.email.Recipient;
import org.slf4j.LoggerFactory;
import se.lth.base.server.Config;
import se.lth.base.server.data.*;

import javax.mail.Message;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Creates a SimpleEmail object based on a standard predefined EmailType
 *
 * @author Group 1 ETSN05 2018
 */
public class CustomEmail {
    private final static String TITLE = "title";
    private final static String PREVIEW = "preview";
    private final static String INTRO = "intro";
    private final static String BUTTON = "button";
    private final static String OUTRO = "outro";
    private final static String WEBSITE_LINK = "http://www.yourcommutecompanion.herokuapp.com";
    private final static String EMAIL_TEMPLATE = "email-template.html";

    private final DriveWrap driveWrap;
    private final EmailType emailType;
    private final User singleUser;
    private List<Recipient> recipients;

    /**
     * Basic custom email, handles most scenarios
     *
     * @param driveWrap DriveWrap DTO
     * @param emailType EmailType defined by se.lth.base.server.mail.EmailType
     */
    public CustomEmail(DriveWrap driveWrap, EmailType emailType) {
        this(driveWrap, emailType, null);
    }

    /**
     * Used to send non drive related emails
     *
     * @param user      User to be warned
     * @param emailType EmailType defined by se.lth.base.server.mail.EmailType
     */
    public CustomEmail(User user, EmailType emailType) {
        this(null, emailType, user);
    }

    // TODO: Additional constructor to handle search filter match and adjust master constructor
    public CustomEmail(DriveWrap driveWrap, EmailType emailType, User user) {
        this.driveWrap = driveWrap;
        this.emailType = emailType;
        this.recipients = new ArrayList<>();
        this.singleUser = user;
    }

    public Email getEmail() throws IOException {
        Map<String, String> subjectAndBody = getSubjectAndBody();

        return EmailBuilder.startingBlank()
                .from(MailHandler.DEFAULT_SENDER)
                .to(recipients)
                .withSubject(subjectAndBody.get("subject"))
                .withHTMLText(subjectAndBody.get("body"))
                .withPlainText("Please view this email in a modern email client!")
                .buildEmail();
    }

    private Map<String, String> getSubjectAndBody() throws IOException {
        String subject = "";
        String body = "";
        Map<String, String> subjectAndBody = new HashMap<>();

        switch (emailType) {
            case WELCOME: {
                subject = getWelcomeSubject();
                body = getWelcomeBody();
                break;
            }
            case NEW_PASSENGER_ON_TRIP: {
                subject = getNewPassengerOnTripSubject();
                body = getNewPassengerOnTripBody();
                break;
            }
            case BOOKING_CONFIRMED: {
                subject = getBookingConfirmedSubject();
                body = getBookingConfirmedBody();
                break;
            }
            case RATING: {
                subject = getRatingSubject();
                body = getRatingBody();
                break;
            }
            case FILTER_MATCH: {
                subject = getFilterMatchSubject();
                body = getFilterMatchBody();
                break;
            }
            case PASSENGER_CANCELLED_TRIP: {
                subject = getPassengerCancelledTripSubject();
                body = getPassengerCancelledTripBody();
                break;
            }
            case DRIVER_CANCELLED_DRIVE: {
                subject = getDriverCancelledDriveSubject();
                body = getDriverCancelledDriveBody();
                break;
            }
            case WARNING: {
                subject = getWarningSubject();
                body = getWarningBody();
                break;
            }
            case DRIVER_REMOVED_PASSENGER: {
                subject = getDriverRemovedPassengerSubject();
                body = getDriverRemovedPassengerBody();
                break;
            }
        }

        subjectAndBody.put("subject", subject);
        subjectAndBody.put("body", body);
        return subjectAndBody;
    }

    // Helper methods to fetch HTML email template
    private String getResource(String resourceName, Map<String, String> replacements) throws IOException {
        InputStream inputStream = CustomEmail.class.getResourceAsStream(resourceName);
        InputStreamReader streamReader = new InputStreamReader(inputStream);

        try (BufferedReader br = new BufferedReader(streamReader)) {
            String template = br.lines().collect(Collectors.joining(System.lineSeparator()));

            // Matches all instances of Mustache wraps
            Matcher m = Pattern.compile("\\{\\{[A-z]\\w+}}").matcher(template);

            while (m.find()) {
                String mustacheValue = m.group().substring(2,m.group().length() - 2);
                String replacement = replacements.get(mustacheValue);
                template = template.replaceAll("\\{\\{" + mustacheValue + "}}", replacement == null ? "" : replacement);
            }

            return template;
        } catch (IOException e) {
            LoggerFactory.getLogger(CustomEmail.class).error(e.getMessage(), e);
            throw e;
        }
    }

    private String getButton(String link, String text) throws IOException {
        Map<String, String> buttonContent = new HashMap<>();
        buttonContent.put("buttonLink", link);
        buttonContent.put("buttonText", text);
        return getResource("button-template.html", buttonContent);
    }

    // ---------- STANDARD EMAILS BELOW --------------
    private String getWelcomeSubject() {
        return singleUser.getFirstName() + ", welcome to CommuteCompanion";
    }

    private String getWelcomeBody() throws IOException {
        String firstName = singleUser.getFirstName();
        Map<String, String> content = new HashMap<>();

        // Title
        content.put(TITLE, getWelcomeSubject());

        // Preview
        String preview = firstName + ", welcome to CommuteCompanion. Please visit our website to find or post trips.";
        content.put(PREVIEW, preview);

        // Intro
        String intro = "<p>Hi " + firstName + ",</p>";
        intro += "<p>Welcome to CommuteCompanion! Please visit our website to post or find your first trip.</p>";
        content.put(INTRO, intro);

        // Action button
        content.put(BUTTON, getButton(WEBSITE_LINK, "Get Started Here"));

        // Add recipient
        String fullName = singleUser.getFirstName() + " " + singleUser.getLastName();
        recipients.add(new Recipient(fullName, singleUser.getEmail(), Message.RecipientType.TO));

        return getResource(EMAIL_TEMPLATE, content);
    }

    private String getNewPassengerOnTripSubject() {
        return "New passenger request for your drive to " + driveWrap.getDrive().getStop();
    }

    private String getNewPassengerOnTripBody() throws IOException {
        Map<String, String> content = new HashMap<>();
        DriveUser lastPassenger = null;
        DriveUser driver = null;
        Timestamp departureTime = driveWrap.getDrive().getDepartureTime();
        String formattedDepartureTime = new SimpleDateFormat("EEEEE MMMMM d 'at' HH:mm").format(departureTime);

        // Fetch the driver and last passenger added
        for (DriveUser u : driveWrap.getUsers()) {
            if (u.isDriver()) {
                driver = u;
            }

            lastPassenger = u;
        }

        if (driver == null) {
            throw new IllegalArgumentException("Driver is null");
        }

        // Get the user data
        UserDataAccess userDao = new UserDataAccess(Config.instance().getDatabaseDriver());
        User driverU = userDao.getUser(driver.getUserId());

        // Title
        content.put(TITLE, getNewPassengerOnTripSubject());

        // Intro
        String intro = "<p>Hi " + driverU.getFirstName() + ",</p>";
        intro += "<p>A passenger has requested to travel with you from <i>";
        intro += lastPassenger.getStart() + "</i> to <i>" + lastPassenger.getStop();
        intro += "</i> on your drive from <i>" + driver.getStart() + "</i> to <i>";
        intro += driver.getStop() + "</i> on " + formattedDepartureTime + ".</p>";
        content.put(INTRO, intro);

        // Action button
        content.put(BUTTON, getButton(WEBSITE_LINK + "/drive/" + driveWrap.getDrive().getDriveId(), "View Drive"));

        // Add recipient
        String fullName = driverU.getFirstName() + " " + driverU.getLastName();
        recipients.add(new Recipient(fullName, driverU.getEmail(), Message.RecipientType.TO));

        return getResource(EMAIL_TEMPLATE, content);
    }

    private String getBookingConfirmedSubject() {
        return "Your booking request is confirmed";
    }

    private String getBookingConfirmedBody() throws IOException {
        Map<String, String> content = new HashMap<>();
        DriveUser acceptedPassenger = null;
        DriveUser driver = null;

        // Fetch the driver and last passenger added
        for (DriveUser u : driveWrap.getUsers()) {
            if (u.isDriver()) {
                driver = u;
            }

            acceptedPassenger = u;
        }

        Timestamp departureTime = driveWrap.getDrive().getDepartureTime();
        String formattedDepartureDate = new SimpleDateFormat("EEEEE MMMMM d").format(departureTime);

        if (driver == null) {
            throw new IllegalArgumentException("Driver is null");
        }

        // Get the user data
        UserDataAccess userDao = new UserDataAccess(Config.instance().getDatabaseDriver());
        User driverU = userDao.getUser(driver.getUserId());

        // Title
        content.put(TITLE, getBookingConfirmedSubject());

        // Intro
        String intro = "<p>Hi " + singleUser.getFirstName() + ",</p>";
        intro += "<p>Your booking request with " + driverU.getFirstName() + " from <i>";
        intro += acceptedPassenger.getStart() + "</i> to <i>" + acceptedPassenger.getStop();
        intro += "</i> on " + formattedDepartureDate + " has been accepted.</p>";
        content.put(INTRO, intro);

        // Action button
        content.put(BUTTON, getButton(WEBSITE_LINK + "/drive/" + driveWrap.getDrive().getDriveId(), "View Trip"));

        // Add recipient
        String fullName = singleUser.getFirstName() + " " + singleUser.getLastName();
        recipients.add(new Recipient(fullName, singleUser.getEmail(), Message.RecipientType.TO));

        return getResource(EMAIL_TEMPLATE, content);
    }

    private String getRatingSubject() {
        return "Please rate your latest trip";
    }

    private String getRatingBody() throws IOException {
        Map<String, String> content = new HashMap<>();
        UserDataAccess userDao = new UserDataAccess(Config.instance().getDatabaseDriver());
        DriveUser driver = null;

        // Fetch the driver and last passenger added
        for (DriveUser u : driveWrap.getUsers()) {
            User user = userDao.getUser(u.getUserId());
            String fullName = user.getFirstName() + " " + user.getLastName();
            recipients.add(new Recipient(fullName, user.getEmail(), Message.RecipientType.TO));
            if (u.isDriver()) {
                driver = u;
            }
        }

        if (driver == null) {
            throw new IllegalArgumentException("Driver is null");
        }

        User driverU = userDao.getUser(driver.getUserId());

        // Title
        content.put(TITLE, getRatingSubject());

        // Intro
        String intro = "<p>Hi,</p>";
        intro += "<p>We hope your ride with " + driverU.getFirstName() + " was satisfactory. ";
        intro += "Please take a minute to tell your fellow CommuteCompanioners about it.</p>";
        content.put(INTRO, intro);

        // Action button
        content.put(BUTTON, getButton(WEBSITE_LINK, "Rate Your Trip"));

        return getResource(WEBSITE_LINK + "/drive/" + driveWrap.getDrive().getDriveId(), content);
    }

    private String getFilterMatchSubject() {
        return "A new trip has matched your filter";
    }

    private String getFilterMatchBody() throws IOException {
        Map<String, String> content = new HashMap<>();

        // Title
        content.put(TITLE, getFilterMatchSubject());

        // Intro
        // TODO: Expand the details in this message when SearchFilterDTO is created
        String intro = "<p>Hi " + singleUser.getFirstName() + ",</p>";
        intro += "<p>A new trip matching one of your search filters has been posted to CommuteCompanion. ";
        intro += "Check it out by clicking the button below.</p>";
        content.put(INTRO, intro);

        // Action button
        // TODO: Set correct deep link when SearchFilterDTO is created
        content.put(BUTTON, getButton(WEBSITE_LINK, "Go to Trip"));

        // Add recipient
        String fullName = singleUser.getFirstName() + " " + singleUser.getLastName();
        recipients.add(new Recipient(fullName, singleUser.getEmail(), Message.RecipientType.TO));

        return getResource(EMAIL_TEMPLATE, content);
    }

    private String getPassengerCancelledTripSubject() {
        return "A passenger has cancelled their trip";
    }

    private String getPassengerCancelledTripBody() throws IOException {
        Map<String, String> content = new HashMap<>();
        DriveUser lastPassenger = null;
        DriveUser driver = null;
        Timestamp departureTime = driveWrap.getDrive().getDepartureTime();
        String formattedDepartureTime = new SimpleDateFormat("EEEEE MMMMM d 'at' HH:mm").format(departureTime);

        // Fetch the driver and last passenger added
        for (DriveUser u : driveWrap.getUsers()) {
            if (u.isDriver()) {
                driver = u;
            }

            lastPassenger = u;
        }

        if (driver == null) {
            throw new IllegalArgumentException("Driver is null");
        }

        // Get the user data
        UserDataAccess userDao = new UserDataAccess(Config.instance().getDatabaseDriver());
        User driverU = userDao.getUser(driver.getUserId());

        // Title
        content.put(TITLE, getNewPassengerOnTripSubject());

        // Intro
        String intro = "<p>Hi " + driverU.getFirstName() + ",</p>";
        intro += "<p>A passenger has cancelled their trip with you from <i>";
        intro += lastPassenger.getStart() + "</i> to <i>" + lastPassenger.getStop();
        intro += "</i> on your drive from <i>" + driver.getStart()+ "</i> to <i>";
        intro += driver.getStop() + "</i> on " + formattedDepartureTime + ".</p>";
        content.put(INTRO, intro);

        // Action button
        content.put(BUTTON, getButton(WEBSITE_LINK + "/drive/" + driveWrap.getDrive().getDriveId(), "View Drive"));

        // Add recipient
        String fullName = driverU.getFirstName() + " " + driverU.getLastName();
        recipients.add(new Recipient(fullName, driverU.getEmail(), Message.RecipientType.TO));

        return getResource(EMAIL_TEMPLATE, content);
    }

    private String getDriverCancelledDriveSubject() {
        return "Your trip has been cancelled";
    }

    private String getDriverCancelledDriveBody() throws IOException {
        Map<String, String> content = new HashMap<>();
        DriveUser driver = null;
        Timestamp departureTime = driveWrap.getDrive().getDepartureTime();
        String formattedDepartureDate = new SimpleDateFormat("EEEEE MMMMM d").format(departureTime);
        UserDataAccess userDao = new UserDataAccess(Config.instance().getDatabaseDriver());

        // Fetch the driver and last passenger added
        for (DriveUser u : driveWrap.getUsers()) {
            if (u.isDriver()) {
                driver = u;
            } else {
                User user = userDao.getUser(u.getUserId());
                String fullName = user.getFirstName() + " " + user.getLastName();
                recipients.add(new Recipient(fullName, user.getEmail(), Message.RecipientType.TO));
            }
        }

        if (driver == null) {
            throw new IllegalArgumentException("Driver is null");
        }

        User driverU = userDao.getUser(driver.getUserId());

        // Title
        content.put(TITLE, getFilterMatchSubject());

        // Intro
        String intro = "<p>Hi,</p>";
        intro += "<p>Your trip with " + driverU.getFirstName() + " on ";
        intro += formattedDepartureDate + " has been cancelled by the driver. ";
        intro += "We apologize for the inconvenience.</p>";
        content.put(INTRO, intro);

        return getResource(EMAIL_TEMPLATE, content);
    }

    private String getWarningSubject() {
        return "A warning has been issued";
    }

    private String getWarningBody() throws IOException {
        Map<String, String> content = new HashMap<>();

        // Title
        content.put(TITLE, getFilterMatchSubject());

        // Intro
        String intro = "<p>Hi " + singleUser.getFirstName() + ",</p>";
        intro += "<p>You have been issued a warning by the CommuteCompanion admins. ";
        intro += "Please be sure to always adhere to common sense when on CommuteCompanion.</p>";
        content.put(INTRO, intro);

        // Add recipient
        String fullName = singleUser.getFirstName() + " " + singleUser.getLastName();
        recipients.add(new Recipient(fullName, singleUser.getEmail(), Message.RecipientType.TO));

        return getResource(EMAIL_TEMPLATE, content);
    }

    private String getDriverRemovedPassengerSubject() {
        return "You have been removed from your trip";
    }

    private String getDriverRemovedPassengerBody() throws IOException {
        Map<String, String> content = new HashMap<>();
        DriveUser driver = null;

        // Fetch the driver and last passenger added
        for (DriveUser u : driveWrap.getUsers()) {
            if (u.isDriver()) {
                driver = u;
            }
        }

        Timestamp departureTime = driveWrap.getDrive().getDepartureTime();
        String formattedDepartureDate = new SimpleDateFormat("EEEEE MMMMM d").format(departureTime);

        if (driver == null) {
            throw new IllegalArgumentException("Driver is null");
        }

        // Get the user data
        UserDataAccess userDao = new UserDataAccess(Config.instance().getDatabaseDriver());
        User driverU = userDao.getUser(driver.getUserId());

        // Title
        content.put(TITLE, getBookingConfirmedSubject());

        // Intro
        String intro = "<p>Hi " + singleUser.getFirstName() + ",</p>";
        intro += "<p>The driver " + driverU.getFirstName() + " has removed you from your trip on ";
        intro += formattedDepartureDate + ". We apologize for the inconvenience.</p>";
        content.put(INTRO, intro);

        // Add recipient
        String fullName = singleUser.getFirstName() + " " + singleUser.getLastName();
        recipients.add(new Recipient(fullName, singleUser.getEmail(), Message.RecipientType.TO));

        return getResource(EMAIL_TEMPLATE, content);
    }
}
