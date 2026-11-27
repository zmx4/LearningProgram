<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ArrowRight, Trophy, User, UserFilled } from '@element-plus/icons-vue'
import CheckInCard from '@/components/CheckInCard.vue'
import GroupActions from '@/components/group/GroupActions.vue'
import { get, publicGet, unauthorized } from '@/net'
import type { GroupSummary } from '@/types/group'

interface GuestCourse {
  courseId: number
  title: string
  description: string | null
  chapterCount: number
}

interface TodoItem {
  key: string
  groupId: number
  groupName: string
  taskId: number
  taskTitle: string
  itemTitle: string
  dueAt: string | null
}

interface GroupOverview {
  groups: GroupSummary[]
  todos: TodoItem[]
  totalOngoing: number
  totalOverdue: number
  totalCompleted: number
}

type TodoTone = 'overdue' | 'today' | 'upcoming' | 'nodue'

interface TodoView extends TodoItem {
  tone: TodoTone
  dateLabel: string
  timeLabel: string
  relative: string
}

const router = useRouter()
const { t } = useI18n()
const isLoggedIn = !unauthorized()

const groups = ref<GroupSummary[]>([])
const todos = ref<TodoItem[]>([])
const todoTotal = ref(0)
const totalOngoing = ref(0)
const totalOverdue = ref(0)
const totalCompleted = ref(0)
const guestCourses = ref<GuestCourse[]>([])
const loadingGroups = ref(false)
const loadFailed = ref(false)
const todayLabel = ref('')
let disposed = false

const DAY_MS = 86400000

function formatMonthDay(value: Date) {
  return new Intl.DateTimeFormat('zh-CN', { month: '2-digit', day: '2-digit' })
      .format(value)
      .replace('/', '.')
}

function formatClock(value: Date) {
  return new Intl.DateTimeFormat('zh-CN', { hour: '2-digit', minute: '2-digit', hour12: false }).format(value)
}

function startOfDay(value: Date) {
  return new Date(value.getFullYear(), value.getMonth(), value.getDate())
}

function formatGroupDate(value: string) {
  return value ? value.slice(0, 10).replaceAll('-', '.') : ''
}

function loadGuestCourses() {
  publicGet<{ courses?: GuestCourse[] }>('/api/courses/progress', data => {
    if (disposed) return
    guestCourses.value = data.courses ?? []
  }, () => undefined)
}

function applyOverview(data: GroupOverview) {
  groups.value = data.groups
  todoTotal.value = data.todos.length
  todos.value = data.todos.slice(0, 6)
  totalOngoing.value = data.totalOngoing
  totalOverdue.value = data.totalOverdue
  totalCompleted.value = data.totalCompleted
}

function loadGroups() {
  loadingGroups.value = true
  loadFailed.value = false
  get<GroupOverview>('/api/groups/overview', data => {
    if (disposed) return
    applyOverview(data)
    loadingGroups.value = false
  }, () => {
    if (disposed) return
    loadingGroups.value = false
    loadFailed.value = true
  })
}

function onGroupChanged(group: GroupSummary) {
  groups.value = [group, ...groups.value.filter(item => item.id !== group.id)]
  loadGroups()
}

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

const taskTotal = computed(() => totalCompleted.value + totalOngoing.value)
const completionPercent = computed(() => (
  taskTotal.value ? Math.round(totalCompleted.value * 100 / taskTotal.value) : 0
))
const completionAria = computed(() => t('home.completionSummary', {
  completed: totalCompleted.value,
  total: taskTotal.value,
}))
const ringStyle = computed(() => {
  const total = taskTotal.value
  if (!total) {
    return { background: 'conic-gradient(rgba(240, 247, 255, .16) 0% 100%)' }
  }
  const done = totalCompleted.value / total * 100
  const overdue = Math.min(totalOverdue.value, totalOngoing.value) / total * 100
  const doneEdge = done.toFixed(2)
  const ongoingEdge = Math.min(100, done + Math.max(0, 100 - done - overdue)).toFixed(2)
  return {
    background: `conic-gradient(var(--el-color-primary) 0% ${doneEdge}%, var(--deck-accent) ${doneEdge}% ${ongoingEdge}%, var(--el-color-danger) ${ongoingEdge}% 100%)`,
  }
})

const groupLanes = computed(() => groups.value.map((group, index) => {
  const completed = group.completedTasks ?? 0
  const ongoing = group.ongoingTasks ?? 0
  const total = completed + ongoing
  return {
    group,
    index: String(index + 1).padStart(2, '0'),
    percent: total ? Math.round(completed * 100 / total) : 0,
  }
}))

const todoViews = computed<TodoView[]>(() => {
  const now = new Date()
  const today = startOfDay(now)
  return todos.value.map(todo => {
    if (!todo.dueAt) {
      return {
        ...todo,
        tone: 'nodue',
        dateLabel: '--',
        timeLabel: '',
        relative: t('home.todoNoDue'),
      }
    }
    const due = new Date(todo.dueAt)
    const diffDays = Math.round((startOfDay(due).getTime() - today.getTime()) / DAY_MS)
    let tone: TodoTone
    if (due.getTime() < now.getTime()) {
      tone = 'overdue'
    } else if (diffDays === 0) {
      tone = 'today'
    } else {
      tone = 'upcoming'
    }
    let relative = t('home.todoUpcoming')
    if (tone === 'overdue') {
      relative = diffDays < 0
        ? t('home.todoOverdueDays', { days: Math.abs(diffDays) })
        : t('home.todoOverdue')
    } else if (tone === 'today') {
      relative = t('home.todoToday')
    }
    return {
      ...todo,
      tone,
      dateLabel: formatMonthDay(due),
      timeLabel: formatClock(due),
      relative,
    }
  })
})

