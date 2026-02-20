# SQL Test Queries

This document contains SQL queries for manual testing and integration test verification. These queries demonstrate common use cases and data relationships.

## Setup

Connect to the database:
```bash
docker exec -it quickbite-postgres psql -U dbuser -d quickbite
```

Or using a GUI client:
- **Host**: localhost
- **Port**: 5432
- **Database**: quickbite
- **Username**: dbuser
- **Password**: dbpass

---

## 1. Order Detail (Full Order View with Customer, Vendor, Items, Payment)

Get complete order information including customer, vendor, items, and payment status.

```sql
-- Replace <ORDER_UUID> with actual order ID from sample data
-- Example: '20000001-0000-0000-0000-000000000001'

SELECT 
    o.id AS order_id,
    o.status AS order_status,
    o.total_cents / 100.0 AS total_amount,
    o.created_at AS order_date,
    
    -- Customer information
    c.name AS customer_name,
    c.email AS customer_email,
    c.phone AS customer_phone,
    
    -- Vendor information
    v.name AS vendor_name,
    v.address AS vendor_address,
    v.rating AS vendor_rating,
    
    -- Delivery address
    a.line1 || ', ' || a.city AS delivery_address,
    
    -- Driver information (if assigned)
    d.name AS driver_name,
    d.phone AS driver_phone,
    
    -- Payment information
    p.status AS payment_status,
    p.provider AS payment_provider,
    p.paid_at AS payment_time
    
FROM orders o
JOIN users c ON o.customer_id = c.id
JOIN vendors v ON o.vendor_id = v.id
LEFT JOIN addresses a ON o.delivery_address_id = a.id
LEFT JOIN users d ON o.driver_id = d.id
LEFT JOIN payments p ON p.order_id = o.id
WHERE o.id = '<ORDER_UUID>';
```

**Expected Result:** Single row with complete order information.

---

## 2. Order Items Detail

Get all items in an order with menu item details.

```sql
-- Replace <ORDER_UUID> with actual order ID

SELECT 
    oi.id AS order_item_id,
    m.name AS item_name,
    m.description AS item_description,
    oi.quantity,
    oi.price_cents / 100.0 AS unit_price,
    (oi.price_cents * oi.quantity) / 100.0 AS line_total,
    oi.special_instructions,
    m.prep_time_mins
    
FROM order_items oi
JOIN menu_items m ON oi.menu_item_id = m.id
WHERE oi.order_id = '<ORDER_UUID>'
ORDER BY oi.created_at;
```

**Expected Result:** Multiple rows, one per item in the order.

**Verification:**
```sql
-- Verify order total matches sum of items
SELECT 
    o.total_cents / 100.0 AS order_total,
    SUM(oi.price_cents * oi.quantity) / 100.0 AS items_total
FROM orders o
JOIN order_items oi ON oi.order_id = o.id
WHERE o.id = '<ORDER_UUID>'
GROUP BY o.id, o.total_cents;
```

---

## 3. Vendor Menu

Get all menu items for a vendor with category grouping.

```sql
-- Replace <VENDOR_UUID> with actual vendor ID
-- Example: '10000001-0000-0000-0000-000000000001' (Tasty Burger Joint)

SELECT 
    m.id AS menu_item_id,
    m.name AS item_name,
    m.description,
    m.price_cents / 100.0 AS price,
    m.category,
    m.prep_time_mins,
    m.available,
    v.name AS vendor_name,
    v.rating AS vendor_rating
    
FROM menu_items m
JOIN vendors v ON m.vendor_id = v.id
WHERE v.id = '<VENDOR_UUID>'
ORDER BY m.category, m.name;
```

**Alternative: With Category Counts**
```sql
SELECT 
    m.category,
    COUNT(*) AS items_count,
    MIN(m.price_cents) / 100.0 AS min_price,
    MAX(m.price_cents) / 100.0 AS max_price,
    AVG(m.price_cents) / 100.0 AS avg_price
    
FROM menu_items m
WHERE m.vendor_id = '<VENDOR_UUID>' AND m.available = true
GROUP BY m.category
ORDER BY m.category;
```

