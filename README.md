# LearningProgram

一个基于 Spring Boot + Vue 的前后端分离在线学习平台，提供账号认证、课程学习、单词与知识测试、讨论区、排行榜、签到积分、学习资源和管理员后台等功能。

> 当前项目仍处于开发阶段，功能和接口会持续完善。

## 功能概览

### 账号与鉴权

- 用户注册、用户名或邮箱登录、“记住我”登录状态
- JWT 无状态认证及过期处理，Redis JWT 黑名单
- 找回密码、个人资料管理
- 前端路由鉴权，Vue i18n 中文语言包

### 学习

- 课程与章节浏览、课程学习进度记录
- CET-4 / CET-6 单词库与单词测试
- 知识测试、题型与题库管理
- 每日单词、词典查询
- 学习资源管理

### 互动与激励

- 讨论区发帖、评论
- 签到记录与积分体系
- 多维度学习排行榜
- 用户通知与已读状态

### 管理后台

- 用户管理、定向通知
- 课程、章节、题库、题集管理
- 学习资源管理

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
- Vue i18n
- Element Plus
- Axios
- `@vueuse/core`、`markdown-it`
- pnpm 或 npm

## 项目结构

```text
LearningProgram/
├── docs/
│   ├── API.md                    # 接口文档
│   ├── CLASS_DIAGRAM.puml        # 后端类图（PlantUML 源文件，附渲染 PNG）
│   ├── DATABASE.md              # 数据库说明文档
│   ├── DEPLOYMENT.md            # 前端部署与 Nginx 配置文档
│   └── INSTALLATION.md          # 安装与运行文档
├── learning-program-backend/    # Spring Boot 后端
│   └── src/main/java/com/tick/
│       ├── config/              # 安全及 Web 配置
│       ├── controller/          # REST 接口
│       ├── entity/              # 实体、DTO、VO 请求和响应对象
│       ├── filter/              # CORS、JWT 等过滤器
│       ├── mapper/              # MyBatis-Plus Mapper
│       ├── security/            # 密码策略、密码生成等安全组件
│       ├── service/             # 业务逻辑
│       └── utils/               # JWT 等工具类
├── learning-program-frontend/   # Vue 前端
│   ├── src/components/          # 复用组件
│   ├── src/i18n/                # i18n 配置和语言包
│   ├── src/net/                 # Axios 请求及认证状态处理
│   ├── src/router/              # 前端路由和路由守卫
│   ├── src/utils/               # 前端工具函数
│   └── src/views/               # 页面组件（welcome / page / admin）
└── README.md
```

## 文档

- [安装与运行](./docs/INSTALLATION.md)：环境要求、数据库、配置、启动和构建命令。
- [数据库说明](./docs/DATABASE.md)：表结构、表间关系、积分与级联删除约定和迁移脚本。
- [接口文档](./docs/API.md)：认证、个人资料、课程、测试、讨论、排行榜、通知、资源和管理员接口。
- [部署文档](./docs/DEPLOYMENT.md)：前端构建产物托管与 Nginx 反向代理配置。
- [后端类图](./docs/CLASS_DIAGRAM.puml)：controller / service / mapper / entity 分层与领域实体关系（[渲染 PNG](./docs/CLASS_DIAGRAM.png)）。

## 开发约定

- 后端接口统一使用 `/api` 前缀。
- 需要登录的后端请求必须经过 JWT 过滤器校验。
- 管理员接口由 Spring Security 强制校验 `admin` 角色。
- 前端认证信息保存在 `localStorage` 或 `sessionStorage` 的 `authorize` 项中。
- 前端统一请求同源相对路径 `/api`，开发时由 Vite 代理转发到后端，地址在 `learning-program-frontend/.env.local` 中配置。
- 修改后端端口、数据库或 Redis 配置后，请同步检查前端 `VITE_DEV_API_TARGET` 和跨域配置。

## 参考资料

- [SpringBoot-Vue-Template-Jwt](https://github.com/itbaima-study/SpringBoot-Vue-Template-Jwt)
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/)
- [Vue.js Documentation](https://cn.vuejs.org/)
- [Vue I18n Documentation](https://vue-i18n.intlify.dev/)
- [Element Plus Documentation](https://element-plus.org/)

## 许可证

项目目前未声明正式开源许可证，仅用于学习和开发交流。若要公开发布或用于商业用途，请先补充许可证并确认第三方依赖的许可要求。
