package se.lth.base.server.rest;

import com.google.gson.internal.LinkedTreeMap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import se.lth.base.server.BaseResourceTest;
import se.lth.base.server.Config;
import se.lth.base.server.data.*;

import javax.ws.rs.client.Entity;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;

public class SearchResourceTest extends BaseResourceTest {

    // Search test users
    private final int idStart = 3;
    private final User SEARCH_TEST_1 = new User(idStart, Role.USER, "test1@commutecompanion.se", "ST1FirstName", "ST1LastName", "+4670207579", 0, Date.valueOf("1995-01-01").getTime(), true, 0, 0, 0);
    private final Credentials SEARCH_TEST_CREDENTIALS_1 = new Credentials("test1@commutecompanion.se", "test", Role.USER, SEARCH_TEST_1);

    private final User SEARCH_TEST_2 = new User(idStart, Role.USER, "test2@commutecompanion.se", "ST2FirstName", "ST2LastName", "+4670207579", 0, Date.valueOf("1995-01-01").getTime(), true, 0, 0, 0);
    private final Credentials SEARCH_TEST_CREDENTIALS_2 = new Credentials("test2@commutecompanion.se", "test", Role.USER, SEARCH_TEST_2);

    private final User SEARCH_TEST_3 = new User(idStart, Role.USER, "test3@commutecompanion.se", "ST3FirstName", "ST3LastName", "+4670207579", 0, Date.valueOf("1995-01-01").getTime(), true, 0, 0, 0);
    private final Credentials SEARCH_TEST_CREDENTIALS_3 = new Credentials("test3@commutecompanion.se", "test", Role.USER, SEARCH_TEST_3);

    private final User SEARCH_TEST_4 = new User(idStart, Role.USER, "test4@commutecompanion.se", "ST4FirstName", "ST4LastName", "+4670207579", 0, Date.valueOf("1995-01-01").getTime(), true, 0, 0, 0);
    private final Credentials SEARCH_TEST_CREDENTIALS_4 = new Credentials("test4@commutecompanion.se", "test", Role.USER, SEARCH_TEST_4);

    private final User SEARCH_TEST_5 = new User(idStart, Role.USER, "test5@commutecompanion.se", "ST5FirstName", "ST5LastName", "+4670207579", 0, Date.valueOf("1995-01-01").getTime(), true, 0, 0, 0);
    private final Credentials SEARCH_TEST_CREDENTIALS_5 = new Credentials("test5@commutecompanion.se", "test", Role.USER, SEARCH_TEST_5);

    private int user1Id;
    private int user2Id;
    private int user3Id;
    private int user4Id;
    private int user5Id;

    private int drive1Id;
    private int drive2Id;
    private int drive3Id;