const todoBuckets = computed(() => {
  const buckets: Array<{ tone: TodoTone; label: string; items: TodoView[] }> = [
    { tone: 'overdue', label: t('home.todoOverdue'), items: [] },
    { tone: 'today', label: t('home.todoToday'), items: [] },
    { tone: 'upcoming', label: t('home.todoUpcoming'), items: [] },
    { tone: 'nodue', label: t('home.todoNoDue'), items: [] },
  ]
  for (const item of todoViews.value) {
    buckets.find(bucket => bucket.tone === item.tone)?.items.push(item)
  }
  return buckets.filter(bucket => bucket.items.length > 0)
})

onMounted(() => {
  const now = new Date()
  const monthDay = new Intl.DateTimeFormat('zh-CN', { month: 'long', day: 'numeric' }).format(now)
  const weekday = new Intl.DateTimeFormat('zh-CN', { weekday: 'long' }).format(now)
  todayLabel.value = `${monthDay} · ${weekday}`
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
    <section class="mission-deck" :class="{ 'is-guest': !isLoggedIn }">
      <span class="deck-grid" aria-hidden="true"></span>
      <span class="deck-glow" aria-hidden="true"></span>

      <div class="deck-copy">
        <p class="deck-kicker">
          <span class="kicker-pulse" aria-hidden="true"></span>
          {{ todayLabel }}
          <span class="kicker-separator" aria-hidden="true">/</span>
          {{ isLoggedIn ? t('home.myGroups') : t('home.guestLabel') }}
        </p>
        <h1 v-if="isLoggedIn">
          <span>{{ t('home.groupHeroTitleLead') }}</span>
          <span class="hero-accent">{{ t('home.groupHeroTitleAccent') }}</span>
        </h1>
        <h1 v-else>{{ t('home.guestHeroTitle') }}</h1>
        <p class="deck-description">
          {{ isLoggedIn ? t('home.groupHeroDescription') : t('home.guestHeroDescription') }}
        </p>
        <div class="deck-actions">
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

      <aside v-if="isLoggedIn" class="deck-console" :aria-label="t('home.deckTitle')">
        <div class="console-head">
          <span>{{ t('home.deckTitle') }}</span>
          <span class="console-total">{{ t('home.totalTasks', { total: taskTotal }) }}</span>
        </div>

        <div class="console-main">
          <div class="completion-ring" role="img" :aria-label="completionAria">
            <span class="ring-track" :style="ringStyle" aria-hidden="true"></span>
            <span class="ring-core" aria-hidden="true">
              <strong>{{ completionPercent }}<small>%</small></strong>
              <span>{{ t('home.taskCompletion') }}</span>
            </span>
          </div>

          <div class="console-legend">
            <div class="legend-row">
              <span class="legend-name"><i class="legend-dot is-done"></i>{{ t('home.completedTaskCount') }}</span>
              <strong>{{ totalCompleted }}</strong>
            </div>
            <div class="legend-row">
              <span class="legend-name"><i class="legend-dot is-ongoing"></i>{{ t('home.ongoingTaskCount') }}</span>
              <strong>{{ totalOngoing }}</strong>
            </div>
            <div class="legend-row" :class="{ 'is-alert': totalOverdue > 0 }">
              <span class="legend-name"><i class="legend-dot is-overdue"></i>{{ t('home.overdueSubset') }}</span>
              <strong>{{ totalOverdue }}</strong>
            </div>
          </div>
        </div>

        <div class="console-foot">
          <div>
            <span>{{ t('home.groupCount') }}</span>
            <strong>{{ groups.length }}</strong>
          </div>
          <div>
            <span>{{ t('home.todoCount') }}</span>
            <strong>{{ todoTotal }}</strong>
          </div>
        </div>
      </aside>

      <div v-else class="deck-art" aria-hidden="true">
        <span class="art-ring art-ring-one"></span>
        <span class="art-ring art-ring-two"></span>
        <span class="art-ring art-ring-three"></span>
        <span class="art-core">学</span>
        <span class="art-node art-node-one">01</span>
        <span class="art-node art-node-two">02</span>
        <span class="art-node art-node-three">03</span>
      </div>
    </section>

    <template v-if="isLoggedIn">
      <div class="home-dashboard">
        <div class="home-column">
          <section class="home-section" aria-labelledby="home-groups-heading">
            <header class="section-head">
              <div class="section-label">
                <span class="section-index">01</span>
                <span>{{ t('home.myGroups') }}</span>
                <span v-if="groups.length" class="section-total">{{ t('groups.total', { count: groups.length }) }}</span>
              </div>
              <div class="section-title-row">
                <h2 id="home-groups-heading">{{ t('groups.myGroups') }}</h2>
                <button class="text-button" type="button" @click="goGroups">
                  {{ t('home.viewAllGroups') }}
                  <el-icon><ArrowRight /></el-icon>
                </button>
              </div>
            </header>

            <div v-if="loadingGroups" class="group-stack" aria-busy="true">
              <div v-for="index in 2" :key="index" class="lane-skeleton">
                <span class="skeleton skeleton-mark"></span>
                <span class="skeleton skeleton-title"></span>
                <span class="skeleton skeleton-track"></span>
              </div>
            </div>

            <section v-else-if="loadFailed" class="empty-panel">
              <h3>{{ t('groups.loadFailed') }}</h3>
              <button class="ghost-button" type="button" @click="loadGroups">{{ t('groups.retry') }}</button>
            </section>

            <section v-else-if="groups.length" class="group-stack" :aria-label="t('groups.myGroups')">
              <article v-for="lane in groupLanes" :key="lane.group.id" class="group-lane">
                <button class="lane-main" type="button" @click="openGroup(lane.group)">
                  <span class="lane-number" aria-hidden="true">{{ lane.index }}</span>
                  <span class="lane-identity">
                    <span class="lane-title">
                      <strong>{{ lane.group.name }}</strong>
                      <span class="role-badge" :class="{ owner: lane.group.role === 'owner' }">
                        {{ lane.group.role === 'owner' ? t('groups.roleOwner') : t('groups.roleMember') }}
                      </span>
                    </span>
                    <span class="lane-meta">
                      <span>
                        <el-icon><UserFilled /></el-icon>
                        {{ t('groups.ownerLabel', { name: lane.group.ownerName || t('groups.unknownUser') }) }}
                      </span>
                      <span>
                        <el-icon><User /></el-icon>
                        {{ t('groups.memberCount', { count: lane.group.memberCount }) }}
                      </span>
                      <span class="lane-created">{{ formatGroupDate(lane.group.createdAt) }}</span>
                    </span>
                  </span>

                  <span class="lane-progress">
                    <span class="lane-progress-head">
                      <small>{{ t('home.groupProgress') }}</small>
                      <strong>{{ lane.percent }}<em>%</em></strong>
                    </span>
                    <span class="lane-track" aria-hidden="true">
                      <span class="lane-fill" :style="{ width: `${lane.percent}%` }"></span>
                    </span>
                  </span>

                  <span class="lane-metrics">
                    <span class="lane-metric">
                      <small>{{ t('groups.ongoingTasks') }}</small>
                      <strong>{{ lane.group.ongoingTasks ?? 0 }}</strong>
                    </span>
                    <span class="lane-metric">
                      <small>{{ t('groups.completedTasks') }}</small>
                      <strong>{{ lane.group.completedTasks ?? 0 }}</strong>
                    </span>
                    <span class="lane-metric" :class="{ 'is-overdue': (lane.group.overdueTasks ?? 0) > 0 }">
                      <small>{{ t('groups.overdueTasks') }}</small>
                      <strong>{{ lane.group.overdueTasks ?? 0 }}</strong>
                    </span>
                  </span>
                </button>

                <button
                    class="lane-action"
                    type="button"
                    :title="t('groups.leaderboard')"
                    :aria-label="`${lane.group.name} ${t('groups.leaderboard')}`"
                    @click="openLeaderboard(lane.group)"
                >
                  <el-icon><Trophy /></el-icon>
                </button>
              </article>
            </section>

            <section v-else class="empty-panel group-empty">
              <span class="empty-icon"><el-icon><User /></el-icon></span>
              <h3>{{ t('home.noGroupsTitle') }}</h3>
              <p>{{ t('home.noGroupsDescription') }}</p>
              <GroupActions @changed="onGroupChanged" />
            </section>
          </section>

          <section class="home-section" aria-labelledby="home-todos-heading">
            <header class="section-head">
              <div class="section-label">
                <span class="section-index">02</span>
                <span>{{ t('home.today') }}</span>
              </div>
              <div class="section-title-row">
                <h2 id="home-todos-heading">{{ t('home.todayTodos') }}</h2>
                <span class="section-note">{{ t('home.todoCountValue', { count: todoTotal }) }}</span>
              </div>
            </header>

            <section v-if="todoBuckets.length" class="todo-board">
              <section
                  v-for="bucket in todoBuckets"
                  :key="bucket.tone"
                  class="todo-bucket"
                  :class="`is-${bucket.tone}`"
              >
                <header class="bucket-head">
                  <span class="bucket-marker" aria-hidden="true"></span>
                  <span>{{ bucket.label }}</span>
                  <span class="bucket-count">{{ bucket.items.length }}</span>
                </header>
                <div class="todo-track">
                  <button
                      v-for="todo in bucket.items"
                      :key="todo.key"
                      class="todo-row"
                      :class="`is-${todo.tone}`"
                      type="button"
                      @click="openTodo(todo)"
                  >
                    <span class="todo-time">
                      <strong>{{ todo.dateLabel }}</strong>
                      <small>{{ todo.timeLabel }}</small>
                    </span>
                    <span class="todo-node" aria-hidden="true"></span>
                    <span class="todo-main">
                      <strong>{{ todo.itemTitle }}</strong>
                      <span class="todo-context">
                        <span class="todo-group">{{ todo.groupName }}</span>
                        <span class="todo-task">{{ todo.taskTitle }}</span>
                      </span>
                    </span>
                    <span class="todo-state">{{ todo.relative }}</span>
                    <el-icon class="todo-arrow"><ArrowRight /></el-icon>
                  </button>
                </div>
              </section>
            </section>

            <section v-else class="empty-panel compact-empty">
              <h3>{{ t('home.noTodosTitle') }}</h3>
              <p>{{ t('home.noTodosDescription') }}</p>
            </section>

            <button v-if="todoTotal > todos.length" class="todo-more" type="button" @click="goGroups">
              {{ t('home.todoMore', { count: todoTotal - todos.length }) }}
              <el-icon><ArrowRight /></el-icon>
            </button>
          </section>
        </div>

        <aside class="home-rail" :aria-label="t('home.railLabel')">
          <div class="rail-sticky">
            <CheckInCard />
          </div>
        </aside>
      </div>
    </template>

    <template v-else>
      <section class="home-section">
        <header class="section-head">
          <div class="section-label">
            <span class="section-index">01</span>
            <span>{{ t('home.howItWorks') }}</span>
          </div>
          <div class="section-title-row">
            <h2>{{ t('home.howItWorksTitle') }}</h2>
          </div>
        </header>
        <div class="step-rail">
          <article
              v-for="(step, index) in [t('home.stepOne'), t('home.stepTwo'), t('home.stepThree')]"
              :key="step"
              class="step-card"
          >
            <span class="step-number" aria-hidden="true">{{ String(index + 1).padStart(2, '0') }}</span>
            <div>
              <h3>{{ step }}</h3>
              <p>{{ t(`home.stepDescription${index + 1}`) }}</p>
            </div>
          </article>
        </div>
      </section>

      <section class="home-section">
        <header class="section-head">
          <div class="section-label">
            <span class="section-index">02</span>
            <span>{{ t('home.explore') }}</span>
          </div>
          <div class="section-title-row">
            <h2>{{ t('home.exploreTitle') }}</h2>
          </div>
        </header>
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
          <p class="section-label">
            <span class="section-index">03</span>
            <span>{{ t('home.guestLabel') }}</span>
          </p>
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
  --deck-ink: #071624;
  --deck-ink-soft: #0b2036;
  --deck-text: #f7fbff;
  --deck-muted: rgba(219, 234, 255, .68);
  --deck-line: rgba(139, 195, 255, .18);
  --deck-accent: #8ec5ff;
  display: grid;
  gap: 40px;
  max-width: 1180px;
  margin: 0 auto;
  padding: 36px 5% 64px;
}

.mission-deck {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1.12fr) minmax(330px, .88fr);
  gap: clamp(24px, 4vw, 48px);
  overflow: hidden;
  padding: clamp(28px, 4vw, 48px);
  border: 1px solid color-mix(in srgb, var(--el-color-primary) 34%, transparent);
  border-radius: 24px;
  color: var(--deck-text);
  background:
      radial-gradient(90% 130% at 94% -12%, color-mix(in srgb, var(--el-color-primary) 38%, transparent), transparent 58%),
      radial-gradient(60% 80% at -8% 112%, color-mix(in srgb, var(--el-color-primary-dark-2) 22%, transparent), transparent 62%),
      linear-gradient(128deg, var(--deck-ink) 0%, var(--deck-ink-soft) 58%, #061323 100%);
  box-shadow: 0 34px 80px -48px rgba(2, 16, 36, .9);
}

.deck-grid {
  position: absolute;
  inset: 0;
  background-image:
      linear-gradient(var(--deck-line) 1px, transparent 1px),
      linear-gradient(90deg, var(--deck-line) 1px, transparent 1px);
  background-size: 44px 44px;
  mask-image: linear-gradient(90deg, transparent 0%, #000 48%, #000 100%);
  opacity: .5;
  pointer-events: none;
}

.deck-glow {
  position: absolute;
  top: -160px;
  right: -120px;
  width: 360px;
  height: 360px;
  border: 1px solid rgba(139, 195, 255, .28);
  border-radius: 50%;
  box-shadow:
      0 0 0 64px rgba(64, 158, 255, .045),
      0 0 0 128px rgba(64, 158, 255, .028);
  pointer-events: none;
}

.deck-copy {
  position: relative;
  z-index: 1;
  display: grid;
  align-content: center;
  animation: deck-rise .6s cubic-bezier(.2, .8, .2, 1) both;
}

.deck-kicker {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0;
  color: var(--deck-muted);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: .16em;
}

.kicker-pulse {
  width: 6px;
  height: 6px;
  flex: 0 0 6px;
  border-radius: 50%;
  background: var(--el-color-primary);
  animation: pulse-dot 2.4s ease-out infinite;
}

.kicker-separator {
  color: rgba(219, 234, 255, .3);
}

.deck-copy h1 {
  max-width: 580px;
  margin: 18px 0 14px;
  color: #fff;
  font-size: clamp(34px, 4.5vw, 56px);
  font-weight: 700;
  line-height: 1.08;
  letter-spacing: -.035em;
}

.deck-copy h1 span {
  display: block;
}

.deck-copy h1 .hero-accent {
  color: var(--deck-accent);
}

.deck-description {
  max-width: 540px;
  margin: 0;
  color: var(--deck-muted);
  font-size: 14px;
  line-height: 1.8;
}

.deck-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 26px;
}

.primary-button,
.ghost-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-height: 42px;
  padding: 10px 18px;
  border-radius: 9px;
  font: inherit;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: border-color .2s, background-color .2s, color .2s, box-shadow .2s;
}

