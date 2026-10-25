# learning-program-frontend

LearningProgram 学习平台的 Vue 3 前端。基于 Vite + TypeScript，使用 Element Plus 组件库、Vue Router 路由、Vue i18n 中文语言包，通过 Axios 以同源 `/api` 相对路径访问后端。

后端与整体项目说明见 [上级 README](../README.md)。

## 环境要求

- Node.js（建议 20+）
- pnpm 或 npm

## 快速开始

```sh
# 安装依赖
pnpm install          # 或 npm install

# 启动开发服务器（默认由 vite 代理 /api 到后端）
pnpm dev              # 或 npm run dev

# 以 SSR 模式启动开发服务器（HTML 服务端直出，/api 代理行为相同）
pnpm run dev:ssr
```

开发时 vite 把 `/api` 转发到 `VITE_DEV_API_TARGET`（默认 `http://127.0.0.1:1231`）。

## 环境变量

复制 `.env.example` 为 `.env.local` 后按需修改（`.env` / `.env.local` 已被 gitignore 忽略，改完需重启 dev server）：

| 变量 | 说明 |
| --- | --- |
| `VITE_DEV_API_TARGET` | dev / preview 时 `/api` 代理转发的后端地址，默认 `http://127.0.0.1:1231` |
| `VITE_API_BASE_URL` | 构建产物中前端直连的后端地址；留空表示与前端同源（推荐，由反向代理转发 `/api`）；前后端分开部署时才填写 |

## 构建与预览

```sh
# 类型检查 + 生产构建（输出到 dist/，SPA 静态部署用）
pnpm build            # 或 npm run build

# 本地预览构建产物（已配置 /api 代理，等价于生产同源行为）
pnpm preview

# 仅类型检查
pnpm type-check

# SSR：双构建（客户端 + 服务端，输出到 dist/ssr/），随后启动生产 SSR 服务（默认 :3000）
pnpm run build:ssr
pnpm run start        # PORT=xxxx 可覆盖端口
```

## 目录结构

```text
src/
├── components/     # 复用组件
├── i18n/           # i18n 配置与中文语言包
├── net/            # Axios 请求封装与认证状态处理
├── router/         # 路由与路由守卫（createAppRouter 工厂，客户端/SSR 共用）
├── utils/          # 工具函数（markdown、密码策略、每日单词、SSR 安全 storage 等）
├── create-app.ts   # 应用工厂：客户端入口（main.ts）与服务端入口（entry-server.ts）共享
└── views/          # 页面组件
    ├── welcome/    # 登录、注册、找回密码
    ├── page/       # 课程、测试、讨论、排行榜、资料、通知等
    └── admin/      # 管理后台（用户、课程、题库、题集、资源、通知）

server.mjs          # SSR 服务器：dev 态内嵌 vite（HMR 可用），prod 态托管 dist/ssr 产物
```

## SSR 说明

前端支持 SPA（默认）与 SSR 两种运行模式，源码共用、命令独立（见上文「快速开始」「构建与预览」）。几个设计要点：

- **双入口共享工厂**：`src/create-app.ts` 负责组装应用，`src/main.ts`（客户端，挂载/水合）与 `src/entry-server.ts`（服务端，每个请求渲染一份 HTML）都走它。
- **登录态只在浏览器**：token 存 localStorage / sessionStorage，服务端读不到，因此 SSR 恒按游客视角渲染，已登录用户水合后由客户端纠正（可能有一瞬闪动）；依赖登录态的路由守卫跳转在服务端跳过。
- **服务端不发请求**：`src/net` 在 SSR 下直接跳过所有请求，页面骨架由服务端直出、数据由水合后的浏览器拉取；因此 ElMessage 等浏览器 API 不会在服务端触发。
- **浏览器 API 统一走 `src/utils/storage.ts`**：模块级 / setup 顶层的 storage 访问必须用它（直接写 `localStorage` 会在服务端渲染时抛错）；`onMounted` 等生命周期内的访问天然只在客户端执行，不受限制。
- **Element Plus 需 `ssr.noExternal`**：unplugin 自动注入的组件样式含 `.css` 导入，Node 无法解析，已在 `vite.config.ts` 配好。
- 渲染出错时生产服务自动降级为 SPA（返回未填充模板），页面仍可用。

生产部署（systemd + nginx 反代）见 [`../docs/DEPLOYMENT.md`](../docs/DEPLOYMENT.md) 第 6 节。

## 部署

构建产物（`dist/`）是纯静态文件，生产环境推荐用 nginx 托管静态资源并把 `/api` 反向代理到后端，做到前后端同源、不涉及跨域；需要首屏直出 / SEO 时可改用 SSR 模式。

- 现成的 nginx 站点配置（含 SSR 反代注释版）：[`deploy/nginx.conf`](./deploy/nginx.conf)
- 完整部署步骤与常见问题：[`../docs/DEPLOYMENT.md`](../docs/DEPLOYMENT.md)

## 开发提示

- 使用 VS Code + [Vue (Official)](https://marketplace.visualstudio.com/items?itemName=Vue.volar) 插件（禁用 Vetur）。
- `.vue` 导入的类型检查由 `vue-tsc` 处理，编辑器需 Volar 让 TS 语言服务识别 `.vue` 类型。
- Element Plus 组件与 API 通过 `unplugin-auto-import` / `unplugin-vue-components` 自动导入，无需手动 import。
