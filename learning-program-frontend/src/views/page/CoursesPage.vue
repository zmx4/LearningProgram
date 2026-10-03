<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { AlarmClock, Reading } from '@element-plus/icons-vue'
import { get } from '@/net'

interface CourseProgress {
  courseId: number
  title: string
  description: string | null
  icon: string | null
  chapterCount: number
  studiedCount: number
  status: 'not_started' | 'in_progress' | 'completed'
  totalSeconds: number
  lastStudiedAt: string | null
  completedAt: string | null
}

interface Overview {
  summary: { ongoingCount: number; completedCount: number; weeklySeconds: number }
  courses: CourseProgress[]
}

const router = useRouter()
const { t } = useI18n()

const loading = ref(true)
const loadFailed = ref(false)
const courses = ref<CourseProgress[]>([])

const ongoingCount = computed(() => courses.value.filter(course => course.status === 'in_progress').length)
const completedCount = computed(() => courses.value.filter(course => course.status === 'completed').length)

function loadCourses() {
  loading.value = true
  loadFailed.value = false
  get<Overview>('/api/courses/progress', data => {
    courses.value = data.courses
    loading.value = false
  }, () => {
    loading.value = false
    loadFailed.value = true
  })
}

function enterCourse(course: CourseProgress) {
  void router.push({ name: 'course-study', params: { id: course.courseId } })
}

function tagText(course: CourseProgress): string {
  if (course.status === 'completed') return t('courses.completedTag')
  if (course.status === 'in_progress') return t('courses.inProgressTag')
  return t('courses.notStartedTag')
}

function actionText(course: CourseProgress): string {
  if (course.status === 'completed') return t('courses.restudy')
  if (course.status === 'in_progress') return t('courses.continueLabel')
  return t('courses.start')
}

function progressPercent(course: CourseProgress): number {
  if (!course.chapterCount) return 0
  return Math.min(100, Math.round(course.studiedCount / course.chapterCount * 100))
}

function formatDate(value: string | null): string {
  if (!value) return ''
  return new Date(value).toLocaleDateString('zh-CN', { month: 'long', day: 'numeric' })
}

function formatDuration(seconds: number): string {
  if (seconds <= 0) return ''
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  if (hours > 0) return `${hours}${t('home.hours')}${minutes > 0 ? minutes + t('home.minutes') : ''}`
  if (minutes > 0) return `${minutes}${t('home.minutes')}`
  return t('home.lessThanMinute')
}

function metaText(course: CourseProgress): string {
  const parts: string[] = []
  if (course.status === 'completed' && course.completedAt) {
    parts.push(t('courses.completedAt', { date: formatDate(course.completedAt) }))
  } else if (course.lastStudiedAt) {
    parts.push(t('courses.lastStudiedAt', { date: formatDate(course.lastStudiedAt) }))
  }
  const duration = formatDuration(course.totalSeconds)
  if (duration) parts.push(t('courses.totalSeconds', { duration }))
  return parts.join(' · ')
}

onMounted(loadCourses)
</script>

<template>
  <main class="courses-page">
    <section class="courses-heading">
      <p class="eyebrow">MY COURSES</p>
      <h2>{{ t('courses.title') }}</h2>
      <p>{{ t('courses.description') }}</p>
      <div class="courses-brief">
        <span>{{ t('home.ongoingCourses') }} <strong>{{ ongoingCount }}</strong></span>
        <span>{{ t('home.completedCourses') }} <strong>{{ completedCount }}</strong></span>
      </div>
    </section>

    <section v-if="loading" class="course-grid" aria-busy="true">
      <div v-for="index in 3" :key="index" class="course-card skeleton-card">
        <div class="skeleton line short"></div>
        <div class="skeleton line long"></div>
        <div class="skeleton line long"></div>
        <div class="skeleton line medium"></div>
      </div>
    </section>

    <section v-else-if="loadFailed" class="empty-panel">
      <h3>{{ t('courses.loadFailed') }}</h3>
      <button class="empty-action" type="button" @click="loadCourses">{{ t('courses.retry') }}</button>
    </section>

    <section v-else-if="courses.length" class="course-grid">
      <article
        v-for="course in courses"
        :key="course.courseId"
        class="course-card"
        :class="{ completed: course.status === 'completed' }"
      >
        <div class="course-head">
          <span class="course-icon">{{ course.icon || '📘' }}</span>
          <span class="course-tag" :class="course.status">
            <el-icon v-if="course.status === 'completed'"><Reading /></el-icon>
            {{ tagText(course) }}
          </span>
        </div>
        <strong class="course-title">{{ course.title }}</strong>
        <p class="course-desc">{{ course.description || t('courses.noDescription') }}</p>
        <div class="course-progress">
          <div class="course-progress-text">
            <span>{{ t('courses.studiedLabel', { studied: course.studiedCount, total: course.chapterCount }) }}</span>
            <span>{{ progressPercent(course) }}%</span>
          </div>
          <div class="course-bar">
            <div
              class="course-bar-fill"
              :class="{ done: course.status === 'completed' }"
              :style="{ width: `${progressPercent(course)}%` }"
            ></div>
          </div>
        </div>
        <div class="course-foot">
          <span class="course-meta">
            <template v-if="metaText(course)">
              <el-icon><AlarmClock /></el-icon>
              {{ metaText(course) }}
            </template>
            <template v-else>{{ t('courses.chapterCount', { count: course.chapterCount }) }}</template>
          </span>
          <button class="course-action" type="button" @click="enterCourse(course)">
            {{ actionText(course) }}
          </button>
        </div>
      </article>
    </section>

    <section v-else class="empty-panel">
      <div class="empty-icon"><el-icon><Reading /></el-icon></div>
      <h3>{{ t('courses.emptyTitle') }}</h3>
      <p>{{ t('courses.emptyDescription') }}</p>
      <button class="empty-action" type="button" @click="router.push({ name: 'tests' })">
        {{ t('courses.goTests') }}
      </button>
    </section>
  </main>