    @Before
    public void registerTestUsers() {
        UserDataAccess userDao = new UserDataAccess(Config.instance().getDatabaseDriver());

        // Users
        User user1 = userDao.addUser(SEARCH_TEST_CREDENTIALS_1);
        User user2 = userDao.addUser(SEARCH_TEST_CREDENTIALS_2);
        User user3 = userDao.addUser(SEARCH_TEST_CREDENTIALS_3);
        User user4 = userDao.addUser(SEARCH_TEST_CREDENTIALS_4);
        User user5 = userDao.addUser(SEARCH_TEST_CREDENTIALS_5);
        user1Id = user1.getId();
        user2Id = user2.getId();
        user3Id = user3.getId();
        user4Id = user4.getId();
        user5Id = user5.getId();

        // Data access objects
        DriveDataAccess driveDao = new DriveDataAccess(Config.instance().getDatabaseDriver());
        DriveMilestoneDataAccess driveMilestoneDao = new DriveMilestoneDataAccess(Config.instance().getDatabaseDriver());
        DriveUserDataAccess driveUserDao = new DriveUserDataAccess(Config.instance().getDatabaseDriver());

        // Drive 1
        long timestamp1 = new Timestamp(2018 - 1900, 10, 20, 12, 0, 0, 0).getTime();
        long timestamp1_5 = new Timestamp(2018 - 1900, 10, 20, 12, 25, 0, 0).getTime();
        Drive drive1 = driveDao.addDrive(new Drive(-1, "A", "F", timestamp1, timestamp1_5, "Comment", "x", "x", "x", "x", 1, 1, false, false, false));
        drive1Id = drive1.getDriveId();

        long timestamp1_1 = new Timestamp(2018 - 1900, 10, 20, 12, 5, 0, 0).getTime();
        long timestamp1_2 = new Timestamp(2018 - 1900, 10, 20, 12, 10, 0, 0).getTime();
        long timestamp1_3 = new Timestamp(2018 - 1900, 10, 20, 12, 15, 0, 0).getTime();
        long timestamp1_4 = new Timestamp(2018 - 1900, 10, 20, 12, 20, 0, 0).getTime();
        driveMilestoneDao.addMilestone(drive1.getDriveId(), "B", timestamp1_1);
        driveMilestoneDao.addMilestone(drive1.getDriveId(), "C", timestamp1_2);
        driveMilestoneDao.addMilestone(drive1.getDriveId(), "D", timestamp1_3);
        driveMilestoneDao.addMilestone(drive1.getDriveId(), "E", timestamp1_4);

        driveUserDao.addDriveUser(drive1.getDriveId(), user1.getId(), "A", "F", true, true, false);

        // Drive 2
        long timestamp2 = new Timestamp(2018 - 1900, 10, 20, 13, 0, 0, 0).getTime();
        long timestamp2_5 = new Timestamp(2018 - 1900, 10, 20, 13, 25, 0, 0).getTime();
        Drive drive2 = driveDao.addDrive(new Drive(-1, "A", "F", timestamp2, timestamp2_5, "Comment", "x", "x", "x", "x", 3, 1, false, false, false));
        drive2Id = drive2.getDriveId();
        long timestamp2_1 = new Timestamp(2018 - 1900, 10, 20, 13, 5, 0, 0).getTime();
        long timestamp2_2 = new Timestamp(2018 - 1900, 10, 20, 13, 10, 0, 0).getTime();
        long timestamp2_3 = new Timestamp(2018 - 1900, 10, 20, 13, 15, 0, 0).getTime();
        long timestamp2_4 = new Timestamp(2018 - 1900, 10, 20, 13, 20, 0, 0).getTime();
        driveMilestoneDao.addMilestone(drive2.getDriveId(), "B", timestamp2_1);
        driveMilestoneDao.addMilestone(drive2.getDriveId(), "C", timestamp2_2);
        driveMilestoneDao.addMilestone(drive2.getDriveId(), "D", timestamp2_3);
        driveMilestoneDao.addMilestone(drive2.getDriveId(), "E", timestamp2_4);

        driveUserDao.addDriveUser(drive2.getDriveId(), user1.getId(), "A", "F", true, true, false);

        // Drive 3
        long timestamp3 = new Timestamp(2018 - 1900, 10, 20, 14, 0, 0, 0).getTime();
        long timestamp3_5 = new Timestamp(2018 - 1900, 10, 20, 14, 25, 0, 0).getTime();
        Drive drive3 = driveDao.addDrive(new Drive(-1, "A2", "F2", timestamp3, timestamp3_5, "Comment", "x", "x", "x", "x", 1, 1, false, false, false));
        drive3Id = drive3.getDriveId();
        long timestamp3_1 = new Timestamp(2018 - 1900, 10, 20, 14, 5, 0, 0).getTime();
        long timestamp3_2 = new Timestamp(2018 - 1900, 10, 20, 14, 10, 0, 0).getTime();
        long timestamp3_3 = new Timestamp(2018 - 1900, 10, 20, 14, 15, 0, 0).getTime();
        long timestamp3_4 = new Timestamp(2018 - 1900, 10, 20, 14, 20, 0, 0).getTime();
        driveMilestoneDao.addMilestone(drive3.getDriveId(), "B2", timestamp3_1);
        driveMilestoneDao.addMilestone(drive3.getDriveId(), "C2", timestamp3_2);
        driveMilestoneDao.addMilestone(drive3.getDriveId(), "D2", timestamp3_3);
        driveMilestoneDao.addMilestone(drive3.getDriveId(), "E2", timestamp3_4);

        driveUserDao.addDriveUser(drive3.getDriveId(), user1.getId(), "A2", "F2", true, true, false);
    }

