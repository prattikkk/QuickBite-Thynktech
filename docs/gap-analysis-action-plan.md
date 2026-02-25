# QuickBite MVP - Gap Analysis & Action Plan
**Generated**: February 24, 2026  
**Based on**: 8-Week Execution Plan vs Current Implementation

---

## Critical Gaps Summary

| Priority | Category | Gap | Impact | Effort |
|----------|----------|-----|--------|--------|
| 🔴 CRITICAL | Security | HttpOnly refresh token cookies | High | 4h |
| 🔴 CRITICAL | Security | Account lockout (5 failed attempts) | High | 4h |
| 🔴 CRITICAL | Security | Security headers (HSTS, X-Frame, etc.) | High | 2h |
| 🔴 CRITICAL | Email | HTML email templates (5 templates) | High | 8h |
| 🔴 CRITICAL | Frontend | Service worker for PWA push | High | 6h |
| 🟠 HIGH | Admin | Refund UI workflow | Medium | 4h |
| 🟠 HIGH | DevOps | S3 file storage migration | Medium | 8h |
| 🟠 HIGH | Monitoring | Sentry error tracking | Medium | 3h |
| 🟡 MEDIUM | Frontend | Notification preferences page | Low | 4h |
| 🟡 MEDIUM | Maps | Google Maps API upgrade (optional) | Low | 12h |
| 🟢 LOW | UX | Loading skeletons | Low | 4h |
| 🟢 LOW | Testing | Performance tests (k6) | Low | 4h |

**Total Critical Work**: ~30 hours (4 days)  
**Total High Priority**: ~15 hours (2 days)  
**Total Medium**: ~16 hours (2 days)

---

## Phase 1: Security Hardening (Week 7) - 2 Days

### Task 1.1: HttpOnly Secure Cookies for Refresh Token
**Current State**: Refresh token in response body, stored in localStorage  
**Target**: HttpOnly, Secure, SameSite=Strict cookie

**Backend Changes**:
```java
// AuthenticationController.java
@PostMapping("/login")
public ResponseEntity<ApiResponse<AuthenticationResponse>> login(
    @Valid @RequestBody LoginRequest request,
    HttpServletResponse response  // Add this
) {
    AuthenticationResponse auth = authService.login(request);
    
    // Set refresh token as HttpOnly cookie
    Cookie refreshCookie = new Cookie("refreshToken", auth.getRefreshToken());
    refreshCookie.setHttpOnly(true);
    refreshCookie.setSecure(true);  // HTTPS only
    refreshCookie.setPath("/api/auth");
    refreshCookie.setMaxAge(7 * 24 * 60 * 60);  // 7 days
    refreshCookie.setAttribute("SameSite", "Strict");
    response.addCookie(refreshCookie);
    
    // Remove refreshToken from response body
    auth.setRefreshToken(null);
    return ResponseEntity.ok(ApiResponse.success(auth, "Login successful"));
}

@PostMapping("/refresh")
public ResponseEntity<ApiResponse<AuthenticationResponse>> refresh(
    @CookieValue(name = "refreshToken", required = false) String refreshToken,
    HttpServletResponse response
) {
    if (refreshToken == null) {
        throw new AuthException("Refresh token missing");
    }
    // ... refresh logic, set new cookie
}

@PostMapping("/logout")
public ResponseEntity<ApiResponse<Void>> logout(
    HttpServletResponse response
) {
    // Clear cookie
    Cookie cookie = new Cookie("refreshToken", "");
    cookie.setMaxAge(0);
    cookie.setPath("/api/auth");
    response.addCookie(cookie);
    return ResponseEntity.ok(ApiResponse.success(null, "Logged out"));
}
```

**Frontend Changes**:
```typescript
// auth.service.ts - Remove refreshToken from localStorage
async login(credentials: LoginRequest): Promise<AuthResponse> {
  const data = await api.post('/auth/login', credentials) as AuthResponse;
  localStorage.setItem('accessToken', data.accessToken);
  // Remove: localStorage.setItem('refreshToken', data.refreshToken);
  return data;
}

async refresh(): Promise<AuthResponse> {
  // Refresh token now sent automatically via cookie
  const data = await api.post('/auth/refresh', {}) as AuthResponse;
  localStorage.setItem('accessToken', data.accessToken);
  return data;
}
```

