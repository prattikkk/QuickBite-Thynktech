-- V3__sample_data.sql
-- QuickBite Day 2: Sample test data

-- Insert Roles
INSERT INTO roles (id, name, description) VALUES
    ('11111111-1111-1111-1111-111111111111'::uuid, 'ADMIN', 'System administrator with full access'),
    ('22222222-2222-2222-2222-222222222222'::uuid, 'VENDOR', 'Restaurant/vendor owner'),
    ('33333333-3333-3333-3333-333333333333'::uuid, 'CUSTOMER', 'End customer placing orders'),
    ('44444444-4444-4444-4444-444444444444'::uuid, 'DRIVER', 'Delivery driver');

-- Insert Sample Users (password is 'password123' hashed with BCrypt)
-- BCrypt hash for 'password123': $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldMqU0rCuC2BfDVDGpG
INSERT INTO users (id, email, password_hash, name, phone, role_id, active) VALUES
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'::uuid, 
     'admin@quickbite.test', 
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldMqU0rCuC2BfDVDGpG',
     'Admin User',
     '+91-9999999999',
     '11111111-1111-1111-1111-111111111111'::uuid,
     TRUE),
    
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'::uuid,
     'alice@quickbite.test',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldMqU0rCuC2BfDVDGpG',
     'Alice Customer',
     '+91-9876543210',
     '33333333-3333-3333-3333-333333333333'::uuid,
     TRUE),
    
    ('cccccccc-cccc-cccc-cccc-cccccccccccc'::uuid,
     'driver@quickbite.test',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldMqU0rCuC2BfDVDGpG',
     'John Driver',
     '+91-9876543211',
     '44444444-4444-4444-4444-444444444444'::uuid,
     TRUE),
    
    -- Vendor Owner Users
    ('dddddddd-dddd-dddd-dddd-dddddddddddd'::uuid,
     'tastyburger@quickbite.test',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldMqU0rCuC2BfDVDGpG',
     'Tasty Burger Owner',
     '+91-9876543212',
     '22222222-2222-2222-2222-222222222222'::uuid,
     TRUE),
    
    ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee'::uuid,
     'pizzapalace@quickbite.test',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldMqU0rCuC2BfDVDGpG',
     'Pizza Palace Owner',
     '+91-9876543213',
     '22222222-2222-2222-2222-222222222222'::uuid,
     TRUE),
    
    ('ffffffff-ffff-ffff-ffff-ffffffffffff'::uuid,
     'curryhouse@quickbite.test',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldMqU0rCuC2BfDVDGpG',
     'Curry House Owner',
     '+91-9876543214',
     '22222222-2222-2222-2222-222222222222'::uuid,
     TRUE);

-- Insert Sample Vendors
INSERT INTO vendors (id, user_id, name, description, address, lat, lng, rating, open_hours) VALUES
    ('10000001-0000-0000-0000-000000000001'::uuid,
     'dddddddd-dddd-dddd-dddd-dddddddddddd'::uuid,
     'Tasty Burger Joint',
     'Best burgers in town with fresh ingredients',
     '123 Main St, Bangalore, Karnataka',
     12.9716, 77.5946,
     4.5,
     '{"monday": "10:00-22:00", "tuesday": "10:00-22:00", "wednesday": "10:00-22:00", "thursday": "10:00-22:00", "friday": "10:00-23:00", "saturday": "10:00-23:00", "sunday": "10:00-22:00"}'::jsonb),
    
    ('10000002-0000-0000-0000-000000000002'::uuid,
     'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee'::uuid,
     'Pizza Palace',
     'Authentic Italian pizzas with wood-fired oven',
     '456 Pizza Lane, Bangalore, Karnataka',
     12.9352, 77.6245,
     4.7,
     '{"monday": "11:00-23:00", "tuesday": "11:00-23:00", "wednesday": "11:00-23:00", "thursday": "11:00-23:00", "friday": "11:00-00:00", "saturday": "11:00-00:00", "sunday": "11:00-23:00"}'::jsonb),
    
    ('10000003-0000-0000-0000-000000000003'::uuid,
     'ffffffff-ffff-ffff-ffff-ffffffffffff'::uuid,
     'Curry House',
     'Traditional Indian curries and tandoor specialties',
     '789 Spice Road, Bangalore, Karnataka',
     12.9279, 77.6271,
     4.3,
     '{"monday": "12:00-15:00,18:00-22:00", "tuesday": "12:00-15:00,18:00-22:00", "wednesday": "12:00-15:00,18:00-22:00", "thursday": "12:00-15:00,18:00-22:00", "friday": "12:00-15:00,18:00-23:00", "saturday": "12:00-15:00,18:00-23:00", "sunday": "12:00-15:00,18:00-22:00"}'::jsonb);

-- Insert Menu Items for Tasty Burger Joint
INSERT INTO menu_items (vendor_id, name, description, price_cents, prep_time_mins, category, available) VALUES
    ('10000001-0000-0000-0000-000000000001'::uuid, 'Classic Beef Burger', 'Juicy beef patty with lettuce, tomato, and special sauce', 29900, 15, 'Burgers', TRUE),
    ('10000001-0000-0000-0000-000000000001'::uuid, 'Chicken Burger', 'Crispy fried chicken with mayo and pickles', 24900, 12, 'Burgers', TRUE),
    ('10000001-0000-0000-0000-000000000001'::uuid, 'Veggie Burger', 'Plant-based patty with avocado and veggies', 22900, 10, 'Burgers', TRUE),
    ('10000001-0000-0000-0000-000000000001'::uuid, 'French Fries', 'Crispy golden fries with seasoning', 9900, 8, 'Sides', TRUE),
    ('10000001-0000-0000-0000-000000000001'::uuid, 'Chocolate Shake', 'Rich chocolate milkshake', 14900, 5, 'Beverages', TRUE);

