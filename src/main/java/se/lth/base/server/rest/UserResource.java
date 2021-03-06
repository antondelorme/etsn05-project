package se.lth.base.server.rest;

import se.lth.base.server.Config;
import se.lth.base.server.data.*;
import se.lth.base.server.database.DataAccessException;
import se.lth.base.server.mail.MailHandler;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.sql.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Path("user")
public class UserResource {
    public static final String USER_TOKEN = "USER_TOKEN";
    private final ContainerRequestContext context;
    private final User user;
    private final Session session;
    private final UserDataAccess userDao = new UserDataAccess(Config.instance().getDatabaseDriver());
    private final DriveDataAccess driveDao = new DriveDataAccess(Config.instance().getDatabaseDriver());
    private final MailHandler mailHandler = new MailHandler();

    public UserResource(@Context ContainerRequestContext context) {
        this.context = context;
        this.user = (User) context.getProperty(User.class.getSimpleName());
        this.session = (Session) context.getProperty(Session.class.getSimpleName());
    }

    @GET
    @PermitAll
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public User currentUser() {
        return user;
    }

    @Path("login")
    @POST
    @PermitAll
    @Consumes(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response login(Credentials credentials, @QueryParam("remember") @DefaultValue("false") boolean rememberMe) {

        try {
            Session newSession = userDao.authenticate(credentials);
            int maxAge = rememberMe ? (int) TimeUnit.DAYS.toSeconds(7) : NewCookie.DEFAULT_MAX_AGE;
            return Response.noContent().cookie(newCookie(newSession.getSessionId().toString(), maxAge, null)).build();
        } catch (DataAccessException e) {
            throw new WebApplicationException("Incorrect password or email, please try again!", Response.Status.PRECONDITION_FAILED);
        }
    }

    @Path("logout")
    @POST
    @PermitAll
    public Response logout() {
        userDao.removeSession(session.getSessionId());
        return Response.noContent().cookie(newCookie("", 0, new Date(0L))).build();
    }

    @Path("roles")
    @GET
    @RolesAllowed(Role.Names.ADMIN)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Set<Role> getRoles() {
        return Role.ALL_ROLES;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @PermitAll
    public User createUser(Credentials credentials) {
        if (credentials == null || credentials.getUser() == null) {
            throw new WebApplicationException("No user data", Response.Status.BAD_REQUEST);
        }

        credentials.sanitizeAndValidate();
        User user = userDao.addUser(credentials);
        try {
            mailHandler.welcomeUser(user);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return user;
    }

    @Path("all")
    @GET
    @RolesAllowed(Role.Names.ADMIN)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public List<User> getUsers() {
        return userDao.getUsers();
    }

    @Path("{userId}")
    @GET
    @RolesAllowed(Role.Names.USER)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public User getUser(@PathParam("userId") int userId) {
        return userDao.getUser(userId);
    }

    @Path("{userId}")
    @RolesAllowed(Role.Names.USER)
    @PUT
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Consumes(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public User putUser(@PathParam("userId") int userId, Credentials credentials) {
        if (credentials == null || credentials.getUser() == null) {
            throw new WebApplicationException("No user data", Response.Status.BAD_REQUEST);
        }

        // Only allowed to update yourself unless you're an admin
        if (userId != user.getId() && !user.getRole().clearanceFor(Role.ADMIN)) {
            throw new WebApplicationException("Operation not allowed", Response.Status.UNAUTHORIZED);
        }

        // Only allowed to change profile settings if your role has not changed unless you're an admin
        if (!user.getRole().clearanceFor(Role.ADMIN) && user.getRole().getLevel() != credentials.getRole().getLevel()) {
            throw new WebApplicationException("Operation not allowed", Response.Status.UNAUTHORIZED);
        }

        // Sanitize and validate input
        credentials.sanitizeAndValidate(credentials.hasPassword());

        return userDao.updateUser(userId, credentials);
    }

    @Path("warn/{userId}")
    @RolesAllowed(Role.Names.ADMIN)
    @PUT
    public void warnUser(@PathParam("userId") int userId) {
        User user = userDao.getUser(userId);
        try {
            mailHandler.notifyUserHasBeenWarned(user);
        } catch (IOException e) {
            e.printStackTrace();
        }

        userDao.warnUser(userId);
    }

    @Path("{userId}")
    @RolesAllowed(Role.Names.USER)
    @DELETE
    public void deleteUser(@PathParam("userId") int userId) {
        if (userId == user.getId() || user.getRole().getLevel() > userDao.getUser(userId).getRole().getLevel()) {

            //remove users drives
            List<Drive> userDrives = driveDao.getDrivesForUser(userId);
            userDrives.forEach(d -> driveDao.deleteDrive(d.getDriveId()));

            if (!userDao.deleteUser(userId)) {
                throw new WebApplicationException("Could not delete user", Response.Status.NOT_FOUND);
            }
        } else {
            throw new WebApplicationException("You are not permitted to delete this user", Response.Status.FORBIDDEN);
        }
    }

    private NewCookie newCookie(String value, int maxAge, Date expiry) {
        return new NewCookie(USER_TOKEN, value,"/rest", context.getUriInfo().getBaseUri().getHost(),
                NewCookie.DEFAULT_VERSION, "", maxAge, expiry, false, true);
    }
}
