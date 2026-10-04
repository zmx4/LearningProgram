<script setup lang="ts">
import {computed, onMounted, reactive, ref} from 'vue'
import {ArrowDown, ArrowUp, Delete, Edit, Notebook, Plus, Reading, View} from '@element-plus/icons-vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import {useI18n} from 'vue-i18n'
import {del, get, post, put} from '@/net'
import {renderMarkdown} from '@/utils/markdown'

interface ChapterItem {
  id: number | null
  title: string
  content: string
}

interface QuestionSet {
  id: number
  title: string
  description: string | null
  questionCount: number
  createdAt: string
}

interface AdminCourse {
  id: number
  title: string
  description: string | null
  icon: string | null
  sortOrder: number
  createdAt: string
  chapterCount: number
  questionSetIds: number[] | null
  chapters: ChapterItem[] | null
}

const courses = ref<AdminCourse[]>([])
const sets = ref<QuestionSet[]>([])
const setsLoading = ref(false)
const loading = ref(false)
const saving = ref(false)
const formVisible = ref(false)
const editingId = ref<number | null>(null)
const { t } = useI18n()

const form = reactive({
  title: '',
  description: '',
  icon: '',
  sortOrder: 0,
  questionSetIds: [] as number[],
  chapters: [] as ChapterItem[],
})

const isEdit = computed(() => editingId.value !== null)
// 正在预览正文的章节下标，同一时间只预览一章
const previewChapter = ref<number | null>(null)

function toggleChapterPreview(index: number) {
  previewChapter.value = previewChapter.value === index ? null : index
}

function loadCourses() {
  loading.value = true
  get<AdminCourse[]>('/api/admin/courses', (data) => {
    courses.value = data
    loading.value = false
  }, () => {
    loading.value = false
  })
}

function loadSets() {
  setsLoading.value = true
  get<QuestionSet[]>('/api/question-sets', (data) => {
    sets.value = data
    setsLoading.value = false
  }, () => {
    setsLoading.value = false
  })
}

function openCreate() {
  editingId.value = null
  form.title = ''
  form.description = ''
  form.icon = ''
  form.sortOrder = courses.value.length + 1
  form.questionSetIds = []
  form.chapters = []
  formVisible.value = true
}

function openEdit(course: AdminCourse) {
  get<AdminCourse>(`/api/admin/courses/${course.id}`, (detail) => {
    editingId.value = detail.id
    form.title = detail.title
    form.description = detail.description ?? ''
    form.icon = detail.icon ?? ''
    form.sortOrder = detail.sortOrder
    form.questionSetIds = detail.questionSetIds ?? []
    form.chapters = (detail.chapters ?? []).map(chapter => ({
      id: chapter.id,
      title: chapter.title,
      content: chapter.content ?? '',
    }))
    formVisible.value = true
  })
}

function addChapter() {
  form.chapters.push({id: null, title: '', content: ''})
}

function removeChapter(index: number) {
  if (previewChapter.value === index) {
    previewChapter.value = null
  }
  form.chapters.splice(index, 1)
}

function moveChapter(index: number, offset: -1 | 1) {
  const target = index + offset
  if (target < 0 || target >= form.chapters.length) return
  const [chapter] = form.chapters.splice(index, 1)
  if (!chapter) return
  form.chapters.splice(target, 0, chapter)
}

function saveCourse() {
  if (!form.title.trim()) {
    ElMessage.warning(t('admin.fillCourseTitle'))
    return
  }
  saving.value = true
  const payload = {
    title: form.title,
    description: form.description,
    icon: form.icon,
    sortOrder: form.sortOrder,
    questionSetIds: form.questionSetIds,
    chapters: form.chapters.map((chapter, index) => ({
      id: chapter.id,
      title: chapter.title,
      content: chapter.content,
      sortOrder: index,
    })),
  }
  const onSaved = () => {
    ElMessage.success(t('admin.courseSaved'))
    saving.value = false
    formVisible.value = false
    loadCourses()
  }
  const onFailed = () => {
    saving.value = false
  }
  if (isEdit.value) {
    put(`/api/admin/courses/${editingId.value}`, payload, onSaved, onFailed)
  } else {
    post('/api/admin/courses', payload, onSaved, onFailed)
  }
}

async function removeCourse(course: AdminCourse) {
  try {
    await ElMessageBox.confirm(t('admin.confirmDeleteCourse', {title: course.title}), t('admin.deleteCourseTitle'), {type: 'warning'})
    del(`/api/admin/courses/${course.id}`, null, () => {
      ElMessage.success(t('admin.courseDeleted'))
      loadCourses()
    })
  } catch {
    // 用户取消确认时不执行删除
  }
}

onMounted(() => {
  loadCourses()
  loadSets()
})
</script>

