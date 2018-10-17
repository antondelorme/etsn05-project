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
import java.util.Iterator;
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
    public Drive putDrive(@PathParam("driveId") int driveId, Drive drive) {
        if (driveUserDao.getDriveUser(driveId, user.getId()).isDriver())
            return driveDao.updateDrive(drive);

        throw new WebApplicationException("Only driver allowed to update drive", Status.UNAUTHORIZED);
    }

    @Path("{driveId}")
    @GET
    @RolesAllowed(Role.Names.USER)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public DriveWrap getDrive(@PathParam("driveId") int driveId) {
        Drive drive = driveDao.getDrive(driveId);
        List<DriveUser> users = driveUserDao.getDriveUsersForDrive(driveId);
        List<DriveMilestone> milestones = driveMilestoneDao.getMilestonesForDrive(driveId);
        List<DriveReport> reports = user.getRole().clearanceFor(Role.ADMIN) ? driveReportDao.getDriveReportsForDrive(driveId) : new ArrayList<>();

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
        if (driveDao.getDrive(driveId).getCarNumberOfSeats() > driveUserDao.getNumberOfUsersInDrive(driveId)) {
            List<Drive> collidingDrives = checkBookingOverlap(driveId, driveUser.getStart(), driveUser.getStop());
            if (collidingDrives.isEmpty()) {
                return driveUserDao.addDriveUser(driveId, user.getId(), driveUser.getStart(), driveUser.getStop(), !IS_DRIVER, !IS_ACCEPTED, !IS_RATED);
            } else {
                throw new WebApplicationException("This drive overlaps in time with other booked drives", Status.PRECONDITION_FAILED);
            }
        } else {
            throw new WebApplicationException("No available seats left", Status.PRECONDITION_FAILED);
        }
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
                if (driveUserDao.getDriveUser(driveId, userId).isAccepted()) {
                    mailHandler.notifyPassengerDriverRemovedPassenger(driveWrap, userDao.getUser(userId));
                } else {
                    mailHandler.notifyPassengerDriverRemovedPassenger(driveWrap, userDao.getUser(userId));
                }

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
            return;
    	}
    	throw new WebApplicationException("You have already rated", Status.UNAUTHORIZED);
    }

    @Path("{driveId}/report")
    @POST
    @RolesAllowed(Role.Names.USER)
    @Consumes(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public DriveReport reportDrive(@PathParam("driveId") int driveId, DriveReport driveReport) {
        try {
            mailHandler.notifyUserHasBeenWarned(user);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return driveReportDao.addDriveReport(driveId, user.getId(), driveReport.getReportMessage());
    }

    @Path("all-reports")
    @GET
    @RolesAllowed(Role.Names.ADMIN)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public List<DriveWrap> getReportedDrives() {
        List<DriveWrap> driveWraps = new ArrayList<>();
        List<Drive> reportedDrives = driveDao.getReportedDrives();
        attachDriveDetails(reportedDrives, driveWraps);

        return driveWraps;
    }

    @Path("user/{userId}")
    @GET
    @RolesAllowed(Role.Names.USER)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public List<DriveWrap> getDrivesForUser(@PathParam("userId") int userId) {
        if (userId == user.getId() || user.getRole().clearanceFor(Role.ADMIN)) {
        	List<DriveWrap> driveWraps = new ArrayList<>();
        	List<Drive> drives = driveDao.getDrivesForUser(userId);
        	attachDriveDetails(drives, driveWraps);

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

    private void attachDriveDetails(List<Drive> drives, List<DriveWrap> driveWraps) {
        for(Drive d : drives) {
            List<DriveMilestone> milestones = driveMilestoneDao.getMilestonesForDrive(d.getDriveId());
            List<DriveUser> users = driveUserDao.getDriveUsersForDrive(d.getDriveId());
            List<DriveReport> reports = driveReportDao.getDriveReportsForDrive(d.getDriveId());
            driveWraps.add(new DriveWrap(d, milestones, users, reports));
        }
    }

    /**
     * Checks if it is possible for a user to book a specific Drive.
     *
     * @param driveId the id of the drive that one wishes to see if it is possible to book
     * @param start   at what milestone the user will be starting at
     * @param stop    at what milestone the user will stop at
     * @return a list of drives associated with this user, conflicting with the entered drive
     */
    private List<Drive> checkBookingOverlap(@PathParam("driveId") int driveId, @PathParam("start") String start, @PathParam("stop") String stop) {
        Drive drive = driveDao.getDrive(driveId);
        List<DriveMilestone> driveMilestones = driveMilestoneDao.getMilestonesForDrive(driveId);
        // Add drive start and stop as milestones to list
        driveMilestones.add(0, new DriveMilestone(-1, -1, drive.getStart(), drive.getDepartureTime()));
        driveMilestones.add(new DriveMilestone(-1, -1, drive.getStop(), drive.getArrivalTime()));

        // Find the time that the user would be busy if user participated in drive
        long busyTimeStart = -1;
        long busyTimeEnd = -1;

        for (DriveMilestone dm : driveMilestones) {
            if (dm.getMilestone().toLowerCase().trim().equals(start.toLowerCase().trim())) {
                busyTimeStart = dm.getDepartureTime();
            } else if (dm.getMilestone().toLowerCase().trim().equals(stop.toLowerCase().trim())) {
                busyTimeEnd = dm.getDepartureTime();
            }
        }

        // Check for time conflicts with other already booked drives/trips
        List<Drive> associatedDrives = getDriveAssociatedWithUser(user.getId());
        Iterator<Drive> driveIterator = associatedDrives.iterator();
        while (driveIterator.hasNext()) {
            Drive d = driveIterator.next();
            List<DriveMilestone> tempDriveMilestones = driveMilestoneDao.getMilestonesForDrive(d.getDriveId());
            // Add drive start and stop as milestones to list
            tempDriveMilestones.add(0, new DriveMilestone(-1, -1, d.getStart(), d.getDepartureTime()));
            tempDriveMilestones.add(new DriveMilestone(-1, -1, d.getStop(), d.getArrivalTime()));

            // Find the time that the user is busy in this drive
            long tempBusyTimeStart = -1;
            long tempBusyTimeEnd = -1;

            DriveUser driveUser = driveUserDao.getDriveUser(d.getDriveId(), user.getId());

            for (DriveMilestone dm : tempDriveMilestones) {
                if (dm.getMilestone().toLowerCase().trim().equals(driveUser.getStart().toLowerCase().trim())) {
                    tempBusyTimeStart = dm.getDepartureTime();
                } else if (dm.getMilestone().toLowerCase().trim().equals(driveUser.getStop().toLowerCase().trim())) {
                    tempBusyTimeEnd = dm.getDepartureTime();
                }
            }

            if (tempBusyTimeEnd < busyTimeStart || tempBusyTimeStart > busyTimeEnd) {
                // No conflict
                driveIterator.remove();
            }
        }

        // Return all booked drives that are in conflict with the new booking
        return associatedDrives;
    }

    private List<Drive> getDriveAssociatedWithUser(int userId) {
        List<Drive> drives = driveDao.getDrives();
        Iterator<Drive> iterator = drives.iterator();
        while (iterator.hasNext()) {
            Drive drive = iterator.next();
            List<DriveUser> driveUsers = driveUserDao.getDriveUsersForDrive(drive.getDriveId());
            for (int i = 0; i < driveUsers.size(); i++) {
                DriveUser driveUser = driveUsers.get(i);
                if (driveUser.getUserId() == userId) {
                    // User is associated with this drive, keep drive in list
                    break;
                }
                if (i + 1 == driveUsers.size()) {
                    // User is not associated with this drive, remove drive from list
                    iterator.remove();
                }
            }

        }
        return drives;
    }
}
