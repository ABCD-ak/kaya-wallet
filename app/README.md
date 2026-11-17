# Kaya 移动端 Web（Vue 3 + Vite + TS + Element Plus）

按即时设计稿还原的 Kaya 钱包移动端项目。当前已完成项目骨架、axios 请求封装、组件化引导页、真实登录/注册闭环；其余画板以原型方式渲染，便于对照设计稿继续开发。

## 命令

```bash
npm install        # 安装依赖
npm run dev        # 开发（自动打开浏览器）
npm run build      # 生产构建
npm run type-check # 类型检查
```

## 路由

| 路径 | 说明 |
| --- | --- |
| `/` | 自动跳转引导页 |
| `/onboarding` | 组件化引导页（正式页面） |
| `/auth` | 登录/注册入口（沉浸式背景） |
| `/login` | 登录（已接后端） |
| `/register` | 注册（已接后端） |
| `/forgot-password` | 忘记密码（发送重置链接） |
| `/reset-password` | 用重置令牌设置新密码 |
| `/wallet` | 登录后的账户页（需要登录） |
| `/p/:slug` | 设计稿原型画板（如 `/p/login-or-register`、`/p/wallet`） |

引导页支持横向滑动、轻触翻页、点击进度条跳转；跳过或末页点击后进入 `/auth`。

## 登录闭环

- `src/stores/auth.ts`：Pinia 管理 access token、refresh token、当前用户
- `src/api/auth.ts` / `src/api/users.ts`：注册、登录、刷新、退出、当前用户
- `src/utils/request.ts`：401 时自动用 refresh token 换新令牌并重试原请求，刷新失败才清理登录态
- 路由守卫：`requiresAuth` 未登录跳 `/login`，`guestOnly` 已登录跳 `/wallet`

## 请求封装

`src/utils/request.ts` 基于 axios 封装：

- `baseURL` 读取 `VITE_API_BASE_URL`（开发代理见 `vite.config.ts`）
- 请求自动携带 `Authorization: Bearer <token>`，token 存取见 `src/utils/auth.ts`
- 统一拆解 `{ code, message, data }` 响应结构，业务失败自动 `ElMessage` 提示
- HTTP 401 自动刷新令牌并重试；刷新失败清除登录态并派发 `auth:unauthorized` 事件（`src/main.ts` 监听后回登录页）
- 导出 `request.get/post/put/delete/download`

```ts
import request from '@/utils/request'

const list = await request.get<User[]>('/users', { page: 1 })
const user = await request.post<User>('/users', { name: 'Nick' })
```

## 引导页组件

```
src/views/onboarding/
├── OnboardingPage.vue          # 页面容器：滑动/翻页/跳过
├── slides.ts                   # 引导页数据（文案 + 插画）
└── components/
    ├── OnboardingSlide.vue     # 单屏内容（插画/渐变标题/描述）
    └── ProgressDots.vue        # 进度条（可点击）
src/components/StatusBar.vue    # 状态栏（时间/信号/电量，适配安全区）
```

插画与图标复用 `public/assets/*.png`（设计稿 2x 导出）。

## 数据库设计

- `docs/database-design.md`：原型字段映射、ER 图、设计规范、核心流程与查询
- `db/schema.sql`：MySQL 8.0 建表 DDL（13 张表）
- `db/seed.sql`：基础收支分类种子数据

```bash
mysql -u root -p -e "CREATE DATABASE kaya DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
mysql -u root -p kaya < db/schema.sql
mysql -u root -p kaya < db/seed.sql
```

## 接口设计

- `docs/api-design.md`：接口约定、46 个接口清单、核心流程、与数据库映射
- `docs/openapi.json`：OpenAPI 3.0.3 源文件，可导入 Apifox
- Apifox 项目：`app`（projectId `9039289`），已导入 46 个接口 / 44 个数据模型 / 11 个分组
- Claude Code 已配置 `apifox-new-mcp`（用户级 MCP，令牌不入库）

重复导入时选择 `AUTO_MERGE`，按 method + path 合并，不会重复创建。

## 原型层

- `src/prototype/specs/*.json` 由 `scripts/flatten.mjs` 从设计稿树生成（绝对坐标 + 样式）
- `src/prototype/ScreenRenderer.vue` 按规格逐节点绝对定位渲染
- `src/prototype/screens.ts` 配置点击热区与跳转，`i18n.ts` 为文案中英对照
- 左侧边栏可直达任意画板；手机宽度下隐藏侧栏全屏显示

重新导出设计树后执行：

```bash
node scripts/flatten.mjs <tree.json> src/prototype
```
