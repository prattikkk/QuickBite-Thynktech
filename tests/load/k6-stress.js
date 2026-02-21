/**
 * k6 Stress Test — QuickBite API
 * Phase 4 — Performance: breaking point discovery
 *
 * Run: k6 run tests/load/k6-stress.js
 */
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 50 },
    { duration: '1m',  target: 200 },
    { duration: '1m',  target: 500 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.15'],   // Allow higher error rate at peak
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export function setup() {
  const loginRes = http.post(`${BASE_URL}/api/auth/login`, JSON.stringify({
    email: 'alice@quickbite.test',
    password: 'Test@1234',
  }), { headers: { 'Content-Type': 'application/json' } });

  const body = JSON.parse(loginRes.body);
  return { token: body.data ? body.data.accessToken : body.accessToken };
}

export default function (data) {
  const headers = {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${data.token}`,
  };

  // Health check — lightest endpoint
  const healthRes = http.get(`${BASE_URL}/api/health`);
  check(healthRes, { 'health OK': (r) => r.status === 200 });

  // Vendor list
  const vendorsRes = http.get(`${BASE_URL}/api/vendors?page=0&size=10`, { headers });
  check(vendorsRes, { 'vendors OK': (r) => r.status === 200 });

  sleep(0.2);
}
