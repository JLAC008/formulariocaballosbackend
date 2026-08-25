UPDATE bonus_packs
SET name = REPLACE(name, 'bonos', 'sesiones')
WHERE name LIKE '%bonos%';

UPDATE bonus_packs
SET name = REPLACE(name, 'Bonos', 'Sesiones')
WHERE name LIKE '%Bonos%';

UPDATE bonus_packs
SET name = REPLACE(name, 'bono', 'sesión')
WHERE name LIKE '%bono%';

UPDATE bonus_packs
SET name = REPLACE(name, 'Bono', 'Sesión')
WHERE name LIKE '%Bono%';
