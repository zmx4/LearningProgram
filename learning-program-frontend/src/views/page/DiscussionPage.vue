<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { ChatDotRound, ChatLineSquare, Promotion } from '@element-plus/icons-vue'
import { get, post } from '@/net'

interface PostSummary {
  id: number
  title: string
  summary: string
  authorId: number
  authorName: string
  commentCount: number
  createdAt: string
}

interface PostPage {
  items: PostSummary[]
  total: number
  page: number
  size: number
}

const router = useRouter()
const { t } = useI18n()

const posts = ref<PostSummary[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const loading = ref(true)
const loadFailed = ref(false)
const submitting = ref(false)
const form = ref({ title: '', content: '' })

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / size.value)))

function loadPosts(target = page.value) {
  loading.value = true
  loadFailed.value = false
  get<PostPage>(`/api/discussions?page=${target}&size=${size.value}`, data => {
    posts.value = data.items
    total.value = data.total
    page.value = data.page
    size.value = data.size
    loading.value = false
  }, () => {
    loading.value = false
    loadFailed.value = true
  })
}

function submitPost() {
  const title = form.value.title.trim()
  const content = form.value.content.trim()
  if (!title) {
    ElMessage.warning(t('discussions.titleRequired'))
    return
  }
  if (!content) {
    ElMessage.warning(t('discussions.contentRequired'))
    return
  }

  submitting.value = true
  post<PostSummary>('/api/discussions', { title, content }, () => {
    submitting.value = false
    form.value = { title: '', content: '' }
    page.value = 1
    loadPosts(1)
    ElMessage.success(t('discussions.submit'))
  }, () => {
    submitting.value = false
  })
}

function goTo(target: number) {
  if (target < 1 || target > totalPages.value || target === page.value) return
  loadPosts(target)
}

function openPost(item: PostSummary) {
  void router.push({ name: 'discussion-detail', params: { id: item.id } })
}

function formatDate(value: string) {
  if (!value) return ''
  return value.replace('T', ' ').slice(0, 16)
}

onMounted(() => loadPosts(1))
</script>

<template>
  <main class="discussion-page">
    <section class="discussion-heading">
      <p class="eyebrow">{{ t('discussions.eyebrow') }}</p>
      <h2>{{ t('discussions.title') }}</h2>
      <p>{{ t('discussions.description') }}</p>
    </section>

    <section class="compose-card">
      <h3>{{ t('discussions.publishTitle') }}</h3>
      <p class="compose-hint">{{ t('discussions.publishDescription') }}</p>
      <label class="field">
        <span>{{ t('discussions.titleLabel') }}</span>
        <input
          v-model="form.title"
          type="text"
          maxlength="150"
          :placeholder="t('discussions.titlePlaceholder')"
        >
      </label>
      <label class="field">
        <span>{{ t('discussions.contentLabel') }}</span>
        <textarea
          v-model="form.content"
          rows="6"
          maxlength="5000"
          :placeholder="t('discussions.contentPlaceholder')"
        ></textarea>
      </label>
      <div class="compose-actions">
        <button class="primary-button" type="button" :disabled="submitting" @click="submitPost">
          <el-icon><Promotion /></el-icon>
          {{ submitting ? t('discussions.submitting') : t('discussions.submit') }}
        </button>
      </div>
    </section>

    <div class="list-heading">
      <h3>{{ t('discussions.title') }}</h3>
      <span>{{ t('discussions.total', { count: total }) }}</span>
    </div>

    <section v-if="loading" class="post-list" aria-busy="true">
      <div v-for="index in 3" :key="index" class="post-card skeleton-card">
        <div class="skeleton line short"></div>
        <div class="skeleton line long"></div>
      </div>
    </section>

    <section v-else-if="loadFailed" class="empty-panel">
      <h3>{{ t('discussions.loadFailed') }}</h3>
      <button class="ghost-button" type="button" @click="loadPosts()">{{ t('resources.retry') }}</button>
    </section>

    <section v-else-if="posts.length" class="post-list">
      <article v-for="item in posts" :key="item.id" class="post-card" @click="openPost(item)">
        <h4>{{ item.title }}</h4>
        <p>{{ item.summary }}</p>
        <div class="post-meta">
          <span>{{ item.authorName || t('discussions.unknownAuthor') }}</span>
          <span class="meta-comments">
            <el-icon><ChatLineSquare /></el-icon>
            {{ item.commentCount }} {{ t('discussions.commentCount') }}
          </span>
          <span>{{ formatDate(item.createdAt) }}</span>
        </div>
      </article>
    </section>

    <section v-else class="empty-panel">
      <span class="empty-icon"><el-icon><ChatDotRound /></el-icon></span>
      <h3>{{ t('discussions.empty') }}</h3>
    </section>

    <div v-if="!loading && total > size" class="pager">
      <button class="ghost-button" type="button" :disabled="page <= 1" @click="goTo(page - 1)">
        {{ t('discussions.prev') }}
      </button>
      <span>{{ t('discussions.pageInfo', { page, pages: totalPages }) }}</span>
      <button class="ghost-button" type="button" :disabled="page >= totalPages" @click="goTo(page + 1)">
        {{ t('discussions.next') }}
      </button>
    </div>
  </main>
