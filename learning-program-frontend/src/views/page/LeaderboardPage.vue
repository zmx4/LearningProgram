<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Trophy, UserFilled } from '@element-plus/icons-vue'
import { get } from '@/net'

interface LeaderboardEntry {
  rank: number
  accountId: number
  username: string
  value: number
  rankedUsers: number
}

interface Leaderboard {
  metric: string
  metricLabel: string
  unit: string
  limit: number
  rankedUsers: number
  entries: LeaderboardEntry[]
  me: LeaderboardEntry | null
}

interface MetricOption {
  code: string
  label: string
  unit: string
}

const { t } = useI18n()

const metrics = ref<MetricOption[]>([])
const activeMetric = ref('points')
const board = ref<Leaderboard | null>(null)
const loading = ref(true)
const loadFailed = ref(false)

/** 学习时长后端给的是秒，这里换成人看得懂的说法 */
function formatDuration(seconds: number): string {
  if (seconds < 60) return t('leaderboard.seconds', { count: seconds })
  if (seconds < 3600) return t('leaderboard.minutes', { count: Math.floor(seconds / 60) })
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  return minutes > 0
    ? t('leaderboard.hoursMinutes', { hours, minutes })
    : t('leaderboard.hours', { hours })
}

function formatValue(value: number): string {
  if (!board.value) return String(value)
  return board.value.metric === 'study'
    ? formatDuration(value)
    : `${value} ${board.value.unit}`
}

function loadBoard(metric: string) {
  loading.value = true
  loadFailed.value = false
  get<Leaderboard>(`/api/leaderboard?metric=${encodeURIComponent(metric)}&limit=20`, data => {
    board.value = data
    loading.value = false
  }, () => {
    loading.value = false
    loadFailed.value = true
  })
}

function selectMetric(metric: string) {
  if (metric === activeMetric.value && board.value) return
  activeMetric.value = metric
  loadBoard(metric)
}

function loadMetrics() {
  loading.value = true
  get<MetricOption[]>('/api/leaderboard/metrics', data => {
    metrics.value = data
    const first = data[0]?.code ?? 'points'
    activeMetric.value = first
    loadBoard(first)
  }, () => {
    loading.value = false
    loadFailed.value = true
  })
}

const rankedLabel = computed(() => t('leaderboard.rankedUsers', { count: board.value?.rankedUsers ?? 0 }))

onMounted(loadMetrics)
</script>

<template>
  <main class="leaderboard-page">
    <section class="leaderboard-heading">
      <p class="eyebrow">LEADERBOARD</p>
      <h2>{{ t('leaderboard.title') }}</h2>
      <p>{{ t('leaderboard.description') }}</p>
    </section>

    <div class="metric-tabs" role="tablist" :aria-label="t('leaderboard.metricLabel')">
      <button
        v-for="option in metrics"
        :key="option.code"
        class="metric-tab"
        :class="{ active: option.code === activeMetric }"
        type="button"
        role="tab"
        :aria-selected="option.code === activeMetric"
        @click="selectMetric(option.code)"
      >
        {{ option.label }}
      </button>
    </div>

    <section v-if="loading" class="board-card" aria-busy="true">
      <div v-for="index in 5" :key="index" class="skeleton row"></div>
    </section>

    <section v-else-if="loadFailed" class="empty-panel">
      <h3>{{ t('leaderboard.loadFailed') }}</h3>
      <button class="ghost-button" type="button" @click="loadMetrics">{{ t('resources.retry') }}</button>
    </section>

    <template v-else-if="board">
      <section class="my-rank-card">
        <span class="my-rank-icon"><el-icon><UserFilled /></el-icon></span>
        <div class="my-rank-body">
          <strong>{{ t('leaderboard.myRank') }}</strong>
          <p v-if="board.me">
            {{ t('leaderboard.myRankValue', { rank: board.me.rank, value: formatValue(board.me.value) }) }}
          </p>
          <p v-else class="muted">{{ t('leaderboard.myRankEmpty') }}</p>
        </div>
        <span class="ranked-users">{{ rankedLabel }}</span>
      </section>

      <section v-if="board.entries.length" class="board-card">
        <div class="board-header">
          <span class="col-rank">{{ t('leaderboard.rank') }}</span>
          <span class="col-user">{{ t('leaderboard.user') }}</span>
          <span class="col-value">{{ board.metricLabel }}</span>
        </div>
        <div
          v-for="entry in board.entries"
          :key="entry.accountId"
          class="board-row"
          :class="{ me: board.me && entry.accountId === board.me.accountId, top: entry.rank <= 3 }"
        >
          <span class="col-rank">
            <span class="rank-badge" :class="`rank-${entry.rank}`">{{ entry.rank }}</span>
          </span>
          <span class="col-user">{{ entry.username }}</span>
          <span class="col-value">{{ formatValue(entry.value) }}</span>
        </div>
      </section>

      <section v-else class="empty-panel">
        <span class="empty-icon"><el-icon><Trophy /></el-icon></span>
        <h3>{{ t('leaderboard.empty') }}</h3>
        <p>{{ t('leaderboard.emptyHint') }}</p>
      </section>
    </template>
  </main>
