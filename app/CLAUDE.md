# Kaya 移动端 Web — 项目说明

按即时设计稿还原的 Kaya 钱包移动端，Vue 3 + Vite + TypeScript + Element Plus。
375×812 设计基准。

## 核心架构（重要）

页面由**设计稿 JSON 驱动渲染**，不是一个个手写 `.vue` 文件。

```
设计稿 JSON  →  src/prototype/specs/*.json
                    ↓ (import.meta.glob 自动收集)
              src/prototype/screens.ts        // slug → spec 映射、跳转规则 TAPS、排序
                    ↓
              src/prototype/PrototypeScreen.vue  // 路由壳 + 侧边栏 + 缩放
                    ↓
              src/prototype/ScreenRenderer.vue   // 把 SpecNode 数组渲染成 DOM
```

**改一个页面 = 改对应的 `specs/<slug>.json`**。
除非要新增渲染能力，否则不要新建页面组件、不要改 ScreenRenderer。

## 目录

```
src/
  prototype/
    specs/*.json      23 个设计稿画板（account, wallet, login, ...）
    screens.ts        slug 映射 + TAPS 点击跳转规则 + screenOrder
    ScreenRenderer.vue  节点渲染器
    PrototypeScreen.vue 路由壳（侧边栏 + 375×812 缩放）
    types.ts          SpecNode / ScreenSpec / TapRule 类型
    i18n.ts           设计稿英文文案 → 中文展示文案
    assets.json       图片资源（设计稿导出链接）
  router/index.ts     /  →  /onboarding ；/p/:slug → PrototypeScreen
  utils/request.ts    axios 封装（Bearer token、统一错误、401 处理）
  utils/auth.ts       token 存取
  views/              正式业务页面（非原型）
```

## SpecNode 字段速查

坐标是设计稿绝对定位（左上角原点），单位 px。

| 字段 | 含义 |
| --- | --- |
| `t` | `box` / `text` / `img` |
| `x y w h` | 位置尺寸 |
| `ch` `fs` `ff` `fw` `col` | 文本、字号、字体、字重、颜色 |
| `f` `fa` `g` `st` `sh` `r` | 填充、透明度、渐变、描边、阴影、圆角 |
| `src` | 图片地址 |
| `o` `rot` | 整体透明度、旋转 |

## 命令

```bash
npm run dev         # 开发（自动开浏览器，端口 5173）
npm run build       # 生产构建
npm run type-check  # 类型检查
```

## 约定

- 路径别名 `@` → `src`
- Element Plus 组件由 `unplugin-vue-components` 按需自动注册，**不要手写 import**
- 接口地址走 `VITE_API_BASE_URL`（见 `.env.development`），代理在 `vite.config.ts`
- 数据未接后端前一律用 mock，写死在画板 JSON 或页面内，不要新建 api 层
- 中文文案统一放 `i18n.ts`，不要散落在模板里

## 还原设计稿的工作方式

1. 一次只做一个画板，做完停下等验收
2. 先读对应 `specs/<slug>.json` 看现有节点，在其基础上改，不要推倒重写
3. 完成后必须 `npm run build` 通过
4. 视觉验收：起 dev server 用 playwright 截图，报告偏差超过 2px 的位置

## 不要做

- 不要重写路由、不要动 `screenOrder` 顺序（侧边栏导航依赖它）
- 不要遍历 `node_modules`、不要 `ls -R` 整个目录、不要 cat `package-lock.json`
- 不要擅自升级依赖版本
- 不要在没被要求时改动其他画板

## 已知待办

- `/onboarding` 已组件化，其余画板仍是原型渲染，需逐个转成正式页面
- `/p/wallet` 等页面的中文文案未完全覆盖
