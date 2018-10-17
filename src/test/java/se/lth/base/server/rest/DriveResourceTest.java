package se.lth.base.server.rest;

import org.junit.Test;
import se.lth.base.server.BaseResourceTest;
import se.lth.base.server.Config;
import se.lth.base.server.data.*;

import javax.ws.rs.client.Entity;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.GenericType;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;


public class DriveResourceTest extends BaseResourceTest {

	private static final GenericType<List<Drive>> DRIVE_LIST = new GenericType<List<Drive>>() {
	};
	private static final GenericType<List<DriveWrap>> DRIVEWRAP_LIST = new GenericType<List<DriveWrap>>() {
	};


    private final int idStart = 3;
    private final User SEARCH_TEST_1 = new User(idStart, Role.USER, "test1@commutecompanion.se", "ST1FirstName", "ST1LastName", "+4670207579", 0, Date.valueOf("1995-01-01").getTime(), true, 0, 0, 0);
    private final Credentials SEARCH_TEST_CREDENTIALS_1 = new Credentials("test1@commutecompanion.se", "test", Role.USER, SEARCH_TEST_1);

    private final User SEARCH_TEST_2 = new User(idStart, Role.USER, "test2@commutecompanion.se", "ST2FirstName", "ST2LastName", "+4670207579", 0, Date.valueOf("1995-01-01").getTime(), true, 0, 0, 0);
    private final Credentials SEARCH_TEST_CREDENTIALS_2 = new Credentials("test2@commutecompanion.se", "test", Role.USER, SEARCH_TEST_2);

    @Test(expected = ForbiddenException.class)
    public void getDrivesAsUser() {
        login(TEST_CREDENTIALS);
        target("drive")
                .path("all")
                .request()
                .get(DRIVE_LIST);
    }

    @Test
    public void getDrivesAsAdmin() {
        login(ADMIN_CREDENTIALS);
        List<Drive> drives = target("drive")
                .path("all")
                .request()
                .get(DRIVE_LIST);
        assertTrue(drives.isEmpty());
    }

    @Test
    public void getDrivesForUserAsRightUser() {
        login(TEST_CREDENTIALS);
        List<DriveWrap> drives = target("drive")
                .path("user/" + TEST.getId())
                .request()
                .get(DRIVEWRAP_LIST);
        assertTrue(drives.isEmpty());

        //Add user to drive
        DriveDataAccess driveDao = new DriveDataAccess(Config.instance().getDatabaseDriver());
        Drive drive1 = new Drive(0, "A", "F", System.currentTimeMillis(), System.currentTimeMillis() + 1, "Comment", "x", "x", "x", "x", 1, 1, false, false, false);
        drive1 = driveDao.addDrive(drive1);
        int drive1Id = drive1.getDriveId();
        DriveUserDataAccess driveUserDao = new DriveUserDataAccess(Config.instance().getDatabaseDriver());
        driveUserDao.addDriveUser(drive1Id, TEST.getId(), "A", "B", false, false, false);

        drives = target("drive")
                .path("user/" + TEST.getId())
                .request()
                .get(DRIVEWRAP_LIST);
        assertEquals(1, drives.size());
        assertEquals(drives.get(0).getDrive().getDriveId(), drive1Id);
    }

    @Test(expected = WebApplicationException.class)
    public void getDrivesForUserAsWrongUser() {
        login(TEST_CREDENTIALS);
        List<DriveWrap> drives = target("drive")
                .path("user/" + ADMIN.getId())
                .request()
                .get(DRIVEWRAP_LIST);
    }

    @Test
    public void getDrivesForUserAsAdmin() {
        login(ADMIN_CREDENTIALS);
        List<DriveWrap> drives = target("drive")
                .path("user/" + TEST.getId())
                .request()
                .get(DRIVEWRAP_LIST);
        assertTrue(drives.isEmpty());

        //Add user to drive
        DriveDataAccess driveDao = new DriveDataAccess(Config.instance().getDatabaseDriver());
        Drive drive1 = new Drive(0, "A", "F", System.currentTimeMillis(), System.currentTimeMillis() + 1, "Comment", "x", "x", "x", "x", 1, 1, false, false, false);
        drive1 = driveDao.addDrive(drive1);
        int drive1Id = drive1.getDriveId();
        DriveUserDataAccess driveUserDao = new DriveUserDataAccess(Config.instance().getDatabaseDriver());
        driveUserDao.addDriveUser(drive1Id, TEST.getId(), "A", "B", false, false, false);

        login(ADMIN_CREDENTIALS);
        drives = target("drive")
                .path("user/" + TEST.getId())
                .request()
                .get(DRIVEWRAP_LIST);
        assertEquals(1, drives.size());
        assertEquals(drives.get(0).getDrive().getDriveId(), drive1Id);
    }

