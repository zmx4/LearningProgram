<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, ChatLineSquare, Delete, UserFilled } from '@element-plus/icons-vue'
import { currentRole, currentUserId, del, get, post as postJson } from '@/net'

interface PostDetail {
  id: number
  title: string
  content: string
  authorId: number
  authorName: string
  commentCount: number
  createdAt: string
}

interface CommentItem {
  id: number
  postId: number
  authorId: number
  authorName: string
  content: string
  createdAt: string
}

const route = useRoute()
const router = useRouter()
const { t } = useI18n()

const postId = Number(route.params.id)
const post = ref<PostDetail | null>(null)
const comments = ref<CommentItem[]>([])
const loading = ref(true)
const notFound = ref(false)
const commentText = ref('')
const submitting = ref(false)

const isAdmin = computed(() => currentRole() === 'admin')

/** 作者本人或管理员可以删除。 */
function mayDelete(authorId: number) {
  return isAdmin.value || currentUserId() === authorId
}

function loadPost() {
  loading.value = true
  notFound.value = false
  get<PostDetail>(`/api/discussions/${postId}`, data => {
    post.value = data
    loading.value = false
    loadComments()
  }, () => {
    loading.value = false
    notFound.value = true
  })
}

function loadComments() {
  get<CommentItem[]>(`/api/discussions/${postId}/comments`, data => {
    comments.value = data
  })
}

function submitComment() {
  const content = commentText.value.trim()
  if (!content) {
    ElMessage.warning(t('discussions.commentEmpty'))
    return
  }
  submitting.value = true
  postJson<CommentItem>(`/api/discussions/${postId}/comments`, { content }, data => {
    submitting.value = false
    commentText.value = ''
    comments.value.push(data)
    if (post.value) post.value.commentCount = (post.value.commentCount ?? 0) + 1
  }, () => {
    submitting.value = false
  })
}

async function removePost() {
  try {
    await ElMessageBox.confirm(t('discussions.confirmDeletePost'), t('discussions.deletePost'), { type: 'warning' })
  } catch {
    return
  }
  del<void>(`/api/discussions/${postId}`, null, () => {
    ElMessage.success(t('discussions.deleted'))
    void router.push({ name: 'discussions' })
  })
}

async function removeComment(item: CommentItem) {
  try {
    await ElMessageBox.confirm(t('discussions.confirmDeleteComment'), t('discussions.deleteComment'), { type: 'warning' })
  } catch {
    return
  }
  del<void>(`/api/discussions/${postId}/comments/${item.id}`, null, () => {
    comments.value = comments.value.filter(comment => comment.id !== item.id)
    if (post.value) post.value.commentCount = Math.max(0, (post.value.commentCount ?? 1) - 1)
    ElMessage.success(t('discussions.deleted'))
  })
}

function formatDate(value: string) {
  if (!value) return ''
  return value.replace('T', ' ').slice(0, 16)
}

onMounted(() => {
  if (Number.isFinite(postId)) loadPost()
  else {
    loading.value = false
    notFound.value = true
  }
})
</script>

