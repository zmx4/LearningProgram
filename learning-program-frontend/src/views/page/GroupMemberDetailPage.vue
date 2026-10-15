<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ArrowLeft, Check, UserFilled } from '@element-plus/icons-vue'
import { get } from '@/net'

interface MemberTaskItem {
  id: number
  type: 'course' | 'resource' | 'member_visit' | 'text'
  refId: number | null
  title: string
  myCompleted: boolean | null
  myCompletedAt: string | null
  completedCount: number | null
}

interface MemberTaskStatus {
  taskId: number
  title: string
  createdAt: string
  completedItems: number
  totalItems: number
  percent: number | null
  items: MemberTaskItem[]
}

interface MemberDetail {
  accountId: number
  username: string
  email: string
  bio: string
  role: string
  joinedAt: string
  tasks: MemberTaskStatus[]
}

const route = useRoute()
const router = useRouter()
const { t } = useI18n()

const groupId = Number(route.params.id)
const memberId = Number(route.params.memberId)

const detail = ref<MemberDetail | null>(null)
const loadFailed = ref(false)

const summary = computed(() => {
  const tasks = detail.value?.tasks ?? []
  const withItems = tasks.filter(task => task.totalItems > 0)
  const done = withItems.reduce((sum, task) => sum + task.completedItems, 0)
  const total = withItems.reduce((sum, task) => sum + task.totalItems, 0)
  return {
    taskCount: tasks.length,
    done,
    total,
    percent: total > 0 ? Math.round(done * 100 / total) : null,
  }
})

function loadDetail() {
  get<MemberDetail>(`/api/groups/${groupId}/members/${memberId}`, data => {
    detail.value = data
    loadFailed.value = false
  }, () => {
    loadFailed.value = true
  })
}

function itemTypeLabel(type: MemberTaskItem['type']) {
  switch (type) {
    case 'course': return t('groups.itemTypeCourse')
    case 'resource': return t('groups.itemTypeResource')
    case 'member_visit': return t('groups.itemTypeVisit')
    default: return t('groups.itemTypeText')
  }
}

function formatTime(value: string) {
  if (!value) return ''
  return value.replace('T', ' ').slice(5, 16)
}

function formatDate(value: string) {
  if (!value) return ''
  return value.slice(0, 10)
}

onMounted(loadDetail)
</script>

<template>
  <main class="member-detail">
    <header class="page-header">
      <button class="ghost-button" type="button"
              @click="router.push({ name: 'group-detail', params: { id: groupId } })">
        <el-icon><ArrowLeft /></el-icon>
        {{ t('groups.backToGroup') }}
      </button>
      <h2>{{ t('groups.memberDetail') }}</h2>
    </header>

    <section v-if="loadFailed" class="empty-panel">
      <h3>{{ t('groups.memberLoadFailed') }}</h3>
      <button class="ghost-button" type="button" @click="loadDetail">{{ t('groups.retry') }}</button>
    </section>

    <template v-else-if="detail">
      <section class="profile-card">
        <div class="profile-avatar">
          <el-icon><UserFilled /></el-icon>
        </div>
        <div class="profile-info">
          <div class="profile-name">
            <h3>{{ detail.username || t('groups.unknownUser') }}</h3>
            <span v-if="detail.role === 'owner'" class="sender-badge">{{ t('groups.roleOwner') }}</span>
            <span v-else class="sender-badge member">{{ t('groups.roleMember') }}</span>
          </div>
          <p class="profile-line">{{ t('groups.emailLabel') }}：{{ detail.email || '—' }}</p>
          <p class="profile-line">{{ t('groups.bioLabel') }}：{{ detail.bio || t('groups.noBio') }}</p>
          <p class="profile-line muted">{{ t('groups.joinedAt', { time: formatDate(detail.joinedAt) }) }}</p>
        </div>
        <div v-if="summary.percent != null" class="summary-box">
          <span class="summary-percent">{{ summary.percent }}%</span>
          <span class="summary-label">{{ t('groups.taskProgressTitle') }}</span>
          <span class="summary-count">{{ summary.done }}/{{ summary.total }}</span>
        </div>
      </section>

      <section class="task-section">
        <h3 class="section-title">{{ t('groups.taskProgressTitle') }}</h3>
        <p v-if="!detail.tasks.length" class="empty-text">{{ t('groups.noTasks') }}</p>
        <div v-for="task in detail.tasks" :key="task.taskId" class="member-task-card">
          <div class="task-card-head">
            <h4>{{ task.title }}</h4>
            <span class="task-time">{{ formatTime(task.createdAt) }}</span>
          </div>
          <template v-if="task.totalItems > 0">
            <div class="task-progress-row">
              <span class="progress-track">
                <span class="progress-fill" :style="{ width: (task.percent ?? 0) + '%' }"></span>
              </span>
              <span class="progress-percent">{{ task.percent }}%</span>
              <span class="progress-count">{{ task.completedItems }}/{{ task.totalItems }}</span>
            </div>
            <ul class="member-task-items">
              <li v-for="item in task.items" :key="item.id">
                <span class="item-type" :class="`type-${item.type}`">{{ itemTypeLabel(item.type) }}</span>
                <span class="item-title">{{ item.title }}</span>
                <span class="item-status" :class="{ done: item.myCompleted }">
                  <el-icon v-if="item.myCompleted"><Check /></el-icon>
                  {{ item.myCompleted ? t('groups.completed') : t('groups.notCompleted') }}
                </span>
                <small v-if="item.myCompleted && item.myCompletedAt" class="item-time">
                  {{ t('groups.completedAt', { time: formatTime(item.myCompletedAt) }) }}
                </small>
              </li>
            </ul>
          </template>
        </div>
      </section>
    </template>
  </main>
