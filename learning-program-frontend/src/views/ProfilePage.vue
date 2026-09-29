<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ArrowLeft, Camera, UserFilled } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { get, put } from '@/net'

interface Profile {
  nickname: string
  email: string
  phone: string
  bio: string
}

const profileStorageKey = 'learning-profile'
const router = useRouter()
const defaultProfile: Profile = { nickname: '学习者', email: '', phone: '', bio: '' }
const saved = localStorage.getItem(profileStorageKey)
let initialProfile = defaultProfile
if (saved) {
  try {
    const parsed = JSON.parse(saved) as Partial<Profile>
    initialProfile = { ...defaultProfile, ...parsed }
  } catch {
    localStorage.removeItem(profileStorageKey)
    ElMessage.warning('个人信息读取失败，已恢复默认值')
  }
}
const form = reactive<Profile>(initialProfile)
const saving = ref(false)
const loading = ref(false)

function saveProfile() {
  saving.value = true
  put<{ username: string; email: string; phone: string; bio: string }>('/api/profile', {
    username: form.nickname.trim(),
    email: form.email.trim(),
    phone: form.phone.trim(),
    bio: form.bio.trim(),
  }, (data) => {
    Object.assign(form, {
      nickname: data.username,
      email: data.email,
      phone: data.phone,
      bio: data.bio,
    })
    localStorage.setItem(profileStorageKey, JSON.stringify(form))
    saving.value = false
    ElMessage.success('个人信息已保存')
  }, () => { saving.value = false })
}

function resetProfile() {
  Object.assign(form, defaultProfile)
  localStorage.removeItem(profileStorageKey)
  ElMessage.info('已恢复默认信息')
}

function loadProfile() {
  loading.value = true
  get<{ username: string; email: string; phone: string; bio: string }>('/api/profile', (data) => {
    Object.assign(form, {
      nickname: data.username,
      email: data.email,
      phone: data.phone,
      bio: data.bio,
    })
    localStorage.setItem(profileStorageKey, JSON.stringify(form))
    loading.value = false
  }, () => { loading.value = false })
}

onMounted(loadProfile)
</script>

<template>
  <main class="profile-page">
    <button class="back-button" type="button" @click="router.back()">
      <el-icon><ArrowLeft /></el-icon>
      返回上一页
    </button>

    <section class="profile-heading">
      <div>
        <p class="eyebrow">ACCOUNT SETTINGS</p>
        <h2>个人信息</h2>
        <p class="heading-copy">完善你的资料，让学习空间更贴近你。</p>
      </div>
    </section>

    <section v-loading="loading" class="profile-card">
      <div class="profile-summary">
        <div class="large-avatar">
          <el-icon><UserFilled /></el-icon>
          <span class="camera-badge" aria-hidden="true"><el-icon><Camera /></el-icon></span>
        </div>
        <div>
          <h3>{{ form.nickname || '学习者' }}</h3>
          <p>学习者</p>
        </div>
      </div>

      <el-form label-position="top" class="profile-form" @submit.prevent="saveProfile">
        <div class="form-grid">
          <el-form-item label="昵称">
            <el-input v-model="form.nickname" maxlength="20" show-word-limit placeholder="请输入昵称" />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="form.email" type="email" placeholder="请输入邮箱" />
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model="form.phone" placeholder="请输入手机号" />
          </el-form-item>
          <el-form-item label="账号身份">
            <el-input model-value="学习者" disabled />
          </el-form-item>
        </div>
        <el-form-item label="个人简介">
          <el-input
            v-model="form.bio"
            maxlength="120"
            show-word-limit
            :rows="4"
            type="textarea"
            placeholder="介绍一下自己吧"
          />
        </el-form-item>
        <div class="form-actions">
          <el-button @click="resetProfile">恢复默认</el-button>
          <el-button type="primary" :loading="saving" native-type="submit">保存信息</el-button>
        </div>
      </el-form>
    </section>
  </main>
</template>

<style scoped>
:global(*) { box-sizing: border-box; }
:global(body) {
  margin: 0;
  color: var(--el-text-color-primary);
  background: var(--el-bg-color-page);
  font-family: Inter, "PingFang SC", "Microsoft YaHei", sans-serif;
}
.profile-page { max-width: 920px; margin: 0 auto; padding: 36px 5% 64px; }
.back-button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 34px;
  padding: 0;
  border: 0;
  color: var(--el-text-color-secondary);
  background: transparent;
  font: inherit;
  font-size: 13px;
  cursor: pointer;
}
.back-button:hover { color: var(--el-color-primary); }
.eyebrow { margin: 0 0 8px; color: var(--el-text-color-placeholder); font-size: 11px; font-weight: 700; letter-spacing: .13em; }
h2, h3, p { margin-top: 0; }
h2 { margin-bottom: 8px; font-size: 26px; }
.heading-copy { margin-bottom: 28px; color: var(--el-text-color-secondary); font-size: 14px; }
.profile-card {
  padding: 30px 34px 32px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 16px;
  background: var(--el-bg-color);
  box-shadow: var(--el-box-shadow-lighter);
}
.profile-summary {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 30px;
  padding-bottom: 26px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.large-avatar {
  position: relative;
  display: grid;
  width: 72px;
  height: 72px;
  place-items: center;
  border-radius: 50%;
  color: var(--el-color-white);
  background: var(--el-color-info);
  font-size: 32px;
}
.camera-badge {
  position: absolute;
  right: -2px;
  bottom: -1px;
  display: grid;
  width: 24px;
  height: 24px;
  place-items: center;
  border: 2px solid var(--el-bg-color);
  border-radius: 50%;
  color: var(--el-color-white);
  background: var(--el-color-primary);
  font-size: 12px;
}
.profile-summary h3 { margin-bottom: 5px; font-size: 18px; }
.profile-summary p { margin-bottom: 0; color: var(--el-text-color-secondary); font-size: 13px; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0 24px; }
.form-actions { display: flex; justify-content: flex-end; gap: 12px; margin-top: 8px; }
@media (max-width: 620px) {
  .profile-page { padding-top: 24px; }
  .profile-card { padding: 24px 20px; }
  .form-grid { grid-template-columns: 1fr; }
}
</style>
