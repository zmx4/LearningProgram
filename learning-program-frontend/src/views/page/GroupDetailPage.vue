<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, CopyDocument, Flag, Promotion, Refresh, User, UserFilled } from '@element-plus/icons-vue'
import { currentUserId, del, get, post } from '@/net'

interface GroupSummary {
  id: number
  name: string
  ownerId: number
  ownerName: string
  inviteCode: string
  memberCount: number
  role: string
  createdAt: string
}

interface GroupMember {
  accountId: number
  username: string
  role: string
  joinedAt: string
}

interface GroupDetail {
  group: GroupSummary
  members: GroupMember[]
}

interface GroupMessage {
  id: number
  accountId: number
  username: string
  role: string
  type: string
  title: string | null
  content: string
  createdAt: string
}

const route = useRoute()
const router = useRouter()
const { t } = useI18n()

const groupId = Number(route.params.id)
const myId = currentUserId()

const detail = ref<GroupDetail | null>(null)
const messages = ref<GroupMessage[]>([])
const loadFailed = ref(false)
const sending = ref(false)
const publishing = ref(false)
const draft = ref('')
const taskFormVisible = ref(false)
const taskForm = ref({ title: '', content: '' })
const messageListEl = ref<HTMLElement | null>(null)
let pollTimer: ReturnType<typeof setInterval> | null = null

const isOwner = computed(() => detail.value?.group.role === 'owner')
const inviteCode = computed(() => detail.value?.group.inviteCode ?? '')

function loadDetail() {
  get<GroupDetail>(`/api/groups/${groupId}`, data => {
    detail.value = data
    loadFailed.value = false
  }, () => {
    loadFailed.value = true
  })
}

function loadMessages(scrollToBottom = false) {
  get<GroupMessage[]>(`/api/groups/${groupId}/messages`, data => {
    const lastId = messages.value[messages.value.length - 1]?.id
    messages.value = data
    const newLastId = data[data.length - 1]?.id
    if (scrollToBottom || (lastId !== undefined && newLastId !== lastId)) {
      void nextTick(() => scrollMessagesToBottom())
    }
  }, () => {
    if (!messages.value.length) loadFailed.value = true
  })
}

function scrollMessagesToBottom() {
  const el = messageListEl.value
  if (el) el.scrollTop = el.scrollHeight
}

function sendMessage() {
  const content = draft.value.trim()
  if (!content) {
    ElMessage.warning(t('groups.messageRequired'))
    return
  }
  sending.value = true
  post<GroupMessage>(`/api/groups/${groupId}/messages`, { content }, message => {
    sending.value = false
    draft.value = ''
    messages.value = [...messages.value, message]
    void nextTick(() => scrollMessagesToBottom())
  }, () => {
    sending.value = false
  })
}

function onComposerKeydown(event: KeyboardEvent) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    sendMessage()
  }
}

function publishTask() {
  const title = taskForm.value.title.trim()
  const content = taskForm.value.content.trim()
  if (!title) {
    ElMessage.warning(t('groups.taskTitleRequired'))
    return
  }
  if (!content) {
    ElMessage.warning(t('groups.taskContentRequired'))
    return
  }
  publishing.value = true
  post<GroupMessage>(`/api/groups/${groupId}/tasks`, { title, content }, message => {
    publishing.value = false
    taskFormVisible.value = false
    taskForm.value = { title: '', content: '' }
    messages.value = [...messages.value, message]
    ElMessage.success(t('groups.taskPublished'))
    void nextTick(() => scrollMessagesToBottom())
  }, () => {
    publishing.value = false
  })
}

function copyInviteCode() {
  if (!inviteCode.value) return
  void navigator.clipboard?.writeText(inviteCode.value)
      .then(() => ElMessage.success(t('groups.codeCopied')))
      .catch(() => ElMessage.info(inviteCode.value))
}

