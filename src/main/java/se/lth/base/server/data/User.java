package se.lth.base.server.data;

import java.security.Principal;
import java.sql.Date;

public class User implements Principal {

    public static User NONE = new User(0, Role.NONE, "-", "-", "-", "0", "-", 0, Date.valueOf("2018-01-01"), false, 0, 0, 0);

    private final int userId;
    private final Role role;
    private final String username;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private final String email;
    private final int gender;
    private final Date dateOfBirth;
    private final Boolean drivingLicence;
    private final int ratingTotalScore;
    private final int numberOfRatings;
    private final int warning;

    public User(int userId, Role role, String username, String firstName, String lastName, String phoneNumber,
			String email, int gender, Date dateOfBirth, Boolean drivingLicence, int ratingTotalScore,
			int numberOfRatings, int warning) {
		this.userId = userId;
		this.role = role;
		this.username = username;
		this.firstName = firstName;
		this.lastName = lastName;
		this.phoneNumber = phoneNumber;
		this.email = email;
		this.gender = gender;
		this.dateOfBirth = dateOfBirth;
		this.drivingLicence = drivingLicence;
		this.ratingTotalScore = ratingTotalScore;
		this.numberOfRatings = numberOfRatings;
		this.warning = warning;
	}

    public Role getRole() {
        return role;
    }

    public int getId() {
        return userId;
    }

    @Override
    public String getName() {
        return username;
    }
	
	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
        this.lastName = lastName;
    }

	public String getPhoneNumber() {
		return phoneNumber;
	}

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

	public String getEmail() {
		return email;
	}

	public int getGender() {
		return gender;
	}

	public Date getDateOfBirth() {
		return dateOfBirth;
	}

	public Boolean getDrivingLicence() {
		return drivingLicence;
	}

	public int getRatingTotalScore() {
		return ratingTotalScore;
	}

	public int getNumberOfRatings() {
		return numberOfRatings;
	}

	public int getWarning() {
		return warning;
	}
}
