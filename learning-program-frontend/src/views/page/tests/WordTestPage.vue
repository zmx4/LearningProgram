<script setup lang="ts">
import { ArrowLeft } from '@element-plus/icons-vue'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { get, post } from '@/net'

type Source = 'cet4' | 'cet6'

interface Word {
  id: number;
  word: string;
  translation: string | null
}

interface TestRecord {
  id: number;
  source: Source;
  totalCount: number;
  correctCount: number;
  wrongCount: number;
  score: number;
  durationSeconds: number;
  createdAt: string
}

interface History {
  summary: { testCount: number; averageScore: number; bestScore: number; totalQuestions: number; totalCorrect: number };
  records: TestRecord[]
}

const router = useRouter()
const {t} = useI18n()
const source = ref<Source>('cet4')
const words = ref<Word[]>([])
const current = ref(0)
const answers = ref<string[]>([])
const startedAt = ref(0)
const completed = ref(false)
const result = ref<TestRecord | null>(null)
const history = ref<History>({
  summary: {
    testCount: 0,
    averageScore: 0,
    bestScore: 0,
    totalQuestions: 0,
    totalCorrect: 0
  }, records: []
})
const loading = ref(false)

const optionCount = 4

function shuffle<T>(items: T[]): T[] {
  const result = [...items]
  for (let i = result.length - 1; i > 0; i -= 1) {
    const j = Math.floor(Math.random() * (i + 1))
    const swapped = result[i]
    result[i] = result[j]!
    result[j] = swapped!
  }
  return result
}

const options = computed(() => {
  const correct = words.value[current.value]?.translation
  if (!correct) return []

  // 干扰项只从本次测试的其他单词中抽取，且正确答案始终保留，
  // 否则选项会与当前题目对不上（甚至没有正确选项）。
  const distractors = [...new Set(
      words.value
          .filter((_, index) => index !== current.value)
          .map(word => word.translation)
          .filter((text): text is string => Boolean(text) && text !== correct),
  )]

  return shuffle([correct, ...shuffle(distractors).slice(0, optionCount - 1)])
})
const currentWord = computed(() => words.value[current.value])
const progress = computed(() => words.value.length ? `${current.value + 1}/${words.value.length}` : '')

function loadHistory() {
  get<History>('/api/tests/words/history', data => {
    history.value = data
  })
}

function startTest() {
  loading.value = true
  get<Word[]>(`/api/dictionary/${source.value}?count=10`, data => {
    // 没有释义的单词会生成一道没有任何选项的题目，直接跳过。
    words.value = data.filter(word => Boolean(word.translation))
    answers.value = []
    current.value = 0
    completed.value = false
    result.value = null
    startedAt.value = Date.now()
    loading.value = false
  }, () => {
    loading.value = false
  })
}

function choose(answer: string) {
  answers.value[current.value] = answer
  if (current.value < words.value.length - 1) current.value += 1
  else submitTest()
}

function submitTest() {
  const correctCount = words.value.reduce((count, word, index) => count + (answers.value[index] === word.translation ? 1 : 0), 0)
  post<TestRecord>('/api/tests/words/results', {
    source: source.value, totalCount: words.value.length, correctCount,
    durationSeconds: Math.floor((Date.now() - startedAt.value) / 1000),
  }, data => {
    result.value = data
    completed.value = true
    loadHistory()
  })
}

onMounted(loadHistory)
</script>

