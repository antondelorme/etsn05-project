package se.lth.base.server.data;

import se.lth.base.server.database.DataAccess;
import se.lth.base.server.database.DataAccessException;
import se.lth.base.server.database.ErrorType;
import se.lth.base.server.database.Mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

/**
 * Data access class for a drive
 * 
 * @author Group 1 ETSN05 2018
 *g
 */
public class DriveDataAccess extends DataAccess<Drive> {
	
    public Drive addDrive(Drive drive) {
        String start = drive.getStart();
        String stop = drive.getStop();
        long departureTime = drive.getDepartureTime();
        long arrivalTime = drive.getArrivalTime();
        String comment = drive.getComment();
        String carBrand = drive.getCarBrand();
        String carModel = drive.getCarModel();
        String carColor = drive.getCarColor();
        String carLicensePlate = drive.getCarLicensePlate();
        int carNumberOfSeats = drive.getCarNumberOfSeats();
        int optLuggageSize = drive.getOptLuggageSize();
        boolean optWinterTires = drive.getOptWinterTires();
        boolean optBicycle = drive.getOptBicycle();
        boolean optPets = drive.getOptPets();

        int driveId = insert("INSERT INTO drive (start, stop, departure_time, arrival_time, comment, car_brand, car_model, car_color, car_license_plate,"
                        + " car_number_of_seats, opt_luggage_size, opt_winter_tires, opt_bicycle, opt_pets) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                start, stop, new Timestamp(departureTime), new Timestamp(arrivalTime), comment, carBrand, carModel, carColor, carLicensePlate,
                carNumberOfSeats, optLuggageSize, optWinterTires, optBicycle, optPets);

        return new Drive(driveId, start, stop, departureTime, arrivalTime, comment, carBrand, carModel, carColor, carLicensePlate,
                carNumberOfSeats, optLuggageSize, optWinterTires, optBicycle, optPets);
    }

    public DriveDataAccess(String driverUrl) {
        super(driverUrl, new DriveMapper());
    }
    
    public Drive updateDrive(Drive drive) {
	    int driveId = drive.getDriveId();
        String start = drive.getStart();
        String stop = drive.getStop();
        long departureTime = drive.getDepartureTime();
        long arrivalTime = drive.getArrivalTime();
        String comment = drive.getComment();
        String carBrand = drive.getCarBrand();
        String carModel = drive.getCarModel();
        String carColor = drive.getCarColor();
        String carLicensePlate = drive.getCarLicensePlate();
        int carNumberOfSeats = drive.getCarNumberOfSeats();
        int optLuggageSize = drive.getOptLuggageSize();
        boolean optWinterTires = drive.getOptWinterTires();
        boolean optBicycle = drive.getOptBicycle();
        boolean optPets = drive.getOptPets();

        execute("UPDATE drive SET start = ?, stop = ?, departure_time = ?, arrival_time = ?, comment = ?, car_brand = ?, car_model = ?, car_color = ?, car_license_plate = ?, car_number_of_seats = ?, opt_luggage_size = ?, opt_winter_tires = ?, opt_bicycle = ?, opt_pets = ? WHERE drive_id = ?",
                start, stop, new Timestamp(departureTime), new Timestamp(arrivalTime), comment, carBrand, carModel, carColor, carLicensePlate,
                carNumberOfSeats, optLuggageSize, optWinterTires, optBicycle, optPets, driveId);

    	return getDrive(driveId);
    }

    public Drive getDrive(int driveId) {
        return queryFirst("SELECT drive_id, start, stop, departure_time, arrival_time, comment, car_brand, car_model, car_color, car_license_plate, car_number_of_seats, opt_luggage_size, opt_winter_tires, opt_bicycle, opt_pets FROM drive WHERE drive_id = ?",
                driveId);
    }

    public List<Drive> getDrives() {
        return query("SELECT drive_id, start, stop, departure_time, arrival_time, comment, car_brand, car_model, car_color, car_license_plate, car_number_of_seats, opt_luggage_size, opt_winter_tires, opt_bicycle, opt_pets FROM drive");
    }

    public List<Drive> getReportedDrives() {
        return query("SELECT * FROM drive INNER JOIN drive_report ON drive.drive_id = drive_report.drive_id");
    }

    private static final class DriveMapper implements Mapper<Drive> {
        @Override
        public Drive map(ResultSet resultSet) throws SQLException {
            return new Drive(resultSet.getInt("drive_id"),
                    resultSet.getString("start"),
                    resultSet.getString("stop"),
                    resultSet.getObject("departure_time", Timestamp.class).getTime(),
                    resultSet.getObject("arrival_time", Timestamp.class).getTime(),
                    resultSet.getString("comment"),
                    resultSet.getString("car_brand"),
                    resultSet.getString("car_model"),
                    resultSet.getString("car_color"),
                    resultSet.getString("car_license_plate"),
                    resultSet.getInt("car_number_of_seats"),
                    resultSet.getInt("opt_luggage_size"),
                    resultSet.getBoolean("opt_winter_tires"),
                    resultSet.getBoolean("opt_bicycle"),
                    resultSet.getBoolean("opt_pets"));
        }
    }

    public List<Drive> getDrivesForUser(int userId) {
        return query("SELECT * FROM drive INNER JOIN drive_user ON drive.drive_id = drive_user.drive_id WHERE user_id = ? ", userId);
    }

    public boolean deleteDrive(int driveId) {
        return execute("DELETE FROM drive WHERE drive_id = ?", driveId) > 0;
    }
}



