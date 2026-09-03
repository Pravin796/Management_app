CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,

                       room_id BIGINT NOT NULL,

                       mobile_number VARCHAR(15) NOT NULL,

                       password VARCHAR(255) NOT NULL,

                       role VARCHAR(20) NOT NULL,

                       status VARCHAR(20) NOT NULL,

                       created_at TIMESTAMP NOT NULL,

                       updated_at TIMESTAMP NOT NULL,

                       CONSTRAINT uk_user_mobile_number
                           UNIQUE (mobile_number),

                       CONSTRAINT uk_user_room
                           UNIQUE (room_id),

                       CONSTRAINT fk_user_room
                           FOREIGN KEY (room_id)
                               REFERENCES rooms(id)
);