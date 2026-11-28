<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ChatDotRound, UserFilled } from '@element-plus/icons-vue'
import GroupTaskCard from './GroupTaskCard.vue'
import type { GroupMessage, GroupTask, TaskItem } from '@/types/group'

const props = defineProps<{
  messages: GroupMessage[]
  tasks: GroupTask[]
  isOwner: boolean
  myId: number | null
}>()

const emit = defineEmits<{
  goCourse: [item: TaskItem]
  accessResource: [item: TaskItem]
  goTest: [item: TaskItem]
  completeItem: [task: GroupTask, item: TaskItem]
  openProgress: [task: GroupTask]
}>()

const { t } = useI18n()
const listEl = ref<HTMLElement | null>(null)

const taskByMessageId = computed(() => new Map(props.tasks.map(task => [task.id, task])))
const messageViews = computed(() => props.messages.map(message => ({
  message,
  task: message.type === 'task' ? taskByMessageId.value.get(message.id) : undefined,
})))

function formatTime(value: string) {
  if (!value) return ''
  return value.replace('T', ' ').slice(5, 16)
}

function onCompleteItem(task: GroupTask, item: TaskItem) {
  emit('completeItem', task, item)
}

function scrollToBottom() {
  const el = listEl.value
  if (el) el.scrollTop = el.scrollHeight
}

defineExpose({ scrollToBottom })
</script>

<template>
  <div ref="listEl" class="message-list">
    <div class="message-stream-head">
      <span class="stream-title">
        <el-icon><ChatDotRound /></el-icon>
        <strong>{{ t('groups.workspaceTitle') }}</strong>
      </span>
      <span class="stream-count">{{ t('groups.taskCount', { count: tasks.length }) }}</span>
    </div>

    <TransitionGroup v-if="messages.length" name="stream" tag="div" class="message-flow">
      <template v-for="view in messageViews" :key="view.message.id">
        <GroupTaskCard
            v-if="view.message.type === 'task'"
            :key="`task-${view.message.id}`"
            :message="view.message"
            :task="view.task"
            :is-owner="isOwner"
            @go-course="emit('goCourse', $event)"
            @access-resource="emit('accessResource', $event)"
            @go-test="emit('goTest', $event)"
            @complete-item="onCompleteItem"
            @open-progress="emit('openProgress', $event)"
        />
        <div
            v-else
            :key="`message-${view.message.id}`"
            class="message-row"
            :class="{ own: view.message.accountId === myId }"
        >
          <div class="message-avatar" aria-hidden="true">
            <el-icon><UserFilled /></el-icon>
          </div>
          <div class="message-bubble-wrap">
            <div class="message-sender">
              <span>{{ view.message.username || t('groups.unknownUser') }}</span>
              <span v-if="view.message.role === 'owner'" class="sender-badge">{{ t('groups.roleOwner') }}</span>
              <span class="message-time">{{ formatTime(view.message.createdAt) }}</span>
            </div>
            <div class="message-bubble">{{ view.message.content }}</div>
          </div>
        </div>
      </template>
    </TransitionGroup>

    <div v-else class="empty-messages">
      <el-icon><ChatDotRound /></el-icon>
      <span>{{ t('groups.emptyMessages') }}</span>
    </div>
  </div>
</template>

<style scoped>
.message-list {
  flex: 1;
  min-height: 0;
  padding: 0 20px 18px;
  overflow-y: auto;
  scrollbar-color: var(--el-border-color-dark) transparent;
  scrollbar-width: thin;
}

.message-list::-webkit-scrollbar,
.member-list::-webkit-scrollbar {
  width: 6px;
}

.message-list::-webkit-scrollbar-thumb,
.member-list::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: var(--el-border-color-dark);
}

.message-stream-head {
  position: sticky;
  top: 0;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: 0 -20px;
  padding: 12px 20px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  background: var(--el-bg-color);
}

.stream-title {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  color: var(--el-text-color-primary);
}

.stream-title .el-icon {
  color: var(--el-color-primary);
  font-size: 15px;
}

.stream-title strong {
  font-size: 12px;
  font-weight: 600;
}

.stream-count {
  color: var(--el-text-color-secondary);
  font-size: 10px;
  font-variant-numeric: tabular-nums;
}

.message-flow {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding-top: 18px;
}

.empty-messages {
  display: grid;
  justify-items: center;
  gap: 10px;
  margin: auto;
  padding: 48px 16px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  text-align: center;
}

.empty-messages .el-icon {
  font-size: 28px;
}

.message-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.message-row.own {
  flex-direction: row-reverse;
}

.message-avatar {
  display: grid;
  width: 34px;
  height: 34px;
  flex: 0 0 34px;
  place-items: center;
  border: 3px solid var(--el-bg-color);
  border-radius: 50%;
  outline: 1px solid var(--el-border-color-lighter);
  color: var(--el-color-white);
  background: var(--el-color-info-dark-2);
  font-size: 14px;
}

.message-row.own .message-avatar {
  background: var(--el-color-primary-dark-2);
}

.message-bubble-wrap {
  display: flex;
  max-width: min(76%, 620px);
  min-width: 0;
  flex-direction: column;
  gap: 5px;
}

.message-row.own .message-bubble-wrap {
  align-items: flex-end;
}

.message-sender {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  justify-content: flex-start;
  gap: 4px 8px;
  color: var(--el-text-color-secondary);
  font-size: 11px;
}

.message-row.own .message-sender {
  justify-content: flex-end;
}

.message-time {
  color: var(--el-text-color-secondary);
  font-size: 10px;
  font-variant-numeric: tabular-nums;
}

.sender-badge {
  padding: 1px 7px;
  border-radius: 999px;
  color: var(--el-color-primary-dark-2);
  background: var(--el-color-primary-light-9);
  font-size: 9px;
  font-weight: 700;
}

.message-bubble {
  padding: 10px 14px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 14px 14px 14px 5px;
  color: var(--el-text-color-primary);
  background: var(--el-fill-color-light);
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.message-row.own .message-bubble {
  border-color: transparent;
  border-radius: 14px 14px 5px 14px;
  color: var(--el-color-white);
  background: color-mix(in srgb, var(--el-color-primary-dark-2) 78%, #071624);
}

.stream-enter-from {
  opacity: 0;
  filter: blur(3px);
  transform: translateY(12px) scale(.985);
}

.stream-enter-active {
  transition: opacity .38s ease, filter .38s ease, transform .48s cubic-bezier(.16, 1, .3, 1);
}

.stream-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

.stream-leave-active {
  transition: opacity .2s ease, transform .2s ease;
}

@media (max-width: 620px) {
  .message-list {
    padding-inline: 14px;
  }

  .message-stream-head {
    margin-inline: -14px;
    padding-inline: 14px;
  }

  .message-flow {
    gap: 14px;
    padding-top: 14px;
  }

  .message-avatar {
    width: 30px;
    height: 30px;
    flex-basis: 30px;
    border-width: 2px;
    font-size: 12px;
  }

  .message-bubble-wrap {
    max-width: 82%;
  }

  .message-bubble {
    padding: 9px 12px;
    font-size: 13px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .stream-enter-from,
  .stream-leave-to {
    opacity: 1;
    filter: none;
    transform: none;
  }

  .stream-enter-active,
  .stream-leave-active {
    transition-duration: .01ms;
  }
}
</style>
