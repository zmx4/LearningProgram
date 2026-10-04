<script setup lang="ts">
import {onMounted, ref} from 'vue'
import {Delete, Plus, Refresh, UserFilled} from '@element-plus/icons-vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import {useI18n} from 'vue-i18n'
import {del, get, post, put} from '@/net'

interface AdminUser {
  id: number
  username: string
  email: string
  phone: string
  role: string
  registerDate: string
}

interface BatchCreateError {
  index: number
  message: string
}

interface BatchCreateResult {
  createdCount: number
  failedCount: number
  errors: BatchCreateError[]
}

const users = ref<AdminUser[]>([])
const loading = ref(false)
const batchDialogVisible = ref(false)
const batchSubmitting = ref(false)
const batchText = ref('')
const batchErrors = ref<string[]>([])
const { t } = useI18n()

function loadUsers() {
  loading.value = true
  get<AdminUser[]>('/api/admin/users', (data) => {
    users.value = data
    loading.value = false
  }, () => {
    loading.value = false
  })
}

function updateRole(user: AdminUser) {
  put(`/api/admin/users/${user.id}/role`, {role: user.role}, loadUsers)
}

async function removeUser(user: AdminUser) {
  try {
    await ElMessageBox.confirm(t('admin.confirmDelete', { username: user.username }), t('admin.deleteTitle'), {type: 'warning'})
    del(`/api/admin/users/${user.id}`, null, () => {
      ElMessage.success(t('admin.deleted'))
      loadUsers()
    })
  } catch {
    // 用户取消确认时不执行删除
  }
}

function openBatchDialog() {
  batchText.value = ''
  batchErrors.value = []
  batchDialogVisible.value = true
}

function parseBatchLines(): {
  accounts: {username: string, email: string, password: string}[],
  lines: number[]
} | null {
  const accounts: {username: string, email: string, password: string}[] = []
  const lines: number[] = []
  const formatErrors: string[] = []
  batchText.value.split('\n').forEach((rawLine, index) => {
    const line = rawLine.trim()
    if (!line) return
    const parts = line.split(/[,，\s]+/).filter(Boolean)
    if (parts.length !== 3) {
      formatErrors.push(t('admin.batchInvalidLine', {line: index + 1}))
      return
    }
    const [username = '', email = '', password = ''] = parts
    accounts.push({username, email, password})
    lines.push(index + 1)
  })
  if (formatErrors.length > 0) {
    batchErrors.value = formatErrors
    return null
  }
  if (accounts.length === 0) {
    ElMessage.warning(t('admin.batchEmpty'))
    return null
  }
  return {accounts, lines}
}

function submitBatch() {
  const parsed = parseBatchLines()
  if (!parsed) return
  batchSubmitting.value = true
  post<BatchCreateResult>('/api/admin/users/batch', {accounts: parsed.accounts}, (data) => {
    batchSubmitting.value = false
    if (data.errors.length > 0) {
      batchErrors.value = data.errors.map(e => t('admin.batchItemError', {
        line: parsed.lines[e.index] ?? '',
        username: parsed.accounts[e.index]?.username ?? '',
        message: e.message
      }))
      ElMessage.warning(t('admin.batchPartial', {created: data.createdCount, failed: data.failedCount}))
    } else {
      batchDialogVisible.value = false
      ElMessage.success(t('admin.batchCreated', {count: data.createdCount}))
    }
    loadUsers()
  }, () => {
    batchSubmitting.value = false
  })
}

onMounted(loadUsers)
</script>

<template>
  <div class="admin-page">
    <section class="page-heading">
      <div>
        <p class="eyebrow">{{ t('admin.eyebrow') }}</p>
        <h2>{{ t('admin.users') }}</h2>
        <p class="description">{{ t('admin.usersDescription') }}</p>
      </div>
      <div class="heading-actions">
        <el-button :icon="Plus" type="primary" @click="openBatchDialog">{{ t('admin.batchAdd') }}</el-button>
        <el-button :icon="Refresh" @click="loadUsers">{{ t('admin.refresh') }}</el-button>
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">
        <el-icon>
          <UserFilled/>
        </el-icon>
        <h3>{{ t('admin.users') }}</h3></div>
      <el-table v-loading="loading" :data="users" stripe>
        <el-table-column prop="username" :label="t('admin.username')" min-width="130"/>
        <el-table-column prop="email" :label="t('admin.email')" min-width="180"/>
        <el-table-column prop="phone" :label="t('admin.phone')" min-width="120"/>
        <el-table-column :label="t('admin.role')" width="150">
          <template #default="{ row }">
            <el-select v-model="row.role" size="small" @change="updateRole(row)">
              <el-option :label="t('admin.ordinaryUser')" value="user"/>
              <el-option :label="t('admin.administrator')" value="admin"/>
            </el-select>
          </template>
        </el-table-column>
        <el-table-column :label="t('admin.action')" width="90">
          <template #default="{ row }">
            <el-button text type="danger" :icon="Delete" @click="removeUser(row)">{{ t('admin.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="batchDialogVisible" :title="t('admin.batchAddTitle')" width="560px">
      <p class="batch-hint">{{ t('admin.batchFormatHint') }}</p>
      <el-input
          v-model="batchText"
          type="textarea"
          :rows="8"
          :placeholder="t('admin.batchPlaceholder')"
      />
      <div v-if="batchErrors.length > 0" class="batch-errors">
        <el-alert
            v-for="error in batchErrors"
            :key="error"
            :title="error"
            type="error"
            :closable="false"
        />
      </div>
      <template #footer>
        <el-button @click="batchDialogVisible = false">{{ t('admin.cancel') }}</el-button>
        <el-button type="primary" :loading="batchSubmitting" @click="submitBatch">
          {{ t('admin.batchSubmit') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.heading-actions {
  display: flex;
  align-items: center;
}

.batch-hint {
  margin: 0 0 10px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.batch-errors {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 12px;
}
</style>
