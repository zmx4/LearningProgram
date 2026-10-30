/**
 * 应用工厂：客户端与服务端两个入口共享的组装逻辑。
 *
 * - 客户端入口（main.ts）：创建实例后挂载到 #app，与 SSR 构建的水合共用同一份代码
 * - 服务端入口（entry-server.ts）：每个 HTTP 请求创建一份新实例（路由 history 不能复用）
 *
 * 全局样式在这里导入（而不是某个入口），保证客户端构建与 SSR 渲染走的模块链一致。
 */
import { createSSRApp } from 'vue'
import { ID_INJECTION_KEY, ZINDEX_INJECTION_KEY } from 'element-plus'
import axios from 'axios'
import App from './App.vue'
import { createAppRouter } from '@/router'
import { setNetRouter } from '@/net'
import i18n from '@/i18n'

// Element Plus 样式按需引入：模板里的 el-* 组件由 unplugin-vue-components 自动注入样式，
// 这里只补编程式调用的服务组件（ElMessage / ElMessageBox / v-loading），
// 它们不经过模板解析，不显式引入会丢失样式。
// 不要再引入 element-plus/dist/index.css（全量约 360KB，会与按需样式重复）。
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'
import 'element-plus/es/components/loading/style/css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import '@/assets/markdown.css'
import '@/assets/common.css'

// 默认同源相对路径：开发时由 vite 代理 /api，生产时由反向代理转发。
// 前后端确实不同源时，构建前设置 VITE_API_BASE_URL 覆盖（见 .env.example）。
axios.defaults.baseURL = import.meta.env.VITE_API_BASE_URL ?? ''

export function createAppInstance() {
    // createSSRApp 在客户端挂载到空容器时等价于 createApp（正常客户端渲染），
    // 挂载到 SSR 输出的 HTML 时自动走水合，因此两种入口统一用它。
    const app = createSSRApp(App)
    app.provide(ID_INJECTION_KEY, {
        prefix: 1024,
        current: 0,
    })
    app.provide(ZINDEX_INJECTION_KEY, {current: 0})

    const router = createAppRouter()
    app.use(router)
    app.use(i18n)

    // 供 net 层 401 过期时跳转登录页使用（见 src/net/index.ts 的说明）
    setNetRouter(router)

    return { app, router }
}
