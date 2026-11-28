<script setup lang="ts">
import { computed } from 'vue'
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
  goTest: [item: TaskItem]
  completeItem: [task: GroupTask, item: TaskItem]
  openProgress: [task: GroupTask]
}>()

const { t } = useI18n()

function itemTypeLabel(type: TaskItemType) {
  switch (type) {
    case 'course': return t('groups.itemTypeCourse')
    case 'resource': return t('groups.itemTypeResource')
    case 'member_visit': return t('groups.itemTypeVisit')
    case 'word_test': return t('groups.itemTypeWordTest')
    case 'question_set': return t('groups.itemTypeQuestionSet')
    default: return t('groups.itemTypeText')
  }
}

function formatTime(value: string) {
  if (!value) return ''
  return value.replace('T', ' ').slice(5, 16)
}

const dueLabel = computed(() => {
  if (!props.task?.dueAt) return ''
  if (props.task.lateCompleted) return t('groups.lateCompleted')
  if (props.task.myFinished) return t('groups.finished')
  return `${t('groups.dueAtLabel')}: ${formatTime(props.task.dueAt)}`
})

const overdue = computed(() => Boolean(
    props.task?.dueAt && !props.task.myFinished && new Date(props.task.dueAt).getTime() < Date.now(),
))

const myProgressPercent = computed(() => {
  if (!props.task?.totalItems) return 0
  return Math.round(props.task.myCompleted * 100 / props.task.totalItems)
})
</script>

<template>
  <div class="task-card">
    <div class="task-topline">
      <div class="task-badge">
        <el-icon><Flag /></el-icon>
        {{ t('groups.taskBadge') }}
      </div>
      <p v-if="dueLabel" class="task-due" :class="{ overdue, late: task?.lateCompleted }">
        <el-icon><Flag /></el-icon>{{ dueLabel }}
      </p>
    </div>
    <h4>{{ message.title }}</h4>
    <p class="task-content">{{ message.content }}</p>
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
                v-else-if="(item.type === 'word_test' || item.type === 'question_set') && !item.myCompleted"
                class="mini-button"
                type="button"
                @click="emit('goTest', item)"
            >{{ t('groups.goTest') }}</button>
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
        <span class="my-progress-track" aria-hidden="true">
          <span class="my-progress-fill" :style="{ width: `${myProgressPercent}%` }"></span>
        </span>
        <template v-if="isOwner && task.progressPercent != null">
          <span class="overall-label">{{ t('groups.overallProgress') }}</span>
          <span class="progress-track">
            <span class="progress-fill" :style="{ width: task.progressPercent + '%' }"></span>
          </span>
          <span class="progress-percent">{{ task.progressPercent }}%</span>
          <button class="mini-button progress-action" type="button" @click="emit('openProgress', task)">
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
  width: min(620px, 96%);
  padding: 16px 18px 13px;
  border: 1px solid var(--el-color-warning-light-7);
  border-radius: 16px;
  background: var(--el-bg-color);
  box-shadow: 0 16px 38px -34px var(--el-color-warning-dark-2);
  transition: border-color .2s, box-shadow .25s, transform .25s;
}

.task-card:hover {
  border-color: var(--el-color-warning-light-5);
  box-shadow: 0 18px 42px -32px var(--el-color-warning-dark-2);
  transform: translateY(-1px);
}

.task-topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
}

.task-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 8px;
  border-radius: 999px;
  color: var(--el-color-warning-dark-2);
  background: var(--el-color-warning-light-9);
  font-size: 10px;
  font-weight: 700;
}

.task-card h4 {
  margin: 10px 0 5px;
  font-size: 17px;
  line-height: 1.4;
  letter-spacing: -.015em;
}

.task-content {
  max-width: 68ch;
  margin: 0;
  color: var(--el-text-color-regular);
  font-size: 13px;
  line-height: 1.65;
  white-space: pre-wrap;
  word-break: break-word;
}

.task-due {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin: 0;
  padding: 3px 8px;
  border-radius: 999px;
  color: var(--el-text-color-secondary);
  background: var(--el-fill-color-light);
  font-size: 10px;
  font-variant-numeric: tabular-nums;
}

.task-due.overdue {
  color: var(--el-color-danger);
  background: var(--el-color-danger-light-9);
}

