/**
 * 网络请求统一封装。
 *
 * - 后端统一返回 ApiResponse（code/data/message），本模块负责解析并分发给回调
 * - 登录态（token + 过期时间 + 用户名 + 角色）保存在 localStorage / sessionStorage 的 authorize 项中
 * - 每次请求由 axios 拦截器自动注入 Authorization 头
 * - 受保护接口的 401 视为登录过期，清除本地登录态并跳转登录页；
 *   公开接口（登录、两步验证等）的 401 是认证失败，保留后端提示信息
 *
 * 业务代码使用底部导出的 post / get / put / del（自动带 token）
 * 与 publicPost / publicGet（匿名接口，不带头）。
 */
import axios, { type AxiosRequestConfig, isAxiosError } from 'axios'
import { ElMessage } from 'element-plus'
import type { Router } from 'vue-router'
import { loadSessionItem, loadStorageItem, removeSessionItem, removeStorageItem, storeSessionItem, storeStorageItem } from '@/utils/storage'

// 登录态在 storage 中的键名，值为序列化后的 AuthStorage
const authItemName = 'authorize'

// 当前应用挂载的路由实例：SSR 时每个请求各有一份，客户端只有一个。
// 通过 setNetRouter 注入，避免本模块反向依赖 router 单例（router 守卫要用本模块，会成环）。
let activeRouter: Router | null = null

function setNetRouter(router: Router): void {
    activeRouter = router
}

interface ApiResponse<T> {
    code: number
    data: T
    message: string
}

interface AuthStorage {
    token: string
    expire: string | number | Date
    id?: number
    username?: string
    role?: string
}

interface LoginResponse {
    id?: number
    token: string
    expireTime: string | number | Date
    username: string
    role: string
}

type SuccessCallback<T> = (data: T) => void
type FailureCallback = (message: string, status?: number, url?: string, data?: unknown) => void
type ErrorCallback = (error: unknown) => void
type Headers = AxiosRequestConfig['headers']

// 组装带 token 的请求头；无 token 时返回空对象
const accessHeader = (): Headers => {
    const token = takeAccessToken()
    if (!token) {
        return {}
    }
    return { Authorization: token.startsWith('Bearer ') ? token : `Bearer ${token}` }
}

axios.interceptors.request.use((config) => {
    const token = takeAccessToken()
    if (token) {
        config.headers.Authorization = token.startsWith('Bearer ') ? token : `Bearer ${token}`
    }
    return config
})

// 网络层错误（非业务失败）：统一弹出提示；429 展示后端限流消息
const defaultError: ErrorCallback = (error) => {
    console.error(error)

    if (isAxiosError(error) && error.response?.status === 429) {
        const message = (error.response.data as { message?: string } | undefined)?.message
        ElMessage.error(message || '请求过于频繁，请稍后再试')
    } else {
        ElMessage.error('发生了一些错误，请联系管理员')
    }
}

const defaultFailure: FailureCallback = (message, status, url) => {
    console.warn(`请求地址: ${url ?? '未知'}, 状态码: ${status ?? '未知'}, 错误信息: ${message}`)
    ElMessage.warning(message)
}

// 读取本地 token；不存在或已过期时清理本地登录态并提示，返回 null（SSR 下恒为 null）
function takeAccessToken(): string | null {
    const str = loadStorageItem(authItemName) ?? loadSessionItem(authItemName)
    if (!str) {
        return null
    }

    let authObj: AuthStorage
    try {
        authObj = JSON.parse(str) as AuthStorage
    } catch {
        deleteAccessToken()
        return null
    }

    if (!authObj.token || new Date(authObj.expire) <= new Date()) {
        deleteAccessToken()
        ElMessage.warning('登录状态已过期，请重新登录！')
        return null
    }

    return authObj.token
}

// 保存登录态：remember 为真写 localStorage（持久），否则写 sessionStorage（随会话）
function storeAccessToken(
    remember: boolean,
    token: string,
    expire: string | number | Date,
    username: string,
    role: string,
    id?: number,
): void {
    const authObj: AuthStorage = { token, expire, id, username, role }
    if (remember) {
        storeStorageItem(authItemName, JSON.stringify(authObj))
    } else {
        storeSessionItem(authItemName, JSON.stringify(authObj))
    }
}

