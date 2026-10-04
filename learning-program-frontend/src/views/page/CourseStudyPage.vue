<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ArrowLeft, ArrowRight, Back, Check, CircleCheckFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { get, post } from '@/net'
import { renderMarkdown } from '@/utils/markdown'

interface Chapter {
  id: number
  title: string
  content: string | null
}

interface QuestionSetInfo {
  id: number
  title: string
  description: string | null
  questionCount: number
}

interface CourseDetail {
  id: number
  title: string
  description: string | null
  icon: string | null
  questionSets: QuestionSetInfo[] | null
  chapters: Chapter[]
}

interface CourseProgress {
  courseId: number
  chapterCount: number
  studiedCount: number
  status: 'not_started' | 'in_progress' | 'completed'
  studiedChapterIds: number[]
}

interface StudyResult {
  studiedCount: number
  totalCount: number
  status: string
}

const route = useRoute()
const router = useRouter()
const { t } = useI18n()

const courseId = Number(route.params.id)
const loading = ref(true)
const notFound = ref(false)
const detail = ref<CourseDetail | null>(null)
const studiedIds = ref<number[]>([])
const current = ref(0)
const submitting = ref(false)
const justCompleted = ref(false)
// 当前章节进入页面的时间，标记已学时换算成学习时长上报
const chapterShownAt = ref(Date.now())

const chapters = computed(() => detail.value?.chapters ?? [])
const questionSets = computed(() => detail.value?.questionSets ?? [])
const currentChapter = computed(() => chapters.value[current.value] ?? null)
const renderedContent = computed(() => renderMarkdown(currentChapter.value?.content))
const totalCount = computed(() => chapters.value.length)
const studiedCount = computed(() => studiedIds.value.length)
const allStudied = computed(() => totalCount.value > 0 && studiedCount.value >= totalCount.value)
const progressPercent = computed(() =>
    totalCount.value ? Math.min(100, Math.round(studiedCount.value / totalCount.value * 100)) : 0)
const remaining = computed(() => Math.max(0, totalCount.value - studiedCount.value))

const isStudied = computed(() =>
    currentChapter.value !== null && studiedIds.value.includes(currentChapter.value.id))

function loadPage() {
  get<CourseDetail>(`/api/courses/${courseId}`, data => {
    detail.value = data
    get<CourseProgress>(`/api/courses/${courseId}/progress`, progress => {
      studiedIds.value = [...progress.studiedChapterIds]
      loading.value = false
      if (chapters.value.length) {
        enterChapter(firstUnstudied(0) ?? 0)
      }
    }, () => {
      // 进度接口失败不阻塞学习，按未学过处理
      loading.value = false
      if (chapters.value.length) enterChapter(0)
    })
  }, () => {
    loading.value = false
    notFound.value = true
  })
}

function firstUnstudied(from: number): number | null {
  for (let index = from; index < chapters.value.length; index += 1) {
    const chapter = chapters.value[index]
    if (chapter && !studiedIds.value.includes(chapter.id)) return index
  }
  return null
}

function enterChapter(index: number) {
  current.value = index
  chapterShownAt.value = Date.now()
}

function goPrev() {
  if (current.value > 0) enterChapter(current.value - 1)
}

function goNext() {
  if (current.value < totalCount.value - 1) enterChapter(current.value + 1)
}

function markStudied() {
  const chapter = currentChapter.value
  if (!chapter || submitting.value) return
  submitting.value = true
  const duration = Math.min(Math.max(Math.round((Date.now() - chapterShownAt.value) / 1000), 0), 600)
  post<StudyResult>(`/api/courses/${courseId}/progress`, { chapterId: chapter.id, durationSeconds: duration },
      data => {
        submitting.value = false
        if (!studiedIds.value.includes(chapter.id)) {
          studiedIds.value = [...studiedIds.value, chapter.id]
        }
        if (data.status === 'completed') {
          justCompleted.value = true
          return
        }
        enterChapter(firstUnstudied(current.value + 1) ?? current.value)
      }, () => {
        submitting.value = false
        ElMessage.warning(t('courses.study.reportFailed'))
      })
}

function reviewAgain() {
  justCompleted.value = false
  enterChapter(0)
}

function startPractice(set: QuestionSetInfo) {
  router.push({name: 'tests-knowledge', query: {set: String(set.id)}})
}

loadPage()
</script>

