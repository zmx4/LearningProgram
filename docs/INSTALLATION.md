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

启动前请配置以下内容：

- MySQL 地址、端口、数据库名、用户名和密码
- Redis 地址、端口和密码（如果启用密码）
- JWT 签名密钥和有效期
- 后端端口

本地开发配置示例：

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/learning?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: your_mysql_user
    password: your_mysql_password
  data:
    redis:
      host: 127.0.0.1
      port: 6379
  security:
    jwt:
      key: replace-with-a-long-random-secret
      expire: 144
    email: false
server:
  port: 1231
```

请勿把真实密码、JWT 密钥或生产环境地址提交到仓库。生产环境应使用环境变量、外部配置中心或部署平台的 Secret。

`spring.security.email` 当前设置为 `false` 时，注册流程不会强制校验邮箱验证码。启用邮箱校验前，需要同时配置邮件服务和验证码流程。

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

Vite 通常会使用：

```text
http://localhost:5173
```

前端 API 地址目前在 `learning-program-frontend/src/main.ts` 中配置。修改后端地址时，请同步检查 CORS 配置。

## 前端构建

```powershell
Set-Location .\learning-program-frontend
pnpm run type-check
pnpm run build
pnpm run preview
```