// 清除登录态；redirect 为真时跳转登录页（401 过期场景，仅客户端需要跳转）
function deleteAccessToken(redirect = false): void {
    removeStorageItem(authItemName)
    removeSessionItem(authItemName)

    if (redirect && !import.meta.env.SSR) {
        void activeRouter?.push({ name: 'welcome-login' })
    }
}

// 解析统一响应体：code 200 分发数据；受保护接口的 401 清理登录态并跳登录页；其余把后端 message 交给 failure
function handleResponse<T>(
    response: ApiResponse<T>,
    url: string,
    success: SuccessCallback<T>,
    failure: FailureCallback,
    sessionExpiryOn401 = true,
): void {
    if (response.code === 200) {
        success(response.data)
    } else if (response.code === 401 && sessionExpiryOn401) {
        failure('登录状态已过期，请重新登录！')
        deleteAccessToken(true)
    } else {
        failure(response.message, response.code, url, response.data)
    }
}

function internalPost<T>(
    url: string,
    data: unknown,
    headers: Headers,
    success: SuccessCallback<T>,
    failure: FailureCallback,
    sessionExpiryOn401 = true,
    error: ErrorCallback = defaultError,
): void {
    // SSR 阶段不发请求：登录态只在浏览器侧，且 Node 的 axios 无法解析同源相对路径，
    // 页面数据统一由客户端水合后再拉取。回调（含 ElMessage 提示）也就不会在服务端触发。
    if (import.meta.env.SSR) return
    void axios
        .post<ApiResponse<T>>(url, data, { headers })
        .then(({ data: response }) => handleResponse(response, url, success, failure, sessionExpiryOn401))
        .catch((requestError) => {
            if (isAxiosError(requestError) && requestError.response?.data?.code) {
                handleResponse(requestError.response.data as ApiResponse<T>, url, success, failure, sessionExpiryOn401)
            } else {
                error(requestError)
            }
        })
}

function internalGet<T>(
    url: string,
    headers: Headers,
    success: SuccessCallback<T>,
    failure: FailureCallback,
    sessionExpiryOn401 = true,
    error: ErrorCallback = defaultError,
): void {
    // 同 internalPost：SSR 阶段不发请求
    if (import.meta.env.SSR) return
    void axios
        .get<ApiResponse<T>>(url, { headers })
        .then(({ data: response }) => handleResponse(response, url, success, failure, sessionExpiryOn401))
        .catch((requestError) => {
            if (isAxiosError(requestError) && requestError.response?.data?.code) {
                handleResponse(requestError.response.data as ApiResponse<T>, url, success, failure, sessionExpiryOn401)
            } else {
                error(requestError)
            }
        })
}

function internalPut<T>(
    url: string,
    data: unknown,
    headers: Headers,
    success: SuccessCallback<T>,
    failure: FailureCallback,
    error: ErrorCallback = defaultError,
): void {
    // 同 internalPost：SSR 阶段不发请求
    if (import.meta.env.SSR) return
    void axios
        .put<ApiResponse<T>>(url, data, { headers })
        .then(({ data: response }) => handleResponse(response, url, success, failure))
        .catch((requestError) => {
            if (isAxiosError(requestError) && requestError.response?.data?.code) {
                handleResponse(requestError.response.data as ApiResponse<T>, url, success, failure)
            } else {
                error(requestError)
            }
        })
}

function internalDelete<T>(
    url: string,
    data: unknown,
    headers: Headers,
    success: SuccessCallback<T>,
    failure: FailureCallback,
    error: ErrorCallback = defaultError,
): void {
    // 同 internalPost：SSR 阶段不发请求
    if (import.meta.env.SSR) return
    void axios
        .delete<ApiResponse<T>>(url, { data, headers })
        .then(({ data: response }) => handleResponse(response, url, success, failure))
        .catch((requestError) => {
            if (isAxiosError(requestError) && requestError.response?.data?.code) {
                handleResponse(requestError.response.data as ApiResponse<T>, url, success, failure)
            } else {
                error(requestError)
            }
        })
}