---

## 4. Customer Orders with Item Counts

Get all orders for a customer with item counts and totals.

```sql
-- Replace <CUSTOMER_UUID> with actual customer ID
-- Example: 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb' (Alice)

SELECT 
    o.id AS order_id,
    o.status,
    v.name AS vendor_name,
    COUNT(oi.*) AS items_count,
    SUM(oi.price_cents * oi.quantity) / 100.0 AS calculated_total,
    o.total_cents / 100.0 AS order_total,
    o.created_at AS order_date,
    p.status AS payment_status
    
FROM orders o
LEFT JOIN order_items oi ON oi.order_id = o.id
JOIN vendors v ON o.vendor_id = v.id
LEFT JOIN payments p ON p.order_id = o.id
WHERE o.customer_id = '<CUSTOMER_UUID>'
GROUP BY o.id, v.name, p.status
ORDER BY o.created_at DESC;
```

**Expected Result:** All orders for the customer, newest first.

---

## 5. Order Status History (Delivery Tracking)

Get complete status history for an order with timestamps.

```sql
-- Replace <ORDER_UUID> with actual order ID

SELECT 
    ds.status,
    ds.changed_at,
    u.name AS changed_by_user,
    u.email AS changed_by_email,
    ds.note,
    ds.location_lat,
    ds.location_lng,
    EXTRACT(EPOCH FROM (ds.changed_at - LAG(ds.changed_at) OVER (ORDER BY ds.changed_at))) / 60 AS minutes_in_status
    
FROM delivery_status ds
LEFT JOIN users u ON ds.changed_by = u.id
WHERE ds.order_id = '<ORDER_UUID>'
ORDER BY ds.changed_at ASC;
```

**Expected Result:** Timeline of status changes with time spent in each status.

---

## 6. Active Drivers Near Location (Haversine Distance)

Find available drivers within a certain radius of a location.

```sql
-- Replace <LAT> and <LNG> with actual coordinates
-- Example: Bangalore coordinates: 12.9716, 77.5946

WITH driver_role AS (
    SELECT id FROM roles WHERE name = 'DRIVER'
)
SELECT 
    u.id,
    u.name,
    u.phone,
    u.email,
    -- Haversine formula for distance in kilometers
    (
        6371 * acos(
            cos(radians(<LAT>)) * 
            cos(radians(CAST(u_meta.lat AS FLOAT))) * 
            cos(radians(CAST(u_meta.lng AS FLOAT)) - radians(<LNG>)) + 
            sin(radians(<LAT>)) * 
            sin(radians(CAST(u_meta.lat AS FLOAT)))
        )
    ) AS distance_km
    
FROM users u
-- For now, using vendor locations as proxy for driver locations
-- In production, you'd have a separate driver_locations table
CROSS JOIN LATERAL (
    SELECT v.lat, v.lng 
    FROM vendors v 
    LIMIT 1
) AS u_meta
WHERE u.role_id = (SELECT id FROM driver_role)
    AND u.active = true
HAVING distance_km < 10
ORDER BY distance_km
LIMIT 10;
```

**Note:** This is a simplified example. In production:
1. Maintain a `driver_locations` table with real-time GPS data
2. Use PostGIS extension for efficient geospatial queries
3. Consider using a bounding box filter before Haversine calculation

---

## 7. Vendor Performance Metrics

Get key metrics for a vendor (orders, revenue, average order value).

```sql
-- Replace <VENDOR_UUID> with actual vendor ID

SELECT 
    v.name AS vendor_name,
    v.rating,
    COUNT(DISTINCT o.id) AS total_orders,
    SUM(o.total_cents) / 100.0 AS total_revenue,
    AVG(o.total_cents) / 100.0 AS avg_order_value,
    COUNT(DISTINCT CASE WHEN o.status = 'COMPLETED' THEN o.id END) AS completed_orders,
    COUNT(DISTINCT CASE WHEN o.status = 'CANCELLED' THEN o.id END) AS cancelled_orders,
    COUNT(DISTINCT m.id) AS menu_items_count,
    COUNT(DISTINCT CASE WHEN m.available THEN m.id END) AS available_items_count
    
FROM vendors v
LEFT JOIN orders o ON o.vendor_id = v.id
LEFT JOIN menu_items m ON m.vendor_id = v.id
WHERE v.id = '<VENDOR_UUID>'
GROUP BY v.id, v.name, v.rating;
```

