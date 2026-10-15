<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Check, CopyDocument, Flag, Promotion, Refresh, User, UserFilled } from '@element-plus/icons-vue'
import { currentUserId, del, get, post } from '@/net'
import { downloadResourceFile } from '@/utils/resources'

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

type TaskItemType = 'course' | 'resource' | 'member_visit' | 'text'

interface TaskItem {
  id: number
  type: TaskItemType
  refId: number | null
  title: string
  myCompleted: boolean | null
  myCompletedAt: string | null
  completedCount: number | null
}

interface GroupTask {
  id: number
  title: string
  content: string
  accountId: number
  username: string
  createdAt: string
  items: TaskItem[]
  memberCount: number
  myCompleted: number
  totalItems: number
  totalCompletions: number
  progressPercent: number | null
}

interface TaskProgressMatrix {
  taskId: number
  title: string
  items: TaskItem[]
  members: {
    accountId: number
    username: string
    role: string
    completedItems: number
    totalItems: number
    percent: number | null
    itemStatuses: { itemId: number; completed: boolean; completedAt: string | null }[]
  }[]
}

interface NamedOption {
  id: number
  title: string
}

interface TaskItemDraft {
  type: TaskItemType
  refId: number | null
  title: string
}

const route = useRoute()
const router = useRouter()
const { t } = useI18n()

const groupId = Number(route.params.id)
const myId = currentUserId()

const detail = ref<GroupDetail | null>(null)
const messages = ref<GroupMessage[]>([])
const tasks = ref<GroupTask[]>([])
const loadFailed = ref(false)
const sending = ref(false)
const publishing = ref(false)
const draft = ref('')
const taskFormVisible = ref(false)
const taskForm = ref({ title: '', content: '' })
const taskItems = ref<TaskItemDraft[]>([])
const itemDraft = ref<TaskItemDraft>({ type: 'course', refId: null, title: '' })
const courseOptions = ref<NamedOption[]>([])
const resourceOptions = ref<NamedOption[]>([])
const optionsLoaded = ref(false)
const progressModalTaskId = ref<number | null>(null)
const progressMatrix = ref<TaskProgressMatrix | null>(null)
const progressLoading = ref(false)
const messageListEl = ref<HTMLElement | null>(null)
let pollTimer: ReturnType<typeof setInterval> | null = null

const isOwner = computed(() => detail.value?.group.role === 'owner')
const inviteCode = computed(() => detail.value?.group.inviteCode ?? '')
const taskByMessageId = computed(() => new Map(tasks.value.map(task => [task.id, task])))
/** 消息与其对应任务（type=task 时）的视图模型，便于模板内类型收窄 */
const messageViews = computed(() => messages.value.map(message => ({
  message,
  task: message.type === 'task' ? taskByMessageId.value.get(message.id) : undefined,
})))

function loadDetail() {
  get<GroupDetail>(`/api/groups/${groupId}`, data => {
    detail.value = data
    loadFailed.value = false
  }, () => {
    loadFailed.value = true
  })
}

