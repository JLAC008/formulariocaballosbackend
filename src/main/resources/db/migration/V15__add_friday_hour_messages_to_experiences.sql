ALTER TABLE experiences
    ADD COLUMN IF NOT EXISTS friday_hour_messages TEXT NOT NULL DEFAULT '{}';
