/**
 * 用户资料的共享类型与工具。
 * 头像功能仅在后端启用对象存储时可用（avatarEnabled），未启用时 avatar 恒为 null。
 */

export interface ProfileData {
  username: string
  email: string
  phone: string
  bio: string
  role: string
  avatar: string | null
  avatarEnabled: boolean
}

// 资料变更后的广播事件：顶栏监听它实时刷新头像（与每日一词设置事件同一模式）
export const profileUpdatedEvent = 'learning-profile-updated'

export function emitProfileUpdated(profile: ProfileData): void {
  window.dispatchEvent(new CustomEvent<ProfileData>(profileUpdatedEvent, { detail: profile }))
}

// 头像 URL：?v=托管文件名 做版本标识，头像一换 URL 就变，天然绕过浏览器缓存
export function avatarUrl(accountId: number, storedName: string): string {
  return `/api/user/${accountId}/avatar?v=${encodeURIComponent(storedName)}`
}