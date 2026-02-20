# QuickBite Database Schema Documentation

## Overview

QuickBite uses PostgreSQL with UUID primary keys for all entities. The schema is designed for a food delivery marketplace connecting customers, vendors (restaurants), and drivers.

## Database Configuration

- **Database**: PostgreSQL 13+
- **Primary Keys**: UUID (using `gen_random_uuid()`)
- **Timestamps**: `timestamptz` (time zone aware)
- **Extensions**: `uuid-ossp`, `pgcrypto`
- **Migrations**: Flyway

## Schema Diagram

```
┌─────────┐         ┌─────────┐         ┌─────────────┐
│  roles  │────┬───>│  users  │<────┬───│  addresses  │
└─────────┘    │    └─────────┘     │   └─────────────┘
               │         │           │
               │         │           │
               │         ▼           │
               │    ┌─────────┐     │
               └───>│ vendors │     │
                    └─────────┘     │
                         │          │
                         ▼          │
                   ┌────────────┐   │
                   │ menu_items │   │
                   └────────────┘   │
                         │          │
                         │          │
           ┌─────────────┴──────────┴──────────┐
           │                                    │
           ▼                                    ▼
      ┌─────────┐                         ┌───────────────┐
      │ orders  │<────────────────────────│ order_items   │
      └─────────┘                         └───────────────┘
           │
           ├──────> ┌──────────┐
           │        │ payments │
           │        └──────────┘
           │
           └──────> ┌──────────────────┐
                    │ delivery_status  │
                    └──────────────────┘
```

## Core Tables

### 1. roles

Defines user roles in the system.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | uuid | PK, DEFAULT gen_random_uuid() | Unique identifier |
| name | varchar(50) | NOT NULL, UNIQUE | Role name (ADMIN, VENDOR, CUSTOMER, DRIVER) |
| description | text | | Role description |
| created_at | timestamptz | DEFAULT NOW() | Creation timestamp |

**Indexes:**
- Primary key on `id`
- Unique index on `name`

**Sample Data:**
- ADMIN: System administrator
- VENDOR: Restaurant/vendor owner
- CUSTOMER: End customer
- DRIVER: Delivery driver

---

### 2. users

All system users including customers, vendors, drivers, and admins.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | uuid | PK, DEFAULT gen_random_uuid() | Unique identifier |
| email | varchar(255) | NOT NULL, UNIQUE | User email (login) |
| password_hash | varchar(255) | | BCrypt hashed password |
| name | varchar(255) | | Full name |
| phone | varchar(20) | | Contact number |
| role_id | uuid | NOT NULL, FK → roles(id) | User's role |
| active | boolean | DEFAULT TRUE | Account active status |
| created_at | timestamptz | DEFAULT NOW() | Creation timestamp |
| updated_at | timestamptz | DEFAULT NOW() | Last update timestamp |

**Indexes:**
- `idx_user_email` on `email`
- `idx_user_role` on `role_id`
- `idx_user_active` on `active`

**Foreign Keys:**
- `role_id` → `roles(id)` ON DELETE RESTRICT

---

### 3. vendors

Vendor/restaurant profiles linked to user accounts.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | uuid | PK, DEFAULT gen_random_uuid() | Unique identifier |
| user_id | uuid | NOT NULL, FK → users(id) | Owner user account |
| name | varchar(255) | NOT NULL | Vendor/restaurant name |
| description | text | | About the vendor |
| address | text | | Full address |
| lat | numeric(10,7) | | Latitude |
| lng | numeric(10,7) | | Longitude |
| open_hours | jsonb | | Opening hours by day |
| rating | numeric(3,2) | DEFAULT 0.0 | Average rating (0-5) |
| active | boolean | DEFAULT TRUE | Vendor active status |
| created_at | timestamptz | DEFAULT NOW() | Creation timestamp |
| updated_at | timestamptz | DEFAULT NOW() | Last update timestamp |

**Indexes:**
- `idx_vendor_name` on `name`
- `idx_vendor_user` on `user_id`
- `idx_vendor_active` on `active`
- `idx_vendor_location` on `(lat, lng)`

**Foreign Keys:**
- `user_id` → `users(id)` ON DELETE CASCADE

