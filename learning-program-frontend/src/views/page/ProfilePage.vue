<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ArrowLeft, Camera, UserFilled } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { get, put } from '@/net'
import { useI18n } from 'vue-i18n'

interface Profile {
  nickname: string
  email: string
  phone: string
  bio: string
  role: string
}

const profileStorageKey = 'learning-profile'
const router = useRouter()
const { t } = useI18n()
const defaultProfile: Profile = { nickname: t('common.learner'), email: '', phone: '', bio: '', role: 'user' }
const saved = localStorage.getItem(profileStorageKey)
let initialProfile = defaultProfile
if (saved) {
  try {
    const parsed = JSON.parse(saved) as Partial<Profile>
    initialProfile = { ...defaultProfile, ...parsed }
  } catch {
    localStorage.removeItem(profileStorageKey)
    ElMessage.warning(t('profile.readFailed'))
  }
}
const form = reactive<Profile>(initialProfile)
const saving = ref(false)
const loading = ref(false)

function roleLabel(role: string) {
  return role === 'admin' ? t('common.administrator') : t('common.ordinaryUser')
}

function saveProfile() {
  saving.value = true
  put<{ username: string; email: string; phone: string; bio: string; role: string }>('/api/profile', {
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
      role: data.role,
    })
    localStorage.setItem(profileStorageKey, JSON.stringify(form))
    saving.value = false
    ElMessage.success(t('profile.saved'))
  }, () => { saving.value = false })
}

function resetProfile() {
  Object.assign(form, defaultProfile)
  localStorage.removeItem(profileStorageKey)
  ElMessage.info(t('profile.restored'))
}

function loadProfile() {
  loading.value = true
  get<{ username: string; email: string; phone: string; bio: string; role: string }>('/api/profile', (data) => {
    Object.assign(form, {
      nickname: data.username,
      email: data.email,
      phone: data.phone,
      bio: data.bio,
      role: data.role,
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
      {{ t('profile.back') }}
    </button>

    <section class="profile-heading">
      <div>
        <p class="eyebrow">{{ t('profile.eyebrow') }}</p>
        <h2>{{ t('profile.title') }}</h2>
        <p class="heading-copy">{{ t('profile.description') }}</p>
      </div>
    </section>

    <section v-loading="loading" class="profile-card">
      <div class="profile-summary">
        <div class="large-avatar">
          <el-icon><UserFilled /></el-icon>
          <span class="camera-badge" aria-hidden="true"><el-icon><Camera /></el-icon></span>
        </div>
        <div>
          <h3>{{ form.nickname || t('common.learner') }}</h3>
          <p>{{ roleLabel(form.role) }}</p>
        </div>
      </div>

      <el-form label-position="top" class="profile-form" @submit.prevent="saveProfile">
        <div class="form-grid">
          <el-form-item :label="t('profile.nickname')">
            <el-input v-model="form.nickname" maxlength="20" show-word-limit :placeholder="t('profile.nicknamePlaceholder')" />
          </el-form-item>
          <el-form-item :label="t('profile.email')">
            <el-input v-model="form.email" type="email" :placeholder="t('profile.emailPlaceholder')" />
          </el-form-item>
          <el-form-item :label="t('profile.phone')">
            <el-input v-model="form.phone" :placeholder="t('profile.phonePlaceholder')" />
          </el-form-item>
          <el-form-item :label="t('profile.identity')">
            <el-input :model-value="roleLabel(form.role)" disabled />
          </el-form-item>
        </div>
        <el-form-item :label="t('profile.bio')">
          <el-input
            v-model="form.bio"
            maxlength="120"
            show-word-limit
            :rows="4"
            type="textarea"
            :placeholder="t('profile.bioPlaceholder')"
          />
        </el-form-item>
        <div class="form-actions">
          <el-button @click="resetProfile">{{ t('profile.reset') }}</el-button>
          <el-button type="primary" :loading="saving" native-type="submit">{{ t('profile.save') }}</el-button>
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