.primary-button {
  border: 1px solid var(--el-color-primary);
  color: var(--el-color-white);
  background: var(--el-color-primary);
  box-shadow: 0 10px 24px -12px color-mix(in srgb, var(--el-color-primary) 75%, transparent);
}

.primary-button:hover {
  border-color: var(--el-color-primary-light-3);
  background: var(--el-color-primary-light-3);
}

.ghost-button {
  border: 1px solid rgba(219, 234, 255, .26);
  color: var(--deck-text);
  background: rgba(255, 255, 255, .035);
}

.ghost-button:hover {
  border-color: color-mix(in srgb, var(--el-color-primary) 64%, transparent);
  color: #fff;
  background: color-mix(in srgb, var(--el-color-primary) 16%, transparent);
}

.primary-button:focus-visible,
.ghost-button:focus-visible,
.text-button:focus-visible,
.lane-main:focus-visible,
.lane-action:focus-visible,
.todo-row:focus-visible,
.todo-more:focus-visible {
  outline: 2px solid var(--el-color-primary);
  outline-offset: 2px;
}

.deck-console {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-rows: auto 1fr auto;
  gap: 20px;
  align-self: stretch;
  padding: 20px;
  border: 1px solid var(--deck-line);
  border-radius: 18px;
  background: rgba(4, 17, 33, .55);
  animation: deck-rise .65s cubic-bezier(.2, .8, .2, 1) .08s both;
}

