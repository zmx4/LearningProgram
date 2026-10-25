/**
 * 客户端入口：index.html 与 SPA / SSR 两种构建共用。
 *
 * - SPA 构建（vite build）：挂载到空 #app，普通客户端渲染
 * - SSR 构建：挂载到服务端输出的 #app HTML，自动水合接管交互
 *
 * 先等路由完成首次导航再挂载，保证水合时客户端路由与服务端渲染的路由一致。
 */
import { createAppInstance } from './create-app'

const { app, router } = createAppInstance()

void router.isReady().then(() => {
    app.mount('#app')
})
