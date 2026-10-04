<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { ArrowLeft, UserFilled } from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import { get } from '@/net'
import { useI18n } from 'vue-i18n'

interface UserInfo {
  username: string
  email: string
  bio: string
  avatar: string
}

const route = useRoute()
const router = useRouter()
const user = ref<UserInfo | null>(null)
const loading = ref(true)
const { t } = useI18n()

function loadUser() {
  loading.value = true
  get<UserInfo>(`/api/user/${encodeURIComponent(String(route.params.id))}`, (data) => {
    user.value = data
    loading.value = false
  }, () => {
    loading.value = false
  })
}

onMounted(loadUser)
watch(() => route.params.id, loadUser)
</script>

<template>
  <main class="user-page">
    <button class="back-button" type="button" @click="router.back()">
      <el-icon><ArrowLeft /></el-icon>
      {{ t('user.back') }}
    </button>

    <section v-loading="loading" class="user-card">
      <template v-if="user">
        <div class="user-avatar" :aria-label="t('user.avatar')">
          <el-icon><UserFilled /></el-icon>
        </div>
        <h2>{{ user.username }}</h2>
        <p class="user-label">{{ t('user.learner') }}</p>
        <div class="user-details">
          <div class="detail-item">
            <span>{{ t('user.email') }}</span>
            <strong>{{ user.email || t('user.notProvided') }}</strong>
          </div>
          <div class="detail-item bio">
            <span>{{ t('user.bio') }}</span>
            <strong>{{ user.bio || t('user.noBio') }}</strong>
          </div>
        </div>
      </template>
      <el-empty v-else-if="!loading" :description="t('user.unavailable')" />
    </section>
  </main>
</template>

<style scoped>
.user-page { max-width: 760px; margin: 0 auto; padding: 36px 5% 64px; }
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
.user-card {
  min-height: 360px;
  padding: 42px 34px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 16px;
  background: var(--el-bg-color);
  text-align: center;
  box-shadow: var(--el-box-shadow-lighter);
}
.user-avatar {
  display: grid;
  width: 88px;
  height: 88px;
  margin: 0 auto 18px;
  place-items: center;
  border-radius: 50%;
  color: var(--el-color-white);
  background: linear-gradient(135deg, var(--el-color-primary), var(--el-color-primary-light-3));
  font-size: 38px;
}
h2 { margin: 0 0 6px; font-size: 25px; }
.user-label { margin: 0 0 30px; color: var(--el-text-color-secondary); font-size: 13px; }
.user-details {
  max-width: 510px;
  margin: 0 auto;
  border-top: 1px solid var(--el-border-color-lighter);
  text-align: left;
}
.detail-item {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 20px;
  padding: 17px 4px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.detail-item span { color: var(--el-text-color-secondary); font-size: 13px; }
.detail-item strong { max-width: 75%; color: var(--el-text-color-primary); font-size: 14px; font-weight: 500; text-align: right; overflow-wrap: anywhere; }
.detail-item.bio { align-items: flex-start; }
@media (max-width: 560px) {
  .user-card { padding: 32px 20px; }
  .detail-item { display: grid; gap: 7px; }
  .detail-item strong { max-width: none; text-align: left; }}
</style>