**JSON Structure (open_hours):**
```json
{
  "monday": "10:00-22:00",
  "tuesday": "10:00-22:00",
  "wednesday": "closed",
  "thursday": "10:00-22:00",
  "friday": "10:00-23:00",
  "saturday": "10:00-23:00",
  "sunday": "11:00-22:00"
}
```

---

### 4. menu_items

Food items offered by vendors.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | uuid | PK, DEFAULT gen_random_uuid() | Unique identifier |
| vendor_id | uuid | NOT NULL, FK → vendors(id) | Vendor offering this item |
| name | varchar(255) | NOT NULL | Item name |
| description | text | | Item description |
| price_cents | bigint | NOT NULL, CHECK >= 0 | Price in cents/paise |
| available | boolean | DEFAULT TRUE | Currently available |
| prep_time_mins | integer | DEFAULT 15 | Preparation time |
| category | varchar(100) | | Item category (Mains, Sides, etc.) |
| image_url | text | | Image URL |
| created_at | timestamptz | DEFAULT NOW() | Creation timestamp |
| updated_at | timestamptz | DEFAULT NOW() | Last update timestamp |

**Indexes:**
- `idx_menuitem_vendor` on `vendor_id`
- `idx_menuitem_available` on `available`
- `idx_menuitem_category` on `category`

**Foreign Keys:**
- `vendor_id` → `vendors(id)` ON DELETE CASCADE

**Note:** Prices stored in cents to avoid floating-point precision issues. ₹299.00 = 29900 cents.

---

### 5. addresses

User delivery addresses.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | uuid | PK, DEFAULT gen_random_uuid() | Unique identifier |
| user_id | uuid | NOT NULL, FK → users(id) | Address owner |
| line1 | varchar(255) | NOT NULL | Address line 1 |
| line2 | varchar(255) | | Address line 2 |
| city | varchar(100) | NOT NULL | City |
| state | varchar(100) | | State/province |
| postal | varchar(20) | | Postal/ZIP code |
| country | varchar(100) | DEFAULT 'India' | Country |
| lat | numeric(10,7) | | Latitude |
| lng | numeric(10,7) | | Longitude |
| is_default | boolean | DEFAULT FALSE | Default address flag |
| created_at | timestamptz | DEFAULT NOW() | Creation timestamp |
| updated_at | timestamptz | DEFAULT NOW() | Last update timestamp |

**Indexes:**
- `idx_address_user` on `user_id`
- `idx_address_default` on `(user_id, is_default)` WHERE is_default = TRUE

**Foreign Keys:**
- `user_id` → `users(id)` ON DELETE CASCADE

---

### 6. orders

Customer orders with status tracking.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | uuid | PK, DEFAULT gen_random_uuid() | Unique identifier |
| customer_id | uuid | NOT NULL, FK → users(id) | Customer placing order |
| vendor_id | uuid | NOT NULL, FK → vendors(id) | Vendor fulfilling order |
| driver_id | uuid | FK → users(id) | Assigned driver (nullable) |
| delivery_address_id | uuid | FK → addresses(id) | Delivery address |
| total_cents | bigint | NOT NULL, CHECK >= 0 | Order total in cents |
| status | varchar(50) | NOT NULL, DEFAULT 'PENDING' | Order status |
| scheduled_time | timestamptz | | Scheduled delivery time |
| delivered_at | timestamptz | | Actual delivery time |
| cancelled_at | timestamptz | | Cancellation time |
| cancellation_reason | text | | Why order was cancelled |
| special_instructions | text | | Customer instructions |
| created_at | timestamptz | DEFAULT NOW() | Creation timestamp |
| updated_at | timestamptz | DEFAULT NOW() | Last update timestamp |

**Indexes:**
- `idx_order_customer` on `customer_id`
- `idx_order_vendor` on `vendor_id`
- `idx_order_driver` on `driver_id`
- `idx_order_status` on `status`
- `idx_order_created` on `created_at`
- `idx_order_customer_status` on `(customer_id, status)`

**Foreign Keys:**
- `customer_id` → `users(id)` ON DELETE RESTRICT
- `vendor_id` → `vendors(id)` ON DELETE RESTRICT
- `driver_id` → `users(id)` ON DELETE SET NULL
- `delivery_address_id` → `addresses(id)` ON DELETE SET NULL

