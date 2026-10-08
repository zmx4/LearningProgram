/**
 * SSR 安全的本地存储访问。
 *
 * 服务端渲染时没有 window，直接调用 localStorage 会抛 `localStorage is not defined`。
 * 这里统一兜底：读返回 null、写静默忽略，于是组件 setup 与路由守卫在 Node 里也能跑通，
 * 浏览器里的行为与直接调用 localStorage / sessionStorage 完全一致。
 *
 * 注意：因此**服务端只能渲染「未登录」状态** —— token 存在浏览器本地，服务端看不到。
 * 这也是目前只预渲染登录/注册这类公开页的原因。
 */
export const isBrowser =
  typeof window !== 'undefined' && typeof window.localStorage !== 'undefined'

function read(storage: 'local' | 'session', key: string): string | null {
  if (!isBrowser) return null
  try {
    return storage === 'local' ? window.localStorage.getItem(key) : window.sessionStorage.getItem(key)
  } catch {
    // 浏览器禁用存储（隐私模式等）时不应让页面崩掉
    return null
  }
}

function write(storage: 'local' | 'session', key: string, value: string): void {
  if (!isBrowser) return
  try {
    if (storage === 'local') window.localStorage.setItem(key, value)
    else window.sessionStorage.setItem(key, value)
  } catch {
    // 写入失败（配额或权限）时忽略，不阻塞业务流程
  }
}

function remove(storage: 'local' | 'session', key: string): void {
  if (!isBrowser) return
  try {
    if (storage === 'local') window.localStorage.removeItem(key)
    else window.sessionStorage.removeItem(key)
  } catch {
    // 同上
  }
}

export function readLocal(key: string): string | null {
  return read('local', key)
}

export function readSession(key: string): string | null {
  return read('session', key)
}

/** 先看持久存储再看会话存储，与原先 `localStorage ?? sessionStorage` 的取值顺序一致。 */
export function readLocalOrSession(key: string): string | null {
  return readLocal(key) ?? readSession(key)
}

export function writeLocal(key: string, value: string): void {
  write('local', key, value)
}

export function writeSession(key: string, value: string): void {
  write('session', key, value)
}

/** 退出登录等场景需要把两处都清掉。 */
export function removeFromBoth(key: string): void {
  remove('local', key)
  remove('session', key)
}
