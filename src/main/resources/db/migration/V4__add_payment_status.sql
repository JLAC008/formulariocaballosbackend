ALTER TABLE bookings ADD COLUMN IF NOT EXISTS payment_status VARCHAR(30) NOT NULL DEFAULT 'PENDING';

CREATE UNIQUE INDEX IF NOT EXISTS bookings_active_slot_idx
    ON bookings (experience_id, date_key, hour)
    WHERE status <> 'CANCELLED';
