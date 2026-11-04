CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username      VARCHAR(255) NOT NULL
                  CONSTRAINT ck_users_username_not_blank CHECK (btrim(username) <> ''),
    password_hash TEXT NOT NULL
                  CONSTRAINT ck_users_password_hash_not_blank CHECK (btrim(password_hash) <> ''),
    role          VARCHAR(32) NOT NULL
                  CONSTRAINT ck_users_role CHECK (role IN ('ADMIN', 'DATA_STEWARD')),
    display_name  VARCHAR(255) NOT NULL
                  CONSTRAINT ck_users_display_name_not_blank CHECK (btrim(display_name) <> ''),
    email         VARCHAR(320) NOT NULL
                  CONSTRAINT ck_users_email_not_blank CHECK (btrim(email) <> ''),
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_users_username_lower ON users (lower(username));
CREATE UNIQUE INDEX ux_users_email_lower ON users (lower(email));

ALTER TABLE datasets
    ADD CONSTRAINT fk_datasets_owner
    FOREIGN KEY (owner_id) REFERENCES users (id);
