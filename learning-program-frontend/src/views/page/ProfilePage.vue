<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ArrowLeft, Camera, UserFilled } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { currentUserId, del, get, post, put } from '@/net'
import { useI18n } from 'vue-i18n'
import { loadStorageItem, removeStorageItem, storeStorageItem } from '@/utils/storage'
import { avatarUrl, emitProfileUpdated, type ProfileData } from '@/utils/profile'

interface Profile {
  nickname: string
  email: string
  phone: string
  bio: string
  role: string
  avatar: string | null
  avatarEnabled: boolean
}

const profileStorageKey = 'learning-profile'
const router = useRouter()
const { t } = useI18n()
const defaultProfile: Profile = {
  nickname: t('common.learner'), email: '', phone: '', bio: '', role: 'user',
  avatar: null, avatarEnabled: false,
}
// setup 顶层读取，SSR 下需经安全封装（返回 null → 走默认资料，水合后由客户端修正）
const saved = loadStorageItem(profileStorageKey)
let initialProfile = defaultProfile
if (saved) {
  try {
    const parsed = JSON.parse(saved) as Partial<Profile>
    initialProfile = { ...defaultProfile, ...parsed }
  } catch {
    removeStorageItem(profileStorageKey)
    ElMessage.warning(t('profile.readFailed'))
  }
}
const form = reactive<Profile>(initialProfile)
const saving = ref(false)
const loading = ref(false)
const avatarUploading = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)
const pendingAvatarFile = ref<File | null>(null)
const cropDialogVisible = ref(false)

const avatarSrc = computed(() => {
  const id = currentUserId()
  return form.avatar && form.avatarEnabled && id !== null ? avatarUrl(id, form.avatar) : ''
})

function roleLabel(role: string) {
  return role === 'admin' ? t('common.administrator') : t('common.ordinaryUser')
}

function applyProfile(data: ProfileData) {
  Object.assign(form, {
    nickname: data.username,
    email: data.email,
    phone: data.phone,
    bio: data.bio,
    role: data.role,
    avatar: data.avatar,
    avatarEnabled: data.avatarEnabled,
  })
  storeStorageItem(profileStorageKey, JSON.stringify(form))
  emitProfileUpdated(data)
}

function saveProfile() {
  saving.value = true
  put<ProfileData>('/api/profile', {
    username: form.nickname.trim(),
    email: form.email.trim(),
    phone: form.phone.trim(),
    bio: form.bio.trim(),
  }, (data) => {
    applyProfile(data)
    saving.value = false
    ElMessage.success(t('profile.saved'))
  }, () => { saving.value = false })
}

function resetProfile() {
  // 只重置可编辑的文本字段；头像与身份来自服务端，不参与本地重置
  Object.assign(form, { nickname: defaultProfile.nickname, email: '', phone: '', bio: '' })
  removeStorageItem(profileStorageKey)
  ElMessage.info(t('profile.restored'))
}

function loadProfile() {
  loading.value = true
  get<ProfileData>('/api/profile', (data) => {
    applyProfile(data)
    loading.value = false
  }, () => { loading.value = false })
}

function pickAvatar() {
  if (!form.avatarEnabled || avatarUploading.value) return
  fileInput.value?.click()
}

function onAvatarSelected(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  // 清空选择，保证下次选同一个文件也能触发 change
  input.value = ''
  if (!file) return
  if (!['image/jpeg', 'image/png', 'image/gif', 'image/webp'].includes(file.type)) {
    ElMessage.warning(t('profile.avatarTypeInvalid'))
    return
  }
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.warning(t('profile.avatarTooLarge'))
    return
  }
  // 先进入裁剪弹窗，确认后再上传
  pendingAvatarFile.value = file
  cropDialogVisible.value = true
}

function onAvatarCropped(blob: Blob) {
  if (avatarUploading.value) return
  if (blob.size > 2 * 1024 * 1024) {
    ElMessage.warning(t('profile.avatarTooLarge'))
    return
  }
  avatarUploading.value = true
  const payload = new FormData()
  payload.append('file', blob, 'avatar.png')
  post<ProfileData>('/api/profile/avatar', payload, (data) => {
    applyProfile(data)
    avatarUploading.value = false
    ElMessage.success(t('profile.avatarUploaded'))
  }, () => { avatarUploading.value = false })
}

function removeAvatar() {
  del<ProfileData>('/api/profile/avatar', null, (data) => {
    applyProfile(data)
    ElMessage.success(t('profile.avatarRemoved'))
  })
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
        <div class="avatar-wrapper">
          <button
            class="large-avatar"
            type="button"
            :disabled="!form.avatarEnabled || avatarUploading"
            :aria-label="t('profile.changeAvatar')"
            @click="pickAvatar"
          >
            <img v-if="avatarSrc" :src="avatarSrc" :alt="t('profile.avatarAlt')" />
            <el-icon v-else><UserFilled /></el-icon>
            <span v-if="form.avatarEnabled" class="camera-badge" aria-hidden="true"><el-icon><Camera /></el-icon></span>
          </button>
          <button v-if="avatarSrc" class="remove-avatar" type="button" @click="removeAvatar">
            {{ t('profile.removeAvatar') }}
          </button>
        </div>
        <div>
          <h3>{{ form.nickname || t('common.learner') }}</h3>
          <p>{{ roleLabel(form.role) }}</p>
        </div>
      </div>
      <input
        ref="fileInput"
        type="file"
        accept="image/jpeg,image/png,image/gif,image/webp"
        class="avatar-input"
        :aria-label="t('profile.changeAvatar')"
        @change="onAvatarSelected"
      />
      <AvatarCropDialog v-model="cropDialogVisible" :file="pendingAvatarFile" @confirm="onAvatarCropped" />

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
.profile-page { max-width: 920px; margin: 0 auto; padding: 36px 5% 64px; }
h2, h3, p { margin-top: 0; }
h2 { margin-bottom: 8px; font-size: 26px; }
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
.avatar-wrapper { display: grid; justify-items: center; gap: 8px; }
.large-avatar {
  position: relative;
  display: grid;
  width: 72px;
  height: 72px;
  padding: 0;
  place-items: center;
  border: 0;
  border-radius: 50%;
  color: var(--el-color-white);
  background: var(--el-color-info);
  font-size: 32px;
  cursor: pointer;
}
.large-avatar:disabled { cursor: default; }
.large-avatar:not(:disabled):hover { filter: brightness(1.08); }
.large-avatar:focus-visible { outline: 2px solid var(--el-color-primary); outline-offset: 2px; }
.large-avatar img { width: 100%; height: 100%; border-radius: 50%; object-fit: cover; }
.avatar-input { display: none; }
.remove-avatar {
  padding: 0;
  border: 0;
  color: var(--el-text-color-secondary);
  background: transparent;
  font: inherit;
  font-size: 12px;
  cursor: pointer;
}
.remove-avatar:hover { color: var(--el-color-danger); }
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
  .form-grid { grid-template-columns: 1fr; }}
</style>