    @Test
    /*
        Start and stop is entered, depature time is null
     */
    public void getDrivesMatchingStartStop() {
        login(SEARCH_TEST_CREDENTIALS_2);

        SearchFilter searchFilter = new SearchFilter(-1, -1, "C", "E", -1);
        // We actually receive a List<LinkedTreeMap<String, Object>>
        List<DriveWrap> response = target("search")
                .path("drives")
                .request()
                .post(Entity.json(searchFilter), List.class);

        Assert.assertEquals(2, response.size());
    }


    //@Test
    // NO LONGER VALID (since advanced drive search is disabled in back-end)
    /*
        Drive is A -> E (with max 1 passenger)
        Already one trip from A -> C
        Trip from B -> C should not be possible
        Trip from C -> E should be possible
     */
    /*
    public void getDrivesWithEnoughSeats() {
        DriveUserDataAccess driveUserDao = new DriveUserDataAccess(Config.instance().getDatabaseDriver());
        driveUserDao.addDriveUser(drive1Id, user2Id, "A", "C", false, true, false);

        login(SEARCH_TEST_CREDENTIALS_2);
        long timestamp1User = new Timestamp(2018 - 1900, 10, 20, 12, 0, 0, 0).getTime();
        SearchFilter searchFilter1 = new SearchFilter(-1, -1, "B", "C", timestamp1User);

        // We actually receive a List<LinkedTreeMap<String, Object>>
        List<DriveWrap> response1 = target("search")
                .path("drives")
                .request()
                .post(Entity.json(searchFilter1), List.class);

        Assert.assertEquals(0, response1.size());

        long timestamp2User = new Timestamp(2018 - 1900, 10, 20, 12, 0, 0, 0).getTime();
        SearchFilter searchFilter2 = new SearchFilter(-1, -1, "C", "E", timestamp2User);

        // We actually receive a List<LinkedTreeMap<String, Object>>
        List<DriveWrap> response2 = target("search")
                .path("drives")
                .request()
                .post(Entity.json(searchFilter2), List.class);

        Assert.assertEquals(1, response2.size());
    }
    */

    /*
        Drive is A -> E (with max 1 passenger)
        Add one passenger
        Make sure that drive is not included in result from getDrives(SearchFilter)
     */
    @Test
    public void verifyUserNotReceivingFullDrives() {
        DriveUserDataAccess driveUserDao = new DriveUserDataAccess(Config.instance().getDatabaseDriver());
        driveUserDao.addDriveUser(drive1Id, user2Id, "A", "E", false, true, false);

        login(SEARCH_TEST_CREDENTIALS_2);
        long timestamp1User = new Timestamp(2018 - 1900, 10, 20, 11, 45, 0, 0).getTime();
        SearchFilter searchFilter1 = new SearchFilter(-1, -1, "A", "E", timestamp1User);

        // We actually receive a List<LinkedTreeMap<String, Object>>
        List<DriveWrap> response1 = target("search")
                .path("drives")
                .request()
                .post(Entity.json(searchFilter1), List.class);

        Assert.assertEquals(0, response1.size());
    }

    @Test
    /*
        New trip from B -> E (13.00) should match with drive 2 (A - E) (C - 13.05)
        New trip from B -> E (17.00) should not match with any drive
     */
    public void getDrivesMatchingTime() {
        login(SEARCH_TEST_CREDENTIALS_2);

        long timestamp1User = new Timestamp(2018 - 1900, 10, 20, 13, 0, 0, 0).getTime();
        SearchFilter searchFilter1 = new SearchFilter(-1, user2Id, "B", "C", timestamp1User);

        // We actually receive a List<LinkedTreeMap<String, Object>>
        List<DriveWrap> response1 = target("search")
                .path("drives")
                .request()
                .post(Entity.json(searchFilter1), List.class);

        Assert.assertEquals(1, response1.size());

        long timestamp2User = new Timestamp(2018 - 1900, 10, 20, 17, 0, 0, 0).getTime();
        SearchFilter searchFilter2 = new SearchFilter(-1, user2Id, "B", "C", timestamp2User);

        // We actually receive a List<LinkedTreeMap<String, Object>>
        List<DriveWrap> response2 = target("search")
                .path("drives")
                .request()
                .post(Entity.json(searchFilter2), List.class);

        Assert.assertEquals(0, response2.size());
    }