<template>
  <main class="study-page">
    <section v-if="loading" class="empty-panel">
      <p>{{ t('courses.loading') }}</p>
    </section>

    <section v-else-if="notFound" class="empty-panel">
      <h3>{{ t('courses.study.notFound') }}</h3>
      <button class="secondary-button" type="button" @click="router.push({ name: 'courses' })">
        {{ t('courses.back') }}
      </button>
    </section>

    <template v-else>
      <section class="study-heading">
        <button class="back-button" type="button" @click="router.push({ name: 'courses' })">
          <el-icon><Back /></el-icon>
          {{ t('courses.back') }}
        </button>
        <div class="study-title">
          <p class="eyebrow">COURSE</p>
          <h2><span v-if="detail?.icon" class="study-icon">{{ detail.icon }}</span>{{ detail?.title }}</h2>
          <p v-if="detail?.description" class="study-desc">{{ detail.description }}</p>
          <p v-if="questionSets.length" class="study-practice">
            {{ t('courses.study.practiceCount', { count: questionSets.length }) }}
          </p>
        </div>
        <div class="study-progress">
          <div class="study-progress-text">
            <span>{{ t('courses.studiedLabel', { studied: studiedCount, total: totalCount }) }}</span>
            <span>{{ progressPercent }}%</span>
          </div>
          <div class="study-bar">
            <div class="study-bar-fill" :style="{ width: `${progressPercent}%` }"></div>
          </div>
          <span class="study-remaining">{{ t('courses.study.remaining', { count: remaining }) }}</span>
        </div>
      </section>

      <section v-if="!totalCount" class="empty-panel">
        <h3>{{ t('courses.study.noChapters') }}</h3>
        <button class="secondary-button" type="button" @click="router.push({ name: 'courses' })">
          {{ t('courses.back') }}
        </button>
      </section>

      <section v-else-if="justCompleted" class="celebrate-card">
        <el-icon class="celebrate-icon"><CircleCheckFilled /></el-icon>
        <h3>{{ t('courses.study.celebrateTitle') }}</h3>
        <p>{{ t('courses.study.celebrateDescription') }}</p>
        <div class="celebrate-actions">
          <button class="secondary-button" type="button" @click="reviewAgain">
            {{ t('courses.study.reviewAgain') }}
          </button>
          <button class="primary-button" type="button" @click="router.push({ name: 'courses' })">
            {{ t('courses.study.backToCourses') }}
          </button>
        </div>
      </section>

      <template v-else>
        <article v-if="currentChapter" class="chapter-card">
          <div class="chapter-head">
            <span class="chapter-index">
              {{ t('courses.study.questionOf', { index: current + 1, total: totalCount }) }}
            </span>
            <span v-if="isStudied" class="chapter-studied">
              <el-icon><Check /></el-icon>
              {{ t('courses.study.studiedTag') }}
            </span>
          </div>
          <h3 class="chapter-title">{{ currentChapter.title }}</h3>
          <div v-if="renderedContent" class="chapter-content markdown-body" v-html="renderedContent"></div>

          <div class="chapter-actions">
            <button class="secondary-button" type="button" :disabled="current === 0" @click="goPrev">
              <el-icon><ArrowLeft /></el-icon>
              {{ t('courses.study.prevChapter') }}
            </button>
            <button
              v-if="current < totalCount - 1"
              class="secondary-button"
              type="button"
              @click="goNext"
            >
              {{ t('courses.study.nextChapter') }}
              <el-icon><ArrowRight /></el-icon>
            </button>
            <button
              class="primary-button"
              type="button"
              :disabled="submitting"
              @click="markStudied"
            >
              <el-icon><CircleCheckFilled /></el-icon>
              {{ t('courses.study.markStudied') }}
            </button>
          </div>
        </article>

        <div class="chapter-dots">
          <button
            v-for="(chapter, index) in chapters"
            :key="chapter.id"
            type="button"
            class="dot"
            :class="{ active: index === current, studied: studiedIds.includes(chapter.id) }"
            @click="enterChapter(index)"
          >
            {{ index + 1 }}
          </button>
        </div>

        <section v-if="questionSets.length" class="practice-card">
          <div class="practice-head">
            <h3>{{ t('courses.study.practiceSection') }}</h3>
            <span>{{ t('courses.study.practiceHint') }}</span>
          </div>
          <article v-for="set in questionSets" :key="set.id" class="practice-item">
            <div class="practice-info">
              <strong>{{ set.title }}</strong>
              <p v-if="set.description">{{ set.description }}</p>
              <span class="practice-count">{{ t('courses.study.practiceQuestionCount', { count: set.questionCount }) }}</span>
            </div>
            <button class="practice-button" type="button" @click="startPractice(set)">
              {{ t('courses.study.startPractice') }}
              <el-icon><ArrowRight /></el-icon>
            </button>
          </article>
        </section>
      </template>
    </template>
  </main>
</template>

<style scoped>
.study-page {
  max-width: 860px;
  margin: 0 auto;
  padding: 42px 5% 60px;
}

.eyebrow {
  margin: 0 0 6px;
  color: var(--el-text-color-placeholder);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: .13em;
}

.study-heading {
  display: grid;
  gap: 14px;
  margin-bottom: 22px;
}

.back-button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  justify-self: start;
  padding: 7px 12px;
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  color: var(--el-text-color-regular);
  background: var(--el-bg-color);
  font: inherit;
  font-size: 13px;
  cursor: pointer;
}

.back-button:hover {
  color: var(--el-color-primary);
  border-color: var(--el-color-primary-light-5);
}

