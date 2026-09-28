# LearningProgram

一个基于 Spring Boot + Vue 的前后端分离学习平台项目，当前以账号认证和 JWT 无状态鉴权为基础功能。项目参考了 [SpringBoot-Vue-Template-Jwt](https://github.com/itbaima-study/SpringBoot-Vue-Template-Jwt)，并在此基础上搭建了自己的前后端工程结构。

> 当前项目仍处于开发阶段，功能和接口会持续完善。

## 功能概览

- 用户注册
- 用户名或邮箱登录
- “记住我”登录状态
- JWT 认证及过期处理
- 退出登录时将 JWT 加入 Redis 黑名单
- 前端路由鉴权，未登录用户不能访问受保护页面
- Spring Security 统一处理未认证、无权限和登录失败响应
- 基于 MyBatis-Plus 操作 MySQL 账户数据
- Element Plus 表单和提示组件

## 技术栈

### 后端

- Java 21
- Spring Boot 4.1.1
- Spring Security
- MyBatis-Plus
- MySQL
- Redis
- Auth0 `java-jwt`
- Maven

### 前端

- Vue 3
- TypeScript
- Vite
- Vue Router
- Element Plus
- Axios
- pnpm 或 npm

## 项目结构

```text
LearningProgram/
├── learning-program-backend/       # Spring Boot 后端
│   ├── src/main/java/com/tick/
│   │   ├── config/                 # 安全及 Web 配置
│   │   ├── controller/             # REST 接口
│   │   ├── entity/                 # 实体、请求和响应对象
│   │   ├── filter/                 # CORS、JWT 等过滤器
│   │   ├── mapper/                 # MyBatis-Plus Mapper
│   │   ├── service/                # 业务逻辑
│   │   └── utils/                  # JWT 等工具类
│   └── src/main/resources/
│       └── application.yaml        # 后端配置
├── learning-program-frontend/      # Vue 前端
│   ├── src/net/                    # Axios 请求及认证状态处理
│   ├── src/router/                 # 前端路由和路由守卫
│   └── src/views/                  # 页面和业务视图
└── README.md
```

## 环境要求

| 软件 | 版本 |
| --- | --- |
| JDK | 21 或更高版本 |
| Maven | 3.9+（也可以使用项目内的 Maven Wrapper） |
| Node.js | 22.18+ 或 24.12+ |
| MySQL | 8.0+ |
| Redis | 6.0+ |

## 数据库准备

后端默认连接名为 `learning` 的 MySQL 数据库，并使用 `db_account` 表保存账户信息。可以执行以下 SQL 初始化开发环境：

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

> `password` 字段需要能够保存 BCrypt 密文，建议使用 `VARCHAR(255)`。如果数据库中已经存在同名表，请按现有数据结构确认字段是否与 `Account` 实体一致。

## 配置说明

后端配置位于 `learning-program-backend/src/main/resources/application.yaml`，启动前至少需要确认以下配置：

- MySQL 地址、端口、数据库名、用户名和密码
- Redis 地址和端口
- JWT 签名密钥及有效期
- 后端端口（默认 `1231`）

示例配置：

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

请勿将真实密码、JWT 密钥或生产环境地址提交到仓库。建议在本地配置文件、环境变量或部署平台的 Secret 中保存这些信息。

`spring.security.email` 当前默认为 `false`，因此注册流程不会强制校验邮箱验证码。邮箱验证码相关能力仍在开发中，启用前需要同时补充后端接口、邮件服务和 Redis 验证码流程。

## 启动项目

### 1. 启动后端

在项目根目录执行：

```bash
cd learning-program-backend
./mvnw spring-boot:run
```

Windows PowerShell 可以执行：

```powershell
cd learning-program-backend
.\mvnw.cmd spring-boot:run
```

后端默认地址为 `http://localhost:1231`。

### 2. 启动前端

另开一个终端，在项目根目录执行：

```bash
cd learning-program-frontend
pnpm install
pnpm dev
```

如果使用 npm：

```bash
cd learning-program-frontend
npm install
npm run dev
```

Vite 会在终端输出前端访问地址，通常为 `http://localhost:5173`。

## 常用命令

### 后端

```bash
# 编译并运行测试
./mvnw test

# 打包
./mvnw clean package
```

Windows PowerShell 将 `./mvnw` 替换为 `.\mvnw.cmd`。

### 前端

```bash
# 启动开发服务器
pnpm dev

# 类型检查并构建生产包
pnpm build

# 预览生产构建
pnpm preview
```

## 主要接口

| 方法 | 路径 | 说明 | 是否需要 JWT |
| --- | --- | --- | --- |
| `POST` | `/api/auth/login` | 用户名/邮箱登录 | 否 |
| `POST` | `/api/auth/register` | 注册账户 | 否 |
| `POST` | `/api/auth/logout` | 退出登录并使当前令牌失效 | 是 |
| `POST` | `/api/auth/reset-confirm` | 校验重置密码信息 | 否 |
| `GET` | `/api/test/hello` | 受保护接口测试 | 是 |
| `GET` | `/api/notifications` | 查询当前用户通知及未读数量 | 是 |
| `POST` | `/api/notifications/{id}/read` | 将当前用户的一条通知标记为已读 | 是 |
| `POST` | `/api/notifications/read-all` | 将当前用户全部通知标记为已读 | 是 |

登录成功后，客户端应在请求头中携带：

```http
Authorization: Bearer <JWT>
```

接口通常返回统一格式：

```json
{
  "code": 200,
  "data": {},
  "message": "success"
}
```

## 开发约定

- 后端接口统一使用 `/api` 前缀。
- 需要登录的后端请求必须经过 JWT 过滤器校验。
- 前端认证信息保存在 `localStorage` 或 `sessionStorage` 的 `authorize` 项中。
- 修改端口、数据库或 Redis 配置后，请同步检查前端请求地址和跨域配置。

## 参考资料

- [SpringBoot-Vue-Template-Jwt](https://github.com/itbaima-study/SpringBoot-Vue-Template-Jwt)
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/)
- [Vue.js Documentation](https://cn.vuejs.org/)
- [Element Plus Documentation](https://element-plus.org/)

## 许可证

项目目前未声明正式开源许可证，仅用于学习和开发交流。若要公开发布或用于商业用途，请先补充许可证并确认第三方依赖的许可要求。
