<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Collection, Download, Document } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { get } from '@/net'
import { downloadResourceFile, formatFileSize } from '@/utils/resources'

interface ResourceItem {
  id: number
  title: string
  description: string | null
  fileName: string
  fileSize: number
  contentType: string | null
  createdAt: string
}

const { t } = useI18n()

const resources = ref<ResourceItem[]>([])
const loading = ref(true)
const loadFailed = ref(false)
const downloadingId = ref<number | null>(null)

function loadResources() {
  loading.value = true
  loadFailed.value = false
  get<ResourceItem[]>('/api/resources', data => {
    resources.value = data
    loading.value = false
  }, () => {
    loading.value = false
    loadFailed.value = true
  })
}

async function download(item: ResourceItem) {
  if (downloadingId.value !== null) return
  downloadingId.value = item.id
  try {
    await downloadResourceFile(item.id, item.fileName)
  } catch {
    ElMessage.error(t('resources.downloadFailed'))
  } finally {
    downloadingId.value = null
  }
}

onMounted(loadResources)
</script>

<template>
  <main class="resources-page">
    <section class="resources-heading">
      <p class="eyebrow">LEARNING RESOURCES</p>
      <h2>{{ t('resources.title') }}</h2>
      <p>{{ t('resources.description') }}</p>
    </section>

    <section v-if="loading" class="resource-list" aria-busy="true">
      <div v-for="index in 3" :key="index" class="resource-card skeleton-card">
        <div class="skeleton line short"></div>
        <div class="skeleton line long"></div>
      </div>
    </section>

    <section v-else-if="loadFailed" class="empty-panel">
      <h3>{{ t('resources.loadFailed') }}</h3>
      <button class="empty-action" type="button" @click="loadResources">{{ t('resources.retry') }}</button>
    </section>

    <section v-else-if="resources.length" class="resource-list">
      <article v-for="item in resources" :key="item.id" class="resource-card">
        <div class="resource-main">
          <span class="resource-icon"><el-icon><Document /></el-icon></span>
          <div class="resource-info">
            <strong>{{ item.title }}</strong>
            <p v-if="item.description">{{ item.description }}</p>
            <div class="resource-meta">
              <span class="file-name">{{ item.fileName }}</span>
              <span>{{ formatFileSize(item.fileSize) }}</span>
              <span>{{ item.createdAt?.slice(0, 10) }}</span>
            </div>
          </div>
        </div>
        <button
          class="download-button"
          type="button"
          :disabled="downloadingId === item.id"
          @click="download(item)"
        >
          <el-icon><Download /></el-icon>
          {{ t('resources.download') }}
        </button>
      </article>
    </section>

    <section v-else class="empty-panel">
      <span class="empty-icon"><el-icon><Collection /></el-icon></span>
      <h3>{{ t('resources.emptyTitle') }}</h3>
      <p>{{ t('resources.emptyDescription') }}</p>
    </section>
  </main>
</template>

<style scoped>
.resources-page {
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

.resources-heading h2 {
  margin: 0 0 6px;
  font-size: 24px;
}

.resources-heading p {
  margin: 0 0 22px;
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.resource-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.resource-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 14px;
  background: var(--el-bg-color);
  box-shadow: var(--el-box-shadow-lighter);
}

.resource-main {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  min-width: 0;
}

.resource-icon {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border-radius: 12px;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  font-size: 20px;
  flex: none;
}

.resource-info {
  min-width: 0;
}

.resource-info strong {
  display: block;
  margin-bottom: 2px;
  font-size: 15px;
}

.resource-info p {
  margin: 0 0 4px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.resource-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  color: var(--el-text-color-placeholder);
  font-size: 12px;
}

.file-name {
  max-width: 320px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.download-button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex: none;
  padding: 9px 15px;
  border: 0;
  border-radius: 8px;
  color: var(--el-color-white);
  background: var(--el-color-primary);
  font: inherit;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.download-button:hover {
  background: var(--el-color-primary-light-3);
}

.download-button:disabled {
  opacity: .6;
  cursor: not-allowed;
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

.empty-panel p {
  margin: 0;
  color: var(--el-text-color-placeholder);
  font-size: 13px;
}

.empty-icon {
  color: var(--el-text-color-placeholder);
  font-size: 40px;
}

.empty-action {
  padding: 8px 16px;
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  color: var(--el-text-color-regular);
  background: var(--el-bg-color);
  font: inherit;
  font-size: 13px;
  cursor: pointer;
}

.empty-action:hover {
  color: var(--el-color-primary);
  border-color: var(--el-color-primary-light-5);
}

/* 骨架屏占位 */
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

@media (max-width: 620px) {
  .resource-card {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