</template>

<style scoped>
.discussion-page {
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

.discussion-heading h2 {
  margin: 0 0 6px;
  font-size: 24px;
}

.discussion-heading p {
  margin: 0 0 22px;
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.compose-card,
.post-card {
  border: 1px solid var(--el-border-color-light);
  border-radius: 14px;
  background: var(--el-bg-color);
  box-shadow: var(--el-box-shadow-lighter);
}

.compose-card {
  display: grid;
  gap: 14px;
  margin-bottom: 26px;
  padding: 22px 24px;
}

.compose-card h3 {
  margin: 0;
  font-size: 17px;
}

.compose-hint {
  margin: -8px 0 0;
  color: var(--el-text-color-placeholder);
  font-size: 12px;
}

.field {
  display: grid;
  gap: 6px;
}

.field > span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  font-weight: 600;
}

.field input,
.field textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--el-border-color);
  border-radius: 9px;
  color: var(--el-text-color-primary);
  background: var(--el-bg-color);
  font: inherit;
  font-size: 14px;
  resize: vertical;
}

.field input:focus,
.field textarea:focus {
  border-color: var(--el-color-primary);
  outline: none;
}

.compose-actions {
  display: flex;
  justify-content: flex-end;
}

.primary-button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 18px;
  border: 0;
  border-radius: 9px;
  color: var(--el-color-white);
  background: var(--el-color-primary);
  font: inherit;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.primary-button:hover {
  background: var(--el-color-primary-light-3);
}

.primary-button:disabled {
  opacity: .6;
  cursor: not-allowed;
}

.list-heading {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 14px;
}

.list-heading h3 {
  margin: 0;
  font-size: 17px;
}

.list-heading span {
  color: var(--el-text-color-placeholder);
  font-size: 13px;
}

.post-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.post-card {
  padding: 18px 20px;
  cursor: pointer;
  transition: border-color .2s, box-shadow .2s, transform .2s;
}

.post-card:hover {
  border-color: var(--el-color-primary-light-5);
  box-shadow: var(--el-box-shadow-light);
  transform: translateY(-1px);
}

.post-card h4 {
  margin: 0 0 6px;
  font-size: 16px;
}

.post-card > p {
  margin: 0 0 10px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.post-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  color: var(--el-text-color-placeholder);
  font-size: 12px;
}

.meta-comments {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

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

.ghost-button:hover:not(:disabled) {
  color: var(--el-color-primary);
  border-color: var(--el-color-primary-light-5);
}

.ghost-button:disabled {
  opacity: .5;
  cursor: not-allowed;
}

.pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  margin-top: 22px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.skeleton-card {
  display: grid;
  gap: 10px;
}

.skeleton.line {
  height: 14px;
  border-radius: 7px;
}

.skeleton.line.short {
  width: 30%;
}

.skeleton.line.long {
  width: 85%;
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
</style>
