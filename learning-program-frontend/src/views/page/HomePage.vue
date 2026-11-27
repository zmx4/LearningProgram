<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ArrowRight, Reading, Trophy, User } from '@element-plus/icons-vue'
import CheckInCard from '@/components/CheckInCard.vue'
import GroupActions from '@/components/group/GroupActions.vue'
import GroupCard from '@/components/group/GroupCard.vue'
import { get, publicGet, unauthorized } from '@/net'
import type { GroupSummary, GroupTask } from '@/types/group'

interface GuestCourse {
  courseId: number
  title: string
  description: string | null
  chapterCount: number
}

interface GroupMetrics {
  ongoingTasks: number
  completionRate: number | null
  overdueTasks: number
}

interface TodoItem {
  key: string
  groupId: number
  groupName: string
  taskId: number
  taskTitle: string
  itemTitle: string
}

const router = useRouter()
const { t } = useI18n()
const isLoggedIn = !unauthorized()

const groups = ref<GroupSummary[]>([])
const tasksByGroup = ref<Record<number, GroupTask[]>>({})
const guestCourses = ref<GuestCourse[]>([])
const loadingGroups = ref(false)
const tasksLoading = ref(false)
const loadFailed = ref(false)
let disposed = false

function loadGuestCourses() {
  publicGet<{ courses?: GuestCourse[] }>('/api/courses/progress', data => {
    if (disposed) return
    guestCourses.value = data.courses ?? []
  }, () => undefined)
}

function loadGroupTasks(groupId: number, onFinished?: () => void) {
  get<GroupTask[]>(`/api/groups/${groupId}/tasks`, data => {
    if (disposed) return
    tasksByGroup.value = { ...tasksByGroup.value, [groupId]: data }
    onFinished?.()
  }, () => {
    if (!disposed) onFinished?.()
  })
}

function loadGroups() {
  loadingGroups.value = true
  loadFailed.value = false
  get<GroupSummary[]>('/api/groups', data => {
    if (disposed) return
    groups.value = data
    loadingGroups.value = false
    tasksLoading.value = data.length > 0
    if (!data.length) return
    let remaining = data.length
    data.forEach(group => {
      loadGroupTasks(group.id, () => {
        remaining -= 1
        if (remaining === 0) tasksLoading.value = false
      })
    })
  }, () => {
    if (disposed) return
    loadingGroups.value = false
    loadFailed.value = true
  })
}

function onGroupChanged(group: GroupSummary) {
  groups.value = [group, ...groups.value.filter(item => item.id !== group.id)]
  tasksLoading.value = true
  loadGroupTasks(group.id, () => {
    tasksLoading.value = false
  })
}

const metricsByGroup = computed<Record<number, GroupMetrics>>(() => {
  const result: Record<number, GroupMetrics> = {}
  const now = Date.now()
  for (const group of groups.value) {
    const tasks = tasksByGroup.value[group.id] ?? []
    let ongoingTasks = 0
    let completedItems = 0
    let totalItems = 0
    let overdueTasks = 0
    for (const task of tasks) {
      if (task.totalItems > 0 && task.myCompleted < task.totalItems) ongoingTasks += 1
      completedItems += task.myCompleted
      totalItems += task.totalItems
      if (task.dueAt && new Date(task.dueAt).getTime() < now && task.myCompleted < task.totalItems) {
        overdueTasks += 1
      }
    }
    result[group.id] = {
      ongoingTasks,
      completionRate: totalItems > 0 ? Math.round(completedItems * 100 / totalItems) : null,
      overdueTasks,
    }
  }
  return result
})

const todos = computed<TodoItem[]>(() => {
  const items: TodoItem[] = []
  for (const group of groups.value) {
    for (const task of tasksByGroup.value[group.id] ?? []) {
      for (const item of task.items) {
        if (item.myCompleted) continue
        items.push({
          key: `${group.id}-${task.id}-${item.id}`,
          groupId: group.id,
          groupName: group.name,
          taskId: task.id,
          taskTitle: task.title,
          itemTitle: item.title,
        })
      }
    }
  }
  return items.slice(0, 6)
})

const totalOngoing = computed(() => Object.values(metricsByGroup.value)
    .reduce((sum, metrics) => sum + metrics.ongoingTasks, 0))

function openGroup(group: GroupSummary) {
  void router.push({ name: 'group-detail', params: { id: group.id } })
}

function openLeaderboard(group: GroupSummary) {
  void router.push({ name: 'leaderboard', query: { groupId: group.id } })
}

