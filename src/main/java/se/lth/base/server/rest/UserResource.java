package se.lth.base.server.rest;

import se.lth.base.server.Config;
import se.lth.base.server.data.*;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.net.URISyntaxException;
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
    public Response login(Credentials credentials,
                          @QueryParam("remember") @DefaultValue("false") boolean rememberMe)
            throws URISyntaxException {
        Session newSession = userDao.authenticate(credentials);
        int maxAge = rememberMe ? (int) TimeUnit.DAYS.toSeconds(7) : NewCookie.DEFAULT_MAX_AGE;
        return Response.noContent().cookie(newCookie(newSession.getSessionId().toString(), maxAge, null)).build();
    }

    private NewCookie newCookie(String value, int maxAge, Date expiry) {
        return new NewCookie(USER_TOKEN,
                value,                                          // value
                "/rest",                                        // path
                context.getUriInfo().getBaseUri().getHost(),    // host
                NewCookie.DEFAULT_VERSION,                      // version
                "",                                             // comment
                maxAge,                                         // max-age
                expiry,                                         // expiry
                false,                                          // secure
                true);                                          // http-onle

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

        return userDao.addUser(credentials);
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
    @RolesAllowed(Role.Names.ADMIN)
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

        // Only allowed to update yourself if you're not an admin
        if (userId != user.getId() && !user.getRole().clearanceFor(Role.ADMIN)) {
            throw new WebApplicationException("Operation not allowed", Response.Status.UNAUTHORIZED);
        }

        // You can only update your own profile if you're not an admin and you're not changing
        // your privilege
        if (userId == user.getId() && (user.getRole().clearanceFor(Role.ADMIN) ||
                user.getRole().getLevel() != credentials.getRole().getLevel())) {
            throw new WebApplicationException("Operation not allowed", Response.Status.UNAUTHORIZED);
        }

        // Sanitize and validate input
        credentials.sanitizeAndValidate(credentials.hasPassword());

        return userDao.updateUser(userId, credentials);
    }

    @Path("{userId}")
    @RolesAllowed(Role.Names.USER)
    @DELETE
    public void deleteUser(@PathParam("userId") int userId) {
        if (userId == user.getId() || user.getRole().getLevel() > userDao.getUser(userId).getRole().getLevel()) {
            if (!userDao.deleteUser(userId)) {
                throw new WebApplicationException("Could not delete user", Response.Status.NOT_FOUND);
            }
        } else {
            throw new WebApplicationException("You are not permitted to delete this user", Response.Status.FORBIDDEN);
        }
    }
}
