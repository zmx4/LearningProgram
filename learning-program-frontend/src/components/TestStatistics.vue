<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

interface RecordItem {
  score: number;
  correctCount: number;
  wrongCount: number;
  createdAt: string
}

const props = defineProps<{ records: RecordItem[] }>()
const { t } = useI18n()

// 最近 10 次记录，按时间正序排列（从左到右）
const recent = computed(() => [...props.records].slice(0, 10).reverse())

function scoreTone(score: number) {
  if (score >= 80) return 'good'
  if (score >= 60) return 'mid'
  return 'bad'
}

const totals = computed(() => {
  const correct = props.records.reduce((sum, item) => sum + item.correctCount, 0)
  const wrong = props.records.reduce((sum, item) => sum + item.wrongCount, 0)
  const total = correct + wrong
  return { correct, wrong, rate: total ? Math.round(correct / total * 100) : 0 }
})
</script>

<template>
  <section class="stats-card">
    <div class="stats-heading">
      <h3>{{ t('tests.stats.title') }}</h3>
    </div>
    <template v-if="records.length">
      <div class="stats-body">
        <div class="trend-block">
          <p class="stats-label">{{ t('tests.stats.trend') }}</p>
          <div class="trend">
            <div v-for="(item, index) in recent" :key="index" class="trend-col">
              <span class="trend-score">{{ item.score }}</span>
              <div class="trend-track">
                <div class="trend-bar" :class="scoreTone(item.score)" :style="{ height: Math.max(item.score, 3) + '%' }"></div>
              </div>
              <span class="trend-date">{{ item.createdAt?.replace('T', ' ').slice(5, 10) }}</span>
            </div>
          </div>
        </div>
        <div class="donut-block">
          <p class="stats-label">{{ t('tests.stats.accuracy') }}</p>
          <div
              class="donut"
              :style="{ background: `conic-gradient(var(--el-color-success) 0 ${totals.rate}%, var(--el-color-danger) ${totals.rate}% 100%)` }"
          >
            <div class="donut-hole">
              <strong>{{ totals.rate }}%</strong>
            </div>
          </div>
          <div class="donut-legend">
            <span class="legend-right">{{ t('tests.stats.correct') }} {{ totals.correct }}</span>
            <span class="legend-wrong">{{ t('tests.stats.wrong') }} {{ totals.wrong }}</span>
          </div>
        </div>
      </div>
    </template>
    <p v-else class="empty-stats">{{ t('tests.stats.noData') }}</p>
  </section>
</template>

<style scoped>
.stats-card {
  margin-bottom: 18px;
  padding: 26px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 16px;
  background: var(--el-bg-color);
  box-shadow: var(--el-box-shadow-lighter);
}

.stats-heading h3 {
  margin: 0;
}

.stats-body {
  display: flex;
  gap: 28px;
  margin-top: 18px;
}

.trend-block {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
}

.stats-label {
  margin: 0 0 12px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.trend {
  display: flex;
  flex: 1;
  gap: 10px;
  min-height: 170px;
}

.trend-col {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.trend-score {
  color: var(--el-text-color-secondary);
  font-size: 11px;
}

.trend-track {
  display: flex;
  flex: 1;
  align-items: flex-end;
  justify-content: center;
  width: 100%;
}

.trend-bar {
  width: 100%;
  max-width: 34px;
  border-radius: 6px 6px 3px 3px;
  transition: height .3s ease;
}

.trend-bar.good {
  background: var(--el-color-success);
}

.trend-bar.mid {
  background: var(--el-color-warning);
}

.trend-bar.bad {
  background: var(--el-color-danger);
}

.trend-date {
  max-width: 100%;
  overflow: hidden;
  color: var(--el-text-color-placeholder);
  font-size: 10px;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.donut-block {
  display: flex;
  flex: 0 0 190px;
  flex-direction: column;
  align-items: center;
}

.donut {
  display: grid;
  width: 132px;
  height: 132px;
  place-items: center;
  border-radius: 50%;
}

.donut-hole {
  display: grid;
  width: 96px;
  height: 96px;
  place-items: center;
  border-radius: 50%;
  background: var(--el-bg-color);
}

.donut-hole strong {
  color: var(--el-color-primary);
  font-size: 24px;
}

.donut-legend {
  display: flex;
  gap: 14px;
  margin-top: 12px;
  font-size: 12px;
}

.legend-right {
  color: var(--el-color-success);
}

.legend-wrong {
  color: var(--el-color-danger);
}

.empty-stats {
  margin: 8px 0 0;
  color: var(--el-text-color-placeholder);
  text-align: center;
}

@media (max-width: 700px) {
  .stats-body {
    flex-direction: column;
  }

  .donut-block {
    flex-basis: auto;
  }
}
</style>
