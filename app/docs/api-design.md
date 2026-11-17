# Kaya Wallet API 接口设计

> 依据即时设计 23 个画板与 `db/schema.sql` 设计。OpenAPI 源文件：`docs/openapi.json`。  
> 已导入 Apifox 项目 **app**（team 4848416 / project 9039289）：**46 个接口**、**44 个数据模型**、11 个分组。

## 1. 通用约定

- Base URL：`https://api.kaya.app/api/v1`，本地 `http://localhost:3000/api/v1`
- 认证：除注册、登录、刷新令牌、找回密码外，均需 `Authorization: Bearer <accessToken>`
- 统一响应：`{ code, message, requestId, data }`，`code = 0` 表示成功
- 金额用两位小数字符串（如 `"150.00"`），ID 用数字字符串，避免 JS 精度丢失
- 时间用 ISO 8601 UTC；分页返回 `{ items, page, pageSize, total, totalPages }`
- 资金变动接口必须带 `Idempotency-Key`；同键重放返回首次结果
- 余额、月度收支、预算使用率、回执均为流水派生数据，不单独存储

## 2. 接口清单

| 模块 | 方法 | 路径 | 说明 |
| --- | --- | --- | --- |
| Auth 认证 | `POST` | `/auth/forgot-password` | 发送重置密码邮件 |
| Auth 认证 | `POST` | `/auth/login` | 登录 |
| Auth 认证 | `POST` | `/auth/logout` | 退出登录 |
| Auth 认证 | `POST` | `/auth/refresh` | 刷新令牌 |
| Auth 认证 | `POST` | `/auth/register` | 注册 |
| Auth 认证 | `POST` | `/auth/reset-password` | 重置密码 |
| Users 用户 | `GET` | `/users/me` | 获取当前用户 |
| Users 用户 | `PATCH` | `/users/me` | 编辑个人资料 |
| Users 用户 | `POST` | `/users/me/avatar` | 上传头像 |
| Users 用户 | `PUT` | `/users/me/password` | 修改密码 |
| Users 用户 | `GET` | `/users/me/settings` | 获取偏好设置 |
| Users 用户 | `PATCH` | `/users/me/settings` | 更新偏好设置 |
| Users 用户 | `GET` | `/users/me/verifications` | 获取认证状态 |
| Users 用户 | `POST` | `/users/me/verifications` | 提交认证 |
| Wallets 钱包 | `GET` | `/wallets` | 我的钱包列表 |
| Wallets 钱包 | `GET` | `/wallets/{walletId}/summary` | 钱包月度汇总 |
| Wallets 钱包 | `GET` | `/wallets/{walletId}/transactions` | 钱包首页交易列表 |
| Cards 银行卡 | `GET` | `/cards` | 我的银行卡 |
| Cards 银行卡 | `POST` | `/cards` | 绑定银行卡 |
| Cards 银行卡 | `DELETE` | `/cards/{cardId}` | 解绑银行卡 |
| Transactions 交易 | `GET` | `/transactions` | 交易列表 |
| Transactions 交易 | `POST` | `/transactions` | 记一笔收入/支出 |
| Transactions 交易 | `GET` | `/transactions/{transactionId}` | 交易详情 |
| Transactions 交易 | `GET` | `/transactions/{transactionId}/receipt` | 获取回执 |
| Transactions 交易 | `GET` | `/transactions/{transactionId}/receipt/download` | 下载回执 PDF |
| Transactions 交易 | `GET` | `/transactions/summary` | 月度交易汇总 |
| Transfers 转账 | `POST` | `/transfers` | 发起转账 |
| Transfers 转账 | `GET` | `/transfers/{transferId}` | 转账详情 |
| Transfers 转账 | `POST` | `/transfers/quote` | 转账试算 |
| Recipients 联系人 | `GET` | `/recipients` | 联系人列表 |
| Recipients 联系人 | `POST` | `/recipients` | 新增联系人 |
| Recipients 联系人 | `DELETE` | `/recipients/{recipientId}` | 删除联系人 |
| Recipients 联系人 | `PATCH` | `/recipients/{recipientId}` | 编辑联系人 |
| Categories 分类 | `GET` | `/categories` | 收支分类 |
| Budgets 预算 | `GET` | `/budgets` | 月度预算列表 |
| Budgets 预算 | `POST` | `/budgets` | 新增预算 |
| Budgets 预算 | `DELETE` | `/budgets/{budgetId}` | 删除预算 |
| Budgets 预算 | `PATCH` | `/budgets/{budgetId}` | 调整预算 |
| Notifications 通知 | `GET` | `/notifications` | 通知列表 |
| Notifications 通知 | `PATCH` | `/notifications/{notificationId}/read` | 标记单条已读 |
| Notifications 通知 | `POST` | `/notifications/read-all` | 全部标记已读 |
| Notifications 通知 | `GET` | `/notifications/unread-count` | 未读通知数 |
| Addresses 地址 | `GET` | `/addresses` | 地址列表 |
| Addresses 地址 | `POST` | `/addresses` | 新增地址 |
| Addresses 地址 | `DELETE` | `/addresses/{addressId}` | 删除地址 |
| Addresses 地址 | `PATCH` | `/addresses/{addressId}` | 编辑地址 |