.study-title h2 {
  margin: 0 0 6px;
  font-size: 24px;
}

.study-icon {
  margin-right: 8px;
}

.study-desc {
  margin: 0 0 4px;
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.study-practice {
  margin: 0;
  color: var(--el-color-primary);
  font-size: 12px;
  font-weight: 600;
}

.study-progress {
  max-width: 420px;
}

.study-progress-text {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.study-bar {
  height: 8px;
  border-radius: 6px;
  background: var(--el-fill-color-light);
  overflow: hidden;
}

.study-bar-fill {
  height: 100%;
  border-radius: 6px;
  background: var(--el-color-primary);
  transition: width .3s ease;
}

.study-remaining {
  display: inline-block;
  margin-top: 6px;
  color: var(--el-text-color-placeholder);
  font-size: 12px;
}

.chapter-card {
  padding: 28px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 16px;
  background: var(--el-bg-color);
  box-shadow: var(--el-box-shadow-lighter);
}

.chapter-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 14px;
}

.chapter-index {
  color: var(--el-color-primary);
  font-size: 13px;
  font-weight: 700;
}

.chapter-studied {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 9px;
  border-radius: 999px;
  color: var(--el-color-success);
  background: var(--el-color-success-light-8);
  font-size: 11px;
  font-weight: 600;
}

.chapter-title {
  margin: 0 0 14px;
  font-size: 20px;
}

/* 章节正文由 Markdown 渲染，排版规则见 assets/markdown.css */
.chapter-content {
  margin: 0 0 20px;
  color: var(--el-text-color-regular);
  font-size: 14px;
  line-height: 1.9;
}

.chapter-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 20px;
}

.primary-button,
.secondary-button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  border: 0;
  border-radius: 8px;
  font: inherit;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.primary-button {
  margin-left: auto;
  color: var(--el-color-white);
  background: var(--el-color-primary);
}

.primary-button:hover {
  background: var(--el-color-primary-light-3);
}

.primary-button:disabled {
  opacity: .6;
  cursor: not-allowed;
}

.secondary-button {
  color: var(--el-text-color-regular);
  background: var(--el-fill-color-light);
}

.secondary-button:hover {
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.secondary-button:disabled {
  opacity: .5;
  cursor: not-allowed;
}

.chapter-dots {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 18px;
  justify-content: center;
}

.practice-card {
  margin-top: 22px;
  padding: 22px 24px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 16px;
  background: var(--el-bg-color);
  box-shadow: var(--el-box-shadow-lighter);
}

.practice-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 14px;
}

.practice-head h3 {
  margin: 0;
  font-size: 17px;
}

.practice-head span {
  color: var(--el-text-color-placeholder);
  font-size: 12px;
}

.practice-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 14px 16px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 12px;
  background: var(--el-fill-color-lighter);
}

.practice-item + .practice-item {
  margin-top: 10px;
}

.practice-info strong {
  font-size: 14px;
}

.practice-info p {
  margin: 4px 0 2px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.practice-count {
  color: var(--el-text-color-placeholder);
  font-size: 12px;
}

.practice-button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex: none;
  padding: 9px 14px;
  border: 0;
  border-radius: 8px;
  color: var(--el-color-white);
  background: var(--el-color-primary);
  font: inherit;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.practice-button:hover {
  background: var(--el-color-primary-light-3);
}

@media (max-width: 600px) {
  .practice-item {
    align-items: flex-start;
    flex-direction: column;
  }
}

.dot {
  display: grid;
  width: 30px;
  height: 30px;
  place-items: center;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 50%;
  color: var(--el-text-color-secondary);
  background: var(--el-bg-color);
  font-size: 12px;
  cursor: pointer;
}

.dot.studied {
  color: var(--el-color-white);
  border-color: var(--el-color-success);
  background: var(--el-color-success);
}

.dot.active {
  border-color: var(--el-color-primary);
  box-shadow: 0 0 0 2px var(--el-color-primary-light-7);
}

.dot.active.studied {
  border-color: var(--el-color-success-light-3);
  box-shadow: 0 0 0 2px var(--el-color-success-light-7);
}

.celebrate-card {
  display: grid;
  justify-items: center;
  padding: 56px 24px;
  border: 1px solid var(--el-color-success-light-5);
  border-radius: 16px;
  background: linear-gradient(160deg, var(--el-color-success-light-9), var(--el-bg-color) 70%);
  text-align: center;
}

.celebrate-icon {
  color: var(--el-color-success);
  font-size: 52px;
}

.celebrate-card h3 {
  margin: 14px 0 8px;
  font-size: 20px;
}

.celebrate-card p {
  margin: 0 0 22px;
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.celebrate-actions {
  display: flex;
  gap: 12px;
}

.empty-panel {
  display: grid;
  justify-items: center;
  gap: 14px;
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

.empty-panel p {
  margin: 0;
  color: var(--el-text-color-placeholder);
  font-size: 13px;
}
</style>
