# 前端部署

本文只讲**生产环境**怎么把前端跑起来。开发环境（`pnpm dev`、局域网调试、代理配置）见 [INSTALLATION.md](./INSTALLATION.md)。

前端构建后是一堆纯静态文件，用任意静态服务器托管即可；因为前端路由用的是 history 模式，
**服务器必须把找不到的路径回退到兜底页**，否则刷新子页面会 404。

构建时还会**预渲染（SSG）登录与注册这两个公开页**，让首屏与爬虫直接拿到真实 HTML；
因为登录态存在浏览器本地、服务端看不到，受保护页面不做预渲染（详见第 4 节）。

推荐做法：让 **nginx 同时托管静态文件并把 `/api` 反向代理到后端**，
这样前后端同源、不涉及跨域，也不需要在前端填后端地址。

```
浏览器 ──► nginx (:80/:443) ──┬── /           预渲染好的登录页
                              ├── /register   预渲染好的注册页
                              ├── 其他路由    兜底页 spa.html（由前端路由接管）
                              └── /api/...    反向代理 ──► Spring Boot (127.0.0.1:1231)
```

---

## 1. 构建

```powershell
Set-Location .\learning-program-frontend
pnpm install
pnpm run build
```

`pnpm run build` 依次执行四步（`run-s` 串行，任一步失败即中断）：

| 步骤 | 命令 | 作用 |
| --- | --- | --- |
| 1 | `type-check` | `vue-tsc` 类型检查，不通过就不产出产物 |
| 2 | `build-only` | `vite build` 打出客户端产物 |
| 3 | `build-ssr` | `vite build --ssr` 打出一份 Node 端渲染用的 bundle（`dist-ssr/`） |
| 4 | `prerender` | 用第 3 步的 bundle 把公开页渲染成静态 HTML，最后删掉 `dist-ssr/` |

只想跳过类型检查快速打包时用 `pnpm run build-only`（**注意：这样不会预渲染**）。

产物在 `learning-program-frontend/dist/`：

```
dist/
├── index.html              # 预渲染好的「/」登录页
├── register/
│   └── index.html          # 预渲染好的「/register」注册页
├── spa.html                # 未预渲染的空壳，其余路由的兜底页
├── favicon.ico
└── assets/                 # 文件名带内容 hash，可长期缓存
    ├── index-XXXXXXXX.js   # 客户端入口（预渲染页与兜底页共用同一份）
    ├── index-XXXXXXXX.css
    └── ...
```

### 后端地址怎么定

`dist` 里的请求地址在**构建时**就定下来了（`src/app.ts` 读取 `VITE_API_BASE_URL`）：

| 场景 | `VITE_API_BASE_URL` | 说明 |
| --- | --- | --- |
| nginx 同源代理（推荐） | 留空 | 请求走 `/api/...`，由 nginx 转发，换后端地址不用重新构建 |
| 前后端分开域名 | `https://api.example.com` | 直连后端，需确认后端 CORS 与浏览器混合内容限制 |

留空是默认值，所以推荐的同源方案下**什么都不用配**。需要覆盖时在构建前设置：

```powershell
Set-Location .\learning-program-frontend
$env:VITE_API_BASE_URL = 'https://api.example.com'
pnpm run build
```

---

## 2. 上传产物

把 `dist/` **里面的内容**（不是 `dist` 目录本身）放到 nginx 配置里 `root` 指向的目录：

```bash
sudo mkdir -p /var/www/learning-program
sudo rsync -av --delete dist/ /var/www/learning-program/
sudo chown -R www-data:www-data /var/www/learning-program
```

`--delete` 很重要：带 hash 的旧资源文件如果一直留着，目录会越来越乱（不影响功能，但没必要）。

部署到 Windows 上用 nginx for Windows 时，`root` 写成类似 `D:/www/learning-program;`（**用正斜杠**）。

---

## 3. 配置 nginx

仓库已提供现成配置：[learning-program-frontend/deploy/nginx.conf](../learning-program-frontend/deploy/nginx.conf)。
它是一段可以直接放进 `conf.d/` 的 `server {}` 块，已经处理好：

