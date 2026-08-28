import request from './request'

// ===== 考生档案 =====
export function listProfiles(params?: Record<string, any>): Promise<ApiResult<any>> {
  return request.get('/student/profiles', { params })
}
export function getProfile(id: number): Promise<ApiResult<any>> {
  return request.get(`/student/profiles/${id}`)
}
export function getProfileRecords(id: number): Promise<ApiResult<any>> {
  return request.get(`/student/profiles/${id}/records`)
}
export function createProfile(data: Record<string, any>): Promise<ApiResult<any>> {
  return request.post('/student/profiles', data)
}
export function updateProfile(data: Record<string, any>): Promise<ApiResult<any>> {
  return request.put(`/student/profiles/${data.id}`, data)
}
export function deleteProfile(id: number): Promise<ApiResult<any>> {
  return request.delete(`/student/profiles/${id}`)
}
