/**
 * Sentry Error Monitoring Integration
 * 
 * Initializes Sentry for error tracking and performance monitoring.
 * Requires @sentry/react to be installed: npm install @sentry/react
 * 
 * Set VITE_SENTRY_DSN environment variable to enable.
 * When DSN is not set, Sentry operates in no-op mode.
 */

// Type-safe Sentry initialization that works even if @sentry/react is not installed
let SentryModule: typeof import('@sentry/react') | null = null;

export async function initSentry(): Promise<void> {
  const dsn = import.meta.env.VITE_SENTRY_DSN;
  
  if (!dsn) {
    console.debug('[Sentry] No DSN configured — error monitoring disabled');
    return;
  }

  try {
    // Dynamic import so the app works even without @sentry/react installed
    SentryModule = await import('@sentry/react');
    
    SentryModule.init({
      dsn,
      environment: import.meta.env.VITE_SENTRY_ENVIRONMENT || 'development',
      release: `quickbite-frontend@${import.meta.env.VITE_APP_VERSION || '0.0.1'}`,
      
      // Performance monitoring — sample 20% of transactions in production
      tracesSampleRate: import.meta.env.PROD ? 0.2 : 1.0,
      
      // Session replay for debugging (1% in production, 100% on error)
      replaysSessionSampleRate: import.meta.env.PROD ? 0.01 : 0,
      replaysOnErrorSampleRate: import.meta.env.PROD ? 1.0 : 0,
      
      // Filter out noise
      ignoreErrors: [
        // Browser extensions
        /extensions\//i,
        /^chrome-extension:\/\//,
        // Network errors (handled by axios interceptor)
        'Network Error',
        'Request aborted',
        'timeout of',
        // ResizeObserver loop (benign)
        'ResizeObserver loop',
      ],
      
      beforeSend(event) {
        // Strip PII from error events
        if (event.request?.cookies) {
          delete event.request.cookies;
        }
        return event;
      },
    });

    console.info('[Sentry] Initialized with DSN');
  } catch (err) {
    console.warn('[Sentry] Failed to initialize — @sentry/react may not be installed:', err);
  }
}

/**
 * Capture an exception manually.
 * No-op if Sentry is not initialized.
 */
export function captureException(error: unknown, context?: Record<string, unknown>): void {
  if (SentryModule) {
    SentryModule.captureException(error, { extra: context });
  }
}

/**
 * Capture a message manually.
 * No-op if Sentry is not initialized.
 */
export function captureMessage(message: string, level: 'info' | 'warning' | 'error' = 'info'): void {
  if (SentryModule) {
    SentryModule.captureMessage(message, level);
  }
}

/**
 * Set user context for error tracking.
 * Call on login. Call with null on logout.
 */
export function setUser(user: { id: string; email: string; role: string } | null): void {
  if (SentryModule) {
    SentryModule.setUser(user ? { id: user.id, email: user.email, segment: user.role } : null);
  }
}