## 3. 核心流程

### 3.1 注册 / 登录
1. `POST /auth/register`：同一事务创建 `users`、`user_settings`、默认 USD `wallets`，直接返回令牌。
2. `POST /auth/login`：返回 accessToken + refreshToken；密码错误 401，账号锁定/禁用 403。
3. `POST /auth/refresh`：刷新令牌轮换，旧 refreshToken 立即失效。
4. `POST /auth/forgot-password` → `POST /auth/reset-password`：邮件一次性令牌。

### 3.2 钱包首页
1. `GET /wallets` 取钱包与缓存余额。
2. `GET /wallets/{walletId}/summary?month=` 取 Income / Spent。
3. `GET /wallets/{walletId}/transactions?direction=` 取首页流水（All / Received / Send）。
4. `GET /cards` 取信用卡列表。

### 3.3 记一笔收入 / 支出
`POST /transactions` + `Idempotency-Key`：写 `transactions` 流水，同事务更新 `wallets.balance`；金额始终为正，方向由 `type` 推导。

### 3.4 转账（5% 手续费）
1. `POST /transfers/quote`：返回本金、手续费、合计扣款、余额是否充足。
2. `POST /transfers` + `Idempotency-Key`：写 `transfer_out` 流水，扣本金 + 手续费，更新 `recipients.last_used_at`，生成 `notifications`。
3. `GET /transfers/{transferId}` / `GET /transactions/{transactionId}/receipt`：回执由流水 + 对手快照实时生成。
4. `GET /transactions/{transactionId}/receipt/download`：下载 PDF。

### 3.5 预算与月度统计
- `GET /budgets?month=`：`spent`、`usedPercent`、`status` 从成功支出流水实时聚合。
- `GET /transactions/summary?month=`：Total Transactions 与分类占比。

### 3.6 通知
- `GET /notifications` 按时间倒序，客户端分 Recent / Older。
- `GET /notifications/unread-count` 红点；`PATCH /notifications/{id}/read`、`POST /notifications/read-all`。

### 3.7 账户
- `GET/PATCH /users/me`：资料；`POST /users/me/avatar`：头像。
- `PUT /users/me/password`：对应安全提示 `Set password`。
- `GET/PATCH /users/me/settings`：语言、时区、默认币种、通知开关。
- `GET/POST /users/me/verifications`：实名/地址/手机/邮箱认证。
- `/addresses`：地址增删改查；`/cards`：银行卡。

## 4. 与数据库的映射

| 接口资源 | 数据表 |
| --- | --- |
| 用户 / 偏好 / 地址 / 认证 | `users`、`user_settings`、`user_addresses`、`user_verifications` |
| 钱包 | `wallets` |
| 银行卡 | `wallet_cards` |
| 分类 / 预算 | `categories`、`budgets` |
| 联系人 | `recipients` |
| 交易 / 回执 | `transactions` |
| 通知 | `notifications` |
| 登录会话 / 重置令牌 | `user_sessions`、`password_reset_tokens`（服务端内部，不暴露） |

## 5. Apifox 导入

- 项目：**app**（projectId `9039289`，主分支 `8851080`）
- 源文件：`docs/openapi.json`（OpenAPI 3.0.3）
- 导入方式：Apifox「导入数据」→ OpenAPI/Swagger → 上传 `docs/openapi.json`；或使用已配置的 `apifox-new-mcp` 直接调用。
- 重复导入选择 `AUTO_MERGE`，按 method + path 合并，不会重复创建。

## 6. 待确认

- 生产域名、支付网关、短信/邮件服务商
- 5% 手续费是否所有转账固定、是否设最低/最高手续费
- 单笔/单日限额、风控规则
- 多币种与汇率来源（当前默认 USD）
