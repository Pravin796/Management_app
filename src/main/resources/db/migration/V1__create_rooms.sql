CREATE TABLE rooms (
                       id BIGSERIAL PRIMARY KEY,

                       room_number VARCHAR(20) NOT NULL,

                       status VARCHAR(20) NOT NULL,

                       maintenance_exempt BOOLEAN NOT NULL DEFAULT FALSE,

                       created_at TIMESTAMP NOT NULL,

                       updated_at TIMESTAMP NOT NULL,

                       CONSTRAINT uk_room_room_number UNIQUE (room_number)
);