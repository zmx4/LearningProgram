# 安装与运行

本文档说明 LearningProgram 的本地开发环境准备、配置和启动方式。

## 环境要求

| 软件 | 版本 |
| --- | --- |
| JDK | 21 或更高版本 |
| Maven | 3.9+，也可以使用项目内的 Maven Wrapper |
| Node.js | 22.18+ 或 24.12+ |
| pnpm | 推荐使用项目当前锁文件对应的 pnpm 版本 |
| MySQL | 8.0+ |
| Redis | 6.0+ |

## 数据库准备

后端默认使用名为 `learning` 的 MySQL 数据库。首次部署时先创建数据库：

```sql
CREATE DATABASE IF NOT EXISTS learning
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

完整的表结构、表间关系和字段说明见 [数据库说明](./DATABASE.md)。初始化表结构有两种方式：

1. **按文档建表**：在空库中执行 [DATABASE.md](./DATABASE.md) 各表定义对应的建表语句（推荐，一次性完成全部 20 张表）。
2. **按迁移脚本升级**：`learning-program-backend/src/main/resources/db/migration/` 中的 V7–V14 脚本覆盖题库、课程、学习进度、资源和积分奖励等表；账号、通知、签到等基础表（V1–V6）未纳入仓库，同样以 [DATABASE.md](./DATABASE.md) 为准手工创建。已有数据库升级时，按序号执行缺失的脚本即可。

注意事项：

- `password` 字段保存 BCrypt 密文，需使用 `VARCHAR(255)`。
- 迁移脚本未启用 Flyway 自动执行，需要手工按序号运行。
- 如果数据库中已存在同名表，请先对照 [DATABASE.md](./DATABASE.md) 确认字段与后端实体一致，避免启动后出现字段映射错误。

## 后端配置

配置文件：

```text
learning-program-backend/src/main/resources/application.yaml
```

MySQL 与 Redis 的连接信息通过环境变量注入，仓库内的 `application.yaml` 不保存真实账号密码。启动后端前需设置以下环境变量：

| 环境变量 | 用途 | 未设置时的默认值 |
| --- | --- | --- |
| `MYSQL_USERNAME` | MySQL 用户名 | 空（连接会失败，必填） |
| `MYSQL_PASSWORD` | MySQL 密码 | 空（连接会失败，必填） |
| `MYSQL_HOST` | MySQL 地址 | `127.0.0.1` |
| `MYSQL_PORT` | MySQL 端口 | `3306` |
| `REDIS_HOST` | Redis 地址 | `127.0.0.1` |
| `REDIS_PORT` | Redis 端口 | `6379` |
| `REDIS_PASSWORD` | Redis 密码 | 空（视为无密码） |

bash / zsh 设置示例：

```bash
export MYSQL_USERNAME=your_mysql_user
export MYSQL_PASSWORD=your_mysql_password
export MYSQL_HOST=127.0.0.1
export REDIS_HOST=127.0.0.1
export REDIS_PASSWORD=your_redis_password
```

PowerShell 设置示例：

```powershell
$env:MYSQL_USERNAME = "your_mysql_user"
$env:MYSQL_PASSWORD = "your_mysql_password"
$env:MYSQL_HOST = "127.0.0.1"
$env:REDIS_HOST = "127.0.0.1"
$env:REDIS_PASSWORD = "your_redis_password"
```

环境变量只在当前终端会话有效，建议写入 IDE 运行配置或系统的环境变量设置中。生产环境应使用部署平台的 Secret、环境变量或外部配置中心注入。

### 使用 .env 文件（推荐）

后端目录提供了 `.env.example` 模板。首次配置时：

```bash
cd learning-program-backend
cp .env.example .env
# 编辑 .env，填入真实的 MySQL / Redis 连接信息
```

`.env` 已被 `.gitignore` 忽略，不会提交到仓库。启动后端时先加载它：

```bash
source .env && mvn spring-boot:run
```

Windows PowerShell 用户可以复制 `.env` 中的值到 `$env:` 设置，或使用 IDEA EnvFile 插件直接引用 `.env` 文件。

JWT 签名密钥（`spring.security.jwt.key`）和有效期目前仍写在 `application.yaml` 中，公开发布前建议一并改为环境变量注入。

请勿把真实密码、JWT 密钥或生产环境地址提交到仓库。

`spring.security.email` 当前设置为 `false` 时，注册流程不会强制校验邮箱验证码。启用邮箱校验前，需要同时配置邮件服务和验证码流程。

## 密码强度策略

所有设置密码的入口（注册、忘记密码重置、修改密码、管理员批量建号）都经过同一条密码规则责任链，
规则与阈值在 `application.yaml` 的 `learning.password.policy` 下配置，调整后无需改代码：

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `min-length` / `max-length` | `8` / `32` | 密码长度区间（含端点） |
| `require-uppercase` / `require-lowercase` | `true` | 是否必须包含大写 / 小写字母 |
| `require-digit` | `true` | 是否必须包含数字 |
| `require-special` | `true` | 是否必须包含特殊字符（非字母、非数字、非空白） |
| `forbid-whitespace` | `true` | 禁止空格等空白字符 |
| `forbid-account-info` | `true` | 禁止密码包含用户名或邮箱 |
| `reject-common-passwords` | `true` | 拒绝常见弱密码（词表见 `PasswordPolicyProperties`，可覆盖） |
| `generated-length` | `12` | 管理员重置密码时生成的随机密码长度 |
| `special-characters` | `!@#$%^&*()-_=+[]{};:,.?/|~` | 生成随机密码时可选的特殊字符 |