function openTodo(todo: TodoItem) {
  void router.push({ name: 'group-detail', params: { id: todo.groupId } })
}

function goGroups() {
  void router.push({ name: 'groups' })
}

function goCourses() {
  void router.push({ name: 'courses' })
}

onMounted(() => {
  if (isLoggedIn) {
    loadGroups()
  } else {
    loadGuestCourses()
  }
})

onUnmounted(() => {
  disposed = true
})
</script>

<template>
  <main class="home-page">
    <section class="hero-card">
      <div class="hero-copy">
        <p class="eyebrow">MY STUDY GROUPS</p>
        <h1>{{ isLoggedIn ? t('home.groupHeroTitle') : t('home.guestHeroTitle') }}</h1>
        <p>{{ isLoggedIn ? t('home.groupHeroDescription') : t('home.guestHeroDescription') }}</p>
        <div class="hero-actions">
          <button v-if="isLoggedIn" class="primary-button" type="button" @click="goGroups">
            {{ t('home.manageGroups') }}
            <el-icon><ArrowRight /></el-icon>
          </button>
          <button v-else class="primary-button" type="button" @click="router.push({ name: 'welcome-login' })">
            {{ t('home.guestLoginButton') }}
          </button>
          <button class="ghost-button" type="button" @click="goCourses">{{ t('home.start') }}</button>
        </div>
      </div>
      <div v-if="isLoggedIn" class="hero-summary">
        <div>
          <span>{{ t('home.groupCount') }}</span>
          <strong>{{ groups.length }}</strong>
        </div>
        <div>
          <span>{{ t('home.ongoingTaskCount') }}</span>
          <strong>{{ totalOngoing }}</strong>
        </div>
        <div>
          <span>{{ t('home.todoCount') }}</span>
          <strong>{{ todos.length }}</strong>
        </div>
      </div>
      <div v-else class="hero-decoration" aria-hidden="true">
        <span class="hero-orbit hero-orbit-one"></span>
        <span class="hero-orbit hero-orbit-two"></span>
        <el-icon><User /></el-icon>
      </div>
    </section>

    <template v-if="isLoggedIn">
      <section class="home-section">
        <div class="section-heading">
          <div>
            <p class="eyebrow">{{ t('home.myGroups') }}</p>
            <h2>{{ t('groups.myGroups') }}</h2>
          </div>
          <button class="text-button" type="button" @click="goGroups">{{ t('home.viewAllGroups') }}</button>
        </div>

        <section v-if="loadingGroups" class="group-grid" aria-busy="true">
          <div v-for="index in 3" :key="index" class="group-card skeleton-card">
            <div class="skeleton line short"></div>
            <div class="skeleton line long"></div>
          </div>
        </section>

        <section v-else-if="loadFailed" class="empty-panel">
          <h3>{{ t('groups.loadFailed') }}</h3>
          <button class="ghost-button" type="button" @click="loadGroups">{{ t('groups.retry') }}</button>
        </section>

        <section v-else-if="groups.length" class="group-grid">
          <GroupCard
              v-for="group in groups"
              :key="group.id"
              :group="group"
              :metrics="metricsByGroup[group.id]"
              @open="openGroup"
              @leaderboard="openLeaderboard"
          />
        </section>

        <section v-else class="empty-panel group-empty">
          <span class="empty-icon"><el-icon><User /></el-icon></span>
          <h3>{{ t('home.noGroupsTitle') }}</h3>
          <p>{{ t('home.noGroupsDescription') }}</p>
          <GroupActions @changed="onGroupChanged" />
        </section>
      </section>

      <section class="home-section">
        <div class="section-heading">
          <div>
            <p class="eyebrow">{{ t('home.today') }}</p>
            <h2>{{ t('home.todayTodos') }}</h2>
          </div>
          <span class="section-count">{{ t('home.todoCountValue', { count: todos.length }) }}</span>
        </div>

        <section v-if="tasksLoading" class="todo-list" aria-busy="true">
          <div v-for="index in 3" :key="index" class="todo-card skeleton-card">
            <div class="skeleton line short"></div>
            <div class="skeleton line long"></div>
          </div>
        </section>

        <section v-else-if="todos.length" class="todo-list">
          <button v-for="todo in todos" :key="todo.key" class="todo-card" type="button" @click="openTodo(todo)">
            <span class="todo-icon"><el-icon><Reading /></el-icon></span>
            <span class="todo-body">
              <strong>{{ todo.itemTitle }}</strong>
              <small>{{ todo.groupName }} · {{ todo.taskTitle }}</small>
            </span>
            <el-icon class="todo-arrow"><ArrowRight /></el-icon>
          </button>
        </section>

        <section v-else class="empty-panel compact-empty">
          <h3>{{ t('home.noTodosTitle') }}</h3>
          <p>{{ t('home.noTodosDescription') }}</p>
        </section>
      </section>

      <CheckInCard />
    </template>

    <template v-else>
      <section class="home-section">
        <div class="section-heading">
          <div>
            <p class="eyebrow">{{ t('home.howItWorks') }}</p>
            <h2>{{ t('home.howItWorksTitle') }}</h2>
          </div>
        </div>
        <div class="step-grid">
          <article v-for="(step, index) in [t('home.stepOne'), t('home.stepTwo'), t('home.stepThree')]" :key="step" class="step-card">
            <span>{{ String(index + 1).padStart(2, '0') }}</span>
            <h3>{{ step }}</h3>
            <p>{{ t(`home.stepDescription${index + 1}`) }}</p>
          </article>
        </div>
      </section>

      <section class="home-section">
        <div class="section-heading">
          <div>
            <p class="eyebrow">{{ t('home.explore') }}</p>
            <h2>{{ t('home.exploreTitle') }}</h2>
          </div>
        </div>
        <div v-if="guestCourses.length" class="guest-course-grid">
          <article v-for="course in guestCourses" :key="course.courseId" class="guest-course-card">
            <div class="guest-course-head">
              <strong>{{ course.title }}</strong>
              <span class="guest-course-tag">{{ t('courses.notStartedTag') }}</span>
            </div>
            <p class="guest-course-desc">{{ course.description || t('courses.noDescription') }}</p>
            <span class="guest-course-meta">{{ t('courses.chapterCount', { count: course.chapterCount }) }}</span>
          </article>
        </div>
        <section v-else class="empty-panel compact-empty">
          <h3>{{ t('home.noPublicCourses') }}</h3>
        </section>
      </section>

      <section class="login-guide-card">
        <div>
          <p class="eyebrow">{{ t('home.guestLabel') }}</p>
          <h2>{{ t('home.guestTitle') }}</h2>
          <p>{{ t('home.guestDescription') }}</p>
        </div>
        <button class="primary-button" type="button" @click="router.push({ name: 'welcome-login' })">
          {{ t('home.guestLoginButton') }}
        </button>
      </section>
    </template>
  </main>
