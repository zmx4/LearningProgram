<script setup lang="ts">
import {computed, onMounted, reactive, ref} from 'vue'
import {Delete, Edit, Notebook, Plus} from '@element-plus/icons-vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import {useI18n} from 'vue-i18n'
import {del, get, post, put} from '@/net'

interface TestType {
  id: number
  code: string
  name: string
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

interface QuestionSet {
  id: number
  title: string
  description: string | null
  questionCount: number
  createdAt: string
}

interface QuestionSetDetail extends QuestionSet {
  questions: TestQuestion[]
}

const sets = ref<QuestionSet[]>([])
const types = ref<TestType[]>([])
const questions = ref<TestQuestion[]>([])
const loading = ref(false)
const saving = ref(false)
const bankLoading = ref(false)
const formVisible = ref(false)
const editingId = ref<number | null>(null)
const { t } = useI18n()

const form = reactive({
  title: '',
  description: '',
  questionIds: [] as number[],
})

const isEdit = computed(() => editingId.value !== null)

const kindLabel = (kind: string): string => {
  const map: Record<string, string> = {single: '单选', multiple: '多选', blank: '填空'}
  return map[kind] ?? kind
}

const questionLabel = computed(() => {
  const typeName = (typeId: number) => types.value.find(item => item.id === typeId)?.name ?? ''
  return (question: TestQuestion) => {
    const stem = question.content.stem.length > 30
        ? question.content.stem.slice(0, 30) + '…'
        : question.content.stem
    return `[${typeName(question.typeId)}][${kindLabel(question.kind)}] ${stem}`
  }
})

const groupedQuestions = computed(() => types.value.map(type => ({
  type,
  questions: questions.value.filter(question => question.typeId === type.id),
})).filter(group => group.questions.length > 0))

function loadSets() {
  loading.value = true
  get<QuestionSet[]>('/api/question-sets', (data) => {
    sets.value = data
    loading.value = false
  }, () => {
    loading.value = false
  })
}

function loadQuestionBank() {
  bankLoading.value = true
  get<TestType[]>('/api/test-types', (data) => {
    types.value = data
    let pending = data.length
    questions.value = []
    if (pending === 0) {
      bankLoading.value = false
      return
    }
    for (const type of data) {
      get<TestQuestion[]>(`/api/test-questions?typeId=${type.id}`, (list) => {
        questions.value = questions.value.concat(list)
        pending -= 1
        if (pending === 0) {
          questions.value.sort((a, b) => a.typeId - b.typeId || a.id - b.id)
          bankLoading.value = false
        }
      })
    }
  }, () => {
    bankLoading.value = false
  })
}

function openCreate() {
  editingId.value = null
  form.title = ''
  form.description = ''
  form.questionIds = []
  formVisible.value = true
}

function openEdit(set: QuestionSet) {
  get<QuestionSetDetail>(`/api/question-sets/${set.id}`, (detail) => {
    editingId.value = set.id
    form.title = detail.title
    form.description = detail.description ?? ''
    form.questionIds = detail.questions.map(question => question.id)
    formVisible.value = true
  })
}

function saveSet() {
  if (!form.title.trim()) {
    ElMessage.warning(t('admin.fillSetTitle'))
    return
  }
  saving.value = true
  const payload = {
    title: form.title,
    description: form.description,
    questionIds: form.questionIds,
  }
  const onSaved = () => {
    ElMessage.success(t('admin.setSaved'))
    saving.value = false
    formVisible.value = false
    loadSets()
  }
  const onFailed = () => {
    saving.value = false
  }
  if (isEdit.value) {
    put(`/api/admin/question-sets/${editingId.value}`, payload, onSaved, onFailed)
  } else {
    post('/api/admin/question-sets', payload, onSaved, onFailed)
  }
}

async function removeSet(set: QuestionSet) {
  try {
    await ElMessageBox.confirm(t('admin.confirmDeleteSet', {title: set.title}), t('admin.deleteSetTitle'), {type: 'warning'})
    del(`/api/admin/question-sets/${set.id}`, null, () => {
      ElMessage.success(t('admin.setDeleted'))
      loadSets()
    })
  } catch {
    // 用户取消确认时不执行删除
  }
}

onMounted(() => {
  loadSets()
  loadQuestionBank()
})
</script>

<template>
  <div class="admin-page">
    <section class="page-heading">
      <div>
        <p class="eyebrow">{{ t('admin.eyebrow') }}</p>
        <h2>{{ t('admin.questionSets') }}</h2>
        <p class="description">{{ t('admin.questionSetsDescription') }}</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">{{ t('admin.createSet') }}</el-button>
    </section>

