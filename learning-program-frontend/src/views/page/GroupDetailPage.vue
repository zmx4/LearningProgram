<script setup lang="ts">
import { computed, nextTick, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { ArrowLeft, CopyDocument, Flag, Refresh, Trophy, User } from '@element-plus/icons-vue'
import GroupMessageList from '@/components/group/GroupMessageList.vue'
import GroupMessageComposer from '@/components/group/GroupMessageComposer.vue'
import GroupTaskForm from '@/components/group/GroupTaskForm.vue'
import GroupMemberPanel from '@/components/group/GroupMemberPanel.vue'
import GroupProgressDialog from '@/components/group/GroupProgressDialog.vue'
import { useGroupDetail } from '@/composables/useGroupDetail'
import type { GroupMessage, GroupTask } from '@/types/group'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()

const groupId = Number(route.params.id)
const messageListRef = ref<InstanceType<typeof GroupMessageList> | null>(null)
const taskFormVisible = ref(false)
const progressTaskId = ref<number | null>(null)

const {
  myId,
  detail,
  messages,
  tasks,
  loadFailed,
  sending,
  draft,
  isOwner,
  inviteCode,
  loadDetail,
  loadMessages,
  loadTasks,
  sendMessage,
  goCourse,
  accessResource,
  goTest,
  completeItem,
  openMember,
  copyInviteCode,
  leaveGroup,
  removeMember,
} = useGroupDetail(groupId, () => messageListRef.value?.scrollToBottom())

const groupInitial = computed(() => detail.value?.group.name.trim().slice(0, 1) ?? '')
const memberCount = computed(() => detail.value?.members.length ?? detail.value?.group.memberCount ?? 0)
const ongoingCount = computed(() => tasks.value.filter(task => !task.myFinished).length)
const completedCount = computed(() => tasks.value.filter(task => task.myFinished).length)
const overdueCount = computed(() => tasks.value.filter(task => (
  task.dueAt && !task.myFinished && new Date(task.dueAt).getTime() < Date.now()
)).length)
const totalItems = computed(() => tasks.value.reduce((total, task) => total + task.totalItems, 0))
const completedItems = computed(() => tasks.value.reduce((total, task) => total + task.myCompleted, 0))
const completionRate = computed(() => totalItems.value ? Math.round(completedItems.value * 100 / totalItems.value) : 0)

function onTaskPublished(message: GroupMessage) {
  taskFormVisible.value = false
  messages.value = [...messages.value, message]
  loadTasks()
  ElMessage.success(t('groups.taskPublished'))
  void nextTick(() => messageListRef.value?.scrollToBottom())
}

function openProgress(task: GroupTask) {
  progressTaskId.value = task.id
}

function openLeaderboard() {
  void router.push({ name: 'leaderboard', query: { groupId } })
}

function refreshWorkspace() {
  loadMessages(true)
  loadTasks()
}
</script>

<template>
  <main class="group-detail">
    <header class="group-header">
      <div class="command-row">
        <button
            class="back-button"
            type="button"
            :aria-label="t('groups.back')"
            @click="router.push({ name: 'groups' })"
        >
          <el-icon><ArrowLeft /></el-icon>
          <span>{{ t('groups.back') }}</span>
        </button>

        <div class="group-identity">
          <span class="group-monogram" aria-hidden="true">{{ groupInitial }}</span>
          <div class="group-title">
            <h2>{{ detail?.group.name ?? '' }}</h2>
            <div class="group-meta">
              <span class="role-badge" :class="{ owner: isOwner }">
                {{ isOwner ? t('groups.roleOwner') : t('groups.roleMember') }}
              </span>
              <span class="member-total">
                <el-icon><User /></el-icon>
                {{ t('groups.memberCount', { count: memberCount }) }}
              </span>
              <button
                  class="code-chip"
                  type="button"
                  :title="t('groups.copyCode')"
                  @click="copyInviteCode"
              >
                <el-icon><CopyDocument /></el-icon>
                <span>{{ t('groups.inviteCode', { code: inviteCode }) }}</span>
              </button>
            </div>
          </div>
        </div>

        <div class="header-actions">
          <button class="ghost-button" type="button" @click="openLeaderboard">
            <el-icon><Trophy /></el-icon>
            <span>{{ t('groups.leaderboard') }}</span>
          </button>
          <button class="ghost-button refresh-button" type="button" @click="refreshWorkspace">
            <el-icon><Refresh /></el-icon>
            <span>{{ t('groups.refresh') }}</span>
          </button>
          <button class="danger-button" type="button" @click="leaveGroup">
            {{ isOwner ? t('groups.disband') : t('groups.leave') }}
          </button>
        </div>
      </div>

      <div class="command-status">
        <div class="status-lead">
          <span class="status-mark" aria-hidden="true">
            <el-icon><Flag /></el-icon>
          </span>
          <div>
            <strong>{{ t('groups.workspaceTitle') }}</strong>
            <span>{{ t('groups.taskCount', { count: tasks.length }) }}</span>
          </div>
        </div>
        <dl class="status-metrics">
          <div class="status-metric">
            <dt>{{ t('groups.ongoingTasks') }}</dt>
            <dd>{{ ongoingCount }}</dd>
          </div>
          <div class="status-metric" :class="{ 'is-alert': overdueCount > 0 }">
            <dt>{{ t('groups.overdueTasks') }}</dt>
            <dd>{{ overdueCount }}</dd>
          </div>
          <div class="status-metric is-done">
            <dt>{{ t('groups.completedTasks') }}</dt>
            <dd>{{ completedCount }}</dd>
          </div>
          <div class="status-metric">
            <dt>{{ t('groups.myCompletion') }}</dt>
            <dd>{{ completionRate }}<small>%</small></dd>
          </div>
        </dl>
      </div>
    </header>

    <section v-if="loadFailed && !detail" class="empty-panel">
      <h3>{{ t('groups.loadFailed') }}</h3>
      <button class="ghost-button" type="button" @click="loadDetail">{{ t('groups.retry') }}</button>
    </section>

    <div v-else class="group-body">
      <section class="chat-area">
        <GroupMessageList
            ref="messageListRef"
            :messages="messages"
            :tasks="tasks"
            :is-owner="isOwner"
            :my-id="myId"
            @go-course="goCourse"
            @access-resource="accessResource"
            @go-test="goTest"
            @complete-item="completeItem"
            @open-progress="openProgress"
        />

        <div class="composer-area">
          <GroupTaskForm
              v-if="isOwner"
              v-show="taskFormVisible"
              :group-id="groupId"
              @published="onTaskPublished"
              @close="taskFormVisible = false"
          />
          <GroupMessageComposer
              v-model:draft="draft"
              :sending="sending"
              :is-owner="isOwner"
              @send="sendMessage"
              @toggle-task-form="taskFormVisible = !taskFormVisible"
          />
        </div>
      </section>

      <GroupMemberPanel
          :members="detail?.members ?? []"
          :is-owner="isOwner"
          :my-id="myId"
          @open-member="openMember"
          @remove-member="removeMember"
      />
    </div>

    <GroupProgressDialog
        v-if="progressTaskId != null"
        :group-id="groupId"
        :task-id="progressTaskId"
        @close="progressTaskId = null"
    />
  </main>
</template>

<style scoped>
.group-detail {
  --command-ink: #071624;
  --command-panel: #0c2035;
  --command-text: #f7fbff;
  --command-muted: rgba(219, 234, 255, .72);
  --command-line: rgba(139, 195, 255, .2);
  --command-accent: #8ec5ff;
  display: flex;
  flex-direction: column;
  height: calc(100dvh - 100px);
  min-height: 600px;
  padding: 18px clamp(14px, 3%, 36px) 24px;
}

.group-detail ::selection {
  color: var(--el-color-white);
  background: var(--el-color-primary-dark-2);
}

.group-detail button:focus-visible {
  outline: 2px solid var(--el-color-primary);
  outline-offset: 2px;
}

.group-header {
  position: relative;
  flex: none;
  overflow: hidden;
  border: 1px solid rgba(139, 195, 255, .24);
  border-radius: 20px;
  color: var(--command-text);
  background: var(--command-ink);
  box-shadow: 0 24px 60px -42px rgba(2, 16, 36, .92);
  animation: command-arrive .58s cubic-bezier(.16, 1, .3, 1) both;
}

.group-header::after {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  height: 2px;
  content: '';
  background: var(--el-color-primary);
  transform-origin: left;
  animation: command-line .8s .12s cubic-bezier(.16, 1, .3, 1) both;
}

.command-row {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 20px;
  padding: 20px 22px 18px;
}

.back-button,
.ghost-button,
.danger-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  min-height: 36px;
  padding: 8px 13px;
  border-radius: 9px;
  font: inherit;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
  cursor: pointer;
  transition: color .2s, border-color .2s, background-color .2s, transform .2s;
}