async function leaveGroup() {
  const confirmText = isOwner.value ? t('groups.disbandConfirm') : t('groups.leaveConfirm')
  try {
    await ElMessageBox.confirm(confirmText, isOwner.value ? t('groups.disband') : t('groups.leave'), {
      type: 'warning',
      confirmButtonText: t('groups.leave'),
      cancelButtonText: t('groups.back'),
    })
  } catch {
    return
  }
  post<null>(`/api/groups/${groupId}/leave`, {}, () => {
    ElMessage.success(t('groups.left'))
    void router.push({ name: 'groups' })
  })
}

async function removeMember(member: GroupMember) {
  try {
    await ElMessageBox.confirm(
        t('groups.removeConfirm', { name: member.username || t('groups.unknownUser') }),
        t('groups.removeMember'),
        { type: 'warning' },
    )
  } catch {
    return
  }
  del<null>(`/api/groups/${groupId}/members/${member.accountId}`, {}, () => {
    ElMessage.success(t('groups.removed'))
    loadDetail()
  })
}

function formatTime(value: string) {
  if (!value) return ''
  return value.replace('T', ' ').slice(5, 16)
}

function formatDate(value: string) {
  if (!value) return ''
  return value.slice(0, 10)
}

onMounted(() => {
  loadDetail()
  loadMessages(true)
  pollTimer = setInterval(() => loadMessages(), 10000)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})
</script>

<template>
  <main class="group-detail">
    <header class="group-header">
      <button class="ghost-button back-button" type="button" @click="router.push({ name: 'groups' })">
        <el-icon><ArrowLeft /></el-icon>
        {{ t('groups.back') }}
      </button>
      <div class="group-title">
        <h2>{{ detail?.group.name ?? '' }}</h2>
        <button class="code-chip" type="button" :title="t('groups.copyCode')" @click="copyInviteCode">
          <el-icon><CopyDocument /></el-icon>
          {{ t('groups.inviteCode', { code: inviteCode }) }}
        </button>
      </div>
      <div class="header-actions">
        <button class="ghost-button" type="button" @click="loadMessages(true)">
          <el-icon><Refresh /></el-icon>
          {{ t('groups.refresh') }}
        </button>
        <button class="danger-button" type="button" @click="leaveGroup">
          {{ isOwner ? t('groups.disband') : t('groups.leave') }}
        </button>
      </div>
    </header>

    <section v-if="loadFailed && !detail" class="empty-panel">
      <h3>{{ t('groups.loadFailed') }}</h3>
      <button class="ghost-button" type="button" @click="loadDetail">{{ t('groups.retry') }}</button>
    </section>

    <div v-else class="group-body">
      <section class="chat-area">
        <div ref="messageListEl" class="message-list">
          <div v-if="!messages.length" class="empty-messages">
            {{ t('groups.emptyMessages') }}
          </div>
          <template v-for="message in messages" :key="message.id">
            <div
                v-if="message.type === 'task'"
                class="task-card"
            >
              <div class="task-badge">
                <el-icon><Flag /></el-icon>
                {{ t('groups.taskBadge') }}
              </div>
              <h4>{{ message.title }}</h4>
              <p>{{ message.content }}</p>
              <div class="message-meta">
                <span>{{ message.username || t('groups.unknownUser') }}</span>
                <span>{{ formatTime(message.createdAt) }}</span>
              </div>
            </div>
            <div
                v-else
                class="message-row"
                :class="{ own: message.accountId === myId }"
            >
              <div class="message-avatar">
                <el-icon><UserFilled /></el-icon>
              </div>
              <div class="message-bubble-wrap">
                <div class="message-sender">
                  <span>{{ message.username || t('groups.unknownUser') }}</span>
                  <span v-if="message.role === 'owner'" class="sender-badge">{{ t('groups.roleOwner') }}</span>
                  <span class="message-time">{{ formatTime(message.createdAt) }}</span>
                </div>
                <div class="message-bubble">{{ message.content }}</div>
              </div>
            </div>
          </template>
        </div>

        <div class="composer-area">
          <div v-if="taskFormVisible" class="task-form">
            <label class="field">
              <span>{{ t('groups.taskTitleLabel') }}</span>
              <input v-model="taskForm.title" type="text" maxlength="100"
                     :placeholder="t('groups.taskTitlePlaceholder')">
            </label>
            <label class="field">
              <span>{{ t('groups.taskContentLabel') }}</span>
              <textarea v-model="taskForm.content" rows="3" maxlength="2000"
                        :placeholder="t('groups.taskContentPlaceholder')"></textarea>
            </label>
            <div class="task-form-actions">
              <button class="ghost-button" type="button" @click="taskFormVisible = false">
                {{ t('groups.back') }}
              </button>
              <button class="primary-button" type="button" :disabled="publishing" @click="publishTask">
                {{ publishing ? t('groups.publishing') : t('groups.publish') }}
              </button>
            </div>
          </div>
          <div class="composer">
            <textarea
                v-model="draft"
                rows="2"
                maxlength="2000"
                :placeholder="t('groups.inputPlaceholder')"
                @keydown="onComposerKeydown"
            ></textarea>
            <div class="composer-actions">
              <button
                  v-if="isOwner"
                  class="ghost-button"
                  type="button"
                  @click="taskFormVisible = !taskFormVisible"
              >
                <el-icon><Flag /></el-icon>
                {{ t('groups.publishTask') }}
              </button>
              <button class="primary-button" type="button" :disabled="sending" @click="sendMessage">
                <el-icon><Promotion /></el-icon>
                {{ sending ? t('groups.sending') : t('groups.send') }}
              </button>
            </div>
          </div>
        </div>
      </section>

      <aside class="member-panel">
        <h3>
          <el-icon><User /></el-icon>
          {{ t('groups.members') }}
          <span class="member-count">{{ detail?.members.length ?? 0 }}</span>
        </h3>
        <ul class="member-list">
          <li v-for="member in detail?.members ?? []" :key="member.accountId" class="member-item">
            <div class="member-avatar">
              <el-icon><UserFilled /></el-icon>
            </div>
            <div class="member-info">
              <div class="member-name">
                <span>{{ member.username || t('groups.unknownUser') }}</span>
                <span v-if="member.role === 'owner'" class="sender-badge">{{ t('groups.roleOwner') }}</span>
              </div>
              <small>{{ t('groups.joinedAt', { time: formatDate(member.joinedAt) }) }}</small>
            </div>
            <button
                v-if="isOwner && member.accountId !== myId"
                class="remove-button"
                type="button"
                :title="t('groups.removeMember')"
                @click="removeMember(member)"
            >
              ×
            </button>
          </li>
        </ul>
      </aside>
    </div>
  </main>
