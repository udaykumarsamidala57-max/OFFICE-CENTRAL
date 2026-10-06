-- Run once on the SRS/authentication database, where the `users` table exists.
-- DBUtil1 maps the SRS connection to the `inventory` database in this project.
USE inventory;

CREATE TABLE IF NOT EXISTS app_pages (
    page_id INT NOT NULL AUTO_INCREMENT,
    page_code VARCHAR(100) NOT NULL,
    page_name VARCHAR(150) NOT NULL,
    page_url VARCHAR(255) NOT NULL,
    PRIMARY KEY (page_id),
    UNIQUE KEY uq_app_pages_code (page_code),
    UNIQUE KEY uq_app_pages_url (page_url)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS app_page_buttons (
    button_id INT NOT NULL AUTO_INCREMENT,
    page_id INT NOT NULL,
    button_code VARCHAR(100) NOT NULL,
    button_name VARCHAR(150) NOT NULL,
    PRIMARY KEY (button_id),
    UNIQUE KEY uq_page_button_code (page_id, button_code),
    CONSTRAINT fk_page_buttons_page FOREIGN KEY (page_id)
        REFERENCES app_pages(page_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS user_page_access (
    user_id INT NOT NULL,
    page_id INT NOT NULL,
    PRIMARY KEY (user_id, page_id),
    CONSTRAINT fk_user_page_access_page FOREIGN KEY (page_id)
        REFERENCES app_pages(page_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS user_page_button_access (
    user_id INT NOT NULL,
    button_id INT NOT NULL,
    PRIMARY KEY (user_id, button_id),
    CONSTRAINT fk_user_button_access_button FOREIGN KEY (button_id)
        REFERENCES app_page_buttons(button_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Role names are read from DISTINCT users.role; this table stores role-level grants.
CREATE TABLE IF NOT EXISTS role_page_access (
    role VARCHAR(20) NOT NULL,
    page_id INT NOT NULL,
    PRIMARY KEY (role, page_id),
    CONSTRAINT fk_role_page_access_page FOREIGN KEY (page_id)
        REFERENCES app_pages(page_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS role_page_button_access (
    role VARCHAR(20) NOT NULL,
    button_id INT NOT NULL,
    PRIMARY KEY (role, button_id),
    CONSTRAINT fk_role_button_access_button FOREIGN KEY (button_id)
        REFERENCES app_page_buttons(button_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Verify the target database and required tables after running this script.
SELECT DATABASE() AS schema_used;
SHOW TABLES LIKE 'app_pages';
SHOW TABLES LIKE 'app_page_buttons';
SHOW TABLES LIKE 'user_page_access';
SHOW TABLES LIKE 'user_page_button_access';
SHOW TABLES LIKE 'role_page_access';
SHOW TABLES LIKE 'role_page_button_access';
