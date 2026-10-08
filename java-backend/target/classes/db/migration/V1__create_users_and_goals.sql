CREATE TABLE users (
    id            UUID                        PRIMARY KEY,
    email         VARCHAR(320)                NOT NULL,
    password_hash VARCHAR(100)                NOT NULL,
    display_name  VARCHAR(120),
    role          VARCHAR(20)                 NOT NULL DEFAULT 'USER',
    created_at    TIMESTAMP WITH TIME ZONE    NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP WITH TIME ZONE    NOT NULL DEFAULT now(),
    CONSTRAINT users_role_check CHECK (role IN ('USER', 'ADMIN')),
    CONSTRAINT users_email_lowercase_check CHECK (email = lower(email)),
    CONSTRAINT users_email_unique UNIQUE (email)
);

CREATE TABLE goals (
    id          UUID                        PRIMARY KEY,
    user_id     UUID                        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title       VARCHAR(200)                NOT NULL,
    description TEXT,
    status      VARCHAR(20)                 NOT NULL DEFAULT 'PENDING',
    progress    INT                         NOT NULL DEFAULT 0,
    target_date DATE,
    created_at  TIMESTAMP WITH TIME ZONE    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP WITH TIME ZONE    NOT NULL DEFAULT now(),
    CONSTRAINT goals_status_check CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'ARCHIVED')),
    CONSTRAINT goals_progress_check CHECK (progress BETWEEN 0 AND 100)
);

CREATE INDEX goals_user_id_idx ON goals (user_id);
CREATE INDEX goals_status_idx ON goals (status);
CREATE INDEX goals_user_status_idx ON goals (user_id, status);
