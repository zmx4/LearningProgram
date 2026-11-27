<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Trophy, UserFilled } from '@element-plus/icons-vue'
import { get } from '@/net'
import type { GroupSummary } from '@/types/group'

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

type Scope = 'global' | 'group'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()

function parseGroupId(value: unknown): number | null {
  const raw = Array.isArray(value) ? value[0] : value
  const id = Number(raw)
  return Number.isInteger(id) && id > 0 ? id : null
}

const initialGroupId = parseGroupId(route.query.groupId)
const scope = ref<Scope>(initialGroupId == null ? 'global' : 'group')
const activeGroupId = ref<number | null>(initialGroupId)
const groups = ref<GroupSummary[]>([])
const groupsLoading = ref(false)
const metrics = ref<MetricOption[]>([])
const activeMetric = ref('points')
const board = ref<Leaderboard | null>(null)
const loading = ref(true)
const loadFailed = ref(false)
let disposed = false

const selectedGroup = computed(() => groups.value.find(group => group.id === activeGroupId.value) ?? null)
const pageTitle = computed(() => scope.value === 'group' && selectedGroup.value
    ? t('leaderboard.groupTitle', { name: selectedGroup.value.name })
    : t('leaderboard.title'))

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
  if (scope.value === 'group' && activeGroupId.value == null) {
    board.value = null
    loading.value = false
    return
  }
  loading.value = true
  loadFailed.value = false
  const base = scope.value === 'group'
      ? `/api/groups/${activeGroupId.value}/leaderboard`
      : '/api/leaderboard'
  get<Leaderboard>(`${base}?metric=${encodeURIComponent(metric)}&limit=20`, data => {
    if (disposed) return
    board.value = data
    loading.value = false
  }, () => {
    if (disposed) return
    loading.value = false
    loadFailed.value = true
  })
}

function selectMetric(metric: string) {
  if (metric === activeMetric.value && board.value) return
  activeMetric.value = metric
  loadBoard(metric)
}

function updateRouteQuery() {
  const query = scope.value === 'group' && activeGroupId.value != null
      ? { groupId: String(activeGroupId.value) }
      : {}
  void router.replace({ query })
}

function selectScope(next: Scope) {
  if (scope.value === next) return
  scope.value = next
  if (next === 'group' && activeGroupId.value == null) {
    activeGroupId.value = groups.value[0]?.id ?? null
  }
  updateRouteQuery()
  loadBoard(activeMetric.value)
}

function selectGroup(groupId: number) {
  activeGroupId.value = groupId
  scope.value = 'group'
  updateRouteQuery()
  loadBoard(activeMetric.value)
}

function loadMetrics() {
  loading.value = true
  get<MetricOption[]>('/api/leaderboard/metrics', data => {
    if (disposed) return
    metrics.value = data
    const first = data[0]?.code ?? 'points'
    activeMetric.value = first
    loadBoard(first)
  }, () => {
    if (disposed) return
    loading.value = false
    loadFailed.value = true
  })
}

function loadGroups() {
  groupsLoading.value = true
  get<GroupSummary[]>('/api/groups', data => {
    if (disposed) return
    groups.value = data
    groupsLoading.value = false
    if (scope.value === 'group' && activeGroupId.value == null && data.length) {
      activeGroupId.value = data[0]?.id ?? null
      updateRouteQuery()
      if (metrics.value.length) loadBoard(activeMetric.value)
    }
  }, () => {
    if (!disposed) groupsLoading.value = false
  })
}

watch(() => route.query.groupId, value => {
  const groupId = parseGroupId(value)
  if (groupId == null) {
    if (scope.value !== 'global') {
      scope.value = 'global'
      loadBoard(activeMetric.value)
    }
    return
  }
  if (scope.value !== 'group' || activeGroupId.value !== groupId) {
    scope.value = 'group'
    activeGroupId.value = groupId
    loadBoard(activeMetric.value)
  }
})

const rankedLabel = computed(() => t('leaderboard.rankedUsers', { count: board.value?.rankedUsers ?? 0 }))

onMounted(() => {
  loadGroups()
  loadMetrics()
})

onUnmounted(() => {
  disposed = true
})
</script>