</template>

<style scoped>
.courses-page {
  max-width: 1180px;
  margin: 0 auto;
  padding: 42px 5% 60px;
}

.courses-heading h2 {
  margin: 0 0 8px;
  font-size: 26px;
}

.courses-heading > p:nth-of-type(1) {
  margin: 0 0 16px;
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.courses-brief {
  display: flex;
  gap: 22px;
  margin-bottom: 26px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.courses-brief strong {
  margin-left: 4px;
  color: var(--el-color-primary);
  font-size: 16px;
}

.course-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 18px;
}

.course-card {
  display: grid;
  gap: 10px;
  align-content: start;
  padding: 24px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 16px;
  background: var(--el-bg-color);
  box-shadow: var(--el-box-shadow-lighter);
  transition: border-color .2s ease, box-shadow .2s ease, transform .2s ease;
}

.course-card:hover {
  border-color: var(--el-color-primary-light-5);
  box-shadow: var(--el-box-shadow-light);
  transform: translateY(-2px);
}

/* 已完成的课程换成绿色完成态 */
.course-card.completed {
  border-color: var(--el-color-success-light-5);
  background: linear-gradient(160deg, var(--el-color-success-light-9), var(--el-bg-color) 55%);
}

.course-card.completed:hover {
  border-color: var(--el-color-success-light-3);
}

.course-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.course-icon {
  display: grid;
  width: 40px;
  height: 40px;
  place-items: center;
  border-radius: 12px;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  font-size: 20px;
}

.course-card.completed .course-icon {
  color: var(--el-color-success);
  background: var(--el-color-success-light-8);
}

.course-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
}

.course-tag.not_started {
  color: var(--el-text-color-secondary);
  background: var(--el-fill-color);
}

.course-tag.in_progress {
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.course-tag.completed {
  color: var(--el-color-success);
  background: var(--el-color-success-light-8);
}

.course-title {
  font-size: 18px;
}

.course-desc {
  display: -webkit-box;
  margin: 0;
  overflow: hidden;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 1.6;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  min-height: 2.6em;
}

.course-progress-text {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.course-bar {
  height: 8px;
  border-radius: 6px;
  background: var(--el-fill-color-light);
  overflow: hidden;
}

.course-bar-fill {
  height: 100%;
  border-radius: 6px;
  background: var(--el-color-primary);
  transition: width .3s ease;
}

.course-bar-fill.done {
  background: var(--el-color-success);
}

.course-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 4px;
}

.course-meta {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  min-width: 0;
  overflow: hidden;
  color: var(--el-text-color-placeholder);
  font-size: 12px;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.course-action {
  flex-shrink: 0;
  padding: 8px 16px;
  border: 0;
  border-radius: 8px;
  color: var(--el-color-white);
  background: var(--el-color-primary);
  font: inherit;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.course-action:hover {
  background: var(--el-color-primary-light-3);
}

/* 完成课程的按钮弱化为次要样式，避免与进行中的课程抢焦点 */
.course-card.completed .course-action {
  color: var(--el-color-success);
  background: var(--el-color-success-light-8);
}

.course-card.completed .course-action:hover {
  background: var(--el-color-success-light-5);
}

.empty-panel {
  display: grid;
  justify-items: center;
  padding: 48px 20px;
  border: 1px dashed var(--el-border-color);
  border-radius: 14px;
  background: var(--el-fill-color-lighter);
  text-align: center;
}

.empty-icon {
  display: grid;
  width: 50px;
  height: 50px;
  place-items: center;
  margin-bottom: 16px;
  border-radius: 14px;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  font-size: 24px;
}

.empty-panel h3 {
  margin-bottom: 8px;
  font-size: 16px;
}

.empty-panel p {
  margin: 0 0 16px;
  color: var(--el-text-color-placeholder);
  font-size: 13px;
}

.empty-action {
  padding: 9px 18px;
  border: 0;
  border-radius: 8px;
  color: var(--el-color-white);
  background: var(--el-color-primary);
  font: inherit;
  font-size: 13px;
  cursor: pointer;
}

.skeleton-card {
  min-height: 190px;
}

.skeleton {
  border-radius: 6px;
  background: var(--el-fill-color-light);
  animation: pulse 1.4s ease-in-out infinite;
}

.skeleton.short { width: 40%; height: 34px; }
.skeleton.long { width: 92%; height: 14px; }
.skeleton.medium { width: 60%; height: 14px; }

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: .45; }
}

@media (max-width: 760px) {
  .course-grid {
    grid-template-columns: 1fr;
  }
}
</style>
