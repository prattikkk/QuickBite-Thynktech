# QuickBite Order Workflow Documentation

## Order State Machine

### Status Flow Diagram

```
PLACED
  ↓ (vendor accepts)
ACCEPTED
  ↓ (vendor starts preparing)
PREPARING
  ↓ (order ready for pickup)
READY
  ↓ (driver assigned by system)
ASSIGNED
  ↓ (driver picks up order)
PICKED_UP
  ↓ (driver on the way)
ENROUTE
  ↓ (driver delivers)
DELIVERED

Any status → CANCELLED (vendor reject, customer cancel, or admin)
```

### Allowed Status Transitions

| Current Status | Allowed Next Status(es) | Who Can Trigger | Notes |
|---------------|------------------------|----------------|-------|
| PLACED | ACCEPTED, CANCELLED | Vendor, Admin | Vendor accepts or rejects order |
| ACCEPTED | PREPARING, CANCELLED | Vendor, Admin | Vendor starts preparation |
| PREPARING | READY, CANCELLED | Vendor, Admin | Order ready for pickup |
| READY | PICKED_UP, CANCELLED | System (auto-assign driver), Driver, Admin | System assigns driver when READY |
| ASSIGNED | PICKED_UP, CANCELLED | Driver, Admin | Driver picks up from vendor |
| PICKED_UP | ENROUTE, CANCELLED | Driver, Admin | Driver starts delivery |
| ENROUTE | DELIVERED, CANCELLED | Driver, Admin | Driver arrives at destination |
| DELIVERED | - | - | Terminal state |
| CANCELLED | - | - | Terminal state |

### Role-Based Permissions

#### CUSTOMER
- Create orders (POST /api/orders)
- View own orders (GET /api/orders, GET /api/orders/{id})
- Cancel orders (limited conditions)

#### VENDOR
- View orders for their restaurant (GET /api/orders?vendorId={id})
- Accept orders (POST /api/orders/{id}/accept)
- Reject orders (POST /api/orders/{id}/reject)
- Update status: PLACED → ACCEPTED → PREPARING → READY
- Cancel orders (with reason)

#### DRIVER
- View assigned orders
- Update status: ASSIGNED → PICKED_UP → ENROUTE → DELIVERED
- Update location during delivery

#### ADMIN
- View all orders
- Update any status
- Override any validation
- Cancel any order

---

## API Endpoints

### 1. Create Order

**Endpoint:** `POST /api/orders`  
**Role:** CUSTOMER  
**Description:** Create a new order from cart items

**Request Body:**
```json
{
  "items": [
    {
      "menuItemId": "uuid",
      "quantity": 2,
      "specialInstructions": "No onions"
    }
  ],
  "addressId": "uuid",
  "scheduledTime": "2026-02-19T18:00:00Z",
  "paymentMethod": "CARD",
  "specialInstructions": "Ring doorbell twice"
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "message": "Order created successfully",
  "data": {
    "id": "uuid",
    "orderNumber": "ORD-1708369200-ABCD1234",
    "customerId": "uuid",
    "customerName": "John Doe",
    "customerPhone": "+91-9876543210",
    "vendorId": "uuid",
    "vendorName": "Pizza Palace",
    "deliveryAddress": {
      "id": "uuid",
      "line1": "123 Main St",
      "city": "Bangalore",
      "state": "KA",
      "postal": "560001",
      "lat": 12.9716,
      "lng": 77.5946
    },
    "items": [
      {
        "id": "uuid",
        "menuItemId": "uuid",
        "menuItemName": "Margherita Pizza",
        "quantity": 2,
        "priceCents": 50000,
        "totalCents": 100000,
        "specialInstructions": "No onions"
      }
    ],
    "subtotalCents": 100000,
    "deliveryFeeCents": 5000,
    "taxCents": 5000,
    "totalCents": 110000,
    "status": "PLACED",
    "paymentStatus": "AUTHORIZED",
    "paymentMethod": "CARD",
    "scheduledTime": "2026-02-19T18:00:00Z",
    "createdAt": "2026-02-19T17:30:00Z",
    "updatedAt": "2026-02-19T17:30:00Z",
    "specialInstructions": "Ring doorbell twice"
  }
}
```