- **SPA 路由回退到兜底页 `spa.html`** —— `/discussions/3` 这类路径刷新不再 404（**不是**回退到 `index.html`，原因见下）
- **预渲染页直接命中** —— `/` 与 `/register` 直接返回各自的静态 HTML，且不会产生 301 跳转
- **HTML 不缓存、`/assets/` 长缓存** —— 发布后立刻生效，同时不牺牲静态资源的缓存收益
- **`/api/` 反向代理** —— 转发到 `127.0.0.1:1231`，并带上 `X-Forwarded-For` 等头
- **`client_max_body_size 60m`** —— 后端允许 55MB 的资源上传，nginx 默认 1MB，不放开会在 nginx 层就被 413 拒绝
- **gzip、安全响应头、隐藏文件拒绝访问**

### 为什么兜底页是 spa.html 而不是 index.html

构建后 `index.html` 已经被预渲染成**登录页**。如果让受保护路由也回退到它，
客户端拿到的是登录页的 DOM，却要渲染课程页，hydration 必然对不上（页面错乱或重复）。

所以构建时额外留了一份未预渲染的空壳 `spa.html` 作为兜底；
`index.html` / `register/index.html` 只服务于它们自己的路由。

部署步骤：

```bash
# 1. 放到 conf.d（Debian/Ubuntu 的 nginx 默认会 include 这个目录）
sudo cp learning-program-frontend/deploy/nginx.conf /etc/nginx/conf.d/learning-program.conf

# 2. 按需修改域名与静态目录
sudo vi /etc/nginx/conf.d/learning-program.conf   # server_name、root

# 3. 校验配置并热加载（不会中断现有连接）
sudo nginx -t
sudo nginx -s reload
```

`nginx -t` 报错时先别 reload —— 配置有语法错误时 reload 会失败并保留旧配置，但 `-t` 能直接告诉你行号。

### 后端不在同一台机器

改 `proxy_pass` 的目标地址，并且要额外处理一件事：后端**只信任来自可信代理的 `X-Forwarded-For`**
（Spring Boot 默认信任 `127.0.0.1` 与内网网段）。若 nginx 与后端不同机且不在默认信任范围内，
需要在后端补上 nginx 的地址，否则后端会把 nginx 的 IP 当作所有用户的 IP：

```yaml
server:
  tomcat:
    remoteip:
      internal-proxies: "10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|192\\.168\\.\\d{1,3}\\.\\d{1,3}|172\\.(1[6-9]|2\\d|3[01])\\.\\d{1,3}\\.\\d{1,3}"
```

### 开启 HTTPS

配置末尾附了 HTTPS 的模板。用 certbot 一条命令即可（它会自动改写 80 端口为跳转并配好证书路径）：

```bash
sudo certbot --nginx -d learning.example.com
```

---

## 4. 验证

```bash
# 公开页必须是「带真实内容的 HTML」，而不是空壳（关键）
curl -s http://learning.example.com/          | grep -q '用户名/邮箱' && echo '登录页已预渲染'
curl -s http://learning.example.com/register  | grep -q '注册新用户' && echo '注册页已预渲染'

# 受保护路由回退到兜底页：应当是空壳（<div id="app"></div>），且不含登录页 HTML
curl -s http://learning.example.com/courses   | grep -q 'id="app"></div>' && echo '兜底页正常'
curl -s -o /dev/null -w '%{http_code}\n' http://learning.example.com/discussions/3   # 期望 200

# 入口 HTML 不缓存
curl -I http://learning.example.com/index.html          # 期望 Cache-Control: no-cache
curl -I http://learning.example.com/register/index.html # 期望 Cache-Control: no-cache
curl -I http://learning.example.com/spa.html            # 期望 Cache-Control: no-cache

# 静态资源长缓存
curl -I http://learning.example.com/assets/index-XXXXXXXX.js   # 期望 max-age=31536000

# 缺失资源必须 404，而不是回退成 HTML
curl -I http://learning.example.com/assets/not-exist.js        # 期望 404

# 后端接口都在同源 /api 下，不需要 CORS
curl http://learning.example.com/api/auth/password-policy
```

浏览器里再确认一遍：打开任意子页面按 F5 不报 404、登录后刷新仍是登录态、
**打开控制台不应出现 hydration 相关的告警**、浏览器控制台没有跨域（CORS）报错。

---

## 5. 预渲染（SSG）说明

### 为什么只预渲染公开页

登录态（token）存在浏览器的 `localStorage` 里，**服务端渲染时读不到**，
所以服务端看到的永远是「未登录」，受保护路由在服务端一律会被路由守卫重定向到登录页，
给它们做预渲染没有意义。因此只预渲染两个不需要登录态的页面：

| 路由 | 产物 |
| --- | --- |
| `/` | `dist/index.html` |
| `/register` | `dist/register/index.html` |