**Files to Modify**:
- `backend/src/main/java/com/quickbite/auth/controller/AuthenticationController.java`
- `frontend/src/services/auth.service.ts`
- `frontend/src/store/authStore.ts`

**Estimated Time**: 4 hours  
**Testing**: Manual login/refresh/logout cycle + cookie inspection

---

### Task 1.2: Account Lockout After 5 Failed Attempts
**Current State**: No lockout mechanism  
**Target**: Lock account for 30 minutes after 5 failed attempts

**Backend Changes**:

1. **Migration V29: Add lockout fields**
```sql
-- V29__add_account_lockout.sql
ALTER TABLE users ADD COLUMN failed_login_attempts INT DEFAULT 0;
ALTER TABLE users ADD COLUMN locked_until TIMESTAMP;
```

2. **AuthService.java**:
```java
public AuthenticationResponse login(LoginRequest request) {
    User user = userRepository.findByEmail(request.getEmail())
        .orElseThrow(() -> new AuthException("Invalid credentials"));
    
    // Check if locked
    if (user.getLockedUntil() != null && 
        user.getLockedUntil().isAfter(OffsetDateTime.now())) {
        long minutesLeft = Duration.between(
            OffsetDateTime.now(), 
            user.getLockedUntil()
        ).toMinutes();
        throw new AuthException("Account locked. Try again in " + minutesLeft + " minutes");
    }
    
    try {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getEmail(), 
                request.getPassword()
            )
        );
        
        // Successful login - reset failed attempts
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);
        
        // ... generate tokens
        
    } catch (BadCredentialsException e) {
        // Increment failed attempts
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        
        if (attempts >= 5) {
            user.setLockedUntil(OffsetDateTime.now().plusMinutes(30));
            userRepository.save(user);
            throw new AuthException("Account locked due to too many failed attempts. Try again in 30 minutes");
        }
        
        userRepository.save(user);
        throw new AuthException("Invalid credentials. " + (5 - attempts) + " attempts remaining");
    }
}
```

**Frontend Changes**:
```typescript
// Login.tsx - Display lockout message
catch (err: any) {
  if (err.message.includes('locked')) {
    showError('Account locked. Check your email for unlock instructions');
  } else {
    showError(err.message || 'Login failed');
  }
}
```

**Files to Modify**:
- `backend/src/main/resources/db/migration/V29__add_account_lockout.sql` (CREATE)
- `backend/src/main/java/com/quickbite/users/entity/User.java`
- `backend/src/main/java/com/quickbite/auth/service/AuthService.java`
- `frontend/src/pages/Login.tsx`

**Estimated Time**: 4 hours  
**Testing**: Attempt 5+ failed logins, verify lockout, wait 30min or reset DB

---

### Task 1.3: Security Headers
**Current State**: Default Spring Security headers  
**Target**: HSTS, X-Frame-Options, X-Content-Type-Options, CSP

**Backend Changes**:
```java
// SecurityConfig.java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        // ... existing config
        .headers(headers -> headers
            .httpStrictTransportSecurity(hsts -> hsts
                .includeSubDomains(true)
                .maxAgeInSeconds(31536000)  // 1 year
            )
            .frameOptions(frame -> frame.deny())
            .xssProtection(xss -> xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
            .contentTypeOptions(Customizer.withDefaults())
            .contentSecurityPolicy(csp -> csp
                .policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; font-src 'self' data:;")
            )
        );
    return http.build();
}
```

**Files to Modify**:
- `backend/src/main/java/com/quickbite/auth/security/SecurityConfig.java`

**Estimated Time**: 2 hours  
**Testing**: curl -I http://localhost:8080 | grep -i "strict-transport"

---

### Task 1.4: Content-Type Validation
**Current State**: No validation  
**Target**: Reject requests with wrong Content-Type

