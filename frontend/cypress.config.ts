import { defineConfig } from 'cypress';

export default defineConfig({
  e2e: {
    baseUrl: process.env.CYPRESS_BASE_URL || 'http://localhost:5173',
    specPattern: 'cypress/e2e/**/*.cy.{js,jsx,ts,tsx}',
    supportFile: 'cypress/support/e2e.ts',
    video: true,
    videosFolder: 'cypress/videos',
    screenshotsFolder: 'cypress/screenshots',
    viewportWidth: 1280,
    viewportHeight: 720,
    defaultCommandTimeout: 10000,
    requestTimeout: 10000,
    responseTimeout: 10000,
    pageLoadTimeout: 30000,
    retries: {
      runMode: 2,
      openMode: 0
    },
    env: {
      apiUrl: process.env.API_BASE_URL || 'http://localhost:8080',
      wsUrl: process.env.WS_URL || 'ws://localhost:8080/ws',
      webhookSecret: process.env.PAYMENT_WEBHOOK_SECRET || 'whsec_test_placeholder_secret_2026',
      // Test user credentials (from seed data)
      testCustomer: {
        email: 'e2e_customer@test.com',
        password: 'Test@1234',
        id: '55555555-5555-5555-5555-555555555555'
      },
      testVendor: {
        email: 'e2e_vendor@test.com',
        password: 'Test@1234',
        id: '66666666-6666-6666-6666-666666666666'
      },
      testDriver: {
        email: 'e2e_driver@test.com',
        password: 'Test@1234',
        id: '77777777-7777-7777-7777-777777777777'
      },
      testVendorId: 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
      testMenuItemIds: [
        'dddddddd-dddd-dddd-dddd-dddddddddddd',
        'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee',
        'ffffffff-ffff-ffff-ffff-ffffffffffff'
      ]
    },
    setupNodeEvents(on, config) {
      // Implement node event listeners here if needed
      on('task', {
        log(message) {
          console.log(message);
          return null;
        },
        generateWebhookSignature({ payload, secret }) {
          const crypto = require('crypto');
          const timestamp = Math.floor(Date.now() / 1000);
          const signature = crypto
            .createHmac('sha256', secret)
            .update(JSON.stringify(payload))
            .digest('hex');
          return `t=${timestamp},v1=${signature}`;
        }
      });

      return config;
    }
  }
});
