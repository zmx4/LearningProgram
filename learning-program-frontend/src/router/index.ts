import { createMemoryHistory, createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { currentRole, unauthorized } from '@/net'

/** 路由表：welcome 为登录注册壳，MainView 承载登录后页面（含侧边栏），AdminView 承载管理员后台。 */
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
            },{
                path: 'forget',
                name: 'welcome-forget',
                component: () => import('@/views/welcome/ForgetPage.vue')
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
        },{
            path: 'words',
            name: 'tests-words',
            component: () => import('@/views/page/tests/WordTestPage.vue'),
            meta: { title: '单词测试' }
        },{
            path: 'knowledge',
            name: 'tests-knowledge',
            component: () => import('@/views/page/tests/KnowledgeTestPage.vue'),
            meta: { title: '知识测试' }
        }]
    },{
        path: '/courses',
        component: () => import('@/views/MainView.vue'),
        children: [{
            path: '',
            name: 'courses',
            component: () => import('@/views/page/CoursesPage.vue'),
            meta: { title: '我的课程' }
        },{
            path: ':id',
            name: 'course-study',
            component: () => import('@/views/page/CourseStudyPage.vue'),
            meta: { title: '课程学习' }
        }]
    },{
        path: '/resources',
        component: () => import('@/views/MainView.vue'),
        children: [{
            path: '',
            name: 'resources',
            component: () => import('@/views/page/ResourcesPage.vue'),
            meta: { title: '学习资源' }
        }]
    },{
        path: '/discussions',
        component: () => import('@/views/MainView.vue'),
        children: [{
            path: '',
            name: 'discussions',
            component: () => import('@/views/page/DiscussionPage.vue'),
            meta: { title: '讨论区' }
        },{
            path: ':id',
            name: 'discussion-detail',
            component: () => import('@/views/page/DiscussionDetailPage.vue'),
            meta: { title: '文章详情' }
        }]
    },{
        path: '/groups',
        component: () => import('@/views/MainView.vue'),
        children: [{
            path: '',
            name: 'groups',
            component: () => import('@/views/page/GroupsPage.vue'),
            meta: { title: '学习小组' }
        },{
            path: ':id',
            name: 'group-detail',
            component: () => import('@/views/page/GroupDetailPage.vue'),
            meta: { title: '小组聊天' }
        },{
            path: ':id/members/:memberId',
            name: 'group-member-detail',
            component: () => import('@/views/page/GroupMemberDetailPage.vue'),
            meta: { title: '成员详情' }
        }]
    },{
        path: '/leaderboard',
        component: () => import('@/views/MainView.vue'),
        children: [{
            path: '',
            name: 'leaderboard',
            component: () => import('@/views/page/LeaderboardPage.vue'),
            meta: { title: '排行榜' }
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
        name: 'admin',
        component: () => import('@/views/AdminView.vue'),
        redirect: { name: 'admin-users' },
        meta: { title: '管理员中心', adminOnly: true },
        children: [{
            path: 'users',
            name: 'admin-users',
            component: () => import('@/views/admin/AdminUsers.vue'),
            meta: { title: '用户管理' }
        },{
            path: 'questions',
            name: 'admin-questions',
            component: () => import('@/views/admin/AdminQuestions.vue'),
            meta: { title: '题目管理' }
        },{
            path: 'sets',
            name: 'admin-sets',
            component: () => import('@/views/admin/AdminQuestionSets.vue'),
            meta: { title: '题集管理' }
        },{
            path: 'courses',
            name: 'admin-courses',
            component: () => import('@/views/admin/AdminCourses.vue'),
            meta: { title: '课程管理' }
        },{
            path: 'resources',
            name: 'admin-resources',
            component: () => import('@/views/admin/AdminResources.vue'),
            meta: { title: '资源管理' }
        },{
            path: 'notifications',
            name: 'admin-notifications',
            component: () => import('@/views/admin/AdminNotifications.vue'),
            meta: { title: '通知管理' }
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

/**
 * 以工厂方式创建路由实例：SSR 时每个请求都要一份独立路由（history 状态不能跨请求共享），
 * 客户端则始终只有一个实例。路由表与守卫逻辑对两者完全一致。
 */
function createAppRouter() {
    const router = createRouter({
        // 服务端没有 window，无法使用基于浏览器 history API 的导航，
        // 用内存 history 承载本次请求的 URL（客户端水合时仍使用 createWebHistory）
        history: import.meta.env.SSR ? createMemoryHistory() : createWebHistory(import.meta.env.BASE_URL),
        routes
    })

    /**
     * 全局路由守卫：受保护路径未登录跳登录页；adminOnly 路由非 admin 拒绝；已登录用户访问 welcome 页跳首页。
     * SSR 下登录态只存在于浏览器 storage（服务端读不到），三类依赖登录态的跳转都无法判断，
     * 统一跳过：服务端按游客视角渲染目标页骨架，水合后客户端守卫会带着真实登录态重新纠正路由。
     */
    router.beforeEach((to) => {
        if (import.meta.env.SSR) {
            return true
        }

        const isUnauthenticated = unauthorized()

        // /index 主界面允许游客浏览（首页有专门的未登录展示），其余页面仍要求登录
        if ((to.path.startsWith('/notifications')
            || to.path.startsWith('/courses') || to.path.startsWith('/resources')
            || to.path.startsWith('/progress') || to.path.startsWith('/profile')
            || to.path.startsWith('/settings') || to.path.startsWith('/tests')
            || to.path.startsWith('/discussions') || to.path.startsWith('/leaderboard')
            || to.path.startsWith('/groups')
            || to.path.startsWith('/admin') || to.path.startsWith('/user/')) && isUnauthenticated) {
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

    // SPA 内导航同步页面标题；首次标题由 SSR 按 meta 生成（见 entry-server.ts）
    router.afterEach((to) => {
        if (typeof document === 'undefined') return
        const title = to.meta.title
        if (typeof title === 'string' && title) {
            document.title = `${title} - 学习平台`
        }
    })

    return router
}

export { createAppRouter }