    @Test
    public void removeUserFromDrive() {
        DriveDataAccess driveDao = new DriveDataAccess(Config.instance().getDatabaseDriver());
        long departureTime = new Timestamp(2018 - 1900, 10, 20, 12, 0, 0, 0).getTime();
        long arrivalTime = new Timestamp(2018 - 1900, 10, 20, 12, 25, 0, 0).getTime();
        Drive drive1 = driveDao.addDrive(new Drive(-1, "A", "F", departureTime, arrivalTime, "Comment", "x", "x", "x", "x", 1, 1, false, false, false));
        int drive1Id = drive1.getDriveId();

        UserDataAccess userDao = new UserDataAccess(Config.instance().getDatabaseDriver());
        User user1 = userDao.addUser(SEARCH_TEST_CREDENTIALS_1);
        User user2 = userDao.addUser(SEARCH_TEST_CREDENTIALS_2);
        int user1Id = user1.getId();
        int user2Id = user2.getId();

        DriveUserDataAccess driveUserDao = new DriveUserDataAccess(Config.instance().getDatabaseDriver());
        driveUserDao.addDriveUser(drive1Id, user1Id, "A", "C", true, true, false);
        driveUserDao.addDriveUser(drive1Id, user2Id, "A", "C", false, true, false);

        login(SEARCH_TEST_CREDENTIALS_1);
        target("drive")
                .path(Integer.toString(drive1Id) + "/user/" + Integer.toString(user1Id))
                .request()
                .delete(Void.class);
        logout();
        login(ADMIN_CREDENTIALS);
        DriveWrap wrap = target("drive")
                .path(Integer.toString(drive1Id))
                .request()
                .get(DriveWrap.class);
        for (DriveUser user : wrap.getUsers()) {
            assertNotEquals(user.getUserId(), user1Id);
        }
    }
    
	@Test
	public void createAndUpdateDrive() {
		login(TEST_CREDENTIALS);
		long departureTime = new Timestamp(2018 - 1900, 10, 20, 12, 0, 0, 0).getTime();
        long arrivalTime = new Timestamp(2018 - 1900, 10, 20, 12, 25, 0, 0).getTime();
		Drive drive = new Drive(-1, "A", "F", departureTime, arrivalTime, "Comment", "x", "x", "x", "x", 1, 1, true, true, false);
		DriveWrap newDriveWrap= new DriveWrap(drive, new ArrayList<DriveMilestone>(), new ArrayList<DriveUser>(), new ArrayList<DriveReport>());
		newDriveWrap = target("drive")
				.request()
				.post(Entity.json(newDriveWrap), DriveWrap.class);
		DriveWrap actual = target("drive")
                .path(Integer.toString(newDriveWrap.getDrive().getDriveId()))
                .request()
                .get(DriveWrap.class);
		
		//Test if the correct drive was added
		assertEquals(actual.getDrive().getDriveId(), newDriveWrap.getDrive().getDriveId());
		assertEquals(actual.getDrive().getCarLicensePlate(), newDriveWrap.getDrive().getCarLicensePlate());
		
		int driveId = actual.getDrive().getDriveId();
		drive = new Drive(driveId, "A", "B", departureTime, arrivalTime, "Comment", "Audi", "Q8", "White Walker White", "ABC123", 4, 1, false, false, false);
		Drive updatedDrive = target("drive")
				.path(Integer.toString(driveId))
				.request()
				.put(Entity.json(drive), Drive.class);
		assertEquals(updatedDrive.getCarColor(), "White Walker White");
	}
	
	@Test(expected = WebApplicationException.class)
	public void updateDriveNotAsDriver() {
		login(TEST_CREDENTIALS);
		long departureTime = new Timestamp(2018 - 1900, 10, 20, 12, 0, 0, 0).getTime();
        long arrivalTime = new Timestamp(2018 - 1900, 10, 20, 12, 25, 0, 0).getTime();
		Drive drive = new Drive(-1, "A", "F", departureTime, arrivalTime, "Comment", "x", "x", "x", "x", 2, 1, true, true, false);
		DriveWrap newDriveWrap= new DriveWrap(drive, new ArrayList<DriveMilestone>(), new ArrayList<DriveUser>(), new ArrayList<DriveReport>());
		newDriveWrap = target("drive")
				.request()
				.post(Entity.json(newDriveWrap), DriveWrap.class);
		logout();
		login(ADMIN_CREDENTIALS);
		int driveId = newDriveWrap.getDrive().getDriveId();
		DriveUser du = new DriveUser(driveId, ADMIN.getId(), "A", "F", false, false, false);
		DriveUser driveUser = target("drive")
				.path(Integer.toString(driveId) + "/user")
				.request()
				.post(Entity.json(du), DriveUser.class);
		
		drive = new Drive(driveId, "A", "B", departureTime, arrivalTime, "Comment", "Audi", "Q8", "White Walker White", "ABC123", 4, 1, false, false, false);
		Drive updatedDrive = target("drive")
				.path(Integer.toString(driveId))
				.request()
				.put(Entity.json(drive), Drive.class);
	}
	
