import { renderToString } from 'vue/server-renderer'
import { createLearningApp } from './app'

/**
 * 服务端渲染入口：给构建期的预渲染脚本用（scripts/prerender.mjs）。
 * <p>
 * 注意此时运行在 Node 里，没有 localStorage，所以路由守卫看到的是「未登录」状态 ——
 * 只有不依赖登录态的公开页（登录 / 注册）适合预渲染，受保护页面会被守卫重定向到登录页。
 * 涉及浏览器 API 的读取都走 src/utils/storage.ts 兜底，因此这里不会抛错。
 */
export async function render(url: string): Promise<string> {
  const { app, router } = createLearningApp({ hydrateOnMount: true })

  // 必须 await 这次 push：app.use(router) 已经触发过一次「初始导航」，
  // 只等 isReady() 会在这两次导航之间返回，结果每个路由都渲染成初始的 "/"。
  await router.push(url)
  await router.isReady()

  return renderToString(app)
}
