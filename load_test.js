import http from 'k6/http';
import { check } from 'k6';

export const options = {
  scenarios: {
    flash_sale_spike: {
      executor: 'per-vu-iterations',
      vus: 500,
      iterations: 10,
      maxDuration: '30s',
    },
  },
};

export default function () {
  const userId = Math.floor(Math.random() * 100000) + 1;
  const url = `http://localhost:8080/api/v1/flashsale/reserve?userId=${userId}&productId=1&quantity=1`;

  const res = http.post(url);

  check(res, {
    'Status is 200 or 409': (r) => r.status === 200 || r.status === 409,
  });
}