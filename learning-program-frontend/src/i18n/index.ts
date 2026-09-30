import { createI18n } from 'vue-i18n'
import zhCN from './locales/zh-CN'

const localeStorageKey = 'learning-locale'
const supportedLocales = ['zh-CN'] as const
type SupportedLocale = typeof supportedLocales[number]

function getInitialLocale(): SupportedLocale {
  const storedLocale = localStorage.getItem(localeStorageKey)
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

export { localeStorageKey, supportedLocales }
export default i18n
