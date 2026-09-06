ALTER TABLE IF EXISTS stripe_bonus_payments RENAME TO redsys_bonus_payments;

ALTER TABLE IF EXISTS redsys_bonus_payments
    RENAME COLUMN session_id TO order_id;

ALTER INDEX IF EXISTS stripe_bonus_payments_user_idx RENAME TO redsys_bonus_payments_user_idx;
