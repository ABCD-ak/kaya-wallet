# 部署手册 · Oracle Cloud 永久免费 VM

目标：后端 + MySQL + Redis 跑在一台 Oracle Cloud Always Free（ARM）VM 上，Caddy 自动 HTTPS；前端部署到 Vercel。全部 0 元。

## 1. 创建免费 VM

1. 注册 Oracle Cloud，进入 **Compute → Instances → Create instance**
2. Image 选 **Ubuntu 22.04**，Shape 选 **Ampere A1（VM.Standard.A1.Flex）**，2 OCPU / 12 GB 足够
3. 下载并保存 SSH 私钥
4. 记下公网 IP

> Oracle 注册需要信用卡做身份验证（不扣费）；若注册失败，可换时间段重试，或退回 Render 免费容器方案。

## 2. 放行 80 / 443

两层防火墙都要开：

1. **VCN Security List**：添加入站规则 `0.0.0.0/0` → TCP `80`、`443`
2. **VM 内 iptables**（Oracle Ubuntu 默认会拦截）：

```bash
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 80 -j ACCEPT
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 443 -j ACCEPT
sudo netfilter-persistent save
```

## 3. 安装 Docker

```bash
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER
newgrp docker
docker compose version
```

## 4. 拉取代码并配置

```bash
git clone https://github.com/<your-name>/kaya-wallet.git
cd kaya-wallet
cp deploy/.env.example deploy/.env
nano deploy/.env
```

`deploy/.env` 至少填写：

- `MYSQL_ROOT_PASSWORD`：强密码
- `JWT_SECRET` / `SECURITY_FIELD_ENCRYPTION_KEY`：`openssl rand -base64 48`
- `MAIL_*`：QQ 邮箱授权码（不填则验证码邮件不可用）
- `APP_RESET_PASSWORD_URL`：Vercel 前端地址
- `SITE_ADDRESS`：
  - 有域名：`api.example.com`
  - 没域名：`<公网IP>.sslip.io`（免费解析到该 IP，自动 HTTPS）
  - 只走 HTTP：`:80`

## 5. 启动

```bash
docker compose --env-file deploy/.env up -d --build
docker compose --env-file deploy/.env ps
curl http://127.0.0.1/api/health
```

首次启动会自动执行 `app/db/schema.sql` 和 `seed.sql`。若改了 SQL 需要重建库：

```bash
docker compose --env-file deploy/.env down -v   # 会删除数据卷，谨慎
docker compose --env-file deploy/.env up -d --build
```

查看日志：

```bash
docker compose --env-file deploy/.env logs -f api
docker compose --env-file deploy/.env logs -f caddy
```

## 6. 前端部署到 Vercel

1. Vercel → **New Project** → 导入同一个 GitHub 仓库
2. Root Directory 选 `app`，Framework 选 Vite（Build `npm run build`，Output `dist`）
3. 环境变量：
   - `VITE_API_BASE_URL = https://<后端域名>/api`
4. Deploy 后访问 `https://<项目>.vercel.app/swagger.html` 验证接口文档

> `.env.production` 已被 git 忽略，生产地址只放 Vercel 环境变量，避免把临时隧道地址写进仓库。

## 7. 上线验收清单

- [ ] `https://<后端域名>/api/health` 返回数据库连通
- [ ] `https://<前端域名>/swagger.html` 能打开并能调通 `/health`
- [ ] 注册 → 登录 → 记一笔 → 转账 → 下载 PDF 回单
- [ ] 退出登录后访问 `/wallet` 自动跳转 `/login`
- [ ] 刷新令牌：等待 access token 过期后操作，前端自动续期且不跳登录
- [ ] 准备一个演示账号，面试时可直接登录（不要提交密码到仓库）

## 8. 常见问题

| 现象 | 原因 | 处理 |
| --- | --- | --- |
| Caddy 申请证书失败 | 80 端口未放行 / 域名未解析 | 检查 VCN + iptables + DNS |
| 502 Bad Gateway | api 容器未起来 | `docker compose logs api`，多为数据库密码或 SQL 初始化失败 |
| 前端请求 CORS 报错 | 后端未允许 Vercel 域名 | `APP_CORS_ALLOWED_ORIGINS` 加 `https://*.vercel.app` 后重启 api |
| 验证码邮件发不出 | QQ 邮箱未开 SMTP / 授权码错误 | 用 16 位授权码，端口 465 + SSL |
| ARM 构建慢 | Maven 依赖首次下载 | 首次 `--build` 耐心等待，之后走缓存 |
