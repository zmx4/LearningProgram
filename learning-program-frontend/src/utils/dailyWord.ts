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

export function saveDailyWordSettings(settings: DailyWordSettings): void {
  localStorage.setItem(dailyWordSettingsKey, JSON.stringify(settings))
  window.dispatchEvent(new CustomEvent(dailyWordSettingsChangedEvent, { detail: settings }))
}
