import request from './request'

// ===== 证书申请 =====
export function listApply(params?: Record<string, any>): Promise<ApiResult<any>> {
  return request.get('/cert/applications', { params })
}
export function createApply(data: Record<string, any>): Promise<ApiResult<any>> {
  return request.post('/cert/applications', data)
}
export function auditApply(id: number, data: Record<string, any>): Promise<ApiResult<any>> {
  return request.put(`/cert/applications/${id}/audit`, data)
}

// ===== 合格证 =====
export function listCertificates(params?: Record<string, any>): Promise<ApiResult<any>> {
  return request.get('/cert/certificates', { params })
}
export function getCertificate(id: number): Promise<ApiResult<any>> {
  return request.get(`/cert/certificates/${id}`)
}
export function getCertificateChanges(id: number): Promise<ApiResult<any>> {
  return request.get(`/cert/certificates/${id}/changes`)
}
export function reissueCertificate(id: number, reason: string): Promise<ApiResult<any>> {
  return request.put(`/cert/certificates/${id}/reissue`, { reason })
}
export function revokeCertificate(id: number, reason: string, changeType = 'REVOKE'): Promise<ApiResult<any>> {
  return request.put(`/cert/certificates/${id}/revoke`, { reason, changeType })
}
