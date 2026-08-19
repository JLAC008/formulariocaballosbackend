CREATE TABLE IF NOT EXISTS stripe_bonus_payments (
    session_id VARCHAR(255) PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES customer_users(id),
    bonuses INTEGER NOT NULL,
    amount_cents BIGINT NOT NULL,
    currency VARCHAR(10) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS stripe_bonus_payments_user_idx ON stripe_bonus_payments(user_id);