**Backend Changes**:
```java
// ContentTypeFilter.java (NEW)
@Component
@Order(1)
public class ContentTypeFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response,
                                    FilterChain chain) 
            throws ServletException, IOException {
        
        String method = request.getMethod();
        if ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method)) {
            String contentType = request.getContentType();
            if (contentType == null || 
                (!contentType.startsWith("application/json") && 
                 !contentType.startsWith("multipart/form-data"))) {
                response.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Content-Type must be application/json or multipart/form-data\"}");
                return;
            }
        }
        
        chain.doFilter(request, response);
    }
}
```

**Files to Create**:
- `backend/src/main/java/com/quickbite/common/filter/ContentTypeFilter.java`

**Estimated Time**: 2 hours  
**Testing**: curl -X POST http://localhost:8080/api/auth/login -H "Content-Type: text/plain"

---

## Phase 2: Missing Core Features - 1 Day

### Task 2.1: HTML Email Templates
**Current State**: Email service sends plain text  
**Target**: Professional HTML templates with Thymeleaf

**Backend Changes**:

1. **Add Thymeleaf dependency** (pom.xml):
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>
```

2. **Create templates**:
```html
<!-- src/main/resources/templates/email/password-reset.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <style>
        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
        .header { background: #f59e0b; color: white; padding: 20px; text-align: center; }
        .content { background: #f9fafb; padding: 30px; }
        .button { display: inline-block; padding: 12px 24px; background: #f59e0b; color: white; text-decoration: none; border-radius: 4px; }
        .footer { text-align: center; padding: 20px; color: #6b7280; font-size: 12px; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>QuickBite Password Reset</h1>
        </div>
        <div class="content">
            <p>Hello <span th:text="${name}">User</span>,</p>
            <p>We received a request to reset your password. Click the button below to proceed:</p>
            <p style="text-align: center; margin: 30px 0;">
               <a th:href="${resetUrl}" class="button">Reset Password</a>
            </p>
            <p>This link will expire in 1 hour.</p>
            <p>If you didn't request this, please ignore this email.</p>
        </div>
        <div class="footer">
            <p>&copy; 2026 QuickBite. All rights reserved.</p>
        </div>
    </div>
</body>
</html>
```

3. **Update EmailService**:
```java
// EmailService.java
public interface EmailService {
    void sendPasswordResetEmail(String to, String name, String token);
    void sendEmailVerificationEmail(String to, String name, String token);
    void sendOrderConfirmationEmail(String to, String name, String orderNumber, BigDecimal total);
    void sendOrderStatusChangeEmail(String to, String orderNumber, String newStatus);
    void sendWelcomeEmail(String to, String name);
}

// SendGridEmailService.java or ConsoleEmailService.java
@Service
@RequiredArgsConstructor
public class SendGridEmailService implements EmailService {
    
    private final TemplateEngine templateEngine;  // Add Thymeleaf
    
    @Override
    public void sendPasswordResetEmail(String to, String name, String token) {
        Context context = new Context();
        context.setVariable("name", name);
        context.setVariable("resetUrl", frontendUrl + "/reset-password?token=" + token);
        
        String htmlBody = templateEngine.process("email/password-reset", context);
        
        // Send via SendGrid with HTML body
        sendHtmlEmail(to, "Reset Your Password", htmlBody);
    }
    
    // ... similar for other templates
}
```

**Templates to Create** (src/main/resources/templates/email/):
1. `password-reset.html`
2. `email-verification.html`
3. `order-confirmation.html`
4. `order-status-change.html`
5. `welcome.html`

**Files to Modify**:
- `backend/pom.xml`
- `backend/src/main/java/com/quickbite/email/service/SendGridEmailService.java`
- `backend/src/main/java/com/quickbite/email/service/ConsoleEmailService.java`

**Files to Create**:
- 5 HTML template files

**Estimated Time**: 8 hours (design + coding + testing)  
**Testing**: Send test email via /api/auth/forgot-password, inspect HTML rendering

---

### Task 2.2: Service Worker for PWA Push Notifications
**Current State**: No service worker, push notifications may not work  
**Target**: Full PWA with push notification support

**Frontend Changes**:

1. **Create service worker**:
```javascript
// public/sw.js
self.addEventListener('install', (event) => {
  console.log('Service Worker installing');
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  console.log('Service Worker activating');
  return self.clients.claim();
});

self.addEventListener('push', (event) => {
  const data = event.data ? event.data.json() : {};
  const title = data.title || 'QuickBite Notification';
  const options = {
    body: data.body || 'You have a new notification',
    icon: '/icon-192.png',
    badge: '/badge-72.png',
    tag: data.tag || 'quickbite-notification',
    data: data.data || {},
    requireInteraction: false,
    actions: [
      { action: 'view', title: 'View Order' }
    ]
  };
  
  event.waitUntil(
    self.registration.showNotification(title, options)
  );
});

self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  
  if (event.action === 'view' || !event.action) {
    const orderId = event.notification.data.orderId;
    const url = orderId 
      ? `/orders/${orderId}` 
      : '/';
    
    event.waitUntil(
      clients.openWindow(url)
    );
  }
});
```

2. **Register service worker**:
```typescript
// main.tsx - Add after app render
if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/sw.js')
      .then(registration => {
        console.log('SW registered:', registration);
      })
      .catch(err => {
        console.error('SW registration failed:', err);
      });
  });
}
```

3. **Request push permission**:
```typescript
// deviceService.ts - Update registerPushToken
async requestPushPermission(): Promise<string | null> {
  if (!('Notification' in window)) {
    console.warn('Push notifications not supported');
    return null;
  }
  
  const permission = await Notification.requestPermission();
  if (permission !== 'granted') {
    console.warn('Push permission denied');
    return null;
  }
  
  const registration = await navigator.serviceWorker.ready;
  const subscription = await registration.pushManager.subscribe({
    userVisibleOnly: true,
    applicationServerKey: urlBase64ToUint8Array(import.meta.env.VITE_VAPID_PUBLIC_KEY)
  });
  
  return JSON.stringify(subscription);
}

