ALTER TABLE experiences
    ADD COLUMN IF NOT EXISTS capacity INTEGER;

UPDATE experiences
SET capacity = CASE
    WHEN LOWER(type) IN ('routes', 'route') THEN 8
    ELSE 5
END
WHERE capacity IS NULL;

ALTER TABLE experiences
    ALTER COLUMN capacity SET NOT NULL,
    ALTER COLUMN capacity SET DEFAULT 5;
