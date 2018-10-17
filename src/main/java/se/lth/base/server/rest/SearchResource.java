package se.lth.base.server.rest;

import se.lth.base.server.Config;
import se.lth.base.server.data.*;
import se.lth.base.server.mail.MailHandler;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@Path("search")
public class SearchResource {

    private final DriveDataAccess driveDao = new DriveDataAccess(Config.instance().getDatabaseDriver());
    private final DriveUserDataAccess driveUserDao = new DriveUserDataAccess(Config.instance().getDatabaseDriver());
    private final DriveMilestoneDataAccess driveMilestoneDao = new DriveMilestoneDataAccess(Config.instance().getDatabaseDriver());
    private final UserDataAccess userDao = new UserDataAccess(Config.instance().getDatabaseDriver());
    private final SearchFilterDataAccess searchFilterDao = new SearchFilterDataAccess(Config.instance().getDatabaseDriver());
    private final MailHandler mailHandler = new MailHandler();
    private final User user;

    // A trip with departure time within interval 13.00-14.00 will match drive milestone with departure time 14.00
    private final int SEARCH_MINUTES_MARGIN = 60;

    public SearchResource(@Context ContainerRequestContext context) {
        this.user = (User) context.getProperty(User.class.getSimpleName());
    }

    public SearchResource(User user) {
        this.user = user;
    }