</template>

<style scoped>
.group-detail {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 100px);
  padding: 20px 3% 24px;
}

.group-header {
  display: flex;
  align-items: center;
  gap: 18px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.group-title {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
  flex: 1;
}

.group-title h2 {
  margin: 0;
  font-size: 20px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.code-chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 5px 11px;
  border: 1px dashed var(--el-color-primary-light-5);
  border-radius: 999px;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  font: inherit;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.group-body {
  display: flex;
  gap: 16px;
  min-height: 0;
  flex: 1;
}

.chat-area {
  display: flex;
  flex: 1;
  min-width: 0;
  min-height: 0;
  flex-direction: column;
  border: 1px solid var(--el-border-color-light);
  border-radius: 14px;
  background: var(--el-bg-color);
  box-shadow: var(--el-box-shadow-lighter);
  overflow: hidden;
}

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

.message-meta {
  display: flex;
  gap: 12px;
  color: var(--el-text-color-placeholder);
  font-size: 11px;
}

.composer-area {
  border-top: 1px solid var(--el-border-color-lighter);
  padding: 12px 16px;
  display: grid;
  gap: 10px;
}

.composer {
  display: grid;
  gap: 8px;
}

.composer textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--el-border-color);
  border-radius: 9px;
  color: var(--el-text-color-primary);
  background: var(--el-bg-color);
  font: inherit;
  font-size: 14px;
  resize: none;
}

