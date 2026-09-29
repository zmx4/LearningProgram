<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'

export type ThemeMode = 'light' | 'dark' | 'system'

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
      <router-view/>
    </div>
  </header>
</template>

<style scoped>
header {
  line-height: 1.5;
}
</style>