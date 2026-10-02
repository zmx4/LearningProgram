<script setup lang="ts">
import {onMounted, ref} from 'vue'
import {Delete, Refresh, UserFilled} from '@element-plus/icons-vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import {useI18n} from 'vue-i18n'
import {del, get, put} from '@/net'

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
      <el-button :icon="Refresh" @click="loadUsers">{{ t('admin.refresh') }}</el-button>
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
