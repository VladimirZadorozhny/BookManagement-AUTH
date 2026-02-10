-- Flyway migration: Security Schema & Initial Roles

-- ============================================================
-- ROLES & USER_ROLES TABLES
-- ============================================================

CREATE TABLE IF NOT EXISTS roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS users_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_users_roles_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_users_roles_role FOREIGN KEY (role_id) REFERENCES roles(id)
);

-- ============================================================
-- UPDATE USERS TABLE
-- ============================================================

ALTER TABLE users ADD COLUMN password VARCHAR(255);
ALTER TABLE users ADD COLUMN active BOOLEAN DEFAULT TRUE;

-- ============================================================
-- INITIAL DATA
-- ============================================================

-- Insert default roles
INSERT INTO roles (name) VALUES ('ROLE_USER'), ('ROLE_ADMIN');

-- Migrating existing users: set password to hashed 'password'
-- BCrypt hash for 'password'
UPDATE users SET password = '{bcrypt}$2a$10$8.UnVuG9HHgffUDalk8qfOuVGkqCYAdVqvoLSu5DM6WTPPD8LsW1q';

-- Assign ROLE_USER to all existing users
INSERT INTO users_roles (user_id, role_id)
SELECT id, (SELECT id FROM roles WHERE name = 'ROLE_USER') FROM users;

-- Create default admin user (admin@library.com / admin)
-- BCrypt hash for 'admin'
INSERT INTO users (name, email, password, active)
VALUES ('System Admin', 'admin@library.com', '{bcrypt}$2a$10$hKDVYxLefVHV/vtuPhWD3u6y9xe.S.A9xpsOInT0m2W14iR7s9B72', TRUE);

-- Assign ROLE_ADMIN to the new admin user
INSERT INTO users_roles (user_id, role_id)
VALUES (
    (SELECT id FROM users WHERE email = 'admin@library.com'),
    (SELECT id FROM roles WHERE name = 'ROLE_ADMIN')
);

-- Also assign ROLE_USER to admin
INSERT INTO users_roles (user_id, role_id)
VALUES (
    (SELECT id FROM users WHERE email = 'admin@library.com'),
    (SELECT id FROM roles WHERE name = 'ROLE_USER')
);
