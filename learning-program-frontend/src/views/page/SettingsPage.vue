<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { CircleCheckFilled, CopyDocument, Lock, Monitor, Moon, Sunny } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import QRCode from 'qrcode'
import type { ThemeMode } from '@/App.vue'
import { useI18n } from 'vue-i18n'
import i18n, { LANGUAGE_OPTIONS, setLocale, type SupportedLocale } from '@/i18n'
import { currentUsername, del, get, post, publicGet, put } from '@/net'
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
const twoFactor = ref({ enabled: false, emailVerificationRequired: false, email: '' })
const twoFactorSetup = ref<{ secret: string; qrCodeUrl: string } | null>(null)
const qrCodeImage = ref('')
const emailCode = ref('')
const totpCode = ref('')
const loadingTwoFactor = ref(false)

// 密码规则来自后端策略，前端只负责即时提示
const policy = ref(fallbackPasswordPolicy)
onMounted(async () => {
  policy.value = await loadPasswordPolicy()
  get<{ enabled: boolean; emailVerificationRequired: boolean; email: string }>('/api/profile/2fa', (data) => { twoFactor.value = data })
})

function requestTwoFactorCode() {
  publicGet(`/api/auth/ask-code?email=${encodeURIComponent(twoFactor.value.email)}&type=2fa`, () => {
    ElMessage.success('验证码已发送，请查收邮箱')
  })
}

function setupTwoFactor() {
  if (twoFactor.value.emailVerificationRequired && !emailCode.value.trim()) {
    ElMessage.warning('请先填写邮箱验证码')
    return
  }
  loadingTwoFactor.value = true
  qrCodeImage.value = ''
  post<{ secret: string; qrCodeUrl: string }>(
    '/api/profile/2fa/setup',
    { emailCode: emailCode.value.trim() },
    async (data) => {
      twoFactorSetup.value = data
      try {
        qrCodeImage.value = await QRCode.toDataURL(data.qrCodeUrl, {
          width: 360,
          margin: 1,
          color: { dark: '#111827', light: '#ffffff' },
        })
      } catch {
        ElMessage.warning('二维码生成失败，请使用下方密钥手动添加')
      } finally {
        loadingTwoFactor.value = false
      }
    },
    () => { loadingTwoFactor.value = false },
  )
}

function resetTwoFactorSetup() {
  twoFactorSetup.value = null
  qrCodeImage.value = ''
  emailCode.value = ''
  totpCode.value = ''
}

function enableTwoFactor() {
  const code = totpCode.value.trim()
  if (!/^\d{6}$/.test(code)) {
    ElMessage.warning('请输入验证器中的 6 位数字验证码')
    return
  }
  loadingTwoFactor.value = true
  post('/api/profile/2fa/enable', { code: Number(code) }, () => {
    twoFactor.value.enabled = true
    resetTwoFactorSetup()
    loadingTwoFactor.value = false
    ElMessage.success('两步验证已启用')
  }, () => { loadingTwoFactor.value = false })
}

