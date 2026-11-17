import http from 'k6/http';
import { check, sleep } from 'k6';

// 登录 + 查询主链路，用于观察认证、数据库与连接池在并发下的表现
// 用法：k6 run -e EMAIL=demo@kaya.app -e PASSWORD='******' perf/login-flow.js
//       k6 run -e BASE_URL=https://api.example.com/api -e EMAIL=... -e PASSWORD=... perf/login-flow.js
export const options = {
  scenarios: {
    login_flow: {
      executor: 'ramping-vus',
      startVUs: 1,
      stages: [
        { duration: '20s', target: 10 },
        { duration: '30s', target: 10 },
        { duration: '10s', target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<800'],
    checks: ['rate>0.99'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api';
const EMAIL = __ENV.EMAIL || 'demo@kaya.app';
const PASSWORD = __ENV.PASSWORD || 'change-me';

export default function () {
  const login = http.post(
    `${BASE_URL}/auth/login`,
    JSON.stringify({ email: EMAIL, password: PASSWORD }),
    { headers: { 'Content-Type': 'application/json' } }
  );
  check(login, {
    '登录成功': (r) => r.status === 200 && r.json('code') === 0,
  });

  const accessToken = login.json('data.accessToken');
  if (!accessToken) {
    sleep(1);
    return;
  }

  const params = { headers: { Authorization: `Bearer ${accessToken}` } };

  const wallets = http.get(`${BASE_URL}/wallets`, params);
  check(wallets, { '钱包列表 200': (r) => r.status === 200 });

  const transactions = http.get(`${BASE_URL}/transactions?page=1&pageSize=10`, params);
  check(transactions, { '交易列表 200': (r) => r.status === 200 });

  sleep(1);
}