</template>

<style scoped>
.leaderboard-page {
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

.leaderboard-heading h2 {
  margin: 0 0 6px;
  font-size: 24px;
}

.leaderboard-heading p {
  margin: 0 0 22px;
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.metric-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 18px;
}

.metric-tab {
  padding: 8px 16px;
  border: 1px solid var(--el-border-color);
  border-radius: 999px;
  color: var(--el-text-color-regular);
  background: var(--el-bg-color);
  font: inherit;
  font-size: 13px;
  cursor: pointer;
  transition: color .2s, border-color .2s, background .2s;
}

.metric-tab:hover {
  color: var(--el-color-primary);
  border-color: var(--el-color-primary-light-5);
}

.metric-tab.active {
  color: var(--el-color-white);
  border-color: var(--el-color-primary);
  background: var(--el-color-primary);
  font-weight: 600;
}

.my-rank-card {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 16px;
  padding: 18px 20px;
  border: 1px solid var(--el-color-primary-light-7);
  border-radius: 14px;
  background: var(--el-color-primary-light-9);
}

.my-rank-icon {
  display: grid;
  width: 40px;
  height: 40px;
  flex: none;
  place-items: center;
  border-radius: 12px;
  color: var(--el-color-primary);
  background: var(--el-bg-color);
  font-size: 20px;
}

.my-rank-body {
  min-width: 0;
  flex: 1;
}

.my-rank-body strong {
  display: block;
  margin-bottom: 2px;
  font-size: 14px;
}

.my-rank-body p {
  margin: 0;
  color: var(--el-text-color-regular);
  font-size: 13px;
}

.my-rank-body p.muted {
  color: var(--el-text-color-placeholder);
}

.ranked-users {
  flex: none;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.board-card {
  border: 1px solid var(--el-border-color-light);
  border-radius: 14px;
  background: var(--el-bg-color);
  box-shadow: var(--el-box-shadow-lighter);
  overflow: hidden;
}

.board-header,
.board-row {
  display: grid;
  grid-template-columns: 64px 1fr auto;
  align-items: center;
  gap: 12px;
  padding: 13px 20px;
}

.board-header {
  border-bottom: 1px solid var(--el-border-color-lighter);
  color: var(--el-text-color-placeholder);
  background: var(--el-fill-color-lighter);
  font-size: 12px;
}

.board-row {
  border-bottom: 1px solid var(--el-border-color-lighter);
  font-size: 14px;
}

.board-row:last-child {
  border-bottom: 0;
}

.board-row.top .col-user {
  font-weight: 600;
}

.board-row.me {
  background: var(--el-color-primary-light-9);
}

.col-rank {
  display: inline-flex;
  align-items: center;
}

.col-user {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.col-value {
  color: var(--el-color-primary);
  font-weight: 600;
}

.rank-badge {
  display: inline-grid;
  width: 26px;
  height: 26px;
  place-items: center;
  border-radius: 8px;
  color: var(--el-text-color-secondary);
  background: var(--el-fill-color);
  font-size: 12px;
  font-weight: 600;
}

.rank-1 {
  color: #b8860b;
  background: #fdf3d3;
}

.rank-2 {
  color: #6b7280;
  background: #eef1f5;
}

.rank-3 {
  color: #a0522d;
  background: #f7ece2;
}

.empty-panel {
  display: grid;
  justify-items: center;
  gap: 10px;
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

.empty-icon {
  color: var(--el-text-color-placeholder);
  font-size: 40px;
}

.ghost-button {
  padding: 8px 16px;
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  color: var(--el-text-color-regular);
  background: var(--el-bg-color);
  font: inherit;
  font-size: 13px;
  cursor: pointer;
}

.ghost-button:hover {
  color: var(--el-color-primary);
  border-color: var(--el-color-primary-light-5);
}

.skeleton.row {
  height: 46px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  background: linear-gradient(90deg, var(--el-fill-color-light) 25%, var(--el-fill-color-lighter) 50%, var(--el-fill-color-light) 75%);
  background-size: 200% 100%;
  animation: skeleton-wave 1.4s ease infinite;
}

@keyframes skeleton-wave {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

@media (max-width: 520px) {
  .board-header,
  .board-row {
    grid-template-columns: 44px 1fr auto;
    gap: 8px;
    padding: 12px 14px;
  }
}
</style>
