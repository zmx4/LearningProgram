# learning-program-frontend

This template should help get you started developing with Vue 3 in Vite.

## Recommended IDE Setup

[VS Code](https://code.visualstudio.com/) + [Vue (Official)](https://marketplace.visualstudio.com/items?itemName=Vue.volar) (and disable Vetur).

## Recommended Browser Setup

- Chromium-based browsers (Chrome, Edge, Brave, etc.):
  - [Vue.js devtools](https://chromewebstore.google.com/detail/vuejs-devtools/nhdogjmejiglipccpnnnanhbledajbpd)
  - [Turn on Custom Object Formatter in Chrome DevTools](http://bit.ly/object-formatters)
- Firefox:
  - [Vue.js devtools](https://addons.mozilla.org/en-US/firefox/addon/vue-js-devtools/)
  - [Turn on Custom Object Formatter in Firefox DevTools](https://fxdx.dev/firefox-devtools-custom-object-formatters/)

## Type Support for `.vue` Imports in TS

TypeScript cannot handle type information for `.vue` imports by default, so we replace the `tsc` CLI with `vue-tsc` for type checking. In editors, we need [Volar](https://marketplace.visualstudio.com/items?itemName=Vue.volar) to make the TypeScript language service aware of `.vue` types.

## Customize configuration

See [Vite Configuration Reference](https://vite.dev/config/).

## Project Setup

```sh
npm install
```

### Compile and Hot-Reload for Development

```sh
npm run dev
```

### Type-Check, Compile and Minify for Production

```sh
npm run build
```

## 部署

构建产物（`dist/`）是纯静态文件，生产环境推荐用 nginx 托管静态资源并把 `/api` 反向代理到后端，
做到前后端同源、不涉及跨域。

`pnpm run build` 会依次执行类型检查、客户端打包、SSR bundle 打包、公开页预渲染（SSG）：

```
type-check → build-only → build-ssr → prerender
```

预渲染只覆盖不需要登录态的 `/` 与 `/register`（token 存在浏览器本地，服务端看不到），
其余路由回退到未预渲染的 `dist/spa.html`，由前端路由接管。

- 现成的 nginx 站点配置：[`deploy/nginx.conf`](./deploy/nginx.conf)
- 完整部署步骤、预渲染说明与常见问题：[`../docs/DEPLOYMENT.md`](../docs/DEPLOYMENT.md)

本地想先验证构建产物，用 `pnpm run preview`（已配置 `/api` 代理，等价于生产环境的同源行为）。
注意 `preview` 不会套用 nginx 的预渲染/兜底规则，它只用于确认页面能跑起来。

