<script setup lang="ts">
import {onMounted, reactive, ref} from 'vue'
import {Delete, Message, Refresh, UserFilled} from '@element-plus/icons-vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import {get, post, put, del} from '@/net'

interface AdminUser {
  id: number
  username: string
  email: string
  phone: string
  role: string
  registerDate: string
}

const users = ref<AdminUser[]>([])
const loading = ref(false)
const sending = ref(false)
const form = reactive({
  title: '',
  content: '',
  type: 'system',
  targetType: 'all',
  targetRole: 'user',
  userIds: [] as number[],
})

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
    await ElMessageBox.confirm(`确定删除用户“${user.username}”吗？`, '删除用户', {type: 'warning'})
    del(`/api/admin/users/${user.id}`, null, () => {
      ElMessage.success('用户已删除')
      loadUsers()
    })
  } catch {
    // 用户取消确认时不执行删除
  }
}

function sendNotification() {
  if (!form.title.trim() || !form.content.trim()) {
    ElMessage.warning('请填写通知标题和内容')
    return
  }
  sending.value = true
  post('/api/admin/notifications', form, (data: { sentCount: number }) => {
    ElMessage.success(`通知已发送给 ${data.sentCount} 位用户`)
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
  <main class="admin-page">
    <section class="page-heading">
      <div>
        <p class="eyebrow">ADMINISTRATION</p>
        <h2>管理员中心</h2>
        <p class="description">管理平台用户并向指定范围发送通知。</p>
      </div>
      <el-button :icon="Refresh" @click="loadUsers">刷新</el-button>
    </section>

    <section class="panel">
      <div class="panel-title">
        <el-icon>
          <UserFilled/>
        </el-icon>
        <h3>用户管理</h3></div>
      <el-table v-loading="loading" :data="users" stripe>
        <el-table-column prop="username" label="用户名" min-width="130"/>
        <el-table-column prop="email" label="邮箱" min-width="180"/>
        <el-table-column prop="phone" label="手机号" min-width="120"/>
        <el-table-column label="角色" width="150">
          <template #default="{ row }">
            <el-select v-model="row.role" size="small" @change="updateRole(row)">
              <el-option label="普通用户" value="user"/>
              <el-option label="管理员" value="admin"/>
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90">
          <template #default="{ row }">
            <el-button text type="danger" :icon="Delete" @click="removeUser(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <section class="panel">
      <div class="panel-title">
        <el-icon>
          <Message/>
        </el-icon>
        <h3>发送通知</h3></div>
      <el-form label-position="top" @submit.prevent="sendNotification">
        <el-form-item label="通知标题">
          <el-input v-model="form.title" maxlength="80"/>
        </el-form-item>
        <el-form-item label="通知内容">
          <el-input v-model="form.content" type="textarea" :rows="4" maxlength="500"/>
        </el-form-item>
        <el-form-item label="发送范围">
          <el-radio-group v-model="form.targetType">
            <el-radio value="all">全体用户</el-radio>
            <el-radio value="role">指定角色</el-radio>
            <el-radio value="users">指定用户</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.targetType === 'role'" label="角色">
          <el-select v-model="form.targetRole">
            <el-option label="普通用户" value="user"/>
            <el-option label="管理员" value="admin"/>
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.targetType === 'users'" label="用户">
          <el-select v-model="form.userIds" multiple filterable placeholder="选择用户" style="width: 100%">
            <el-option v-for="user in users" :key="user.id" :label="user.username" :value="user.id"/>
          </el-select>
        </el-form-item>
        <el-button type="primary" :loading="sending" @click="sendNotification">发送通知</el-button>
      </el-form>
    </section>
  </main>
</template>

<style scoped>
.admin-page {
  max-width: 1180px;
  margin: 0 auto;
  padding: 42px 5% 60px;
}

.page-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
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

@media (max-width: 700px) {
  .page-heading {
    align-items: flex-start;
    gap: 12px;
    flex-direction: column;
  }

  .panel {
    padding: 16px;
  }
}
</style>
