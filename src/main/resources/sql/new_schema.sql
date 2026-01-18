ALTER TABLE ingredient
DROP COLUMN IF EXISTS id_dish;

ALTER TABLE dish
    ADD COLUMN IF NOT EXISTS selling_price NUMERIC;

CREATE TABLE IF NOT EXISTS dish_ingredient (
                                               id SERIAL PRIMARY KEY,
                                               dish_id INT NOT NULL REFERENCES dish(id) ON DELETE CASCADE,
    ingredient_id INT NOT NULL REFERENCES ingredient(id) ON DELETE CASCADE,
    required_quantity NUMERIC,
    unit VARCHAR(10)
    );