**Business Rules:**
- All items must be from the same vendor
- All items must be available
- Delivery address must belong to customer
- Subtotal = sum(item.priceCents * quantity)
- Tax = 5% of subtotal
- Delivery fee = ₹50 (5000 cents)
- Total = subtotal + tax + delivery fee
- Menu item prices are snapshotted (no price drift)

---

### 2. Get Order by ID

**Endpoint:** `GET /api/orders/{id}`  
**Role:** CUSTOMER, VENDOR, DRIVER, ADMIN  
**Description:** Get order details (with role-based visibility)

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Order retrieved successfully",
  "data": { ... }
}
```

**Visibility Rules:**
- Customer: can view own orders
- Vendor: can view orders for their restaurant
- Driver: can view assigned orders
- Admin: can view all orders

---

### 3. List Orders

**Endpoint:** `GET /api/orders?customerId={uuid}&status={status}&page=0&size=20`  
**Role:** CUSTOMER, VENDOR, DRIVER, ADMIN  
**Description:** List orders with filters and pagination

**Query Parameters:**
- `customerId` (optional): Filter by customer UUID
- `vendorId` (optional): Filter by vendor UUID
- `status` (optional): Filter by status (PLACED, ACCEPTED, etc.)
- `page` (default: 0): Page number
- `size` (default: 20): Page size
- `sortBy` (default: createdAt): Sort field
- `sortDir` (default: DESC): Sort direction (ASC/DESC)

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Orders retrieved successfully",
  "data": {
    "content": [ ... ],
    "totalElements": 100,
    "totalPages": 5,
    "size": 20,
    "number": 0
  }
}
```

**Auto-filtering:**
- Non-admin customers only see their own orders
- Non-admin vendors only see orders for their restaurant

---

### 4. Update Order Status

**Endpoint:** `PATCH /api/orders/{id}/status`  
**Role:** VENDOR, DRIVER, ADMIN  
**Description:** Update order status with validation

**Request Body:**
```json
{
  "status": "PREPARING",
  "note": "Started cooking",
  "locationLat": 12.9716,
  "locationLng": 77.5946
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Order status updated successfully",
  "data": { ... }
}
```

**Side Effects:**
- READY: automatically assigns nearest driver (if available)
- DELIVERED: captures payment, sets deliveredAt timestamp
- CANCELLED: refunds payment (if captured)

---

### 5. Accept Order (Vendor)

**Endpoint:** `POST /api/orders/{id}/accept`  
**Role:** VENDOR  
**Description:** Vendor accepts an order

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Order accepted successfully",
  "data": { ... }
}
```

**Validations:**
- Order must be in PLACED status
- Only the order's vendor can accept
- Creates audit entry in delivery_status table

---

### 6. Reject Order (Vendor)

**Endpoint:** `POST /api/orders/{id}/reject?reason=Out+of+stock`  
**Role:** VENDOR  
**Description:** Vendor rejects an order

**Query Parameters:**
- `reason` (required): Cancellation reason

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Order rejected successfully",
  "data": {
    "status": "CANCELLED",
    "cancellationReason": "Out of stock",
    ...
  }
}
```

**Side Effects:**
- Order status → CANCELLED
- Payment refunded (if authorized)
- Cancellation reason stored
- Customer notified (stub)

---

## Sequence Diagrams

### Happy Path: Order Creation to Delivery

```
Customer              Backend             Vendor              System              Driver
   |                     |                   |                   |                   |
   |-- POST /orders ---->|                   |                   |                   |
   |                     |-- validate items  |                   |                   |
   |                     |-- calculate total |                   |                   |
   |                     |-- create order    |                   |                   |
   |                     |-- create payment  |                   |                   |
   |                     |-- notify -------->|                   |                   |
   |<--- 201 Created ----|                   |                   |                   |
   |                     |                   |                   |                   |
   |                     |<-- accept order --|                   |                   |
   |                     |-- update status   |                   |                   |
   |                     |                   |                   |                   |
   |                     |<-- PREPARING -----|                   |                   |
   |                     |                   |                   |                   |
   |                     |<-- READY ---------|                   |                   |
   |                     |-- assign driver ---------------------->|                   |
   |                     |-- find nearest driver                 |                   |
   |                     |-- notify driver ----------------------------------------->|
   |                     |-- update status: ASSIGNED             |                   |
   |                     |                   |                   |                   |
   |                     |<-- PICKED_UP ---------------------------------------------|
   |                     |                   |                   |                   |
   |                     |<-- ENROUTE ------------------------------------------------|
   |                     |-- track location  |                   |                   |
   |                     |                   |                   |                   |
   |                     |<-- DELIVERED ----------------------------------------------|
   |                     |-- capture payment |                   |                   |
   |                     |-- notify customer |                   |                   |
   |<--- notification ---|                   |                   |                   |
```

