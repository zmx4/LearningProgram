import { createI18n } from 'vue-i18n'
import zhCN from './locales/zh-CN'
import { readLocal, writeLocal } from '@/utils/storage'

const localeStorageKey = 'learning-locale'
const supportedLocales = ['zh-CN'] as const
type SupportedLocale = typeof supportedLocales[number]

// 语言选项以各自母语展示（语言选择器的惯例），新增语言时在此登记即可
export const LANGUAGE_OPTIONS: ReadonlyArray<{ value: SupportedLocale, label: string }> = [
  { value: 'zh-CN', label: '简体中文' },
]

function getInitialLocale(): SupportedLocale {
  // 走 SSR 安全的读取：服务端没有 localStorage，此时回落默认语言
  const storedLocale = readLocal(localeStorageKey)
  return supportedLocales.includes(storedLocale as SupportedLocale)
    ? storedLocale as SupportedLocale
    : 'zh-CN'
}

const i18n = createI18n({
  legacy: false,
  locale: getInitialLocale(),
  fallbackLocale: 'zh-CN',
  messages: {
    'zh-CN': zhCN,
  },
})

function setLocale(locale: SupportedLocale) {
  i18n.global.locale.value = locale
  writeLocal(localeStorageKey, locale)
}

export { localeStorageKey, supportedLocales, setLocale }
export type { SupportedLocale }
export default i18n
