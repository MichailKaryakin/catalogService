CREATE TABLE stocks
(
    id                 UUID    NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    product_id         UUID    NOT NULL UNIQUE REFERENCES products (id) ON DELETE CASCADE,
    quantity           INTEGER NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    reserved           INTEGER NOT NULL DEFAULT 0 CHECK (reserved >= 0),
    warehouse_location VARCHAR(255)
);