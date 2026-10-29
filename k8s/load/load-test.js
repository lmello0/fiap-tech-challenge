import http from 'k6/http';
import { check, sleep } from 'k6';

const APP = __ENV.APP_URL || 'http://workshop:8080';
const KEYCLOAK = __ENV.KEYCLOAK_URL || 'http://keycloak:8080';
const SERVICE_ID = '0198a000-0000-7000-8000-000000000201';
const TOKEN_TTL_MS = 240000;

export const options = {
  scenarios: {
    peak: {
      executor: 'ramping-vus',
      startVUs: 1,
      stages: [
        { duration: '30s', target: 40 },
        { duration: '3m', target: 60 },
        { duration: '30s', target: 0 },
      ],
      gracefulRampDown: '10s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.05'],
  },
};

let token = null;
let tokenAt = 0;

function bearer() {
  if (token === null || Date.now() - tokenAt > TOKEN_TTL_MS) {
    const res = http.post(
      `${KEYCLOAK}/realms/workshop/protocol/openid-connect/token`,
      { grant_type: 'password', client_id: 'workshop-cli', username: 'attendant', password: 'attendant' },
    );
    check(res, { 'token issued': (r) => r.status === 200 });
    token = res.json('access_token');
    tokenAt = Date.now();
  }
  return { headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' } };
}

function randomDigits(n) {
  let s = '';
  for (let i = 0; i < n; i++) s += Math.floor(Math.random() * 10);
  return s;
}

function cpf() {
  const d = randomDigits(9).split('').map(Number);
  const digit = (len, weight) => {
    let sum = 0;
    for (let i = 0; i < len; i++) sum += d[i] * (weight - i);
    const r = (sum * 10) % 11;
    return r === 10 ? 0 : r;
  };
  d.push(digit(9, 10));
  d.push(digit(10, 11));
  return d.join('');
}

function plate() {
  const letter = () => String.fromCharCode(65 + Math.floor(Math.random() * 26));
  return `${letter()}${letter()}${letter()}${randomDigits(1)}${letter()}${randomDigits(2)}`;
}

function openWorkOrder(params) {
  const document = cpf();
  const body = JSON.stringify({
    customer: { document, name: 'Load Test', email: `load-${document}@example.com` },
    vehicle: { plate: plate(), brand: 'Fiat', model: 'Argo', year: 2022 },
    complaint: 'load test',
    items: [{ type: 'SERVICE', referenceId: SERVICE_ID, quantity: 1 }],
  });
  const res = http.post(`${APP}/api/v1/work-orders`, body, params);
  check(res, { 'work order opened': (r) => r.status === 201 });
}

export default function () {
  const params = bearer();

  const queue = http.get(`${APP}/api/v1/work-orders?size=50`, params);
  check(queue, { 'queue listed': (r) => r.status === 200 });

  const parts = http.get(`${APP}/api/v1/parts?size=50`, params);
  check(parts, { 'parts listed': (r) => r.status === 200 });

  const metrics = http.get(`${APP}/api/v1/work-orders/metrics`, params);
  check(metrics, { 'metrics returned': (r) => r.status === 200 });

  if (Math.random() < 0.15) {
    openWorkOrder(params);
  }
  sleep(0.2);
}
