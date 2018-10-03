package se.lth.base.server.data;

import java.sql.Timestamp;

/**
 * Data class for a drive
 *
 * @author Group 1 ETSN05 2018
 * @see DriveDataAccess
 */
public class Drive {
    private final int driveId, carNumberOfSeats, optLuggageSize;
    private final String start, stop, comment, carBrand, carModel, carColor, carLicensePlate; 
    private final boolean optWinterTires, optPets, optBicycle;
    private final Timestamp departureTime;
    
	public Drive(int driveId, String start, String stop, Timestamp departureTime, String comment, String carBrand,
			String carModel, String carColor, String carLicensePlate, int carNumberOfSeats, int optLuggageSize,
			boolean optWinterTires, boolean optBicycle, boolean optPets) {
		this.driveId = driveId;
		this.start = start;
		this.stop = stop;
		this.departureTime = departureTime;
		this.comment = comment;
		this.carBrand = carBrand;
		this.carModel = carModel;
		this.carColor = carColor;
		this.carLicensePlate = carLicensePlate;
		this.carNumberOfSeats = carNumberOfSeats;
		this.optLuggageSize = optLuggageSize;
		this.optWinterTires = optWinterTires;
		this.optBicycle = optBicycle;
		this.optPets = optPets;
	}

	public int getDriveId() {
		return driveId;
	}

	public String getStart() {
		return start;
	}

	public String getStop() {
		return stop;
	}
	
	public Timestamp getDepartureTime() {
		return departureTime;
	}	

	public String getComment() {
		return comment;
	}

	public String getCarBrand() {
		return carBrand;
	}

	public String getCarModel() {
		return carModel;
	}

	public String getCarColor() {
		return carColor;
	}

	public String getCarLicensePlate() {
		return carLicensePlate;
	}
	
	public int getCarNumberOfSeats() {
		return carNumberOfSeats;
	}
	
	public int getOptLuggageSize() {
		return optLuggageSize;
	}

	public boolean getOptWinterTires() {
		return optWinterTires;
	}
	
	public boolean getOptBicycle() {
		return optBicycle;
	}

	public boolean getOptPets() {
		return optPets;
	}
}


