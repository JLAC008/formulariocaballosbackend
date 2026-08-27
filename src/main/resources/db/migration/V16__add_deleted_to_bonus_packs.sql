ALTER TABLE bonus_packs
    ADD COLUMN IF NOT EXISTS deleted boolean NOT NULL DEFAULT false;

UPDATE bonus_packs
SET deleted = false
WHERE deleted IS NULL;

DROP INDEX IF EXISTS bonus_packs_name_lower_key;

CREATE UNIQUE INDEX IF NOT EXISTS bonus_packs_name_lower_active_key
    ON bonus_packs (LOWER(name))
    WHERE deleted = false;