<template>
  <div class="admin-page">
    <section class="page-heading">
      <div>
        <p class="eyebrow">{{ t('admin.eyebrow') }}</p>
        <h2>{{ t('admin.courses') }}</h2>
        <p class="description">{{ t('admin.coursesDescription') }}</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">{{ t('admin.createCourse') }}</el-button>
    </section>

    <section v-if="formVisible" class="panel">
      <div class="panel-title">
        <el-icon>
          <Notebook/>
        </el-icon>
        <h3>{{ isEdit ? t('admin.editCourse') : t('admin.createCourse') }}</h3></div>
      <el-form label-position="top" @submit.prevent="saveCourse">
        <el-form-item :label="t('admin.courseTitle')">
          <el-input v-model="form.title" maxlength="100"/>
        </el-form-item>
        <el-form-item :label="t('admin.courseDescription')">
          <el-input v-model="form.description" type="textarea" :rows="2" maxlength="500"/>
        </el-form-item>
        <div class="inline-fields">
          <el-form-item :label="t('admin.courseIcon')">
            <el-input v-model="form.icon" :placeholder="t('admin.courseIconPlaceholder')" maxlength="16" clearable/>
          </el-form-item>
          <el-form-item :label="t('admin.courseSort')">
            <el-input-number v-model="form.sortOrder" :min="0" :max="9999"/>
          </el-form-item>
          <el-form-item :label="t('admin.selectQuestionSets')" class="inline-grow">
            <el-select
                v-model="form.questionSetIds"
                multiple
                filterable
                :loading="setsLoading"
                :placeholder="t('admin.selectQuestionSets')"
                style="width: 100%">
              <el-option
                  v-for="set in sets"
                  :key="set.id"
                  :label="`[${set.questionCount} 题] ${set.title}`"
                  :value="set.id"/>
            </el-select>
          </el-form-item>
        </div>
        <el-form-item :label="t('admin.courseChapters')">
          <div class="chapter-editor">
            <div v-for="(chapter, index) in form.chapters" :key="index" class="chapter-row">
              <div class="chapter-head">
                <span class="chapter-index">{{ index + 1 }}</span>
                <el-input v-model="chapter.title" :placeholder="t('admin.chapterTitle')" maxlength="150"/>
                <div class="chapter-tools">
                  <el-button
                      text
                      :icon="previewChapter === index ? Edit : View"
                      @click="toggleChapterPreview(index)">
                    {{ previewChapter === index ? t('admin.editMode') : t('admin.preview') }}
                  </el-button>
                  <el-button text :icon="ArrowUp" :disabled="index === 0" @click="moveChapter(index, -1)"/>
                  <el-button text :icon="ArrowDown" :disabled="index === form.chapters.length - 1"
                             @click="moveChapter(index, 1)"/>
                  <el-button text type="danger" :icon="Delete" @click="removeChapter(index)"/>
                </div>
              </div>
              <el-input
                  v-if="previewChapter !== index"
                  v-model="chapter.content"
                  type="textarea"
                  :rows="3"
                  :placeholder="t('admin.chapterContent')"/>
              <div v-else class="chapter-preview markdown-body" v-html="renderMarkdown(chapter.content)"></div>
            </div>
            <div class="chapter-editor-footer">
              <el-button :icon="Plus" @click="addChapter">{{ t('admin.addChapter') }}</el-button>
              <span class="markdown-hint">{{ t('admin.markdownSupported') }}</span>
            </div>
          </div>
        </el-form-item>
        <div class="form-actions">
          <el-button @click="formVisible = false">{{ t('admin.cancel') }}</el-button>
          <el-button type="primary" :loading="saving" @click="saveCourse">{{ t('admin.confirm') }}</el-button>
        </div>
      </el-form>
    </section>

    <section class="panel">
      <div class="panel-title">
        <el-icon>
          <Reading/>
        </el-icon>
        <h3>{{ t('admin.courses') }}</h3></div>
      <el-table v-loading="loading" :data="courses" stripe>
        <el-table-column prop="icon" :label="t('admin.courseIcon')" width="70" align="center"/>
        <el-table-column prop="title" :label="t('admin.courseTitle')" min-width="150"/>
        <el-table-column prop="description" :label="t('admin.courseDescription')" min-width="200"
                         show-overflow-tooltip/>
        <el-table-column prop="chapterCount" :label="t('admin.chapterCount')" width="90" align="center"/>
        <el-table-column :label="t('admin.questionSetCount')" width="90" align="center">
          <template #default="{ row }">
            {{ (row.questionSetIds ?? []).length }}
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" :label="t('admin.sortOrder')" width="80" align="center"/>
        <el-table-column prop="createdAt" :label="t('admin.createdAt')" min-width="170"/>
        <el-table-column :label="t('admin.action')" width="170" align="center">
          <template #default="{ row }">
            <div class="row-actions">
              <el-button text type="primary" :icon="Edit" @click="openEdit(row)">{{ t('admin.edit') }}</el-button>
              <el-button text type="danger" :icon="Delete" @click="removeCourse(row)">{{ t('admin.delete') }}</el-button>
            </div>
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

.inline-fields {
  display: flex;
  gap: 14px;
  align-items: flex-start;
}

.inline-fields .el-form-item {
  flex: none;
}

.inline-grow {
  flex: 1 !important;
  min-width: 0;
}

.chapter-editor {
  display: flex;
  flex-direction: column;
  gap: 14px;
  width: 100%;
}

.chapter-row {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  background: var(--el-fill-color-lighter);
}

.chapter-head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.chapter-index {
  display: grid;
  place-items: center;
  width: 26px;
  height: 26px;
  border-radius: 8px;
  color: var(--el-color-white);
  background: var(--el-color-primary);
  font-size: 13px;
  font-weight: 600;
  flex: none;
}

.chapter-head .el-input {
  flex: 1;
}

.chapter-tools {
  display: flex;
  flex: none;
}

.chapter-tools .el-button + .el-button {
  margin-left: 0;
}

.chapter-preview {
  min-height: 72px;
  padding: 10px 12px;
  border: 1px dashed var(--el-border-color-lighter);
  border-radius: 6px;
  background: var(--el-bg-color);
}

.chapter-editor-footer {
  display: flex;
  align-items: center;
  gap: 12px;
}

.markdown-hint {
  color: var(--el-text-color-placeholder);
  font-size: 12px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.row-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.row-actions .el-button + .el-button {
  margin-left: 0;
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

  .inline-fields {
    flex-direction: column;
  }

  .inline-fields .el-form-item {
    width: 100%;
  }
}
</style>
