<script setup lang="ts">
import { ArrowLeft } from '@element-plus/icons-vue'
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { get, post } from '@/net'

type QuestionKind = 'single' | 'multiple' | 'blank'

interface QuestionOption {
  key: string;
  text: string
}

interface QuestionContent {
  stem: string;
  options: QuestionOption[] | null;
  answer: string[] | null;
  analysis: string | null
}

interface TestQuestion {
  id: number;
  typeId: number;
  kind: QuestionKind;
  content: QuestionContent
}

interface TestType {
  id: number;
  code: string;
  name: string;
  description: string | null
}

interface AnswerDetail {
  questionId: number;
  kind: QuestionKind;
  userAnswer: string[];
  correct: boolean
}

interface TestRecord {
  id: number;
  typeId: number;
  typeName: string;
  totalCount: number;
  correctCount: number;
  wrongCount: number;
  score: number;
  durationSeconds: number;
  detail: AnswerDetail[] | null;
  createdAt: string
}

interface History {
  summary: { testCount: number; averageScore: number; bestScore: number; totalQuestions: number; totalCorrect: number };
  records: TestRecord[]
}

const countOptions = [5, 10, 20]

const router = useRouter()
const {t} = useI18n()

const types = ref<TestType[]>([])
const typeId = ref<number | null>(null)
const questionCount = ref(10)
const questions = ref<TestQuestion[]>([])
const current = ref(0)
// answers 按题目下标存放，选择题存选中的 key，填空题按空格顺序存原文
const answers = ref<string[][]>([])
const blankAnswers = ref<string[]>([])
const startedAt = ref(0)
const submitting = ref(false)
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

const currentQuestion = computed(() => questions.value[current.value])
const currentAnswer = computed(() => answers.value[current.value] ?? [])
const typeName = computed(() => types.value.find(item => item.id === typeId.value)?.name ?? '')
const progress = computed(() => questions.value.length ? `${current.value + 1}/${questions.value.length}` : '')

function kindLabel(kind: QuestionKind) {
  if (kind === 'single') return t('tests.knowledge.kinds.single')
  if (kind === 'multiple') return t('tests.knowledge.kinds.multiple')
  return t('tests.knowledge.kinds.blank')
}

// 填空题题干用下划线占位，拆开与输入框交错渲染；拆分数量对不上时回退为整段展示
const stemParts = computed(() => {
  const question = currentQuestion.value
  if (!question || question.kind !== 'blank') return []
  return question.content.stem.split(/_{2,}/)
})

const inlineBlanks = computed(() => {
  const question = currentQuestion.value
  if (!question) return false
  return stemParts.value.length - 1 === question.content.answer?.length && stemParts.value.length > 1
})

const canAdvance = computed(() => {
  const question = currentQuestion.value
  if (!question) return false
  const answer = answers.value[current.value] ?? []
  if (question.kind === 'single') return answer.length === 1
  if (question.kind === 'multiple') return answer.length >= 1
  return (question.content.answer?.length ?? 0) === answer.length
      && blankAnswers.value.every(item => item.trim().length > 0)
})

watch(current, index => {
  const question = questions.value[index]
  if (question?.kind === 'blank') blankAnswers.value = answers.value[index] ?? []
})

function loadTypes() {
  get<TestType[]>('/api/test-types', data => {
    types.value = data
  })
}

function loadHistory() {
  get<History>('/api/tests/knowledge/history', data => {
    history.value = data
  })
}

function startTest() {
  if (!typeId.value) return
  loading.value = true
  get<TestQuestion[]>(`/api/test-questions?typeId=${typeId.value}&count=${questionCount.value}`, data => {
    loading.value = false
    if (!data.length) {
      ElMessage.warning(t('tests.knowledge.noQuestions'))
      return
    }
    questions.value = data
    answers.value = data.map(question => question.kind === 'blank'
        ? Array.from({length: question.content.answer?.length ?? 0}, () => '')
        : [])
    blankAnswers.value = answers.value[0] ?? []
    current.value = 0
    completed.value = false
    result.value = null
    startedAt.value = Date.now()
  }, () => {
    loading.value = false
  })
}

function chooseOption(key: string) {
  const question = currentQuestion.value
  if (!question) return
  if (question.kind === 'single') {
    answers.value[current.value] = [key]
    return
  }
  const selected = [...(answers.value[current.value] ?? [])]
  const index = selected.indexOf(key)
  if (index >= 0) selected.splice(index, 1)
  else selected.push(key)
  answers.value[current.value] = selected
}