    <section v-if="formVisible" class="panel">
      <div class="panel-title">
        <el-icon>
          <Notebook/>
        </el-icon>
        <h3>{{ isEdit ? t('admin.editSet') : t('admin.createSet') }}</h3></div>
      <el-form label-position="top" @submit.prevent="saveSet">
        <el-form-item :label="t('admin.setTitle')">
          <el-input v-model="form.title" maxlength="100"/>
        </el-form-item>
        <el-form-item :label="t('admin.setDescription')">
          <el-input v-model="form.description" type="textarea" :rows="2" maxlength="255"/>
        </el-form-item>
        <el-form-item :label="t('admin.setQuestions')">
          <el-select
              v-model="form.questionIds"
              multiple
              filterable
              :loading="bankLoading"
              :placeholder="t('admin.selectQuestions')"
              style="width: 100%">
            <el-option-group v-for="group in groupedQuestions" :key="group.type.id" :label="group.type.name">
              <el-option
                  v-for="question in group.questions"
                  :key="question.id"
                  :label="questionLabel(question)"
                  :value="question.id"/>
            </el-option-group>
          </el-select>
        </el-form-item>
        <div class="form-actions">
          <el-button @click="formVisible = false">{{ t('admin.cancel') }}</el-button>
          <el-button type="primary" :loading="saving" @click="saveSet">{{ t('admin.confirm') }}</el-button>
        </div>
      </el-form>
    </section>

    <section class="panel">
      <div class="panel-title">
        <el-icon>
          <Notebook/>
        </el-icon>
        <h3>{{ t('admin.questionSets') }}</h3></div>
      <el-table v-loading="loading" :data="sets" stripe>
        <el-table-column prop="title" :label="t('admin.setTitle')" min-width="160"/>
        <el-table-column prop="description" :label="t('admin.setDescription')" min-width="220" show-overflow-tooltip/>
        <el-table-column prop="questionCount" :label="t('admin.questionCount')" width="100"/>
        <el-table-column prop="createdAt" :label="t('admin.createdAt')" min-width="170"/>
        <el-table-column :label="t('admin.action')" width="150">
          <template #default="{ row }">
            <el-button text type="primary" :icon="Edit" @click="openEdit(row)">{{ t('admin.edit') }}</el-button>
            <el-button text type="danger" :icon="Delete" @click="removeSet(row)">{{ t('admin.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<style scoped>
.admin-page {
  width: 100%;
  max-width: 1180px;
  margin: 0 auto;
  padding: 42px 5% 60px;
}

.page-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.eyebrow {
  margin: 0 0 7px;
  color: var(--el-color-primary);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: .14em;
}

h2, h3, p {
  margin-top: 0;
}

h2 {
  margin-bottom: 8px;
  font-size: 26px;
}

.description {
  margin-bottom: 0;
  color: var(--el-text-color-secondary);
}

.panel {
  margin-bottom: 20px;
  padding: 24px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 14px;
  background: var(--el-bg-color);
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 18px;
  color: var(--el-color-primary);
}

.panel-title h3 {
  margin-bottom: 0;
  color: var(--el-text-color-primary);
  font-size: 17px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 700px) {
  .page-heading {
    align-items: flex-start;
    gap: 12px;
    flex-direction: column;
  }

  .panel {
    padding: 16px;
  }
}
</style>
