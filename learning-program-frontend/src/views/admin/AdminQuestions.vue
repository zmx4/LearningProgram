<script setup lang="ts">
import {computed, onMounted, reactive, ref} from 'vue'
import {Collection, Delete, Edit, EditPen, Plus, View} from '@element-plus/icons-vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import {useI18n} from 'vue-i18n'
import {del, get, post, put} from '@/net'
import {renderMarkdown} from '@/utils/markdown'

interface TestType {
  id: number
  code: string
  name: string
  description: string | null
  createdAt: string
}

interface QuestionOption {
  key: string
  text: string
}

interface TestQuestion {
  id: number
  typeId: number
  kind: string
  content: {
    stem: string
    options: QuestionOption[]
    answer: string[]
    analysis?: string
  }
  createdAt: string
}

const types = ref<TestType[]>([])
const questions = ref<TestQuestion[]>([])
const loading = ref(false)
const saving = ref(false)
const analysisPreview = ref(false)
const typeSaving = ref(false)
const formVisible = ref(false)
const typeFormVisible = ref(false)
const editingId = ref<number | null>(null)
const editingTypeId = ref<number | null>(null)
const filterTypeId = ref<number | null>(null)
const filterKind = ref<string | null>(null)
const { t } = useI18n()

const kindOptions = [
  {value: 'single', label: 'admin.kindSingle'},
  {value: 'multiple', label: 'admin.kindMultiple'},
  {value: 'blank', label: 'admin.kindBlank'},
] as const

const form = reactive({
  typeId: null as number | null,
  kind: 'single',
  stem: '',
  options: [
    {key: 'A', text: ''},
    {key: 'B', text: ''},
  ] as QuestionOption[],
  answerKey: '',
  answerKeys: [] as string[],
  blankAnswers: [''] as string[],
  analysis: '',
})

const typeForm = reactive({
  code: '',
  name: '',
  description: '',
})

const isEdit = computed(() => editingId.value !== null)
const typeIsEdit = computed(() => editingTypeId.value !== null)
const isChoice = computed(() => form.kind !== 'blank')

const kindLabel = (kind: string): string => {
  const map: Record<string, string> = {single: t('admin.kindSingle'), multiple: t('admin.kindMultiple'), blank: t('admin.kindBlank')}
  return map[kind] ?? kind
}

const typeName = (typeId: number): string => types.value.find(item => item.id === typeId)?.name ?? ''

const filteredQuestions = computed(() => questions.value.filter(question =>
    (filterTypeId.value === null || question.typeId === filterTypeId.value)
    && (filterKind.value === null || question.kind === filterKind.value)))

function loadTypes(done?: () => void) {
  get<TestType[]>('/api/test-types', (data) => {
    types.value = data
    done?.()
  })
}

function loadQuestions() {
  loading.value = true
  questions.value = []
  let pending = types.value.length
  if (pending === 0) {
    loading.value = false
    return
  }
  for (const type of types.value) {
    get<TestQuestion[]>(`/api/test-questions?typeId=${type.id}`, (list) => {
      questions.value = questions.value.concat(list)
      pending -= 1
      if (pending === 0) {
        questions.value.sort((a, b) => a.typeId - b.typeId || a.id - b.id)
        loading.value = false
      }
    }, () => {
      pending -= 1
      if (pending === 0) {
        loading.value = false
      }
    })
  }
}

function nextOptionKey(): string {
  const used = new Set(form.options.map(option => option.key.toUpperCase()))
  for (let i = 0; i < 26; i++) {
    const letter = String.fromCharCode(65 + i)
    if (!used.has(letter)) {
      return letter
    }
  }
  return ''
}

function addOption() {
  if (form.options.length >= 10) {
    ElMessage.warning(t('admin.maxOptions'))
    return
  }
  form.options.push({key: nextOptionKey(), text: ''})
}

function removeOption(index: number) {
  const removed = form.options[index]?.key
  if (removed === undefined) return
  form.options.splice(index, 1)
  form.answerKey = form.answerKey === removed ? '' : form.answerKey
  form.answerKeys = form.answerKeys.filter(key => key !== removed)
}

function onKindChange() {
  form.answerKey = ''
  form.answerKeys = []
  form.blankAnswers = ['']
}

