<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import i18n, { type SupportedLocale } from './i18n'

export type ThemeMode = 'light' | 'dark' | 'system'

// Element Plus 组件文案跟随应用语言，新增语言时在此登记对应语言包
const elementLocales = { 'zh-CN': zhCn } as const
const elementLocale = computed(() => elementLocales[i18n.global.locale.value as SupportedLocale])

const themeStorageKey = 'learning-theme-mode'
const themeMode = ref<ThemeMode>('system')
let systemThemeMedia: MediaQueryList | null = null

function isThemeMode(value: string | null): value is ThemeMode {
  return value === 'light' || value === 'dark' || value === 'system'
}

function applyTheme() {
  const dark = themeMode.value === 'dark'
    || (themeMode.value === 'system' && systemThemeMedia?.matches === true)
  document.documentElement.classList.toggle('dark', dark)
}

function onSystemThemeChange() {
  if (themeMode.value === 'system') applyTheme()
}

onMounted(() => {
  const storedMode = localStorage.getItem(themeStorageKey)
  if (isThemeMode(storedMode)) themeMode.value = storedMode
  systemThemeMedia = window.matchMedia('(prefers-color-scheme: dark)')
  systemThemeMedia.addEventListener('change', onSystemThemeChange)
  applyTheme()
})

onBeforeUnmount(() => {
  systemThemeMedia?.removeEventListener('change', onSystemThemeChange)
})
</script>

<template>
  <header>
    <div class="wrapper">
      <el-config-provider :locale="elementLocale">
        <router-view/>
      </el-config-provider>
    </div>
  </header>
</template>

<style scoped>
header {
  line-height: 1.5;
}
</style>