	@Test(expected = NotFoundException.class)
	public void deleteDrive() {
		login(TEST_CREDENTIALS);
		long departureTime = new Timestamp(2018 - 1900, 10, 20, 12, 0, 0, 0).getTime();
        long arrivalTime = new Timestamp(2018 - 1900, 10, 20, 12, 25, 0, 0).getTime();
		Drive drive = new Drive(-1, "A", "F", departureTime, arrivalTime, "Comment", "x", "x", "x", "x", 2, 1, true, true, false);
		DriveWrap newDriveWrap= new DriveWrap(drive, new ArrayList<DriveMilestone>(), new ArrayList<DriveUser>(), new ArrayList<DriveReport>());
		newDriveWrap = target("drive")
				.request()
				.post(Entity.json(newDriveWrap), DriveWrap.class);
		int driveId = newDriveWrap.getDrive().getDriveId();
		target("drive")
				.path(Integer.toString(driveId))
				.request()
				.delete(Void.class);
		target("drive")
                .path(Integer.toString(driveId))
                .request()
                .get(DriveWrap.class);
	}
	
	@Test
    public void reportDriveAndgetAllReports() {
    	login(ADMIN_CREDENTIALS);
    	long departureTime = new Timestamp(2018 - 1900, 10, 20, 12, 0, 0, 0).getTime();
        long arrivalTime = new Timestamp(2018 - 1900, 10, 20, 12, 25, 0, 0).getTime();
        Drive drive = new Drive(-1, "A", "B", departureTime, arrivalTime, "Comment", "x", "x", "x", "x", 4, 1, false, false, false);
        DriveWrap newDriveWrap= new DriveWrap(drive, new ArrayList<DriveMilestone>(), new ArrayList<DriveUser>(), new ArrayList<DriveReport>());
		newDriveWrap = target("drive")
				.request()
				.post(Entity.json(newDriveWrap), DriveWrap.class);
		//Look for reported drives, expect 0
		List<DriveWrap> reportWraps = target("drive")
				.path("all-reports")
				.request()
				.get(DRIVEWRAP_LIST);
		assertTrue(reportWraps.isEmpty());
		//Add report
		int driveId = newDriveWrap.getDrive().getDriveId();
		DriveReport report = new DriveReport(-1, driveId, ADMIN.getId(), "Driving like a mad man");
		DriveReport newReport = target("drive")
				.path(Integer.toString(driveId) + "/report")
				.request()
				.post(Entity.json(report), DriveReport.class);
		reportWraps = target("drive")
				.path("all-reports")
				.request()
				.get(DRIVEWRAP_LIST);
		assertEquals(reportWraps.get(0).getReports().get(0).getReportMessage(), "Driving like a mad man");
    }
    