.console-head,
.console-total {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: var(--deck-muted);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: .14em;
}

.console-head > span:first-child {
  color: #fff;
}

.console-total {
  font-weight: 600;
  letter-spacing: .04em;
  white-space: nowrap;
}

.console-main {
  display: flex;
  align-items: center;
  gap: clamp(16px, 3vw, 28px);
}

.completion-ring {
  position: relative;
  display: grid;
  width: clamp(126px, 14vw, 164px);
  aspect-ratio: 1;
  flex: 0 0 auto;
  place-items: center;
  filter: drop-shadow(0 0 18px rgba(64, 158, 255, .25));
}

.ring-track {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  mask: radial-gradient(farthest-side, transparent calc(100% - 15px), #000 calc(100% - 14px));
  -webkit-mask: radial-gradient(farthest-side, transparent calc(100% - 15px), #000 calc(100% - 14px));
}

.ring-core {
  display: grid;
  gap: 2px;
  place-items: center;
  text-align: center;
}

.ring-core strong {
  color: #fff;
  font-size: clamp(30px, 3.6vw, 42px);
  font-weight: 700;
  line-height: 1;
  letter-spacing: -.04em;
  font-variant-numeric: tabular-nums;
}

.ring-core strong small {
  margin-left: 1px;
  color: var(--deck-muted);
  font-size: .42em;
  letter-spacing: 0;
}

.ring-core > span {
  color: var(--deck-muted);
  font-size: 10px;
  letter-spacing: .12em;
}

.console-legend {
  display: grid;
  min-width: 0;
  flex: 1;
  gap: 10px;
}

.legend-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(139, 195, 255, .12);
  color: var(--deck-muted);
  font-size: 11.5px;
}

.legend-row:last-child {
  padding-bottom: 0;
  border-bottom: 0;
}

.legend-name {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  white-space: nowrap;
}

.legend-dot {
  width: 7px;
  height: 7px;
  flex: 0 0 7px;
  border-radius: 50%;
}

.legend-dot.is-done { background: var(--el-color-primary); }
.legend-dot.is-ongoing { background: var(--deck-accent); }
.legend-dot.is-overdue { background: var(--el-color-danger); }

.legend-row strong {
  color: #fff;
  font-size: 18px;
  font-variant-numeric: tabular-nums;
}

.legend-row.is-alert strong {
  color: var(--el-color-danger-light-3);
}

.console-foot {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid var(--deck-line);
}

.console-foot > div {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
}

.console-foot span {
  color: var(--deck-muted);
  font-size: 11px;
}

.console-foot strong {
  color: #fff;
  font-size: 20px;
  font-variant-numeric: tabular-nums;
}

.deck-art {
  position: relative;
  display: grid;
  min-height: 260px;
  place-items: center;
}

.art-ring {
  position: absolute;
  border: 1px solid rgba(139, 195, 255, .22);
  border-radius: 50%;
}

.art-ring-one { inset: 12px; }
.art-ring-two { inset: 48px; border-style: dashed; }
.art-ring-three {
  inset: 84px;
  border-color: color-mix(in srgb, var(--el-color-primary) 45%, transparent);
  box-shadow: inset 0 0 40px rgba(64, 158, 255, .15);
}

.art-core {
  position: relative;
  z-index: 1;
  display: grid;
  width: 74px;
  height: 74px;
  place-items: center;
  border-radius: 22px;
  color: #fff;
  background: color-mix(in srgb, var(--el-color-primary) 34%, transparent);
  border: 1px solid color-mix(in srgb, var(--el-color-primary) 60%, transparent);
  font-size: 32px;
  font-weight: 700;
}

.art-node {
  position: absolute;
  z-index: 2;
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  border: 1px solid rgba(139, 195, 255, .35);
  border-radius: 50%;
  color: var(--deck-muted);
  background: var(--deck-ink);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: .06em;
}

.art-node-one { top: 13%; left: 18%; }
.art-node-two { top: 45%; right: 8%; }
.art-node-three { bottom: 15%; left: 24%; }

.home-dashboard {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 352px;
  gap: 18px;
  align-items: start;
}

.home-column {
  display: grid;
  min-width: 0;
  gap: 42px;
}

.home-section {
  display: grid;
  gap: 16px;
}

.section-head {
  display: grid;
  gap: 9px;
}

.section-label {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  color: var(--el-text-color-placeholder);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: .13em;
}

.section-index {
  display: grid;
  width: 22px;
  height: 22px;
  flex: 0 0 22px;
  place-items: center;
  border: 1px solid var(--el-border-color);
  border-radius: 50%;
  color: var(--el-color-primary);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0;
  font-variant-numeric: tabular-nums;
}

.section-total {
  margin-left: auto;
  color: var(--el-text-color-placeholder);
  font-size: 11px;
  font-weight: 500;
  letter-spacing: .04em;
}

.section-title-row {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 16px;
}

.section-title-row h2 {
  margin: 0;
  color: var(--el-text-color-primary);
  font-size: clamp(22px, 2.4vw, 30px);
  line-height: 1.2;
  letter-spacing: -.02em;
}

.section-note {
  color: var(--el-text-color-placeholder);
  font-size: 12px;
  white-space: nowrap;
}

.text-button {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 0;
  border: 0;
  color: var(--el-color-primary);
  background: transparent;
  font: inherit;
  font-size: 13px;
  cursor: pointer;
  white-space: nowrap;
}

.text-button:hover {
  color: var(--el-color-primary-dark-2);
}

.group-stack {
  display: grid;
  gap: 10px;
}

.group-lane {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 50px;
  overflow: hidden;
  border: 1px solid var(--el-border-color-light);
  border-radius: 16px;
  background: var(--el-bg-color);
  transition: border-color .2s, box-shadow .2s, transform .2s;
}

.group-lane:hover {
  border-color: var(--el-color-primary-light-5);
  box-shadow: var(--el-box-shadow-lighter);
  transform: translateY(-1px);
}

.lane-main {
  display: grid;
  grid-template-columns: 44px minmax(0, 1.35fr) minmax(140px, .75fr) auto;
  align-items: center;
  gap: 18px;
  min-width: 0;
  padding: 18px 18px 18px 16px;
  border: 0;
  color: inherit;
  background: transparent;
  font: inherit;
  text-align: left;
  cursor: pointer;
}

.lane-number {
  display: grid;
  width: 32px;
  height: 32px;
  place-items: center;
  border-radius: 50%;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: .04em;
  font-variant-numeric: tabular-nums;
}

.lane-identity {
  display: grid;
  min-width: 0;
  gap: 5px;
}

.lane-title {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.lane-title strong {
  overflow: hidden;
  font-size: 17px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.role-badge {
  flex: 0 0 auto;
  padding: 2px 8px;
  border-radius: 999px;
  color: var(--el-color-info);
  background: var(--el-color-info-light-9);
  font-size: 10px;
  font-weight: 600;
}

.role-badge.owner {
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.lane-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  color: var(--el-text-color-placeholder);
  font-size: 11.5px;
}

.lane-meta > span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.lane-created {
  font-variant-numeric: tabular-nums;
}

.lane-progress {
  display: grid;
  min-width: 0;
  gap: 7px;
}

.lane-progress-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
}

.lane-progress-head small {
  color: var(--el-text-color-placeholder);
  font-size: 10px;
  letter-spacing: .06em;
  white-space: nowrap;
}

.lane-progress-head strong {
  color: var(--el-text-color-primary);
  font-size: 15px;
  font-variant-numeric: tabular-nums;
}

.lane-progress-head em {
  margin-left: 1px;
  color: var(--el-text-color-placeholder);
  font-size: 10px;
  font-style: normal;
}

.lane-track {
  display: block;
  height: 6px;
  overflow: hidden;
  border-radius: 999px;
  background: var(--el-fill-color);
}

.lane-fill {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--el-color-primary-dark-2), var(--el-color-primary));
  transform-origin: left;
  animation: lane-grow .75s cubic-bezier(.2, .8, .2, 1) both;
}

