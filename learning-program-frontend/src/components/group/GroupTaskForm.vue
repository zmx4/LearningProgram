<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { get, post } from '@/net'
import type { GroupMessage, NamedOption, TaskItemDraft, TaskItemType } from '@/types/group'

const props = defineProps<{ groupId: number }>()

const emit = defineEmits<{
  published: [message: GroupMessage]
  close: []
}>()

const { t } = useI18n()
const publishing = ref(false)
const taskForm = ref({ title: '', content: '', dueAt: '' })
const taskItems = ref<(TaskItemDraft & { draftId: number })[]>([])
const itemDraft = ref<TaskItemDraft>({
  type: 'course', refId: null, title: '', source: 'cet4', passScore: 60,
})
const courseOptions = ref<NamedOption[]>([])
const resourceOptions = ref<NamedOption[]>([])
const questionSetOptions = ref<NamedOption[]>([])
let nextDraftId = 1
let disposed = false

watch(() => itemDraft.value.type, () => {
  itemDraft.value.refId = null
  itemDraft.value.title = ''
  itemDraft.value.source = 'cet4'
  itemDraft.value.passScore = 60
})

function loadBindOptions() {
  get<{ courses: { id: number; title: string }[] }>('/api/courses/progress', data => {
    if (disposed) return
    courseOptions.value = (data.courses ?? []).map(course => ({ id: course.id, title: course.title }))
  }, () => undefined)
  get<{ id: number; title: string }[]>('/api/resources', data => {
    if (disposed) return
    resourceOptions.value = data.map(resource => ({ id: resource.id, title: resource.title }))
  }, () => undefined)
  get<{ id: number; title: string }[]>('/api/question-sets', data => {
    if (disposed) return
    questionSetOptions.value = data.map(set => ({ id: set.id, title: set.title }))
  }, () => undefined)
}

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

function addTaskItem() {
  const item = itemDraft.value
  const passScore = Number(item.passScore)
  if ((item.type === 'word_test' || item.type === 'question_set')
      && (!Number.isInteger(passScore) || passScore < 0 || passScore > 100)) {
    ElMessage.warning(t('groups.passScoreRequired'))
    return
  }
  if (item.type === 'word_test') {
    taskItems.value = [...taskItems.value, {
      type: item.type,
      refId: null,
      source: item.source ?? 'cet4',
      passScore,
      title: item.title.trim() || `${item.source?.toUpperCase()} ${t('groups.wordTestDefault')}（≥${passScore}）`,
      draftId: nextDraftId++,
    }]
  } else if (item.type === 'course' || item.type === 'resource' || item.type === 'question_set') {
    if (item.refId == null) {
      ElMessage.warning(t('groups.itemNeedRef'))
      return
    }
    const options = item.type === 'course'
      ? courseOptions.value
      : item.type === 'resource' ? resourceOptions.value : questionSetOptions.value
    const title = options.find(option => option.id === item.refId)?.title ?? ''
    taskItems.value = [...taskItems.value, {
      type: item.type,
      refId: item.refId,
      source: null,
      passScore: item.type === 'question_set' ? passScore : null,
      title: item.title.trim() || (item.type === 'question_set' ? `${title}（≥${passScore}）` : title),
      draftId: nextDraftId++,
    }]
  } else {
    const title = item.title.trim()
      || (item.type === 'member_visit' ? t('groups.itemVisitDefault') : '')
    if (!title) {
      ElMessage.warning(t('groups.itemTitleRequired'))
      return
    }
    taskItems.value = [...taskItems.value, {
      type: item.type,
      refId: null,
      source: null,
      passScore: null,
      title,
      draftId: nextDraftId++,
    }]
  }
  itemDraft.value = { type: item.type, refId: null, title: '', source: 'cet4', passScore: 60 }
}

function removeTaskItem(draftId: number) {
  taskItems.value = taskItems.value.filter(item => item.draftId !== draftId)
}