<template>
  <main class="leaderboard-page">
    <section class="leaderboard-heading">
      <p class="eyebrow">LEADERBOARD</p>
      <h2>{{ pageTitle }}</h2>
      <p>{{ t('leaderboard.description') }}</p>
    </section>

    <div class="scope-tabs" role="tablist" :aria-label="t('leaderboard.scopeLabel')">
      <button
          class="scope-tab"
          :class="{ active: scope === 'global' }"
          type="button"
          role="tab"
          :aria-selected="scope === 'global'"
          @click="selectScope('global')"
      >
        {{ t('leaderboard.scopeGlobal') }}
      </button>
      <button
          class="scope-tab"
          :class="{ active: scope === 'group' }"
          type="button"
          role="tab"
          :aria-selected="scope === 'group'"
          @click="selectScope('group')"
      >
        {{ t('leaderboard.scopeGroup') }}
      </button>
    </div>

    <label v-if="scope === 'group'" class="group-select">
      <span>{{ t('leaderboard.groupLabel') }}</span>
      <select v-model="activeGroupId" :disabled="groupsLoading || !groups.length" @change="selectGroup(Number(activeGroupId))">
        <option v-if="groupsLoading" :value="null">{{ t('leaderboard.groupsLoading') }}</option>
        <option v-else-if="!groups.length" :value="null">{{ t('leaderboard.noGroups') }}</option>
        <option v-for="group in groups" :key="group.id" :value="group.id">{{ group.name }}</option>
      </select>
    </label>

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
      <button class="ghost-button" type="button" @click="loadBoard(activeMetric)">{{ t('resources.retry') }}</button>
    </section>

    <section v-else-if="scope === 'group' && !groups.length" class="empty-panel">
      <span class="empty-icon"><el-icon><Trophy /></el-icon></span>
      <h3>{{ t('leaderboard.noGroups') }}</h3>
      <button class="ghost-button" type="button" @click="router.push({ name: 'groups' })">{{ t('leaderboard.goGroups') }}</button>
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
  max-width: 1180px;
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

.scope-tabs,
.metric-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
}

.scope-tab,
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

.scope-tab:hover,
.metric-tab:hover {
  border-color: var(--el-color-primary-light-5);
  color: var(--el-color-primary);
}

.scope-tab.active,
.metric-tab.active {
  border-color: var(--el-color-primary);
  color: var(--el-color-white);
  background: var(--el-color-primary);
}

.group-select {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.group-select select {
  min-width: 220px;
  padding: 8px 10px;
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  color: var(--el-text-color-primary);
  background: var(--el-bg-color);
  font: inherit;
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
  width: 42px;
  height: 42px;
  flex: 0 0 42px;
  place-items: center;
  border-radius: 50%;
  color: var(--el-color-primary);
  background: var(--el-bg-color);
  font-size: 20px;
}

.my-rank-body {
  min-width: 0;
  flex: 1;
}

.my-rank-body p {
  margin: 4px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.ranked-users {
  color: var(--el-text-color-placeholder);
  font-size: 12px;
}

.board-card {
  overflow: hidden;
  border: 1px solid var(--el-border-color-light);
  border-radius: 14px;
  background: var(--el-bg-color);
}

.board-header,
.board-row {
  display: grid;
  grid-template-columns: 80px minmax(0, 1fr) 160px;
  align-items: center;
  gap: 12px;
  padding: 13px 20px;
}

.board-header {
  color: var(--el-text-color-placeholder);
  background: var(--el-fill-color-lighter);
  font-size: 12px;
}

.board-row {
  border-top: 1px solid var(--el-border-color-lighter);
  font-size: 14px;
}

.board-row.me {
  background: var(--el-color-primary-light-9);
}

.col-value {
  text-align: right;
}

.rank-badge {
  display: inline-grid;
  width: 26px;
  height: 26px;
  place-items: center;
  border-radius: 50%;
  color: var(--el-text-color-secondary);
  background: var(--el-fill-color-light);
  font-size: 12px;
  font-weight: 700;
}

.rank-badge.rank-1 { color: var(--el-color-warning-dark-2); background: var(--el-color-warning-light-9); }
.rank-badge.rank-2 { color: var(--el-text-color-secondary); background: var(--el-fill-color-dark-1); }
.rank-badge.rank-3 { color: var(--el-color-danger-dark-2); background: var(--el-color-danger-light-9); }

.empty-panel {
  display: grid;
  justify-items: center;
  gap: 12px;
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
  border-color: var(--el-color-primary-light-5);
  color: var(--el-color-primary);
}

.skeleton.row {
  height: 42px;
  margin: 10px 16px;
  border-radius: 8px;
}

.skeleton {
  background: linear-gradient(90deg, var(--el-fill-color-light) 25%, var(--el-fill-color-lighter) 50%, var(--el-fill-color-light) 75%);
  background-size: 200% 100%;
  animation: skeleton-wave 1.4s ease infinite;
}

@keyframes skeleton-wave {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

@media (max-width: 700px) {
  .leaderboard-page { padding: 28px 4% 48px; }
  .my-rank-card { align-items: flex-start; flex-wrap: wrap; }
  .board-header,
  .board-row { grid-template-columns: 52px minmax(0, 1fr) 100px; padding: 12px 14px; }
  .group-select { align-items: flex-start; flex-direction: column; }
  .group-select select { width: 100%; }
}
</style>
