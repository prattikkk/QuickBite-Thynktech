# QuickBite - Functional Requirements (MVP)

## FR-1: User Registration & Authentication

**Description**: Users can register with email/password and authenticate to receive JWT tokens.

**Endpoints**:
- `POST /api/auth/register` - Create new account
- `POST /api/auth/login` - Authenticate user
- `POST /api/auth/refresh` - Refresh access token

**Success Criteria**:
- Email uniqueness enforced
- Passwords hashed with BCrypt (strength ≥10)
- JWT access token valid for 15 minutes
- Refresh token valid for 7 days, stored in httpOnly cookie
- Role assignment during registration (CUSTOMER default)

---

## FR-2: User Profile Management

**Description**: Users can view and update their profile, manage delivery addresses.

**Endpoints**:
- `GET /api/users/me` - Get current user
- `PUT /api/users/me` - Update profile
- `POST /api/users/addresses` - Add address
- `GET /api/users/addresses` - List addresses
- `DELETE /api/users/addresses/{id}` - Remove address

**Success Criteria**:
- Users can store multiple addresses
- Address validation (required: line1, city, postal code)
- Geocoding to lat/lng for distance calculation

---

## FR-3: Vendor Registration & Profile

**Description**: Vendors can register their business, set hours, upload logo.

**Endpoints**:
- `POST /api/vendors` - Register vendor (requires VENDOR role)
- `GET /api/vendors/{id}` - Get vendor details
- `PUT /api/vendors/{id}` - Update vendor (owner only)
- `PUT /api/vendors/{id}/hours` - Set business hours

**Success Criteria**:
- Vendor linked to user account
- Business location stored as lat/lng
- Open/close hours in ISO-8601 format
- Logo upload supported (max 2MB)

---

## FR-4: Menu Management

**Description**: Vendors can create, update, and manage menu items with prices and availability.

**Endpoints**:
- `GET /api/vendors/{id}/menu` - Public menu view
- `POST /api/vendors/{id}/menu` - Add item (vendor only)
- `PUT /api/menu/{itemId}` - Update item
- `DELETE /api/menu/{itemId}` - Soft delete item
- `PATCH /api/menu/{itemId}/availability` - Toggle availability

**Success Criteria**:
- Prices stored in cents (integer)
- Items can be marked unavailable without deletion
- Support for item description, image URL, dietary tags
- Batch updates for availability

---

## FR-5: Vendor Search & Discovery

**Description**: Customers can search vendors by location, cuisine, rating, or name.

**Endpoints**:
- `GET /api/vendors?lat={lat}&lng={lng}&radius={km}` - Location-based search
- `GET /api/vendors?q={query}` - Text search
- `GET /api/vendors?cuisine={type}` - Filter by cuisine

**Success Criteria**:
- Distance calculation using PostGIS or Haversine formula
- Results sorted by distance, rating, or delivery time
- Pagination (default 20 per page)
- Open/closed status computed from business hours

---

## FR-6: Shopping Cart (Client-Side)

**Description**: Customers can add menu items to cart before checkout.

**Implementation**: Client-side state management (React Context/Redux)

**Success Criteria**:
- Cart persisted in localStorage
- Quantity adjustment (min 1, max 99)
- Real-time total calculation
- Validate menu items still available before order submission

---

## FR-7: Order Creation

**Description**: Customer submits cart to create an order with payment.

**Endpoints**:
- `POST /api/orders` - Create order

**Request Body**:
```json
{
  "vendorId": "uuid",
  "deliveryAddressId": "uuid",
  "items": [
    { "menuItemId": "uuid", "quantity": 2 }
  ],
  "paymentMethodId": "pm_stripe_token"
}
```

**Success Criteria**:
- Order validation: items exist, vendor open, prices current
- Total calculated server-side (prevent tampering)
- Payment intent created with Stripe
- Order status initialized to PLACED
- Atomic transaction (order + payment record)

---

## FR-8: Payment Integration

**Description**: Process payments via Stripe PaymentIntents.

**Endpoints**:
- `POST /api/payments/intent` - Create PaymentIntent
- `POST /api/payments/confirm` - Confirm payment

**Success Criteria**:
- Idempotent payment creation
- Support for 3D Secure (SCA compliance)
- Webhook handling for `payment_intent.succeeded`
- Failed payments prevent order progression
- Refund support for cancellations

---

## FR-9: Order Lifecycle Management

**Description**: Vendors and drivers update order status as it progresses.

**Endpoints**:
- `GET /api/orders/{id}` - Get order details
- `PATCH /api/orders/{id}/status` - Update status
- `POST /api/orders/{id}/cancel` - Cancel order

**Success Criteria**:
- State machine validation (PLACED → ACCEPTED → PREPARING → READY → ENROUTE → DELIVERED)
- Only authorized users can update (vendor for ACCEPTED, driver for ENROUTE)
- Status history logged in `delivery_status` table
- Notifications sent on each transition

---

## FR-10: Driver Assignment

**Description**: Drivers can view available orders and accept delivery jobs.

**Endpoints**:
- `GET /api/drivers/available-orders` - List orders READY for pickup
- `POST /api/drivers/accept/{orderId}` - Accept delivery
- `PUT /api/drivers/location` - Update GPS coordinates

**Success Criteria**:
- Orders filtered by driver's current location (radius)
- First-come-first-served assignment (optimistic locking)
- Driver can only have one active delivery
- Real-time location tracking stored in Redis

---

## FR-11: Real-Time Notifications

**Description**: Users receive push notifications for order updates.

**Channels**:
- WebSocket (real-time dashboard)
- Email (order confirmation, delivery)
- SMS (optional, via Twilio)

**Success Criteria**:
- Customer notified on: order accepted, out for delivery, delivered
- Vendor notified on: new order placed
- Driver notified on: new orders available nearby
- Notification preferences stored per user

---

## FR-12: Admin Dashboard

**Description**: Admins can view analytics, moderate vendors, manage users.

**Endpoints**:
- `GET /api/admin/metrics` - Order volume, revenue, active users
- `GET /api/admin/vendors` - List all vendors (pending approval)
- `PUT /api/admin/vendors/{id}/approve` - Approve vendor
- `PUT /api/admin/users/{id}/ban` - Ban user

**Success Criteria**:
- Role-based access control (ADMIN role required)
- Audit log for admin actions
- Exports for reporting (CSV/PDF)

---

## FR-13: File Uploads

**Description**: Support for vendor logos, menu images, delivery proof photos.

**Endpoints**:
- `POST /api/uploads` - Upload file to S3/MinIO
- `DELETE /api/uploads/{key}` - Delete file

**Success Criteria**:
- File size limit: 5MB
- Allowed types: jpg, png, pdf
- Pre-signed URLs for direct S3 upload
- File keys stored in database entities

---

## FR-14: Search & Filtering

**Description**: Advanced search for menu items across vendors.

**Endpoints**:
- `GET /api/search?q={query}&filters={json}` - Elasticsearch/PostgreSQL full-text

**Success Criteria**:
- Search indexed on menu item name, description, vendor name
- Filters: price range, dietary (vegan, gluten-free), rating
- Results ranked by relevance + distance

---

## Summary

**Total Functional Requirements**: 14  
**High Priority (MVP)**: FR-1 through FR-10  
**Medium Priority**: FR-11, FR-13  
**Low Priority (Post-MVP)**: FR-12, FR-14  

**Estimated API Endpoints**: ~35  
**Estimated Database Tables**: ~12
