/**
 * 网络请求统一封装。
 *
 * - 后端统一返回 ApiResponse（code/data/message），本模块负责解析并分发给回调
 * - 登录态（token + 过期时间 + 用户名 + 角色）保存在 localStorage / sessionStorage 的 authorize 项中
 * - 每次请求由 axios 拦截器自动注入 Authorization 头
 * - 401 视为登录过期，清除本地登录态并跳转登录页
 *
 * 业务代码使用底部导出的 post / get / put / del（自动带 token）
 * 与 publicPost / publicGet（匿名接口，不带头）。
 */
import axios, { type AxiosRequestConfig, isAxiosError } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { readLocalOrSession, removeFromBoth, writeLocal, writeSession } from '@/utils/storage'

// 登录态在 storage 中的键名，值为序列化后的 AuthStorage
const authItemName = 'authorize'

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
type FailureCallback = (message: string, status?: number, url?: string) => void
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

// 读取本地 token；不存在或已过期时清理本地登录态并提示，返回 null
function takeAccessToken(): string | null {
    const str = readLocalOrSession(authItemName)
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
    const serialized = JSON.stringify(authObj)
    if (remember) writeLocal(authItemName, serialized)
    else writeSession(authItemName, serialized)
}

// 清除登录态；redirect 为真时跳转登录页（401 过期场景）
function deleteAccessToken(redirect = false): void {
    removeFromBoth(authItemName)

    if (redirect) {
        void router.push({ name: 'welcome-login' })
    }
}

// 解析统一响应体：code 200 分发数据；401 清登录态并跳登录页；其余把后端 message 交给 failure
function handleResponse<T>(
    response: ApiResponse<T>,
    url: string,
    success: SuccessCallback<T>,
    failure: FailureCallback,
): void {
    if (response.code === 200) {
        success(response.data)
    } else if (response.code === 401) {
        failure('登录状态已过期，请重新登录！')
        deleteAccessToken(true)
    } else {
        failure(response.message, response.code, url)
    }
}

function internalPost<T>(
    url: string,
    data: unknown,
    headers: Headers,
    success: SuccessCallback<T>,
    failure: FailureCallback,
    error: ErrorCallback = defaultError,
): void {
    void axios
        .post<ApiResponse<T>>(url, data, { headers })
        .then(({ data: response }) => handleResponse(response, url, success, failure))
        .catch(error)
}

function internalGet<T>(
    url: string,
    headers: Headers,
    success: SuccessCallback<T>,
    failure: FailureCallback,
    error: ErrorCallback = defaultError,
): void {
    void axios
        .get<ApiResponse<T>>(url, { headers })
        .then(({ data: response }) => handleResponse(response, url, success, failure))
        .catch(error)
}

function internalPut<T>(
    url: string,
    data: unknown,
    headers: Headers,
    success: SuccessCallback<T>,
    failure: FailureCallback,
    error: ErrorCallback = defaultError,
): void {
    void axios
        .put<ApiResponse<T>>(url, data, { headers })
        .then(({ data: response }) => handleResponse(response, url, success, failure))
        .catch(error)
}

function internalDelete<T>(
    url: string,
    data: unknown,
    headers: Headers,
    success: SuccessCallback<T>,
    failure: FailureCallback,
    error: ErrorCallback = defaultError,
): void {
    void axios
        .delete<ApiResponse<T>>(url, { data, headers })
        .then(({ data: response }) => handleResponse(response, url, success, failure))
        .catch(error)
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
    )
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
    internalPost<T>(url, data, {}, success, failure)
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
    internalGet<T>(url, {}, success, failure, error)
}

// 路由守卫用：本地是否持有有效登录态
function unauthorized(): boolean {
    return !takeAccessToken()
}

/**
 * 本地是否存有登录态（不校验是否过期）。
 * 客户端入口用它判断预渲染出来的「未登录」DOM 能否直接复用。
 */
function hasStoredSession(): boolean {
    return readLocalOrSession(authItemName) !== null
}

// 读取当前登录用户名（登录时缓存），未登录返回空串
function currentUsername(): string {
    const str = readLocalOrSession(authItemName)
    if (!str) return ''
    try {
        const authObj = JSON.parse(str) as AuthStorage
        return authObj.username?.trim() ?? ''
    } catch {
        return ''
    }
}

// 读取当前登录角色（user / admin），供路由守卫判断 adminOnly 路由
function currentRole(): string {
    const str = readLocalOrSession(authItemName)
    if (!str) return ''
    try {
        const authObj = JSON.parse(str) as AuthStorage
        return authObj.role?.trim() ?? ''
    } catch {
        return ''
    }
}

// 读取当前登录账号 id，未登录或旧登录态（无 id）返回 null
function currentUserId(): number | null {
    const str = readLocalOrSession(authItemName)
    if (!str) return null
    try {
        const authObj = JSON.parse(str) as AuthStorage
        return typeof authObj.id === 'number' ? authObj.id : null
    } catch {
        return null
    }
}

export {
    post,
    publicPost,
    get,
    put,
    del,
    publicGet,
    login,
    logout,
    unauthorized,
    hasStoredSession,
    currentUsername,
    currentRole,
    currentUserId,
}
