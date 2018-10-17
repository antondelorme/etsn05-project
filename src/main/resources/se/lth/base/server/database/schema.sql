-- This is the schema file that the database is initialized with. It is specific to the H2 SQL dialect.

-- User roles describe what each user can do on a generic level.
CREATE TABLE user_role(role_id TINYINT,
                       role VARCHAR(255) NOT NULL UNIQUE,
                       PRIMARY KEY (role_id));

CREATE TABLE user(user_id INT AUTO_INCREMENT,
                  role_id TINYINT NOT NULL DEFAULT 2,
                  email VARCHAR NOT NULL UNIQUE, -- emails should be unique
                  salt BIGINT NOT NULL,
                  password_hash UUID NOT NULL,
                  first_name VARCHAR(255) NOT NULL,
                  last_name VARCHAR(255) NOT NULL,
                  phone_number VARCHAR(255) NOT NULL,
                  gender TINYINT NOT NULL,
                  date_of_birth DATE NOT NULL,
                  driving_license BOOLEAN NOT NULL DEFAULT FALSE,
                  rating_total_score INT NOT NULL DEFAULT 0,
                  number_of_ratings INT NOT NULL DEFAULT 0,
                  warning INT NOT NULL DEFAULT 1,
                  PRIMARY KEY (user_id),
                  FOREIGN KEY (role_id) REFERENCES user_role (role_id));

-- Sessions are indexed by large random numbers instead of a sequence of integers, because they could otherwise
-- be guessed by a malicious user.
CREATE TABLE session(session_uuid UUID DEFAULT RANDOM_UUID(),
                     user_id INT NOT NULL,
                     last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP(),
                     PRIMARY KEY(session_uuid),
                     FOREIGN KEY(user_id) REFERENCES user(user_id) ON DELETE CASCADE);

CREATE TABLE drive(drive_id INT AUTO_INCREMENT,
                   start VARCHAR(255) NOT NULL,
                   stop VARCHAR(255) NOT NULL,
                   departure_time TIMESTAMP NOT NULL,
                   arrival_time TIMESTAMP NOT NULL,
                   comment VARCHAR(255) NOT NULL,
                   car_brand VARCHAR(255) NOT NULL,
                   car_model VARCHAR(255) NOT NULL,
                   car_color VARCHAR(255) NOT NULL,
                   car_license_plate VARCHAR(255) NOT NULL,
                   car_number_of_seats TINYINT NOT NULL,
                   opt_luggage_size TINYINT NOT NULL DEFAULT 1,
                   opt_winter_tires BOOLEAN NOT NULL DEFAULT FALSE,
                   opt_bicycle BOOLEAN NOT NULL DEFAULT FALSE,
                   opt_pets BOOLEAN NOT NULL DEFAULT FALSE,
                   PRIMARY KEY(drive_id));

CREATE TABLE drive_milestone(milestone_id INT AUTO_INCREMENT,
                             drive_id INT NOT NULL,
                             milestone_name VARCHAR(255) NOT NULL,
                             departure_time TIMESTAMP NOT NULL,
                             PRIMARY KEY(milestone_id),
                             FOREIGN KEY(drive_id) REFERENCES drive(drive_id) ON DELETE CASCADE);

CREATE TABLE drive_user(drive_user_id INT AUTO_INCREMENT,
                        drive_id INT NOT NULL,
                        user_id INT NOT NULL,
                        start VARCHAR(255) NOT NULL,
                        stop VARCHAR(255) NOT NULL,
                        is_driver BOOLEAN NOT NULL DEFAULT FALSE,
                        accepted BOOLEAN NOT NULL DEFAULT FALSE,
                        rated BOOLEAN NOT NULL DEFAULT FALSE,
                        PRIMARY KEY(drive_user_id),
                        FOREIGN KEY(drive_id) REFERENCES drive(drive_id) ON DELETE CASCADE,
                        FOREIGN KEY(user_id) REFERENCES user(user_id) ON DELETE CASCADE);

CREATE TABLE drive_report(report_id INT AUTO_INCREMENT,
                          drive_id INT NOT NULL,
                          reported_by_user_id INT NOT NULL,
                          report_message VARCHAR(255) NOT NULL,
                          PRIMARY KEY(report_id),
                          FOREIGN KEY(drive_id) REFERENCES drive(drive_id) ON DELETE CASCADE,
                          FOREIGN KEY(reported_by_user_id) REFERENCES user(user_id) ON DELETE CASCADE);

CREATE TABLE search_filter(search_filter_id INT AUTO_INCREMENT,
                           user_id INT NOT NULL,
                           start VARCHAR(255),
                           stop VARCHAR(255),
                           departure_time TIMESTAMP,
                           PRIMARY KEY(search_filter_id),
                           FOREIGN KEY(user_id) REFERENCES user(user_id) ON DELETE CASCADE);

INSERT INTO user_role VALUES (1, 'ADMIN'), (2, 'USER');
INSERT INTO user (role_id, email, salt, password_hash, first_name, last_name, phone_number, gender, date_of_birth)
    VALUES (1, 'admin@lu.se', 8486015201299423224, '54c89afb-d106-8cf7-ff28-9362aff5a6bc', 'Admin', 'Admin', '0701234', 1, CURRENT_TIMESTAMP() - 00000010000),
           (2, 'test@lu.se', 5336889820313124494, '144141f3-c868-85e8-0243-805ca28cdabd', 'Test', 'Test', '0701234', 1, CURRENT_TIMESTAMP() - 00000010000),
           (2, 'driver@lu.se', -8230560395748062196, '026edaa7-7e68-4bdf-b1d5-2acda6e9ec05', 'Driver', 'Test', '070123456', 1, CURRENT_TIMESTAMP() - 00000010000),
           (2, 'passenger@lu.se', 7048289284615084861, 'f8444eec-5d7e-8b36-ac6b-2c6042623b30', 'Passenger', 'Test', '070123456', 1, CURRENT_TIMESTAMP() - 00000010000);

--INSERT INTO drive (start, stop, departure_time, arrival_time, comment, car_brand, car_model, car_color, car_license_plate, car_number_of_seats, opt_luggage_size, opt_winter_tires, opt_bicycle, opt_pets)
--VALUES ('Gothenburg', 'Lund', CURRENT_TIMESTAMP() + 0000000020, CURRENT_TIMESTAMP() + 0000000022, 'This is test comment', 'Audi', 'A3 Sportsback', 'Black', 'DBG400', 5, 1, TRUE, FALSE, FALSE);
--INSERT INTO drive_user (drive_id, user_id, start, stop, is_driver, accepted, rated) VALUES (1, 3, 'Gothenburg', 'Lund', TRUE, TRUE, FALSE);
--INSERT INTO drive_milestone (drive_id, milestone_name, departure_time) VALUES (1, 'Halmstad', CURRENT_TIMESTAMP() + 0000000021,);
--INSERT INTO drive_report (drive_id, reported_by_user_id, report_message) VALUES (1, 3, 'Hello');