-- Insert Menu Items for Pizza Palace
INSERT INTO menu_items (vendor_id, name, description, price_cents, prep_time_mins, category, available) VALUES
    ('10000002-0000-0000-0000-000000000002'::uuid, 'Margherita Pizza', 'Classic tomato, mozzarella, and basil', 39900, 20, 'Pizza', TRUE),
    ('10000002-0000-0000-0000-000000000002'::uuid, 'Pepperoni Pizza', 'Loaded with pepperoni and cheese', 49900, 20, 'Pizza', TRUE),
    ('10000002-0000-0000-0000-000000000002'::uuid, 'Veggie Supreme', 'Bell peppers, onions, mushrooms, olives', 44900, 22, 'Pizza', TRUE),
    ('10000002-0000-0000-0000-000000000002'::uuid, 'Garlic Bread', 'Toasted bread with garlic butter', 12900, 10, 'Sides', TRUE),
    ('10000002-0000-0000-0000-000000000002'::uuid, 'Tiramisu', 'Classic Italian dessert', 19900, 5, 'Desserts', TRUE);

-- Insert Menu Items for Curry House
INSERT INTO menu_items (vendor_id, name, description, price_cents, prep_time_mins, category, available) VALUES
    ('10000003-0000-0000-0000-000000000003'::uuid, 'Butter Chicken', 'Creamy tomato curry with tender chicken', 34900, 25, 'Mains', TRUE),
    ('10000003-0000-0000-0000-000000000003'::uuid, 'Paneer Tikka Masala', 'Cottage cheese in spiced tomato gravy', 29900, 20, 'Mains', TRUE),
    ('10000003-0000-0000-0000-000000000003'::uuid, 'Garlic Naan', 'Fresh baked bread with garlic', 7900, 8, 'Breads', TRUE),
    ('10000003-0000-0000-0000-000000000003'::uuid, 'Biryani', 'Fragrant rice with chicken and spices', 32900, 30, 'Rice', TRUE),
    ('10000003-0000-0000-0000-000000000003'::uuid, 'Mango Lassi', 'Sweet yogurt drink with mango', 9900, 5, 'Beverages', TRUE);

-- Insert Sample Address for Alice
INSERT INTO addresses (user_id, line1, line2, city, state, postal, country, lat, lng, is_default) VALUES
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'::uuid,
     '42 Residential Complex',
     'Near City Mall',
     'Bangalore',
     'Karnataka',
     '560001',
     'India',
     12.9716, 77.5946,
     TRUE);

-- Insert a Sample Order (for testing)
INSERT INTO orders (id, customer_id, vendor_id, total_cents, status, created_at) VALUES
    ('20000001-0000-0000-0000-000000000001'::uuid,
     'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'::uuid,
     '10000001-0000-0000-0000-000000000001'::uuid,
     69700,
     'DELIVERED',
     NOW() - INTERVAL '2 days');

-- Insert Order Items for the sample order
INSERT INTO order_items (order_id, menu_item_id, quantity, price_cents) VALUES
    ('20000001-0000-0000-0000-000000000001'::uuid,
     (SELECT id FROM menu_items WHERE name = 'Classic Beef Burger' LIMIT 1),
     2,
     29900),
    ('20000001-0000-0000-0000-000000000001'::uuid,
     (SELECT id FROM menu_items WHERE name = 'French Fries' LIMIT 1),
     1,
     9900);

-- Insert Payment for the sample order
INSERT INTO payments (order_id, amount_cents, status, provider, paid_at) VALUES
    ('20000001-0000-0000-0000-000000000001'::uuid,
     69700,
     'SUCCESS',
     'RAZORPAY',
     NOW() - INTERVAL '2 days');

-- Insert Delivery Status entries for the sample order
INSERT INTO delivery_status (order_id, status, changed_at, note) VALUES
    ('20000001-0000-0000-0000-000000000001'::uuid, 'PLACED', NOW() - INTERVAL '2 days', 'Order placed successfully'),
    ('20000001-0000-0000-0000-000000000001'::uuid, 'CONFIRMED', NOW() - INTERVAL '2 days' + INTERVAL '2 minutes', 'Vendor confirmed the order'),
    ('20000001-0000-0000-0000-000000000001'::uuid, 'PREPARING', NOW() - INTERVAL '2 days' + INTERVAL '5 minutes', 'Food is being prepared'),
    ('20000001-0000-0000-0000-000000000001'::uuid, 'READY', NOW() - INTERVAL '2 days' + INTERVAL '20 minutes', 'Order ready for pickup'),
    ('20000001-0000-0000-0000-000000000001'::uuid, 'DELIVERED', NOW() - INTERVAL '2 days' + INTERVAL '40 minutes', 'Order delivered successfully');

-- Verification queries
SELECT 'Roles inserted:' AS info, COUNT(*) AS count FROM roles;
SELECT 'Users inserted:' AS info, COUNT(*) AS count FROM users;
SELECT 'Vendors inserted:' AS info, COUNT(*) AS count FROM vendors;
SELECT 'Menu items inserted:' AS info, COUNT(*) AS count FROM menu_items;
SELECT 'Addresses inserted:' AS info, COUNT(*) AS count FROM addresses;
SELECT 'Orders inserted:' AS info, COUNT(*) AS count FROM orders;
SELECT 'Order items inserted:' AS info, COUNT(*) AS count FROM order_items;
SELECT 'Payments inserted:' AS info, COUNT(*) AS count FROM payments;
SELECT 'Delivery status entries:' AS info, COUNT(*) AS count FROM delivery_status;
