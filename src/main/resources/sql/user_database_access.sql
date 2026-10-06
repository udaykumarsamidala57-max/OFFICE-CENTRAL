-- Run this once against the database that contains the `users` authentication table.
-- Assign one row per user and selectable company database.
CREATE TABLE IF NOT EXISTS user_database_access (
    user_id INT NOT NULL,
    database_code VARCHAR(30) NOT NULL,
    PRIMARY KEY (user_id, database_code),
    CONSTRAINT fk_user_database_access_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Example assignments (replace usernames / company codes as needed):
-- INSERT INTO user_database_access (user_id, database_code)
-- SELECT id, 'SRS' FROM users WHERE username = 'your_username';
-- INSERT INTO user_database_access (user_id, database_code)
-- SELECT id, 'Sanpoly' FROM users WHERE username = 'your_username';
-- Allowed database_code values: SRS, Sanpoly, Sanpoly2.
