package se.lth.base.server.data;

import javax.ws.rs.core.SecurityContext;
import java.security.Principal;
import java.sql.Timestamp;
import java.util.UUID;

/**
 * A session is a user that has logged in. The sessionId is used to identify the user in the database.
 *
 * @author Rasmus Ros, rasmus.ros@cs.lth.se
 */
public class Session implements SecurityContext {

    private final UUID sessionId;
    private final User user;
    private final Timestamp lastSeen;
    
    public Session(UUID sessionId, User user, Timestamp lastSeen) {
        this.sessionId = sessionId;
        this.user = user;
        this.lastSeen = lastSeen;
    }

    public User getUser() {
        return user;
    }

    public UUID getSessionId() {
        return sessionId;
    }
    
    public Timestamp getLastSeen(){
    	return lastSeen;
    }

    @Override
    public Principal getUserPrincipal() {
        return user;
    }

    @Override
    public boolean isUserInRole(String role) {
        return user.getRole().clearanceFor(Role.valueOf(role));
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public String getAuthenticationScheme() {
        return SecurityContext.FORM_AUTH;
    }
}
