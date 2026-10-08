/**
 * 构建期预渲染（SSG）。
 *
 * 只预渲染**不依赖登录态**的公开页：token 存在浏览器 localStorage 里，Node 端看不到，
 * 受保护页面在服务端一律会被路由守卫重定向到登录页，直出没有意义。
 *
 * 产物：
 *   dist/index.html            预渲染的「/」登录页
 *   dist/register/index.html   预渲染的「/register」注册页
 *   dist/spa.html              原本的空壳页，作为其余路由的 SPA 兜底
 *
 * 之所以要单独留一份 spa.html：如果让兜底也返回预渲染过的 index.html，
 * 客户端在 /courses 这类路由上会拿登录页的 DOM 去 hydration，必然不匹配。
 */
import { mkdir, readFile, rm, writeFile } from 'node:fs/promises'
import { existsSync } from 'node:fs'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'

const projectRoot = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const distDir = join(projectRoot, 'dist')
const ssrEntry = join(projectRoot, 'dist-ssr', 'entry-server.js')

/**
 * 需要预渲染的公开路由。
 * mustContain 是防呆检查：渲染结果里必须出现该页面的特征文案，
 * 否则说明路由没真正切过去（比如少 await 了 router.push，会把每个页面都渲染成首页），
 * 这种错误从文件大小上看不出来，一定要让构建直接失败。
 */
const routes = [
  { path: '/', output: 'index.html', title: '学习平台 - 登录', mustContain: ['用户名/邮箱', '立即登录'] },
  { path: '/register', output: 'register/index.html', title: '注册 - 学习平台', mustContain: ['注册新用户', '获取验证码'] },
]

function inject(html, { path, title }, renderedHtml) {
  let result = html
  if (title) {
    result = result.replace(/<title>[\s\S]*?<\/title>/, `<title>${title}</title>`)
  }
  return result.replace(
    /<div id="app"><\/div>/,
    `<div id="app" data-prerendered-route="${path}">${renderedHtml}</div>`,
  )
}

async function main() {
  if (!existsSync(ssrEntry)) {
    throw new Error(`找不到 SSR 产物 ${ssrEntry}，请先执行 vite build --ssr src/entry-server.ts`)
  }

  const template = await readFile(join(distDir, 'index.html'), 'utf-8')
  if (!template.includes('<div id="app"></div>')) {
    throw new Error('dist/index.html 里找不到空的 <div id="app"></div>，模板可能已被改过')
  }

  // 先留一份未预渲染的空壳，供其余路由兜底（nginx 的 try_files 指向它）
  await writeFile(join(distDir, 'spa.html'), template, 'utf-8')
  console.log('  dist/spa.html             (SPA 兜底页)')

  const { render } = await import(pathToFileURL(ssrEntry).href)

  for (const route of routes) {
    const renderedHtml = await render(route.path)
    if (!renderedHtml.trim()) {
      throw new Error(`预渲染 ${route.path} 得到空内容，多半是渲染过程中被吞了异常`)
    }
    const missing = (route.mustContain ?? []).filter(text => !renderedHtml.includes(text))
    if (missing.length > 0) {
      throw new Error(
        `预渲染 ${route.path} 的结果里找不到 ${missing.join('、')}，` +
        '说明路由没有切换到这个页面（检查 entry-server 是否 await 了 router.push）',
      )
    }

    const outputPath = join(distDir, route.output)
    await mkdir(dirname(outputPath), { recursive: true })
    await writeFile(outputPath, inject(template, route, renderedHtml), 'utf-8')
    console.log(`  dist/${route.output.padEnd(22)} (预渲染 ${route.path}，${renderedHtml.length} 字符)`)
  }

  await rm(join(projectRoot, 'dist-ssr'), { recursive: true, force: true })
  console.log('  已清理 dist-ssr/')
}

main().catch(error => {
  console.error('\n预渲染失败：', error)
  process.exit(1)
})
