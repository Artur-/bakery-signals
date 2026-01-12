-- Customers table for customer information
CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(50) NOT NULL,
    email VARCHAR(255),
    billing_info VARCHAR(500),
    notes TEXT,
    version BIGINT NOT NULL DEFAULT 0
);

-- Indexes for autocomplete and search
CREATE INDEX idx_customers_name ON customers(name);
CREATE INDEX idx_customers_phone ON customers(phone);