</template>

<style scoped>
.home-page {
  display: grid;
  gap: 42px;
  max-width: 1180px;
  margin: 0 auto;
  padding: 42px 5% 60px;
}

.hero-card {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 32px;
  overflow: hidden;
  padding: 34px;
  border: 1px solid var(--el-color-primary-light-7);
  border-radius: 18px;
  background: linear-gradient(120deg, var(--el-color-primary-light-9), var(--el-bg-color));
  box-shadow: var(--el-box-shadow-light);
}

.hero-copy {
  position: relative;
  z-index: 1;
  max-width: 680px;
}

.eyebrow {
  margin: 0 0 6px;
  color: var(--el-text-color-placeholder);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: .13em;
}

.hero-copy h1 {
  margin: 0 0 10px;
  color: var(--el-text-color-primary);
  font-size: 28px;
  line-height: 1.25;
}

.hero-copy > p:not(.eyebrow) {
  margin: 0;
  color: var(--el-text-color-secondary);
  font-size: 14px;
  line-height: 1.7;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 22px;
}

.primary-button,
.ghost-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px 16px;
  border-radius: 8px;
  font: inherit;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.primary-button {
  border: 1px solid var(--el-color-primary);
  color: var(--el-color-white);
  background: var(--el-color-primary);
}

.primary-button:hover {
  border-color: var(--el-color-primary-light-3);
  background: var(--el-color-primary-light-3);
}

.ghost-button {
  border: 1px solid var(--el-border-color);
  color: var(--el-text-color-regular);
  background: var(--el-bg-color);
}

.ghost-button:hover {
  border-color: var(--el-color-primary-light-5);
  color: var(--el-color-primary);
}

.hero-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(84px, 1fr));
  gap: 10px;
  min-width: 300px;
}

.hero-summary > div {
  display: grid;
  gap: 4px;
  padding: 14px 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 12px;
  background: color-mix(in srgb, var(--el-bg-color) 82%, transparent);
  text-align: center;
}

.hero-summary span {
  color: var(--el-text-color-placeholder);
  font-size: 11px;
}

.hero-summary strong {
  color: var(--el-color-primary);
  font-size: 24px;
  line-height: 1.2;
}