    @Test
    /**
     * When tripStart, tripStop and departureTime in SearchFilter is null and getDrives(SearchFilter) is called,
     * all drives should be returned in most recently created order
     */
    public void getDrivesInMostRecentlyCreatedOrder() {
        login(SEARCH_TEST_CREDENTIALS_2);

        SearchFilter searchFilter1 = new SearchFilter(-1, user2Id, null, null, -1);

        List<LinkedTreeMap<String, Object>> response = target("search")
                .path("drives")
                .request()
                .post(Entity.json(searchFilter1), List.class);

        // Apparently the list contains LinkedTreeMap<String, Object>, each representing a DriveWrap object
        LinkedTreeMap<String, Object> drive3 = (LinkedTreeMap<String, Object>) (response.get(0)).get("drive");
        Assert.assertEquals((double) drive3Id, drive3.get("driveId"));
        LinkedTreeMap<String, Object> drive2 = (LinkedTreeMap<String, Object>) (response.get(1)).get("drive");
        Assert.assertEquals((double) drive2Id, drive2.get("driveId"));
        LinkedTreeMap<String, Object> drive1 = (LinkedTreeMap<String, Object>) (response.get(2)).get("drive");
        Assert.assertEquals((double) drive1Id, drive1.get("driveId"));
    }

    @Test
    /**
     * 1.Search for first search test user
     * 2.Get all search test users (by specifying part of first name)
     * 3.Get all search test users (by specifying part of first name and part of email)
     */
    public void getUsersMatching() {
        login(ADMIN_CREDENTIALS);

        // We actually receive a List<LinkedTreeMap<String, Object>>
        List<User> response1 = target("search")
                .path("getUsers/ST1FirstName ST1LastName/test1@commutecompanion.se")
                .request()
                .get(List.class);

        Assert.assertEquals(1, response1.size());

        // We actually receive a List<LinkedTreeMap<String, Object>>
        List<User> response2 = target("search")
                .path("getUsers/First/")
                .request()
                .get(List.class);

        Assert.assertEquals(5, response2.size());

        // We actually receive a List<LinkedTreeMap<String, Object>>
        List<User> response3 = target("search")
                .path("getUsers/First/commute")
                .request()
                .get(List.class);

        Assert.assertEquals(5, response3.size());
    }


    /* Manual test (prints) (method can not return enough information for this test)
    @Test
    public void searchFilterSubscription() {
        // Create a searchFilter
        SearchFilterDataAccess searchFilterDao = new SearchFilterDataAccess(Config.instance().getDatabaseDriver());
        long timestamp3 = new Timestamp(2018, 10, 20, 12, 0, 0,0).getTime();
        searchFilterDao.addSearchFilter(new SearchFilter(-1, user2Id, "A", "B", timestamp3));

        // Add a matching drive
        login(TEST_CREDENTIALS);
        long timestamp = new Timestamp(2018, 10, 20, 12, 0, 0, 0).getTime();
        long timestamp2 = new Timestamp(2018, 10, 20, 12, 5, 0, 0).getTime();
        Drive drive = new Drive(-1, "A", "C", timestamp, "a", "x", "x", "x", "x", 2, 2, false, false, false);
        List<DriveMilestone> milestones = new ArrayList<>();
        milestones.add(new DriveMilestone(-1, -1, "B", timestamp2));
        List<DriveUser> driveUsers = new ArrayList<>();
        List<DriveReport> reports = new ArrayList<>();
        DriveWrap driveWrap = new DriveWrap(drive, milestones, driveUsers, reports);

        Response response = target("drive")
                .path("")
                .request()
                .post(Entity.json(driveWrap));

    }
    */
}