</template>

<style scoped>
.member-detail {
  padding: 20px 3% 24px;
  max-width: 860px;
  margin: 0 auto;
  display: grid;
  gap: 16px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
}

.profile-card {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 20px 24px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 14px;
  background: var(--el-bg-color);
  box-shadow: var(--el-box-shadow-lighter);
}

.profile-avatar {
  display: grid;
  width: 64px;
  height: 64px;
  flex: 0 0 64px;
  place-items: center;
  border-radius: 50%;
  color: var(--el-color-white);
  background: var(--el-color-primary);
  font-size: 30px;
}

.profile-info {
  min-width: 0;
  flex: 1;
}

.profile-name {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.profile-name h3 {
  margin: 0;
  font-size: 18px;
}

.profile-line {
  margin: 2px 0;
  color: var(--el-text-color-regular);
  font-size: 13px;
  word-break: break-all;
}

.profile-line.muted {
  color: var(--el-text-color-placeholder);
  font-size: 12px;
}

.summary-box {
  display: grid;
  justify-items: center;
  gap: 2px;
  padding: 10px 18px;
  border-left: 1px solid var(--el-border-color-lighter);
}

.summary-percent {
  color: var(--el-color-success);
  font-size: 26px;
  font-weight: 700;
}

.summary-label {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.summary-count {
  color: var(--el-text-color-placeholder);
  font-size: 12px;
}

.sender-badge {
  padding: 1px 7px;
  border-radius: 999px;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  font-size: 10px;
  font-weight: 600;
}

.sender-badge.member {
  color: var(--el-color-info);
  background: var(--el-color-info-light-9);
}

.task-section {
  display: grid;
  gap: 12px;
}

.section-title {
  margin: 0;
  font-size: 15px;
}

.empty-text {
  margin: 0;
  color: var(--el-text-color-placeholder);
  font-size: 13px;
}

.member-task-card {
  padding: 14px 18px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 12px;
  background: var(--el-bg-color);
  box-shadow: var(--el-box-shadow-lighter);
  display: grid;
  gap: 10px;
}

.task-card-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
}

.task-card-head h4 {
  margin: 0;
  font-size: 14px;
}

.task-time {
  color: var(--el-text-color-placeholder);
  font-size: 11px;
}

.task-progress-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

.progress-track {
  display: inline-block;
  width: 140px;
  height: 8px;
  border-radius: 999px;
  background: var(--el-fill-color);
  overflow: hidden;
}

.progress-fill {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: var(--el-color-success);
}

.progress-percent {
  color: var(--el-color-success);
  font-weight: 700;
}

.progress-count {
  color: var(--el-text-color-placeholder);
}

.member-task-items {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.member-task-items li {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  padding: 7px 10px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 9px;
  font-size: 12px;
}

.item-type {
  flex: 0 0 auto;
  padding: 1px 8px;
  border-radius: 999px;
  font-size: 10px;
  font-weight: 700;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.item-type.type-resource {
  color: var(--el-color-success);
  background: var(--el-color-success-light-9);
}

.item-type.type-member_visit {
  color: var(--el-color-warning);
  background: var(--el-color-warning-light-9);
}

.item-type.type-text {
  color: var(--el-color-info);
  background: var(--el-color-info-light-9);
}

.item-title {
  min-width: 0;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--el-text-color-primary);
}

.item-status {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  flex: 0 0 auto;
  color: var(--el-text-color-placeholder);
  font-size: 11px;
}

.item-status.done {
  color: var(--el-color-success);
  font-weight: 600;
}

.item-time {
  color: var(--el-text-color-placeholder);
  font-size: 11px;
}

.ghost-button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  color: var(--el-text-color-regular);
  background: var(--el-bg-color);
  font: inherit;
  font-size: 13px;
  cursor: pointer;
}

.ghost-button:hover {
  color: var(--el-color-primary);
  border-color: var(--el-color-primary-light-5);
}

.empty-panel {
  display: grid;
  justify-items: center;
  gap: 12px;
  padding: 48px 20px;
  border: 1px dashed var(--el-border-color);
  border-radius: 14px;
  background: var(--el-fill-color-lighter);
  text-align: center;
}

.empty-panel h3 {
  margin: 0;
  font-size: 16px;
}
</style>
