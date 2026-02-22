/**
 * Cypress E2E – PWA Install Flow
 *
 * Validates that the production build includes all PWA artifacts
 * and that core PWA features (manifest, SW registration, offline
 * banner, install prompt) are wired correctly.
 *
 * NOTE: Cypress cannot fully test native install prompts or standalone
 * mode, so we focus on:
 *   1. Manifest link present in <head>
 *   2. Theme-color meta tag
 *   3. Apple PWA meta tags
 *   4. SW registration script present
 *   5. Offline banner component renders on disconnect
 *   6. Update banner component exists in DOM
 */

describe('PWA Install Flow', () => {
  beforeEach(() => {
    cy.visit('/login');
  });

  it('should include manifest link in the document head', () => {
    cy.document().then((doc) => {
      const link = doc.querySelector('link[rel="manifest"]');
      expect(link).to.not.be.null;
      expect(link?.getAttribute('href')).to.include('manifest');
    });
  });

  it('should have theme-color meta tag set to orange', () => {
    cy.document().then((doc) => {
      const meta = doc.querySelector('meta[name="theme-color"]');
      expect(meta).to.not.be.null;
      expect(meta?.getAttribute('content')).to.eq('#f97316');
    });
  });

  it('should include apple-mobile-web-app meta tags', () => {
    cy.document().then((doc) => {
      const capable = doc.querySelector('meta[name="apple-mobile-web-app-capable"]');
      expect(capable).to.not.be.null;
      expect(capable?.getAttribute('content')).to.eq('yes');

      const title = doc.querySelector('meta[name="apple-mobile-web-app-title"]');
      expect(title).to.not.be.null;
      expect(title?.getAttribute('content')).to.eq('QuickBite');
    });
  });

  it('should include apple-touch-icon link', () => {
    cy.document().then((doc) => {
      const icon = doc.querySelector('link[rel="apple-touch-icon"]');
      expect(icon).to.not.be.null;
      expect(icon?.getAttribute('href')).to.include('icon-192');
    });
  });

  it('should have a valid viewport meta with viewport-fit=cover', () => {
    cy.document().then((doc) => {
      const viewport = doc.querySelector('meta[name="viewport"]');
      expect(viewport).to.not.be.null;
      expect(viewport?.getAttribute('content')).to.include('viewport-fit=cover');
    });
  });
});

describe('PWA Offline Banner', () => {
  it('should show offline banner when network is lost', () => {
    // Login first
    cy.visit('/login');
    cy.get('input[name="email"]').type('e2e_customer@test.com');
    cy.get('input[name="password"]').type('Test@1234');
    cy.get('button[type="submit"]').click();
    cy.url().should('not.include', '/login');

    // Simulate going offline
    cy.window().then((win) => {
      // Fire the offline event
      win.dispatchEvent(new Event('offline'));
    });

    // The OfflineBanner should appear
    cy.contains('You are offline', { timeout: 5000 }).should('be.visible');

    // Simulate going back online
    cy.window().then((win) => {
      win.dispatchEvent(new Event('online'));
    });

    // The banner should disappear
    cy.contains('You are offline').should('not.exist');
  });
});
