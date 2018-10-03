package se.lth.base.server.data;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import se.lth.base.server.database.DataAccess;
import se.lth.base.server.database.Mapper;

/**
 * Data access class for a drive
 * 
 * @author Group 1 ETSN05 2018
 *g
 */
public class DriveDataAccess extends DataAccess<Drive> {
	
	private static final class DriveMapper implements Mapper<Drive> {
        @Override
        public Drive map(ResultSet resultSet) throws SQLException {
            return new Drive(resultSet.getInt("drive_id"),
                    resultSet.getString("start"),
                    resultSet.getString("stop"),
                    resultSet.getTimestamp("departure_time"),
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

    public DriveDataAccess(String driverUrl) {
        super(driverUrl, new DriveMapper());
    }
    
    public Drive addDrive(String start, String stop, Timestamp departureTime, String comment, String carBrand,
			String carModel, String carColor, String carLicensePlate, int carNumberOfSeats, int optLuggageSize,
			boolean optWinterTires, boolean optBicycle, boolean optPets) {

    	int driveId = insert("INSERT INTO drive (start, stop, departure_time, comment, car_brand, car_model, car_color, car_license_plate,"
    			+ " car_number_of_seats, opt_luggage_size, opt_winter_tires, opt_bicycles, opt_pets) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",
                start, stop, departureTime, comment, carBrand, carModel, carColor, carLicensePlate, 
                carNumberOfSeats, optLuggageSize, optWinterTires, optBicycle, optPets);
    	
    	return new Drive(driveId, start, stop, departureTime, comment, carBrand, carModel, carColor, carLicensePlate, 
                carNumberOfSeats, optLuggageSize, optWinterTires, optBicycle, optPets);
    }
    
    public Drive updateDrive(int driveId, String start, String stop, Timestamp departureTime, String comment, String carBrand,
			String carModel, String carColor, String carLicensePlate, int carNumberOfSeats, int optLuggageSize,
			boolean optWinterTires, boolean optBicycle, boolean optPets) {

    	execute("UPDATE drive SET start = ?, stop = ?, departure_time = ?, comment = ?, car_brand = ?, car_model = ?, car_color = ?, car_license_plate = ?, car_number_of_seats = ?, opt_luggage_size = ?, opt_winter_tires = ?, opt_bicycles = ?, opt_pets = ? WHERE drive_id = ?)",
                start, stop, departureTime, comment, carBrand, carModel, carColor, carLicensePlate, 
                carNumberOfSeats, optLuggageSize, optWinterTires, optBicycle, optPets, driveId);    	
    	
    	return getDrive(driveId);
    }
    
    public Drive getDrive(int driveId) {
    	return queryFirst("SELECT drive_id, start, stop, departure_time, comment, car_brand, car_model, car_color, car_license_plate, car_number_of_seats, opt_luggage_size, opt_winter_tires, opt_bicycles, opt_pets FROM drive WHERE drive_id = ?", 
    			driveId);
    }
    
    public List<Drive> getDrives() {
    	return query("SELECT drive_id, start, stop, departure_time, comment, car_brand, car_model, car_color, car_license_plate, car_number_of_seats, opt_luggage_size, opt_winter_tires, opt_bicycles, opt_pets FROM drive");
    }
    
    public boolean deleteDrive(int driveId) {
        return execute("DELETE FROM drive WHERE drive_id = ?", driveId) > 0;
    }
    
}



