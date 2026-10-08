<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Monitor, Moon, Sunny } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { ThemeMode } from '@/App.vue'
import { useI18n } from 'vue-i18n'
import i18n, { LANGUAGE_OPTIONS, setLocale, type SupportedLocale } from '@/i18n'
import { currentUsername, put } from '@/net'
import PasswordPolicyHint from '@/components/PasswordPolicyHint.vue'
import { fallbackPasswordPolicy, firstUnmetRequirement, loadPasswordPolicy } from '@/utils/passwordPolicy'
import {
  readDailyWordSettings,
  saveDailyWordSettings,
  type DailyWordSource,
} from '@/utils/dailyWord'

const themeStorageKey = 'learning-theme-mode'
const { t } = useI18n()
const storedMode = localStorage.getItem(themeStorageKey)
const themeMode = ref<ThemeMode>(
  storedMode === 'light' || storedMode === 'dark' || storedMode === 'system'
    ? storedMode
    : 'system',
)
const dailyWordSettings = ref(readDailyWordSettings())

// 密码规则来自后端策略，前端只负责即时提示
const policy = ref(fallbackPasswordPolicy)
onMounted(async () => {
  policy.value = await loadPasswordPolicy()
})

const themeOptions = [
  { value: 'light' as const, label: 'settings.light', description: 'settings.lightDescription', icon: Sunny },
  { value: 'dark' as const, label: 'settings.dark', description: 'settings.darkDescription', icon: Moon },
  { value: 'system' as const, label: 'settings.system', description: 'settings.systemDescription', icon: Monitor },
]

function updateTheme(mode: ThemeMode) {
  themeMode.value = mode
  localStorage.setItem(themeStorageKey, mode)
  const media = window.matchMedia('(prefers-color-scheme: dark)')
  document.documentElement.classList.toggle('dark', mode === 'dark' || (mode === 'system' && media.matches))
  const selectedOption = themeOptions.find((option) => option.value === mode)
  ElMessage.success(t('settings.switched', { mode: selectedOption ? t(selectedOption.label) : '' }))
}

function updateDailyWordEnabled(enabled: boolean) {
  dailyWordSettings.value.enabled = enabled
  saveDailyWordSettings(dailyWordSettings.value)
}

function updateDailyWordSource(source: DailyWordSource) {
  dailyWordSettings.value.source = source
  saveDailyWordSettings(dailyWordSettings.value)
}

const languageOptions = LANGUAGE_OPTIONS
const currentLocale = computed(() => i18n.global.locale.value)

function updateLocale(lang: string | number | boolean | undefined) {
  if (typeof lang !== 'string' || lang === currentLocale.value) return
  const selected = languageOptions.find(option => option.value === lang)
  setLocale(lang as SupportedLocale)
  ElMessage.success(t('settings.languageSwitched', { language: selected?.label ?? lang }))
}

const emptyPasswordForm = () => ({ oldPassword: '', newPassword: '', confirmPassword: '' })
const passwordForm = ref(emptyPasswordForm())
const changingPassword = ref(false)

function submitChangePassword() {
  const { oldPassword, newPassword, confirmPassword } = passwordForm.value
  if (!oldPassword || !newPassword || !confirmPassword) {
    ElMessage.warning(t('settings.fillPasswordFields'))
    return
  }
  // 规则与阈值都来自后端策略，服务端提交时还会再校验一次
  const unmet = firstUnmetRequirement(newPassword, policy.value, { username: currentUsername() })
  if (unmet) {
    ElMessage.warning(unmet)
    return
  }
  if (newPassword !== confirmPassword) {
    ElMessage.warning(t('settings.passwordMismatch'))
    return
  }
  changingPassword.value = true
  put('/api/profile/password', { oldPassword, newPassword }, () => {
    changingPassword.value = false
    passwordForm.value = emptyPasswordForm()
    ElMessage.success(t('settings.passwordChanged'))
  }, () => {
    changingPassword.value = false
  })
}
</script>

