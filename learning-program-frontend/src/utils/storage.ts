/**
 * SSR 安全的 Web Storage 访问封装。
 *
 * 服务端渲染时没有 localStorage / sessionStorage（Node 环境且按请求隔离），
 * 直接访问会抛 ReferenceError 并使整个渲染失败。这里统一收口：
 * - 服务端：读返回 null、写 / 删为空操作（SSR 恒按“无本地状态”的游客视角渲染）
 * - 客户端：行为与原生 storage 完全一致
 *
 * 项目内所有模块级 / setup 级的 storage 访问都应经由本模块，
 * 避免在服务端渲染路径上触发浏览器 API。
 */

function getStorage(kind: 'local' | 'session'): Storage | null {
    // typeof 探测未声明变量不会抛错，SSR 下两者均为 undefined
    if (typeof localStorage === 'undefined' || typeof sessionStorage === 'undefined') {
        return null
    }
    return kind === 'local' ? localStorage : sessionStorage
}

export function loadStorageItem(key: string): string | null {
    return getStorage('local')?.getItem(key) ?? null
}

export function storeStorageItem(key: string, value: string): void {
    getStorage('local')?.setItem(key, value)
}

export function removeStorageItem(key: string): void {
    getStorage('local')?.removeItem(key)
}

export function loadSessionItem(key: string): string | null {
    return getStorage('session')?.getItem(key) ?? null
}

export function storeSessionItem(key: string, value: string): void {
    getStorage('session')?.setItem(key, value)
}

export function removeSessionItem(key: string): void {
    getStorage('session')?.removeItem(key)
}
