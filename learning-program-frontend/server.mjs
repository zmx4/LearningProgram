/**
 * SSR 服务器：开发与生产共用一个入口文件。
 *
 * 开发态（pnpm run dev:ssr）：
 *   vite 以 middlewareMode 内嵌进本服务，源码即时编译、HMR 照常可用，
 *   /api 仍按 vite.config 的 server.proxy 转发到后端。
 * 生产态（先 pnpm run build:ssr，再 pnpm run start，即 node server.mjs --prod）：
 *   托管 dist/ssr/client 下的静态资源，HTML 由 dist/ssr/server/entry-server.js 渲染。
 *   生产建议由 nginx 前置：静态资源与页面仍可由本服务提供，/api 由 nginx 转给后端。
 *
 * 端口：环境变量 PORT 覆盖，默认开发 5173 / 生产 3000。
 */
import { createServer as createHttpServer } from 'node:http'
import { existsSync, readdirSync, readFileSync } from 'node:fs'
import { resolve, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'
import express from 'express'

const root = dirname(fileURLToPath(import.meta.url))
const distClient = resolve(root, 'dist/ssr/client')
const distServerEntry = resolve(root, 'dist/ssr/server/entry-server.js')

// 用显式参数区分模式（--prod），不依赖 NODE_ENV 或构建产物是否存在：
// 曾经构建过 SSR 的机器上跑 dev:ssr 仍应进开发模式
const isProduction = process.argv.includes('--prod')
const port = Number(process.env.PORT) || (isProduction ? 3000 : 5173)

if (isProduction && !existsSync(distServerEntry)) {
  console.error('生产模式需要先执行 pnpm run build:ssr 构建 SSR 产物')
  process.exit(1)
}

const app = express()
const httpServer = createHttpServer(app)

let vite = null
let template = ''            // 页面模板：dev 为源 index.html（经 vite 注入），prod 为构建产物
let render = null            // SSR 渲染函数（src/entry-server.ts 导出）
let cssManifest = null       // 生产态产物清单（.vite/manifest.json），用于按路由收集样式
let fallbackCssLinks = ''    // 清单缺失时的兜底：注入全部路由样式

// 进程级兜底：个别组件库代码在服务端触发未捕获的 Promise 拒绝（如访问 document），
// 默认行为是整个进程退出，这里改为记录日志并由请求级 try/catch 兜住页面渲染。
process.on('unhandledRejection', (reason) => {
  console.error('未处理的 Promise 拒绝（进程继续运行）:', reason)
})
process.on('uncaughtException', (err) => {
  console.error('未捕获的异常（进程继续运行）:', err)
})


if (!isProduction) {
  // Vite 8 的 Node API 导出名为 createServer（旧版本的 createViteServer 已移除）
  const { createServer: createViteServer } = await import('vite')
  vite = await createViteServer({
    server: {
      middlewareMode: true,
      // 把 HMR 的 WebSocket 挂到本服务的 httpServer 上（middlewareMode 下必须显式指定）
      ws: { server: httpServer },
    },
    appType: 'custom',
  })
  app.use(vite.middlewares)
} else {
  template = readFileSync(resolve(distClient, 'index.html'), 'utf-8')

  // 构建产物 index.html 只带入入口 chunk 的样式；SSR 直出深层路由时，
  // 该路由的样式在懒加载 chunk 里。这里读产物清单，按本次渲染实际用到的组件
  // 收集 CSS（见 cssLinksForModules），避免把全部路由的样式都塞进首屏 HTML。
  const manifestPath = resolve(distClient, '.vite/manifest.json')
  if (existsSync(manifestPath)) {
    cssManifest = JSON.parse(readFileSync(manifestPath, 'utf-8'))
  } else {
    // 兼容没有产物清单的旧构建：退回注入全部路由样式（首屏会多下载，但不会无样式）
    console.warn('未找到 dist/ssr/client/.vite/manifest.json，退回注入全部路由 CSS')
    const assetsDir = resolve(distClient, 'assets')
    fallbackCssLinks = readdirSync(assetsDir)
      .filter((file) => file.endsWith('.css') && !template.includes(file))
      .map((file) => `<link rel="stylesheet" href="/assets/${file}">`)
      .join('')
  }

  ;({ render } = await import(distServerEntry))
}

// 生产态静态资源：带 hash 的 assets 长缓存，其余（favicon 等）协商缓存
if (isProduction) {
  app.use('/assets', express.static(resolve(distClient, 'assets'), { maxAge: '1y', immutable: true }))
  app.use(express.static(distClient, { index: false }))
}

/**
 * 按本次 SSR 渲染登记的组件模块收集样式链接。
 * 清单里一个模块只记录自身 chunk 的 CSS，共享的组件库样式挂在它静态依赖的 chunk 上，
 * 因此要顺着 imports 递归；入口模板里已有的样式直接跳过，避免重复加载。
 */
function cssLinksForModules(modules) {
  if (!cssManifest) return fallbackCssLinks

  const visited = new Set()
  const cssFiles = new Set()
  const walk = (key) => {
    if (!key || visited.has(key)) return
    visited.add(key)
    const entry = cssManifest[key]
    if (!entry) return
    for (const file of entry.css ?? []) cssFiles.add(file)
    for (const dep of entry.imports ?? []) walk(dep)
  }
  for (const moduleId of modules) walk(moduleId)

  let links = ''
  for (const file of cssFiles) {
    const href = `/${file}`
    if (!template.includes(href)) links += `<link rel="stylesheet" href="${href}">`
  }
  return links
}

app.use(async (req, res, next) => {
  if (req.method !== 'GET' || !req.path.startsWith('/')) {
    next()
    return
  }
  if (req.path.startsWith('/api')) {
    res.status(404).json({ code: 404, message: 'SSR 服务器不处理 /api，请由反向代理转发到后端' })
    return
  }

  try {
    let html = template
    let renderPage = render
    if (!isProduction) {
      // 每次请求都重新读模板并注入 dev 脚本，保证 index.html 修改即时生效；
      // 服务端入口也按需加载，源码改动后 vite 会自动失效重编译
      html = readFileSync(resolve(root, 'index.html'), 'utf-8')
      html = await vite.transformIndexHtml(req.originalUrl, html)
      ;({ render: renderPage } = await vite.ssrLoadModule('/src/entry-server.ts'))
    }

    const { html: appHtml, title, modules } = await renderPage(req.originalUrl)

    // 用函数形式替换，避免渲染内容里的 $& / $' 等被当作 replace 的特殊模式
    html = html.replace('<div id="app"></div>', () => `<div id="app">${appHtml}</div>`)
    if (title) {
      html = html.replace('<title>学习平台</title>', () => `<title>${title} - 学习平台</title>`)
    }
    // 开发态样式由 vite 内联注入，不需要额外 <link>
    const cssLinks = isProduction ? cssLinksForModules(modules ?? []) : ''
    if (cssLinks) {
      html = html.replace('</head>', () => `${cssLinks}</head>`)
    }

    res.status(200).set({ 'Content-Type': 'text/html; charset=utf-8' }).end(html)
  } catch (error) {
    if (!isProduction && vite) {
      vite.ssrFixStacktrace(error)
    }
    console.error(`SSR 渲染失败: ${req.originalUrl}`, error)
    // 生产态降级为 SPA：退回未填充的模板，页面仍能由客户端脚本正常拉起
    if (isProduction && template) {
      res.status(200).set({ 'Content-Type': 'text/html; charset=utf-8' }).end(template)
    } else {
      next(error)
    }
  }
})

httpServer.listen(port, () => {
  console.log(`SSR 服务器已启动（${isProduction ? '生产' : '开发'}模式）: http://127.0.0.1:${port}`)
})
