-- This is the schema file that the database is initialized with. It is specific to the H2 SQL dialect.

-- User roles describe what each user can do on a generic level.
CREATE TABLE user_role(role_id TINYINT,
                       role VARCHAR(255) NOT NULL UNIQUE,
                       PRIMARY KEY (role_id));

CREATE TABLE user(user_id INT AUTO_INCREMENT,
                  role_id TINYINT NOT NULL DEFAULT 2,
                  username VARCHAR_IGNORECASE NOT NULL UNIQUE, -- username should be unique
                  salt BIGINT NOT NULL,
                  password_hash UUID NOT NULL,
                  first_name VARCHAR(255) NOT NULL,
                  last_name VARCHAR(255) NOT NULL,
                  phone_number VARCHAR(255) NOT NULL,
                  email VARCHAR(255) NOT NULL,
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
                   comment VARCHAR(255) NOT NULL,
                   car_brand VARCHAR(255) NOT NULL,
                   car_model VARCHAR(255) NOT NULL,
                   car_color VARCHAR(255) NOT NULL,
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

CREATE TABLE drive_user(drive_id INT NOT NULL,
                        user_id INT NOT NULL,
                        start VARCHAR(255) NOT NULL,
                        stop VARCHAR(255) NOT NULL,
                        is_driver BOOLEAN NOT NULL DEFAULT FALSE,
                        accepted BOOLEAN NOT NULL DEFAULT FALSE,
                        PRIMARY KEY(drive_id, user_id),
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
INSERT INTO user (role_id, username, salt, password_hash, first_name, last_name, phone_number, email, gender, date_of_birth)
    VALUES (1, 'Admin', -2883142073796788660, '8dc0e2ab-4bf1-7671-c0c4-d22ffb55ee59', 'Admin', 'Admin', '0701234', 'admin@admin', 1, CURRENT_TIMESTAMP()),
           (2, 'Test', 5336889820313124494, '144141f3-c868-85e8-0243-805ca28cdabd', 'Test', 'Test', '0701234', 'test@test', 1, CURRENT_TIMESTAMP());