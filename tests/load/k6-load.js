/**
 * k6 Load Test — QuickBite API
 * Phase 4 — Performance: NFR-1 100 concurrent users
 *
 * Run: k6 run tests/load/k6-load.js
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const errorRate = new Rate('errors');
const vendorLatency = new Trend('vendor_list_duration');
const menuLatency = new Trend('menu_fetch_duration');

export const options = {
  stages: [
    { duration: '1m', target: 50 },    // Ramp up to 50 users
    { duration: '3m', target: 100 },   // Hold at 100 users
    { duration: '1m', target: 0 },     // Ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<500', 'p(99)<1000'],
    http_req_failed: ['rate<0.05'],
    errors: ['rate<0.1'],
    vendor_list_duration: ['p(95)<300'],
    menu_fetch_duration: ['p(95)<200'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export function setup() {
  const loginRes = http.post(`${BASE_URL}/api/auth/login`, JSON.stringify({
    email: 'alice@quickbite.test',
    password: 'Test@1234',
  }), { headers: { 'Content-Type': 'application/json' } });

  if (loginRes.status !== 200) {
    throw new Error(`Login failed: ${loginRes.status} ${loginRes.body}`);
  }

  const body = JSON.parse(loginRes.body);
  return { token: body.data ? body.data.accessToken : body.accessToken };
}

export default function (data) {
  const headers = {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${data.token}`,
  };

  // Vendor list
  const vendorsRes = http.get(`${BASE_URL}/api/vendors?page=0&size=20`, { headers });
  vendorLatency.add(vendorsRes.timings.duration);
  const vendorOk = check(vendorsRes, { 'vendors OK': (r) => r.status === 200 });
  errorRate.add(!vendorOk);

  sleep(0.3);

  // Menu fetch (random vendor)
  const vendors = JSON.parse(vendorsRes.body);
  const vendorList = vendors.data?.content || [];
  if (vendorList.length > 0) {
    const vendor = vendorList[Math.floor(Math.random() * vendorList.length)];
    const menuRes = http.get(`${BASE_URL}/api/vendors/${vendor.id}/menu`, { headers });
    menuLatency.add(menuRes.timings.duration);
    const menuOk = check(menuRes, { 'menu OK': (r) => r.status === 200 });
    errorRate.add(!menuOk);
  }

  sleep(0.3);

  // Search vendors
  const searchRes = http.get(`${BASE_URL}/api/vendors/search?query=burger&page=0&size=10`, { headers });
  check(searchRes, { 'search OK': (r) => r.status === 200 });

  sleep(0.3);

  // Favorites
  const favRes = http.get(`${BASE_URL}/api/favorites`, { headers });
  check(favRes, { 'favorites OK': (r) => r.status === 200 });

  sleep(0.5);
}