function setBlankAnswer(index: number, event: Event) {
  blankAnswers.value[index] = (event.target as HTMLInputElement).value
}

function advance() {
  if (!canAdvance.value || submitting.value) return
  if (current.value < questions.value.length - 1) current.value += 1
  else submitTest()
}

function checkCorrect(question: TestQuestion, answer: string[]): boolean {
  const expected = question.content.answer ?? []
  if (question.kind === 'single') {
    return expected.length === 1 && answer.length === 1 && answer[0] === expected[0]
  }
  if (question.kind === 'multiple') {
    return expected.length === answer.length
        && [...answer].sort().join() === [...expected].sort().join()
  }
  return expected.length === answer.length
      && expected.every((item, index) => item.trim().toLowerCase() === (answer[index] ?? '').trim().toLowerCase())
}

// 交卷后基于本地题目与作答生成回顾列表，正确答案与解析直接来自题目内容
const reviewItems = computed(() => {
  if (!completed.value) return []
  return questions.value.map((question, index) => {
    const userAnswer = answers.value[index] ?? []
    return { question, userAnswer, correct: checkCorrect(question, userAnswer) }
  })
})

function optionText(question: TestQuestion, key: string): string {
  const option = question.content.options?.find(item => item.key === key)
  return option ? `${key}. ${option.text}` : key
}

function displayAnswer(question: TestQuestion, keys: string[]): string {
  const values = keys.map(item => item.trim()).filter(item => item.length > 0)
  if (!values.length) return '—'
  if (question.kind === 'blank') return values.join('、')
  return values.map(key => optionText(question, key)).join('、')
}

function submitTest() {
  submitting.value = true
  const detail: AnswerDetail[] = questions.value.map((question, index) => {
    const answer = answers.value[index] ?? []
    return {
      questionId: question.id,
      kind: question.kind,
      userAnswer: answer.map(item => item.trim()).filter(item => item.length > 0),
      correct: checkCorrect(question, answer),
    }
  })
  const correctCount = detail.filter(item => item.correct).length
  post<TestRecord>('/api/tests/knowledge/results', {
    typeId: typeId.value,
    totalCount: questions.value.length,
    correctCount,
    durationSeconds: Math.floor((Date.now() - startedAt.value) / 1000),
    detail,
  }, data => {
    result.value = data
    completed.value = true
    submitting.value = false
    loadHistory()
  }, () => {
    submitting.value = false
  })
}

