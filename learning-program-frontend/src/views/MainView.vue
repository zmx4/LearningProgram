<script setup lang="ts">
import {
  ArrowLeft,
  ArrowRight,
  Bell,
  Avatar,
  Collection,
  DataAnalysis,
  HomeFilled,
  Reading,
  Setting,
  Notebook,
  UserFilled,
} from '@element-plus/icons-vue'
import { computed, onMounted, onUnmounted, ref } from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {currentRole, currentUsername, get, logout} from '@/net'
import { useI18n } from 'vue-i18n'
import {
  dailyWordSettingsChangedEvent,
  readDailyWordSettings,
  type DailyWordSettings,
  type DailyWordSource,
} from '@/utils/dailyWord'

interface DailyWord {
  id: number
  word: string
  translation: string | null
}

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const sidebarExpanded = ref(false)
const username = ref(currentUsername() || t('common.user'))
const isAdmin = currentRole() === 'admin'
const dailyWordSettings = ref<DailyWordSettings>(readDailyWordSettings())
const dailyWord = ref<DailyWord | null>(null)
const dailyWordLoading = ref(false)

const navigationItems = [
  {name: 'index', label: 'navigation.home', icon: HomeFilled},
  {name: 'courses', label: 'navigation.courses', icon: Reading},
  {name: 'resources', label: 'navigation.resources', icon: Collection},
  {name: 'progress', label: 'navigation.progress', icon: DataAnalysis},
]

const activeSection = computed(() => route.name === 'notifications' || route.name === 'profile' || route.name === 'settings' ? '' : String(route.name ?? 'index'))

function userLogout() {
  logout(() => router.push({name: 'welcome-login'}))
}

function toggleSidebar() {
  sidebarExpanded.value = !sidebarExpanded.value
}

function dailyWordCacheKey(source: DailyWordSource): string {
  return `learning-daily-word-${source}-${new Date().toISOString().slice(0, 10)}`
}

function loadDailyWord() {
  dailyWord.value = null
  if (!dailyWordSettings.value.enabled) return

  const cacheKey = dailyWordCacheKey(dailyWordSettings.value.source)
  const cached = localStorage.getItem(cacheKey)
  if (cached) {
    try {
      dailyWord.value = JSON.parse(cached) as DailyWord
      return
    } catch {
      localStorage.removeItem(cacheKey)
    }
  }

  dailyWordLoading.value = true
  get<DailyWord[]>(
    `/api/dictionary/${dailyWordSettings.value.source}?count=1`,
    (words) => {
      dailyWordLoading.value = false
      const word = words[0]
      if (!word) return
      dailyWord.value = word
      localStorage.setItem(cacheKey, JSON.stringify(word))
    },
    () => {
      dailyWordLoading.value = false
    },
  )
}

function onDailyWordSettingsChanged(event: Event) {
  const settings = (event as CustomEvent<DailyWordSettings>).detail
  dailyWordSettings.value = settings
  loadDailyWord()
}

onMounted(() => {
  loadDailyWord()
  window.addEventListener(dailyWordSettingsChangedEvent, onDailyWordSettingsChanged)
})

