-- Insert initial admin user
-- Password: admin (BCrypt hash with strength 10)
-- Generated using: htpasswd -bnBC 10 "" admin
INSERT INTO users (username, password_hash, full_name, role, active)
VALUES ('admin', '$2y$10$k8soHyIPuBv3bTIRLWr/KuNU8Bej.jFcQUcGMwbpAXTQu.oHBp0Ry', 'Administrator', 'ADMIN', true);

-- Insert sample products
INSERT INTO products (name, description, price, available) VALUES
('Chocolate Cake', 'Rich chocolate layer cake', 45.00, true),
('Vanilla Cake', 'Classic vanilla sponge cake', 40.00, true),
('Red Velvet Cake', 'Elegant red velvet with cream cheese frosting', 50.00, true),
('Cupcakes (dozen)', 'Assorted cupcakes, 12 pieces', 30.00, true),
('Cookies (dozen)', 'Freshly baked cookies, 12 pieces', 15.00, true),
('Birthday Cake Special', 'Customizable birthday cake', 55.00, true);

-- Insert sample customers
INSERT INTO customers (name, phone, email) VALUES
('John Doe', '+1-555-0101', 'john.doe@email.com'),
('Jane Smith', '+1-555-0102', 'jane.smith@email.com'),
('Bob Johnson', '+1-555-0103', 'bob.johnson@email.com'),
('Alice Williams', '+1-555-0104', 'alice.williams@email.com');
