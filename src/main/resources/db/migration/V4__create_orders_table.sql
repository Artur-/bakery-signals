-- Orders table for managing bakery orders
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    due_date DATE NOT NULL,
    state VARCHAR(50) NOT NULL CHECK (state IN ('NEW', 'READY', 'DELIVERED', 'CANCELLED')),
    customer_id BIGINT NOT NULL REFERENCES customers(id),
    total_price DECIMAL(10, 2) NOT NULL DEFAULT 0,
    discount DECIMAL(10, 2) DEFAULT 0,
    paid BOOLEAN NOT NULL DEFAULT false,
    pickup_location VARCHAR(50) NOT NULL CHECK (pickup_location IN ('STOREFRONT', 'PRODUCTION_FACILITY')),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by_user_id BIGINT REFERENCES users(id),
    state_changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

-- Order items table (one-to-many relationship with orders)
CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    price_per_unit DECIMAL(10, 2) NOT NULL,
    customization_specs TEXT
);

-- Indexes for performance
CREATE INDEX idx_orders_due_date ON orders(due_date);
CREATE INDEX idx_orders_state ON orders(state);
CREATE INDEX idx_orders_customer_id ON orders(customer_id);
CREATE INDEX idx_orders_created_by ON orders(created_by_user_id);
CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);
