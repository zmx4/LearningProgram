import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { unauthorized } from '@/net'

const routes: RouteRecordRaw[] = [
    {
        path: '/',
        name: 'welcome',
        component: () => import('../views/WelcomeView.vue'),
        children: [
            {
                path: '',
                name: 'welcome-login',
                component: () => import('@/views/welcome/LoginPage.vue')
            },{
                path: 'register',
                name: 'welcome-register',
                component: () => import('@/views/welcome/RegisterPage.vue')
            }
        ]
    },{
        path: '/index',
        name: 'index',
        component: () => import('@/views/IndexView.vue')
    }
]

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes
})

router.beforeEach((to) => {
    const isUnauthenticated = unauthorized()

    if (to.path.startsWith('/index') && isUnauthenticated) {
        return { name: 'welcome-login' }
    }

    if (typeof to.name === 'string' && to.name.startsWith('welcome') && !isUnauthenticated) {
        return { name: 'index' }
    }

    return true
})

export default router