onMounted(() => {
  loadTypes()
  loadHistory()
})
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
      <h2>{{ t('tests.knowledge.title') }}</h2>
      <p>{{ t('tests.knowledge.description') }}</p>
    </section>

    <section v-if="!questions.length || completed" class="test-card start-card">
      <template v-if="completed && result">
        <h3>{{ t('tests.knowledge.completed') }}</h3>
        <div class="result-score">
          <strong>{{ result.score }}</strong>
          <span>{{ t('tests.knowledge.score') }}</span>
        </div>
        <div class="test-summary">
          <span>{{ t('tests.knowledge.correctCount') }} <strong>{{ result.correctCount }}</strong></span>
          <span>{{ t('tests.knowledge.wrongCount') }} <strong>{{ result.wrongCount }}</strong></span>
          <span>{{ t('tests.knowledge.duration') }} <strong>{{ result.durationSeconds }}s</strong></span>
        </div>
      </template>
      <template v-else>
        <h3>{{ t('tests.knowledge.ready') }}</h3>
        <p>{{ t('tests.knowledge.description') }}</p>
      </template>
      <div v-if="types.length" class="test-actions">
        <select v-model="typeId" :disabled="completed" :aria-label="t('tests.knowledge.type')">
          <option :value="null" disabled>{{ t('tests.knowledge.type') }}</option>
          <option v-for="item in types" :key="item.id" :value="item.id">{{ item.name }}</option>
        </select>
        <select v-model.number="questionCount" :disabled="completed" :aria-label="t('tests.knowledge.questionCount')">
          <option v-for="count in countOptions" :key="count" :value="count">{{ count }}</option>
        </select>
        <button type="button" :disabled="loading || !typeId" @click="startTest">
          {{ completed ? t('tests.knowledge.tryAgain') : t('tests.knowledge.start') }}
        </button>
      </div>
      <p v-else class="empty-types">{{ t('tests.knowledge.noTypes') }}</p>
    </section>

    <section v-else-if="currentQuestion" class="test-card question-card">
      <div class="question-meta">
        <span>{{ typeName }}</span>
        <span>{{ kindLabel(currentQuestion.kind) }}
          <em v-if="currentQuestion.kind === 'multiple'">{{ t('tests.knowledge.multipleHint') }}</em>
        </span>
        <strong>{{ progress }}</strong>
      </div>
      <h3 v-if="currentQuestion.kind !== 'blank'">{{ currentQuestion.content.stem }}</h3>
      <p v-else class="blank-stem">
        <template v-for="(part, index) in (inlineBlanks ? stemParts : [currentQuestion.content.stem])" :key="index">
          <span>{{ part }}</span>
          <input
              v-if="inlineBlanks && index < stemParts.length - 1"
              class="blank-input"
              :value="blankAnswers[index] ?? ''"
              :placeholder="t('tests.knowledge.blankPlaceholder')"
              @input="setBlankAnswer(index, $event)"
              @keyup.enter="advance"
          >
        </template>
      </p>
      <div v-if="currentQuestion.kind !== 'blank'" class="answer-grid">
        <button
            v-for="option in currentQuestion.content.options ?? []"
            :key="option.key"
            type="button"
            class="option-button"
            :class="{ active: currentAnswer.includes(option.key) }"
            @click="chooseOption(option.key)"
        >{{ option.key }}. {{ option.text }}</button>
      </div>
      <div class="question-actions">
        <button type="button" :disabled="!canAdvance || submitting" @click="advance">
          {{ current < questions.length - 1 ? t('tests.knowledge.next') : t('tests.knowledge.submit') }}
        </button>
      </div>
    </section>

    <section v-if="completed && reviewItems.length" class="test-card review-card">
      <div class="history-heading">
        <h3>{{ t('tests.knowledge.review') }}</h3>
        <span>{{ t('tests.knowledge.reviewHint') }}</span>
      </div>
      <article v-for="(item, index) in reviewItems" :key="item.question.id" class="review-item">
        <div class="review-item-head">
          <span class="review-no" :class="item.correct ? 'right' : 'wrong'">{{ index + 1 }}</span>
          <span class="review-kind">{{ kindLabel(item.question.kind) }}</span>
          <span class="review-badge" :class="item.correct ? 'right' : 'wrong'">{{
              item.correct ? t('tests.knowledge.tagRight') : t('tests.knowledge.tagWrong')
            }}</span>
        </div>
        <p class="review-stem">{{ item.question.content.stem }}</p>
        <div class="review-answers">
          <p class="review-line" :class="item.correct ? 'right' : 'wrong'">
            <span class="review-label">{{ t('tests.knowledge.yourAnswer') }}</span>
            {{ displayAnswer(item.question, item.userAnswer) }}
          </p>
          <p class="review-line right">
            <span class="review-label">{{ t('tests.knowledge.correctAnswer') }}</span>
            {{ displayAnswer(item.question, item.question.content.answer ?? []) }}
          </p>
        </div>
        <p v-if="item.question.content.analysis" class="review-analysis">
          <span class="review-label">{{ t('tests.knowledge.analysis') }}</span>
          {{ item.question.content.analysis }}
        </p>
      </article>
    </section>

    <section class="test-card">
      <div class="history-heading"><h3>{{ t('tests.knowledge.history') }}</h3><span>{{
          t('tests.knowledge.testCount')
        }} {{ history.summary.testCount }}</span></div>
      <div class="test-summary">
        <span>{{ t('tests.knowledge.average') }} <strong>{{ history.summary.averageScore.toFixed(1) }}</strong></span>
        <span>{{ t('tests.knowledge.best') }} <strong>{{ history.summary.bestScore }}</strong></span>
        <span>{{
            t('tests.knowledge.accuracy')
          }} <strong>{{
              history.summary.totalQuestions ? Math.round(history.summary.totalCorrect / history.summary.totalQuestions * 100) : 0
            }}%</strong></span>
      </div>
      <div class="history-table">
        <table>
          <thead>
          <tr>
            <th>{{ t('tests.knowledge.date') }}</th>
            <th>{{ t('tests.knowledge.typeName') }}</th>
            <th>{{ t('tests.knowledge.correct') }}</th>
            <th>{{ t('tests.knowledge.score') }}</th>
            <th>{{ t('tests.knowledge.duration') }}</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="item in history.records" :key="item.id">
            <td>{{ item.createdAt?.replace('T', ' ').slice(0, 16) }}</td>
            <td>{{ item.typeName }}</td>
            <td>{{ item.correctCount }}/{{ item.totalCount }}</td>
            <td>{{ item.score }}</td>
            <td>{{ item.durationSeconds }}s</td>
          </tr>
          </tbody>
        </table>
        <p v-if="!history.records.length" class="empty-history">{{ t('tests.knowledge.noHistory') }}</p>
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