async function disableTwoFactor() {
  try {
    await ElMessageBox.confirm(
      '关闭后，登录时将不再要求输入动态验证码，账号安全性会降低。',
      '关闭两步验证',
      { type: 'warning', confirmButtonText: '确认关闭', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  del('/api/profile/2fa', null, () => {
    twoFactor.value.enabled = false
    ElMessage.success('两步验证已关闭')
  })
}

function copySecret() {
  const secret = twoFactorSetup.value?.secret
  if (!secret) return
  if (!navigator.clipboard) {
    ElMessage.info(`密钥：${secret}`)
    return
  }
  void navigator.clipboard.writeText(secret)
    .then(() => ElMessage.success('密钥已复制'))
    .catch(() => ElMessage.info(`密钥：${secret}`))
}

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
          <h3>两步验证（2FA）</h3>
          <p>登录时除密码外，还需要验证器应用生成的动态验证码。</p>
        </div>
        <el-tag class="two-factor-status" :type="twoFactor.enabled ? 'success' : 'info'" effect="light" round>
          {{ twoFactor.enabled ? '已启用' : '未启用' }}
        </el-tag>
      </div>

      <div v-if="twoFactor.enabled" class="two-factor-enabled">
        <el-icon><CircleCheckFilled /></el-icon>
        <div>
          <strong>两步验证已启用</strong>
          <p>每次登录都需要输入验证器中的动态验证码；更换设备前请先关闭，再重新绑定。</p>
        </div>
        <el-button type="danger" plain @click="disableTwoFactor">关闭两步验证</el-button>
      </div>

      <div v-else-if="!twoFactorSetup" class="two-factor-entry">
        <div class="two-factor-intro">
          <span class="intro-icon"><el-icon><Lock /></el-icon></span>
          <div>
            <strong>为账号增加一道防线</strong>
            <p>绑定 Google Authenticator、Microsoft Authenticator 等验证器应用，即可开启动态验证码登录。</p>
          </div>
        </div>
        <div v-if="twoFactor.emailVerificationRequired" class="two-factor-email">
          <p>启用前需要验证邮箱 <strong>{{ twoFactor.email }}</strong>，请填写收到的验证码。</p>
          <el-input v-model="emailCode" maxlength="6" placeholder="邮箱验证码">
            <template #append><el-button @click="requestTwoFactorCode">发送验证码</el-button></template>
          </el-input>
        </div>
        <div>
          <el-button type="primary" :loading="loadingTwoFactor" @click="setupTwoFactor">生成验证器密钥</el-button>
        </div>
      </div>

      <div v-else class="totp-setup">
        <div class="totp-qr">
          <div class="qr-frame">
            <img v-if="qrCodeImage" :src="qrCodeImage" alt="两步验证二维码" />
            <div v-else class="qr-fallback">
              <el-icon><Lock /></el-icon>
              <span>二维码生成失败，请使用密钥手动添加</span>
            </div>
          </div>
          <span class="qr-caption">使用验证器应用扫码</span>
        </div>
        <div class="totp-panel">
          <div class="totp-step">
            <span class="step-index">1</span>
            <div class="step-body">
              <strong>扫描二维码</strong>
              <p>打开验证器应用，扫描左侧二维码添加 LearningProgram。</p>
              <div class="totp-manual">
                <span>无法扫码？手动输入密钥</span>
                <code>{{ twoFactorSetup.secret }}</code>
                <el-button text type="primary" size="small" @click="copySecret">
                  <el-icon><CopyDocument /></el-icon>复制
                </el-button>
              </div>
            </div>
          </div>
          <div class="totp-step">
            <span class="step-index">2</span>
            <div class="step-body">
              <strong>输入验证码</strong>
              <p>输入验证器应用当前显示的 6 位验证码，完成绑定。</p>
              <el-input
                v-model="totpCode"
                class="totp-code-input"
                maxlength="6"
                inputmode="numeric"
                placeholder="000000"
              />
            </div>
          </div>
          <div class="totp-actions">
            <el-button
              type="primary"
              :loading="loadingTwoFactor"
              :disabled="totpCode.trim().length !== 6"
              @click="enableTwoFactor"
            >
              确认启用
            </el-button>
            <el-button text @click="resetTwoFactorSetup">取消</el-button>
          </div>
        </div>
      </div>
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
.two-factor-status { flex: none; margin-top: 2px; }
.two-factor-enabled {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 18px;
  border: 1px solid var(--el-color-success-light-7);
  border-radius: 12px;
  background: var(--el-color-success-light-9);
}
.two-factor-enabled > .el-icon { flex: none; color: var(--el-color-success); font-size: 26px; }
.two-factor-enabled strong { font-size: 14px; }
.two-factor-enabled p { margin: 4px 0 0; color: var(--el-text-color-secondary); font-size: 13px; line-height: 1.6; }
.two-factor-enabled > .el-button { flex: none; margin-left: auto; }
.two-factor-entry { display: flex; flex-direction: column; align-items: flex-start; gap: 18px; }
.two-factor-intro { display: flex; gap: 14px; align-self: stretch; padding: 16px 18px; border-radius: 12px; background: var(--el-fill-color-light); }
.intro-icon {
  display: grid;
  width: 38px;
  height: 38px;
  flex: none;
  place-items: center;
  border-radius: 10px;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-8);
  font-size: 19px;
}
.two-factor-intro strong { font-size: 14px; }
.two-factor-intro p { margin: 4px 0 0; color: var(--el-text-color-secondary); font-size: 13px; line-height: 1.6; }
.two-factor-email { width: 100%; max-width: 420px; }
.two-factor-email p { margin: 0 0 10px; color: var(--el-text-color-secondary); font-size: 13px; }
.totp-setup { display: grid; grid-template-columns: 220px minmax(0, 1fr); gap: 30px; align-items: start; }
.totp-qr { display: flex; flex-direction: column; align-items: center; gap: 10px; }
.qr-frame {
  display: grid;
  width: 200px;
  height: 200px;
  place-items: center;
  padding: 10px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 14px;
  background: #fff;
}
.qr-frame img { display: block; width: 100%; height: 100%; }
.qr-fallback { display: flex; flex-direction: column; align-items: center; gap: 8px; color: var(--el-text-color-placeholder); font-size: 12px; line-height: 1.5; text-align: center; }
.qr-fallback .el-icon { font-size: 26px; }
.qr-caption { color: var(--el-text-color-secondary); font-size: 12px; }
.totp-panel { display: flex; flex-direction: column; gap: 22px; }
.totp-step { display: flex; gap: 12px; }
.step-index {
  display: grid;
  width: 24px;
  height: 24px;
  flex: none;
  place-items: center;
  border-radius: 50%;
  color: #fff;
  background: var(--el-color-primary);
  font-size: 12px;
  font-weight: 600;
}
.step-body { display: flex; min-width: 0; flex-direction: column; gap: 6px; }
.step-body strong { font-size: 14px; }
.step-body p { margin: 0; color: var(--el-text-color-secondary); font-size: 13px; line-height: 1.6; }
.totp-manual { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; margin-top: 4px; color: var(--el-text-color-secondary); font-size: 12px; }
.totp-manual code {
  padding: 4px 10px;
  border: 1px dashed var(--el-border-color);
  border-radius: 6px;
  color: var(--el-text-color-primary);
  background: var(--el-fill-color-light);
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 12px;
  letter-spacing: .04em;
  word-break: break-all;
}
.totp-manual .el-button { padding: 0 4px; }
.totp-code-input { max-width: 220px; }
.totp-code-input :deep(.el-input__inner) { font-size: 18px; letter-spacing: .35em; text-align: center; }
.totp-actions { display: flex; align-items: center; gap: 4px; }
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
.password-form { 
  /* max-width: 420px;  */
  max-width: 100%;
}
.password-form .form-actions { display: flex; justify-content: flex-end; }
@media (max-width: 680px) {
  .settings-card { padding: 24px 20px; }
  .theme-options { grid-template-columns: 1fr; }
  .theme-option { min-height: auto; flex-direction: row; align-items: center; }
  .two-factor-enabled { align-items: flex-start; flex-wrap: wrap; }
  .two-factor-enabled > .el-button { margin-left: 0; }
  .totp-setup { grid-template-columns: 1fr; gap: 24px; }
}
</style>