function openCreate() {
  editingId.value = null
  form.typeId = filterTypeId.value ?? types.value[0]?.id ?? null
  form.kind = 'single'
  form.stem = ''
  form.options = [
    {key: 'A', text: ''},
    {key: 'B', text: ''},
  ]
  form.answerKey = ''
  form.answerKeys = []
  form.blankAnswers = ['']
  form.analysis = ''
  analysisPreview.value = false
  formVisible.value = true
}

function openEdit(question: TestQuestion) {
  editingId.value = question.id
  form.typeId = question.typeId
  form.kind = question.kind
  form.stem = question.content.stem
  form.options = question.content.options.map(option => ({...option}))
  form.answerKey = question.content.answer[0] ?? ''
  form.answerKeys = [...question.content.answer]
  form.blankAnswers = question.kind === 'blank' ? [...question.content.answer] : ['']
  form.analysis = question.content.analysis ?? ''
  analysisPreview.value = false
  formVisible.value = true
}

function saveQuestion() {
  if (form.typeId === null || !form.stem.trim()) {
    ElMessage.warning(t('admin.fillQuestion'))
    return
  }
  saving.value = true
  const answer = form.kind === 'blank'
      ? form.blankAnswers
      : form.kind === 'single' ? [form.answerKey] : form.answerKeys
  const payload = {
    typeId: form.typeId,
    kind: form.kind,
    content: {
      stem: form.stem,
      options: isChoice.value ? form.options : [],
      answer,
      analysis: form.analysis,
    },
  }
  const onSaved = () => {
    ElMessage.success(t('admin.questionSaved'))
    saving.value = false
    formVisible.value = false
    loadQuestions()
  }
  const onFailed = () => {
    saving.value = false
  }
  if (isEdit.value) {
    put(`/api/admin/test-questions/${editingId.value}`, payload, onSaved, onFailed)
  } else {
    post('/api/admin/test-questions', payload, onSaved, onFailed)
  }
}

async function removeQuestion(question: TestQuestion) {
  try {
    await ElMessageBox.confirm(t('admin.confirmDeleteQuestion', {stem: question.content.stem.slice(0, 30)}), t('admin.deleteQuestionTitle'), {type: 'warning'})
    del(`/api/admin/test-questions/${question.id}`, null, () => {
      ElMessage.success(t('admin.questionDeleted'))
      loadQuestions()
    })
  } catch {
    // 用户取消确认时不执行删除
  }
}

function openTypeCreate() {
  editingTypeId.value = null
  typeForm.code = ''
  typeForm.name = ''
  typeForm.description = ''
  typeFormVisible.value = true
}

function openTypeEdit(type: TestType) {
  editingTypeId.value = type.id
  typeForm.code = type.code
  typeForm.name = type.name
  typeForm.description = type.description ?? ''
  typeFormVisible.value = true
}

function saveType() {
  typeSaving.value = true
  const payload = {code: typeForm.code, name: typeForm.name, description: typeForm.description}
  const onSaved = () => {
    ElMessage.success(t('admin.typeSaved'))
    typeSaving.value = false
    typeFormVisible.value = false
    loadTypes()
  }
  const onFailed = () => {
    typeSaving.value = false
  }
  if (typeIsEdit.value) {
    put(`/api/admin/test-types/${editingTypeId.value}`, payload, onSaved, onFailed)
  } else {
    post('/api/admin/test-types', payload, onSaved, onFailed)
  }
}

async function removeType(type: TestType) {
  try {
    await ElMessageBox.confirm(t('admin.confirmDeleteType', {name: type.name}), t('admin.deleteTypeTitle'), {type: 'warning'})
    del(`/api/admin/test-types/${type.id}`, null, () => {
      ElMessage.success(t('admin.typeDeleted'))
      loadTypes()
    })
  } catch {
    // 用户取消确认时不执行删除
  }
}

onMounted(() => {
  loadTypes(loadQuestions)
})
</script>