**Order Status Lifecycle:**
```
PENDING → CONFIRMED → PREPARING → READY → PICKED_UP → ON_THE_WAY → DELIVERED → COMPLETED
                                     ↓
                                 CANCELLED
```

---

### 7. order_items

Line items within an order (order details).

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | uuid | PK, DEFAULT gen_random_uuid() | Unique identifier |
| order_id | uuid | NOT NULL, FK → orders(id) | Parent order |
| menu_item_id | uuid | NOT NULL, FK → menu_items(id) | Menu item ordered |
| quantity | integer | NOT NULL, CHECK > 0 | Quantity ordered |
| price_cents | bigint | NOT NULL, CHECK >= 0 | Price snapshot in cents |
| special_instructions | text | | Item-specific instructions |
| created_at | timestamptz | DEFAULT NOW() | Creation timestamp |

**Indexes:**
- `idx_orderitem_order` on `order_id`
- `idx_orderitem_menuitem` on `menu_item_id`

**Foreign Keys:**
- `order_id` → `orders(id)` ON DELETE CASCADE
- `menu_item_id` → `menu_items(id)` ON DELETE RESTRICT

**Note:** `price_cents` captures the price at order time (historical snapshot).

---

### 8. payments

Payment transactions for orders.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | uuid | PK, DEFAULT gen_random_uuid() | Unique identifier |
| order_id | uuid | NOT NULL, FK → orders(id) | Related order |
| amount_cents | bigint | NOT NULL, CHECK >= 0 | Payment amount in cents |
| currency | varchar(10) | DEFAULT 'INR' | Currency code |
| provider | varchar(50) | | Payment provider (RAZORPAY, etc.) |
| provider_payment_id | varchar(255) | | Provider's transaction ID |
| status | varchar(50) | NOT NULL, DEFAULT 'PENDING' | Payment status |
| paid_at | timestamptz | | Payment success timestamp |
| failed_at | timestamptz | | Payment failure timestamp |
| failure_reason | text | | Failure details |
| created_at | timestamptz | DEFAULT NOW() | Creation timestamp |
| updated_at | timestamptz | DEFAULT NOW() | Last update timestamp |

**Indexes:**
- `idx_payment_order` on `order_id`
- `idx_payment_status` on `status`
- `idx_payment_provider_id` on `provider_payment_id`

**Foreign Keys:**
- `order_id` → `orders(id)` ON DELETE CASCADE

**Payment Status:**
- PENDING: Initiated
- PROCESSING: In progress
- SUCCESS: Completed
- FAILED: Failed
- REFUNDED: Refunded to customer
- CANCELLED: Cancelled

---

### 9. delivery_status

Order status change audit trail with location tracking.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | uuid | PK, DEFAULT gen_random_uuid() | Unique identifier |
| order_id | uuid | NOT NULL, FK → orders(id) | Related order |
| status | varchar(50) | NOT NULL | Status at this point |
| changed_by | uuid | FK → users(id) | User who made change |
| changed_at | timestamptz | DEFAULT NOW() | Change timestamp |
| location_lat | numeric(10,7) | | Driver location latitude |
| location_lng | numeric(10,7) | | Driver location longitude |
| note | text | | Additional notes |

**Indexes:**
- `idx_deliverystatus_order` on `order_id`
- `idx_deliverystatus_changed_at` on `changed_at`

**Foreign Keys:**
- `order_id` → `orders(id)` ON DELETE CASCADE
- `changed_by` → `users(id)` ON DELETE SET NULL

**Purpose:** Provides complete audit trail of order lifecycle with timestamps and optional location data for driver tracking.

---

### 10. token_store

JWT refresh tokens for authentication.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | uuid | PK, DEFAULT gen_random_uuid() | Unique identifier |
| user_id | uuid | NOT NULL, FK → users(id) | Token owner |
| token_hash | varchar(255) | NOT NULL | Hashed token value |
| token_type | varchar(50) | DEFAULT 'REFRESH' | Token type |
| issued_at | timestamptz | DEFAULT NOW() | Issuance timestamp |
| expires_at | timestamptz | NOT NULL | Expiration timestamp |
| revoked | boolean | DEFAULT FALSE | Revocation status |
| revoked_at | timestamptz | | Revocation timestamp |
| device_info | text | | Device/browser info |
| ip_address | varchar(45) | | IP address at issuance |