function urlBase64ToUint8Array(base64String: string): Uint8Array {
  const padding = '='.repeat((4 - base64String.length % 4) % 4);
  const base64 = (base64String + padding).replace(/\\-/g, '+').replace(/_/g, '/');
  const rawData = window.atob(base64);
  const outputArray = new Uint8Array(rawData.length);
  for (let i = 0; i < rawData.length; ++i) {
    outputArray[i] = rawData.charCodeAt(i);
  }
  return outputArray;
}
```

4. **Trigger permission after first order**:
```typescript
// Checkout.tsx - After successful order
if (orderResponse.success) {
  // Ask for push permission after first successful order
  const hasAskedBefore = localStorage.getItem('push-permission-asked');
  if (!hasAskedBefore) {
    setTimeout(async () => {
      const token = await deviceService.requestPushPermission();
      if (token) {
        await deviceService.register({
          token,
          platform: 'WEB',
          deviceName: navigator.userAgent
        });
      }
      localStorage.setItem('push-permission-asked', 'true');
    }, 2000);  // Wait 2s after order success
  }
}
```

**Files to Create**:
- `public/sw.js`

**Files to Modify**:
- `frontend/src/main.tsx`
- `frontend/src/services/device.service.ts`
- `frontend/src/pages/Checkout.tsx`
- `frontend/.env` (add VITE_VAPID_PUBLIC_KEY)

**Estimated Time**: 6 hours  
**Testing**: Complete order, grant permission, trigger push notification from backend

---

### Task 2.3: Admin Refund UI
**Current State**: Refund API exists, no UI  
**Target**: Refund button + modal in AdminOrderTimeline

**Frontend Changes**:

1. **Create RefundModal component**:
```typescript
// components/RefundModal.tsx
export interface RefundModalProps {
  orderId: string;
  totalCents: number;
  onClose: () => void;
  onRefunded: () => void;
}

