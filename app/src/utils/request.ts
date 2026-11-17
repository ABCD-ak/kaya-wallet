import axios, {
  type AxiosError,
  type AxiosRequestConfig,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from 'axios'
import { ElMessage } from 'element-plus'
import 'element-plus/es/components/message/style/css'

import type { AuthTokens } from '@/api/types'
import {
  clearAuthStorage,
  getRefreshToken,
  getToken,
  setRefreshToken,
  setToken,
} from './auth'

/** 后端统一响应结构 */
export interface ApiResult<T = unknown> {
  code: number
  message: string
  data: T
}

export type ApiPayload<T> = ApiResult<T> | T

declare module 'axios' {
  export interface AxiosRequestConfig {
    /** 已尝试过刷新令牌，避免死循环 */
    _retry?: boolean
  }
}

const SUCCESS_CODES = [0, 200]
const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'
// 避免 LocalTunnel 将浏览器 API 请求拦截为 511 验证页。
const LOCALTUNNEL_HEADERS = BASE_URL.includes('.loca.lt')
  ? { 'Bypass-Tunnel-Reminder': 'true' }
  : undefined

/** 登录态失效事件，应用层监听后跳转登录页 */
export const AUTH_UNAUTHORIZED_EVENT = 'auth:unauthorized'
/** 刷新令牌成功事件，应用层用它同步 Pinia / localStorage */
export const AUTH_REFRESHED_EVENT = 'auth:refreshed'

export class ApiError extends Error {
  code: number

  constructor(code: number, message?: string) {
    super(message || '请求失败')
    this.name = 'ApiError'
    this.code = code
  }
}

const service = axios.create({
  baseURL: BASE_URL,
  timeout: 15_000,
  headers: LOCALTUNNEL_HEADERS,
})

/** 刷新令牌专用实例：不挂拦截器，避免 401 递归 */
const refreshClient = axios.create({
  baseURL: BASE_URL,
  timeout: 15_000,
  headers: LOCALTUNNEL_HEADERS,
})

service.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

let unauthorizedNotified = false
let refreshPromise: Promise<string> | null = null

function notifyUnauthorized() {
  if (unauthorizedNotified) return
  unauthorizedNotified = true
  clearAuthStorage()
  ElMessage.error('登录状态已失效，请重新登录')
  window.dispatchEvent(new CustomEvent(AUTH_UNAUTHORIZED_EVENT))
  window.setTimeout(() => {
    unauthorizedNotified = false
  }, 3_000)
}

function isAuthEndpoint(url?: string): boolean {
  return !!url && url.startsWith('/auth/')
}

function isApiResult<T>(payload: unknown): payload is ApiResult<T> {
  return !!payload && typeof payload === 'object' && 'code' in payload
}

async function requestNewTokens(): Promise<string> {
  const refreshToken = getRefreshToken()
  if (!refreshToken) throw new ApiError(40102, '没有可用的刷新令牌')

  const { data } = await refreshClient.post<ApiResult<AuthTokens>>('/auth/refresh', {
    refreshToken,
  })
  if (!isApiResult<AuthTokens>(data) || !SUCCESS_CODES.includes(data.code) || !data.data?.accessToken) {
    throw new ApiError(data?.code ?? 40102, data?.message)
  }

  setToken(data.data.accessToken)
  setRefreshToken(data.data.refreshToken)
  window.dispatchEvent(new CustomEvent<AuthTokens>(AUTH_REFRESHED_EVENT, { detail: data.data }))
  return data.data.accessToken
}

function refreshAccessToken(): Promise<string> {
  if (!refreshPromise) {
    refreshPromise = requestNewTokens().finally(() => {
      refreshPromise = null
    })
  }
  return refreshPromise
}

function showRequestError(error: AxiosError<ApiResult>) {
  const status = error.response?.status
  if (error.code === 'ECONNABORTED') {
    ElMessage.error('请求超时，请稍后重试')
    return
  }
  const message =
    error.response?.data?.message ??
    (status ? `请求失败（${status}）` : '网络异常，请检查网络后重试')
  ElMessage.error(message)
}

service.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiResult>) => {
    const status = error.response?.status
    const config = error.config

    if (status === 401 && config) {
      const authEndpoint = isAuthEndpoint(config.url)
      if (!authEndpoint && !config._retry && getRefreshToken()) {
        config._retry = true
        try {
          const token = await refreshAccessToken()
          config.headers?.set('Authorization', `Bearer ${token}`)
          return service(config)
        } catch (refreshError) {
          notifyUnauthorized()
          return Promise.reject(refreshError)
        }
      }

      if (authEndpoint) {
        // 登录/注册失败是业务错误，直接展示后端 message，不跳转
        showRequestError(error)
      } else {
        notifyUnauthorized()
      }
      return Promise.reject(error)
    }

    showRequestError(error)
    return Promise.reject(error)
  },
)

/** 拆统一响应结构；业务码非成功时提示并抛出 ApiError */
function unwrap<T>(promise: Promise<AxiosResponse<ApiPayload<T>>>): Promise<T> {
  return promise.then((response) => {
    const payload = response.data
    if (isApiResult<T>(payload)) {
      if (SUCCESS_CODES.includes(payload.code)) return payload.data
      ElMessage.error(payload.message || '请求失败')
      throw new ApiError(payload.code, payload.message)
    }
    return payload
  })
}

export const request = {
  get<T = unknown>(
    url: string,
    params?: Record<string, unknown>,
    config?: AxiosRequestConfig,
  ): Promise<T> {
    return unwrap<T>(service.get<ApiPayload<T>>(url, { ...config, params }))
  },

  post<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return unwrap<T>(service.post<ApiPayload<T>>(url, data, config))
  },

  put<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return unwrap<T>(service.put<ApiPayload<T>>(url, data, config))
  },

  patch<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return unwrap<T>(service.patch<ApiPayload<T>>(url, data, config))
  },

  delete<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return unwrap<T>(service.delete<ApiPayload<T>>(url, config))
  },

  /** 文件下载：直接返回 Blob */
  download(url: string, params?: Record<string, unknown>, config?: AxiosRequestConfig): Promise<Blob> {
    return service
      .get<Blob>(url, { ...config, params, responseType: 'blob' })
      .then((response) => response.data)
  },

  /** 原始 axios 实例，处理特殊场景时使用 */
  raw: service,
}

export default request
