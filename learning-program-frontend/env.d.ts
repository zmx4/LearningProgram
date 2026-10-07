/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** 前端直连的后端地址；留空表示与前端同源 */
  readonly VITE_API_BASE_URL?: string
  /** dev / preview 时 vite 代理 /api 的后端地址 */
  readonly VITE_DEV_API_TARGET?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}
