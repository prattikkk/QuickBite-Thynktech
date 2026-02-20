// ***********************************************************
// This support file is processed and loaded automatically before your test files.
//
// You can read more here:
// https://on.cypress.io/configuration
// ***********************************************************

/// <reference types="cypress" />

// Custom command type declarations
declare global {
  namespace Cypress {
    interface Chainable {
      /**
       * Custom command to login as a specific role
       * @param role - 'customer' | 'vendor' | 'driver' | 'admin'
       * @example cy.loginAs('customer')
       */
      loginAs(role: 'customer' | 'vendor' | 'driver' | 'admin'): Chainable<void>;
      
      /**
       * Custom command to login via API and store tokens
       * @param email - Email address
       * @param password - Password
       * @example cy.loginViaApi('user@test.com', 'password123')
       */
      loginViaApi(email: string, password: string): Chainable<any>;
      
      /**
       * Custom command to wait for API request to complete
       * @param alias - Request alias
       * @example cy.waitForApi('@getOrders')
       */
      waitForApi(alias: string): Chainable<any>;
      
      /**
       * Custom command to create an order via API
       * @param orderData - Order creation payload
       * @example cy.createOrderViaApi({ vendorId, items, deliveryAddress })
       */
      createOrderViaApi(orderData: any): Chainable<any>;
      
      /**
       * Custom command to simulate webhook
       * @param payload - Webhook payload
       * @example cy.simulateWebhook({ type: 'payment_intent.succeeded', data: {...} })
       */
      simulateWebhook(payload: any): Chainable<any>;
    }
  }
}

// Login as predefined test user
Cypress.Commands.add('loginAs', (role: 'customer' | 'vendor' | 'driver' | 'admin') => {
  const userMap = {
    customer: Cypress.env('testCustomer'),
    vendor: Cypress.env('testVendor'),
    driver: Cypress.env('testDriver'),
    admin: { email: 'e2e_admin@test.com', password: 'Test@1234' }
  };

  const user = userMap[role];
  
  cy.loginViaApi(user.email, user.password);
});

// Login via API
Cypress.Commands.add('loginViaApi', (email: string, password: string) => {
  cy.request({
    method: 'POST',
    url: `${Cypress.env('apiUrl')}/api/auth/login`,
    body: { email, password }
  }).then((response) => {
    expect(response.status).to.eq(200);
    // API wraps response in { success, data: { accessToken, refreshToken, ... } }
    const data = response.body.data || response.body;
    expect(data).to.have.property('accessToken');
    expect(data).to.have.property('refreshToken');

    // Store tokens in localStorage
    cy.window().then((win) => {
      win.localStorage.setItem('accessToken', data.accessToken);
      win.localStorage.setItem('refreshToken', data.refreshToken);
    });

    // Also store in Cypress env for API requests
    Cypress.env('accessToken', data.accessToken);
    Cypress.env('currentUser', data);
  });
});

// Wait for API request with better error handling
Cypress.Commands.add('waitForApi', (alias: string) => {
  return cy.wait(alias).its('response.statusCode').should('be.oneOf', [200, 201]);
});

// Create order via API
Cypress.Commands.add('createOrderViaApi', (orderData: any) => {
  const token = Cypress.env('accessToken');
  
  return cy.request({
    method: 'POST',
    url: `${Cypress.env('apiUrl')}/api/orders`,
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: orderData
  }).then((response) => {
    expect(response.status).to.be.oneOf([200, 201]);
    expect(response.body.data).to.have.property('id');
    
    // Store order ID for later use
    Cypress.env('lastOrderId', response.body.data.id);
    
    return response.body.data;
  });
});

// Simulate webhook with proper signature
Cypress.Commands.add('simulateWebhook', (payload: any) => {
  const secret = Cypress.env('webhookSecret');
  
  // Generate signature using task
  return cy.task('generateWebhookSignature', { payload, secret }).then((signature) => {
    return cy.request({
      method: 'POST',
      url: `${Cypress.env('apiUrl')}/api/payments/webhook`,
      headers: {
        'Content-Type': 'application/json',
        'X-Webhook-Signature': signature as string
      },
      body: payload,
      failOnStatusCode: false // Don't fail test on webhook errors
    });
  });
});

// Automatically clear localStorage before each test
beforeEach(() => {
  cy.clearLocalStorage();
  cy.clearCookies();
});

// Add custom error logging
Cypress.on('fail', (error) => {
  cy.task('log', `Test failed: ${error.message}`);
  throw error;
});

export {};
