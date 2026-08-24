CREATE TABLE bonus_packs (
    id BIGINT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    bonuses INTEGER NOT NULL,
    price_cents BIGINT NOT NULL,
    currency VARCHAR(8) NOT NULL DEFAULT 'eur',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO bonus_packs (id, name, bonuses, price_cents, currency, active)
VALUES
  (1, 'Pack 10 bonos', 10, 16000, 'eur', TRUE),
  (2, 'Pack 5 bonos', 5, 10000, 'eur', TRUE);

ALTER TABLE stripe_bonus_payments
ADD COLUMN bonus_pack_id BIGINT;
