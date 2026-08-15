CREATE TABLE auth_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES customer_users(id) ON DELETE CASCADE,
    token VARCHAR(100) NOT NULL UNIQUE,
    type VARCHAR(30) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP
);

CREATE INDEX auth_tokens_user_type_idx ON auth_tokens (user_id, type);