onUnmounted(() => {
  window.removeEventListener(dailyWordSettingsChangedEvent, onDailyWordSettingsChanged)
})
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
        <span>{{ t('common.appName') }}</span>
        <button
            class="sidebar-toggle"
            type="button"
            :aria-label="sidebarExpanded ? t('common.collapseSidebar') : t('common.expandSidebar')"
            @click="toggleSidebar"
        >
          <el-icon>
            <ArrowLeft v-if="sidebarExpanded"/>
            <ArrowRight v-else/>
          </el-icon>
        </button>
      </div>

      <nav class="side-nav" :aria-label="t('common.mainNavigation')">
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
          <span>{{ t(item.label) }}</span>
        </button>
        <button
            v-if="isAdmin"
            class="nav-item"
            :class="{ active: activeSection === 'admin' }"
            type="button"
            @click="router.push({ name: 'admin' })"
        >
          <el-icon><Avatar/></el-icon>
          <span>{{ t('navigation.admin') }}</span>
        </button>
      </nav>

      <div
          v-if="dailyWordSettings.enabled"
          class="daily-word"
          :aria-label="t('dailyWord.title')"
      >
        <div class="daily-word-heading">
          <el-icon><Notebook /></el-icon>
          <span>{{ t('dailyWord.title') }}</span>
          <small>{{ dailyWordSettings.source.toUpperCase() }}</small>
        </div>
        <template v-if="dailyWord">
          <strong>{{ dailyWord.word }}</strong>
          <p>{{ dailyWord.translation || t('dailyWord.noTranslation') }}</p>
        </template>
        <p v-else class="daily-word-status">
          {{ dailyWordLoading ? t('dailyWord.loading') : t('dailyWord.unavailable') }}
        </p>
      </div>

      <div class="sidebar-footer">
        <button class="nav-item" type="button" :class="{ active: activeSection === 'settings' }"
                @click="router.push({ name: 'settings' })">
          <el-icon>
            <Setting/>
          </el-icon>
          <span>{{ t('common.settings') }}</span>
        </button>
      </div>
    </aside>

    <section class="page">
      <header class="topbar">
        <div>
          <p class="eyebrow">LEARNING SPACE</p>
          <h1>{{ t('common.welcomeBack') }}</h1>
        </div>
        <div class="topbar-actions">
          <button class="icon-button" type="button" :aria-label="t('common.notifications')"
                  @click="router.push({ name: 'notifications' })">
            <el-icon>
              <Bell/>
            </el-icon>
            <span class="notification-dot"></span>
          </button>
          <button class="profile" type="button" :aria-label="t('common.openProfile')" @click="router.push({ name: 'profile' })">
            <div class="avatar" :aria-label="t('common.userAvatar')">
              <el-icon>
                <UserFilled/>
              </el-icon>
            </div>
            <span class="profile-name">{{ username }}</span>
          </button>
          <button class="logout-button" type="button" @click="userLogout">{{ t('common.logout') }}</button>
        </div>
      </header>

      <router-view v-slot="{ Component }">
        <transition name="page-fade-slide" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
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
  height: 100vh;
  min-height: 100vh;
  flex-direction: column;
  position: sticky;
  top: 0;
  align-self: flex-start;
  padding: 28px 16px 20px;
  background: var(--el-bg-color);
  border-right: 1px solid var(--el-border-color-light);
  overflow: hidden;
  transition: width .2s ease, flex-basis .2s ease;
}

.sidebar.expanded {
  width: 248px;
  flex-basis: 248px;
  overflow-x: hidden;
  overflow-y: auto;
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
.sidebar:not(.expanded) .nav-item span {
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

.daily-word {
  display: grid;
  gap: 8px;
  max-height: 180px;
  margin: 24px 13px;
  padding: 14px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 13px;
  background: var(--el-fill-color-lighter);
  overflow: hidden;
  opacity: 1;
  transform: translateY(0);
  visibility: visible;
  transition:
    max-height .2s ease,
    margin .2s ease,
    padding .2s ease,
    border-width .2s ease,
    opacity .12s ease .08s,
    transform .2s ease .08s,
    visibility 0s linear .08s;
}

.sidebar:not(.expanded) .daily-word {
  max-height: 0;
  margin-top: 0;
  margin-bottom: 0;
  padding-top: 0;
  padding-bottom: 0;
  border-width: 0;
  opacity: 0;
  transform: translateY(-6px);
  visibility: hidden;
  transition:
    max-height .2s ease,
    margin .2s ease,
    padding .2s ease,
    border-width .2s ease,
    opacity .08s ease,
    transform .2s ease,
    visibility 0s linear .2s;
}

.daily-word-heading {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--el-text-color-secondary);
  font-size: 11px;
  font-weight: 700;
}

.daily-word-heading small {
  margin-left: auto;
  color: var(--el-color-primary);
  font-size: 10px;
}

.daily-word strong {
  overflow: hidden;
  color: var(--el-text-color-primary);
  font-size: 19px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.daily-word p {
  margin: 0;
  overflow: hidden;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.daily-word .daily-word-status {
  color: var(--el-text-color-placeholder);
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

.page-fade-slide-enter-active,
.page-fade-slide-leave-active {
  transition: opacity .2s ease, transform .2s ease;
}

.page-fade-slide-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.page-fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-6px);
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
  padding: 0;
  border: 0;
  background: transparent;
  cursor: pointer;
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