### Vendor Rejects Order

```
Customer              Backend             Vendor
   |                     |                   |
   |-- POST /orders ---->|                   |
   |<--- 201 Created ----|                   |
   |                     |                   |
   |                     |<-- reject --------|
   |                     |-- update status   |
   |                     |-- refund payment  |
   |                     |-- notify customer |
   |<--- notification ---|                   |
```

---

## Data Flow

### Order Creation

1. **Validate Customer**: Check customer exists and is active
2. **Validate Address**: Check address exists and belongs to customer
3. **Validate Menu Items**:
   - All items exist
   - All items are available
   - All items from same vendor
4. **Calculate Pricing**:
   - Subtotal = Σ(item.priceCents × quantity)
   - Tax = subtotal × 0.05
   - Total = subtotal + tax + delivery_fee
5. **Create Order**: Save Order + OrderItems with price snapshots
6. **Create Payment**: Generate payment intent with provider
7. **Authorize Payment**: For CARD/UPI, authorize immediately
8. **Audit Trail**: Create initial DeliveryStatus entry
9. **Notify Vendor**: Send notification (async stub)

### Driver Assignment

**Trigger:** Order status transitions to READY

**Algorithm:**
1. Extract delivery coordinates from order.deliveryAddress
2. Query active drivers with location data
3. Calculate distance using Haversine formula:
   ```sql
   SELECT user_id,
          (6371 * acos(
              cos(radians(delivery_lat)) * cos(radians(driver_lat)) *
              cos(radians(driver_lng) - radians(delivery_lng)) +
              sin(radians(delivery_lat)) * sin(radians(driver_lat))
          )) AS distance_km
   FROM driver_locations
   WHERE active = true
   HAVING distance_km <= 10
   ORDER BY distance_km
   LIMIT 1
   ```
4. Assign driver to order
5. Update status to ASSIGNED
6. Notify driver (stub)

**Fallback:** If no drivers available, order remains in READY status and retries later

---

## Payment Flow

### Payment States

| Order Status | Payment Action | Payment Status |
|-------------|----------------|----------------|
| PLACED | Create intent | PENDING |
| PLACED (CARD/UPI) | Authorize | AUTHORIZED |
| PLACED (COD) | No action | PENDING |
| DELIVERED | Capture | CAPTURED |
| DELIVERED (COD) | Mark captured | CAPTURED |
| CANCELLED | Refund | REFUNDED |

### Payment Methods

**CARD:** Authorize on order creation, capture on delivery  
**UPI:** Authorize on order creation, capture on delivery  
**CASH_ON_DELIVERY:** No authorization, capture on delivery

---

## Audit Trail

Every status change is recorded in the `delivery_status` table:

