CREATE TABLE products
(
    id          UUID                     NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    sku         VARCHAR(255)             NOT NULL UNIQUE,
    name        VARCHAR(255)             NOT NULL,
    description TEXT,
    price       NUMERIC(19, 2)           NOT NULL,
    currency    VARCHAR(3)               NOT NULL DEFAULT 'EUR',
    category_id UUID                     REFERENCES categories (id) ON DELETE SET NULL,
    available   BOOLEAN                  NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_products_sku ON products (sku);