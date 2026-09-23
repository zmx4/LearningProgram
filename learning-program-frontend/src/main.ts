import { createApp } from 'vue'
// @ts-ignore
import App from './App.vue'
// @ts-expect-error - Vue SFC module resolution
import router from '@/router'


const app = createApp(App)

app.use(router)

app.mount('#app')

