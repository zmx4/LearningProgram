import { createApp } from 'vue'
import App from './App.vue'
import router from '@/router'
import i18n from '@/i18n'
import axios from "axios";

import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'

axios.defaults.baseURL = 'http://127.0.0.1:1231'

const app = createApp(App)

app.use(router)
app.use(i18n)

app.mount('#app')