<template>
  <main class="detail-page">
    <button class="back-button" type="button" @click="router.push({ name: 'discussions' })">
      <el-icon><ArrowLeft /></el-icon>
      {{ t('discussions.backToList') }}
    </button>

    <section v-if="loading" class="article-card" aria-busy="true">
      <div class="skeleton line short"></div>
      <div class="skeleton line long"></div>
      <div class="skeleton line long"></div>
    </section>

    <section v-else-if="notFound || !post" class="empty-panel">
      <h3>{{ t('discussions.notFound') }}</h3>
      <button class="ghost-button" type="button" @click="router.push({ name: 'discussions' })">
        {{ t('discussions.backToList') }}
      </button>
    </section>

    <template v-else>
      <article class="article-card">
        <div class="article-head">
          <h2>{{ post.title }}</h2>
          <button v-if="mayDelete(post.authorId)" class="danger-button" type="button" @click="removePost">
            <el-icon><Delete /></el-icon>
            {{ t('discussions.deletePost') }}
          </button>
        </div>
        <div class="article-meta">
          <span class="author"><el-icon><UserFilled /></el-icon>{{ post.authorName || t('discussions.unknownAuthor') }}</span>
          <span>{{ formatDate(post.createdAt) }}</span>
          <span class="meta-comments">
            <el-icon><ChatLineSquare /></el-icon>
            {{ post.commentCount }} {{ t('discussions.commentCount') }}
          </span>
        </div>
        <p class="article-body">{{ post.content }}</p>
      </article>

      <section class="comment-section">
        <h3>{{ t('discussions.comments') }}（{{ comments.length }}）</h3>

        <div class="comment-compose">
          <textarea
            v-model="commentText"
            rows="3"
            maxlength="1000"
            :placeholder="t('discussions.commentPlaceholder')"
          ></textarea>
          <div class="compose-actions">
            <button class="primary-button" type="button" :disabled="submitting" @click="submitComment">
              {{ submitting ? t('discussions.submitting') : t('discussions.commentSubmit') }}
            </button>
          </div>
        </div>

        <div v-if="comments.length" class="comment-list">
          <article v-for="item in comments" :key="item.id" class="comment-card">
            <div class="comment-head">
              <span class="author">
                <el-icon><UserFilled /></el-icon>{{ item.authorName || t('discussions.unknownAuthor') }}
              </span>
              <span class="comment-time">{{ formatDate(item.createdAt) }}</span>
              <button
                v-if="mayDelete(item.authorId)"
                class="link-danger"
                type="button"
                @click="removeComment(item)"
              >{{ t('discussions.deleteComment') }}</button>
            </div>
            <p>{{ item.content }}</p>
          </article>
        </div>
        <p v-else class="empty-comments">{{ t('discussions.noComments') }}</p>
      </section>
    </template>
  </main>
</template>

<style scoped>
.detail-page {
  max-width: 860px;
  margin: 0 auto;
  padding: 36px 5% 60px;
}

.back-button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 22px;
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

.article-card,
.comment-card {
  border: 1px solid var(--el-border-color-light);
  border-radius: 14px;
  background: var(--el-bg-color);
  box-shadow: var(--el-box-shadow-lighter);
}

.article-card {
  display: grid;
  gap: 12px;
  margin-bottom: 26px;
  padding: 26px 28px;
}

.article-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.article-head h2 {
  margin: 0;
  font-size: 22px;
  line-height: 1.4;
}

.article-meta,
.comment-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 14px;
  color: var(--el-text-color-placeholder);
  font-size: 12px;
}

.author,
.meta-comments {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.article-body {
  margin: 4px 0 0;
  color: var(--el-text-color-regular);
  font-size: 14px;
  line-height: 1.85;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.danger-button {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  flex: none;
  padding: 7px 12px;
  border: 1px solid var(--el-color-danger-light-5);
  border-radius: 8px;
  color: var(--el-color-danger);
  background: transparent;
  font: inherit;
  font-size: 12px;
  cursor: pointer;
}

.danger-button:hover {
  background: var(--el-color-danger-light-9);
}

.comment-section h3 {
  margin: 0 0 14px;
  font-size: 17px;
}

.comment-compose {
  display: grid;
  gap: 10px;
  margin-bottom: 22px;
}

.comment-compose textarea {
  width: 100%;
  padding: 11px 13px;
  border: 1px solid var(--el-border-color);
  border-radius: 10px;
  color: var(--el-text-color-primary);
  background: var(--el-bg-color);
  font: inherit;
  font-size: 14px;
  resize: vertical;
}

.comment-compose textarea:focus {
  border-color: var(--el-color-primary);
  outline: none;
}

.compose-actions {
  display: flex;
  justify-content: flex-end;
}

.primary-button {
  padding: 9px 18px;
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

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.comment-card {
  padding: 16px 18px;
}

.comment-head {
  margin-bottom: 8px;
}

.comment-time {
  margin-left: auto;
}

.link-danger {
  padding: 0;
  border: 0;
  color: var(--el-color-danger);
  background: transparent;
  font: inherit;
  font-size: 12px;
  cursor: pointer;
}

.link-danger:hover {
  text-decoration: underline;
}

.comment-card p {
  margin: 0;
  color: var(--el-text-color-regular);
  font-size: 13px;
  line-height: 1.7;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.empty-comments {
  margin: 0;
  padding: 26px 0;
  color: var(--el-text-color-placeholder);
  font-size: 13px;
  text-align: center;
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