**Indexes:**
- `idx_token_user` on `user_id`
- `idx_token_hash` on `token_hash`
- `idx_token_expires` on `expires_at`
- `idx_token_revoked` on `revoked` WHERE revoked = FALSE

**Foreign Keys:**
- `user_id` → `users(id)` ON DELETE CASCADE

**Purpose:** Enables token revocation and session management. Supports "logout all devices" functionality.

---

### 11. audit_logs

System-wide audit trail for security and compliance.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | uuid | PK, DEFAULT gen_random_uuid() | Unique identifier |
| user_id | uuid | FK → users(id) | User who performed action |
| action | varchar(100) | NOT NULL | Action type |
| entity | varchar(100) | | Entity type affected |
| entity_id | uuid | | Entity ID affected |
| old_values | jsonb | | Previous state (for updates) |
| new_values | jsonb | | New state (for updates) |
| meta | jsonb | | Additional metadata |
| ip_address | varchar(45) | | Request IP address |
| user_agent | text | | Browser/device info |
| created_at | timestamptz | DEFAULT NOW() | Action timestamp |

**Indexes:**
- `idx_auditlog_user` on `user_id`
- `idx_auditlog_entity` on `(entity, entity_id)`
- `idx_auditlog_created` on `created_at`

**Foreign Keys:**
- `user_id` → `users(id)` ON DELETE SET NULL

**Common Actions:**
- CREATE, UPDATE, DELETE
- LOGIN, LOGOUT, LOGIN_FAILED
- PASSWORD_CHANGE
- ORDER_PLACED, ORDER_CANCELLED
- PAYMENT_PROCESSED

---

## Migration Strategy

### Running Migrations

```bash
# Start database
docker-compose up -d postgres

# Run Spring Boot (automatic Flyway migration)
mvn spring-boot:run

# Or manually with Flyway CLI
flyway migrate -url=jdbc:postgresql://localhost:5432/quickbite -user=dbuser -password=dbpass
```

### Migration Files

1. **V1__init_schema.sql**: Initial schema (Day 1)
2. **V2__create_core_tables.sql**: Core tables with UUID PKs (Day 2)
3. **V3__sample_data.sql**: Sample test data (Day 2)

### Rollback Strategy

Flyway doesn't support automatic rollback. For rollback:
1. Restore from database backup
2. Or write compensating migrations (V4__rollback_v3.sql)

---

## Performance Considerations

### Indexing Strategy

- **Email lookups**: Indexed on `users.email`
- **Order queries**: Composite index on `(customer_id, status)`
- **Location searches**: Indexes on `(lat, lng)` for vendors
- **Audit queries**: Indexed on `created_at` with DESC for recent-first queries

### Query Optimization

- Use pagination for all list queries
- Lazy loading for entity relationships (LAZY fetch type)
- Projection queries when full entities not needed
- Connection pooling via HikariCP (default in Spring Boot)

### Partitioning (Future)

Consider partitioning for large tables:
- `orders`: Partition by `created_at` (monthly/quarterly)
- `audit_logs`: Partition by `created_at` with retention policy
- `delivery_status`: Partition by `changed_at`

---

## Security

### Data Protection

- **Passwords**: BCrypt hashed (never stored plaintext)
- **Tokens**: SHA-256 hashed before storage
- **PII**: Consider encryption at rest for sensitive fields
- **Audit**: All data changes logged in `audit_logs`

### Access Control

- Row-level security considerations (future)
- Application-level authorization via Spring Security
- API rate limiting per user/IP

---

## Maintenance

### Regular Tasks

1. **Cleanup expired tokens**: Delete from `token_store` where `expires_at < NOW()`
2. **Archive old audit logs**: Move logs older than 1 year to cold storage
3. **Vacuum and analyze**: Regular PostgreSQL maintenance
4. **Update statistics**: For query planner optimization

### Monitoring

- Table sizes and growth rates
- Slow query log analysis
- Index usage statistics
- Connection pool metrics

---

## Future Enhancements

1. **Full-text search**: Add `tsvector` columns for menu item search
2. **PostGIS**: Proper geospatial queries with distance calculations
3. **Read replicas**: For scaling read operations
4. **Caching**: Redis for frequently accessed data
5. **CDC**: Change data capture for real-time analytics
