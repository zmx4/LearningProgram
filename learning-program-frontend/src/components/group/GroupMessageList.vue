<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { UserFilled } from '@element-plus/icons-vue'
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
/** 消息与其对应任务（type=task 时）的视图模型，便于模板内类型收窄 */
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
    <div v-if="!messages.length" class="empty-messages">
      {{ t('groups.emptyMessages') }}
    </div>
    <template v-for="view in messageViews" :key="view.message.id">
      <GroupTaskCard
          v-if="view.message.type === 'task'"
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
          class="message-row"
          :class="{ own: view.message.accountId === myId }"
      >
        <div class="message-avatar">
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
  </div>
</template>

<style scoped>
.message-list {
  flex: 1;
  min-height: 0;
  padding: 18px 20px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.empty-messages {
  margin: auto;
  color: var(--el-text-color-placeholder);
  font-size: 13px;
}

.message-row {
  display: flex;
  gap: 10px;
  align-items: flex-start;
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
  border-radius: 50%;
  color: var(--el-color-white);
  background: var(--el-color-info);
  font-size: 16px;
}

.message-row.own .message-avatar {
  background: var(--el-color-primary);
}

.message-bubble-wrap {
  max-width: 70%;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.message-row.own .message-bubble-wrap {
  align-items: flex-end;
}

.message-sender {
  display: flex;
  align-items: baseline;
  gap: 8px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.message-time {
  color: var(--el-text-color-placeholder);
  font-size: 11px;
}

.sender-badge {
  padding: 1px 7px;
  border-radius: 999px;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  font-size: 10px;
  font-weight: 600;
}

.message-bubble {
  padding: 9px 13px;
  border-radius: 12px;
  border-top-left-radius: 4px;
  color: var(--el-text-color-primary);
  background: var(--el-fill-color-light);
  font-size: 14px;
  line-height: 1.55;
  white-space: pre-wrap;
  word-break: break-word;
}

.message-row.own .message-bubble {
  border-radius: 12px;
  border-top-right-radius: 4px;
  color: var(--el-color-white);
  background: var(--el-color-primary);
}
</style>