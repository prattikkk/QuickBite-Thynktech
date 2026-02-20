-- QuickBite E2E Test Seed Data
-- Creates known test users for E2E testing with fixed UUIDs
-- Run this before executing E2E tests to ensure consistent test data
-- Password for all test accounts: 'Test@1234'

-- Clean up existing test data (in case of re-run)
DELETE FROM delivery_status WHERE order_id IN (SELECT id FROM orders WHERE customer_id IN (SELECT id FROM users WHERE email LIKE 'e2e_%@test.com'));
DELETE FROM order_items WHERE order_id IN (SELECT id FROM orders WHERE customer_id IN (SELECT id FROM users WHERE email LIKE 'e2e_%@test.com'));
DELETE FROM payments WHERE order_id IN (SELECT id FROM orders WHERE customer_id IN (SELECT id FROM users WHERE email LIKE 'e2e_%@test.com'));
DELETE FROM orders WHERE customer_id IN (SELECT id FROM users WHERE email LIKE 'e2e_%@test.com');
DELETE FROM menu_items WHERE vendor_id IN (SELECT id FROM vendors WHERE user_id IN (SELECT id FROM users WHERE email LIKE 'e2e_%@test.com'));
DELETE FROM vendors WHERE user_id IN (SELECT id FROM users WHERE email LIKE 'e2e_%@test.com');
DELETE FROM addresses WHERE user_id IN (SELECT id FROM users WHERE email LIKE 'e2e_%@test.com');
DELETE FROM users WHERE email LIKE 'e2e_%@test.com';

-- Get role IDs for reference
-- CUSTOMER=33333333-3333-3333-3333-333333333333
-- VENDOR=22222222-2222-2222-2222-222222222222
-- DRIVER=44444444-4444-4444-4444-444444444444
-- ADMIN=11111111-1111-1111-1111-111111111111

-- Create test users (password hash for 'Test@1234')
-- Schema: id, email, password_hash, name, role_id, created_at, updated_at

-- Test Customer (role_id='33333333-3333-3333-3333-333333333333' for CUSTOMER)
INSERT INTO users (id, email, password_hash, name, role_id, created_at, updated_at)
VALUES 
    ('55555555-5555-5555-5555-555555555555', 'e2e_customer@test.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'E2E Test Customer', '33333333-3333-3333-3333-333333333333', NOW(), NOW());

-- Test Vendor User (role_id='22222222-2222-2222-2222-222222222222' for VENDOR)
INSERT INTO users (id, email, password_hash, name, role_id, created_at, updated_at)
VALUES 
    ('66666666-6666-6666-6666-666666666666', 'e2e_vendor@test.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'E2E Test Vendor', '22222222-2222-2222-2222-222222222222', NOW(), NOW());

-- Test Driver User (role_id='44444444-4444-4444-4444-444444444444' for DRIVER)
INSERT INTO users (id, email, password_hash, name, role_id, created_at, updated_at)
VALUES 
    ('77777777-7777-7777-7777-777777777777', 'e2e_driver@test.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'E2E Test Driver', '44444444-4444-4444-4444-444444444444', NOW(), NOW());

-- Test Admin User (role_id='11111111-1111-1111-1111-111111111111' for ADMIN)
INSERT INTO users (id, email, password_hash, name, role_id, created_at, updated_at)
VALUES 
    ('88888888-8888-8888-8888-888888888888', 'e2e_admin@test.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'E2E Test Admin', '11111111-1111-1111-1111-111111111111', NOW(), NOW());

-- Create addresses for test users
-- Schema: id, user_id, line1, line2, city, state, postal, country, lat, lng, is_default, created_at, updated_at
INSERT INTO addresses (id, user_id, line1, line2, city, state, postal, country, lat, lng, is_default)
VALUES 
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '55555555-5555-5555-5555-555555555555', '100 E2E Test Street', 'Apt 1A', 'Test City', 'CA', '94000', 'USA', 37.7749, -122.4194, true),
    ('cccccccc-cccc-cccc-cccc-cccccccccccc', '66666666-6666-6666-6666-666666666666', '200 E2E Vendor Street', NULL, 'Test City', 'CA', '94001', 'USA', 37.7849, -122.4094, true);

-- Create test vendor
-- Schema: id, user_id, name, description, address, lat, lng, open_hours, rating, created_at, updated_at, active
INSERT INTO vendors (id, user_id, name, description, address, lat, lng, rating, active)
VALUES 
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '66666666-6666-6666-6666-666666666666', 'E2E Test Restaurant', 'Test restaurant for E2E testing', '200 E2E Vendor Street, Test City, CA', 37.7849, -122.4094, 4.5, true);

-- Create test menu items
-- Schema: id, vendor_id, name, description, price_cents, available, prep_time_mins, category, image_url, created_at, updated_at
-- Note: price_cents is in cents, so $12.99 = 1299 cents
INSERT INTO menu_items (id, vendor_id, name, description, price_cents, category, available, prep_time_mins)
VALUES 
    ('dddddddd-dddd-dddd-dddd-dddddddddddd', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'E2E Test Pizza', 'Delicious test pizza', 1299, 'Main Course', true, 20),
    ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'E2E Test Pasta', 'Amazing test pasta', 1099, 'Main Course', true, 15),
    ('ffffffff-ffff-ffff-ffff-ffffffffffff', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'E2E Test Salad', 'Fresh test salad', 799, 'Appetizer', true, 5);

-- Display test credentials
SELECT '=== E2E Test Users ===' as info;
SELECT email, name, 'Test@1234' as password
FROM users 
WHERE email LIKE 'e2e_%@test.com'
ORDER BY email;

SELECT '=== Test Vendor ===' as info;
SELECT v.id, v.name, v.address, u.email as vendor_email
FROM vendors v
JOIN users u ON v.user_id = u.id
WHERE u.email = 'e2e_vendor@test.com';

SELECT '=== Test Menu Items ===' as info;
SELECT mi.id, mi.name, mi.price_cents, mi.category, v.name as vendor_name
FROM menu_items mi
JOIN vendors v ON mi.vendor_id = v.id
JOIN users u ON v.user_id = u.id
WHERE u.email = 'e2e_vendor@test.com';

-- Display IDs for reference in tests
SELECT 
    'CUSTOMER_ID' as key, id::text as value FROM users WHERE email = 'e2e_customer@test.com'
UNION ALL
SELECT 
    'VENDOR_USER_ID' as key, id::text as value FROM users WHERE email = 'e2e_vendor@test.com'
UNION ALL
SELECT 
    'DRIVER_ID' as key, id::text as value FROM users WHERE email = 'e2e_driver@test.com'
UNION ALL
SELECT 
    'ADMIN_ID' as key, id::text as value FROM users WHERE email = 'e2e_admin@test.com'
UNION ALL
SELECT 
    'VENDOR_ID' as key, id::text as value FROM vendors WHERE user_id = (SELECT id FROM users WHERE email = 'e2e_vendor@test.com')
UNION ALL
SELECT 
    'MENU_ITEM_PIZZA_ID' as key, id::text as value FROM menu_items WHERE name = 'E2E Test Pizza'
UNION ALL
SELECT 
    'MENU_ITEM_PASTA_ID' as key, id::text as value FROM menu_items WHERE name = 'E2E Test Pasta'
UNION ALL
SELECT 
    'CUSTOMER_ADDRESS_ID' as key, id::text as value FROM addresses WHERE user_id = (SELECT id FROM users WHERE email = 'e2e_customer@test.com');
