CREATE TABLE users
(
    id         SERIAL PRIMARY KEY,
    first_name VARCHAR(50)  NOT NULL,
    last_name  VARCHAR(50)  NOT NULL,
    email      VARCHAR(150) NOT NULL UNIQUE,
    password   VARCHAR(255),
    role       VARCHAR(20)  NOT NULL,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE rooms
(
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    capacity    INTEGER      NOT NULL CHECK (capacity >= 1 AND capacity <= 1000),
    location    VARCHAR(100),
    available   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE room_equipment
(
    id        SERIAL PRIMARY KEY,
    room_id   INTEGER     NOT NULL REFERENCES rooms (id) ON DELETE CASCADE,
    equipment VARCHAR(50) NOT NULL,
    CONSTRAINT uq_room_equipment UNIQUE (room_id, equipment)
);

CREATE TABLE reservations
(
    id                SERIAL PRIMARY KEY,
    start_date_time   TIMESTAMP    NOT NULL,
    end_date_time     TIMESTAMP    NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    purpose           VARCHAR(255),
    number_of_people  INTEGER      NOT NULL,
    user_id           INTEGER      NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    room_id           INTEGER      NOT NULL REFERENCES rooms (id) ON DELETE RESTRICT,
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_reservations_room_id ON reservations (room_id);
CREATE INDEX idx_reservations_user_id ON reservations (user_id);

CREATE TABLE files
(
    id           SERIAL PRIMARY KEY,
    filename     VARCHAR(255) NOT NULL,
    stored_path  VARCHAR(500) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size         BIGINT       NOT NULL,
    room_id      INTEGER      NOT NULL REFERENCES rooms (id) ON DELETE CASCADE,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_files_room_id ON files (room_id);