<script setup lang="ts">
import {onMounted, reactive, ref} from 'vue'
import {Message} from '@element-plus/icons-vue'
import {ElMessage} from 'element-plus'
import {useI18n} from 'vue-i18n'
import {get, post} from '@/net'

interface AdminUser {
  id: number
  username: string
  role: string
}

const users = ref<AdminUser[]>([])
const sending = ref(false)
const { t } = useI18n()
const form = reactive({
  title: '',
  content: '',
  type: 'system',
  targetType: 'all',
  targetRole: 'user',
  userIds: [] as number[],
})

function loadUsers() {
  get<AdminUser[]>('/api/admin/users', (data) => {
    users.value = data
  })
}

function sendNotification() {
  if (!form.title.trim() || !form.content.trim()) {
    ElMessage.warning(t('admin.fillNotification'))
    return
  }
  sending.value = true
  post('/api/admin/notifications', form, (data: { sentCount: number }) => {
    ElMessage.success(t('admin.sent', { count: data.sentCount }))
    form.title = ''
    form.content = ''
    sending.value = false
  }, () => {
    sending.value = false
  })
}

onMounted(loadUsers)
</script>

<template>
  <div class="admin-page">
    <section class="page-heading">
      <div>
        <p class="eyebrow">{{ t('admin.eyebrow') }}</p>
        <h2>{{ t('admin.notifications') }}</h2>
        <p class="description">{{ t('admin.notificationsDescription') }}</p>
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">
        <el-icon>
          <Message/>
        </el-icon>
        <h3>{{ t('admin.send') }}</h3></div>
      <el-form label-position="top" @submit.prevent="sendNotification">
        <el-form-item :label="t('admin.notificationTitle')">
          <el-input v-model="form.title" maxlength="80"/>
        </el-form-item>
        <el-form-item :label="t('admin.notificationContent')">
          <el-input v-model="form.content" type="textarea" :rows="4" maxlength="500"/>
        </el-form-item>
        <el-form-item :label="t('admin.target')">
          <el-radio-group v-model="form.targetType">
            <el-radio value="all">{{ t('admin.allUsers') }}</el-radio>
            <el-radio value="role">{{ t('admin.roleUsers') }}</el-radio>
            <el-radio value="users">{{ t('admin.selectedUsers') }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.targetType === 'role'" :label="t('admin.role')">
          <el-select v-model="form.targetRole">
            <el-option :label="t('admin.ordinaryUser')" value="user"/>
            <el-option :label="t('admin.administrator')" value="admin"/>
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.targetType === 'users'" :label="t('admin.username')">
          <el-select v-model="form.userIds" multiple filterable :placeholder="t('admin.selectUsers')" style="width: 100%">
            <el-option v-for="user in users" :key="user.id" :label="user.username" :value="user.id"/>
          </el-select>
        </el-form-item>
        <div class="form-actions">
          <el-button type="primary" :loading="sending" @click="sendNotification">{{ t('admin.send') }}</el-button>
        </div>
      </el-form>
    </section>
  </div>
</template>

<style scoped>
.admin-page {
  width: 100%;
  max-width: 1180px;
  margin: 0 auto;
  padding: 42px 5% 60px;
}

.page-heading {
  margin-bottom: 24px;
}

.eyebrow {
  margin: 0 0 7px;
  color: var(--el-color-primary);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: .14em;
}

h2, h3, p {
  margin-top: 0;
}

h2 {
  margin-bottom: 8px;
  font-size: 26px;
}

.description {
  margin-bottom: 0;
  color: var(--el-text-color-secondary);
}

.panel {
  margin-bottom: 20px;
  padding: 24px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 14px;
  background: var(--el-bg-color);
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 18px;
  color: var(--el-color-primary);
}

.panel-title h3 {
  margin-bottom: 0;
  color: var(--el-text-color-primary);
  font-size: 17px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 700px) {
  .panel {
    padding: 16px;
  }
}
</style>
