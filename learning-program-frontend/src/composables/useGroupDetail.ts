import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { currentUserId, del, get, post } from '@/net'
import { downloadResourceFile } from '@/utils/resources'
import type { GroupDetail, GroupMember, GroupMessage, GroupTask, TaskItem } from '@/types/group'

export function useGroupDetail(groupId: number, scrollToBottom: () => void) {
  const router = useRouter()
  const { t } = useI18n()
  const myId = currentUserId()

  const detail = ref<GroupDetail | null>(null)
  const messages = ref<GroupMessage[]>([])
  const tasks = ref<GroupTask[]>([])
  const loadFailed = ref(false)
  const sending = ref(false)
  const draft = ref('')
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

  function loadTasks() {
    get<GroupTask[]>(`/api/groups/${groupId}/tasks`, data => {
      tasks.value = data
    }, () => undefined)
  }

  function loadMessages(scrollToBottomOnLoad = false) {
    get<GroupMessage[]>(`/api/groups/${groupId}/messages`, data => {
      const previous = messages.value
      const lastId = previous[previous.length - 1]?.id
      const newLastId = data[data.length - 1]?.id
      // 轮询通常只是追加新消息：保留旧数组、只拼接新增部分，
      // 避免整体替换导致消息列表（含任务卡片）全量重渲染。
      // 前缀对不上（消息被清理、重新进入页面）时退回整体替换。
      const isAppend = previous.length > 0
        && data.length >= previous.length
        && data[previous.length - 1]?.id === lastId
      if (isAppend) {
        if (data.length > previous.length) {
          messages.value = [...previous, ...data.slice(previous.length)]
        }
      } else {
        messages.value = data
      }
      if (scrollToBottomOnLoad || (lastId !== undefined && newLastId !== lastId)) {
        void nextTick(scrollToBottom)
      }
    }, () => {
      if (!messages.value.length) loadFailed.value = true
    })
  }

  function onVisibilityChange() {
    if (document.hidden) return
    loadMessages()
    loadTasks()
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
      void nextTick(scrollToBottom)
    }, () => {
      sending.value = false
    })
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

  onMounted(() => {
    loadDetail()
    loadMessages(true)
    loadTasks()
    // 页面切到后台时跳过轮询，回到前台立即补一次，避免看不见的标签页持续拉取和重渲染
    pollTimer = setInterval(() => {
      if (document.hidden) return
      loadMessages()
      loadTasks()
    }, 10000)
    document.addEventListener('visibilitychange', onVisibilityChange)
  })

  onUnmounted(() => {
    if (pollTimer) clearInterval(pollTimer)
    document.removeEventListener('visibilitychange', onVisibilityChange)
  })

  return {
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
  }
}