---

## 8. Popular Menu Items (Most Ordered)

Find the most popular menu items across all vendors or for a specific vendor.

```sql
-- Top 10 most ordered items (all vendors)
SELECT 
    m.name AS item_name,
    v.name AS vendor_name,
    COUNT(DISTINCT oi.order_id) AS times_ordered,
    SUM(oi.quantity) AS total_quantity_sold,
    SUM(oi.price_cents * oi.quantity) / 100.0 AS total_revenue,
    m.price_cents / 100.0 AS current_price
    
FROM order_items oi
JOIN menu_items m ON oi.menu_item_id = m.id
JOIN vendors v ON m.vendor_id = v.id
GROUP BY m.id, m.name, v.name, m.price_cents
ORDER BY times_ordered DESC, total_quantity_sold DESC
LIMIT 10;
```

```sql
-- For a specific vendor
-- Replace <VENDOR_UUID> with actual vendor ID

SELECT 
    m.name AS item_name,
    m.category,
    COUNT(DISTINCT oi.order_id) AS times_ordered,
    SUM(oi.quantity) AS total_quantity_sold,
    SUM(oi.price_cents * oi.quantity) / 100.0 AS revenue_generated
    
FROM menu_items m
JOIN order_items oi ON oi.menu_item_id = m.id
WHERE m.vendor_id = '<VENDOR_UUID>'
GROUP BY m.id, m.name, m.category
ORDER BY times_ordered DESC
LIMIT 10;
```

---

## 9. Payment Status Summary

Get payment statistics by status.

```sql
SELECT 
    p.status,
    COUNT(*) AS payment_count,
    SUM(p.amount_cents) / 100.0 AS total_amount,
    AVG(p.amount_cents) / 100.0 AS avg_amount,
    MIN(p.created_at) AS first_payment,
    MAX(p.created_at) AS last_payment
    
FROM payments p
GROUP BY p.status
ORDER BY payment_count DESC;
```

---

## 10. User Activity Audit

Get recent activity for a user from audit logs.

```sql
-- Replace <USER_UUID> with actual user ID

SELECT 
    al.action,
    al.entity,
    al.entity_id,
    al.created_at,
    al.ip_address,
    al.meta->>'details' AS details
    
FROM audit_logs al
WHERE al.user_id = '<USER_UUID>'
ORDER BY al.created_at DESC
LIMIT 20;
```

---

## 11. Verification Queries

Run these after loading sample data to verify everything is set up correctly.

```sql
-- Count records in each table
SELECT 'roles' AS table_name, COUNT(*) AS count FROM roles
UNION ALL
SELECT 'users', COUNT(*) FROM users
UNION ALL
SELECT 'vendors', COUNT(*) FROM vendors
UNION ALL
SELECT 'menu_items', COUNT(*) FROM menu_items
UNION ALL
SELECT 'addresses', COUNT(*) FROM addresses
UNION ALL
SELECT 'orders', COUNT(*) FROM orders
UNION ALL
SELECT 'order_items', COUNT(*) FROM order_items
UNION ALL
SELECT 'payments', COUNT(*) FROM payments
UNION ALL
SELECT 'delivery_status', COUNT(*) FROM delivery_status
UNION ALL
SELECT 'token_store', COUNT(*) FROM token_store
UNION ALL
SELECT 'audit_logs', COUNT(*) FROM audit_logs
ORDER BY table_name;
```

**Expected Counts (after V3 migration):**
- roles: 4
- users: 6
- vendors: 3
- menu_items: 15
- addresses: 1
- orders: 1
- order_items: 2
- payments: 1
- delivery_status: 5