.back-button,
.ghost-button {
  border: 1px solid var(--command-line);
  color: var(--command-muted);
  background: rgba(255, 255, 255, .035);
}

.back-button:hover,
.ghost-button:hover {
  border-color: rgba(139, 195, 255, .48);
  color: var(--command-text);
  background: rgba(139, 195, 255, .1);
  transform: translateY(-1px);
}

.command-row button:focus-visible {
  outline-color: var(--command-accent);
}

.danger-button {
  border: 1px solid rgba(245, 108, 108, .3);
  color: #ffb4b4;
  background: rgba(245, 108, 108, .08);
}

.danger-button:hover {
  border-color: rgba(245, 108, 108, .56);
  color: var(--el-color-white);
  background: var(--el-color-danger-dark-2);
}

.group-identity {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.group-monogram {
  display: grid;
  width: 50px;
  height: 50px;
  flex: 0 0 50px;
  place-items: center;
  border: 1px solid rgba(139, 195, 255, .46);
  border-radius: 15px;
  color: var(--command-accent);
  background: rgba(64, 158, 255, .14);
  font-size: 22px;
  font-weight: 700;
  line-height: 1;
  box-shadow: inset 0 0 0 5px rgba(139, 195, 255, .035);
}

.group-title {
  min-width: 0;
}

.group-title h2 {
  max-width: 100%;
  margin: 0 0 7px;
  overflow: hidden;
  color: var(--command-text);
  font-size: clamp(22px, 2.2vw, 30px);
  font-weight: 700;
  line-height: 1.16;
  letter-spacing: -.035em;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.group-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px 12px;
  color: var(--command-muted);
  font-size: 11px;
}

.role-badge {
  display: inline-flex;
  align-items: center;
  min-height: 20px;
  padding: 2px 8px;
  border: 1px solid rgba(144, 147, 153, .28);
  border-radius: 999px;
  color: #c7d3e2;
  background: rgba(144, 147, 153, .1);
  font-size: 10px;
  font-weight: 700;
}

.role-badge.owner {
  border-color: rgba(139, 195, 255, .34);
  color: var(--command-accent);
  background: rgba(64, 158, 255, .12);
}

.member-total {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  white-space: nowrap;
}

.code-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 25px;
  padding: 3px 9px;
  border: 1px dashed rgba(139, 195, 255, .38);
  border-radius: 999px;
  color: var(--command-accent);
  background: rgba(64, 158, 255, .07);
  font: inherit;
  font-size: 10px;
  font-weight: 600;
  cursor: pointer;
  transition: border-color .2s, background-color .2s;
}

