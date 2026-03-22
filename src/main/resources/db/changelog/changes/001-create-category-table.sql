CREATE TABLE categories
(
    id        UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    name      VARCHAR(255) NOT NULL UNIQUE,
    parent_id UUID         REFERENCES categories (id) ON DELETE SET NULL
);