```sql
-- Verify all foreign key relationships
SELECT 
    'users -> roles' AS relationship,
    COUNT(*) AS count
FROM users u
JOIN roles r ON u.role_id = r.id

UNION ALL

SELECT 'vendors -> users', COUNT(*)
FROM vendors v
JOIN users u ON v.user_id = u.id

UNION ALL

SELECT 'menu_items -> vendors', COUNT(*)
FROM menu_items m
JOIN vendors v ON m.vendor_id = v.id

UNION ALL

SELECT 'orders -> customers', COUNT(*)
FROM orders o
JOIN users c ON o.customer_id = c.id

UNION ALL

SELECT 'order_items -> orders', COUNT(*)
FROM order_items oi
JOIN orders o ON oi.order_id = o.id;
```

---

## 12. Sample UUIDs for Testing

Use these UUIDs from the sample data for your queries:

### Roles
```
ADMIN:    11111111-1111-1111-1111-111111111111
VENDOR:   22222222-2222-2222-2222-222222222222
CUSTOMER: 33333333-3333-3333-3333-333333333333
DRIVER:   44444444-4444-4444-4444-444444444444
```

### Users
```
Admin:       aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa
Alice (Customer): bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb
Driver:      cccccccc-cccc-cccc-cccc-cccccccccccc
```

### Vendors
```
Tasty Burger Joint: 10000001-0000-0000-0000-000000000001
Pizza Palace:       10000002-0000-0000-0000-000000000002
Curry House:        10000003-0000-0000-0000-000000000003
```

### Orders
```
Sample Order: 20000001-0000-0000-0000-000000000001
```

---

## Performance Testing Queries

### Check Index Usage
```sql
SELECT 
    schemaname,
    tablename,
    indexname,
    idx_scan AS index_scans,
    idx_tup_read AS tuples_read,
    idx_tup_fetch AS tuples_fetched
FROM pg_stat_user_indexes
WHERE schemaname = 'public'
ORDER BY idx_scan DESC;
```

### Find Slow Queries (requires pg_stat_statements extension)
```sql
-- Enable extension first (as superuser)
-- CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

SELECT 
    calls,
    mean_exec_time,
    max_exec_time,
    LEFT(query, 100) AS query_preview
FROM pg_stat_statements
WHERE query NOT LIKE '%pg_stat_statements%'
ORDER BY mean_exec_time DESC
LIMIT 10;
```

---

## Integration Test Queries

These queries are useful for automated integration tests:

```sql
-- Test 1: Verify user can place order
BEGIN;
    -- Create test order
    -- Verify order_items created
    -- Verify payment created
    -- Verify delivery_status created
ROLLBACK;

-- Test 2: Verify vendor menu retrieval
SELECT COUNT(*) >= 5 AS has_menu_items
FROM menu_items
WHERE vendor_id = '<VENDOR_UUID>';

-- Test 3: Verify order total calculation
SELECT 
    o.total_cents = SUM(oi.price_cents * oi.quantity) AS totals_match
FROM orders o
JOIN order_items oi ON oi.order_id = o.id
WHERE o.id = '<ORDER_UUID>'
GROUP BY o.id, o.total_cents;
```

---

## Cleanup Queries (for testing)

```sql
-- Delete test data (be careful in production!)
DELETE FROM delivery_status WHERE order_id IN (SELECT id FROM orders WHERE customer_id = '<TEST_USER_UUID>');
DELETE FROM payments WHERE order_id IN (SELECT id FROM orders WHERE customer_id = '<TEST_USER_UUID>');
DELETE FROM order_items WHERE order_id IN (SELECT id FROM orders WHERE customer_id = '<TEST_USER_UUID>');
DELETE FROM orders WHERE customer_id = '<TEST_USER_UUID>';

-- Or truncate all tables (removes all data)
TRUNCATE TABLE 
    delivery_status, payments, order_items, orders, 
    menu_items, addresses, vendors, token_store, audit_logs, 
    users, roles 
RESTART IDENTITY CASCADE;
```