    @Test
    public void numberOfDrivesForUser() {
    	login(TEST_CREDENTIALS);
    	long departureTime = new Timestamp(2018 - 1900, 1, 1, 1, 1, 1, 1).getTime();
        long arrivalTime = new Timestamp(2018 - 1900, 2, 2, 2, 2, 2, 2).getTime();
        Drive drive = new Drive(-1, "A", "B", departureTime, arrivalTime, "Comment", "x", "x", "x", "x", 4, 1, false, false, false);
        DriveWrap newDriveWrap= new DriveWrap(drive, new ArrayList<DriveMilestone>(), new ArrayList<DriveUser>(), new ArrayList<DriveReport>());
		newDriveWrap = target("drive")
				.request()
				.post(Entity.json(newDriveWrap), DriveWrap.class);
		int driveId1 = newDriveWrap.getDrive().getDriveId();
		newDriveWrap = target("drive")
				.request()
				.post(Entity.json(newDriveWrap), DriveWrap.class);
		int driveId2 = newDriveWrap.getDrive().getDriveId();
		int numberOfDrives = target("drive")
				.path("count/" + TEST.getId())
				.request()
				.get(int.class);
		assertEquals(2, numberOfDrives);
    }
    
//    @Test
//    public void addUserToDrive() {
//    	login(TEST_CREDENTIALS);
//		long departureTime = new Timestamp(2018 - 1900, 10, 20, 12, 0, 0, 0).getTime();
//        long arrivalTime = new Timestamp(2018 - 1900, 10, 20, 12, 25, 0, 0).getTime();
//		Drive drive = new Drive(-1, "A", "F", departureTime, arrivalTime, "Comment", "x", "x", "x", "x", 2, 1, true, true, false);
//		DriveWrap driveWrap= new DriveWrap(drive, new ArrayList<DriveMilestone>(), new ArrayList<DriveUser>(), new ArrayList<DriveReport>());
//		driveWrap = target("drive")
//				.request()
//				.post(Entity.json(driveWrap), DriveWrap.class);
//		int driveId = driveWrap.getDrive().getDriveId();
//    	DriveUser driveUser = new DriveUser(driveId, TEST.getId(), "A", "F", false, false, false);
//		driveUser = target("drive")
//				.path(Integer.toString(driveId) + "/user")
//				.request()
//				.post(Entity.json(driveUser), DriveUser.class);
//		driveWrap = target("drive")
//                .path(Integer.toString(driveWrap.getDrive().getDriveId()))
//                .request()
//                .get(DriveWrap.class);
//		assertEquals(driveWrap.getUsers().get(0).getUserId(), TEST.getId());
//    }
	
    @Test
    public void addAndAcceptUserInDrive() {
    	login(TEST_CREDENTIALS);
    	long departureTime = new Timestamp(2018 - 1900, 1, 1, 1, 1, 1, 1).getTime();
        long arrivalTime = new Timestamp(2018 - 1900, 2, 2, 2, 2, 2, 2).getTime();
        Drive drive = new Drive(-1, "A", "B", departureTime, arrivalTime, "Comment", "x", "x", "x", "x", 4, 1, false, false, false);
        DriveWrap newDriveWrap= new DriveWrap(drive, new ArrayList<DriveMilestone>(), new ArrayList<DriveUser>(), new ArrayList<DriveReport>());
		newDriveWrap = target("drive")
				.request()
				.post(Entity.json(newDriveWrap), DriveWrap.class);
		int driveId = newDriveWrap.getDrive().getDriveId();
		logout();
		login(ADMIN_CREDENTIALS);
		DriveUser driveUser = new DriveUser(driveId, ADMIN.getId(), "A", "F", false, false, false);
		driveUser = target("drive")
				.path(driveId + "/user")
				.request()
				.post(Entity.json(driveUser), DriveUser.class);
		logout();
		login(TEST_CREDENTIALS);
		target("drive")
				.path(driveId + "/user/" + ADMIN.getId())
				.request()
				.put(Entity.json(driveUser));
		newDriveWrap = target("drive")
              .path(Integer.toString(driveId))
              .request()
              .get(DriveWrap.class);
		assertTrue(newDriveWrap.getUsers().get(0).isAccepted());
		assertTrue(newDriveWrap.getUsers().get(1).isAccepted());
    }
    
    @Test
    public void rateUser() {
    	login(TEST_CREDENTIALS);
    	long departureTime = new Timestamp(2018 - 1900, 1, 1, 1, 1, 1, 1).getTime();
        long arrivalTime = new Timestamp(2018 - 1900, 2, 2, 2, 2, 2, 2).getTime();
        Drive drive = new Drive(-1, "A", "B", departureTime, arrivalTime, "Comment", "x", "x", "x", "x", 4, 1, false, false, false);
        DriveWrap newDriveWrap= new DriveWrap(drive, new ArrayList<DriveMilestone>(), new ArrayList<DriveUser>(), new ArrayList<DriveReport>());
		newDriveWrap = target("drive")
				.request()
				.post(Entity.json(newDriveWrap), DriveWrap.class);
		int driveId = newDriveWrap.getDrive().getDriveId();
		//add another user
		logout();
		login(ADMIN_CREDENTIALS);
		DriveUser driveUser = new DriveUser(driveId, ADMIN.getId(), "A", "F", false, false, false);
		driveUser = target("drive")
				.path(driveId + "/user")
				.request()
				.post(Entity.json(driveUser), DriveUser.class);
		target("drive")
				.path(driveId + "/rate/" + 4)
				.request()
				.put(Entity.json(4));
		newDriveWrap = target("drive")
	              .path(Integer.toString(driveId))
	              .request()
	              .get(DriveWrap.class);
		assertTrue(newDriveWrap.getUsers().get(1).hasRated());
		
    }
    
}
