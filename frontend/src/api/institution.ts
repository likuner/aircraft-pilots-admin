import request from './request'

// ===== 机构管理 =====
export function listInstitutions(params?: Record<string, any>): Promise<ApiResult<any>> {
  return request.get('/institution/institutions', { params })
}
export function getInstitution(id: number): Promise<ApiResult<any>> {
  return request.get(`/institution/institutions/${id}`)
}
export function createInstitution(data: Record<string, any>): Promise<ApiResult<any>> {
  return request.post('/institution/institutions', data)
}
export function updateInstitution(data: Record<string, any>): Promise<ApiResult<any>> {
  return request.put(`/institution/institutions/${data.id}`, data)
}
export function deleteInstitution(id: number): Promise<ApiResult<any>> {
  return request.delete(`/institution/institutions/${id}`)
}

// ===== 认证申请 =====
export function listApplications(params?: Record<string, any>): Promise<ApiResult<any>> {
  return request.get('/institution/applications', { params })
}
export function getApplication(id: number): Promise<ApiResult<any>> {
  return request.get(`/institution/applications/${id}`)
}
export function createApplication(data: Record<string, any>): Promise<ApiResult<any>> {
  return request.post('/institution/applications', data)
}
export function submitMaterials(id: number, materials: any[]): Promise<ApiResult<any>> {
  return request.put(`/institution/applications/${id}/materials`, materials)
}
/** 材料审查：result=PASS/REJECT, reviewStep 轮次 */
export function reviewMaterial(id: number, data: Record<string, any>): Promise<ApiResult<any>> {
  return request.put(`/institution/applications/${id}/review-material`, data)
}
/** 指派现场核查 */
export function assignInspection(id: number, data: Record<string, any>): Promise<ApiResult<any>> {
  return request.put(`/institution/applications/${id}/assign-inspection`, data)
}
/** 资质评定（result=PASS 后自动发证） */
export function qualifyApplication(id: number, data: Record<string, any>): Promise<ApiResult<any>> {
  return request.put(`/institution/applications/${id}/qualify`, data)
}

// ===== 现场核查 =====
export function listInspections(params?: Record<string, any>): Promise<ApiResult<any>> {
  return request.get('/institution/inspections', { params })
}
export function getInspection(id: number): Promise<ApiResult<any>> {
  return request.get(`/institution/inspections/${id}`)
}
/** 完成核查：result=PASS/FAIL */
export function completeInspection(id: number, data: Record<string, any>): Promise<ApiResult<any>> {
  return request.put(`/institution/inspections/${id}/complete`, data)
}

// ===== 资质证书 =====
export function listQualifications(params?: Record<string, any>): Promise<ApiResult<any>> {
  return request.get('/institution/qualifications', { params })
}
export function getQualification(id: number): Promise<ApiResult<any>> {
  return request.get(`/institution/qualifications/${id}`)
}
export function renewQualification(id: number): Promise<ApiResult<any>> {
  return request.put(`/institution/qualifications/${id}/renew`)
}
export function revokeQualification(id: number, reason: string, changeType = 'REVOKE'): Promise<ApiResult<any>> {
  return request.put(`/institution/qualifications/${id}/revoke`, { reason, changeType })
}