<template>
  <main class="tests-page">
    <button class="back-button" type="button" @click="router.push({ name: 'tests' })">
      <el-icon>
        <ArrowLeft/>
      </el-icon>
      {{ t('tests.back') }}
    </button>

    <section class="tests-heading">
      <p class="eyebrow">LEARNING TESTS</p>
      <h2>{{ t('tests.word.title') }}</h2>
      <p>{{ t('tests.word.description') }}</p>
    </section>

    <section v-if="!words.length || completed" class="test-card start-card">
      <h3>{{ completed ? t('tests.word.completed') : t('tests.word.ready') }}</h3>
      <p>{{ completed ? `${t('tests.word.score')}: ${result?.score}` : t('tests.word.description') }}</p>
      <div class="test-actions">
        <select v-model="source" :disabled="completed">
          <option value="cet4">CET4</option>
          <option value="cet6">CET6</option>
        </select>
        <button type="button" :disabled="loading" @click="startTest">
          {{ completed ? t('tests.word.tryAgain') : t('tests.word.start') }}
        </button>
      </div>
    </section>

    <section v-else class="test-card question-card">
      <div class="question-meta"><span>{{ source.toUpperCase() }}</span><strong>{{ progress }}</strong></div>
      <h3>{{ currentWord?.word }}</h3>
      <p>{{ t('tests.word.chooseTranslation') }}</p>
      <div class="answer-grid">
        <button v-for="option in options" :key="option" type="button" @click="choose(option)">{{ option }}</button>
      </div>
    </section>

    <section class="test-card">
      <div class="history-heading"><h3>{{ t('tests.word.history') }}</h3><span>{{
          t('tests.word.testCount')
        }} {{ history.summary.testCount }}</span></div>
      <div class="test-summary">
        <span>{{ t('tests.word.average') }} <strong>{{ history.summary.averageScore.toFixed(1) }}</strong></span>
        <span>{{ t('tests.word.best') }} <strong>{{ history.summary.bestScore }}</strong></span>
        <span>{{
            t('tests.word.accuracy')
          }} <strong>{{
              history.summary.totalQuestions ? Math.round(history.summary.totalCorrect / history.summary.totalQuestions * 100) : 0
            }}%</strong></span>
      </div>
      <div class="history-table">
        <table>
          <thead>
          <tr>
            <th>{{ t('tests.word.date') }}</th>
            <th>{{ t('tests.word.source') }}</th>
            <th>{{ t('tests.word.correct') }}</th>
            <th>{{ t('tests.word.score') }}</th>
            <th>{{ t('tests.word.duration') }}</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="item in history.records" :key="item.id">
            <td>{{ item.createdAt?.replace('T', ' ').slice(0, 16) }}</td>
            <td>{{ item.source.toUpperCase() }}</td>
            <td>{{ item.correctCount }}/{{ item.totalCount }}</td>
            <td>{{ item.score }}</td>
            <td>{{ item.durationSeconds }}s</td>
          </tr>
          </tbody>
        </table>
        <p v-if="!history.records.length" class="empty-history">{{ t('tests.word.noHistory') }}</p>
      </div>
    </section>
  </main>
</template>

<style scoped>
.tests-page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 42px 5% 60px;
}

.back-button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 28px;
  padding: 0;
  border: 0;
  color: var(--el-text-color-secondary);
  background: transparent;
  font: inherit;
  font-size: 13px;
  cursor: pointer;
}

.back-button:hover {
  color: var(--el-color-primary);
}

.eyebrow {
  margin: 0 0 8px;
  color: var(--el-text-color-placeholder);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: .13em;
}

.tests-heading h2 {
  margin: 0 0 8px;
  font-size: 26px;
}

.tests-heading > p:last-child {
  margin: 0 0 28px;
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.test-card {
  margin-bottom: 18px;
  padding: 26px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 16px;
  background: var(--el-bg-color);
  box-shadow: var(--el-box-shadow-lighter);
}

.start-card h3, .question-card h3 {
  margin: 0 0 8px;
  font-size: 22px;
}

.start-card p, .question-card p {
  color: var(--el-text-color-secondary);
}

.test-actions {
  display: flex;
  gap: 12px;
  margin-top: 20px;
}

.test-actions select, .test-actions button {
  padding: 10px 14px;
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  background: var(--el-bg-color);
}

.test-actions button, .answer-grid button {
  color: var(--el-color-white);
  background: var(--el-color-primary);
  cursor: pointer;
}

.question-meta, .history-heading, .test-summary {
  display: flex;
  justify-content: space-between;
  gap: 14px;
}

.question-meta {
  color: var(--el-color-primary);
  font-size: 13px;
}

.question-card h3 {
  margin-top: 30px;
  font-size: 32px;
}

.answer-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.answer-grid button {
  min-height: 48px;
  border: 0;
  border-radius: 9px;
  font: inherit;
}

.history-heading h3 {
  margin: 0;
}

.history-heading span, .test-summary {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.test-summary {
  justify-content: flex-start;
  margin: 22px 0;
}

.test-summary strong {
  margin-left: 4px;
  color: var(--el-color-primary);
  font-size: 20px;
}

.history-table {
  overflow-x: auto;
}

.history-table table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
}

.history-table th, .history-table td {
  padding: 10px 8px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  text-align: left;
  white-space: nowrap;
}

.history-table th {
  color: var(--el-text-color-placeholder);
}

.empty-history {
  color: var(--el-text-color-placeholder);
  text-align: center;
}

@media (max-width: 600px) {
  .answer-grid {
    grid-template-columns: 1fr;
  }

  .test-summary {
    flex-wrap: wrap;
  }
}
</style>
