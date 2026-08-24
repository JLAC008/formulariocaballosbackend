WITH duplicates AS (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY LOWER(name) ORDER BY id) AS duplicate_index
    FROM bonus_packs
)
UPDATE bonus_packs bp
SET name = bp.name || ' (' || duplicates.duplicate_index || ')',
    updated_at = CURRENT_TIMESTAMP
FROM duplicates
WHERE bp.id = duplicates.id
  AND duplicates.duplicate_index > 1;

CREATE UNIQUE INDEX bonus_packs_name_lower_key ON bonus_packs (LOWER(name));
