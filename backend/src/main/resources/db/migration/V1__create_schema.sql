-- PeopleDesk schema. Kept to portable SQL so the same migration runs on MySQL and on H2 (tests).

CREATE TABLE departments (
    id   BIGINT      NOT NULL AUTO_INCREMENT,
    name VARCHAR(60) NOT NULL,
    CONSTRAINT pk_departments PRIMARY KEY (id),
    CONSTRAINT uk_departments_name UNIQUE (name)
);

CREATE TABLE users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    username      VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,          -- BCrypt hash, never plain text
    full_name     VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT ck_users_role CHECK (role IN ('ADMIN', 'USER'))
);

CREATE TABLE employees (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    name          VARCHAR(80)  NOT NULL,
    email         VARCHAR(254) NOT NULL,          -- stored lower-case by the service layer
    phone         VARCHAR(20)  NOT NULL,
    title         VARCHAR(80)  NOT NULL,
    department_id BIGINT       NOT NULL,
    status        VARCHAR(20)  NOT NULL,
    joined_on     DATE         NOT NULL,
    created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_employees PRIMARY KEY (id),
    CONSTRAINT uk_employees_email UNIQUE (email),
    CONSTRAINT fk_employees_department FOREIGN KEY (department_id) REFERENCES departments (id),
    CONSTRAINT ck_employees_status CHECK (status IN ('ACTIVE', 'ON_LEAVE', 'INACTIVE'))
);

CREATE INDEX idx_employees_joined_on ON employees (joined_on);
CREATE INDEX idx_employees_status ON employees (status);
CREATE INDEX idx_employees_name ON employees (name);

-- Refresh tokens are stored as SHA-256 hashes so a database leak does not expose live sessions.
CREATE TABLE refresh_tokens (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    token_hash VARCHAR(64) NOT NULL,
    user_id    BIGINT      NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT uk_refresh_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- Access tokens invalidated by logout, kept only until they would have expired anyway.
CREATE TABLE revoked_access_tokens (
    jti        VARCHAR(36) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_revoked_access_tokens PRIMARY KEY (jti)
);
