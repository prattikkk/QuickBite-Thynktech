/**
 * k6 Smoke Test — QuickBite API
 * Phase 4 — Performance: NFR-1 load test acceptance criteria
 *
 * Run: k6 run tests/load/k6-smoke.js
 * Prerequisites: Backend running on localhost:8080, seed data loaded
 */
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 5,
  duration: '1m',
  thresholds: {
    http_req_duration: ['p(95)<500'],  // 95th percentile < 500ms
    http_req_failed: ['rate<0.05'],     // Error rate < 5%
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

let token = null;

export function setup() {
  // Login as customer
  const loginRes = http.post(`${BASE_URL}/api/auth/login`, JSON.stringify({
    email: 'alice@quickbite.test',
    password: 'Test@1234',
  }), { headers: { 'Content-Type': 'application/json' } });

  check(loginRes, { 'login success': (r) => r.status === 200 });

  const body = JSON.parse(loginRes.body);
  return { token: body.data ? body.data.accessToken : body.accessToken };
}

export default function (data) {
  const headers = {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${data.token}`,
  };

  // 1. List vendors
  const vendorsRes = http.get(`${BASE_URL}/api/vendors?page=0&size=10`, { headers });
  check(vendorsRes, {
    'vendors 200': (r) => r.status === 200,
    'vendors < 500ms': (r) => r.timings.duration < 500,
  });

  sleep(0.5);

  // 2. Get first vendor's menu
  const vendors = JSON.parse(vendorsRes.body);
  const vendorList = vendors.data?.content || [];
  if (vendorList.length > 0) {
    const vendorId = vendorList[0].id;
    const menuRes = http.get(`${BASE_URL}/api/vendors/${vendorId}/menu`, { headers });
    check(menuRes, {
      'menu 200': (r) => r.status === 200,
      'menu < 300ms': (r) => r.timings.duration < 300,
    });
  }

  sleep(0.5);

  // 3. Get my orders
  const ordersRes = http.get(`${BASE_URL}/api/orders/my?page=0&size=5`, { headers });
  check(ordersRes, {
    'orders 200': (r) => r.status === 200,
  });

  sleep(0.5);

  // 4. Get notifications
  const notifRes = http.get(`${BASE_URL}/api/notifications?page=0&size=5`, { headers });
  check(notifRes, {
    'notifications 200': (r) => r.status === 200,
  });

  sleep(1);
}
