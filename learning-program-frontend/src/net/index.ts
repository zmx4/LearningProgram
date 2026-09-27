import axios, { type AxiosRequestConfig, isAxiosError } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const authItemName = 'authorize'

interface ApiResponse<T> {
    code: number
    data: T
    message: string
}

interface AuthStorage {
    token: string
    expire: string | number | Date
}

interface LoginResponse {
    token: string
    expireTime: string | number | Date
    username: string
}

type SuccessCallback<T> = (data: T) => void
type FailureCallback = (message: string, status?: number, url?: string) => void
type ErrorCallback = (error: unknown) => void
type Headers = AxiosRequestConfig['headers']

const accessHeader = (): Headers => {
    const token = takeAccessToken()
    return token ? { Authorization: `Bearer ${token}` } : {}
}

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

function takeAccessToken(): string | null {
    const str = localStorage.getItem(authItemName) ?? sessionStorage.getItem(authItemName)
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

function storeAccessToken(
    remember: boolean,
    token: string,
    expire: string | number | Date,
): void {
    const authObj: AuthStorage = { token, expire }
    const storage = remember ? localStorage : sessionStorage
    storage.setItem(authItemName, JSON.stringify(authObj))
}

function deleteAccessToken(redirect = false): void {
    localStorage.removeItem(authItemName)
    sessionStorage.removeItem(authItemName)

    if (redirect) {
        void router.push({ name: 'welcome-login' })
    }
}

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
            storeAccessToken(remember, data.token, data.expireTime)
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

function publicGet<T>(
    url: string,
    success: SuccessCallback<T>,
    failure: FailureCallback = defaultFailure,
    error: ErrorCallback = defaultError,
): void {
    internalGet<T>(url, {}, success, failure, error)
}

function unauthorized(): boolean {
    return !takeAccessToken()
}

export { post, publicPost, get, publicGet, login, logout, unauthorized }
