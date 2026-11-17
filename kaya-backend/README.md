# Kaya Backend

Kaya 钱包后端骨架，与前端项目 `../app` 分开存放。

技术栈：

- JDK 17（编译目标 `release=17`）
- Spring MVC 6.1（传统 Servlet 项目，**不使用 Spring Boot**）
- MyBatis 3.5 + mybatis-spring 3.0
- PageHelper 5.3
- HikariCP + MySQL Connector/J 8.4
- jetty-maven-plugin 11（开发服务器）
- Maven `war` 打包

## 环境要求

- JDK 17+
- Maven 3.9+
- MySQL 8，数据库 `kaya` 已按 `../app/db/schema.sql`、`../app/db/seed.sql` 初始化

本机当前 Maven 运行在 JDK 21 上也可以，`pom.xml` 通过 `maven.compiler.release=17` 输出 Java 17 字节码。IntelliJ IDEA 打开本目录后，把 Project SDK 设为 JDK 17 即可。

## 运行

```bash
# 在 kaya-backend 目录
mvn clean package
mvn jetty:run
```

启动后：

- 服务地址：`http://localhost:8080/api`
- 健康检查：`http://localhost:8080/api/health`
- 分类分页：`http://localhost:8080/api/categories?type=expense&page=1&pageSize=10`

`mvn jetty:run` 使用 context path `/api`，与前端 Vite 代理保持一致：

```
前端 http://localhost:5173/api/health
  -> Vite proxy
后端 http://localhost:8080/api/health
```

前端 `vite.config.ts` 中的 `/api` 代理没有 rewrite，因此后端不要去掉 `/api` 前缀。

## 数据库

`src/main/resources/application.properties`：

```properties
jdbc.url=jdbc:mysql://127.0.0.1:3306/kaya?...
jdbc.username=root
jdbc.password=root
```

当前为本地开发配置。生产环境建议通过外部配置或 `-Djdbc.password=...` 覆盖，不要把真实密码提交到仓库。

## 认证与安全

- Spring Security 6 + JWT（HS256），无状态会话，`/auth/**`、`/health`、`/categories/**` 公开，其余接口需要 `Authorization: Bearer <accessToken>`。
- 密码使用 BCrypt（strength 12）存储。
- 刷新令牌为随机串，数据库只保存 SHA-256 哈希；每次刷新都会吊销旧会话并签发新令牌（rotation）。
- 登录失败统一返回 `40101`，账号禁用/锁定返回 `40301`，请求参数校验失败返回 `40001`。

`application.properties` 中的 JWT 配置：

```properties
jwt.secret=kaya-dev-only-jwt-secret-change-me-0123456789
jwt.issuer=kaya
jwt.access-token-ttl-seconds=3600
jwt.refresh-token-ttl-days=30
```

`jwt.secret` 至少 32 字节，上面的值**仅限本地开发**；部署时必须通过外部配置或 `-Djwt.secret=...` 覆盖。

