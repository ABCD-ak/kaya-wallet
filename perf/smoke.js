import http from 'k6/http';
import { check, sleep } from 'k6';

// 基线冒烟：不写数据，验证部署链路与只读接口延迟
// 用法：k6 run perf/smoke.js
//       k6 run -e BASE_URL=https://api.example.com/api perf/smoke.js
export const options = {
  vus: 10,
  duration: '30s',
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<300'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api';

export default function () {
  const health = http.get(`${BASE_URL}/health`);
  check(health, {
    'health 200': (r) => r.status === 200,
    'health code=0': (r) => r.json('code') === 0,
  });

  const categories = http.get(`${BASE_URL}/categories?type=expense&page=1&pageSize=10`);
  check(categories, {
    'categories 200': (r) => r.status === 200,
    'categories 有数据': (r) => (r.json('data.items') || []).length > 0,
  });

  sleep(1);
}
