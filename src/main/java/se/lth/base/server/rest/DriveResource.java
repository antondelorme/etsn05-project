package se.lth.base.server.rest;

import se.lth.base.server.Config;
import se.lth.base.server.data.*;
import se.lth.base.server.mail.MailHandler;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

@Path("drive")
public class DriveResource {
    private final boolean IS_DRIVER = true;
    private final boolean IS_ACCEPTED = true;
    private final boolean IS_RATED = true;

    private final DriveDataAccess driveDao = new DriveDataAccess(Config.instance().getDatabaseDriver());
    private final DriveUserDataAccess driveUserDao = new DriveUserDataAccess(Config.instance().getDatabaseDriver());
    private final DriveMilestoneDataAccess driveMilestoneDao = new DriveMilestoneDataAccess(Config.instance().getDatabaseDriver());
    private final DriveReportDataAccess driveReportDao = new DriveReportDataAccess(Config.instance().getDatabaseDriver());
    private final UserDataAccess userDao = new UserDataAccess(Config.instance().getDatabaseDriver());
    private final MailHandler mailHandler = new MailHandler();
    private final User user;

    public DriveResource(@Context ContainerRequestContext context) {
        this.user = (User) context.getProperty(User.class.getSimpleName());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RolesAllowed(Role.Names.USER)
    public DriveWrap createDrive(DriveWrap driveWrap) {
        Drive drive = driveWrap.getDrive();
        List<DriveMilestone> milestones = driveWrap.getMilestones();

        // Add drive
        drive = driveDao.addDrive(drive);

        List<DriveMilestone> returningMilestones = new ArrayList<>();

        // Add all milestones
        for (DriveMilestone m : milestones)
            returningMilestones.add(driveMilestoneDao.addMilestone(drive.getDriveId(), m.getMilestone(), m.getDepartureTime()));

        // Add driver to list of users
        List<DriveUser> users = new ArrayList<DriveUser>();
        users.add(driveUserDao.addDriveUser(drive.getDriveId(), user.getId(), drive.getStart(), drive.getStop(), IS_DRIVER, IS_ACCEPTED, !IS_RATED));

        // No reports yet
        List<DriveReport> reports = new ArrayList<>();

        // Check if any SearchFilters match with this drive, and if so notify these users by email
        new SearchResource(user).matchDriveWithSearchFilters(drive);

        return new DriveWrap(drive, milestones, users, reports);
    }

    @Path("{driveId}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @RolesAllowed(Role.Names.USER)
    public Drive putDrive(@PathParam("driveId") int driveId, Drive drive) throws URISyntaxException {
        if (driveUserDao.getDriveUser(driveId, user.getId()).isDriver())
            return driveDao.updateDrive(drive);

        throw new WebApplicationException("Only driver allowed to update drive", Status.UNAUTHORIZED);
    }

    @Path("{driveId}")
    @GET
    @RolesAllowed(Role.Names.USER)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public DriveWrap getDrive(@PathParam("driveId") int driveId) throws URISyntaxException {
        Drive drive = driveDao.getDrive(driveId);
        List<DriveUser> users = driveUserDao.getDriveUsersForDrive(driveId);
        List<DriveMilestone> milestones = driveMilestoneDao.getMilestonesForDrive(driveId);
        List<DriveReport> reports = user.getRole().clearanceFor(Role.ADMIN) ? driveReportDao.getDriveReportsForDrive(driveId) : new ArrayList<DriveReport>();

        return new DriveWrap(drive, milestones, users, reports);
    }

    @Path("all")
    @GET
    @RolesAllowed(Role.Names.ADMIN)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public List<Drive> getDrives() {
        return driveDao.getDrives();
    }

    @Path("{driveId}")
    @DELETE
    @RolesAllowed(Role.Names.USER)
    public void deleteDrive(@PathParam("driveId") int driveId) {
        if (!driveUserDao.getDriveUser(driveId, user.getId()).isDriver()) {
            throw new WebApplicationException("Only driver allowed to delete drive", Status.UNAUTHORIZED);
        }
        Drive drive = driveDao.getDrive(driveId);
        List<DriveUser> users = driveUserDao.getDriveUsersForDrive(driveId);
        List<DriveMilestone> milestones = driveMilestoneDao.getMilestonesForDrive(driveId);
        List<DriveReport> reports = driveReportDao.getDriveReportsForDrive(driveId);

        DriveWrap driveWrap = new DriveWrap(drive, milestones, users, reports);

        try {
            mailHandler.notifyPassengersDriverCancelledDrive(driveWrap);
        } catch (IOException e) {
            e.printStackTrace();
        }
        driveDao.deleteDrive(driveId);
    }

    @Path("{driveId}/user")
    @POST
    @RolesAllowed(Role.Names.USER)
    @Consumes(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public DriveUser addUserToDrive(@PathParam("driveId") int driveId, DriveUser driveUser) {
        if (driveDao.getDrive(driveId).getCarNumberOfSeats() > driveUserDao.getNumberOfUsersInDrive(driveId)) {
            Drive drive = driveDao.getDrive(driveId);
            List<DriveUser> users = driveUserDao.getDriveUsersForDrive(driveId);
            List<DriveMilestone> milestones = driveMilestoneDao.getMilestonesForDrive(driveId);
            List<DriveReport> reports = driveReportDao.getDriveReportsForDrive(driveId);

            DriveWrap driveWrap = new DriveWrap(drive, milestones, users, reports);

            try {
                mailHandler.notifyDriverNewPassengerOnTrip(driveWrap);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return driveUserDao.addDriveUser(driveId, user.getId(), driveUser.getStart(), driveUser.getStop(), !IS_DRIVER, !IS_ACCEPTED, !IS_RATED);
        }

        throw new WebApplicationException("No available seats left", Status.PRECONDITION_FAILED);
    }

    @Path("{driveId}/user/{userId}")
    @PUT
    @RolesAllowed(Role.Names.USER)
    public DriveUser acceptUserInDrive(@PathParam("driveId") int driveId, @PathParam("userId") int userId) {
        if (driveUserDao.getDriveUser(driveId, user.getId()).isDriver()) {
            driveUserDao.acceptDriveUser(driveId, userId);

            Drive drive = driveDao.getDrive(driveId);
            List<DriveUser> users = driveUserDao.getDriveUsersForDrive(driveId);
            List<DriveMilestone> milestones = driveMilestoneDao.getMilestonesForDrive(driveId);
            List<DriveReport> reports = driveReportDao.getDriveReportsForDrive(driveId);

            DriveWrap driveWrap = new DriveWrap(drive, milestones, users, reports);

            try {
                mailHandler.notifyPassengerBookingConfirmed(driveWrap, userDao.getUser(userId));
            } catch (IOException e) {
                e.printStackTrace();
            }

            return driveUserDao.getDriveUser(driveId, userId);
        }

        throw new WebApplicationException("Only driver allowed to accept passengers", Status.UNAUTHORIZED);
    }

    @Path("{driveId}/user/{userId}")
    @DELETE
    @RolesAllowed(Role.Names.USER)
    public void removeUserFromDrive(@PathParam("driveId") int driveId, @PathParam("userId") int userId) {
        if (!driveUserDao.getDriveUser(driveId, user.getId()).isDriver() && user.getId() != userId) {
            throw new WebApplicationException("Only driver or yourself allowed to delete", Status.UNAUTHORIZED);
        }

        Drive drive = driveDao.getDrive(driveId);
        List<DriveUser> users = driveUserDao.getDriveUsersForDrive(driveId);
        List<DriveMilestone> milestones = driveMilestoneDao.getMilestonesForDrive(driveId);
        List<DriveReport> reports = driveReportDao.getDriveReportsForDrive(driveId);

        DriveWrap driveWrap = new DriveWrap(drive, milestones, users, reports);

        if (driveUserDao.getDriveUser(driveId, user.getId()).isDriver()) {
            try {
                mailHandler.notifyPassengerDriverRemovedPassenger(driveWrap, userDao.getUser(userId));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (user.getId() == userId) {
            try {
                mailHandler.notifyDriverPassengerCancelledTrip(driveWrap, userDao.getUser(userId));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        driveUserDao.deleteDriveUser(driveId, userId);
    }

    @Path("{driveId}/rate/{rating}")
    @PUT
    @RolesAllowed(Role.Names.USER)
    @Consumes(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public void rateUsers(@PathParam("driveId") int driveId, @PathParam("rating") int rating) {
    	if (!driveUserDao.getDriveUser(driveId, user.getId()).hasRated()) {

            List<DriveUser> driveUsers = driveUserDao.getDriveUsersForDrive(driveId);

            for (DriveUser dU : driveUsers) {
                if (dU.isDriver()) {
                    userDao.updateUserRating(user.getId(), rating);
                }
            }
            driveUserDao.hasRated(user.getId(), driveId);
    	}
    	throw new WebApplicationException("You have already rated", Status.UNAUTHORIZED);
    }

    @Path("{driveId}/report")
    @POST
    @RolesAllowed(Role.Names.USER)
    @Consumes(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public DriveReport reportDrive(@PathParam("driveId") int driveId, DriveReport driveReport) {
        return driveReportDao.addDriveReport(driveId, user.getId(), driveReport.getReportMessage());
    }

    @Path("all-reports")
    @GET
    @RolesAllowed(Role.Names.ADMIN)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public List<DriveWrap> getReportedDrives() {
        List<DriveWrap> driveWraps = new ArrayList<DriveWrap>();
        List<Drive> reportedDrives = driveDao.getReportedDrives();

        for (Drive d : reportedDrives) {
            List<DriveMilestone> milestones = driveMilestoneDao.getMilestonesForDrive(d.getDriveId());
            List<DriveUser> users = driveUserDao.getDriveUsersForDrive(d.getDriveId());
            List<DriveReport> reports = driveReportDao.getDriveReportsForDrive(d.getDriveId());
            driveWraps.add(new DriveWrap(d, milestones, users, reports));
        }
        return driveWraps;
    }

    @Path("user/{userId}")
    @GET
    @RolesAllowed(Role.Names.USER)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public List<DriveWrap> getDrivesForUser(@PathParam("userId") int userId) {
        if (userId == user.getId() || user.getRole().clearanceFor(Role.ADMIN)) {
        	List<DriveWrap> driveWraps = new ArrayList<DriveWrap>();
        	List<Drive> drives = driveDao.getDrivesForUser(userId);
        	
        	for(Drive d : drives) {
        		List<DriveMilestone> milestones = driveMilestoneDao.getMilestonesForDrive(d.getDriveId());
        		List<DriveUser> users = driveUserDao.getDriveUsersForDrive(d.getDriveId());
        		List<DriveReport> reports = driveReportDao.getDriveReportsForDrive(d.getDriveId());
        		driveWraps.add(new DriveWrap(d, milestones, users, reports));
        	}
            return driveWraps;
        }
        throw new WebApplicationException("You do not have access to these drives", Status.UNAUTHORIZED);
    }


    @Path("count/{userId}")
    @GET
    @RolesAllowed(Role.Names.USER)
    @Produces(MediaType.TEXT_PLAIN)
    public int getNumberOfDrives(@PathParam("userId") int userId) {
        return driveUserDao.getNumberOfDrivesForUser(userId);
    }
}