.lane-metrics {
  display: flex;
  gap: 16px;
}

.lane-metric {
  display: grid;
  min-width: 40px;
  gap: 1px;
}

.lane-metric small {
  color: var(--el-text-color-placeholder);
  font-size: 10px;
  white-space: nowrap;
}

.lane-metric strong {
  color: var(--el-text-color-primary);
  font-size: 18px;
  line-height: 1.15;
  font-variant-numeric: tabular-nums;
}

.lane-metric.is-overdue strong {
  color: var(--el-color-danger);
}

.lane-action {
  display: grid;
  place-items: center;
  border: 0;
  border-left: 1px solid var(--el-border-color-lighter);
  color: var(--el-text-color-secondary);
  background: transparent;
  cursor: pointer;
  transition: color .2s, background-color .2s;
}

.lane-action:hover {
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.lane-action .el-icon {
  font-size: 18px;
}

.lane-skeleton {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr) 170px;
  align-items: center;
  gap: 18px;
  padding: 24px 18px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 16px;
  background: var(--el-bg-color);
}

.skeleton {
  display: block;
  background: linear-gradient(90deg, var(--el-fill-color-light) 25%, var(--el-fill-color-lighter) 50%, var(--el-fill-color-light) 75%);
  background-size: 200% 100%;
  animation: skeleton-wave 1.4s ease infinite;
}

