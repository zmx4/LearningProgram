import {createApp} from 'vue'
import {ID_INJECTION_KEY, ZINDEX_INJECTION_KEY} from 'element-plus'
import App from './App.vue'
import router from '@/router'
import i18n from '@/i18n'
import axios from "axios";

import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import '@/assets/markdown.css'
import '@/assets/common.css'

// 默认同源相对路径：开发时由 vite 代理 /api，生产时由反向代理转发。
// 前后端确实不同源时，构建前设置 VITE_API_BASE_URL 覆盖（见 .env.example）。
axios.defaults.baseURL = import.meta.env.VITE_API_BASE_URL ?? ''

const app = createApp(App)
app.provide(ID_INJECTION_KEY, {
    prefix: 1024,
    current: 0,
})
app.provide(ZINDEX_INJECTION_KEY, {current: 0})
app.use(router)
app.use(i18n)

app.mount('#app')