.start-card > p, .question-card p {
  color: var(--el-text-color-secondary);
}

.test-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 20px;
}

.test-actions select, .test-actions button, .question-actions button {
  padding: 10px 14px;
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  background: var(--el-bg-color);
  font: inherit;
}

.test-actions button, .question-actions button {
  color: var(--el-color-white);
  background: var(--el-color-primary);
  border-color: var(--el-color-primary);
  cursor: pointer;
}

.test-actions button:disabled, .question-actions button:disabled {
  opacity: .55;
  cursor: not-allowed;
}

.empty-types {
  margin: 20px 0 0;
  color: var(--el-text-color-placeholder);
  font-size: 14px;
}

.question-meta {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  color: var(--el-color-primary);
  font-size: 13px;
}

.question-meta em {
  margin-left: 4px;
  color: var(--el-text-color-placeholder);
  font-size: 12px;
  font-style: normal;
}

.question-card h3 {
  margin-top: 30px;
  font-size: 22px;
  line-height: 1.6;
}

.blank-stem {
  margin-top: 30px;
  color: var(--el-text-color-primary);
  font-size: 20px;
  line-height: 2.4;
}

.blank-input {
  width: 9em;
  margin: 0 4px;
  padding: 4px 8px;
  border: 0;
  border-bottom: 2px solid var(--el-color-primary);
  border-radius: 6px 6px 0 0;
  background: var(--el-fill-color-light);
  font: inherit;
  text-align: center;
  outline: none;
}

.blank-input:focus {
  background: var(--el-color-primary-light-9);
}

.answer-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin-top: 24px;
}

.option-button {
  min-height: 48px;
  padding: 12px 16px;
  border: 1px solid var(--el-border-color);
  border-radius: 9px;
  background: var(--el-bg-color);
  font: inherit;
  text-align: left;
  cursor: pointer;
  transition: border-color .2s ease, background-color .2s ease;
}

.option-button:hover {
  border-color: var(--el-color-primary-light-5);
}

.option-button.active {
  border-color: var(--el-color-primary);
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  font-weight: 600;
}

.question-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 24px;
}

.result-score {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin: 8px 0;
}

.result-score strong {
  color: var(--el-color-primary);
  font-size: 46px;
  line-height: 1;
}

.history-heading {
  display: flex;
  justify-content: space-between;
  gap: 14px;
}

.history-heading h3 {
  margin: 0;
}

.history-heading span, .test-summary {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.test-summary {
  display: flex;
  justify-content: flex-start;
  gap: 14px;
  flex-wrap: wrap;
  margin: 22px 0;
}

.test-summary strong {
  margin-left: 4px;
  color: var(--el-color-primary);
  font-size: 20px;
}

.review-card {
  padding-top: 22px;
  padding-bottom: 8px;
}

.review-item {
  padding: 18px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.review-item:last-child {
  border-bottom: 0;
}

.review-item-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.review-no {
  display: grid;
  width: 24px;
  height: 24px;
  place-items: center;
  border-radius: 50%;
  color: var(--el-color-white);
  font-size: 12px;
  font-weight: 600;
}

.review-no.right {
  background: var(--el-color-success);
}

.review-no.wrong {
  background: var(--el-color-danger);
}

.review-kind {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.review-badge {
  margin-left: auto;
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
}

.review-badge.right {
  color: var(--el-color-success);
  background: var(--el-color-success-light-9);
}

.review-badge.wrong {
  color: var(--el-color-danger);
  background: var(--el-color-danger-light-9);
}

.review-stem {
  margin: 10px 0 8px;
  font-size: 15px;
  font-weight: 600;
  line-height: 1.7;
}

.review-line {
  margin: 4px 0;
  font-size: 13px;
  line-height: 1.6;
}

.review-line.right {
  color: var(--el-color-success);
}

.review-line.wrong {
  color: var(--el-color-danger);
}

.review-label {
  margin-right: 8px;
  color: var(--el-text-color-secondary);
  font-weight: 400;
}

.review-analysis {
  margin: 10px 0 0;
  padding: 10px 12px;
  border-radius: 8px;
  color: var(--el-text-color-secondary);
  background: var(--el-fill-color-light);
  font-size: 13px;
  line-height: 1.6;
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
}
</style>