.hero-decoration {
  position: relative;
  display: grid;
  width: 170px;
  height: 170px;
  flex: 0 0 170px;
  place-items: center;
  color: var(--el-color-primary);
  font-size: 54px;
}

.hero-orbit {
  position: absolute;
  border: 1px solid var(--el-color-primary-light-5);
  border-radius: 50%;
}

.hero-orbit-one { inset: 8px; }
.hero-orbit-two { inset: 34px; background: var(--el-color-primary-light-9); }

.home-section {
  display: grid;
  gap: 16px;
}

.section-heading {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 16px;
}

.section-heading h2 {
  margin: 0;
  font-size: 20px;
}

.text-button {
  padding: 4px 0;
  border: 0;
  color: var(--el-color-primary);
  background: transparent;
  font: inherit;
  font-size: 13px;
  cursor: pointer;
}

.text-button:hover {
  color: var(--el-color-primary-dark-2);
}

.section-count {
  color: var(--el-text-color-placeholder);
  font-size: 12px;
}

.group-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 14px;
}

.group-empty {
  justify-items: stretch;
  text-align: left;
}

.group-empty > h3,
.group-empty > p {
  text-align: center;
}

.group-empty .group-actions {
  margin-top: 8px;
}

.todo-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.todo-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 14px;
  color: inherit;
  background: var(--el-bg-color);
  font: inherit;
  text-align: left;
  cursor: pointer;
  transition: border-color .2s, box-shadow .2s;
}

.todo-card:hover {
  border-color: var(--el-color-primary-light-5);
  box-shadow: var(--el-box-shadow-lighter);
}

.todo-icon {
  display: grid;
  width: 38px;
  height: 38px;
  flex: 0 0 38px;
  place-items: center;
  border-radius: 10px;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  font-size: 18px;
}

.todo-body {
  display: grid;
  min-width: 0;
  flex: 1;
  gap: 3px;
}

.todo-body strong {
  overflow: hidden;
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.todo-body small {
  overflow: hidden;
  color: var(--el-text-color-placeholder);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.todo-arrow {
  color: var(--el-text-color-placeholder);
}

.empty-panel {
  display: grid;
  justify-items: center;
  gap: 10px;
  padding: 42px 20px;
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

.compact-empty {
  padding: 28px 20px;
}

.empty-icon {
  color: var(--el-text-color-placeholder);
  font-size: 40px;
}

.step-grid,
.guest-course-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.step-card,
.guest-course-card {
  padding: 20px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 14px;
  background: var(--el-bg-color);
}

.step-card > span {
  color: var(--el-color-primary);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: .08em;
}

.step-card h3 {
  margin: 10px 0 6px;
  font-size: 16px;
}

.step-card p,
.guest-course-desc {
  margin: 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.guest-course-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
}

.guest-course-tag {
  flex: 0 0 auto;
  padding: 2px 8px;
  border-radius: 999px;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  font-size: 11px;
}

.guest-course-meta {
  display: inline-block;
  margin-top: 14px;
  color: var(--el-text-color-placeholder);
  font-size: 12px;
}

.login-guide-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 26px;
  border: 1px solid var(--el-color-primary-light-7);
  border-radius: 16px;
  background: var(--el-color-primary-light-9);
}

.login-guide-card h2 {
  margin: 0 0 6px;
  font-size: 20px;
}

.login-guide-card p:not(.eyebrow) {
  margin: 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.skeleton-card {
  display: grid;
  gap: 10px;
  padding: 18px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 14px;
  background: var(--el-bg-color);
}

.skeleton.line {
  height: 14px;
  border-radius: 7px;
}

.skeleton.line.short { width: 30%; }
.skeleton.line.long { width: 85%; }

.skeleton {
  background: linear-gradient(90deg, var(--el-fill-color-light) 25%, var(--el-fill-color-lighter) 50%, var(--el-fill-color-light) 75%);
  background-size: 200% 100%;
  animation: skeleton-wave 1.4s ease infinite;
}

@keyframes skeleton-wave {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

@media (max-width: 900px) {
  .hero-card {
    align-items: flex-start;
    flex-direction: column;
  }

  .hero-summary {
    width: 100%;
    min-width: 0;
  }

  .todo-list,
  .step-grid,
  .guest-course-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 700px) {
  .home-page {
    gap: 30px;
    padding: 28px 4% 48px;
  }

  .hero-card {
    padding: 24px;
  }

  .hero-copy h1 {
    font-size: 24px;
  }

  .hero-decoration {
    display: none;
  }

  .login-guide-card {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
