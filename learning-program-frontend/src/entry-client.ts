import { createLearningApp } from './app'
import { hasStoredSession } from '@/net'

/**
 * 客户端入口。
 * <p>
 * 预渲染页面（构建期由 scripts/prerender.mjs 直出）会在挂载点上留下
 * `data-prerendered-route`，说明容器里已经有服务端渲染好的 DOM。
 * <p>
 * 预渲染时是在 Node 里跑的，看不到浏览器本地的登录态，渲染出来的一定是「未登录」版本。
 * 所以只有同时满足下面两点才复用这份 DOM 做 hydration：
 * 1. 容器里确实是预渲染结果；
 * 2. 本地没有登录态 —— 否则路由守卫会把 `welcome*` 重定向到首页，与预渲染内容对不上，
 *    这时改用普通 createApp 做一次干净渲染（预渲染 HTML 仍已用于首屏，只是不再复用）。
 * 没有 `data-prerendered-route` 的 SPA 兜底页同样走干净渲染。
 */
const container = document.getElementById('app')!
const hydrateOnMount = container.dataset.prerenderedRoute !== undefined && !hasStoredSession()

const { app, router } = createLearningApp({ hydrateOnMount })

// 等首次导航（含守卫里的重定向）完成再挂载，避免先渲染出错误的页面
await router.isReady()

if (!hydrateOnMount) {
  // createApp 的 mount 是往容器里**追加**节点，而不是替换。
  // 容器里可能还留着预渲染的 HTML（最典型：已登录的用户打开预渲染过的登录页，
  // 守卫把他重定向到首页），必须先清空，否则旧内容会和新渲染结果叠在一起。
  container.textContent = ''
}
app.mount(container)
