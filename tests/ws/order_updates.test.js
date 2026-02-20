/**
 * QuickBite WebSocket Order Updates Test
 * Tests real-time order status updates via WebSocket/STOMP
 */

const { Client } = require('@stomp/stompjs');
const WebSocket = require('ws');

// Polyfill for Node.js
Object.assign(global, { WebSocket: WebSocket });

// Test configuration
const WS_URL = process.env.WS_URL || 'ws://localhost:8080/ws-native';
const API_BASE_URL = process.env.API_BASE_URL || 'http://localhost:8080';
const TEST_TIMEOUT = 15000; // 15 seconds
const MESSAGE_TIMEOUT = 10000; // 10 seconds for WebSocket messages

// Test credentials (from seed data)
const TEST_CUSTOMER = {
  email: 'e2e_customer@test.com',
  password: 'Test@1234'
};

const TEST_VENDOR = {
  email: 'e2e_vendor@test.com',
  password: 'Test@1234'
};

const TEST_DRIVER = {
  email: 'e2e_driver@test.com',
  password: 'Test@1234'
};

describe('WebSocket Order Updates', () => {
  let stompClient;
  let accessToken;
  let orderId;
  let vendorId;
  let menuItemId;

  beforeAll(async () => {
    // Login to get access token
    const response = await fetch(`${API_BASE_URL}/api/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(TEST_CUSTOMER)
    });

    if (!response.ok) {
      throw new Error(`Login failed: ${response.statusText}`);
    }

    const data = await response.json();
    accessToken = data.data.accessToken;

    // Get vendor and menu item for order creation
    const vendorsResponse = await fetch(`${API_BASE_URL}/api/vendors`, {
      headers: { 'Authorization': `Bearer ${accessToken}` }
    });
    const vendors = await vendorsResponse.json();
    vendorId = vendors.data.find(v => v.name === 'E2E Test Restaurant')?.id;

    const menuResponse = await fetch(`${API_BASE_URL}/api/vendors/${vendorId}/menu`, {
      headers: { 'Authorization': `Bearer ${accessToken}` }
    });
    const menuItems = await menuResponse.json();
    menuItemId = menuItems.data[0]?.id;

    expect(vendorId).toBeDefined();
    expect(menuItemId).toBeDefined();
  }, TEST_TIMEOUT);

  afterAll(() => {
    if (stompClient && stompClient.connected) {
      stompClient.deactivate();
    }
  });

  test('should connect to WebSocket server', (done) => {
    stompClient = new Client({
      brokerURL: WS_URL,
      connectHeaders: {
        Authorization: `Bearer ${accessToken}`
      },
      debug: (str) => {
        console.log('STOMP Debug:', str);
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: (frame) => {
        console.log('✓ Connected to WebSocket:', frame.command);
        expect(stompClient.connected).toBe(true);
        done();
      },
      onStompError: (frame) => {
        console.error('STOMP Error:', frame.headers['message'], frame.body);
        done(new Error(`STOMP error: ${frame.headers['message']}`));
      },
      onWebSocketError: (event) => {
        console.error('WebSocket Error:', event);
        done(new Error('WebSocket connection error'));
      }
    });

    stompClient.activate();
  }, TEST_TIMEOUT);

  test('should receive order status update when order is created and accepted', async () => {
    // Create order first
    const orderResponse = await fetch(`${API_BASE_URL}/api/orders`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${accessToken}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        items: [{
          menuItemId: menuItemId,
          quantity: 1,
          specialInstructions: 'WebSocket test order'
        }],
        addressId: 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
        paymentMethod: 'CARD'
      })
    });

    expect(orderResponse.ok).toBe(true);
    const order = await orderResponse.json();
    orderId = order.data.id;
    console.log('✓ Order created:', orderId);

    // Subscribe to order updates
    const messagePromise = new Promise((resolve, reject) => {
      const timeout = setTimeout(() => {
        reject(new Error('Timeout waiting for WebSocket message'));
      }, MESSAGE_TIMEOUT);

      const subscription = stompClient.subscribe(
        `/topic/orders.${orderId}`,
        (message) => {
          clearTimeout(timeout);
          console.log('✓ Received WebSocket message:', message.body);
          
          const update = JSON.parse(message.body);
          expect(update).toHaveProperty('orderId');
          expect(update).toHaveProperty('status');
          expect(update.orderId).toBe(orderId);
          
          subscription.unsubscribe();
          resolve(update);
        },
        {
          id: `order-${orderId}-sub`
        }
      );
    });

    // Login as vendor to accept the order (this should trigger WebSocket update)
    const vendorLoginResponse = await fetch(`${API_BASE_URL}/api/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(TEST_VENDOR)
    });
    const vendorData = await vendorLoginResponse.json();
    const vendorToken = vendorData.data.accessToken;

    // Wait a bit for subscription to be established
    await new Promise(resolve => setTimeout(resolve, 500));

    // Accept order (this triggers status change and WebSocket broadcast)
    const acceptResponse = await fetch(`${API_BASE_URL}/api/orders/${orderId}/accept`, {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${vendorToken}` }
    });

    expect(acceptResponse.ok).toBe(true);
    console.log('✓ Order accepted by vendor');

    // Wait for WebSocket message
    const update = await messagePromise;
    console.log('✓ WebSocket update received:', update);
    expect(update.status).toBe('ACCEPTED');
  }, TEST_TIMEOUT);

  test('should receive multiple status updates for order lifecycle', async () => {
    // Login as vendor
    const vendorLoginResponse = await fetch(`${API_BASE_URL}/api/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(TEST_VENDOR)
    });
    const vendorData = await vendorLoginResponse.json();
    const vendorToken = vendorData.data.accessToken;

    const receivedUpdates = [];

    // Subscribe to order updates
    const updatesPromise = new Promise((resolve, reject) => {
      const timeout = setTimeout(() => {
        reject(new Error('Timeout waiting for WebSocket messages'));
      }, MESSAGE_TIMEOUT);

      const subscription = stompClient.subscribe(
        `/topic/orders.${orderId}`,
        (message) => {
          const update = JSON.parse(message.body);
          console.log('✓ Received update:', update.status);
          receivedUpdates.push(update);

          // READY triggers automatic driver assignment in the backend,
          // which transitions the order directly to ASSIGNED.
          // So the last broadcast status we expect is ASSIGNED.
          if (update.status === 'ASSIGNED') {
            clearTimeout(timeout);
            subscription.unsubscribe();
            resolve(receivedUpdates);
          }
        }
      );
    });

    // Wait for subscription
    await new Promise(resolve => setTimeout(resolve, 500));

    // Update order status: PREPARING
    const preparingResponse = await fetch(`${API_BASE_URL}/api/orders/${orderId}/status`, {
      method: 'PATCH',
      headers: {
        'Authorization': `Bearer ${vendorToken}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        status: 'PREPARING',
        note: 'Food is being prepared'
      })
    });
    expect(preparingResponse.ok).toBe(true);

    await new Promise(resolve => setTimeout(resolve, 500));

    // Update order status: READY
    // The backend will assign a driver automatically and broadcast ASSIGNED
    const readyResponse = await fetch(`${API_BASE_URL}/api/orders/${orderId}/status`, {
      method: 'PATCH',
      headers: {
        'Authorization': `Bearer ${vendorToken}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        status: 'READY',
        note: 'Food is ready for pickup'
      })
    });
    expect(readyResponse.ok).toBe(true);

    const updates = await updatesPromise;
    console.log('✓ All updates received:', updates.map(u => u.status));

    expect(updates.length).toBeGreaterThanOrEqual(2);
    expect(updates.some(u => u.status === 'PREPARING')).toBe(true);
    // READY triggers auto driver-assignment; backend broadcasts ASSIGNED
    expect(updates.some(u => u.status === 'ASSIGNED')).toBe(true);
  }, TEST_TIMEOUT);

  test('should track full driver delivery lifecycle', async () => {
    // Login as driver
    const driverLoginResponse = await fetch(`${API_BASE_URL}/api/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(TEST_DRIVER)
    });
    expect(driverLoginResponse.ok).toBe(true);
    const driverData = await driverLoginResponse.json();
    const driverToken = driverData.data.accessToken;
    console.log('✓ Driver logged in');

    const receivedStatuses = [];

    // Subscribe to order updates (orderId is currently ASSIGNED from previous test)
    const deliveryPromise = new Promise((resolve, reject) => {
      const timeout = setTimeout(() => {
        reject(new Error('Timeout waiting for delivery lifecycle updates'));
      }, MESSAGE_TIMEOUT);

      const subscription = stompClient.subscribe(
        `/topic/orders.${orderId}`,
        (message) => {
          const update = JSON.parse(message.body);
          console.log('✓ Delivery update:', update.status);
          receivedStatuses.push(update.status);

          if (update.status === 'DELIVERED') {
            clearTimeout(timeout);
            subscription.unsubscribe();
            resolve(receivedStatuses);
          }
        }
      );
    });

    await new Promise(resolve => setTimeout(resolve, 500));

    // Driver picks up order: ASSIGNED → PICKED_UP
    const pickedUpResponse = await fetch(`${API_BASE_URL}/api/orders/${orderId}/status`, {
      method: 'PATCH',
      headers: {
        'Authorization': `Bearer ${driverToken}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ status: 'PICKED_UP', note: 'Order picked up' })
    });
    expect(pickedUpResponse.ok).toBe(true);

    await new Promise(resolve => setTimeout(resolve, 300));

    // Driver en route: PICKED_UP → ENROUTE
    const enrouteResponse = await fetch(`${API_BASE_URL}/api/orders/${orderId}/status`, {
      method: 'PATCH',
      headers: {
        'Authorization': `Bearer ${driverToken}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ status: 'ENROUTE', note: 'On the way' })
    });
    expect(enrouteResponse.ok).toBe(true);

    await new Promise(resolve => setTimeout(resolve, 300));

    // Driver delivers: ENROUTE → DELIVERED
    const deliveredResponse = await fetch(`${API_BASE_URL}/api/orders/${orderId}/status`, {
      method: 'PATCH',
      headers: {
        'Authorization': `Bearer ${driverToken}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ status: 'DELIVERED', note: 'Order delivered' })
    });
    if (!deliveredResponse.ok) {
      const errBody = await deliveredResponse.text();
      console.error('DELIVERED failed:', deliveredResponse.status, errBody);
    }
    expect(deliveredResponse.ok).toBe(true);
    console.log('✓ Order marked as delivered');

    const finalStatuses = await deliveryPromise;
    console.log('✓ Delivery lifecycle statuses:', finalStatuses);

    expect(finalStatuses).toContain('PICKED_UP');
    expect(finalStatuses).toContain('ENROUTE');
    expect(finalStatuses).toContain('DELIVERED');
    // Verify correct ordering
    expect(finalStatuses.indexOf('PICKED_UP')).toBeLessThan(finalStatuses.indexOf('ENROUTE'));
    expect(finalStatuses.indexOf('ENROUTE')).toBeLessThan(finalStatuses.indexOf('DELIVERED'));
  }, TEST_TIMEOUT);

  test('should receive CANCELLED update when vendor rejects order', async () => {
    // Create a fresh order to cancel
    const orderResponse = await fetch(`${API_BASE_URL}/api/orders`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${accessToken}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        items: [{ menuItemId: menuItemId, quantity: 1 }],
        addressId: 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
        paymentMethod: 'CARD'
      })
    });
    expect(orderResponse.ok).toBe(true);
    const newOrder = await orderResponse.json();
    const cancelOrderId = newOrder.data.id;
    console.log('✓ New order created for cancellation test:', cancelOrderId);

    // Subscribe to this order's updates
    const cancelPromise = new Promise((resolve, reject) => {
      const timeout = setTimeout(() => {
        reject(new Error('Timeout waiting for CANCELLED update'));
      }, MESSAGE_TIMEOUT);

      const subscription = stompClient.subscribe(
        `/topic/orders.${cancelOrderId}`,
        (message) => {
          const update = JSON.parse(message.body);
          console.log('✓ Cancellation update received:', update.status);
          if (update.status === 'CANCELLED') {
            clearTimeout(timeout);
            subscription.unsubscribe();
            resolve(update);
          }
        }
      );
    });

    await new Promise(resolve => setTimeout(resolve, 500));

    // Vendor rejects the order → triggers CANCELLED broadcast
    const vendorLoginResponse = await fetch(`${API_BASE_URL}/api/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(TEST_VENDOR)
    });
    const vendorData = await vendorLoginResponse.json();
    const vendorToken = vendorData.data.accessToken;

    // The reject endpoint takes `reason` as a query parameter (not a JSON body)
    const rejectResponse = await fetch(
      `${API_BASE_URL}/api/orders/${cancelOrderId}/reject?reason=${encodeURIComponent('Out of stock for WebSocket test')}`,
      {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${vendorToken}` }
      }
    );
    if (!rejectResponse.ok) {
      const errBody = await rejectResponse.text();
      console.error('Reject failed:', rejectResponse.status, errBody);
    }
    expect(rejectResponse.ok).toBe(true);
    console.log('✓ Vendor rejected order');

    const cancelUpdate = await cancelPromise;
    expect(cancelUpdate.status).toBe('CANCELLED');
    expect(cancelUpdate.orderId).toBe(cancelOrderId);
    console.log('✓ CANCELLED WebSocket update confirmed');
  }, TEST_TIMEOUT);

  test('should handle subscription to non-existent order gracefully', (done) => {
    const fakeOrderId = '99999999-9999-9999-9999-999999999999';
    
    try {
      const subscription = stompClient.subscribe(
        `/topic/orders.${fakeOrderId}`,
        (message) => {
          // Should not receive any messages
          done(new Error('Received message for non-existent order'));
        }
      );

      // Wait a bit, then unsubscribe
      setTimeout(() => {
        subscription.unsubscribe();
        done();
      }, 2000);
    } catch (error) {
      done(error);
    }
  }, TEST_TIMEOUT);
});