当前生效的规则可以通过 `GET /api/auth/password-policy` 查看；前端注册 / 重置 / 改密页面会读取它来展示要求并做即时校验。

临时调整（不改配置文件，仅本次运行有效）：

```powershell
Set-Location .\learning-program-backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--learning.password.policy.min-length=10 --learning.password.policy.require-special=false"
```

## 启动后端

在项目根目录执行：

```powershell
Set-Location .\learning-program-backend
.\mvnw.cmd spring-boot:run
```

后端默认地址为：

```text
http://localhost:1231
```

执行后端测试：

```powershell
Set-Location .\learning-program-backend
.\mvnw.cmd test
```

## 启动前端

另开一个终端，在项目根目录执行：

```powershell
Set-Location .\learning-program-frontend
pnpm install
pnpm dev
```

如果使用 npm：

```powershell
Set-Location .\learning-program-frontend
npm install
npm run dev
```

Vite 启动后会同时打印本机地址和局域网地址：

```text
➜  Local:   http://localhost:5173/
➜  Network: http://192.168.1.10:5173/
```

`Network` 一行的地址可供同一局域网内的其他设备（手机、别的电脑）直接访问。

### 前后端连接方式

前端所有请求都走**同源相对路径** `/api/...`（见 `src/main.ts` 的 `axios.defaults.baseURL`），
开发时由 Vite 代理转发到后端，因此换 IP、换设备访问都不需要改代码，也不会触发跨域。

代理目标地址通过环境变量配置，默认 `http://127.0.0.1:1231`：

```powershell
Copy-Item .\learning-program-frontend\.env.example .\learning-program-frontend\.env.local
```

| 变量 | 作用 | 默认值 |
| --- | --- | --- |
| `VITE_DEV_API_TARGET` | `pnpm dev` / `pnpm preview` 时 `/api` 转发到的后端地址 | `http://127.0.0.1:1231` |
| `VITE_API_BASE_URL` | 构建产物中前端直连的后端地址，留空表示同源 | 空（同源） |

后端与前端不在同一台机器上时，把 `VITE_DEV_API_TARGET` 改成后端的实际地址即可，
改完需要重启 dev server。

生产部署推荐用 nginx 等反向代理把前端静态资源和 `/api` 挂在同一个域名下，
`VITE_API_BASE_URL` 保持留空；确实要前后端分域名部署时再填写它，并同步检查后端 CORS 配置。

## 前端构建

```powershell
Set-Location .\learning-program-frontend
pnpm run type-check
pnpm run build
pnpm run preview
```

`pnpm run build` 依次做类型检查、打包、以及**公开页（登录/注册）的预渲染**，产物在 `dist/`。
生产环境的托管方式（nginx 配置、预渲染与兜底页、接口反向代理、HTTPS 与常见问题）
见 [DEPLOYMENT.md](./DEPLOYMENT.md)。