function loadTasks() {
  get<GroupTask[]>(`/api/groups/${groupId}/tasks`, data => {
    tasks.value = data
  }, () => undefined)
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

function loadBindOptions() {
  if (optionsLoaded.value) return
  optionsLoaded.value = true
  get<{ courses: { id: number; title: string }[] }>('/api/courses/progress', data => {
    courseOptions.value = (data.courses ?? []).map(course => ({ id: course.id, title: course.title }))
  }, () => undefined)
  get<{ id: number; title: string }[]>('/api/resources', data => {
    resourceOptions.value = data.map(resource => ({ id: resource.id, title: resource.title }))
  }, () => undefined)
}

watch(taskFormVisible, visible => {
  if (visible) loadBindOptions()
})

function addTaskItem() {
  const item = itemDraft.value
  if (item.type === 'course' || item.type === 'resource') {
    if (item.refId == null) {
      ElMessage.warning(t('groups.itemNeedRef'))
      return
    }
    const options = item.type === 'course' ? courseOptions.value : resourceOptions.value
    const title = options.find(option => option.id === item.refId)?.title ?? ''
    taskItems.value = [...taskItems.value, { type: item.type, refId: item.refId, title }]
  } else {
    const title = item.title.trim()
      || (item.type === 'member_visit' ? t('groups.itemVisitDefault') : '')
    if (!title) {
      ElMessage.warning(t('groups.itemTitleRequired'))
      return
    }
    taskItems.value = [...taskItems.value, { type: item.type, refId: null, title }]
  }
  itemDraft.value = { type: item.type, refId: null, title: '' }
}

function removeTaskItem(index: number) {
  taskItems.value = taskItems.value.filter((_, i) => i !== index)
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
  const items = taskItems.value.map(item => ({ type: item.type, refId: item.refId, title: item.title }))
  post<GroupMessage>(`/api/groups/${groupId}/tasks`, { title, content, items }, message => {
    publishing.value = false
    taskFormVisible.value = false
    taskForm.value = { title: '', content: '' }
    taskItems.value = []
    itemDraft.value = { type: 'course', refId: null, title: '' }
    messages.value = [...messages.value, message]
    loadTasks()
    ElMessage.success(t('groups.taskPublished'))
    void nextTick(() => scrollMessagesToBottom())
  }, () => {
    publishing.value = false
  })
}

function itemTypeLabel(type: TaskItemType) {
  switch (type) {
    case 'course': return t('groups.itemTypeCourse')
    case 'resource': return t('groups.itemTypeResource')
    case 'member_visit': return t('groups.itemTypeVisit')
    default: return t('groups.itemTypeText')
  }
}

function goCourse(item: TaskItem) {
  if (item.refId != null) {
    void router.push({ name: 'course-study', params: { id: item.refId } })
  }
}

async function accessResource(item: TaskItem) {
  if (item.refId == null) return
  await downloadResourceFile(item.refId, item.title)
  loadTasks()
}

function completeItem(task: GroupTask, item: TaskItem) {
  post<GroupTask>(`/api/groups/${groupId}/tasks/${task.id}/items/${item.id}/complete`, {}, updated => {
    tasks.value = tasks.value.map(existing => existing.id === updated.id ? updated : existing)
    ElMessage.success(t('groups.marked'))
  })
}

function openProgress(task: GroupTask) {
  progressModalTaskId.value = task.id
  progressMatrix.value = null
  progressLoading.value = true
  get<TaskProgressMatrix>(`/api/groups/${groupId}/tasks/${task.id}/progress`, data => {
    progressMatrix.value = data
    progressLoading.value = false
  }, () => {
    progressLoading.value = false
    progressModalTaskId.value = null
  })
}

function closeProgress() {
  progressModalTaskId.value = null
  progressMatrix.value = null
}

function memberItemStatus(matrix: TaskProgressMatrix, memberId: number, itemId: number) {
  const member = matrix.members.find(entry => entry.accountId === memberId)
  return member?.itemStatuses.find(status => status.itemId === itemId)
}

function openMember(member: GroupMember) {
  void router.push({ name: 'group-member-detail', params: { id: groupId, memberId: member.accountId } })
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
  loadTasks()
  pollTimer = setInterval(() => {
    loadMessages()
    loadTasks()
  }, 10000)
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
        <button class="ghost-button" type="button" @click="loadMessages(true); loadTasks()">
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
          <template v-for="view in messageViews" :key="view.message.id">
            <div
                v-if="view.message.type === 'task'"
                class="task-card"
            >
              <div class="task-badge">
                <el-icon><Flag /></el-icon>
                {{ t('groups.taskBadge') }}
              </div>
              <h4>{{ view.message.title }}</h4>
              <p>{{ view.message.content }}</p>
              <template v-if="view.task">
                <div v-if="view.task.items.length" class="task-items">
                  <div v-for="item in view.task.items" :key="item.id" class="task-item">
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
                          @click="goCourse(item)"
                      >{{ t('groups.goStudy') }}</button>
                      <button
                          v-else-if="item.type === 'resource' && !item.myCompleted"
                          class="mini-button"
                          type="button"
                          @click="accessResource(item)"
                      >{{ t('groups.goDownload') }}</button>
                      <button
                          v-else-if="item.type === 'text' && !item.myCompleted"
                          class="mini-button"
                          type="button"
                          @click="completeItem(view.task!, item)"
                      >{{ t('groups.markComplete') }}</button>
                      <small v-else-if="item.type === 'member_visit' && !item.myCompleted" class="item-hint">
                        {{ t('groups.visitHint') }}
                      </small>
                      <small v-else-if="item.myCompleted && item.myCompletedAt" class="item-hint">
                        {{ t('groups.completedAt', { time: formatTime(item.myCompletedAt) }) }}
                      </small>
                    </span>
                    <small class="item-count">
                      {{ t('groups.completedBy', { done: item.completedCount ?? 0, total: view.task.memberCount }) }}
                    </small>
                  </div>
                </div>
                <div v-if="view.task.items.length" class="task-progress-row">
                  <span class="my-progress">
                    {{ t('groups.myProgress', { done: view.task.myCompleted, total: view.task.totalItems }) }}
                  </span>
                  <template v-if="isOwner && view.task.progressPercent != null">
                    <span class="overall-label">{{ t('groups.overallProgress') }}</span>
                    <span class="progress-track">
                      <span class="progress-fill" :style="{ width: view.task.progressPercent + '%' }"></span>
                    </span>
                    <span class="progress-percent">{{ view.task.progressPercent }}%</span>
                    <button class="mini-button" type="button" @click="openProgress(view.task!)">
                      {{ t('groups.viewProgress') }}
                    </button>
                  </template>
                </div>
              </template>
              <div class="message-meta">
                <span>{{ view.message.username || t('groups.unknownUser') }}</span>
                <span>{{ formatTime(view.message.createdAt) }}</span>
              </div>
            </div>
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
            <div class="field">
              <span>{{ t('groups.taskItems') }}</span>
              <div class="item-builder">
                <select v-model="itemDraft.type" class="item-select">
                  <option value="course">{{ t('groups.itemTypeCourse') }}</option>
                  <option value="resource">{{ t('groups.itemTypeResource') }}</option>
                  <option value="member_visit">{{ t('groups.itemTypeVisit') }}</option>
                  <option value="text">{{ t('groups.itemTypeText') }}</option>
                </select>
                <select
                    v-if="itemDraft.type === 'course'"
                    v-model="itemDraft.refId"
                    class="item-select grow"
                >
                  <option :value="null" disabled>{{ t('groups.itemCoursePlaceholder') }}</option>
                  <option v-for="course in courseOptions" :key="course.id" :value="course.id">
                    {{ course.title }}
                  </option>
                </select>
                <select
                    v-else-if="itemDraft.type === 'resource'"
                    v-model="itemDraft.refId"
                    class="item-select grow"
                >
                  <option :value="null" disabled>{{ t('groups.itemResourcePlaceholder') }}</option>
                  <option v-for="resource in resourceOptions" :key="resource.id" :value="resource.id">
                    {{ resource.title }}
                  </option>
                </select>
                <input
                    v-else
                    v-model="itemDraft.title"
                    type="text"
                    maxlength="200"
                    class="item-input grow"
                    :placeholder="itemDraft.type === 'member_visit'
                      ? t('groups.itemVisitDefault') : t('groups.itemTitlePlaceholder')"
                >
                <button class="ghost-button" type="button" @click="addTaskItem">{{ t('groups.addItem') }}</button>
              </div>
              <ul v-if="taskItems.length" class="item-draft-list">
                <li v-for="(item, index) in taskItems" :key="index">
                  <span class="item-type" :class="`type-${item.type}`">{{ itemTypeLabel(item.type) }}</span>
                  <span class="item-title">{{ item.title }}</span>
                  <button class="remove-inline" type="button" @click="removeTaskItem(index)">×</button>
                </li>
              </ul>
              <small v-else class="no-items-hint">{{ t('groups.noItems') }}</small>
            </div>
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
          <li
              v-for="member in detail?.members ?? []"
              :key="member.accountId"
              class="member-item clickable"
              @click="openMember(member)"
          >
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
                @click.stop="removeMember(member)"
            >
              ×
            </button>
          </li>
        </ul>
      </aside>
    </div>

    <div v-if="progressModalTaskId != null" class="modal-mask" @click.self="closeProgress">
      <div class="modal-panel">
        <header class="modal-header">
          <h3>{{ t('groups.progressMatrixTitle') }}</h3>
          <button class="remove-inline" type="button" @click="closeProgress">×</button>
        </header>
        <div v-if="progressLoading" class="modal-loading">…</div>
        <div v-else-if="progressMatrix" class="modal-body">
          <div class="matrix-item-legend">
            <span v-for="item in progressMatrix.items" :key="item.id" class="legend-chip">
              <span class="item-type" :class="`type-${item.type}`">{{ itemTypeLabel(item.type) }}</span>
              {{ item.title }}
              <small>{{ t('groups.completedBy', { done: item.completedCount ?? 0, total: progressMatrix.members.length }) }}</small>
            </span>
          </div>
          <ul class="matrix-member-list">
            <li v-for="member in progressMatrix.members" :key="member.accountId">
              <div class="matrix-member-head">
                <span class="matrix-member-name">
                  {{ member.username || t('groups.unknownUser') }}
                  <span v-if="member.role === 'owner'" class="sender-badge">{{ t('groups.roleOwner') }}</span>
                </span>
                <span class="matrix-member-percent">
                  {{ member.completedItems }}/{{ member.totalItems }}
                  <template v-if="member.percent != null"> · {{ member.percent }}%</template>
                </span>
              </div>
              <div class="matrix-status-row">
                <span
                    v-for="item in progressMatrix.items"
                    :key="item.id"
                    class="status-chip"
                    :class="{ done: memberItemStatus(progressMatrix, member.accountId, item.id)?.completed }"
                    :title="item.title"
                >
                  {{ memberItemStatus(progressMatrix, member.accountId, item.id)?.completed ? '✓' : '✗' }}
                  {{ item.title }}
                </span>
              </div>
            </li>
          </ul>
        </div>
      </div>
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

.item-builder {
  display: flex;
  gap: 8px;
  align-items: center;
}

.item-select,
.item-input {
  padding: 8px 10px;
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  color: var(--el-text-color-primary);
  background: var(--el-bg-color);
  font: inherit;
  font-size: 12px;
}

.item-select.grow,
.item-input.grow {
  flex: 1;
  min-width: 0;
}

.item-draft-list {
  display: flex;
  flex-direction: column;
  gap: 5px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.item-draft-list li {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  border: 1px dashed var(--el-border-color);
  border-radius: 8px;
  background: var(--el-bg-color);
  font-size: 12px;
}

.no-items-hint {
  color: var(--el-text-color-placeholder);
  font-size: 11px;
}

.remove-inline {
  display: grid;
  width: 20px;
  height: 20px;
  flex: 0 0 auto;
  place-items: center;
  border: 0;
  border-radius: 50%;
  color: var(--el-text-color-placeholder);
  background: transparent;
  font-size: 14px;
  cursor: pointer;
}

.remove-inline:hover {
  color: var(--el-color-white);
  background: var(--el-color-danger);
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

.member-item.clickable {
  cursor: pointer;
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

.modal-mask {
  position: fixed;
  inset: 0;
  z-index: 2000;
  display: grid;
  place-items: center;
  background: rgba(0, 0, 0, .35);
}

.modal-panel {
  width: min(640px, 92vw);
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  border-radius: 14px;
  background: var(--el-bg-color);
  box-shadow: var(--el-box-shadow);
  overflow: hidden;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.modal-header h3 {
  margin: 0;
  font-size: 15px;
}

.modal-loading {
  padding: 40px;
  text-align: center;
  color: var(--el-text-color-placeholder);
}

.modal-body {
  padding: 14px 18px;
  overflow-y: auto;
  display: grid;
  gap: 14px;
}

.matrix-item-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.legend-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 10px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 999px;
  font-size: 12px;
}

.legend-chip small {
  color: var(--el-text-color-placeholder);
}

.matrix-member-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.matrix-member-list li {
  padding: 10px 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  display: grid;
  gap: 8px;
}

.matrix-member-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
}

.matrix-member-name {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
}

.matrix-member-percent {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.matrix-status-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.status-chip {
  padding: 2px 9px;
  border-radius: 999px;
  color: var(--el-color-danger);
  background: var(--el-color-danger-light-9);
  font-size: 11px;
}

.status-chip.done {
  color: var(--el-color-success);
  background: var(--el-color-success-light-9);
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
