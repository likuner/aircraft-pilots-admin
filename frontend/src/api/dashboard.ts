import request from './request'

// 首页统计
export function getDashboardStats(): Promise<ApiResult<any>> {
  return request.get('/dashboard/stats')
}

// 公告（门户视角，可选）
export function listPublicNotices(params?: Record<string, any>): Promise<ApiResult<any>> {
  return request.get('/dashboard/notices', { params })
}

// 最新公告（首页）
export function listLatestNotices(limit?: number): Promise<ApiResult<any>> {
  return request.get('/system/notices/latest', { params: { limit } })
}
