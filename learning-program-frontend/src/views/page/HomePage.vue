<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ArrowLeft, ArrowRight, Reading } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { get, post } from '@/net'

const { t } = useI18n()
const router = useRouter()
interface CheckInStatus {
  dates: string[]
  todayChecked: boolean
  streak: number
  points: number
  checkInCount: number
  monthPoints: number
  records: Array<{ date: string; points: number; streak: number }>
}

interface ProgressSummary {
  ongoingCount: number
  completedCount: number
  weeklySeconds: number
}

const now = new Date()
const selectedYear = ref(now.getFullYear())
const selectedMonth = ref(now.getMonth() + 1)
const checkInStatus = ref<CheckInStatus>({
  dates: [], todayChecked: false, streak: 0, points: 0, checkInCount: 0, monthPoints: 0, records: [],
})
const progressSummary = ref<ProgressSummary | null>(null)
const loading = ref(false)
const today = now.toISOString().slice(0, 10)

const stats = computed(() => [
  {
    label: 'home.ongoingCourses',
    value: progressSummary.value ? String(progressSummary.value.ongoingCount) : '—',
    hint: 'home.ongoingHint',
  }, {
    label: 'home.weeklyTime',
    value: progressSummary.value ? formatDuration(progressSummary.value.weeklySeconds) : '—',
    hint: 'home.weeklyHint',
  }, {
    label: 'home.completedCourses',
    value: progressSummary.value ? String(progressSummary.value.completedCount) : '—',
    hint: 'home.completedHint',
  },
])

function formatDuration(seconds: number): string {
  if (seconds <= 0) return t('home.noStudyTime')
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  if (hours > 0) return `${hours}${t('home.hours')}${minutes > 0 ? minutes + t('home.minutes') : ''}`
  if (minutes > 0) return `${minutes}${t('home.minutes')}`
  return t('home.lessThanMinute')
}

function loadProgressSummary() {
  get<{ summary: ProgressSummary }>('/api/courses/progress', data => {
    progressSummary.value = data.summary
  })
}

const monthTitle = computed(() => `${selectedYear.value}年${selectedMonth.value}月`)
const calendarDays = computed(() => {
  const firstDay = new Date(selectedYear.value, selectedMonth.value - 1, 1).getDay()
  const daysInMonth = new Date(selectedYear.value, selectedMonth.value, 0).getDate()
  return Array.from({ length: 42 }, (_, index) => {
    const day = index - firstDay + 1
    return day > 0 && day <= daysInMonth ? day : null
  })
})

function dateKey(day: number): string {
  return `${selectedYear.value}-${String(selectedMonth.value).padStart(2, '0')}-${String(day).padStart(2, '0')}`
}

function loadCheckInStatus() {
  loading.value = true
  get<CheckInStatus>(
    `/api/check-in?year=${selectedYear.value}&month=${selectedMonth.value}`,
    (data) => {
      checkInStatus.value = data
      loading.value = false
    },
    () => { loading.value = false },
  )
}

function changeMonth(offset: number) {
  const date = new Date(selectedYear.value, selectedMonth.value - 1 + offset, 1)
  selectedYear.value = date.getFullYear()
  selectedMonth.value = date.getMonth() + 1
  loadCheckInStatus()
}

function checkIn() {
  post('/api/check-in', null, () => {
    loadCheckInStatus()
  })
}

onMounted(() => {
  loadCheckInStatus()
  loadProgressSummary()
})
</script>