export default function RefundModal({ orderId, totalCents, onClose, onRefunded }: RefundModalProps) {
  const [amount, setAmount] = useState(totalCents);
  const [reason, setReason] = useState('');
  const [loading, setLoading] = useState(false);
  const { success, error } = useToastStore();
  
  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setLoading(true);
    
    try {
      await paymentService.refund({ orderId, amountCents: amount, reason });
      success('Refund processed successfully');
      onRefunded();
      onClose();
    } catch (err: any) {
      error(err.message || 'Refund failed');
    } finally {
      setLoading(false);
    }
  };
  
  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg p-6 max-w-md w-full">
        <h2 className="text-xl font-bold mb-4">Process Refund</h2>
        <form onSubmit={handleSubmit}>
          <div className="mb-4">
            <label className="block text-sm font-medium mb-2">
              Refund Amount
            </label>
            <input
              type="number"
              value={amount / 100}
              onChange={(e) => setAmount(Math.round(parseFloat(e.target.value) * 100))}
              max={totalCents / 100}
              step="0.01"
              className="w-full px-3 py-2 border rounded"
              required
            />
            <p className="text-xs text-gray-500 mt-1">
              Maximum: {formatCurrency(totalCents)}
            </p>
          </div>
          
          <div className="mb-6">
            <label className="block text-sm font-medium mb-2">
              Reason
            </label>
            <textarea
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              className="w-full px-3 py-2 border rounded"
              rows={3}
              required
            />
          </div>
          
          <div className="flex gap-2 justify-end">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 border rounded hover:bg-gray-50"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading || amount <= 0}
              className="px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700 disabled:opacity-50"
            >
              {loading ? 'Processing...' : `Refund ${formatCurrency(amount)}`}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
```

2. **Add to AdminOrderTimeline**:
```typescript
// AdminOrderTimeline.tsx
import RefundModal from '../components/RefundModal';

const [showRefundModal, setShowRefundModal] = useState(false);

// Add button after order details
{order.paymentStatus === 'CAPTURED' && !order.refundStatus && (
  <button
    onClick={() => setShowRefundModal(true)}
    className="px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700"
  >
    Process Refund
  </button>
)}

{/* Refund status display */}
{order.refundStatus && (
  <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4 mb-4">
    <p className="font-medium text-yellow-800">
      Refund Status: {order.refundStatus}
    </p>
  </div>
)}

{/* Modal */}
{showRefundModal && (
  <RefundModal
    orderId={order.id}
    totalCents={order.totalCents}
    onClose={() => setShowRefundModal(false)}
    onRefunded={() => {
      loadOrder();  // Refresh order details
    }}
  />
)}
```

**Files to Create**:
- `frontend/src/components/RefundModal.tsx`

**Files to Modify**:
- `frontend/src/pages/AdminOrderTimeline.tsx`
- `frontend/src/components/index.ts` (export RefundModal)

**Estimated Time**: 4 hours  
**Testing**: Admin → View order → Click refund → Submit → Verify Stripe refund

---

## Phase 3: High Priority Enhancements - 1 Day

### Task 3.1: S3 File Storage Migration
**Current State**: LocalFileStorageService stores to disk  
**Target**: S3FileStorageService for delivery proofs

**Backend Changes**:

1. **Add S3 dependency** (pom.xml):
```xml
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>s3</artifactId>
    <version>2.20.0</version>
</dependency>
```

2. **S3 configuration**:
```java
// S3Config.java
@Configuration
public class S3Config {
    
    @Value("${aws.s3.bucket-name}")
    private String bucketName;
    
    @Value("${aws.s3.region}")
    private String region;
    
    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
            .region(Region.of(region))
            .build();
    }
}
```

3. **S3FileStorageService implementation**:
```java
@Service
@Primary  // Use this instead of LocalFileStorageService
@RequiredArgsConstructor
public class S3FileStorageService implements FileStorageService {
    
    private final S3Client s3Client;
    
    @Value("${aws.s3.bucket-name}")
    private String bucketName;
    