```sql
CREATE TABLE delivery_status (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders(id),
    status VARCHAR(20) NOT NULL,
    changed_by_user_id UUID NOT NULL,
    note TEXT,
    location_lat DECIMAL(10,8),
    location_lng DECIMAL(11,8),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

**Example entries for one order:**

| Status | Changed By | Note | Timestamp |
|--------|-----------|------|-----------|
| PLACED | customer-uuid | Order placed | 2026-02-19 17:30:00 |
| ACCEPTED | vendor-uuid | Order accepted by vendor | 2026-02-19 17:32:00 |
| PREPARING | vendor-uuid | Started cooking | 2026-02-19 17:35:00 |
| READY | vendor-uuid | Order ready for pickup | 2026-02-19 17:50:00 |
| ASSIGNED | system-uuid | Driver assigned | 2026-02-19 17:51:00 |
| PICKED_UP | driver-uuid | Picked up from restaurant | 2026-02-19 17:55:00 |
| ENROUTE | driver-uuid | On the way to customer | 2026-02-19 17:56:00 |
| DELIVERED | driver-uuid | Delivered successfully | 2026-02-19 18:10:00 |

---

## Error Handling

### Business Exceptions

| Error | HTTP Status | Message |
|-------|-------------|---------|
| Order not found | 404 | "Order not found: {id}" |
| Invalid status transition | 400 | "Invalid status transition: {from} -> {to}" |
| Menu item unavailable | 400 | "Menu item not available: {name}" |
| Mixed vendor items | 400 | "All items must be from the same vendor" |
| Address not found | 400 | "Delivery address not found: {id}" |
| Unauthorized access | 403 | "Access denied to order: {id}" |
| Vendor mismatch | 403 | "Vendor cannot accept order from another vendor" |

### Example Error Response

```json
{
  "success": false,
  "message": "Invalid status transition: PLACED -> DELIVERED",
  "timestamp": "2026-02-19T17:30:00Z"
}
```

---

## Testing

### Unit Tests Coverage

**OrderServiceTest.java** includes:

✅ `createOrder_happyPath_success()`  
✅ `createOrder_menuItemNotAvailable_throwsException()`  
✅ `createOrder_addressNotBelongToCustomer_throwsException()`  
✅ `acceptOrder_happyPath_success()`  
✅ `rejectOrder_happyPath_success()`  
✅ `rejectOrder_wrongVendor_throwsException()`  
✅ `updateOrderStatus_invalidTransition_throwsException()`  
✅ `updateOrderStatus_toReady_assignsDriver()`  
✅ `updateOrderStatus_toDelivered_capturesPayment()`  
✅ `getOrder_notFound_throwsException()`  
✅ `listOrders_withFilters_success()`

### Manual Testing with cURL

**1. Create Order:**
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer {customer_token}" \
  -H "Content-Type: application/json" \
  -d '{
    "items": [{"menuItemId": "uuid", "quantity": 2}],
    "addressId": "address-uuid",
    "paymentMethod": "CARD"
  }'
```

**2. Vendor Accepts:**
```bash
curl -X POST http://localhost:8080/api/orders/{order-id}/accept \
  -H "Authorization: Bearer {vendor_token}"
```

**3. Update to PREPARING:**
```bash
curl -X PATCH http://localhost:8080/api/orders/{order-id}/status \
  -H "Authorization: Bearer {vendor_token}" \
  -H "Content-Type: application/json" \
  -d '{"status": "PREPARING", "note": "Started cooking"}'
```

**4. List Orders:**
```bash
curl -X GET "http://localhost:8080/api/orders?customerId={uuid}&status=PLACED&page=0&size=10" \
  -H "Authorization: Bearer {token}"
```

---

## Future Enhancements

### Day 5+
- [ ] Real payment provider integration (Razorpay)
- [ ] Email/SMS notifications
- [ ] Real-time order tracking (WebSocket)
- [ ] Advanced driver assignment (load balancing, preferences)
- [ ] Order rating and feedback
- [ ] Order history and analytics
- [ ] Scheduled orders (future delivery time)
- [ ] Order modification (before PREPARING)
- [ ] Promo codes and discounts
- [ ] Loyalty points

### Performance Optimizations
- [ ] Cache frequently-accessed menu items
- [ ] Batch driver location updates
- [ ] Async event-driven notifications
- [ ] Database indexes on order lookups
- [ ] Read replicas for order history

---

## Summary

The QuickBite order system implements a robust state machine with:

✅ **Role-based authorization** - customers, vendors, drivers, admins  
✅ **Validated transitions** - prevents invalid status changes  
✅ **Audit trail** - every change logged in delivery_status  
✅ **Payment integration** - authorize, capture, refund flows  
✅ **Driver assignment** - nearest-driver matching  
✅ **Price snapshots** - no price drift after order creation  
✅ **Comprehensive tests** - 11 unit tests covering key scenarios  

The system is ready for integration with frontend and further enhancements in subsequent days.