<template>
  <main class="content">
        <div class="hero-card">
          <div>
            <p class="card-label">{{ t('home.plan') }}</p>
            <h2>{{ t('home.focus') }}</h2>
            <p class="hero-copy">{{ t('home.planDescription') }}</p>
            <button class="primary-button" type="button" @click="router.push({ name: 'courses' })">{{ t('home.start') }}</button>
          </div>
          <div class="hero-decoration" aria-hidden="true">
            <div class="decoration-circle circle-large"></div>
            <div class="decoration-circle circle-small"></div>
            <span>✦</span>
          </div>
        </div>

        <div class="section-heading">
          <div>
            <p class="card-label">{{ t('home.overview') }}</p>
            <h2>{{ t('home.summary') }}</h2>
          </div>
          <button class="placeholder-chip chip-link" type="button" @click="router.push({ name: 'courses' })">
            {{ t('courses.title') }}
          </button>
        </div>

        <div class="stats-grid">
          <article v-for="stat in stats" :key="stat.label" class="stat-card">
            <p>{{ t(stat.label) }}</p>
            <strong>{{ stat.value }}</strong>
            <span>{{ t(stat.hint) }}</span>
          </article>
        </div>

        <section class="check-in-card">
          <div class="check-in-header">
            <div>
              <p class="card-label">{{ t('home.checkIn') }}</p>
              <h2>{{ t('home.checkInTitle') }}</h2>
            </div>
            <button class="check-in-button" type="button" :disabled="checkInStatus.todayChecked || loading" @click="checkIn">
              {{ checkInStatus.todayChecked ? t('home.checkedIn') : t('home.checkInNow') }}
            </button>
          </div>
          <div class="check-in-summary">
            <span>{{ t('home.points') }} <strong>{{ checkInStatus.points }}</strong></span>
            <span>{{ t('home.streak') }} <strong>{{ checkInStatus.streak }}</strong>{{ t('home.days') }}</span>
          </div>
          <div class="calendar-heading">
            <button type="button" :aria-label="t('home.previousMonth')" @click="changeMonth(-1)"><el-icon><ArrowLeft /></el-icon></button>
            <strong>{{ monthTitle }}</strong>
            <button type="button" :aria-label="t('home.nextMonth')" @click="changeMonth(1)"><el-icon><ArrowRight /></el-icon></button>
          </div>
          <div class="calendar-weekdays">
            <span v-for="weekday in t('home.weekdays').split(',')" :key="weekday">{{ weekday }}</span>
          </div>
          <div class="calendar-grid">
            <span
              v-for="(day, index) in calendarDays"
              :key="index"
              class="calendar-day"
              :class="{ checked: day !== null && checkInStatus.dates.includes(dateKey(day)), today: day !== null && dateKey(day) === today }"
            >{{ day }}</span>
          </div>
          <div class="points-summary">
            <div class="points-summary-heading">
              <strong>{{ t('home.pointsSummary') }}</strong>
              <span>{{ t('home.monthPoints') }} {{ checkInStatus.monthPoints }}</span>
            </div>
            <div v-if="checkInStatus.records.length" class="points-table-wrapper">
              <table class="points-table">
                <thead>
                  <tr>
                    <th>{{ t('home.checkInDate') }}</th>
                    <th>{{ t('home.dailyPoints') }}</th>
                    <th>{{ t('home.continuousDays') }}</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="record in checkInStatus.records" :key="record.date">
                    <td>{{ record.date }}</td>
                    <td>+{{ record.points }}</td>
                    <td>{{ record.streak }}{{ t('home.days') }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
            <p v-else class="points-empty">{{ t('home.noPointsRecord') }}</p>
          </div>
        </section>

        <div class="empty-panel">
          <div class="empty-icon"><el-icon>
              <Reading />
            </el-icon></div>
          <h3>{{ t('home.ready') }}</h3>
          <p>{{ t('home.readyDescription') }}</p>
        </div>
  </main>
</template>

<style scoped>
:global(*) {
  box-sizing: border-box;
}

:global(body) {
  background: var(--el-bg-color-page);
  color: var(--el-text-color-primary);
  font-family: Inter, "PingFang SC", "Microsoft YaHei", sans-serif;
}

.app-shell {
  display: flex;
  min-height: 100vh;
  background: var(--el-bg-color-page);
}

.sidebar {
  width: 248px;
  flex: 0 0 248px;
  display: flex;
  flex-direction: column;
  padding: 28px 16px 20px;
  background: var(--el-bg-color);
  border-right: 1px solid var(--el-border-color-light);
}

.brand {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 0 13px;
  color: var(--el-text-color-primary);
  font-size: 18px;
  font-weight: 700;
}

.brand-mark {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  border-radius: 10px;
  color: var(--el-color-white);
  background: var(--el-color-primary);
  box-shadow: var(--el-box-shadow-light);
}

.sidebar-placeholder {
  display: grid;
  gap: 9px;
  margin: 42px 13px 24px;
}

.sidebar-placeholder span {
  width: 100%;
  height: 9px;
  border-radius: 5px;
  background: var(--el-fill-color-light);
}

.sidebar-placeholder span:nth-child(2) {
  width: 72%;
}

.sidebar-placeholder span:nth-child(3) {
  width: 86%;
}

.side-nav {
  display: grid;
  gap: 7px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 14px;
  width: 100%;
  padding: 12px 14px;
  border: 0;
  border-radius: 10px;
  color: var(--el-text-color-secondary);
  background: transparent;
  font: inherit;
  font-size: 14px;
  text-align: left;
  cursor: pointer;
  transition: .2s;
}

.nav-item:hover {
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.nav-item.active {
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  font-weight: 600;
}

.nav-item .el-icon {
  font-size: 18px;
}

.sidebar-footer {
  margin-top: auto;
  padding-top: 20px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.page {
  min-width: 0;
  flex: 1;
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 100px;
  padding: 22px 5%;
  background: var(--el-bg-color);
  border-bottom: 1px solid var(--el-border-color-light);
}

.eyebrow,
.card-label {
  margin: 0 0 7px;
  color: var(--el-text-color-placeholder);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: .13em;
}

h1,
h2,
h3,
p {
  margin-top: 0;
}

h1 {
  margin-bottom: 0;
  font-size: 24px;
}

.topbar-actions {
  display: flex;
  align-items: center;
  gap: 22px;
}

.icon-button,
.logout-button {
  border: 0;
  background: transparent;
  cursor: pointer;
}

.icon-button {
  position: relative;
  display: grid;
  place-items: center;
  color: var(--el-text-color-secondary);
  font-size: 20px;
}

.notification-dot {
  position: absolute;
  top: -1px;
  right: -2px;
  width: 6px;
  height: 6px;
  border: 1px solid var(--el-color-white);
  border-radius: 50%;
  background: var(--el-color-danger);
}

.profile {
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  border-radius: 50%;
  color: var(--el-color-white);
  background: var(--el-color-info);
  font-size: 18px;
}

.profile-name {
  color: var(--el-text-color-regular);
  font-size: 14px;
}

.logout-button {
  padding: 9px 14px;
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  color: var(--el-text-color-regular);
  font-size: 13px;
}

.logout-button:hover {
  color: var(--el-color-primary);
  border-color: var(--el-color-primary-light-5);
  background: var(--el-color-primary-light-9);
}

.content {
  max-width: 1180px;
  margin: 0 auto;
  padding: 42px 5% 60px;
}

.hero-card {
  position: relative;
  display: flex;
  min-height: 224px;
  align-items: center;
  justify-content: space-between;
  overflow: hidden;
  padding: 34px 42px;
  border-radius: 18px;
  color: var(--el-color-white);
  background: linear-gradient(120deg, var(--el-color-primary), var(--el-color-primary-light-3));
  box-shadow: var(--el-box-shadow);
}

.hero-card h2 {
  margin-bottom: 10px;
  font-size: 28px;
}

.hero-copy {
  max-width: 470px;
  margin-bottom: 24px;
  color: var(--el-color-primary-light-9);
  font-size: 14px;
  line-height: 1.7;
}

.primary-button {
  padding: 11px 20px;
  border: 0;
  border-radius: 8px;
  color: var(--el-color-primary);
  background: var(--el-color-white);
  font: inherit;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.hero-decoration {
  position: relative;
  width: 190px;
  height: 160px;
  margin-right: 5%;
  opacity: .8;
}

.decoration-circle {
  position: absolute;
  border: 1px solid color-mix(in srgb, var(--el-color-white) 30%, transparent);
  border-radius: 50%;
}

.circle-large {
  inset: -20px -15px 0 15px;
}

.circle-small {
  inset: 25px 30px 35px 55px;
}

.hero-decoration span {
  position: absolute;
  top: 55px;
  left: 96px;
  font-size: 35px;
}

.section-heading {
  display: flex;
  align-items: end;
  justify-content: space-between;
  margin: 42px 0 18px;
}

.section-heading h2 {
  margin-bottom: 0;
  font-size: 20px;
}

.placeholder-chip {
  padding: 7px 11px;
  border: 0;
  border-radius: 20px;
  color: var(--el-text-color-secondary);
  background: var(--el-fill-color-light);
  font: inherit;
  font-size: 12px;
}

.chip-link {
  cursor: pointer;
}

.chip-link:hover {
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 18px;
}

.check-in-card {
  margin-top: 18px;
  padding: 24px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 14px;
  background: var(--el-bg-color);
}

.check-in-header, .calendar-heading, .check-in-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.check-in-header h2 { margin: 0; font-size: 20px; }
.check-in-button {
  padding: 10px 16px;
  border: 0;
  border-radius: 8px;
  color: var(--el-color-white);
  background: var(--el-color-primary);
  cursor: pointer;
}
.check-in-button:disabled { opacity: .55; cursor: not-allowed; }
.check-in-summary {
  justify-content: flex-start;
  margin: 18px 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
.check-in-summary strong { margin: 0 3px; color: var(--el-color-primary); font-size: 20px; }
.calendar-heading { margin: 12px 0; }
.calendar-heading button {
  display: grid;
  width: 28px;
  height: 28px;
  place-items: center;
  border: 0;
  border-radius: 7px;
  color: var(--el-text-color-secondary);
  background: transparent;
  cursor: pointer;
}
.calendar-heading button:hover { color: var(--el-color-primary); background: var(--el-color-primary-light-9); }
.calendar-weekdays, .calendar-grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 6px; text-align: center; }
.calendar-weekdays { color: var(--el-text-color-placeholder); font-size: 12px; }
.calendar-grid { margin-top: 8px; }
.calendar-day { display: grid; width: 34px; height: 34px; place-self: center; place-items: center; border-radius: 50%; color: var(--el-text-color-regular); font-size: 13px; }
.calendar-day.checked { color: var(--el-color-white); background: var(--el-color-primary); }
.calendar-day.today { box-shadow: inset 0 0 0 1px var(--el-color-primary); }
.calendar-day.checked.today { box-shadow: inset 0 0 0 2px var(--el-color-primary-light-3); }
.points-summary { margin-top: 24px; border-top: 1px solid var(--el-border-color-lighter); padding-top: 18px; }
.points-summary-heading { display: flex; justify-content: space-between; gap: 12px; margin-bottom: 10px; font-size: 13px; }
.points-summary-heading span { color: var(--el-color-primary); }
.points-table-wrapper { overflow-x: auto; }
.points-table { width: 100%; border-collapse: collapse; font-size: 12px; }
.points-table th, .points-table td { padding: 9px 8px; border-bottom: 1px solid var(--el-border-color-lighter); text-align: left; white-space: nowrap; }
.points-table th { color: var(--el-text-color-placeholder); font-weight: 600; }
.points-table td:nth-child(n+2) { color: var(--el-color-primary); }
.points-empty { margin: 0; color: var(--el-text-color-placeholder); font-size: 12px; }

.stat-card {
  padding: 22px 24px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 14px;
  background: var(--el-bg-color);
}

.stat-card p {
  margin-bottom: 18px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.stat-card strong {
  display: block;
  margin-bottom: 9px;
  color: var(--el-text-color-primary);
  font-size: 30px;
}

.stat-card span {
  color: var(--el-text-color-placeholder);
  font-size: 12px;
}

.empty-panel {
  display: grid;
  justify-items: center;
  margin-top: 18px;
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
  margin-bottom: 0;
  color: var(--el-text-color-placeholder);
  font-size: 13px;
}

@media (max-width: 760px) {
  .sidebar {
    width: 70px;
    flex-basis: 70px;
    padding: 22px 10px;
  }

  .brand {
    justify-content: center;
    padding: 0;
  }

  .brand span,
  .nav-item span,
  .sidebar-placeholder {
    display: none;
  }

  .nav-item {
    justify-content: center;
    padding: 13px 0;
  }

  .topbar {
    align-items: flex-start;
    gap: 16px;
  }

  .profile-name {
    display: none;
  }

  .topbar-actions {
    gap: 12px;
  }

  .hero-card {
    padding: 28px;
  }

  .hero-decoration {
    display: none;
  }

  .check-in-header { align-items: flex-start; flex-direction: column; }

  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>
