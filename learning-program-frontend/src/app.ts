import { createApp, createSSRApp, type App } from 'vue'
import { ID_INJECTION_KEY, ZINDEX_INJECTION_KEY } from 'element-plus'
import AppComponent from './App.vue'
import router from '@/router'
import i18n from '@/i18n'
import axios from 'axios'

import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import '@/assets/markdown.css'
import '@/assets/common.css'

// 默认同源相对路径：开发时由 vite 代理 /api，生产时由反向代理转发。
// 前后端确实不同源时，构建前设置 VITE_API_BASE_URL 覆盖（见 .env.example）。
axios.defaults.baseURL = import.meta.env.VITE_API_BASE_URL ?? ''

export interface LearningAppOptions {
  /**
   * 用 createSSRApp 创建，于是 `mount()` 会去**复用**已有的服务端/预渲染 DOM（hydration）。
   * 服务端渲染时始终为 true；客户端只在容器里确实是匹配的预渲染结果时才打开 ——
   * 否则会拿一份对不上的 DOM 去做 hydration。
   */
  hydrateOnMount?: boolean
}

/**
 * 创建应用实例。客户端与服务端共用同一个入口，避免两端渲染出不同的组件树。
 */
export function createLearningApp(options: LearningAppOptions = {}) {
  const app: App<Element> = options.hydrateOnMount ? createSSRApp(AppComponent) : createApp(AppComponent)
  app.use(router)
  app.use(i18n)
  // Element Plus 的表单控件会生成随机 id，两端不一致会导致 hydration 报错。
  // 固定 prefix 并让两端从同一个计数器开始，服务端与客户端生成的 id 就完全一致。
  app.provide(ID_INJECTION_KEY, { prefix: 1024, current: 0 })
  app.provide(ZINDEX_INJECTION_KEY, { current: 0 })
  return { app, router, i18n }
}