客户端接管时的策略（见 `src/entry-client.ts`）：

- 容器里是匹配的预渲染结果、且本地**未登录** → 用 `createSSRApp` 做 hydration，复用这份 DOM
- 其余情况（本地已登录、或走的是 `spa.html` 兜底页）→ 用 `createApp` 清空容器后重新渲染

之所以区分这两种情况：已登录用户打开预渲染的登录页会被守卫重定向到首页，
渲染结果与预渲染内容不同，强行 hydration 会不匹配。

### 想把别的页面也做成预渲染

1. 在 `scripts/prerender.mjs` 的 `routes` 里加一条 `{ path, output, title, mustContain }`
   - `mustContain` 是防呆检查：渲染结果里必须出现该页的特征文案，否则构建失败
     （少写一句 `await router.push` 就会把每个页面都渲染成首页，从文件大小上看不出来）
2. 在 `deploy/nginx.conf` 的 `location /` 里无需改动 —— `$uri/index.html` 已经能覆盖新的子目录
3. 该页面必须**不依赖登录态**，且 `setup()` 里不能直接访问 `window` / `localStorage`
   （一律走 `src/utils/storage.ts`，它在服务端返回 null）

### 已知限制

- 服务端不会渲染登录后的内容，所以预渲染对「登录后的首屏」没有帮助，收益集中在登录/注册页与爬虫抓取
- 真正的按请求 SSR（每个请求都服务端渲染）需要把鉴权改成 Cookie，目前没有做

---

## 6. 上线新版本

```bash
# 本地构建后同步过去，nginx 不需要重启
rsync -av --delete dist/ server:/var/www/learning-program/
```

因为所有 HTML 入口都不缓存、`assets/` 文件名带 hash，用户下次打开就是新版本，不需要额外清缓存操作。

---

## 常见问题

**刷新子页面 404**
nginx 缺少 SPA 回退。确认 `location /` 里有 `try_files $uri $uri/index.html /spa.html;`。

**首页白屏 / 控制台报 hydration 相关错误**
多半是兜底页配错了：`location /` 的兜底目标必须是 `spa.html`，
若写成 `/index.html`，受保护路由会拿到预渲染的登录页 HTML 去做 hydration。

**公开页没有预渲染内容（打开只有空壳）**
`pnpm run build-only` 不会预渲染。请用完整的 `pnpm run build`（含 `build-ssr` 与 `prerender`），
并确认 `dist/spa.html`、`dist/register/index.html` 都存在。

**`/register` 出现 301 跳转**
说明 `location /` 里用了 `$uri/` 而不是 `$uri/index.html`。前者命中目录时 nginx 会补一个斜杠跳转，
后者直接命中文件，不会跳转。

**页面能打开但所有接口 404**
`/api/` 的 location 没配或没生效；也检查后端是否在 `127.0.0.1:1231` 上运行。用
`curl -i http://<前端域名>/api/auth/password-policy` 区分是 nginx 的问题还是后端没起来。

**接口报跨域错误**
说明请求没有走同源代理 —— 多半是构建时设置了 `VITE_API_BASE_URL` 指到了另一个域名。
同源方案下这个变量必须留空（构建时生效，改完要重新构建）。

**上传大文件报 413**
`client_max_body_size` 没放开。后端上限是 55MB，配置里给的是 60m。

**操作几次就提示「请求过于频繁，请稍后再试」**
后端 `FlowLimitFilter` 限制同一 IP 3 秒内最多 50 次请求，超过则拉黑 30 秒并返回 HTTP `429`。
如果**所有用户**都很快被限流，说明限流是按代理的 IP 计数的，通常是下面几种原因之一：

- nginx 没把客户端地址传过去（本仓库的配置已经带了 `X-Forwarded-For`）
- 后端没启用 `server.forward-headers-strategy=native`（本仓库已开启）
- nginx 与后端不同机、且 nginx 不在后端的可信代理列表里（见上文「后端不在同一台机器」）

可以这样确认限流键到底用的是哪个 IP：

```bash
redis-cli -h <redis-host> -a <redis-password> KEYS 'flow::counter:*'
```

正常应当看到一个个**客户端真实 IP**；如果只有一个 `flow::counter:127.0.0.1`，
就说明所有用户都被算成了同一个 IP。

**发布后仍是旧页面**
检查 HTML 入口（`index.html` / `register/index.html` / `spa.html`）的响应头是不是 `no-cache`；
如果是强缓存，用户会一直加载旧的入口文件。
