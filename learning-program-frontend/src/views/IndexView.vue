<script setup lang="ts">
import { ref } from 'vue'
import {
  Bell,
  Reading,
  Collection,
  DataAnalysis,
  HomeFilled,
  Setting,
  UserFilled,
} from '@element-plus/icons-vue'
import { logout } from '@/net'
import router from '@/router'

const activeSection = ref('home')

const navigationItems = [
  { id: 'home', label: '首页', icon: HomeFilled },
  { id: 'courses', label: '我的课程', icon: Reading },
  { id: 'resources', label: '学习资源', icon: Collection },
  { id: 'progress', label: '学习进度', icon: DataAnalysis },
]

function userLogout() {
  logout(() => router.push('/'))
}
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-mark">学</div>
        <span>学习平台</span>
      </div>

      <div class="sidebar-placeholder" aria-label="导航占位区域">
        <span></span>
        <span></span>
        <span></span>
      </div>

      <nav class="side-nav" aria-label="主导航">
        <button
          v-for="item in navigationItems"
          :key="item.id"
          class="nav-item"
          :class="{ active: activeSection === item.id }"
          type="button"
          @click="activeSection = item.id"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </button>
      </nav>

      <div class="sidebar-footer">
        <button class="nav-item" type="button">
          <el-icon><Setting /></el-icon>
          <span>设置</span>
        </button>
      </div>
    </aside>

    <section class="page">
      <header class="topbar">
        <div>
          <p class="eyebrow">LEARNING SPACE</p>
          <h1>你好，欢迎回来</h1>
        </div>
        <div class="topbar-actions">
          <button class="icon-button" type="button" aria-label="通知">
            <el-icon><Bell /></el-icon>
            <span class="notification-dot"></span>
          </button>
          <div class="profile">
            <div class="avatar" aria-label="用户头像"><el-icon><UserFilled /></el-icon></div>
            <span class="profile-name">学习者</span>
          </div>
          <button class="logout-button" type="button" @click="userLogout">退出登录</button>
        </div>
      </header>

      <main class="content">
        <div class="hero-card">
          <div>
            <p class="card-label">今日学习计划</p>
            <h2>保持专注，持续进步</h2>
            <p class="hero-copy">安排好今天的学习时间，让每一次积累都离目标更近一步。</p>
            <button class="primary-button" type="button" @click="activeSection = 'courses'">开始学习</button>
          </div>
          <div class="hero-decoration" aria-hidden="true">
            <div class="decoration-circle circle-large"></div>
            <div class="decoration-circle circle-small"></div>
            <span>✦</span>
          </div>
        </div>

        <div class="section-heading">
          <div>
            <p class="card-label">概览</p>
            <h2>学习概况</h2>
          </div>
          <span class="placeholder-chip">数据即将上线</span>
        </div>

        <div class="stats-grid">
          <article v-for="stat in [
            { label: '进行中的课程', value: '—', hint: '等待课程数据' },
            { label: '本周学习时长', value: '—', hint: '等待学习记录' },
            { label: '完成的课程', value: '—', hint: '等待完成数据' },
          ]" :key="stat.label" class="stat-card">
            <p>{{ stat.label }}</p>
            <strong>{{ stat.value }}</strong>
            <span>{{ stat.hint }}</span>
          </article>
        </div>

        <div class="empty-panel">
          <div class="empty-icon"><el-icon><Reading /></el-icon></div>
          <h3>准备好开始你的学习了吗？</h3>
          <p>课程、学习资源和进度信息将在这里展示。</p>
        </div>
      </main>
    </section>
  </div>
</template>

