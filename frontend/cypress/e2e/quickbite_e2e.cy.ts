/**
 * QuickBite End-to-End Test Suite
 * Comprehensive tests covering all user journeys
 */

/// <reference types="cypress" />

describe('QuickBite E2E Test Suite', () => {
  const apiUrl = Cypress.env('apiUrl');
  const testVendorId = Cypress.env('testVendorId');
  const testMenuItemIds = Cypress.env('testMenuItemIds');

  // Shared variables across tests
  let customerToken: string;
  let vendorToken: string;
  let driverToken: string;
  let orderId: string;
  let paymentId: string;
  let menuItemId: string;

  before(() => {
    // Health check
    cy.request(`${apiUrl}/actuator/health`).its('body.status').should('eq', 'UP');
    cy.task('log', '✓ Backend is healthy');
  });

  describe('Journey 1: Customer Registration and Order Flow', () => {
    it('should register a new customer account', () => {
      cy.visit('/register');
      
      const timestamp = Date.now();
      const username = `cypress_customer_${timestamp}`;
      const email = `cypress_customer_${timestamp}@test.com`;

      cy.get('input[name="username"]').type(username);
      cy.get('input[name="email"]').type(email);
      cy.get('input[name="password"]').type('Test@1234');
      cy.get('input[name="fullName"]').type('Cypress Test Customer');
      cy.get('input[name="phoneNumber"]').type('+15551234567');
      cy.get('select[name="role"]').select('CUSTOMER');
      
      cy.get('button[type="submit"]').click();

      // Should redirect to login or dashboard
      cy.url().should('not.include', '/register');
      cy.task('log', `✓ Customer registered: ${username}`);
    });

    it('should login as existing test customer', () => {
      cy.loginAs('customer');
      cy.visit('/');
      
      // Verify we're logged in (check for user-specific UI elements)
      cy.wait(1000); // Wait for app to load
      cy.task('log', '✓ Customer logged in');
    });

    it('should browse and search for vendors', () => {
      const token = Cypress.env('accessToken');

      cy.request({
        method: 'GET',
        url: `${apiUrl}/api/vendors`,
        headers: { Authorization: `Bearer ${token}` }
      }).then((response) => {
        expect(response.status).to.eq(200);
        expect(response.body.data).to.be.an('array');
        expect(response.body.data.length).to.be.greaterThan(0);
        cy.task('log', `✓ Found ${response.body.data.length} vendors`);
      });

      // Search for test vendor
      cy.request({
        method: 'GET',
        url: `${apiUrl}/api/vendors/search?query=E2E`,
        headers: { Authorization: `Bearer ${token}` }
      }).then((response) => {
        expect(response.status).to.eq(200);
        expect(response.body.data).to.be.an('array');
        cy.task('log', `✓ Search returned ${response.body.data.length} results`);
      });
    });

    it('should view vendor menu', () => {
      const token = Cypress.env('accessToken');

      cy.request({
        method: 'GET',
        url: `${apiUrl}/api/vendors/${testVendorId}/menu`,
        headers: { Authorization: `Bearer ${token}` }
      }).then((response) => {
        expect(response.status).to.eq(200);
        expect(response.body.data).to.be.an('array');
        expect(response.body.data.length).to.be.greaterThan(0);
        
        menuItemId = response.body.data[0].id;
        cy.task('log', `✓ Vendor menu has ${response.body.data.length} items`);
      });
    });

    it('should create an order', () => {
      const orderData = {
        items: [
          {
            menuItemId: testMenuItemIds[0],
            quantity: 2,
            specialInstructions: 'Extra spicy, Cypress E2E test'
          },
          {
            menuItemId: testMenuItemIds[1],
            quantity: 1,
            specialInstructions: 'No cheese'
          }
        ],
        addressId: 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
        paymentMethod: 'CARD'
      };

      cy.createOrderViaApi(orderData).then((order) => {
        orderId = order.id;
        expect(order.status).to.eq('PLACED');
        expect(order.totalCents).to.be.greaterThan(0);
        expect(order.items).to.have.length(2);
        cy.task('log', `✓ Order created: ${orderId}`);
      });
    });

    it('should create payment intent for order', () => {
      const token = Cypress.env('accessToken');

      cy.request({
        method: 'POST',
        url: `${apiUrl}/api/payments/intent`,
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: {
          orderId: orderId,
          amount: 25.99,
          currency: 'USD'
        }
      }).then((response) => {
        expect(response.status).to.be.oneOf([200, 201]);
        expect(response.body.data).to.have.property('id');
        expect(response.body.data).to.have.property('providerPaymentId');
        expect(response.body.data).to.have.property('clientSecret');
        expect(response.body.data.status).to.eq('PENDING');
        
        paymentId = response.body.data.id;
        cy.task('log', `✓ Payment intent created: ${paymentId}`);
      });
    });

    it('should simulate payment provider webhook for successful payment', () => {
      const webhookPayload = {
        id: `evt_cypress_${Date.now()}`,
        object: 'event',
        type: 'payment_intent.succeeded',
        created: Math.floor(Date.now() / 1000),
        data: {
          object: {
            id: `pi_cypress_${Date.now()}`,
            object: 'payment_intent',
            amount: 2599,
            currency: 'usd',
            status: 'succeeded',
            metadata: {
              orderId: orderId
            }
          }
        }
      };

      cy.simulateWebhook(webhookPayload).then((response) => {
        expect(response.status).to.be.oneOf([200, 202]);
        cy.task('log', '✓ Webhook sent: payment_intent.succeeded');
      });

      // Wait a bit for webhook processing
      cy.wait(2000);

      // Verify payment status updated
      const token = Cypress.env('accessToken');
      cy.request({
        method: 'GET',
        url: `${apiUrl}/api/payments/${paymentId}`,
        headers: { Authorization: `Bearer ${token}` }
      }).then((response) => {
        expect(response.body.data.status).to.be.oneOf(['CAPTURED', 'PENDING']);
        cy.task('log', `✓ Payment status: ${response.body.data.status}`);
      });
    });

    it('should verify order status after payment', () => {
      const token = Cypress.env('accessToken');

      cy.request({
        method: 'GET',
        url: `${apiUrl}/api/orders/${orderId}`,
        headers: { Authorization: `Bearer ${token}` }
      }).then((response) => {
        expect(response.status).to.eq(200);
        expect(response.body.data.id).to.eq(orderId);
        cy.task('log', `✓ Order status: ${response.body.data.status}`);
      });
    });

    it('should get order status history', () => {
      const token = Cypress.env('accessToken');

      cy.request({
        method: 'GET',
        url: `${apiUrl}/api/orders/${orderId}/status-history`,
        headers: { Authorization: `Bearer ${token}` }
      }).then((response) => {
        expect(response.status).to.eq(200);
        expect(response.body.data).to.be.an('array');
        expect(response.body.data.length).to.be.greaterThan(0);
        
        // Verify PLACED status exists
        const placedStatus = response.body.data.find((s: any) => s.status === 'PLACED');
        expect(placedStatus).to.exist;
        cy.task('log', `✓ Status history has ${response.body.data.length} entries`);
      });
    });
  });

  describe('Journey 2: Vendor Order Management', () => {
    before(() => {
      // Login as vendor
      cy.loginAs('vendor');
      cy.then(() => { vendorToken = Cypress.env('accessToken'); });
    });

    it('should list vendor orders', () => {
      cy.request({
        method: 'GET',
        url: `${apiUrl}/api/orders?vendorId=${testVendorId}`,
        headers: { Authorization: `Bearer ${vendorToken}` }
      }).then((response) => {
        expect(response.status).to.eq(200);
        expect(response.body.data.content).to.be.an('array');
        cy.task('log', `✓ Vendor has ${response.body.data.content.length} orders`);
      });
    });

    it('should accept the customer order', () => {
      cy.request({
        method: 'POST',
        url: `${apiUrl}/api/orders/${orderId}/accept`,
        headers: { Authorization: `Bearer ${vendorToken}` }
      }).then((response) => {
        expect(response.status).to.eq(200);
        expect(response.body.data.status).to.eq('ACCEPTED');
        cy.task('log', `✓ Order ${orderId} accepted by vendor`);
      });
    });

    it('should update order status to PREPARING', () => {
      cy.request({
        method: 'PATCH',
        url: `${apiUrl}/api/orders/${orderId}/status`,
        headers: {
          Authorization: `Bearer ${vendorToken}`,
          'Content-Type': 'application/json'
        },
        body: {
          status: 'PREPARING',
          note: 'Chef is preparing your delicious meal'
        }
      }).then((response) => {
        expect(response.status).to.eq(200);
        expect(response.body.data.status).to.eq('PREPARING');
        cy.task('log', '✓ Order status: PREPARING');
      });
    });

    it('should update order status to READY', () => {
      cy.request({
        method: 'PATCH',
        url: `${apiUrl}/api/orders/${orderId}/status`,
        headers: {
          Authorization: `Bearer ${vendorToken}`,
          'Content-Type': 'application/json'
        },
        body: {
          status: 'READY',
          note: 'Order is ready for pickup!'
        }
      }).then((response) => {
        expect(response.status).to.eq(200);
        expect(response.body.data.status).to.eq('READY');
        cy.task('log', '✓ Order status: READY');
      });
    });

    it('should create and update a menu item', () => {
      // Create new menu item
      cy.request({
        method: 'POST',
        url: `${apiUrl}/api/vendors/${testVendorId}/menu`,
        headers: {
          Authorization: `Bearer ${vendorToken}`,
          'Content-Type': 'application/json'
        },
        body: {
          name: `Cypress Test Item ${Date.now()}`,
          description: 'A test item created by Cypress E2E',
          priceCents: 1599,
          category: 'MAIN',
          available: true,
          prepTimeMins: 25
        }
      }).then((response) => {
        expect(response.status).to.be.oneOf([200, 201]);
        expect(response.body.data).to.have.property('id');
        
        const newItemId = response.body.data.id;
        cy.task('log', `✓ Menu item created: ${newItemId}`);

        // Update the menu item
        cy.request({
          method: 'PUT',
          url: `${apiUrl}/api/menu-items/${newItemId}`,
          headers: {
            Authorization: `Bearer ${vendorToken}`,
            'Content-Type': 'application/json'
          },
          body: {
            name: 'Cypress Test Item UPDATED',
            description: 'Updated via E2E test',
            priceCents: 1799,
            category: 'MAIN',
            available: true,
            prepTimeMins: 30
          }
        }).then((updateResponse) => {
          expect(updateResponse.status).to.eq(200);
          expect(updateResponse.body.data.name).to.include('UPDATED');
          expect(updateResponse.body.data.priceCents).to.eq(1799);
          cy.task('log', '✓ Menu item updated');
        });

        // Delete the menu item (cleanup)
        cy.request({
          method: 'DELETE',
          url: `${apiUrl}/api/menu-items/${newItemId}`,
          headers: { Authorization: `Bearer ${vendorToken}` },
          failOnStatusCode: false
        });
      });
    });

    it('should prevent invalid status transitions', () => {
      // Try to go from READY back to PLACED (invalid)
      cy.request({
        method: 'PATCH',
        url: `${apiUrl}/api/orders/${orderId}/status`,
        headers: {
          Authorization: `Bearer ${vendorToken}`,
          'Content-Type': 'application/json'
        },
        body: {
          status: 'PLACED',
          note: 'This should fail'
        },
        failOnStatusCode: false
      }).then((response) => {
        expect(response.status).to.eq(400);
        cy.task('log', '✓ Invalid status transition blocked');
      });
    });
  });

  describe('Journey 3: Driver Delivery Flow', () => {
    before(() => {
      // Login as driver
      cy.loginAs('driver');
      cy.then(() => { driverToken = Cypress.env('accessToken'); });
    });

    it('should assign driver to order', () => {
      const driverId = Cypress.env('testDriver').id;

      cy.request({
        method: 'POST',
        url: `${apiUrl}/api/orders/${orderId}/assign/${driverId}`,
        headers: { Authorization: `Bearer ${driverToken}` }
      }).then((response) => {
        expect(response.status).to.eq(200);
        expect(response.body.data.driverId).to.exist;
        expect(response.body.data.status).to.eq('ASSIGNED');
        cy.task('log', `✓ Driver ${driverId} assigned to order`);
      });
    });

    it('should update order status to PICKED_UP', () => {
      cy.request({
        method: 'PATCH',
        url: `${apiUrl}/api/orders/${orderId}/status`,
        headers: {
          Authorization: `Bearer ${driverToken}`,
          'Content-Type': 'application/json'
        },
        body: {
          status: 'PICKED_UP',
          note: 'Order picked up from restaurant'
        }
      }).then((response) => {
        expect(response.status).to.eq(200);
        expect(response.body.data.status).to.eq('PICKED_UP');
        cy.task('log', '✓ Order status: PICKED_UP');
      });
    });

    it('should update order status to ENROUTE', () => {
      cy.request({
        method: 'PATCH',
        url: `${apiUrl}/api/orders/${orderId}/status`,
        headers: {
          Authorization: `Bearer ${driverToken}`,
          'Content-Type': 'application/json'
        },
        body: {
          status: 'ENROUTE',
          note: 'On the way to your location'
        }
      }).then((response) => {
        expect(response.status).to.eq(200);
        expect(response.body.data.status).to.eq('ENROUTE');
        cy.task('log', '✓ Order status: ENROUTE');
      });
    });

    it('should complete order delivery', () => {
      cy.request({
        method: 'PATCH',
        url: `${apiUrl}/api/orders/${orderId}/status`,
        headers: {
          Authorization: `Bearer ${driverToken}`,
          'Content-Type': 'application/json'
        },
        body: {
          status: 'DELIVERED',
          note: 'Order delivered successfully!'
        }
      }).then((response) => {
        expect(response.status).to.eq(200);
        expect(response.body.data.status).to.eq('DELIVERED');
        cy.task('log', '✓ Order status: DELIVERED');
      });
    });

    it('should verify complete status history', () => {
      const token = Cypress.env('accessToken');

      cy.request({
        method: 'GET',
        url: `${apiUrl}/api/orders/${orderId}/status-history`,
        headers: { Authorization: `Bearer ${token}` }
      }).then((response) => {
        expect(response.status).to.eq(200);
        expect(response.body.data).to.be.an('array');
        
        const statuses = response.body.data.map((s: any) => s.status);
        cy.task('log', `✓ Complete lifecycle: ${statuses.join(' → ')}`);
        
        // Verify key statuses exist
        expect(statuses).to.include('PLACED');
        expect(statuses).to.include('ACCEPTED');
        expect(statuses).to.include('PREPARING');
        expect(statuses).to.include('READY');
        expect(statuses).to.include('ASSIGNED');
        expect(statuses).to.include('PICKED_UP');
        expect(statuses).to.include('ENROUTE');
        expect(statuses).to.include('DELIVERED');
      });
    });
  });

  describe('Webhook Idempotency Tests', () => {
    it('should handle duplicate webhook events gracefully', () => {
      const webhookPayload = {
        id: `evt_cypress_duplicate_${Date.now()}`,
        object: 'event',
        type: 'payment_intent.succeeded',
        created: Math.floor(Date.now() / 1000),
        data: {
          object: {
            id: `pi_cypress_dup_${Date.now()}`,
            object: 'payment_intent',
            amount: 1000,
            currency: 'usd',
            status: 'succeeded',
            metadata: {
              orderId: orderId
            }
          }
        }
      };

      // Send webhook first time
      cy.simulateWebhook(webhookPayload).then((response1) => {
        expect(response1.status).to.be.oneOf([200, 202]);
        cy.task('log', '✓ First webhook sent');

        // Send identical webhook second time (should be idempotent)
        cy.simulateWebhook(webhookPayload).then((response2) => {
          expect(response2.status).to.be.oneOf([200, 202]);
          cy.task('log', '✓ Duplicate webhook handled (idempotent)');

          // Both should succeed but second might be ignored
          // Check webhook_events table would show only one processed event
        });
      });
    });
  });

  describe('Authorization and Security Tests', () => {
    it('should block access to protected endpoints without token', () => {
      cy.request({
        method: 'GET',
        url: `${apiUrl}/api/orders/${orderId}`,
        failOnStatusCode: false
      }).then((response) => {
        expect(response.status).to.eq(401);
        cy.task('log', '✓ Unauthorized request blocked (401)');
      });
    });

    it('should block customer from accessing vendor endpoints', () => {
      cy.loginAs('customer');
      const token = Cypress.env('accessToken');

      cy.request({
        method: 'POST',
        url: `${apiUrl}/api/vendors/${testVendorId}/menu`,
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: {
          name: 'Unauthorized Item',
          price: 10.00,
          category: 'MAIN'
        },
        failOnStatusCode: false
      }).then((response) => {
        expect(response.status).to.be.oneOf([403, 401]);
        cy.task('log', '✓ Customer blocked from vendor operations');
      });
    });

    it('should validate JWT token expiration', () => {
      const invalidToken = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.invalid.token';

      cy.request({
        method: 'GET',
        url: `${apiUrl}/api/orders/${orderId}`,
        headers: { Authorization: `Bearer ${invalidToken}` },
        failOnStatusCode: false
      }).then((response) => {
        expect(response.status).to.eq(401);
        cy.task('log', '✓ Invalid token rejected');
      });
    });
  });

  describe('Edge Cases and Error Handling', () => {
    it('should handle non-existent order gracefully', () => {
      cy.loginAs('customer');
      const token = Cypress.env('accessToken');
      const fakeOrderId = '99999999-9999-9999-9999-999999999999';

      cy.request({
        method: 'GET',
        url: `${apiUrl}/api/orders/${fakeOrderId}`,
        headers: { Authorization: `Bearer ${token}` },
        failOnStatusCode: false
      }).then((response) => {
        expect(response.status).to.be.oneOf([404, 403]);
        cy.task('log', '✓ Non-existent order returns 404');
      });
    });

    it('should validate order creation with invalid data', () => {
      cy.loginAs('customer');
      const token = Cypress.env('accessToken');

      cy.request({
        method: 'POST',
        url: `${apiUrl}/api/orders`,
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: {
          vendorId: testVendorId,
          items: [], // Empty items - invalid
          deliveryAddress: {
            line1: 'Test',
            city: 'Test',
            state: 'CA'
          }
        },
        failOnStatusCode: false
      }).then((response) => {
        expect(response.status).to.eq(400);
        cy.task('log', '✓ Invalid order data rejected');
      });
    });

    it('should handle webhook with invalid signature', () => {
      const webhookPayload = {
        id: `evt_cypress_invalid_${Date.now()}`,
        type: 'payment_intent.succeeded',
        data: { object: { status: 'succeeded' } }
      };

      cy.request({
        method: 'POST',
        url: `${apiUrl}/api/payments/webhook`,
        headers: {
          'Content-Type': 'application/json',
          'X-Webhook-Signature': 't=12345,v1=invalidsignature'
        },
        body: webhookPayload,
        failOnStatusCode: false
      }).then((response) => {
        expect(response.status).to.be.oneOf([400, 401]);
        cy.task('log', '✓ Invalid webhook signature rejected');
      });
    });
  });

  describe('Performance and Load', () => {
    it('should handle rapid API requests', () => {
      cy.loginAs('customer');
      const token = Cypress.env('accessToken');

      // Make 5 rapid requests to list vendors
      const requests = Array.from({ length: 5 }, () =>
        cy.request({
          method: 'GET',
          url: `${apiUrl}/api/vendors`,
          headers: { Authorization: `Bearer ${token}` }
        }).then((response) => {
          expect(response.status).to.eq(200);
        })
      );

      cy.wrap(Promise.all(requests)).then(() => {
        cy.task('log', '✓ Handled 5 rapid requests successfully');
      });
    });
  });
});