.composer textarea:focus {
  border-color: var(--el-color-primary);
  outline: none;
}

.composer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.task-form {
  display: grid;
  gap: 10px;
  padding: 12px 14px;
  border: 1px solid var(--el-color-warning-light-5);
  border-radius: 10px;
  background: var(--el-color-warning-light-9);
}

.task-form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.field {
  display: grid;
  gap: 6px;
}

.field > span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  font-weight: 600;
}

.field input,
.field textarea {
  width: 100%;
  padding: 9px 12px;
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  color: var(--el-text-color-primary);
  background: var(--el-bg-color);
  font: inherit;
  font-size: 13px;
  resize: vertical;
}

.field input:focus,
.field textarea:focus {
  border-color: var(--el-color-primary);
  outline: none;
}

.member-panel {
  width: 240px;
  flex: 0 0 240px;
  min-height: 0;
  display: flex;
  flex-direction: column;
  border: 1px solid var(--el-border-color-light);
  border-radius: 14px;
  background: var(--el-bg-color);
  box-shadow: var(--el-box-shadow-lighter);
  overflow: hidden;
}

.member-panel h3 {
  display: flex;
  align-items: center;
  gap: 7px;
  margin: 0;
  padding: 14px 16px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  font-size: 14px;
}

.member-count {
  margin-left: auto;
  color: var(--el-text-color-placeholder);
  font-size: 12px;
  font-weight: 400;
}

.member-list {
  flex: 1;
  min-height: 0;
  margin: 0;
  padding: 8px;
  overflow-y: auto;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.member-item {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 8px;
  border-radius: 9px;
}

.member-item:hover {
  background: var(--el-fill-color-light);
}

.member-avatar {
  display: grid;
  width: 30px;
  height: 30px;
  flex: 0 0 30px;
  place-items: center;
  border-radius: 50%;
  color: var(--el-color-white);
  background: var(--el-color-info);
  font-size: 14px;
}

.member-info {
  min-width: 0;
  flex: 1;
}

.member-name {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}

.member-name > span:first-child {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.member-info small {
  color: var(--el-text-color-placeholder);
  font-size: 11px;
}

.remove-button {
  display: grid;
  width: 20px;
  height: 20px;
  place-items: center;
  border: 0;
  border-radius: 50%;
  color: var(--el-text-color-placeholder);
  background: transparent;
  font-size: 14px;
  cursor: pointer;
  opacity: 0;
  transition: opacity .15s;
}

.member-item:hover .remove-button {
  opacity: 1;
}

.remove-button:hover {
  color: var(--el-color-white);
  background: var(--el-color-danger);
}

.ghost-button,
.primary-button,
.danger-button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border-radius: 8px;
  font: inherit;
  font-size: 13px;
  cursor: pointer;
}

.ghost-button {
  border: 1px solid var(--el-border-color);
  color: var(--el-text-color-regular);
  background: var(--el-bg-color);
}

.ghost-button:hover:not(:disabled) {
  color: var(--el-color-primary);
  border-color: var(--el-color-primary-light-5);
}

.primary-button {
  border: 0;
  color: var(--el-color-white);
  background: var(--el-color-primary);
  font-weight: 600;
}

.primary-button:hover {
  background: var(--el-color-primary-light-3);
}

.primary-button:disabled {
  opacity: .6;
  cursor: not-allowed;
}

.danger-button {
  border: 1px solid var(--el-color-danger-light-5);
  color: var(--el-color-danger);
  background: var(--el-color-danger-light-9);
}

.danger-button:hover {
  color: var(--el-color-white);
  background: var(--el-color-danger);
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

@media (max-width: 900px) {
  .group-body {
    flex-direction: column;
    overflow-y: auto;
  }

  .chat-area {
    min-height: 420px;
  }

  .member-panel {
    width: 100%;
    flex-basis: auto;
    max-height: 260px;
  }
}
</style>
