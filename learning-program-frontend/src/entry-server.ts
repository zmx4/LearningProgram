/**
 * 服务端渲染入口：由 server.mjs 调用（开发态经 vite.ssrLoadModule 加载，生产态为 dist/ssr/server 的打包产物）。
 *
 * 每个请求创建独立的应用与路由实例，渲染出 HTML 片段交给 server.mjs 拼进页面模板。
 * 登录态存于浏览器 storage，服务端读不到，因此恒按游客视角渲染，
 * 登录用户水合后由客户端状态接管（详见 README「SSR 说明」）。
 */
import { renderToString } from 'vue/server-renderer'
import { createAppInstance } from './create-app'

export async function render(url: string): Promise<{ html: string, title: string | null, modules: string[] }> {
    const { app, router } = createAppInstance()

    await router.push(url)
    await router.isReady()

    // 就近取匹配路由链上最后一个 meta.title（子路由覆盖父路由）
    let title: string | null = null
    for (const record of router.currentRoute.value.matched) {
        if (typeof record.meta.title === 'string' && record.meta.title) {
            title = record.meta.title
        }
    }

    // SFC 编译产物会把自身模块登记到 SSR context.modules，
    // server.mjs 用这份清单 + 构建产物 manifest.json 找出本次渲染真正用到的路由样式。
    const context: { modules?: Set<string> } = {}
    const html = await renderToString(app, context)
    return { html, title, modules: [...(context.modules ?? [])] }
}