<style scoped>
:global(*) { box-sizing: border-box; }
:global(body) { background: #f6f8fc; color: #172033; font-family: Inter, "PingFang SC", "Microsoft YaHei", sans-serif; }
.app-shell { display: flex; min-height: 100vh; background: #f6f8fc; }
.sidebar { width: 248px; flex: 0 0 248px; display: flex; flex-direction: column; padding: 28px 16px 20px; background: #fff; border-right: 1px solid #edf0f6; }
.brand { display: flex; align-items: center; gap: 11px; padding: 0 13px; color: #182338; font-size: 18px; font-weight: 700; }
.brand-mark { display: grid; width: 34px; height: 34px; place-items: center; border-radius: 10px; color: #fff; background: #5d6df5; box-shadow: 0 7px 15px #5d6df540; }
.sidebar-placeholder { display: grid; gap: 9px; margin: 42px 13px 24px; }
.sidebar-placeholder span { width: 100%; height: 9px; border-radius: 5px; background: #f0f2f8; }
.sidebar-placeholder span:nth-child(2) { width: 72%; }
.sidebar-placeholder span:nth-child(3) { width: 86%; }
.side-nav { display: grid; gap: 7px; }
.nav-item { display: flex; align-items: center; gap: 14px; width: 100%; padding: 12px 14px; border: 0; border-radius: 10px; color: #7b8497; background: transparent; font: inherit; font-size: 14px; text-align: left; cursor: pointer; transition: .2s; }
.nav-item:hover { color: #5d6df5; background: #f4f5ff; }
.nav-item.active { color: #5d6df5; background: #eef0ff; font-weight: 600; }
.nav-item .el-icon { font-size: 18px; }
.sidebar-footer { margin-top: auto; padding-top: 20px; border-top: 1px solid #f0f2f6; }
.page { min-width: 0; flex: 1; }
.topbar { display: flex; align-items: center; justify-content: space-between; min-height: 100px; padding: 22px 5%; background: #fff; border-bottom: 1px solid #edf0f6; }
.eyebrow, .card-label { margin: 0 0 7px; color: #9aa2b2; font-size: 11px; font-weight: 700; letter-spacing: .13em; }
h1, h2, h3, p { margin-top: 0; }
h1 { margin-bottom: 0; font-size: 24px; }
.topbar-actions { display: flex; align-items: center; gap: 22px; }
.icon-button, .logout-button { border: 0; background: transparent; cursor: pointer; }
.icon-button { position: relative; display: grid; place-items: center; color: #7d8799; font-size: 20px; }
.notification-dot { position: absolute; top: -1px; right: -2px; width: 6px; height: 6px; border: 1px solid #fff; border-radius: 50%; background: #f16d7a; }
.profile { display: flex; align-items: center; gap: 10px; }
.avatar { display: grid; width: 36px; height: 36px; place-items: center; border-radius: 50%; color: #fff; background: #a8b0d9; font-size: 18px; }
.profile-name { color: #4e596d; font-size: 14px; }
.logout-button { padding: 9px 14px; border: 1px solid #e5e8ef; border-radius: 8px; color: #697386; font-size: 13px; }
.logout-button:hover { color: #5d6df5; border-color: #cdd2ff; background: #f7f8ff; }
.content { max-width: 1180px; margin: 0 auto; padding: 42px 5% 60px; }
.hero-card { position: relative; display: flex; min-height: 224px; align-items: center; justify-content: space-between; overflow: hidden; padding: 34px 42px; border-radius: 18px; color: #fff; background: linear-gradient(120deg, #6270f5, #7783f7); box-shadow: 0 16px 30px #6270f526; }
.hero-card h2 { margin-bottom: 10px; font-size: 28px; }
.hero-copy { max-width: 470px; margin-bottom: 24px; color: #e9ebff; font-size: 14px; line-height: 1.7; }
.primary-button { padding: 11px 20px; border: 0; border-radius: 8px; color: #5d6df5; background: #fff; font: inherit; font-size: 13px; font-weight: 600; cursor: pointer; }
.hero-decoration { position: relative; width: 190px; height: 160px; margin-right: 5%; opacity: .8; }
.decoration-circle { position: absolute; border: 1px solid #ffffff50; border-radius: 50%; }
.circle-large { inset: -20px -15px 0 15px; }
.circle-small { inset: 25px 30px 35px 55px; }
.hero-decoration span { position: absolute; top: 55px; left: 96px; font-size: 35px; }
.section-heading { display: flex; align-items: end; justify-content: space-between; margin: 42px 0 18px; }
.section-heading h2 { margin-bottom: 0; font-size: 20px; }
.placeholder-chip { padding: 7px 11px; border-radius: 20px; color: #8791a5; background: #eef1f7; font-size: 12px; }
.stats-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 18px; }
.stat-card { padding: 22px 24px; border: 1px solid #edf0f6; border-radius: 14px; background: #fff; }
.stat-card p { margin-bottom: 18px; color: #788296; font-size: 13px; }
.stat-card strong { display: block; margin-bottom: 9px; color: #27334b; font-size: 30px; }
.stat-card span { color: #a2aabb; font-size: 12px; }
.empty-panel { display: grid; justify-items: center; margin-top: 18px; padding: 48px 20px; border: 1px dashed #dfe4ee; border-radius: 14px; background: #fafbfe; text-align: center; }
.empty-icon { display: grid; width: 50px; height: 50px; place-items: center; margin-bottom: 16px; border-radius: 14px; color: #7582e8; background: #eef0ff; font-size: 24px; }
.empty-panel h3 { margin-bottom: 8px; font-size: 16px; }
.empty-panel p { margin-bottom: 0; color: #929bad; font-size: 13px; }
@media (max-width: 760px) {
  .sidebar { width: 70px; flex-basis: 70px; padding: 22px 10px; }
  .brand { justify-content: center; padding: 0; }
  .brand span, .nav-item span, .sidebar-placeholder { display: none; }
  .nav-item { justify-content: center; padding: 13px 0; }
  .topbar { align-items: flex-start; gap: 16px; }
  .profile-name { display: none; }
  .topbar-actions { gap: 12px; }
  .hero-card { padding: 28px; }
  .hero-decoration { display: none; }
  .stats-grid { grid-template-columns: 1fr; }
}
</style>
