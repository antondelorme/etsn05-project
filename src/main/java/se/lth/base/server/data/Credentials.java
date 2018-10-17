package se.lth.base.server.data;

import com.google.gson.annotations.Expose;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Date;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Used for authentication and user operations requiring passwords.
 *
 * @author Rasmus Ros, rasmus.ros@cs.lth.se
 */
public class Credentials {
    private static final int SIZE = 256;
    private static final int ITERATION_COST = 16;
    private static final String ALGORITHM = "PBKDF2WithHmacSHA1";
    private final String email;
    @Expose(serialize = false)
    private final String password;
    private final Role role;
    private final User user;

    /**
     * Creates a new Credentials object
     *
     * @param email user email
     * @param password user password
     * @param role user role
     * @param user user info
     */
    public Credentials(String email, String password, Role role, User user) {
        this.email = email;
        this.password = password;
        this.role = role;
        this.user = user;
    }

    static long generateSalt() {
        return new SecureRandom().nextLong();
    }

    /**
     * Get a user's email
     *
     * @return user email
     */
    public String getEmail() {
        return email;
    }

    /**
     * Get a user's role
     *
     * @return user role
     */
    public Role getRole() {
        return role;
    }

    /**
     * Get user info
     *
     * @return user object
     */
    public User getUser() {
        return user;
    }

    /**
     * Check if the credentials object has a password
     *
     * @return true/false
     */
    public boolean hasPassword() {
        return password != null;
    }

    private String phoneNumberSanitizer(String phoneNumber) {
        // Strip illegal characters
        phoneNumber = phoneNumber.trim().replaceAll("[^+0-9]", "");

        // Check for number format
        if (phoneNumber.substring(0, 2).equals("00")) {
            phoneNumber = "+" + phoneNumber.substring(2);
        } else if (phoneNumber.substring(0, 1).equals("0")) {
            phoneNumber = "+46" + phoneNumber.substring(1);
        }

        return phoneNumber;
    }

    private String nameSanitizer(String name) {
        // Strip illegal chars and make lowercase
        name = name.trim().replaceAll("[^\\s-A-zÅÄÖåäö]", "").toLowerCase();
        StringBuilder sb = new StringBuilder(name);
        Pattern p = Pattern.compile("[\\s+-]");
        Matcher m = p.matcher(name);

        // Capitalize first character in every word in first name
        sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
        while (m.find()) {
            int charPos = m.start() + 1;
            sb.setCharAt(charPos, Character.toUpperCase(sb.charAt(charPos)));
        }

        return sb.toString();
    }

    /**
     * Sanitize the credential object from faulty input
     */
    public void sanitizeAndValidate() {
        sanitizeAndValidate(true);
    }

    /**
     * Sanitize the credential object from faulty input
     *
     * @param checkPass if checks should be made on the password
     */
    public void sanitizeAndValidate(boolean checkPass) {
        String firstName = user.getFirstName();
        String lastName = user.getLastName();
        String phoneNumber = user.getPhoneNumber();
        long dateOfBirth = user.getDateOfBirth();
        String email = user.getEmail();

        // Check all values are set
        if (firstName == null) {
            throw new WebApplicationException("First name not specified", Response.Status.BAD_REQUEST);
        }

        if (lastName == null) {
            throw new WebApplicationException("Last name not specified", Response.Status.BAD_REQUEST);
        }

        if (dateOfBirth == 0) {
            throw new WebApplicationException("Date of birth not specified", Response.Status.BAD_REQUEST);
        }

        if (user.getDrivingLicence() == null) {
            throw new WebApplicationException("Driving license not specified", Response.Status.BAD_REQUEST);
        }

        if (email == null) {
            throw new WebApplicationException("No email specified", Response.Status.BAD_REQUEST);
        }

        if (phoneNumber == null) {
            throw new WebApplicationException("Phone number not specified", Response.Status.BAD_REQUEST);
        }

        if (checkPass && password == null) {
            throw new WebApplicationException("Password not specified", Response.Status.BAD_REQUEST);
        }

        // Sanitize name and numbers
        user.setFirstName(nameSanitizer(firstName));
        user.setLastName(nameSanitizer(lastName));
        user.setPhoneNumber(phoneNumberSanitizer(phoneNumber));

        // Check first name
        if (user.getFirstName().length() < 2) {
            throw new WebApplicationException("First name too short, minimum 2 characters", Response.Status.BAD_REQUEST);
        }

        // Check last name
        if (user.getLastName().length() < 2) {
            throw new WebApplicationException("Last name too short, minimum 2 characters", Response.Status.BAD_REQUEST);
        }

        // Check age
        long currentAge = (new Date().getTime() - dateOfBirth) / 1000;
        long legalAge = 60 * 60 * 24 * 365 * 18;
        if (currentAge < legalAge) {
            throw new WebApplicationException("Too young to register, minimum 18 years old", Response.Status.BAD_REQUEST);
        }

        // Check email
        try {
            new InternetAddress(email);
        } catch (AddressException e) {
            throw new WebApplicationException("Email not valid", Response.Status.BAD_REQUEST);
        }

        String[] tokens = email.split("@");
        if (tokens.length != 2 ||
                tokens[0] == null || tokens[0].trim().length() == 0 ||
                tokens[1] == null || tokens[1].trim().length() == 0) {
            throw new WebApplicationException("Email not valid", Response.Status.BAD_REQUEST);
        }

        // Check phone number
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        try {
            phoneUtil.isValidNumber(phoneUtil.parse(user.getPhoneNumber(), null));
        } catch (NumberParseException e) {
            throw new WebApplicationException("Invalid phone number", Response.Status.BAD_REQUEST);
        }

        // Check password
        if (checkPass && !password.matches("^(?=.*\\d)(?=.*\\d)(?=.*\\d).{7,}$")) {
            throw new WebApplicationException("Password not secure enough, minimum 8 characters whereof 3 non alpha", Response.Status.BAD_REQUEST);
        }
    }

    /**
     * Hash password using hashing algorithm intended for this purpose.
     *
     * @return base64 encoded hash result.
     */
    UUID generatePasswordHash(long salt) {
        try {
            KeySpec spec = new PBEKeySpec(password.toCharArray(),
                    ByteBuffer.allocate(8).putLong(salt).array(),
                    ITERATION_COST, SIZE);
            SecretKeyFactory f = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] blob = f.generateSecret(spec).getEncoded();
            LongBuffer lb = ByteBuffer.wrap(blob).asLongBuffer();
            return new UUID(lb.get(), lb.get());
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Missing algorithm: " + ALGORITHM, ex);
        } catch (InvalidKeySpecException ex) {
            throw new IllegalStateException("Invalid SecretKeyFactory", ex);
        }
    }
}