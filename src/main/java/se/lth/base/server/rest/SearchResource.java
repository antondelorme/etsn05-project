package se.lth.base.server.rest;

import se.lth.base.server.Config;
import se.lth.base.server.data.*;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
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
    private final User user;

    // A trip with departure time within interval 13.00-13.10 will match drive milestone with departure time 13.10
    private final int SEARCH_MINUTES_MARGIN = 10;

    public SearchResource(@Context ContainerRequestContext context) {
        this.user = (User) context.getProperty(User.class.getSimpleName());
    }

    /**
     * This method lets a user search for drives by specifying search parameters such as trip start, stop and departure time
     * in the form of a SearchFilter object.
     *
     * @param searchFilter is used to filter out possible drives. If the timestamp attribute in this object is null, then filtering will be done only on trip start and stop.
     *                    If trip start, trip stop are null and departure time is equal to -1, then all drives will be returned in the order of most recently added drive first.
     * @return A list of Drive-objects matching the input arguments.
     */
    @Path("getDrives")
    @POST
    @RolesAllowed(Role.Names.USER)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public List<Drive> getDrives(SearchFilter searchFilter) {

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

        return filteredDrives;
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

            // Make sure that tripStart and tripStop exists in driveMilestones and that they are not the same and that tripStart is before tripStop
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
            if(checkMilestoneIntervalOverlap(driveMilestones, driveUsers, carSeats)) {
                iterator.remove();
                continue;
            }

            // Check if so that departure time of passenger matches with departure time of milestone set by driver
            if (departureTime != null) {
                if (!checkDepartureTimeMatch(departureTime, tripStart, driveMilestones)) {
                    iterator.remove();
                    continue;
                }
            }
        }
        return drives;
    }

    private boolean doesTripStartExist(String tripStart, List<DriveMilestone> driveMilestones) {
        for (DriveMilestone driveMilestone : driveMilestones) {
            if (driveMilestone.getMilestone().equals(tripStart)) return true;
        }
        return false;
    }

    private boolean doesTripStopExist(String tripStop, List<DriveMilestone> driveMilestones) {
        for (DriveMilestone driveMilestone : driveMilestones) {
            if (driveMilestone.getMilestone().equals(tripStop)) return true;
        }
        return false;
    }

    private boolean isTripStartSameAsTripStop(String tripStart, String tripStop) {
        return tripStart.equals(tripStop);
    }

    private boolean isTripStartBeforeTripStop(String tripStart, String tripStop, List<DriveMilestone> driveMilestones) {
        for (DriveMilestone driveMilestone : driveMilestones) {
            if (driveMilestone.getMilestone().equals(tripStart)) {
                return true;
            } else if (driveMilestone.getMilestone().equals(tripStop)) {
                return false;
            }
        }
        return false;
    }

    private boolean checkMilestoneIntervalOverlap(List<DriveMilestone> milestones, List<DriveUser> driveUsers, int carSeats) {
        // Create DriveUserIntervals
        List<DriveUserInterval> driveUserIntervals = new ArrayList<>();
        for(DriveUser driveUser : driveUsers) {
            driveUserIntervals.add(new DriveUserInterval(driveUser.getStart(), driveUser.getStop(), milestones));
        }

        for(int i = 0; i < milestones.size() - 1; i++) {
            // Seats taken in this interval
            int seatsTaken = 0;
            int start = i;
            int stop = i + 1;
            for(DriveUserInterval driveUserInterval : driveUserIntervals) {
                int startIndex = driveUserInterval.getStartIndex();
                int endIndex = driveUserInterval.getStopIndex();
                if(startIndex <= start && endIndex >= stop) {
                    seatsTaken++;
                }
            }
            if(seatsTaken > carSeats) {
                // The car will be too full at some interval if the new passenger is added!
                return true;
            }
        }
        return false;
    }

    private boolean checkDepartureTimeMatch(Timestamp departureTime, String startMiletone, List<DriveMilestone> driveMilestones) {
        // Find the departure time of the milestone
        for (DriveMilestone driveMilestone : driveMilestones) {
            if (driveMilestone.getMilestone().equals(startMiletone)) {
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
            for(int i = 0; i < milestones.size(); i++) {
                if(milestones.get(i).getMilestone().equals(name)) {
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