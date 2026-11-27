<script setup lang="ts">
import { Trophy, User, UserFilled } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import type { GroupSummary } from '@/types/group'

interface GroupMetrics {
  ongoingTasks: number
  completionRate: number | null
  overdueTasks: number
}

const props = defineProps<{
  group: GroupSummary
  metrics?: GroupMetrics
}>()

const emit = defineEmits<{
  open: [group: GroupSummary]
  leaderboard: [group: GroupSummary]
}>()

const { t } = useI18n()

function formatDate(value: string) {
  if (!value) return ''
  return value.replace('T', ' ').slice(0, 16)
}
</script>

<template>
  <article class="group-card">
    <button class="group-card-main" type="button" @click="emit('open', props.group)">
      <span class="group-card-title">
        <strong>{{ props.group.name }}</strong>
        <span class="role-badge" :class="{ owner: props.group.role === 'owner' }">
          {{ props.group.role === 'owner' ? t('groups.roleOwner') : t('groups.roleMember') }}
        </span>
      </span>
      <span class="group-meta">
        <span class="meta-item">
          <el-icon><UserFilled /></el-icon>
          {{ t('groups.ownerLabel', { name: props.group.ownerName || t('groups.unknownUser') }) }}
        </span>
        <span class="meta-item">
          <el-icon><User /></el-icon>
          {{ t('groups.memberCount', { count: props.group.memberCount }) }}
        </span>
      </span>
      <span v-if="metrics" class="group-metrics">
        <span class="metric">
          <small>{{ t('groups.ongoingTasks') }}</small>
          <strong>{{ metrics.ongoingTasks }}</strong>
        </span>
        <span class="metric">
          <small>{{ t('groups.myCompletion') }}</small>
          <strong>{{ metrics.completionRate == null ? '—' : `${metrics.completionRate}%` }}</strong>
        </span>
        <span class="metric">
          <small>{{ t('groups.overdueTasks') }}</small>
          <strong>{{ metrics.overdueTasks }}</strong>
        </span>
      </span>
    </button>
    <footer class="group-card-footer">
      <span>{{ formatDate(props.group.createdAt) }}</span>
      <button class="leaderboard-button" type="button" @click="emit('leaderboard', props.group)">
        <el-icon><Trophy /></el-icon>
        {{ t('groups.leaderboard') }}
      </button>
    </footer>
  </article>
</template>

<style scoped>
.group-card {
  overflow: hidden;
  border: 1px solid var(--el-border-color-light);
  border-radius: 14px;
  background: var(--el-bg-color);
  box-shadow: var(--el-box-shadow-lighter);
  transition: border-color .2s, box-shadow .2s, transform .2s;
}

.group-card:hover {
  border-color: var(--el-color-primary-light-5);
  box-shadow: var(--el-box-shadow-light);
  transform: translateY(-1px);
}

.group-card-main {
  display: grid;
  width: 100%;
  gap: 12px;
  padding: 18px 20px 14px;
  border: 0;
  color: inherit;
  background: transparent;
  font: inherit;
  text-align: left;
  cursor: pointer;
}

.group-card-main:focus-visible,
.leaderboard-button:focus-visible {
  outline: 2px solid var(--el-color-primary);
  outline-offset: -2px;
}

.group-card-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.group-card-title strong {
  overflow: hidden;
  font-size: 16px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.role-badge {
  flex: 0 0 auto;
  padding: 2px 9px;
  border-radius: 999px;
  color: var(--el-color-info);
  background: var(--el-color-info-light-9);
  font-size: 11px;
  font-weight: 600;
}

.role-badge.owner {
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.group-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  color: var(--el-text-color-placeholder);
  font-size: 12px;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.group-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  padding-top: 4px;
}

.metric {
  display: grid;
  gap: 2px;
  padding: 9px 10px;
  border-radius: 10px;
  background: var(--el-fill-color-lighter);
}

.metric small {
  overflow: hidden;
  color: var(--el-text-color-placeholder);
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.metric strong {
  color: var(--el-text-color-primary);
  font-size: 18px;
  line-height: 1.2;
}

.group-card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 20px 12px;
  border-top: 1px solid var(--el-border-color-lighter);
  color: var(--el-text-color-placeholder);
  font-size: 12px;
}

.leaderboard-button {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 10px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  color: var(--el-text-color-secondary);
  background: var(--el-bg-color);
  font: inherit;
  font-size: 12px;
  cursor: pointer;
}

.leaderboard-button:hover {
  border-color: var(--el-color-primary-light-5);
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}
</style>
