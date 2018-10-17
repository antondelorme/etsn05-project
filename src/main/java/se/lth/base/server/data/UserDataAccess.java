package se.lth.base.server.data;

import se.lth.base.server.database.DataAccess;
import se.lth.base.server.database.DataAccessException;
import se.lth.base.server.database.ErrorType;
import se.lth.base.server.database.Mapper;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Basic functionality to support standard user operations. Some notable omissions are removing user, time out on
 * sessions, getting a user by name or id, etc.
 * <p>
 * This is intended to be as realistic as possible with reasonable security (single factor authentication).
 * The security measures are as follows.
 * <ul>
 * <li>All passwords are stored in a hashed format in the database, using @{@link Credentials#generatePasswordHash(long)}}</li>
 * <li>emails are used to salt passwords,
 * <a href="https://en.wikipedia.org/wiki/Salt_(cryptography)">see here for explanation.</a>
 * <li>When a user does login, it receives a UUID-token. This token is then used to authenticate,
 * using @{@link #getSession}.
 * </li>
 * </ul>
 *
 * @author Rasmus Ros, rasmus.ros@cs.lth.se
 * @see DataAccess
 */
public class UserDataAccess extends DataAccess<User> {

    public UserDataAccess(String driverUrl) {
        super(driverUrl, new UserMapper());
    }

    /**
     * Add a new user to the system.
     *
     * @param credentials of the new user, containing name, role, and password.
     * @throws DataAccessException if duplicated emails or wrong format.
     */
    public User addUser(Credentials credentials) {
        User user = credentials.getUser();
        Role role = credentials.getRole();
        String email = credentials.getEmail();
        long salt = Credentials.generateSalt();
        UUID passwordHash = credentials.generatePasswordHash(salt);
        String firstName = user.getFirstName();
        String lastName = user.getLastName();
        String phoneNumber = user.getPhoneNumber();
        int gender = user.getGender();
        long dateOfBirth = user.getDateOfBirth();
        boolean drivingLicense = user.getDrivingLicence();
        int userId = insert("INSERT INTO user (role_id, email, salt, password_hash, first_name, last_name, " +
                        "phone_number, gender, date_of_birth, driving_license) VALUES " +
                        "((SELECT role_id FROM user_role WHERE user_role.role=?),?,?,?,?,?,?,?,?,?)",
                role.name(), email, salt, passwordHash, firstName, lastName, phoneNumber, gender,
                new Date(dateOfBirth), drivingLicense);
        return new User(userId, credentials.getRole(), email, firstName, lastName, phoneNumber, gender,
                dateOfBirth, drivingLicense, 0, 0, 0);
    }

    public User updateUser(int userId, Credentials credentials) {
        User user = credentials.getUser();
        Role role = credentials.getRole();
        String email = credentials.getEmail();
        String firstName = user.getFirstName();
        String lastName = user.getLastName();
        int gender = user.getGender();
        boolean drivingLicence = user.getDrivingLicence();

        if (credentials.hasPassword()) {
            long salt = Credentials.generateSalt();
            UUID passwordHash = credentials.generatePasswordHash(salt);
            execute("UPDATE user SET email = ?, password_hash = ?, salt = ?, first_name = ?, last_name = ?, " +
                            "gender = ?, driving_license = ?, " +
                            "role_id = (SELECT user_role.role_id FROM user_role WHERE user_role.role = ?) " +
                            "WHERE user_id = ?",
                    email, passwordHash, salt, firstName, lastName, gender, drivingLicence, role.name(), userId);
        } else {
            execute("UPDATE user SET email = ?, first_name = ?, last_name = ?, " +
                            "gender = ?, driving_license = ?, role_id = (" +
                            "    SELECT user_role.role_id FROM user_role WHERE user_role.role = ?) " +
                            "WHERE user_id = ?",
                    email, firstName, lastName, gender, drivingLicence, credentials.getRole().name(), userId);
        }
        return getUser(userId);
    }	

    public User getUser(int userId) {
        return queryFirst("SELECT user_id, role, email, first_name, last_name, phone_number, gender, " +
                "date_of_birth, driving_license, rating_total_score, number_of_ratings, warning FROM user, user_role " +
                "WHERE user.user_id = ? AND user.role_id = user_role.role_id", userId);
    }

    public boolean deleteUser(int userId) {
        return execute("DELETE FROM user WHERE user_id = ?", userId) > 0;
    }

    /**
     * @return all users in the system.
     */
    public List<User> getUsers() {
        return query("SELECT user_id, role, email, first_name, last_name, phone_number, gender, " +
                "date_of_birth, driving_license, rating_total_score, number_of_ratings, warning FROM user, user_role " +
                "WHERE user.role_id = user_role.role_id");
    }

    /**
     * Updates the drivers rating after a drive.
     *
     * @param userId the userId of the driver.
     * @param rating the rating that the driver recieves.
     */
    public boolean updateUserRating(int userId, int rating) {
        return execute("UPDATE user SET rating_total_score = " +
                "(SELECT rating_total_score FROM user WHERE user_id = ?) + ?, " +
                "number_of_ratings = (SELECT number_of_ratings FROM user WHERE user_id = ?) + 1 " +
                "WHERE user_id = ?", userId, rating, userId, userId) > 0;
    }

    /**
     * Fetch session and the corresponding user.
     *
     * @param sessionId globally unqiue identifier, stored in the client.
     * @return session object wrapping the user.
     * @throws DataAccessException if the session is not found.
     */
    public Session getSession(UUID sessionId) {
        User user = queryFirst("SELECT user.user_id, role, email, first_name, last_name, phone_number, " +
                "gender, date_of_birth, driving_license, rating_total_score, number_of_ratings, warning FROM user, " +
                "user_role, session WHERE user_role.role_id = user.role_id AND session.user_id = user.user_id " +
                "AND session.session_uuid = ?", sessionId);
        execute("UPDATE session SET last_seen = CURRENT_TIMESTAMP() " +
                "WHERE session_uuid = ?", sessionId);
        return new Session(sessionId, user);
    }

    /**
     * Logout a user. This method is idempotent, meaning it is safe to repeat indefinitely.
     *
     * @param sessionId session to remove
     * @return true if the session was found, false otherwise.
     */
    public boolean removeSession(UUID sessionId) {
        return execute("DELETE FROM session WHERE session_uuid = ?", sessionId) > 0;
    }

    public boolean warnUser(int userId) {
        return execute("UPDATE user SET warning = (SELECT warning FROM user WHERE user_id = ?) + 1 WHERE user_id = ?", userId, userId) > 0;
    }

    /**
     * Login a user.
     *
     * @param credentials email and plain text password.
     * @return New user session, consisting of a @{@link UUID} and @{@link User}.
     * @throws DataAccessException if the email or password does not match.
     */
    public Session authenticate(Credentials credentials) {
        long salt = new DataAccess<>(getDriverUrl(), (rs) -> rs.getLong(1))
                .queryFirst("SELECT salt FROM user WHERE email = ?", credentials.getEmail());
        UUID hash = credentials.generatePasswordHash(salt);

        try {
            User user = queryFirst("SELECT user_id, role, email, first_name, last_name, phone_number, " +
                            "gender, date_of_birth, driving_license, rating_total_score, number_of_ratings, warning FROM user, " +
                            "user_role WHERE user_role.role_id = user.role_id AND email = ? AND password_hash = ?",
                    credentials.getEmail(), hash);
            UUID sessionId = insert("INSERT INTO session (user_id) SELECT user_id FROM user WHERE email = ?",
                    user.getEmail());
            return new Session(sessionId, user);
        } catch (DataAccessException e) {
            throw new DataAccessException("Email or password incorrect", ErrorType.DATA_QUALITY);
        }
    }

    private static class UserMapper implements Mapper<User> {
        @Override
        public User map(ResultSet resultSet) throws SQLException {
            return new User(resultSet.getInt("user_id"), Role.valueOf(resultSet.getString("role")),
            		resultSet.getString("email"), resultSet.getString("first_name"), resultSet.getString("last_name"),
                    resultSet.getString("phone_number"), resultSet.getInt("gender"),
                    resultSet.getObject("date_of_birth", Date.class).getTime(), resultSet.getBoolean("driving_license"),
                    resultSet.getInt("rating_total_score"), resultSet.getInt("number_of_ratings"), resultSet.getInt("warning"));
        }
    }
}