<template>
  <main class="settings-page">
    <section class="settings-heading">
      <p class="eyebrow">{{ t('settings.eyebrow') }}</p>
      <h2>{{ t('settings.title') }}</h2>
      <p>{{ t('settings.description') }}</p>
    </section>

    <section class="settings-card">
      <div class="setting-title">
        <div>
          <h3>{{ t('settings.theme') }}</h3>
          <p>{{ t('settings.themeDescription') }}</p>
        </div>
        <span class="current-mode">{{ t(themeOptions.find((option) => option.value === themeMode)?.label ?? 'settings.system') }}</span>
      </div>

      <div class="theme-options" role="radiogroup" :aria-label="t('settings.theme')">
        <button
          v-for="option in themeOptions"
          :key="option.value"
          class="theme-option"
          :class="{ selected: themeMode === option.value }"
          type="button"
          role="radio"
          :aria-checked="themeMode === option.value"
          @click="updateTheme(option.value)"
        >
          <span class="theme-icon"><el-icon><component :is="option.icon" /></el-icon></span>
          <span class="theme-copy">
            <strong>{{ t(option.label) }}</strong>
            <small>{{ t(option.description) }}</small>
          </span>
          <span class="radio-indicator" aria-hidden="true"></span>
        </button>
      </div>
    </section>

    <section class="settings-card">
      <div class="setting-title">
        <div>
          <h3>{{ t('settings.dailyWord') }}</h3>
          <p>{{ t('settings.dailyWordDescription') }}</p>
        </div>
        <el-switch
          :model-value="dailyWordSettings.enabled"
          :aria-label="t('settings.dailyWord')"
          @update:model-value="updateDailyWordEnabled"
        />
      </div>

      <div class="daily-word-source">
        <span>{{ t('settings.dailyWordSource') }}</span>
        <el-radio-group
          :model-value="dailyWordSettings.source"
          :disabled="!dailyWordSettings.enabled"
          @update:model-value="updateDailyWordSource"
        >
          <el-radio-button label="cet4">CET4</el-radio-button>
          <el-radio-button label="cet6">CET6</el-radio-button>
        </el-radio-group>
      </div>
    </section>

    <section class="settings-card">
      <div class="setting-title">
        <div>
          <h3>{{ t('settings.language') }}</h3>
          <p>{{ t('settings.languageDescription') }}</p>
        </div>
      </div>

      <div class="language-options">
        <el-radio-group
          :model-value="currentLocale"
          :aria-label="t('settings.language')"
          @update:model-value="updateLocale"
        >
          <el-radio-button
            v-for="lang in languageOptions"
            :key="lang.value"
            :label="lang.value"
          >
            {{ lang.label }}
          </el-radio-button>
        </el-radio-group>
        <p v-if="languageOptions.length === 1" class="language-hint">
          {{ t('settings.languageComingSoon') }}
        </p>
      </div>
    </section>

    <section class="settings-card">
      <div class="setting-title">
        <div>
          <h3>{{ t('settings.changePassword') }}</h3>
          <p>{{ t('settings.changePasswordDescription', { min: policy.minLength, max: policy.maxLength }) }}</p>
        </div>
      </div>

      <el-form label-position="top" class="password-form" @submit.prevent="submitChangePassword">
        <el-form-item :label="t('settings.oldPassword')">
          <el-input
            v-model="passwordForm.oldPassword"
            type="password"
            show-password
            :placeholder="t('settings.oldPasswordPlaceholder')"
          />
        </el-form-item>
        <el-form-item :label="t('settings.newPassword')">
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            show-password
            :maxlength="policy.maxLength"
            :placeholder="t('settings.newPasswordPlaceholder', { min: policy.minLength, max: policy.maxLength })"
          />
          <PasswordPolicyHint :requirements="policy.requirements" :value="passwordForm.newPassword" :username="currentUsername()" />
        </el-form-item>
        <el-form-item :label="t('settings.confirmPassword')">
          <el-input
            v-model="passwordForm.confirmPassword"
            type="password"
            show-password
            :maxlength="policy.maxLength"
            :placeholder="t('settings.confirmPasswordPlaceholder')"
          />
        </el-form-item>
        <div class="form-actions">
          <el-button type="primary" :loading="changingPassword" native-type="submit">
            {{ t('settings.confirmChange') }}
          </el-button>
        </div>
      </el-form>
    </section>
  </main>
</template>

<style scoped>
.settings-page { max-width: 920px; margin: 0 auto; padding: 42px 5% 64px; }
h2, h3, p { margin-top: 0; }
h2 { margin-bottom: 8px; font-size: 26px; }
.eyebrow { margin: 0 0 8px; color: var(--el-text-color-placeholder); font-size: 11px; font-weight: 700; letter-spacing: .13em; }
.settings-heading > p:last-child { margin-bottom: 30px; color: var(--el-text-color-secondary); font-size: 14px; }
.settings-card {
  margin-bottom: 20px;
  padding: 30px 34px 34px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 16px;
  background: var(--el-bg-color);
  box-shadow: var(--el-box-shadow-lighter);
}
.setting-title { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; margin-bottom: 22px; }
.setting-title h3 { margin-bottom: 6px; font-size: 17px; }
.setting-title p { margin-bottom: 0; color: var(--el-text-color-secondary); font-size: 13px; }
.current-mode { color: var(--el-color-primary); font-size: 13px; }
.daily-word-source { display: flex; align-items: center; justify-content: space-between; gap: 16px; color: var(--el-text-color-secondary); font-size: 13px; }
.language-options { display: flex; flex-direction: column; align-items: flex-start; gap: 10px; }
.language-hint { margin: 0; color: var(--el-text-color-placeholder); font-size: 12px; }
.theme-options { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; }
.theme-option {
  display: flex;
  min-height: 126px;
  flex-direction: column;
  align-items: flex-start;
  gap: 13px;
  padding: 18px;
  border: 1px solid var(--el-border-color);
  border-radius: 12px;
  color: var(--el-text-color-primary);
  background: var(--el-bg-color);
  text-align: left;
  cursor: pointer;
  transition: border-color .2s, background .2s, box-shadow .2s;
}
.theme-option:hover, .theme-option.selected { border-color: var(--el-color-primary); }
.theme-option.selected { background: var(--el-color-primary-light-9); box-shadow: 0 0 0 1px var(--el-color-primary); }
.theme-icon {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  border-radius: 9px;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-8);
  font-size: 19px;
}
.theme-copy { display: grid; gap: 5px; }
.theme-copy strong { font-size: 14px; }
.theme-copy small { color: var(--el-text-color-secondary); font-size: 12px; }
.radio-indicator {
  position: absolute;
  width: 0;
  height: 0;
  overflow: hidden;
}
.password-form { max-width: 420px; }
.password-form .form-actions { display: flex; justify-content: flex-end; }
@media (max-width: 680px) {
  .settings-card { padding: 24px 20px; }
  .theme-options { grid-template-columns: 1fr; }
  .theme-option { min-height: auto; flex-direction: row; align-items: center; }}
</style>
