<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { get, post } from '@/net'

interface CheckInStatus {
  dates: string[]
  todayChecked: boolean
  streak: number
  points: number
  checkInCount: number
  monthPoints: number
  records: Array<{ date: string; points: number; streak: number }>
}

const { t } = useI18n()
const now = new Date()
const selectedYear = ref(now.getFullYear())
const selectedMonth = ref(now.getMonth() + 1)
const today = now.toISOString().slice(0, 10)
const loading = ref(false)
let disposed = false
const checkInStatus = ref<CheckInStatus>({
  dates: [], todayChecked: false, streak: 0, points: 0, checkInCount: 0, monthPoints: 0, records: [],
})

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
      data => {
        if (disposed) return
        checkInStatus.value = data
        loading.value = false
      },
      () => {
        if (!disposed) loading.value = false
      },
  )
}

function changeMonth(offset: number) {
  const date = new Date(selectedYear.value, selectedMonth.value - 1 + offset, 1)
  selectedYear.value = date.getFullYear()
  selectedMonth.value = date.getMonth() + 1
  loadCheckInStatus()
}

function checkIn() {
  post('/api/check-in', null, loadCheckInStatus)
}

onMounted(loadCheckInStatus)
onUnmounted(() => {
  disposed = true
})
</script>

<template>
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
      <span class="check-in-metric check-in-metric--points">{{ t('home.points') }} <strong>{{ checkInStatus.points }}</strong></span>
      <span class="check-in-metric check-in-metric--streak">{{ t('home.streak') }} <strong>{{ checkInStatus.streak }}</strong>{{ t('home.days') }}</span>
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
</template>

<style scoped>
.check-in-card {
  padding: 24px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 14px;
  background: var(--el-bg-color);
}

.card-label {
  margin: 0 0 4px;
  color: var(--el-text-color-placeholder);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: .13em;
}

.check-in-header,
.calendar-heading,
.check-in-summary {
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
  font: inherit;
  font-size: 13px;
  cursor: pointer;
}
.check-in-button:disabled { opacity: .55; cursor: not-allowed; }
.check-in-summary {
  justify-content: flex-start;
  margin: 18px 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
.check-in-metric {
  display: inline-flex;
  min-height: 36px;
  align-items: center;
  gap: 5px;
  padding: 6px 10px;
  border-radius: 9px;
  background: var(--el-fill-color-lighter);
}
.check-in-metric strong { font-size: 20px; }
.check-in-metric--points {
  color: var(--el-color-warning);
  background: color-mix(in srgb, var(--el-color-warning-light-9) 72%, var(--el-bg-color));
}
.check-in-metric--points strong { color: var(--el-color-warning-dark-2); }
.check-in-metric--streak {
  color: var(--el-color-success);
  background: color-mix(in srgb, var(--el-color-success-light-9) 72%, var(--el-bg-color));
}
.check-in-metric--streak strong { color: var(--el-color-success-dark-2); }
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

@media (max-width: 700px) {
  .check-in-card { padding: 18px; }
  .check-in-header { align-items: flex-start; flex-direction: column; }
}
</style>