    @Override
    public String saveFile(MultipartFile file, String directory) throws IOException {
        String key = directory + "/" + UUID.randomUUID() + "-" + file.getOriginalFilename();
        
        PutObjectRequest putRequest = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .contentType(file.getContentType())
            .build();
        
        s3Client.putObject(putRequest, 
            RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        
        return key;  // Return S3 key
    }
    
    @Override
    public byte[] loadFile(String filePath) throws IOException {
        GetObjectRequest getRequest = GetObjectRequest.builder()
            .bucket(bucketName)
            .key(filePath)
            .build();
        
        ResponseBytes<GetObjectResponse> objectBytes = 
            s3Client.getObjectAsBytes(getRequest);
        
        return objectBytes.asByteArray();
    }
    
    @Override
    public void deleteFile(String filePath) {
        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
            .bucket(bucketName)
            .key(filePath)
            .build();
        
        s3Client.deleteObject(deleteRequest);
    }
}
```

4. **Update application.properties**:
```properties
# AWS S3 Configuration
aws.s3.bucket-name=${AWS_S3_BUCKET_NAME:quickbite-delivery-proofs}
aws.s3.region=${AWS_REGION:us-east-1}

# AWS Credentials (use IAM role in production, env vars for local)
aws.accessKeyId=${AWS_ACCESS_KEY_ID:}
aws.secretAccessKey=${AWS_SECRET_ACCESS_KEY:}
```

**Files to Create**:
- `backend/src/main/java/com/quickbite/config/S3Config.java`
- `backend/src/main/java/com/quickbite/delivery/service/S3FileStorageService.java`

**Files to Modify**:
- `backend/pom.xml`
- `backend/src/main/resources/application.properties`

**Estimated Time**: 8 hours (setup S3 bucket + IAM + coding + testing)  
**Testing**: Upload delivery proof, verify file in S3 bucket, download to view

---

### Task 3.2: Sentry Error Tracking
**Current State**: No error tracking  
**Target**: Sentry integrated for frontend & backend

**Backend Changes**:

1. **Add Sentry dependency** (pom.xml):
```xml
<dependency>
    <groupId>io.sentry</groupId>
    <artifactId>sentry-spring-boot-starter</artifactId>
    <version>6.34.0</version>
</dependency>
```

2. **Configure Sentry** (application.properties):
```properties
sentry.dsn=${SENTRY_DSN:}
sentry.environment=${ENVIRONMENT:local}
sentry.traces-sample-rate=0.1
```

**Frontend Changes**:

1. **Install Sentry**:
```bash
npm install @sentry/react @sentry/tracing
```

2. **Initialize Sentry** (main.tsx):
```typescript
import * as Sentry from "@sentry/react";
import { BrowserTracing } from "@sentry/tracing";

Sentry.init({
  dsn: import.meta.env.VITE_SENTRY_DSN,
  environment: import.meta.env.MODE,
  integrations: [new BrowserTracing()],
  tracesSampleRate: 0.1,
});

ReactDOM.createRoot(document.getElementById('root')!).render(
  <Sentry.ErrorBoundary fallback={<ErrorFallback />}>
    <App />
  </Sentry.ErrorBoundary>
);
```

3. **Environment variables**:
```env
VITE_SENTRY_DSN=https://xxx@yyy.ingest.sentry.io/zzz
```

**Files to Modify**:
- `backend/pom.xml`
- `backend/src/main/resources/application.properties`
- `frontend/package.json`
- `frontend/src/main.tsx`
- `frontend/.env`

**Estimated Time**: 3 hours (setup Sentry project + integration)  
**Testing**: Trigger error, verify in Sentry dashboard

---

## Phase 4: Medium Priority Features - 1 Day

### Task 4.1: Notification Preferences Page
**Current State**: No preferences UI  
**Target**: Settings page with push/email/SMS toggles

**Frontend Changes**:

1. **Create Settings page**:
```typescript
// pages/Settings.tsx
export default function Settings() {
  const [preferences, setPreferences] = useState({
    pushEnabled: true,
    emailOrderUpdates: true,
    emailPromotions: false,
    smsDelivery: true
  });
  
  const handleToggle = async (key: string, value: boolean) => {
    setPreferences(prev => ({ ...prev, [key]: value }));
    // TODO: Save to backend /api/users/me/preferences
  };
  
  return (
    <div className="max-w-2xl mx-auto p-6">
      <h1 className="text-2xl font-bold mb-6">Notification Preferences</h1>
      
      <div className="bg-white rounded-lg shadow p-6 space-y-4">
        <div className="flex justify-between items-center">
          <div>
            <p className="font-medium">Push Notifications</p>
            <p className="text-sm text-gray-500">Receive order updates via push</p>
          </div>
          <input
            type="checkbox"
            checked={preferences.pushEnabled}
            onChange={(e) => handleToggle('pushEnabled', e.target.checked)}
            className="toggle"
          />
        </div>
        
        <div className="flex justify-between items-center">
          <div>
            <p className="font-medium">Email - Order Updates</p>
            <p className="text-sm text-gray-500">Get notified via email when order status changes</p>
          </div>
          <input
            type="checkbox"
            checked={preferences.emailOrderUpdates}
            onChange={(e) => handleToggle('emailOrderUpdates', e.target.checked)}
            className="toggle"
          />
        </div>
        
        <div className="flex justify-between items-center">
          <div>
            <p className="font-medium">Email - Promotions</p>
            <p className="text-sm text-gray-500">Receive promotional offers and discounts</p>
          </div>
          <input
            type="checkbox"
            checked={preferences.emailPromotions}
            onChange={(e) => handleToggle('emailPromotions', e.target.checked)}
            className="toggle"
          />
        </div>
        
        <div className="flex justify-between items-center">
          <div>
            <p className="font-medium">SMS - Delivery Alerts</p>
            <p className="text-sm text-gray-500">Get SMS when driver is nearby</p>
          </div>
          <input
            type="checkbox"
            checked={preferences.smsDelivery}
            onChange={(e) => handleToggle('smsDelivery', e.target.checked)}
            className="toggle"
          />
        </div>
      </div>
    </div>
  );
}
```

2. **Add route**:
```typescript
// App.tsx
<Route path="/settings" element={<ProtectedRoute><Settings /></ProtectedRoute>} />
```

**Files to Create**:
- `frontend/src/pages/Settings.tsx`

**Files to Modify**:
- `frontend/src/App.tsx`
- `frontend/src/components/Header.tsx` (add link to settings)

**Estimated Time**: 4 hours  
**Testing**: Navigate to /settings, toggle preferences, reload page

---

## Testing & Validation Plan

### After Phase 1 (Security):
- [ ] Verify refresh token in cookie (not localStorage)
- [ ] Test account lockout (5 failed attempts)
- [ ] Verify security headers (curl -I)
- [ ] Test Content-Type rejection
- [ ] Run OWASP ZAP baseline scan

### After Phase 2 (Core Features):
- [ ] Send password reset email, verify HTML rendering
- [ ] Complete order, verify push permission prompt
- [ ] Test push notification on Chrome desktop
- [ ] Admin refund workflow (full cycle)

### After Phase 3 (High Priority):
- [ ] Upload delivery proof, verify file in S3
- [ ] Trigger error, verify in Sentry dashboard

### After Phase 4 (Medium Priority):
- [ ] Toggle notification preferences, verify persistence

---

## Deployment Checklist

### Before Production:
- [ ] All Phase 1-4 tasks complete
- [ ] Set environment variables:
  - `AWS_S3_BUCKET_NAME`
  - `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` (or IAM role)
  - `SENTRY_DSN` (backend & frontend)
  - `VITE_VAPID_PUBLIC_KEY` / `VAPID_PRIVATE_KEY`
  - `SENDGRID_API_KEY`
  - `TWILIO_SID` / `TWILIO_AUTH_TOKEN`
- [ ] Run full test suite (unit + integration)
- [ ] Run Newman collection (50+ requests)
- [ ] Run Cypress E2E tests
- [ ] Load test with k6 (100 concurrent users)
- [ ] Manual testing checklist (all user roles)
- [ ] Security scan (OWASP ZAP)
- [ ] Accessibility audit (axe-core)
- [ ] Performance audit (Lighthouse)

---

## Summary

**Recommended Implementation Order**:
1. **Week 1**: Phase 1 (Security Hardening) - 2 days
2. **Week 2**: Phase 2 (Email Templates, Service Worker, Refund UI) - 1 day
3. **Week 3**: Phase 3 (S3, Sentry) - 1 day
4. **Week 4**: Phase 4 (Preferences Page) + Testing - 1 day
5. **Week 5-6**: Performance optimization, staging deployment, final testing
6. **Week 7**: Production deployment

**Total Estimated Effort**: ~5 weeks for critical gaps + testing + deployment

**Next Steps**:
1. Create JIRA tickets for each task
2. Assign priorities (P0, P1, P2)
3. Begin Phase 1 immediately (security is critical)
4. Set up S3 bucket and Sentry projects in parallel
5. Schedule code reviews for security changes