// 登录：表单编码提交给 Spring Security 的 /api/auth/login，成功后保存登录态
function login(
    username: string,
    password: string,
    remember: boolean,
    success: SuccessCallback<LoginResponse>,
    failure: FailureCallback = defaultFailure,
): void {
    internalPost<LoginResponse>(
        '/api/auth/login',
        { username, password },
        { 'Content-Type': 'application/x-www-form-urlencoded' },
        (data) => {
            storeAccessToken(remember, data.token, data.expireTime, data.username, data.role, data.id)
            ElMessage.success(`登录成功，欢迎 ${data.username} 来到我们的系统`)
            success(data)
        },
        failure,
        false,
    )
}

function verifyTotpLogin(
    challenge: string,
    code: string,
    remember: boolean,
    success: SuccessCallback<LoginResponse>,
    failure: FailureCallback = defaultFailure,
): void {
    publicPost<LoginResponse>('/api/2fa/login', { challenge, code }, (data) => {
        storeAccessToken(remember, data.token, data.expireTime, data.username, data.role, data.id)
        success(data)
    }, failure)
}

function post<T>(
    url: string,
    data: unknown,
    success: SuccessCallback<T>,
    failure: FailureCallback = defaultFailure,
): void {
    internalPost<T>(url, data, accessHeader(), success, failure)
}

function publicPost<T>(
    url: string,
    data: unknown,
    success: SuccessCallback<T>,
    failure: FailureCallback = defaultFailure,
): void {
    internalPost<T>(url, data, {}, success, failure, false)
}

// 登出：请求后端把 token 加入黑名单，同时清除本地登录态
function logout(success: () => void, failure: FailureCallback = defaultFailure): void {
    internalPost<null>(
        '/api/auth/logout',
        null,
        accessHeader(),
        () => {
            deleteAccessToken()
            ElMessage.success('退出登录成功，欢迎您再次使用')
            success()
        },
        failure,
    )
}

function get<T>(
    url: string,
    success: SuccessCallback<T>,
    failure: FailureCallback = defaultFailure,
): void {
    internalGet<T>(url, accessHeader(), success, failure)
}

function put<T>(
    url: string,
    data: unknown,
    success: SuccessCallback<T>,
    failure: FailureCallback = defaultFailure,
): void {
    internalPut<T>(url, data, accessHeader(), success, failure)
}

function del<T>(
    url: string,
    data: unknown,
    success: SuccessCallback<T>,
    failure: FailureCallback = defaultFailure,
): void {
    internalDelete<T>(url, data, accessHeader(), success, failure)
}

function publicGet<T>(
    url: string,
    success: SuccessCallback<T>,
    failure: FailureCallback = defaultFailure,
    error: ErrorCallback = defaultError,
): void {
    internalGet<T>(url, {}, success, failure, false, error)
}

// 路由守卫用：本地是否持有有效登录态（SSR 下恒为“未登录”视角）
function unauthorized(): boolean {
    return !takeAccessToken()
}

// 读取当前登录用户名（登录时缓存），未登录返回空串
function currentUsername(): string {
    return parseAuthStorage()?.username?.trim() ?? ''
}

// 读取当前登录角色（user / admin），供路由守卫判断 adminOnly 路由
function currentRole(): string {
    return parseAuthStorage()?.role?.trim() ?? ''
}

// 读取当前登录账号 id，未登录或旧登录态（无 id）返回 null
function currentUserId(): number | null {
    const id = parseAuthStorage()?.id
    return typeof id === 'number' ? id : null
}

// 解析本地登录态；无登录态或内容损坏返回 null（SSR 下恒为 null）
function parseAuthStorage(): AuthStorage | null {
    const str = loadStorageItem(authItemName) ?? loadSessionItem(authItemName)
    if (!str) return null
    try {
        return JSON.parse(str) as AuthStorage
    } catch {
        return null
    }
}

export {
    setNetRouter,
    post,
    publicPost,
    get,
    put,
    del,
    publicGet,
    login,
    verifyTotpLogin,
    logout,
    unauthorized,
    currentUsername,
    currentRole,
    currentUserId,
}
