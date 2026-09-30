<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Message } from '@element-plus/icons-vue'
import { get, post } from '@/net'
import { useI18n } from 'vue-i18n'

interface NotificationItem {
  id: number
  title: string
  content: string
  type: string
  read: boolean
  createdAt: string
}

interface NotificationResponse {
  items: NotificationItem[]
  unreadCount: number
}

const notifications = ref<NotificationItem[]>([])
const unreadCount = ref(0)
const loading = ref(true)
const { t } = useI18n()

function loadNotifications() {
  loading.value = true
  get<NotificationResponse>('/api/notifications', (data) => {
    notifications.value = data.items
    unreadCount.value = data.unreadCount
    loading.value = false
  }, () => {
    loading.value = false
  })
}

function markRead(notification: NotificationItem) {
  if (notification.read) return
  post<void>(`/api/notifications/${notification.id}/read`, null, () => {
    notification.read = true
    unreadCount.value = Math.max(0, unreadCount.value - 1)
  })
}

function markAllRead() {
  if (unreadCount.value === 0) return
  post<void>('/api/notifications/read-all', null, () => {
    notifications.value.forEach((notification) => { notification.read = true })
    unreadCount.value = 0
  })
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}

onMounted(loadNotifications)
</script>

<template>
  <div class="notification-page">
    <main class="notification-content">
      <div class="summary">
        <h2>{{ t('notifications.all') }}</h2>
        <button class="read-all-button" type="button" :disabled="unreadCount === 0" @click="markAllRead">
          {{ t('notifications.markAllRead') }}
        </button>
      </div>

      <div v-loading="loading" class="notification-list">
        <article
          v-for="notification in notifications"
          :key="notification.id"
          class="notification-card"
          :class="{ unread: !notification.read }"
          @click="markRead(notification)"
        >
          <div class="message-icon"><el-icon><Message /></el-icon></div>
          <div class="message-body">
            <div class="message-heading">
              <h3>{{ notification.title }}</h3>
              <span v-if="!notification.read" class="unread-label">{{ t('notifications.unread') }}</span>
            </div>
            <p>{{ notification.content }}</p>
            <time>{{ formatDate(notification.createdAt) }}</time>
          </div>
        </article>

        <el-empty v-if="!loading && notifications.length === 0" :description="t('notifications.empty')" />
      </div>
    </main>
  </div>
</template>

<style scoped>
:global(*) { box-sizing: border-box; }
:global(body) {
  margin: 0;
  color: var(--el-text-color-primary);
  background: var(--el-bg-color-page);
  font-family: Inter, "PingFang SC", "Microsoft YaHei", sans-serif;
}
.notification-page { min-height: 100vh; }
.notification-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 100px;
  padding: 22px 7%;
  background: var(--el-bg-color);
  border-bottom: 1px solid var(--el-border-color-light);
}
.back-button, .read-all-button {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  border: 0;
  color: var(--el-text-color-secondary);
  background: transparent;
  font: inherit;
  font-size: 13px;
  cursor: pointer;
}
.back-button:hover, .read-all-button:not(:disabled):hover { color: var(--el-color-primary); }
.read-all-button:disabled { color: var(--el-text-color-disabled); cursor: not-allowed; }
.header-title { display: flex; align-items: center; gap: 13px; }
.title-icon {
  display: grid;
  width: 40px;
  height: 40px;
  place-items: center;
  border-radius: 12px;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  font-size: 20px;
}
.eyebrow { margin: 0 0 4px; color: var(--el-text-color-placeholder); font-size: 10px; font-weight: 700; letter-spacing: .12em; }
h1, h2, h3, p { margin-top: 0; }
h1 { margin-bottom: 0; font-size: 22px; }
.notification-content { max-width: 880px; margin: 0 auto; padding: 42px 5% 60px; }
.summary { display: flex; align-items: baseline; justify-content: space-between; margin-bottom: 18px; }
.summary h2 { margin-bottom: 0; font-size: 20px; }
.summary span { color: var(--el-color-primary); font-size: 13px; }
.notification-list { min-height: 220px; }
.notification-card {
  display: flex;
  gap: 16px;
  margin-bottom: 12px;
  padding: 20px 22px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 14px;
  background: var(--el-bg-color);
  cursor: pointer;
  transition: border-color .2s, box-shadow .2s;
}
.notification-card:hover { border-color: var(--el-color-primary-light-5); box-shadow: var(--el-box-shadow-light); }
.notification-card.unread { border-left: 3px solid var(--el-color-primary); }
.message-icon {
  display: grid;
  width: 38px;
  height: 38px;
  flex: 0 0 38px;
  place-items: center;
  border-radius: 10px;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}
.message-body { min-width: 0; flex: 1; }
.message-heading { display: flex; align-items: center; gap: 9px; }
.message-heading h3 { margin-bottom: 7px; font-size: 15px; }
.unread-label { padding: 3px 7px; border-radius: 10px; color: var(--el-color-primary); background: var(--el-color-primary-light-9); font-size: 11px; }
.message-body p { margin-bottom: 10px; color: var(--el-text-color-regular); font-size: 13px; line-height: 1.7; }
.message-body time { color: var(--el-text-color-placeholder); font-size: 12px; }
@media (max-width: 650px) {
  .notification-header { gap: 14px; padding: 18px 5%; }
  .header-title { order: -1; }
  .back-button { font-size: 0; }
  .back-button .el-icon { font-size: 18px; }
  .read-all-button { font-size: 0; }
  .read-all-button .el-icon { font-size: 18px; }
}
</style>
