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
