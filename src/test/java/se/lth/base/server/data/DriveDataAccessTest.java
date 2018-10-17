package se.lth.base.server.data;

import org.junit.Before;
import org.junit.Test;
import se.lth.base.server.Config;
import se.lth.base.server.database.BaseDataAccessTest;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * @author Group 1 ETSN05 2018
 */

public class DriveDataAccessTest extends BaseDataAccessTest {

    private final DriveDataAccess driveDao = new DriveDataAccess(Config.instance().getDatabaseDriver());

    private int drive1Id;
    private Drive drive1;
    private Drive drive2;

    @Before
    public void registerTestDrive() {
        long departureTime = new Timestamp(Date.valueOf("2017-07-22").getTime()).getTime();
        long arrivalTime = new Timestamp(Date.valueOf("2017-07-23").getTime()).getTime();
        drive1 = driveDao.addDrive(new Drive(-1, "A", "F", departureTime, arrivalTime, "Comment", "x", "x", "x", "x", 1, 1, false, false, false));
        drive1Id = drive1.getDriveId();
        drive2 = driveDao.addDrive(new Drive(-1, "A", "F", departureTime, arrivalTime, "Comment", "x", "x", "x", "x", 1, 1, false, false, false));
    }

    @Test
    public void addDrive() {
        List<Drive> drives = driveDao.getDrives();
        assertEquals(drives.get(0).getDriveId(), drive1Id);
    }

    @Test
    public void updateDrive() {
        long departureTime = new Timestamp(Date.valueOf("2017-07-22").getTime()).getTime();
        long arrivalTime = new Timestamp(Date.valueOf("2017-07-23").getTime()).getTime();
        Drive drive2 = new Drive(drive1Id, "A", "B", departureTime, arrivalTime, "i", "j", "k", "l", "m", 3, 2, false, false, false);
        driveDao.updateDrive(drive2);
        
        assertEquals("A", drive2.getStart());
        assertEquals("B", drive2.getStop());
        assertEquals(departureTime, drive2.getDepartureTime());
        assertEquals(arrivalTime, drive2.getArrivalTime());
        assertEquals("i", drive2.getComment());
        assertEquals("j", drive2.getCarBrand());
        assertEquals("k", drive2.getCarModel());
        assertEquals("l", drive2.getCarColor());
        assertEquals("m", drive2.getCarLicensePlate());
        assertEquals(3, drive2.getCarNumberOfSeats());
        assertEquals(2, drive2.getOptLuggageSize());
        assertEquals(false, drive2.getOptWinterTires());
        assertEquals(false, drive2.getOptBicycle());
        assertEquals(false, drive2.getOptPets());
    }

    @Test
    public void getReportedDrives() {
        DriveUserDataAccess driveUserDao = new DriveUserDataAccess(Config.instance().getDatabaseDriver());
        driveUserDao.addDriveUser(drive1.getDriveId(), TEST.getId(), "A", "F", true, true, false);
        DriveReportDataAccess driveReportDao = new DriveReportDataAccess(Config.instance().getDatabaseDriver());
        driveReportDao.addDriveReport(drive1Id, TEST.getId(), "Bad driver");
        assertEquals(1, driveDao.getReportedDrives().size());
    }

    @Test
    public void getNumberOfDrivesForUser() {
        DriveUserDataAccess driveUserDao = new DriveUserDataAccess(Config.instance().getDatabaseDriver());
        driveUserDao.addDriveUser(drive1.getDriveId(), TEST.getId(), "A", "F", true, true, false);
        driveUserDao.addDriveUser(drive2.getDriveId(), TEST.getId(), "A", "F", false, true, false);

        assertEquals(1, driveUserDao.getNumberOfDrivesForUser(TEST.getId()));
    }

    @Test
    public void getDrivesForUser() {
        long departureTime = new Timestamp(Date.valueOf("2017-07-22").getTime()).getTime();
        long arrivalTime = new Timestamp(Date.valueOf("2017-07-23").getTime()).getTime();
        Drive drive2 = driveDao.addDrive(new Drive(-1, "A", "B", departureTime, arrivalTime, "i", "j", "k", "l", "m", 3, 2, false, false, false));

        DriveUserDataAccess driveUserDao = new DriveUserDataAccess(Config.instance().getDatabaseDriver());

        driveUserDao.addDriveUser(drive1.getDriveId(), TEST.getId(), "A", "F", true, true, false);
        driveUserDao.addDriveUser(drive2.getDriveId(), TEST.getId(), "A", "B", true, true, false);
        assertEquals(2, driveUserDao.getNumberOfDrivesForUser(TEST.getId()));
    }

    @Test
    public void deleteDrive() {
        driveDao.deleteDrive(drive1Id);
        List<Drive> drives = driveDao.getDrives();
        assertTrue(drives.size() == 1);
    }
}

