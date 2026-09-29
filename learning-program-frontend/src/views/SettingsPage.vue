<script setup lang="ts">
import { ref } from 'vue'
import { Monitor, Moon, Sunny } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { ThemeMode } from '@/App.vue'

const themeStorageKey = 'learning-theme-mode'
const storedMode = localStorage.getItem(themeStorageKey)
const themeMode = ref<ThemeMode>(
  storedMode === 'light' || storedMode === 'dark' || storedMode === 'system'
    ? storedMode
    : 'system',
)

const themeOptions = [
  { value: 'light' as const, label: '明亮', description: '始终使用明亮主题', icon: Sunny },
  { value: 'dark' as const, label: '暗黑', description: '始终使用暗黑主题', icon: Moon },
  { value: 'system' as const, label: '跟随系统', description: '根据系统外观自动切换', icon: Monitor },
]

function updateTheme(mode: ThemeMode) {
  themeMode.value = mode
  localStorage.setItem(themeStorageKey, mode)
  const media = window.matchMedia('(prefers-color-scheme: dark)')
  document.documentElement.classList.toggle('dark', mode === 'dark' || (mode === 'system' && media.matches))
  ElMessage.success(`已切换为${themeOptions.find((option) => option.value === mode)?.label}模式`)
}
</script>

<template>
  <main class="settings-page">
    <section class="settings-heading">
      <p class="eyebrow">PREFERENCES</p>
      <h2>设置</h2>
      <p>调整你的使用偏好，设置仅保存在当前浏览器中。</p>
    </section>

    <section class="settings-card">
      <div class="setting-title">
        <div>
          <h3>主题模式</h3>
          <p>选择应用的显示主题</p>
        </div>
        <span class="current-mode">{{ themeOptions.find((option) => option.value === themeMode)?.label }}</span>
      </div>

      <div class="theme-options" role="radiogroup" aria-label="主题模式">
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
            <strong>{{ option.label }}</strong>
            <small>{{ option.description }}</small>
          </span>
          <span class="radio-indicator" aria-hidden="true"></span>
        </button>
      </div>
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
.settings-page { max-width: 920px; margin: 0 auto; padding: 42px 5% 64px; }
.eyebrow { margin: 0 0 8px; color: var(--el-text-color-placeholder); font-size: 11px; font-weight: 700; letter-spacing: .13em; }
h2, h3, p { margin-top: 0; }
h2 { margin-bottom: 8px; font-size: 26px; }
.settings-heading > p:last-child { margin-bottom: 30px; color: var(--el-text-color-secondary); font-size: 14px; }
.settings-card {
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
@media (max-width: 680px) {
  .settings-card { padding: 24px 20px; }
  .theme-options { grid-template-columns: 1fr; }
  .theme-option { min-height: auto; flex-direction: row; align-items: center; }
}
</style>