.skeleton-mark {
  width: 32px;
  height: 32px;
  border-radius: 50%;
}

.skeleton-title {
  width: 52%;
  height: 18px;
  border-radius: 9px;
}

.skeleton-track {
  width: 100%;
  height: 6px;
  border-radius: 999px;
}

.todo-board {
  overflow: hidden;
  border: 1px solid var(--el-border-color-light);
  border-radius: 18px;
  background: var(--el-bg-color);
}

.todo-bucket {
  --bucket-color: var(--el-color-primary);
}

.todo-bucket + .todo-bucket {
  border-top: 1px solid var(--el-border-color-lighter);
}

.todo-bucket.is-overdue { --bucket-color: var(--el-color-danger); }
.todo-bucket.is-today { --bucket-color: var(--el-color-warning); }
.todo-bucket.is-nodue { --bucket-color: var(--el-text-color-placeholder); }

.bucket-head {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 13px 20px 4px;
  color: var(--el-text-color-secondary);
  font-size: 10.5px;
  font-weight: 700;
  letter-spacing: .12em;
}

.bucket-marker {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--bucket-color);
}

.bucket-count {
  display: grid;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  place-items: center;
  border-radius: 999px;
  color: var(--el-text-color-placeholder);
  background: var(--el-fill-color-light);
  font-size: 10px;
  letter-spacing: 0;
  font-variant-numeric: tabular-nums;
}

