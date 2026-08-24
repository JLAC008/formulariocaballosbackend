UPDATE experiences
SET price = 1
WHERE price IS NULL OR price < 1 OR price > 10;