<template>
  <div class="admin-page">
    <section class="page-heading">
      <div>
        <p class="eyebrow">{{ t('admin.eyebrow') }}</p>
        <h2>{{ t('admin.questions') }}</h2>
        <p class="description">{{ t('admin.questionsDescription') }}</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">{{ t('admin.createQuestion') }}</el-button>
    </section>

    <section v-if="formVisible" class="panel">
      <div class="panel-title">
        <el-icon>
          <EditPen/>
        </el-icon>
        <h3>{{ isEdit ? t('admin.editQuestion') : t('admin.createQuestion') }}</h3></div>
      <el-form label-position="top" @submit.prevent="saveQuestion">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="t('admin.typeName')">
              <el-select v-model="form.typeId" :placeholder="t('admin.selectType')" style="width: 100%">
                <el-option v-for="type in types" :key="type.id" :label="type.name" :value="type.id"/>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="t('admin.kind')">
              <el-select v-model="form.kind" style="width: 100%" @change="onKindChange">
                <el-option v-for="kind in kindOptions" :key="kind.value" :label="t(kind.label)" :value="kind.value"/>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item :label="t('admin.stem')">
          <el-input v-model="form.stem" type="textarea" :rows="3" maxlength="1000"/>
        </el-form-item>

        <el-form-item v-if="isChoice" :label="t('admin.options')">
          <div class="options-editor">
            <div v-for="(option, index) in form.options" :key="index" class="option-row">
              <el-input v-model="option.key" class="option-key" maxlength="10"/>
              <el-input v-model="option.text" class="option-text" maxlength="255"/>
              <el-button text type="danger" :icon="Delete" @click="removeOption(index)"/>
            </div>
            <el-button :icon="Plus" @click="addOption">{{ t('admin.addOption') }}</el-button>
          </div>
        </el-form-item>

        <el-form-item v-if="form.kind === 'single'" :label="t('admin.answer')">
          <el-select v-model="form.answerKey" :placeholder="t('admin.selectAnswer')" style="width: 100%">
            <el-option v-for="option in form.options" :key="option.key" :label="`${option.key}. ${option.text}`" :value="option.key"/>
          </el-select>
        </el-form-item>

        <el-form-item v-else-if="form.kind === 'multiple'" :label="t('admin.answer')">
          <el-select v-model="form.answerKeys" multiple :placeholder="t('admin.selectAnswer')" style="width: 100%">
            <el-option v-for="option in form.options" :key="option.key" :label="`${option.key}. ${option.text}`" :value="option.key"/>
          </el-select>
        </el-form-item>

        <el-form-item v-else :label="t('admin.answer')">
          <div class="options-editor">
            <div v-for="(_, index) in form.blankAnswers" :key="index" class="option-row">
              <el-input v-model="form.blankAnswers[index]" maxlength="100" :placeholder="t('admin.blankAnswerPlaceholder', {index: index + 1})"/>
              <el-button text type="danger" :icon="Delete" @click="form.blankAnswers.splice(index, 1)"/>
            </div>
            <el-button :icon="Plus" @click="form.blankAnswers.push('')">{{ t('admin.addBlankAnswer') }}</el-button>
          </div>
        </el-form-item>

        <el-form-item :label="t('admin.analysis')">
          <div class="analysis-editor">
            <el-button
                text
                type="primary"
                :icon="analysisPreview ? Edit : View"
                class="analysis-toggle"
                @click="analysisPreview = !analysisPreview">
              {{ analysisPreview ? t('admin.editMode') : t('admin.preview') }}
            </el-button>
            <el-input v-if="!analysisPreview" v-model="form.analysis" type="textarea" :rows="2" maxlength="1000"/>
            <div v-else class="analysis-preview markdown-body" v-html="renderMarkdown(form.analysis)"></div>
          </div>
        </el-form-item>
        <div class="form-actions">
          <el-button @click="formVisible = false">{{ t('admin.cancel') }}</el-button>
          <el-button type="primary" :loading="saving" @click="saveQuestion">{{ t('admin.confirm') }}</el-button>
        </div>
      </el-form>
    </section>

    <section class="panel">
      <div class="panel-title">
        <el-icon>
          <EditPen/>
        </el-icon>
        <h3>{{ t('admin.questionBank') }}</h3></div>
      <div class="filters">
        <el-select v-model="filterTypeId" clearable :placeholder="t('admin.allTypes')" style="width: 200px">
          <el-option v-for="type in types" :key="type.id" :label="type.name" :value="type.id"/>
        </el-select>
        <el-select v-model="filterKind" clearable :placeholder="t('admin.allKinds')" style="width: 160px">
          <el-option v-for="kind in kindOptions" :key="kind.value" :label="t(kind.label)" :value="kind.value"/>
        </el-select>
      </div>
      <el-table v-loading="loading" :data="filteredQuestions" stripe>
        <el-table-column prop="id" label="ID" width="70"/>
        <el-table-column :label="t('admin.typeName')" width="120">
          <template #default="{ row }">{{ typeName(row.typeId) }}</template>
        </el-table-column>
        <el-table-column :label="t('admin.kind')" width="90">
          <template #default="{ row }">{{ kindLabel(row.kind) }}</template>
        </el-table-column>
        <el-table-column prop="content.stem" :label="t('admin.stem')" min-width="260" show-overflow-tooltip/>
        <el-table-column prop="createdAt" :label="t('admin.createdAt')" min-width="170"/>
        <el-table-column :label="t('admin.action')" width="170" align="center">
          <template #default="{ row }">
            <div class="row-actions">
              <el-button text type="primary" :icon="Edit" @click="openEdit(row)">{{ t('admin.edit') }}</el-button>
              <el-button text type="danger" :icon="Delete" @click="removeQuestion(row)">{{ t('admin.delete') }}</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <section class="panel">
      <div class="panel-title with-action">
        <div class="panel-title-inner">
          <el-icon>
            <Collection/>
          </el-icon>
          <h3>{{ t('admin.typeManagement') }}</h3>
        </div>
        <el-button :icon="Plus" @click="openTypeCreate">{{ t('admin.addType') }}</el-button>
      </div>

      <div v-if="typeFormVisible" class="type-form">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-input v-model="typeForm.code" :placeholder="t('admin.typeCode')" :disabled="typeIsEdit"/>
          </el-col>
          <el-col :span="8">
            <el-input v-model="typeForm.name" :placeholder="t('admin.typeName')"/>
          </el-col>
          <el-col :span="8">
            <el-input v-model="typeForm.description" :placeholder="t('admin.typeDescription')"/>
          </el-col>
        </el-row>
        <div class="form-actions">
          <el-button @click="typeFormVisible = false">{{ t('admin.cancel') }}</el-button>
          <el-button type="primary" :loading="typeSaving" @click="saveType">{{ t('admin.confirm') }}</el-button>
        </div>
      </div>

      <el-table :data="types" stripe>
        <el-table-column prop="code" :label="t('admin.typeCode')" min-width="140"/>
        <el-table-column prop="name" :label="t('admin.typeName')" min-width="120"/>
        <el-table-column prop="description" :label="t('admin.typeDescription')" min-width="220" show-overflow-tooltip/>
        <el-table-column prop="createdAt" :label="t('admin.createdAt')" min-width="170"/>
        <el-table-column :label="t('admin.action')" width="170" align="center">
          <template #default="{ row }">
            <div class="row-actions">
              <el-button text type="primary" :icon="Edit" @click="openTypeEdit(row)">{{ t('admin.edit') }}</el-button>
              <el-button text type="danger" :icon="Delete" @click="removeType(row)">{{ t('admin.delete') }}</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<style scoped>
.panel-title.with-action {
  justify-content: space-between;
}

.panel-title-inner {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--el-color-primary);
}

.filters {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.options-editor {
  display: flex;
  width: 100%;
  flex-direction: column;
  gap: 8px;
  align-items: flex-start;
}

.analysis-editor {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.analysis-toggle {
  justify-content: flex-start;
  padding: 0;
}

.analysis-preview {
  min-height: 56px;
  padding: 10px 12px;
  border: 1px dashed var(--el-border-color-lighter);
  border-radius: 6px;
  background: var(--el-bg-color);
}

.option-row {
  display: flex;
  width: 100%;
  gap: 8px;
  align-items: center;
}

.option-key {
  width: 72px;
  flex-shrink: 0;
}

.option-text {
  flex: 1;
}

.type-form {
  margin-bottom: 18px;
  padding: 16px;
  border: 1px dashed var(--el-border-color);
  border-radius: 10px;
}

.type-form .form-actions {
  margin-top: 12px;
}

@media (max-width: 700px) {
  .filters {
    flex-direction: column;
  }
}
</style>
