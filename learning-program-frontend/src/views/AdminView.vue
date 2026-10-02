<script setup lang="ts">
import {computed} from 'vue'
import {ArrowLeft, Message, UserFilled} from '@element-plus/icons-vue'
import {useRouter, useRoute} from 'vue-router'
import {useI18n} from 'vue-i18n'
import {logout} from '@/net'

const router = useRouter()
const route = useRoute()
const { t } = useI18n()

const navItems = computed(() => [
  {name: 'admin-users', label: t('admin.users'), icon: UserFilled},
  {name: 'admin-notifications', label: t('admin.notifications'), icon: Message},
])

function userLogout() {
  logout(() => router.push({name: 'welcome-login'}))
}
</script>

<template>
  <div class="admin-shell">
    <aside class="admin-sidebar">
      <div class="brand">
        <div class="brand-mark">学</div>
        <span>{{ t('common.appName') }}</span>
      </div>

      <button class="back-button" type="button" @click="router.push({ name: 'index' })">
        <el-icon>
          <ArrowLeft/>
        </el-icon>
        {{ t('admin.backToApp') }}
      </button>

      <nav class="admin-nav">
        <router-link
            v-for="item in navItems"
            :key="item.name"
            :to="{ name: item.name }"
            class="nav-item"
            :class="{ active: route.name === item.name }">
          <el-icon>
            <component :is="item.icon"/>
          </el-icon>
          <span>{{ item.label }}</span>
        </router-link>
      </nav>

      <button class="logout-button" type="button" @click="userLogout">{{ t('common.logout') }}</button>
    </aside>

    <main class="admin-main">
      <router-view/>
    </main>
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

.admin-shell {
  display: flex;
  min-height: 100vh;
  background: var(--el-bg-color-page);
}

.admin-sidebar {
  position: sticky;
  top: 0;
  display: flex;
  flex-direction: column;
  gap: 20px;
  width: 224px;
  height: 100vh;
  padding: 24px 16px;
  border-right: 1px solid var(--el-border-color-light);
  background: var(--el-bg-color);
}

.brand {
  display: flex;
  align-items: center;
  gap: 11px;
  color: var(--el-text-color-primary);
  font-size: 18px;
  font-weight: 700;
}

.brand-mark {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  border-radius: 10px;
  color: var(--el-color-white);
  background: var(--el-color-primary);
  box-shadow: var(--el-box-shadow-light);
}

.back-button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 9px 14px;
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  color: var(--el-text-color-regular);
  background: transparent;
  font: inherit;
  font-size: 13px;
  cursor: pointer;
}

.back-button:hover {
  color: var(--el-color-primary);
  border-color: var(--el-color-primary-light-5);
  background: var(--el-color-primary-light-9);
}

.admin-nav {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border-radius: 10px;
  color: var(--el-text-color-regular);
  background: transparent;
  font-size: 14px;
  text-decoration: none;
  transition: background-color .2s, color .2s;
}

.nav-item:hover {
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.nav-item.active {
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  font-weight: 600;
}

.logout-button {
  padding: 9px 14px;
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  color: var(--el-text-color-regular);
  background: transparent;
  font: inherit;
  font-size: 13px;
  cursor: pointer;
}

.logout-button:hover {
  color: var(--el-color-primary);
  border-color: var(--el-color-primary-light-5);
  background: var(--el-color-primary-light-9);
}

.admin-main {
  flex: 1;
  min-width: 0;
}

@media (max-width: 700px) {
  .admin-shell {
    flex-direction: column;
  }

  .admin-sidebar {
    width: 100%;
    height: auto;
    border-right: none;
    border-bottom: 1px solid var(--el-border-color-light);
  }

  .admin-nav {
    flex-direction: row;
    flex-wrap: wrap;
  }
}
</style>
