<script setup lang="ts">
import {
  ArrowLeft,
  ArrowRight,
  Bell,
  Collection,
  DataAnalysis,
  HomeFilled,
  Reading,
  Setting,
  UserFilled,
} from '@element-plus/icons-vue'
import {computed, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {logout} from '@/net'

const route = useRoute()
const router = useRouter()
const sidebarExpanded = ref(false)

const navigationItems = [
  {name: 'index', label: '首页', icon: HomeFilled},
  {name: 'courses', label: '我的课程', icon: Reading},
  {name: 'resources', label: '学习资源', icon: Collection},
  {name: 'progress', label: '学习进度', icon: DataAnalysis},
]

const activeSection = computed(() => route.name === 'notifications' ? '' : String(route.name ?? 'index'))

function userLogout() {
  logout(() => router.push({name: 'welcome-login'}))
}

function toggleSidebar() {
  sidebarExpanded.value = !sidebarExpanded.value
}
</script>

<template>
  <div class="app-shell">
    <aside
        class="sidebar"
        :class="{ expanded: sidebarExpanded }"
        @mouseenter="sidebarExpanded = true"
        @mouseleave="sidebarExpanded = false"
    >
      <div class="brand">
        <div class="brand-mark">学</div>
        <span>学习平台</span>
        <button
            class="sidebar-toggle"
            type="button"
            :aria-label="sidebarExpanded ? '收起侧边栏' : '展开侧边栏'"
            @click="toggleSidebar"
        >
          <el-icon>
            <ArrowLeft v-if="sidebarExpanded"/>
            <ArrowRight v-else/>
          </el-icon>
        </button>
      </div>

      <nav class="side-nav" aria-label="主导航">
        <button
            v-for="item in navigationItems"
            :key="item.name"
            class="nav-item"
            :class="{ active: activeSection === item.name }"
            type="button"
            @click="router.push({ name: item.name })"
        >
          <el-icon>
            <component :is="item.icon"/>
          </el-icon>
          <span>{{ item.label }}</span>
        </button>
      </nav>

      <div class="sidebar-placeholder" aria-label="导航占位区域">
        <span></span>
        <span></span>
        <span></span>
      </div>

      <div class="sidebar-footer">
        <button class="nav-item" type="button">
          <el-icon>
            <Setting/>
          </el-icon>
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
          <button class="icon-button" type="button" aria-label="通知"
                  @click="router.push({ name: 'notifications' })">
            <el-icon>
              <Bell/>
            </el-icon>
            <span class="notification-dot"></span>
          </button>
          <div class="profile">
            <div class="avatar" aria-label="用户头像">
              <el-icon>
                <UserFilled/>
              </el-icon>
            </div>
            <span class="profile-name">学习者</span>
          </div>
          <button class="logout-button" type="button" @click="userLogout">退出登录</button>
        </div>
      </header>

      <router-view/>
    </section>
  </div>
</template>

<style scoped>
:global(*) {
  box-sizing: border-box;
}

:global(body) {
  margin: 0;
  background: var(--el-bg-color-page);
  color: var(--el-text-color-primary);
  font-family: Inter, "PingFang SC", "Microsoft YaHei", sans-serif;
}

.app-shell {
  display: flex;
  min-height: 100vh;
  background: var(--el-bg-color-page);
}

.sidebar {
  display: flex;
  width: 72px;
  flex: 0 0 72px;
  flex-direction: column;
  position: relative;
  padding: 28px 16px 20px;
  background: var(--el-bg-color);
  border-right: 1px solid var(--el-border-color-light);
  transition: width .2s ease, flex-basis .2s ease;
}

.sidebar.expanded {
  width: 248px;
  flex-basis: 248px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 11px;
  min-height: 34px;
  min-width: 34px;
  padding: 0 6px;
  color: var(--el-text-color-primary);
  font-size: 18px;
  font-weight: 700;
  white-space: nowrap;
}

.brand-mark {
  display: grid;
  width: 34px;
  height: 34px;
  flex: 0 0 34px;
  place-items: center;
  border-radius: 10px;
  color: var(--el-color-white);
  background: var(--el-color-primary);
  box-shadow: var(--el-box-shadow-light);
}

.brand > span, .sidebar .nav-item span {
  opacity: 0;
  transition: opacity .12s ease;
}

.sidebar.expanded .brand > span, .sidebar.expanded .nav-item span {
  opacity: 1;
}

.sidebar-toggle {
  display: grid;
  width: 28px;
  height: 28px;
  margin-left: auto;
  place-items: center;
  border: 0;
  border-radius: 7px;
  color: var(--el-text-color-secondary);
  background: transparent;
  cursor: pointer;
}

.sidebar-toggle:hover {
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.sidebar:not(.expanded) .brand > span,
.sidebar:not(.expanded) .nav-item span,
.sidebar:not(.expanded) .sidebar-placeholder {
  display: none;
}

.sidebar:not(.expanded) .sidebar-toggle {
  position: absolute;
  top: 70px;
  left: 50%;
  width: 22px;
  height: 22px;
  margin: 0;
  transform: translateX(-50%);
  opacity: 0;
}

.sidebar:hover:not(.expanded) .sidebar-toggle {
  opacity: 1;
}

.sidebar-placeholder {
  display: grid;
  gap: 9px;
  margin: 24px 13px 24px;
}

.sidebar-placeholder span {
  width: 100%;
  height: 9px;
  border-radius: 5px;
  background: var(--el-fill-color-light);
}

.sidebar-placeholder span:nth-child(2) {
  width: 72%;
}

.sidebar-placeholder span:nth-child(3) {
  width: 86%;
}

.side-nav {
  margin-top: 10px;
  display: grid;
  gap: 7px;
}

.nav-item {
  display: flex;
  height: 45px;
  align-items: center;
  gap: 20px;
  width: 100%;
  padding: 12px 14px;
  border: 0;
  border-radius: 10px;
  color: var(--el-text-color-secondary);
  background: transparent;
  font: inherit;
  font-size: 14px;
  text-align: left;
  cursor: pointer;
  transition: .2s;
}

.nav-item span {
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.nav-item:hover, .nav-item.active {

  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.nav-item.active {
  font-weight: 600;
}

.nav-item .el-icon {
  width: 18px;
  height: 18px;
  flex: 0 0 18px;
  font-size: 18px;
}

.sidebar-footer {
  margin-top: auto;
  padding-top: 20px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.page {
  min-width: 0;
  flex: 1;
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 100px;
  padding: 22px 5%;
  background: var(--el-bg-color);
  border-bottom: 1px solid var(--el-border-color-light);
}

.eyebrow {
  margin: 0 0 7px;
  color: var(--el-text-color-placeholder);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: .13em;
}

.topbar h1 {
  margin: 0;
  font-size: 24px;
}

.topbar-actions {
  display: flex;
  align-items: center;
  gap: 22px;
}

.icon-button, .logout-button {
  border: 0;
  background: transparent;
  cursor: pointer;
}

.icon-button {
  position: relative;
  display: grid;
  place-items: center;
  color: var(--el-text-color-secondary);
  font-size: 20px;
}

.notification-dot {
  position: absolute;
  top: -1px;
  right: -2px;
  width: 6px;
  height: 6px;
  border: 1px solid var(--el-color-white);
  border-radius: 50%;
  background: var(--el-color-danger);
}

.profile {
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  border-radius: 50%;
  color: var(--el-color-white);
  background: var(--el-color-info);
  font-size: 18px;
}

.profile-name {
  color: var(--el-text-color-regular);
  font-size: 14px;
}

.logout-button {
  padding: 9px 14px;
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  color: var(--el-text-color-regular);
  font-size: 13px;
}

.logout-button:hover {
  color: var(--el-color-primary);
  border-color: var(--el-color-primary-light-5);
  background: var(--el-color-primary-light-9);
}

@media (max-width: 760px) {
  .sidebar {
    width: 64px;
    flex-basis: 64px;
    padding: 22px 10px;
  }

  .sidebar.expanded {
    width: 220px;
    flex-basis: 220px;
  }

  .brand {
    padding: 0;
  }

  .sidebar:not(.expanded) .sidebar-toggle {
    top: 64px;
  }

  .nav-item {
    padding: 13px 14px;
  }

  .topbar {
    align-items: flex-start;
    gap: 16px;
  }

  .profile-name {
    display: none;
  }

  .topbar-actions {
    gap: 12px;
  }
}
</style>