    /**
     * This method lets a user search for drives by specifying search parameters such as trip start, stop and departure time
     * in the form of a SearchFilter object.
     *
     * @param searchFilter is used to filter out possible drives. If the timestamp attribute in this object is null, then filtering will be done only on trip start and stop.
     *                     If trip start, trip stop are null and departure time is equal to -1, then all drives will be returned in the order of most recently added drive first.
     * @return A list of Drive-objects matching the input arguments.
     */
    @Path("drives")
    @POST
    @RolesAllowed(Role.Names.USER)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Consumes(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public List<DriveWrap> getDrives(SearchFilter searchFilter) {

        // Get requested trip start, stop and departure
        String tripStart = searchFilter.getStart();
        String tripStop = searchFilter.getStop();
        long departure = searchFilter.getDepartureTime();
        Timestamp departureTime = null;
        if (departure != -1) {
            departureTime = new Timestamp(departure);
        }

        // Get all drives matching start and end point of search
        List<Drive> filteredDrives = filterDrivesMatchingTrip(tripStart, tripStop, departureTime);

        // Remove drives with too many drive users
        removeDrivesWithTooManyDriveUsers(filteredDrives);

        // Turn into DriveWraps
        List<DriveWrap> driveWraps = new ArrayList<>();
        for (Drive d : filteredDrives) {
            List<DriveMilestone> driveMilestones = driveMilestoneDao.getMilestonesForDrive(d.getDriveId());
            List<DriveUser> driveUsers = driveUserDao.getDriveUsersForDrive(d.getDriveId());
            driveWraps.add(new DriveWrap(d, driveMilestones, driveUsers, null));
        }

        return driveWraps;
    }

    /**
     * This method lets an admin search for users by specifying a name and/or an email.
     * If both name and email is left blank then all users will be returned.
     *
     * @param name  the name of the user(s) searched for. The method expects the following format (firstName + " " + lastName)
     *              to be entered and will use the String function equals(String) to compare against (firstName + " " + lastName) received from a User object.
     * @param email the email of the user(s) searched for.
     * @return A list of users matching the input arguments.
     */
    @Path("getUsers/{name: .*}/{email: .*}") // .* is used to accept an empty name
    @GET
    @RolesAllowed(Role.Names.ADMIN)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public List<User> getUsers(@PathParam("name") String name, @PathParam("email") String email) {
        List<User> users = userDao.getUsers();

        // Check for search input (if none, then return full user list)
        if ((name == null || name.isEmpty()) && (email == null || email.isEmpty())) {
            return users;
        }

        // Match against name
        if (name != null && !name.isEmpty()) {
            // Set name to lower case characters
            name = name.toLowerCase();
            Iterator<User> iterator = users.iterator();
            while (iterator.hasNext()) {
                User user = iterator.next();
                String tempName = (user.getFirstName() + " " + user.getLastName()).toLowerCase();
                if (!tempName.contains(name)) {
                    iterator.remove();
                }
            }
        }

        // Match against email
        if (email != null && !email.isEmpty()) {
            // Set email to lower case characters
            email = email.toLowerCase();
            Iterator<User> iterator = users.iterator();
            while (iterator.hasNext()) {
                User user = iterator.next();
                String tempEmail = user.getEmail().toLowerCase();
                if (!tempEmail.contains(email)) {
                    iterator.remove();
                }
            }
        }

        return users;
    }

    /**
     * A user can call this method to subscribe to a search filter. Matches will be notified through email.
     *
     * @param searchFilter SearchFilter DTO.
     * @return a new SearchFilter with correct searchFilterId
     */
    @Path("subsribe")
    @POST
    @RolesAllowed(Role.Names.USER)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public SearchFilter subscribeToSearch(SearchFilter searchFilter) {
        return searchFilterDao.addSearchFilter(new SearchFilter(-1, user.getId(), searchFilter.getStart(), searchFilter.getStop(), searchFilter.getDepartureTime()));
    }

    /**
     * This method will check for any possible matches between a Drive and existing SearchFilters.
     * Matches will be notified through email.
     *
     * @param drive The drive one wishes to check matches against.
     */
    public void matchDriveWithSearchFilters(Drive drive) {
        List<SearchFilter> searchFilters = searchFilterDao.getSearchFilters();
        for (SearchFilter searchFilter : searchFilters) {
            List<DriveWrap> drivesMatching = getDrives(searchFilter);
            for (DriveWrap driveWrap : drivesMatching) {
                Drive tempDrive = driveWrap.getDrive();
                if (tempDrive.getDriveId() == drive.getDriveId()) {
                    // Match found, notify user by email
                    try {
                        mailHandler.notifyUserSearchFilterMatch(driveWrap, userDao.getUser(searchFilter.getUserId()), searchFilter);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void removeDrivesWithTooManyDriveUsers(List<Drive> drives) {
        Iterator<Drive> iterator = drives.iterator();
        while (iterator.hasNext()) {
            Drive drive = iterator.next();
            int numberOfSeats = drive.getCarNumberOfSeats();
            int usersInDrive = driveUserDao.getNumberOfUsersInDrive(drive.getDriveId()) - 1; // Remove driver
            if (numberOfSeats == usersInDrive) {
                iterator.remove();
            }
        }
    }

    private List<Drive> filterDrivesMatchingTrip(String tripStart, String tripStop, Timestamp departureTime) {
        // Get all drives
        List<Drive> drives = driveDao.getDrives();
        Iterator<Drive> iterator = drives.iterator();

        if ((tripStart == null || tripStart.isEmpty()) && (tripStop == null || tripStop.isEmpty()) && departureTime == null) {
            // Drives are returned in most recently created drive first (need a created date in Drive class)
            Collections.reverse(drives);
            return drives;
        }

        // Loop through all drives
        while (iterator.hasNext()) {
            Drive drive = iterator.next();
            // Create drive stop and start as "DriveMilestones" and add the to the list
            // Milestone id is not of interest and is therefore set to -1
            DriveMilestone driveStart = new DriveMilestone(-1, drive.getDriveId(), drive.getStart(), drive.getDepartureTime());
            // driveStop does not need an "arrival" time
            DriveMilestone driveStop = new DriveMilestone(-1, drive.getDriveId(), drive.getStop(), -1);
            List<DriveMilestone> driveMilestones = driveMilestoneDao.getMilestonesForDrive(drive.getDriveId());
            driveMilestones.add(0, driveStart);
            driveMilestones.add(driveStop);

            if (tripStart == null) {
                tripStart = driveMilestones.get(0).getMilestone();
            }

            if (tripStop == null) {
                tripStop = driveMilestones.get(driveMilestones.size() - 1).getMilestone();
            }

            if (!(doesTripStartExist(tripStart, driveMilestones) && doesTripStopExist(tripStop, driveMilestones) &&
                    !isTripStartSameAsTripStop(tripStart, tripStop) && isTripStartBeforeTripStop(tripStart, tripStop, driveMilestones))) {
                iterator.remove();
                continue;
            }

            // Get all DriveUsers associated with the current drive
            List<DriveUser> driveUsers = driveUserDao.getDriveUsersForDrive(drive.getDriveId());

            // Remove driver from driveUsers list
            Iterator<DriveUser> driveUsersIterator = driveUsers.iterator();
            while (driveUsersIterator.hasNext()) {
                DriveUser driveUser = driveUsersIterator.next();
                if (driveUser.isDriver()) {
                    driveUsersIterator.remove();
                    break;
                }
            }

            // Add the potentially new passenger to the driveUser list for this drive
            driveUsers.add(new DriveUser(-1, -1, tripStart, tripStop, false, false, false));
            int carSeats = drive.getCarNumberOfSeats();

            // Check if too many drive users start and stop overlap (seats taken > max seats)
            if (checkMilestoneIntervalOverlap(driveMilestones, driveUsers, carSeats)) {
                iterator.remove();
                continue;
            }

            // Check if so that departure time of passenger matches with departure time of milestone set by driver
            if (departureTime != null) {
                if (!checkDepartureTimeMatch(departureTime, tripStart, driveMilestones)) {
                    iterator.remove();
                }
            }
        }
        return drives;
    }

    private boolean doesTripStartExist(String tripStart, List<DriveMilestone> driveMilestones) {
        for (DriveMilestone driveMilestone : driveMilestones) {
            if (driveMilestone.getMilestone().toLowerCase().trim().equals(tripStart.toLowerCase().trim())) return true;
        }
        return false;
    }

    private boolean doesTripStopExist(String tripStop, List<DriveMilestone> driveMilestones) {
        for (DriveMilestone driveMilestone : driveMilestones) {
            if (driveMilestone.getMilestone().toLowerCase().trim().equals(tripStop.toLowerCase().trim())) return true;
        }
        return false;
    }

    private boolean isTripStartSameAsTripStop(String tripStart, String tripStop) {
        return tripStart.toLowerCase().trim().equals(tripStop.toLowerCase().trim());
    }

    private boolean isTripStartBeforeTripStop(String tripStart, String tripStop, List<DriveMilestone> driveMilestones) {
        for (DriveMilestone driveMilestone : driveMilestones) {
            if (driveMilestone.getMilestone().toLowerCase().trim().equals(tripStart.toLowerCase().trim())) {
                return true;
            } else if (driveMilestone.getMilestone().toLowerCase().trim().equals(tripStop.toLowerCase().trim())) {
                return false;
            }
        }
        return false;
    }

    private boolean checkMilestoneIntervalOverlap(List<DriveMilestone> milestones, List<DriveUser> driveUsers, int carSeats) {
        // Create DriveUserIntervals
        List<DriveUserInterval> driveUserIntervals = new ArrayList<>();
        for (DriveUser driveUser : driveUsers) {
            driveUserIntervals.add(new DriveUserInterval(driveUser.getStart(), driveUser.getStop(), milestones));
        }

        for (int start = 0; start < milestones.size() - 1; start++) {
            // Seats taken in this interval
            int seatsTaken = 0;
            int stop = start + 1;
            for (DriveUserInterval driveUserInterval : driveUserIntervals) {
                int startIndex = driveUserInterval.getStartIndex();
                int endIndex = driveUserInterval.getStopIndex();
                if (startIndex <= start && endIndex >= stop) {
                    seatsTaken++;
                }
            }
            if (seatsTaken > carSeats) {
                // The car will be too full at some interval if the new passenger is added!
                return true;
            }
        }
        return false;
    }

    private boolean checkDepartureTimeMatch(Timestamp departureTime, String startMilestone, List<DriveMilestone> driveMilestones) {
        // Find the departure time of the milestone
        for (DriveMilestone driveMilestone : driveMilestones) {
            if (driveMilestone.getMilestone().toLowerCase().trim().equals(startMilestone.toLowerCase().trim())) {
                Timestamp milestoneDepartureTime = new Timestamp(driveMilestone.getDepartureTime());

                Timestamp timeMargin = new Timestamp(milestoneDepartureTime.getTime() - SEARCH_MINUTES_MARGIN * 60 * 1000);
                if ((timeMargin.before(departureTime) || timeMargin.equals(departureTime)) &&
                        (departureTime.before(milestoneDepartureTime) || departureTime.equals(milestoneDepartureTime))) {
                    return true;
                }
            }
        }
        return false;
    }

    private class DriveUserInterval {
        private int startIndex;
        private int stopIndex;

        public DriveUserInterval(String start, String stop, List<DriveMilestone> milestones) {
            startIndex = getIndexOfMilestone(start, milestones);
            stopIndex = getIndexOfMilestone(stop, milestones);
        }

        private int getIndexOfMilestone(String name, List<DriveMilestone> milestones) {
            for (int i = 0; i < milestones.size(); i++) {
                if (milestones.get(i).getMilestone().toLowerCase().trim().equals(name.toLowerCase().trim())) {
                    return i;
                }
            }
            return -1;
        }

        public int getStartIndex() {
            return startIndex;
        }

        public int getStopIndex() {
            return stopIndex;
        }
    }

}
