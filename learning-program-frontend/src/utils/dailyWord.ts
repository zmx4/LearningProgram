/** 每日一词设置：词表来源与开关，持久化在 localStorage，设置页与侧边栏通过自定义事件同步。
 *  单词内容按日期缓存在 localStorage（见 MainView 的 dailyWordCacheKey）。 */
export type DailyWordSource = 'cet4' | 'cet6'

export interface DailyWordSettings {
  enabled: boolean
  source: DailyWordSource
}

export const dailyWordSettingsKey = 'learning-daily-word-settings'
export const dailyWordSettingsChangedEvent = 'learning-daily-word-settings-changed'

const defaultSettings: DailyWordSettings = {
  enabled: true,
  source: 'cet4',
}

/** 读取设置，缺省或解析失败时回退到默认值（开启、CET4）。 */
export function readDailyWordSettings(): DailyWordSettings {
  const stored = localStorage.getItem(dailyWordSettingsKey)
  if (!stored) return { ...defaultSettings }

  try {
    const parsed = JSON.parse(stored) as Partial<DailyWordSettings>
    return {
      enabled: parsed.enabled !== false,
      source: parsed.source === 'cet6' ? 'cet6' : 'cet4',
    }
  } catch {
    return { ...defaultSettings }
  }
}

/** 保存设置并广播变更事件，侧边栏监听该事件即时刷新，无需刷新页面。 */
export function saveDailyWordSettings(settings: DailyWordSettings): void {
  localStorage.setItem(dailyWordSettingsKey, JSON.stringify(settings))
  window.dispatchEvent(new CustomEvent(dailyWordSettingsChangedEvent, { detail: settings }))
}