## 已实现接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `GET` | `/health` | 服务与数据库连通性 |
| `GET` | `/categories` | 分类分页，支持 `type=income/expense`、`page`、`pageSize` |
| `GET` | `/categories/{id}` | 分类详情，不存在返回 404 |
| `POST` | `/auth/register` | 注册，返回 201 与 `AuthTokens` |
| `POST` | `/auth/login` | 登录，返回 `AuthTokens` |
| `POST` | `/auth/refresh` | 用刷新令牌换新令牌（旧刷新令牌立即失效） |
| `POST` | `/auth/logout` | 退出登录，可带 `refreshToken` |
| `POST` | `/auth/forgot-password` | 发送重置密码链接；无论邮箱是否存在都返回 200。未接邮件服务，链接写入应用日志 |
| `POST` | `/auth/reset-password` | 用一次性令牌重置密码，令牌过期/已用返回 410，重置后撤销全部会话 |
| `GET` | `/users/me` | 当前登录用户，需要 Access Token |
| `PATCH` | `/users/me` | 修改昵称、用户名、性别、生日；邮箱暂不支持直接修改 |
| `PUT` | `/users/me/password` | 修改密码，保留当前会话并撤销其他会话的刷新令牌 |
| `POST` | `/users/me/avatar` | 上传头像（multipart `file`，JPG/PNG/WebP，≤5MB），返回 `avatarUrl` |
| `GET` | `/users/me/settings` | 读取偏好设置，首次访问自动创建默认记录 |
| `PATCH` | `/users/me/settings` | 修改语言、时区、默认币种与通知开关 |
| `GET` | `/users/me/verifications` | 认证记录列表，证件号只返回后 4 位 |
| `POST` | `/users/me/verifications` | 提交认证（identity/address/phone/email），同类型重复提交返回 409 |
| `GET` | `/files/avatars/{filename}` | 公共头像文件，禁止路径穿越 |
| `GET` | `/wallets` | 当前用户钱包列表 |
| `GET` | `/wallets/{walletId}/summary` | 指定月份（`month=YYYY-MM`，默认当月）收入/支出汇总 |
| `GET` | `/wallets/{walletId}/transactions` | 钱包流水，支持 `type`、`direction`、`month`、`q`、分页 |
| `GET` | `/cards` | 银行卡列表，支持 `walletId`、`status` |
| `POST` | `/cards` | 绑定银行卡，返回 201；重复 token 返回 409 |
| `DELETE` | `/cards/{cardId}` | 解绑银行卡（软删除） |
| `GET` | `/transactions` | 交易列表，支持 `walletId`、`type`、`direction`、`categoryId`、`from`、`to`、`q`、分页 |
| `POST` | `/transactions` | 记账，需 `Idempotency-Key`，返回 201；余额不足 40003 |
| `GET` | `/transactions/summary` | 月度收支汇总与分类占比，支持 `month` |
| `GET` | `/transactions/{id}` | 交易详情 |
| `GET` | `/transactions/{id}/receipt` | 电子回单 |
| `GET` | `/transactions/{id}/receipt/download` | 下载 PDF 回单 |
| `POST` | `/transfers/quote` | 转账报价（金额、手续费 5%、可用余额、是否足够） |
| `POST` | `/transfers` | 发起转账，需 `Idempotency-Key`，返回 201；重复请求返回同一笔 |
| `GET` | `/transfers/{transferId}` | 转账详情（含回单） |
| `GET` | `/recipients` | 收款人分页，支持 `search`、`recent=true` |
| `POST` | `/recipients` | 新增收款人，返回 201；账号重复返回 409 |
| `PATCH` | `/recipients/{recipientId}` | 修改收款人 |
| `DELETE` | `/recipients/{recipientId}` | 删除收款人（软删除） |
| `GET` | `/addresses` | 地址列表，默认地址置顶 |
| `POST` | `/addresses` | 新增地址，返回 201；`isDefault` 会清除其他默认 |
| `PATCH` | `/addresses/{addressId}` | 修改地址 |
| `DELETE` | `/addresses/{addressId}` | 删除地址（软删除） |
| `GET` | `/notifications` | 通知分页，支持 `isRead` |
| `GET` | `/notifications/unread-count` | 未读数量 |
| `PATCH` | `/notifications/{notificationId}/read` | 标记单条已读 |
| `POST` | `/notifications/read-all` | 全部标记已读，返回 `updatedCount` |
| `GET` | `/budgets` | 月度预算列表，`spent/usedPercent/status` 由成功支出实时聚合 |
| `POST` | `/budgets` | 新增预算，返回 201；同分类同月重复返回 409 |
| `PATCH` | `/budgets/{budgetId}` | 调整预算金额或提醒阈值 |
| `DELETE` | `/budgets/{budgetId}` | 删除预算 |

响应结构与前端 `src/utils/request.ts` 对齐：

```json
{
  "code": 0,
  "message": "ok",
  "requestId": "…",
  "data": { "items": [], "page": 1, "pageSize": 10, "total": 12, "totalPages": 2 }
}
```

错误响应 `code != 0`，HTTP 状态根据错误码映射（400/401/403/404/409/410/413/415/422/500），响应头与 body 都带 `X-Request-Id`。

## 目录

```
src/main/java/com/kaya/
  config/       AppConfig、WebConfig、WebAppInitializer、SecurityConfig
  common/       ApiResponse、PageResult、异常处理、RequestIdFilter
  controller/   HealthController、CategoryController、AuthController、UserController
  service/      AuthService、CategoryService 及实现（PageHelper 分页）
  security/     JwtService、JwtAuthenticationFilter、AuthenticatedUser
  dto/          auth / user 请求与响应对象
  mapper/       MyBatis Mapper 接口
  entity/       User、UserSession、Category 等数据库实体
src/main/resources/
  application.properties
  logback.xml
  mapper/*.xml
src/main/webapp/WEB-INF/web.xml   # 空 web.xml，Servlet 由 WebAppInitializer 注册
```

新增业务时按 `controller -> service -> mapper -> XML` 分层；SQL 放 `resources/mapper/*.xml`，不要写进 Controller。分页统一用 `PageHelper.startPage` + `PageInfo`，返回 `PageResult`。

## 打包部署

```bash
mvn clean package
# target/api.war
```

把 `api.war` 放入 Tomcat 10.1+ 的 `webapps`，部署上下文为 `/api`。Tomcat 10.0 及以下（javax.servlet）不兼容。
