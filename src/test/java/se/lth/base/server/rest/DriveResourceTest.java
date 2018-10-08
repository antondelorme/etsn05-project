package se.lth.base.server.rest;

import org.junit.Test;

import se.lth.base.server.BaseResourceTest;
import se.lth.base.server.data.Drive;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.core.GenericType;
import java.util.List;

import static org.junit.Assert.*;


public class DriveResourceTest extends BaseResourceTest {
	
	 private static final GenericType<List<Drive>> DRIVE_LIST = new GenericType<List<Drive>>() {
	    };
	
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
}