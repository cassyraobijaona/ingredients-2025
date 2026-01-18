INSERT INTO dish (name, dish_type, selling_price)
VALUES
    ('Salade fraîche', 'START', 3500),
    ('Poulet grillé', 'MAIN', 12000),
    ('Riz aux legumes', 'MAIN', NULL),
    ('Gâteau au chocolat', 'DESSERT', 8000),
    ('Salade de fruits', 'DESSERT', NULL)
ON CONFLICT DO NOTHING;

INSERT INTO ingredient (name, category, price)
VALUES
    ('Laitue', 'VEGETABLE', 800.0),
    ('Tomate', 'VEGETABLE', 600.0),
    ('Poulet', 'ANIMAL', 4500.0),
    ('Chocolat', 'OTHER', 3000.0),
    ('Beurre', 'DAIRY', 2500.0)
ON CONFLICT DO NOTHING;

INSERT INTO dish_ingredient (dish_id, ingredient_id, required_quantity, unit)
VALUES
    (1, 1, 1, 'piece'),
    (1, 2, 0.25, 'KG'),
    (2, 3, 0.5, 'KG'),
    (2, 5, 0.15, 'L'),
    (4, 4, 0.2, 'KG'),
    (4, 5, 0.1, 'KG')
ON CONFLICT DO NOTHING;

UPDATE dish
SET selling_price = 3500
WHERE name = 'Salade fraîche';

UPDATE dish
SET selling_price = 12000
WHERE name = 'Poulet grillé';
