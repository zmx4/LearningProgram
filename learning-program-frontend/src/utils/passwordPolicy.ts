/**
 * 密码强度策略。
 *
 * 规则由后端 PasswordPolicy 责任链决定，并通过 GET /api/auth/password-policy 暴露
 * （规则码 code、说明 message、参数 min/max）。前端只按「规则码」做即时校验，
 * 阈值与开关一律以后端返回为准 —— 后端调整配置后前端不用改；提交时服务端仍会再校验一次。
 *
 * 说明：`not-common`（弱密码词表）只在服务端校验，前端不做包含判断，
 * 未知规则码一律视为通过，避免前端错误拦截。
 */
import { publicGet } from '@/net'

export interface PasswordRequirement {
  code: string
  message: string
  min?: number
  max?: number
}

export interface PasswordPolicy {
  minLength: number
  maxLength: number
  requirements: PasswordRequirement[]
}

/** 后端默认策略的副本，用于请求未返回前的占位，页面不至于空白。 */
export const fallbackPasswordPolicy: PasswordPolicy = {
  minLength: 8,
  maxLength: 32,
  requirements: [
    { code: 'length', message: '长度需在 8 到 32 个字符之间', min: 8, max: 32 },
    { code: 'require-uppercase', message: '至少包含一个大写字母' },
    { code: 'require-lowercase', message: '至少包含一个小写字母' },
    { code: 'require-digit', message: '至少包含一个数字' },
    { code: 'require-special', message: '至少包含一个特殊字符' },
    { code: 'no-whitespace', message: '不能包含空格等空白字符' },
    { code: 'no-account-info', message: '不能包含用户名或邮箱' },
    { code: 'not-common', message: '不能是常见弱密码' },
  ],
}

let cached: PasswordPolicy | null = null
let inflight: Promise<PasswordPolicy> | null = null

/** 读取策略，结果会被缓存；请求失败时回退到内置副本，不打扰用户。 */
export function loadPasswordPolicy(): Promise<PasswordPolicy> {
  if (cached) return Promise.resolve(cached)
  if (inflight) return inflight

  const settle = (policy: PasswordPolicy) => {
    cached = policy
    inflight = null
    return policy
  }

  inflight = new Promise<PasswordPolicy>((resolve) => {
    publicGet<PasswordPolicy>(
      '/api/auth/password-policy',
      data => resolve(settle(data)),
      () => resolve(settle(fallbackPasswordPolicy)),
      () => resolve(settle(fallbackPasswordPolicy)),
    )
  })
  return inflight
}

function containsAccountInfo(password: string, username: string, email: string): boolean {
  const value = password.toLowerCase()
  const name = username.trim().toLowerCase()
  if (name.length >= 3 && value.includes(name)) return true

  const mail = email.trim().toLowerCase()
  if (!mail) return false
  if (value.includes(mail)) return true
  const localPart = mail.split('@')[0] ?? ''
  return localPart.length >= 3 && value.includes(localPart)
}

function satisfies(
  requirement: PasswordRequirement,
  value: string,
  username: string,
  email: string,
): boolean {
  switch (requirement.code) {
    case 'length':
      return value.length >= (requirement.min ?? 0)
        && value.length <= (requirement.max ?? Number.MAX_SAFE_INTEGER)
    case 'require-uppercase':
      return /[A-Z]/.test(value)
    case 'require-lowercase':
      return /[a-z]/.test(value)
    case 'require-digit':
      return /[0-9]/.test(value)
    case 'require-special':
      return /[^A-Za-z0-9\s]/.test(value)
    case 'no-whitespace':
      return !/\s/.test(value)
    case 'no-account-info':
      return !containsAccountInfo(value, username, email)
    default:
      // 例如 not-common，交给服务端判断
      return true
  }
}

/**
 * 返回第一条不满足的规则说明，全部满足返回 null。
 *
 * @param account 当前账号信息，用于「不能包含用户名或邮箱」的即时校验
 */
export function firstUnmetRequirement(
  value: string,
  policy: PasswordPolicy,
  account: { username?: string, email?: string } = {},
): string | null {
  const username = account.username ?? ''
  const email = account.email ?? ''
  for (const requirement of policy.requirements) {
    if (!satisfies(requirement, value, username, email)) {
      return requirement.message
    }
  }
  return null
}