.task-due.late {
  color: var(--el-color-warning-dark-2);
  background: var(--el-color-warning-light-9);
}

.task-items {
  display: flex;
  flex-direction: column;
  gap: 7px;
  margin: 13px 0 11px;
}

.task-item {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 7px 8px;
  padding: 8px 10px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  background: var(--el-fill-color-lighter);
  font-size: 12px;
}

.item-type {
  flex: 0 0 auto;
  padding: 2px 8px;
  border-radius: 999px;
  color: var(--el-color-primary-dark-2);
  background: var(--el-color-primary-light-9);
  font-size: 9px;
  font-weight: 700;
}

.item-type.type-resource {
  color: var(--el-color-success-dark-2);
  background: var(--el-color-success-light-9);
}

.item-type.type-member_visit {
  color: var(--el-color-warning-dark-2);
  background: var(--el-color-warning-light-9);
}

.item-type.type-text {
  color: var(--el-color-info-dark-2);
  background: var(--el-color-info-light-9);
}

.item-title {
  min-width: 0;
  flex: 1 1 150px;
  overflow: hidden;
  color: var(--el-text-color-primary);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-status {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 3px;
  color: var(--el-text-color-secondary);
  font-size: 10px;
}

.item-status.done {
  color: var(--el-color-success-dark-2);
  font-weight: 600;
}

.item-actions {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
}

.item-hint {
  color: var(--el-text-color-secondary);
  font-size: 10px;
}

.item-count {
  flex: 0 0 auto;
  margin-left: auto;
  color: var(--el-text-color-secondary);
  font-size: 10px;
  font-variant-numeric: tabular-nums;
}

.mini-button {
  min-height: 25px;
  padding: 3px 10px;
  border: 1px solid var(--el-color-primary-light-5);
  border-radius: 999px;
  color: var(--el-color-primary-dark-2);
  background: var(--el-color-primary-light-9);
  font: inherit;
  font-size: 10px;
  font-weight: 600;
  white-space: nowrap;
  cursor: pointer;
  transition: color .2s, background-color .2s, transform .2s;
}

.mini-button:hover {
  color: var(--el-color-white);
  background: var(--el-color-primary-dark-2);
  transform: translateY(-1px);
}

.mini-button:focus-visible {
  outline: 2px solid var(--el-color-primary);
  outline-offset: 2px;
}

.task-progress-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 7px 9px;
  margin-bottom: 10px;
  font-size: 11px;
}

.my-progress {
  color: var(--el-text-color-secondary);
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.my-progress-track,
.progress-track {
  display: inline-block;
  height: 6px;
  overflow: hidden;
  border-radius: 999px;
  background: var(--el-fill-color);
}

.my-progress-track {
  width: 84px;
}

.progress-track {
  width: 104px;
}

.my-progress-fill,
.progress-fill {
  display: block;
  height: 100%;
  border-radius: inherit;
  transform-origin: left;
  animation: progress-grow .55s cubic-bezier(.16, 1, .3, 1) both;
}

.my-progress-fill {
  background: var(--el-color-primary);
}

.progress-fill {
  background: var(--el-color-success);
}

.overall-label {
  margin-left: auto;
  color: var(--el-text-color-secondary);
}

.progress-percent {
  color: var(--el-color-success-dark-2);
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.progress-action {
  margin-left: 3px;
}

.message-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-top: 10px;
  border-top: 1px solid var(--el-border-color-lighter);
  color: var(--el-text-color-secondary);
  font-size: 10px;
  font-variant-numeric: tabular-nums;
}

@keyframes progress-grow {
  from { transform: scaleX(0); }
  to { transform: scaleX(1); }
}

@media (max-width: 620px) {
  .task-card {
    width: 98%;
    padding: 13px 13px 11px;
    border-radius: 13px;
  }

  .task-card h4 {
    font-size: 15px;
  }

  .task-content {
    font-size: 12px;
  }

  .task-item {
    padding: 8px;
  }

  .item-count {
    width: 100%;
    margin-left: 0;
  }

  .overall-label {
    margin-left: 0;
  }
}

@media (prefers-reduced-motion: reduce) {
  .task-card,
  .mini-button {
    transition-duration: .01ms;
  }

  .my-progress-fill,
  .progress-fill {
    animation: none;
  }
}
</style>