.code-chip:hover {
  border-color: var(--command-accent);
  background: rgba(64, 158, 255, .15);
}

.header-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.refresh-button:active .el-icon {
  transform: rotate(180deg);
}

.refresh-button .el-icon {
  transition: transform .35s cubic-bezier(.2, .8, .2, 1);
}

.command-status {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 12px 22px;
  border-top: 1px solid var(--command-line);
  background: var(--command-panel);
}

.status-lead {
  display: flex;
  align-items: center;
  gap: 11px;
  min-width: 150px;
}

.status-mark {
  display: grid;
  width: 34px;
  height: 34px;
  flex: 0 0 34px;
  place-items: center;
  border: 1px solid rgba(139, 195, 255, .24);
  border-radius: 10px;
  color: var(--command-accent);
  background: rgba(139, 195, 255, .07);
}

.status-lead > div {
  display: grid;
  gap: 2px;
}

.status-lead strong {
  font-size: 12px;
  font-weight: 600;
}

.status-lead span:not(.status-mark) {
  color: var(--command-muted);
  font-size: 10px;
}

.status-metrics {
  display: flex;
  align-items: stretch;
  justify-content: flex-end;
  margin: 0;
}

.status-metric {
  display: grid;
  min-width: 102px;
  gap: 2px;
  padding: 0 18px;
  border-left: 1px solid var(--command-line);
  animation: status-rise .48s .16s cubic-bezier(.16, 1, .3, 1) both;
}

.status-metric:nth-child(2) { animation-delay: .2s; }
.status-metric:nth-child(3) { animation-delay: .24s; }
.status-metric:nth-child(4) { animation-delay: .28s; }

.status-metric dt {
  color: var(--command-muted);
  font-size: 10px;
  line-height: 1.3;
  white-space: nowrap;
}

