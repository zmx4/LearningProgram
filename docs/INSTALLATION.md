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

后端默认使用名为 `learning` 的 MySQL 数据库。首次部署时可以执行：

```sql
CREATE DATABASE IF NOT EXISTS learning
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE learning;

CREATE TABLE IF NOT EXISTS db_account (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(32) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(128) NOT NULL UNIQUE,
    role VARCHAR(32) NOT NULL DEFAULT 'user',
    register_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS db_notification (
    id INT PRIMARY KEY AUTO_INCREMENT,
    account_id INT NOT NULL,
    title VARCHAR(128) NOT NULL,
    content VARCHAR(1000) NOT NULL,
    type VARCHAR(32) NOT NULL DEFAULT 'system',
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notification_account
        FOREIGN KEY (account_id) REFERENCES db_account(id)
        ON DELETE CASCADE,
    INDEX idx_notification_account_created (account_id, created_at)
);
```

如果数据库中已经存在同名表，请先确认其字段与后端 `Account`、`Notification` 实体一致。`password` 字段需要能够保存 BCrypt 密文，建议使用 `VARCHAR(255)`。

项目中的数据库迁移脚本位于 `learning-program-backend/src/main/resources/db/migration/`，已有数据库升级时应按实际结构执行。

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

