<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { get } from '@/net'
import type { TaskItemType, TaskProgressMatrix } from '@/types/group'

const props = defineProps<{
  groupId: number
  taskId: number
}>()

const emit = defineEmits<{ close: [] }>()
const { t } = useI18n()
const progressMatrix = ref<TaskProgressMatrix | null>(null)
const progressLoading = ref(true)
let disposed = false

function loadProgress() {
  get<TaskProgressMatrix>(`/api/groups/${props.groupId}/tasks/${props.taskId}/progress`, data => {
    if (disposed) return
    progressMatrix.value = data
    progressLoading.value = false
  }, () => {
    if (disposed) return
    progressLoading.value = false
    emit('close')
  })
}

function memberItemStatus(matrix: TaskProgressMatrix, memberId: number, itemId: number) {
  const member = matrix.members.find(entry => entry.accountId === memberId)
  return member?.itemStatuses.find(status => status.itemId === itemId)
}

function itemTypeLabel(type: TaskItemType) {
  switch (type) {
    case 'course': return t('groups.itemTypeCourse')
    case 'resource': return t('groups.itemTypeResource')
    case 'member_visit': return t('groups.itemTypeVisit')
    default: return t('groups.itemTypeText')
  }
}

onMounted(loadProgress)
onUnmounted(() => {
  disposed = true
})
</script>

<template>
  <div class="modal-mask" @click.self="emit('close')">
    <div class="modal-panel">
      <header class="modal-header">
        <h3>{{ t('groups.progressMatrixTitle') }}</h3>
        <button class="remove-inline" type="button" @click="emit('close')">×</button>
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
</template>

<style scoped>
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

.sender-badge {
  padding: 1px 7px;
  border-radius: 999px;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  font-size: 10px;
  font-weight: 600;
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
</style>