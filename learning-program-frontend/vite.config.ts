import { fileURLToPath, URL } from 'node:url'

import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
// https://vite.dev/config/
export default defineConfig(({ mode }) => {
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
    // host: true 监听 0.0.0.0，允许其他 IP 访问开发服务器
    server: {
      host: true,
      proxy,
    },
    preview: {
      host: true,
      proxy,
    },
    // 预渲染要用 `vite build --ssr src/entry-server.ts` 产出一份 Node 端 bundle。
    // element-plus 由 Vite 一起打包（而不是留给 Node 去 import），
    // 否则它在 SSR 运行时会去加载 .css 子路径，Node 直接报错。
    ssr: {
      noExternal: ['element-plus'],
    },
  }
})