.status-metric dd {
  margin: 0;
  color: var(--command-text);
  font-size: 21px;
  font-weight: 700;
  line-height: 1.05;
  letter-spacing: -.02em;
  font-variant-numeric: tabular-nums;
}

.status-metric dd small {
  margin-left: 2px;
  color: var(--command-muted);
  font-size: 11px;
  font-weight: 500;
}

.status-metric.is-alert dd {
  color: #ff9f9f;
}

.status-metric.is-done dd {
  color: #9ee6a8;
}

.group-body {
  display: flex;
  flex: 1;
  gap: 14px;
  min-height: 0;
  margin-top: 14px;
}

.chat-area {
  display: flex;
  flex: 1;
  min-width: 0;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid var(--el-border-color-light);
  border-radius: 18px;
  background: var(--el-bg-color);
  box-shadow: var(--el-box-shadow-lighter);
}

.composer-area {
  display: grid;
  flex: none;
  gap: 10px;
  padding: 12px 16px 14px;
  border-top: 1px solid var(--el-border-color-lighter);
  background: var(--el-bg-color);
}

.empty-panel {
  display: grid;
  justify-items: center;
  gap: 12px;
  margin-top: 14px;
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

.empty-panel .ghost-button {
  border-color: var(--el-border-color);
  color: var(--el-text-color-regular);
  background: var(--el-bg-color);
}

@keyframes command-arrive {
  from {
    opacity: 0;
    clip-path: inset(0 0 16% 0 round 20px);
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    clip-path: inset(0 0 0 0 round 20px);
    transform: translateY(0);
  }
}

@keyframes command-line {
  from { transform: scaleX(0); }
  to { transform: scaleX(1); }
}

@keyframes status-rise {
  from {
    opacity: 0;
    transform: translateY(7px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 1180px) {
  .command-row {
    gap: 14px;
    padding-inline: 18px;
  }

  .back-button span,
  .refresh-button span {
    display: none;
  }

  .back-button,
  .refresh-button {
    width: 36px;
    padding: 0;
  }

  .status-metric {
    min-width: 92px;
    padding-inline: 14px;
  }
}

@media (max-width: 900px) {
  .group-body {
    flex-direction: column;
    overflow-y: auto;
  }

  .chat-area {
    min-height: 440px;
  }

  .command-status {
    align-items: flex-start;
    flex-direction: column;
    gap: 12px;
  }

  .status-metrics {
    width: 100%;
    justify-content: space-between;
  }

  .status-metric {
    flex: 1;
    min-width: 0;
  }
}

@media (max-width: 760px) {
  .group-detail {
    height: calc(100dvh - 77px);
    padding: 12px 12px 16px;
  }

  .command-row {
    grid-template-columns: auto minmax(0, 1fr);
    gap: 12px;
    padding: 16px;
  }

  .header-actions {
    grid-column: 1 / -1;
    justify-content: flex-start;
  }

  .group-monogram {
    width: 44px;
    height: 44px;
    flex-basis: 44px;
    border-radius: 13px;
    font-size: 20px;
  }

  .group-title h2 {
    font-size: 23px;
  }

  .command-status {
    padding: 13px 16px;
  }

  .chat-area {
    min-height: 420px;
    border-radius: 15px;
  }
}

@media (max-width: 560px) {
  .group-detail {
    min-height: 560px;
  }

  .group-header {
    border-radius: 16px;
  }

  .group-identity {
    gap: 10px;
  }

  .group-title h2 {
    font-size: 20px;
  }

  .group-meta {
    gap: 6px 8px;
  }

  .member-total {
    display: none;
  }

  .header-actions {
    display: grid;
    grid-template-columns: auto auto 1fr;
  }

  .header-actions .ghost-button {
    width: auto;
    padding-inline: 11px;
  }

  .header-actions .ghost-button span {
    display: inline;
  }

  .header-actions .danger-button {
    justify-self: end;
  }

  .status-metrics {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px 0;
  }

  .status-metric {
    padding: 0 12px;
  }

  .status-metric:nth-child(odd) {
    border-left: 0;
  }

  .status-metric dd {
    font-size: 19px;
  }

  .composer-area {
    padding-inline: 12px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .group-header,
  .group-header::after,
  .status-metric {
    animation: none;
  }

  .group-detail button,
  .refresh-button .el-icon {
    transition-duration: .01ms;
  }
}
</style>
