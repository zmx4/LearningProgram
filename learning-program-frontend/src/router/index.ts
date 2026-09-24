import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
// @ts-expect-error `@/net` is currently implemented as JavaScript without declarations.
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

router.beforeEach((to, from,next)=>{
    const isUnauthenticated = unauthorized()
    if(typeof to.name === 'string' && to.name.startsWith('index') && !isUnauthenticated){
        next('/index')
    }else if(to.fullPath.startsWith('/index') && isUnauthenticated){
        next('/')
    }else{
        next()
    }
})

export default router
