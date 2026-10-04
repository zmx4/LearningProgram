<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Delete, Download, Plus, UploadFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { UploadFile } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { del, get, post, put } from '@/net'
import { downloadResourceFile, formatFileSize } from '@/utils/resources'

interface ResourceItem {
  id: number
  title: string
  description: string | null
  fileName: string
  fileSize: number
  contentType: string | null
  visible: boolean
  uploaderName: string | null
  createdAt: string
}

const resources = ref<ResourceItem[]>([])
const loading = ref(false)
const uploading = ref(false)
const downloadingId = ref<number | null>(null)
const selectedFile = ref<File | null>(null)
const { t } = useI18n()

const form = reactive({
  title: '',
  description: '',
  visible: true,
})

function loadResources() {
  loading.value = true
  get<ResourceItem[]>('/api/admin/resources', data => {
    resources.value = data
    loading.value = false
  }, () => {
    loading.value = false
  })
}

function onFileChange(file: UploadFile) {
  selectedFile.value = (file.raw as File) ?? null
}

function onFileRemove() {
  selectedFile.value = null
}

function resetForm() {
  form.title = ''
  form.description = ''
  form.visible = true
  selectedFile.value = null
}

function uploadResource() {
  if (!form.title.trim()) {
    ElMessage.warning(t('admin.fillResourceTitle'))
    return
  }
  if (!selectedFile.value) {
    ElMessage.warning(t('admin.selectResourceFile'))
    return
  }
  uploading.value = true
  const payload = new FormData()
  payload.append('file', selectedFile.value)
  payload.append('title', form.title)
  if (form.description.trim()) payload.append('description', form.description)
  payload.append('visible', String(form.visible))
  post<ResourceItem>('/api/admin/resources', payload, () => {
    ElMessage.success(t('admin.uploadSuccess'))
    uploading.value = false
    resetForm()
    loadResources()
  }, () => {
    uploading.value = false
  })
}

function toggleVisible(item: ResourceItem) {
  put(`/api/admin/resources/${item.id}`, { visible: item.visible }, () => {
    ElMessage.success(item.visible ? t('admin.resourceShown') : t('admin.resourceHidden'))
  }, () => {
    // 失败时回滚开关状态
    item.visible = !item.visible
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

async function removeResource(item: ResourceItem) {
  try {
    await ElMessageBox.confirm(t('admin.confirmDeleteResource', { title: item.title }), t('admin.deleteResourceTitle'), { type: 'warning' })
    del(`/api/admin/resources/${item.id}`, null, () => {
      ElMessage.success(t('admin.resourceDeleted'))
      loadResources()
    })
  } catch {
    // 用户取消确认时不执行删除
  }
}

onMounted(loadResources)
</script>

<template>
  <div class="admin-page">
    <section class="page-heading">
      <div>
        <p class="eyebrow">{{ t('admin.eyebrow') }}</p>
        <h2>{{ t('admin.resources') }}</h2>
        <p class="description">{{ t('admin.resourcesDescription') }}</p>
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">
        <el-icon>
          <UploadFilled/>
        </el-icon>
        <h3>{{ t('admin.uploadResource') }}</h3></div>
      <el-form label-position="top" @submit.prevent="uploadResource">
        <el-form-item :label="t('admin.resourceFile')">
          <el-upload
              drag
              :auto-upload="false"
              :show-file-list="true"
              :limit="1"
              :on-change="onFileChange"
              :on-remove="onFileRemove"
              :on-exceed="onFileRemove"
              style="width: 100%">
            <el-icon class="el-icon--upload">
              <UploadFilled/>
            </el-icon>
            <div class="el-upload__text">{{ t('admin.selectFileHint') }}</div>
          </el-upload>
        </el-form-item>
        <el-form-item :label="t('admin.resourceTitle')">
          <el-input v-model="form.title" maxlength="100"/>
        </el-form-item>
        <el-form-item :label="t('admin.resourceDescription')">
          <el-input v-model="form.description" type="textarea" :rows="2" maxlength="255"/>
        </el-form-item>
        <el-form-item :label="t('admin.visibleOnPage')">
          <el-switch v-model="form.visible"/>
          <span class="switch-hint">{{ form.visible ? t('admin.visibleHintOn') : t('admin.visibleHintOff') }}</span>
        </el-form-item>
        <div class="form-actions">
          <el-button @click="resetForm">{{ t('admin.reset') }}</el-button>
          <el-button type="primary" :icon="Plus" :loading="uploading" @click="uploadResource">
            {{ t('admin.uploadResource') }}
          </el-button>
        </div>
      </el-form>
    </section>

    <section class="panel">
      <div class="panel-title">
        <el-icon>
          <UploadFilled/>
        </el-icon>
        <h3>{{ t('admin.resources') }}</h3></div>
      <el-table v-loading="loading" :data="resources" stripe>
        <el-table-column prop="title" :label="t('admin.resourceTitle')" min-width="150"/>
        <el-table-column prop="description" :label="t('admin.resourceDescription')" min-width="160"
                         show-overflow-tooltip/>
        <el-table-column prop="fileName" :label="t('admin.fileName')" min-width="180" show-overflow-tooltip/>
        <el-table-column :label="t('admin.fileSize')" width="100" align="center">
          <template #default="{ row }">{{ formatFileSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column :label="t('admin.visibleOnPage')" width="90" align="center">
          <template #default="{ row }">
            <el-switch v-model="row.visible" @change="toggleVisible(row)"/>
          </template>
        </el-table-column>
        <el-table-column prop="uploaderName" :label="t('admin.uploader')" width="110">
          <template #default="{ row }">{{ row.uploaderName ?? '—' }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" :label="t('admin.createdAt')" min-width="170"/>
        <el-table-column :label="t('admin.action')" width="170" align="center">
          <template #default="{ row }">
            <div class="row-actions">
              <el-button text type="primary" :icon="Download" :loading="downloadingId === row.id"
                         @click="download(row)">{{ t('resources.download') }}
              </el-button>
              <el-button text type="danger" :icon="Delete" @click="removeResource(row)">{{ t('admin.delete') }}</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<style scoped>
.switch-hint {
  margin-left: 10px;
  color: var(--el-text-color-placeholder);
  font-size: 12px;
}
</style>
