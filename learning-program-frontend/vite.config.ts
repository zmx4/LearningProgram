import { fileURLToPath, URL } from 'node:url'

import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  // SSR 客户端构建（pnpm build:ssr:client，--mode ssr）才需要产物清单：
  // 生产 SSR 据此只注入当前路由用到的 CSS，而不是把所有路由样式都塞进首屏 HTML。
  // 普通 SPA 构建不生成 .vite/*.json，避免把构建内部信息带进发布目录。
  const isSsrClientBuild = mode === 'ssr'
  // 后端地址只在运行 dev / preview 的机器上生效：浏览器始终请求同源 /api，
  // 由这里转发到后端，所以用局域网 IP 或域名打开前端都不需要改前端代码。
  const proxy = {
    '/api': {
      target: loadEnv(mode, process.cwd()).VITE_DEV_API_TARGET || 'http://127.0.0.1:1231',
      changeOrigin: true,
    },
  }

  return {
    plugins: [
      vue(),
      // vueDevTools(),
      AutoImport({
        resolvers: [ElementPlusResolver()],
      }),
      Components({
        resolvers: [ElementPlusResolver()],
      }),
    ],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
    // 应用只用 Composition API（legacy: false），关掉 vue-i18n 的 Options API 兼容代码和
    // 生产环境 devtools 桥接，减少初始包里用不到的代码。
    define: {
      __VUE_I18N_LEGACY_API__: false,
      __INTLIFY_PROD_DEVTOOLS__: false,
    },
    build: isSsrClientBuild
      ? { ssrManifest: true, manifest: true }
      : {},
    // SSR（dev:ssr / build:ssr）时 element-plus 必须经 vite 处理：
    // unplugin 自动注入的组件样式（element-plus/es/components/**/style/css）内部含 .css 导入，
    // 若按默认外部化交给 Node 直接加载，会因 Node 无法解析 .css 而报错。
    ssr: {
      noExternal: ['element-plus'],
    },
    // host: true 监听 0.0.0.0，允许其他 IP 访问开发服务器
    server: {
      host: true,
      proxy,
    },
    preview: {
      host: true,
      proxy,
    },
  }
})