function resetForm() {
  taskForm.value = { title: '', content: '', dueAt: '' }
  taskItems.value = []
  itemDraft.value = { type: 'course', refId: null, title: '', source: 'cet4', passScore: 60 }
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
  const items = taskItems.value.map(({ type, refId, title: itemTitle, source, passScore }) => ({
    type, refId, title: itemTitle, source, passScore,
  }))
  post<GroupMessage>(`/api/groups/${props.groupId}/tasks`, {
    title, content, dueAt: taskForm.value.dueAt || null, items,
  }, message => {
    if (disposed) return
    publishing.value = false
    resetForm()
    emit('published', message)
  }, () => {
    if (!disposed) publishing.value = false
  })
}

onMounted(loadBindOptions)
onUnmounted(() => {
  disposed = true
})
</script>

<template>
  <div class="task-form">
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
    <label class="field">
      <span>{{ t('groups.dueAtLabel') }}</span>
      <input v-model="taskForm.dueAt" type="datetime-local">
    </label>
    <div class="field">
      <span>{{ t('groups.taskItems') }}</span>
      <div class="item-builder">
        <select v-model="itemDraft.type" class="item-select">
          <option value="course">{{ t('groups.itemTypeCourse') }}</option>
          <option value="resource">{{ t('groups.itemTypeResource') }}</option>
          <option value="member_visit">{{ t('groups.itemTypeVisit') }}</option>
          <option value="text">{{ t('groups.itemTypeText') }}</option>
          <option value="word_test">{{ t('groups.itemTypeWordTest') }}</option>
          <option value="question_set">{{ t('groups.itemTypeQuestionSet') }}</option>
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
        <select
            v-else-if="itemDraft.type === 'question_set'"
            v-model="itemDraft.refId"
            class="item-select grow"
        >
          <option :value="null" disabled>{{ t('groups.itemQuestionSetPlaceholder') }}</option>
          <option v-for="set in questionSetOptions" :key="set.id" :value="set.id">
            {{ set.title }}
          </option>
        </select>
        <select
            v-else-if="itemDraft.type === 'word_test'"
            v-model="itemDraft.source"
            class="item-select"
        >
          <option value="cet4">CET-4</option>
          <option value="cet6">CET-6</option>
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
        <template v-if="itemDraft.type === 'word_test' || itemDraft.type === 'question_set'">
          <label class="test-config">
            <span>{{ t('groups.passScore') }}</span>
            <input v-model.number="itemDraft.passScore" type="number" min="0" max="100">
          </label>
          <input v-model="itemDraft.title" type="text" maxlength="200" class="item-input grow"
                 :placeholder="t('groups.itemTitleOptional')">
        </template>
        <button class="ghost-button" type="button" @click="addTaskItem">{{ t('groups.addItem') }}</button>
      </div>
      <ul v-if="taskItems.length" class="item-draft-list">
        <li v-for="item in taskItems" :key="item.draftId">
          <span class="item-type" :class="`type-${item.type}`">{{ itemTypeLabel(item.type) }}</span>
          <span class="item-title">{{ item.title }}</span>
          <button class="remove-inline" type="button" @click="removeTaskItem(item.draftId)">×</button>
        </li>
      </ul>
      <small v-else class="no-items-hint">{{ t('groups.noItems') }}</small>
    </div>
    <div class="task-form-actions">
      <button class="ghost-button" type="button" @click="emit('close')">
        {{ t('groups.back') }}
      </button>
      <button class="primary-button" type="button" :disabled="publishing" @click="publishTask">
        {{ publishing ? t('groups.publishing') : t('groups.publish') }}
      </button>
    </div>
  </div>
</template>

<style scoped>
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
  flex-wrap: wrap;
}

.test-config {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.test-config input {
  width: 72px;
  padding: 8px;
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  color: var(--el-text-color-primary);
  background: var(--el-bg-color);
  font: inherit;
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

.ghost-button,
.primary-button {
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
</style>