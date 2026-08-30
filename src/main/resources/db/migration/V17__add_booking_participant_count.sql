ALTER TABLE bookings
    ADD COLUMN IF NOT EXISTS participant_count INTEGER NOT NULL DEFAULT 1;

UPDATE bookings
SET participant_count = 1
WHERE participant_count IS NULL OR participant_count < 1;

ALTER TABLE bookings
    ADD CONSTRAINT bookings_participant_count_check CHECK (participant_count BETWEEN 1 AND 2);
