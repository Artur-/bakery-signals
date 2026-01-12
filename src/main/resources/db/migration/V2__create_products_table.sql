-- Products table for bakery items catalog
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL,
    available BOOLEAN NOT NULL DEFAULT true,
    version BIGINT NOT NULL DEFAULT 0
);

-- Index for faster searches
CREATE INDEX idx_products_available ON products(available);
CREATE INDEX idx_products_name ON products(name);
