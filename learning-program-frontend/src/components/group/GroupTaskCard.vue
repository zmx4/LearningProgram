<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { Check, Flag } from '@element-plus/icons-vue'
import type { GroupMessage, GroupTask, TaskItem, TaskItemType } from '@/types/group'

const props = defineProps<{
  message: GroupMessage
  task?: GroupTask
  isOwner: boolean
}>()

const emit = defineEmits<{
  goCourse: [item: TaskItem]
  accessResource: [item: TaskItem]
  completeItem: [task: GroupTask, item: TaskItem]
  openProgress: [task: GroupTask]
}>()

const { t } = useI18n()

function itemTypeLabel(type: TaskItemType) {
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
</script>

<template>
  <div class="task-card">
    <div class="task-badge">
      <el-icon><Flag /></el-icon>
      {{ t('groups.taskBadge') }}
    </div>
    <h4>{{ message.title }}</h4>
    <p>{{ message.content }}</p>
    <template v-if="task">
      <div v-if="task.items.length" class="task-items">
        <div v-for="item in task.items" :key="item.id" class="task-item">
          <span class="item-type" :class="`type-${item.type}`">{{ itemTypeLabel(item.type) }}</span>
          <span class="item-title">{{ item.title }}</span>
          <span class="item-status" :class="{ done: item.myCompleted }">
            <el-icon v-if="item.myCompleted"><Check /></el-icon>
            {{ item.myCompleted ? t('groups.completed') : t('groups.notCompleted') }}
          </span>
          <span class="item-actions">
            <button
                v-if="item.type === 'course' && !item.myCompleted"
                class="mini-button"
                type="button"
                @click="emit('goCourse', item)"
            >{{ t('groups.goStudy') }}</button>
            <button
                v-else-if="item.type === 'resource' && !item.myCompleted"
                class="mini-button"
                type="button"
                @click="emit('accessResource', item)"
            >{{ t('groups.goDownload') }}</button>
            <button
                v-else-if="item.type === 'text' && !item.myCompleted"
                class="mini-button"
                type="button"
                @click="emit('completeItem', task, item)"
            >{{ t('groups.markComplete') }}</button>
            <small v-else-if="item.type === 'member_visit' && !item.myCompleted" class="item-hint">
              {{ t('groups.visitHint') }}
            </small>
            <small v-else-if="item.myCompleted && item.myCompletedAt" class="item-hint">
              {{ t('groups.completedAt', { time: formatTime(item.myCompletedAt) }) }}
            </small>
          </span>
          <small class="item-count">
            {{ t('groups.completedBy', { done: item.completedCount ?? 0, total: task.memberCount }) }}
          </small>
        </div>
      </div>
      <div v-if="task.items.length" class="task-progress-row">
        <span class="my-progress">
          {{ t('groups.myProgress', { done: task.myCompleted, total: task.totalItems }) }}
        </span>
        <template v-if="isOwner && task.progressPercent != null">
          <span class="overall-label">{{ t('groups.overallProgress') }}</span>
          <span class="progress-track">
            <span class="progress-fill" :style="{ width: task.progressPercent + '%' }"></span>
          </span>
          <span class="progress-percent">{{ task.progressPercent }}%</span>
          <button class="mini-button" type="button" @click="emit('openProgress', task)">
            {{ t('groups.viewProgress') }}
          </button>
        </template>
      </div>
    </template>
    <div class="message-meta">
      <span>{{ message.username || t('groups.unknownUser') }}</span>
      <span>{{ formatTime(message.createdAt) }}</span>
    </div>
  </div>
</template>

<style scoped>
.task-card {
  align-self: center;
  width: min(560px, 92%);
  padding: 14px 18px;
  border: 1px solid var(--el-color-warning-light-5);
  border-left: 4px solid var(--el-color-warning);
  border-radius: 12px;
  background: var(--el-color-warning-light-9);
}

.task-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: var(--el-color-warning);
  font-size: 12px;
  font-weight: 700;
}

.task-card h4 {
  margin: 6px 0;
  font-size: 15px;
}

.task-card p {
  margin: 0 0 8px;
  color: var(--el-text-color-regular);
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.task-items {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin: 4px 0 10px;
}

.task-item {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  padding: 7px 10px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 9px;
  background: var(--el-bg-color);
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

.item-actions {
  display: inline-flex;
  align-items: center;
  flex: 0 0 auto;
}

.item-hint {
  color: var(--el-text-color-placeholder);
  font-size: 11px;
}

.item-count {
  flex: 0 0 auto;
  margin-left: auto;
  color: var(--el-text-color-placeholder);
  font-size: 11px;
}

.mini-button {
  padding: 3px 10px;
  border: 1px solid var(--el-color-primary-light-5);
  border-radius: 999px;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  font: inherit;
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
}

.mini-button:hover {
  color: var(--el-color-white);
  background: var(--el-color-primary);
}

.task-progress-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 8px;
  font-size: 12px;
}

.my-progress {
  color: var(--el-text-color-secondary);
  font-weight: 600;
}

.overall-label {
  color: var(--el-text-color-placeholder);
}

.progress-track {
  display: inline-block;
  width: 110px;
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
  transition: width .3s;
}

.progress-percent {
  color: var(--el-color-success);
  font-weight: 700;
}

.message-meta {
  display: flex;
  gap: 12px;
  color: var(--el-text-color-placeholder);
  font-size: 11px;
}
</style>