.todo-track {
  --time-col: 86px;
  position: relative;
  padding: 2px 12px 12px;
}

.todo-track::before {
  content: '';
  position: absolute;
  top: 14px;
  bottom: 20px;
  left: calc(22px + var(--time-col) + 10px + 10px);
  width: 1px;
  background: var(--el-border-color-lighter);
}

.todo-row {
  --todo-color: var(--el-color-primary);
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: var(--time-col) 20px minmax(0, 1fr) auto 18px;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 12px 10px;
  border: 0;
  border-radius: 12px;
  color: inherit;
  background: transparent;
  font: inherit;
  text-align: left;
  cursor: pointer;
  transition: background-color .2s;
}

.todo-row:hover {
  background: var(--el-fill-color-lighter);
}

.todo-row.is-overdue { --todo-color: var(--el-color-danger); }
.todo-row.is-today { --todo-color: var(--el-color-warning); }
.todo-row.is-nodue { --todo-color: var(--el-text-color-placeholder); }

.todo-time {
  display: grid;
  justify-items: end;
  gap: 1px;
  color: var(--el-text-color-placeholder);
  font-variant-numeric: tabular-nums;
}

.todo-time strong {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.todo-time small {
  font-size: 10px;
}

.todo-node {
  width: 8px;
  height: 8px;
  justify-self: center;
  border-radius: 50%;
  background: var(--todo-color);
  box-shadow: 0 0 0 4px var(--el-bg-color);
}

.todo-main {
  display: grid;
  min-width: 0;
  gap: 3px;
}

.todo-main > strong {
  overflow: hidden;
  color: var(--el-text-color-primary);
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.todo-context {
  display: flex;
  min-width: 0;
  gap: 6px;
  overflow: hidden;
  color: var(--el-text-color-placeholder);
  font-size: 11.5px;
  white-space: nowrap;
}

.todo-group {
  flex: 0 0 auto;
  color: var(--el-color-primary);
}

.todo-task {
  overflow: hidden;
  text-overflow: ellipsis;
}

.todo-task::before {
  content: '·';
  margin-right: 6px;
  color: var(--el-text-color-placeholder);
}

.todo-state {
  padding: 3px 8px;
  border: 1px solid color-mix(in srgb, var(--todo-color) 22%, transparent);
  border-radius: 999px;
  color: var(--todo-color);
  background: color-mix(in srgb, var(--todo-color) 10%, transparent);
  font-size: 10px;
  font-weight: 600;
  white-space: nowrap;
}

.todo-arrow {
  color: var(--el-text-color-placeholder);
  transition: color .2s, transform .2s;
}

.todo-row:hover .todo-arrow {
  color: var(--el-color-primary);
  transform: translateX(2px);
}

.todo-more {
  display: inline-flex;
  align-items: center;
  justify-self: start;
  gap: 4px;
  padding: 4px 0;
  border: 0;
  color: var(--el-color-primary);
  background: transparent;
  font: inherit;
  font-size: 12px;
  cursor: pointer;
}

.todo-more:hover {
  color: var(--el-color-primary-dark-2);
}

.home-rail {
  padding-top: 83px;
}

.rail-sticky {
  position: sticky;
  top: 22px;
}

.empty-panel {
  display: grid;
  justify-items: center;
  gap: 10px;
  padding: 42px 20px;
  border: 1px dashed var(--el-border-color);
  border-radius: 16px;
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

.step-rail {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  overflow: hidden;
  border: 1px solid var(--el-border-color-light);
  border-radius: 18px;
  background: var(--el-bg-color);
}

.step-card {
  display: grid;
  align-content: start;
  gap: 14px;
  padding: 24px;
}

.step-card + .step-card {
  border-left: 1px solid var(--el-border-color-lighter);
}

.step-number {
  color: transparent;
  font-size: clamp(38px, 4vw, 56px);
  font-weight: 700;
  line-height: .9;
  letter-spacing: -.04em;
  -webkit-text-stroke: 1px var(--el-color-primary-light-5);
  font-variant-numeric: tabular-nums;
}

.step-card h3 {
  margin: 0 0 6px;
  font-size: 16px;
}

.step-card p {
  margin: 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 1.65;
}

.guest-course-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.guest-course-card {
  position: relative;
  overflow: hidden;
  padding: 22px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 16px;
  background: var(--el-bg-color);
}

.guest-course-card::after {
  content: '';
  position: absolute;
  right: -34px;
  bottom: -34px;
  width: 96px;
  height: 96px;
  border: 1px solid var(--el-color-primary-light-7);
  border-radius: 50%;
  pointer-events: none;
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
  font-size: 10px;
}

.guest-course-desc {
  margin: 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.guest-course-meta {
  display: inline-block;
  margin-top: 14px;
  color: var(--el-text-color-placeholder);
  font-size: 12px;
}

.login-guide-card {
  position: relative;
  display: flex;
  overflow: hidden;
  align-items: center;
  justify-content: space-between;
  gap: 26px;
  padding: 32px;
  border: 1px solid var(--el-color-primary-light-7);
  border-radius: 18px;
  background:
      radial-gradient(80% 140% at 100% 0%, var(--el-color-primary-light-8), transparent 60%),
      var(--el-color-primary-light-9);
}

.login-guide-card > * {
  position: relative;
  z-index: 1;
}

.login-guide-card h2 {
  margin: 10px 0 6px;
  font-size: 22px;
}

.login-guide-card p:not(.section-label) {
  margin: 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 1.65;
}

@keyframes deck-rise {
  from {
    opacity: 0;
    transform: translateY(14px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes lane-grow {
  from { transform: scaleX(0); }
  to { transform: scaleX(1); }
}

@keyframes pulse-dot {
  0%, 100% { box-shadow: 0 0 0 0 color-mix(in srgb, var(--el-color-primary) 45%, transparent); }
  50% { box-shadow: 0 0 0 6px transparent; }
}

@keyframes skeleton-wave {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

@media (max-width: 1080px) {
  .home-dashboard {
    grid-template-columns: 1fr;
  }

  .home-rail {
    padding-top: 0;
  }

  .rail-sticky {
    position: static;
  }
}

@media (max-width: 980px) {
  .mission-deck {
    grid-template-columns: 1fr;
  }

  .deck-console {
    align-self: auto;
  }

  .deck-art {
    min-height: 220px;
  }
}

@media (max-width: 860px) {
  .lane-main {
    grid-template-columns: 38px minmax(0, 1fr);
    row-gap: 14px;
  }

  .lane-progress,
  .lane-metrics {
    grid-column: 1 / -1;
  }

  .lane-metrics {
    justify-content: space-between;
  }

  .lane-number {
    align-self: start;
  }
}

@media (max-width: 760px) {
  .home-page {
    gap: 30px;
    padding: 24px 4% 48px;
  }

  .mission-deck {
    padding: 26px 22px;
    border-radius: 20px;
  }

  .deck-copy h1 {
    font-size: clamp(30px, 8vw, 40px);
  }

  .deck-description {
    font-size: 13px;
  }

  .deck-console {
    padding: 16px;
    gap: 16px;
  }

  .console-main {
    gap: 18px;
  }

  .completion-ring {
    width: 120px;
  }

  .ring-core strong {
    font-size: 30px;
  }

  .section-title-row {
    align-items: center;
  }

  .section-title-row h2 {
    font-size: 22px;
  }

  .step-rail {
    grid-template-columns: 1fr;
  }

  .step-card + .step-card {
    border-top: 1px solid var(--el-border-color-lighter);
    border-left: 0;
  }

  .guest-course-grid {
    grid-template-columns: 1fr;
  }

  .login-guide-card {
    align-items: flex-start;
    flex-direction: column;
    padding: 24px;
  }
}

@media (max-width: 620px) {
  .group-lane {
    grid-template-columns: minmax(0, 1fr) 44px;
  }

  .lane-main {
    padding: 16px 14px;
    gap: 12px;
  }

  .lane-title strong {
    font-size: 16px;
  }

  .lane-created {
    display: none;
  }

  .lane-metrics {
    gap: 12px;
  }

  .lane-metric {
    min-width: 0;
    flex: 1;
  }

  .lane-skeleton {
    grid-template-columns: 32px minmax(0, 1fr);
  }

  .skeleton-track {
    grid-column: 1 / -1;
  }

  .todo-track {
    --time-col: 64px;
    padding: 2px 8px 8px;
  }

  .todo-track::before {
    left: calc(16px + var(--time-col) + 8px + 9px);
  }

  .todo-row {
    grid-template-columns: var(--time-col) 18px minmax(0, 1fr) auto;
    gap: 8px;
    padding: 11px 8px;
  }

  .todo-row > .todo-arrow {
    display: none;
  }

  .todo-state {
    padding: 3px 6px;
    font-size: 9.5px;
  }

  .todo-context {
    font-size: 10.5px;
  }

  .deck-art {
    display: none;
  }
}

@media (max-width: 430px) {
  .console-main {
    align-items: flex-start;
    flex-direction: column;
  }

  .console-legend {
    width: 100%;
  }

  .completion-ring {
    align-self: center;
  }

  .deck-actions {
    width: 100%;
  }

  .deck-actions .primary-button,
  .deck-actions .ghost-button {
    flex: 1;
  }

  .lane-metrics {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .todo-row {
    grid-template-columns: var(--time-col) 16px minmax(0, 1fr) auto;
    gap: 6px;
  }

  .todo-state {
    padding: 2px 5px;
    font-size: 9px;
  }

  .todo-track::before {
    left: calc(16px + var(--time-col) + 6px + 8px);
  }
}

@media (prefers-reduced-motion: reduce) {
  .deck-copy,
  .deck-console,
  .kicker-pulse,
  .lane-fill,
  .skeleton {
    animation: none;
  }

  .group-lane,
  .todo-row,
  .lane-action,
  .todo-arrow,
  .primary-button,
  .ghost-button {
    transition: none;
  }
}
</style>