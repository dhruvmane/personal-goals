CREATE TABLE refresh_tokens (
    id          UUID                        PRIMARY KEY,
    user_id     UUID                        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  VARCHAR(64)                 NOT NULL,
    expires_at  TIMESTAMP WITH TIME ZONE    NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE    NOT NULL DEFAULT now(),
    rotated_at  TIMESTAMP WITH TIME ZONE,
    CONSTRAINT refresh_tokens_token_hash_unique UNIQUE (token_hash)
);

CREATE INDEX refresh_tokens_user_id_idx ON refresh_tokens (user_id);
CREATE INDEX refresh_tokens_expires_at_idx ON refresh_tokens (expires_at);
