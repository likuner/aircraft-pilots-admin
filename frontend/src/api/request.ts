import axios from 'axios'
import type { AxiosRequestConfig, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { getToken, getRefreshToken, setToken, setRefreshToken, clearTokens } from '@/utils/auth'

// 创建 axios 实例
const service = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// 请求拦截器：注入 Bearer token
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error: unknown) => Promise.reject(error)
)

// 是否正在刷新 token
let isRefreshing = false
// 刷新期间的排队请求
let pendingQueue: Array<{ resolve: () => void; reject: (e: unknown) => void }> = []

function flushQueue(error: unknown = null) {
  pendingQueue.forEach(({ resolve, reject }) => (error ? reject(error) : resolve()))
  pendingQueue = []
}

interface RefreshTokenData {
  accessToken: string
  refreshToken: string
}

// 刷新 token
async function refreshToken(): Promise<string> {
  const refreshToken = getRefreshToken()
  if (!refreshToken) throw new Error('no refresh token')
  const { data } = await axios.post<ApiResult<RefreshTokenData>>('/api/auth/refresh', { refreshToken })
  setToken(data.data.accessToken)
  setRefreshToken(data.data.refreshToken)
  return data.data.accessToken
}

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data as ApiResult
    // 后端统一包装 { code, msg, data }
    if (res.code !== 0 && res.code !== 200) {
      ElMessage.error(res.msg || '请求失败')
      if (res.code === 401) {
        handleUnauthorized()
      }
      return Promise.reject(new Error(res.msg || 'Request Error'))
    }
    return res as unknown as AxiosResponse
  },
  async (error: any) => {
    const status = error.response?.status
    const res = error.response?.data
    if (status === 401) {
      // 尝试用 refresh token 续期
      if (!isRefreshing) {
        isRefreshing = true
        try {
          const newToken = await refreshToken()
          flushQueue()
          // 重放原请求
          error.config.headers.Authorization = `Bearer ${newToken}`
          return service(error.config)
        } catch (e) {
          flushQueue(e)
          clearTokens()
          ElMessage.error('登录已过期，请重新登录')
          router.push(`/login?redirect=${encodeURIComponent(router.currentRoute.value.fullPath)}`)
          return Promise.reject(e)
        } finally {
          isRefreshing = false
        }
      } else {
        // 刷新中，排队等待
        return new Promise((resolve, reject) => {
          pendingQueue.push({
            resolve: () => {
              error.config.headers.Authorization = `Bearer ${getToken()}`
              resolve(service(error.config))
            },
            reject
          })
        })
      }
    }
    if (status === 403) {
      ElMessage.error(res?.msg || '没有权限执行该操作')
    } else {
      ElMessage.error(res?.msg || error.message || '网络异常')
    }
    return Promise.reject(error)
  }
)

function handleUnauthorized() {
  clearTokens()
  router.push(`/login?redirect=${encodeURIComponent(router.currentRoute.value.fullPath)}`)
}

// 统一导出带泛型的请求方法，业务层拿到 Promise<ApiResult<T>> 可直接取 .data
export default service as unknown as {
  <T = any>(config: AxiosRequestConfig): Promise<ApiResult<T>>
  get<T = any>(url: string, config?: AxiosRequestConfig): Promise<ApiResult<T>>
  post<T = any>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<ApiResult<T>>
  put<T = any>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<ApiResult<T>>
  delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<ApiResult<T>>
}
