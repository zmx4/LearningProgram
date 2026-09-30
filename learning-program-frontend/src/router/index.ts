import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { currentRole, unauthorized } from '@/net'

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
        component: () => import('@/views/MainView.vue'),
        children: [
            {
                path: '',
                name: 'index',
                component: () => import('@/views/page/HomePage.vue')
            }
        ]
    },{
        path: '/notifications',
        component: () => import('@/views/MainView.vue'),
        children: [
            {
                path: '',
                name: 'notifications',
                component: () => import('@/views/page/NotificationPage.vue')
            }
        ]
    },{
        path: '/tests',
        component: () => import('@/views/MainView.vue'),
        children: [{
            path: '',
            name: 'tests',
            component: () => import('@/views/page/TestsPage.vue'),
            meta: { title: '测试中心' }
        }]
    },{
        path: '/courses',
        component: () => import('@/views/MainView.vue'),
        children: [{
            path: '',
            name: 'courses',
            component: () => import('@/views/page/FeaturePage.vue'),
            meta: { title: '我的课程' }
        }]
    },{
        path: '/resources',
        component: () => import('@/views/MainView.vue'),
        children: [{
            path: '',
            name: 'resources',
            component: () => import('@/views/page/FeaturePage.vue'),
            meta: { title: '学习资源' }
        }]
    },{
        path: '/progress',
        component: () => import('@/views/MainView.vue'),
        children: [{
            path: '',
            name: 'progress',
            component: () => import('@/views/page/FeaturePage.vue'),
            meta: { title: '学习进度' }
        }]
    },{
        path: '/profile',
        component: () => import('@/views/MainView.vue'),
        children: [{
            path: '',
            name: 'profile',
            component: () => import('@/views/page/ProfilePage.vue'),
            meta: { title: '个人信息' }
        }]
    },{
        path: '/settings',
        component: () => import('@/views/MainView.vue'),
        children: [{
            path: '',
            name: 'settings',
            component: () => import('@/views/page/SettingsPage.vue'),
            meta: { title: '设置' }
        }]
    },{
        path: '/admin',
        component: () => import('@/views/MainView.vue'),
        children: [{
            path: '',
            name: 'admin',
            component: () => import('@/views/page/AdminPage.vue'),
            meta: { title: '管理员中心', adminOnly: true }
        }]
    },{
        path: '/user/:id',
        component: () => import('@/views/MainView.vue'),
        children: [{
            path: '',
            name: 'user',
            component: () => import('@/views/page/UserPage.vue'),
            meta: { title: '用户信息' }
        }]
    }
]

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes
})

router.beforeEach((to) => {
    const isUnauthenticated = unauthorized()

    if ((to.path.startsWith('/index') || to.path.startsWith('/notifications')
        || to.path.startsWith('/courses') || to.path.startsWith('/resources')
        || to.path.startsWith('/progress') || to.path.startsWith('/profile')
        || to.path.startsWith('/settings') || to.path.startsWith('/tests') || to.path.startsWith('/user/')) && isUnauthenticated) {
        return { name: 'welcome-login' }
    }

    if (to.meta.adminOnly && currentRole() !== 'admin') {
        return { name: 'index' }
    }

    if (typeof to.name === 'string' && to.name.startsWith('welcome') && !isUnauthenticated) {
        return { name: 'index' }
    }

    return true
})

export default router