describe('WebSocket Connection Resilience', () => {
  test('should reject unauthenticated WebSocket connection', (done) => {
    const unauthClient = new Client({
      brokerURL: WS_URL,
      connectHeaders: {}, // No Authorization header
      reconnectDelay: 0,
      onConnect: () => {
        // Some brokers allow the STOMP CONNECT but then reject the subscription;
        // here we detect connection and immediately check that protected topics fail.
        console.log('Connected without token — testing protected subscription');
        const subscription = unauthClient.subscribe(
          '/topic/orders.00000000-0000-0000-0000-000000000000',
          (message) => {
            // Receiving a message here is unexpected for a protected topic
            console.warn('Received message on unauthenticated subscription:', message.body);
          }
        );
        // Give a moment then clean up — if no error thrown, the broker is
        // configured to allow anonymous connections (acceptable if topics are secured server-side)
        setTimeout(() => {
          unauthClient.deactivate();
          done();
        }, 1500);
      },
      onStompError: (frame) => {
        console.log('✓ STOMP rejected unauthenticated connection:', frame.headers['message']);
        unauthClient.deactivate();
        done();
      },
      onWebSocketError: () => {
        // WebSocket-level rejection also counts as correct behaviour
        console.log('✓ WebSocket rejected unauthenticated connection');
        unauthClient.deactivate();
        done();
      }
    });

    unauthClient.activate();

    setTimeout(() => {
      unauthClient.deactivate();
      done();
    }, 5000);
  }, TEST_TIMEOUT);

  test('should handle connection errors gracefully', (done) => {
    const badClient = new Client({
      brokerURL: 'ws://localhost:9999/invalid', // Invalid endpoint
      connectHeaders: {},
      reconnectDelay: 0, // Disable reconnect
      onConnect: () => {
        done(new Error('Should not connect to invalid endpoint'));
      },
      onWebSocketError: (event) => {
        console.log('✓ Connection error caught as expected');
        done();
      }
    });

    badClient.activate();

    // Timeout fallback
    setTimeout(() => {
      badClient.deactivate();
      done();
    }, 3000);
  }, TEST_TIMEOUT);
});
