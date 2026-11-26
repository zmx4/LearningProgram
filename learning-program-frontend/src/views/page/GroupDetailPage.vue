<script setup lang="ts">
import { nextTick, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { ArrowLeft, CopyDocument, Refresh } from '@element-plus/icons-vue'
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
  completeItem,
  openMember,
  copyInviteCode,
  leaveGroup,
  removeMember,
} = useGroupDetail(groupId, () => messageListRef.value?.scrollToBottom())

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
        <GroupMessageList
            ref="messageListRef"
            :messages="messages"
            :tasks="tasks"
            :is-owner="isOwner"
            :my-id="myId"
            @go-course="goCourse"
            @access-resource="accessResource"
            @complete-item="completeItem"
            @open-progress="openProgress"
        />

        <div class="composer-area">
          <GroupTaskForm
              v-if="isOwner && taskFormVisible"
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

.composer-area {
  border-top: 1px solid var(--el-border-color-lighter);
  padding: 12px 16px;
  display: grid;
  gap: 10px;
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
}
</style>