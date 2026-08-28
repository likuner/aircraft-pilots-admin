import request from './request'

// ===== 考试计划 =====
export function listPlans(params?: Record<string, any>): Promise<ApiResult<any>> {
  return request.get('/exam/plans', { params })
}
export function createPlan(data: Record<string, any>): Promise<ApiResult<any>> {
  return request.post('/exam/plans', data)
}
export function updatePlan(data: Record<string, any>): Promise<ApiResult<any>> {
  return request.put(`/exam/plans/${data.id}`, data)
}
export function deletePlan(id: number): Promise<ApiResult<any>> {
  return request.delete(`/exam/plans/${id}`)
}
export function publishPlan(id: number): Promise<ApiResult<any>> {
  return request.put(`/exam/plans/${id}/publish`)
}
export function cancelPlan(id: number): Promise<ApiResult<any>> {
  return request.put(`/exam/plans/${id}/cancel`)
}

// ===== 考场 =====
export function listRooms(params?: Record<string, any>): Promise<ApiResult<any>> {
  return request.get('/exam/rooms', { params })
}
export function createRoom(data: Record<string, any>): Promise<ApiResult<any>> {
  return request.post('/exam/rooms', data)
}
export function updateRoom(data: Record<string, any>): Promise<ApiResult<any>> {
  return request.put(`/exam/rooms/${data.id}`, data)
}
export function deleteRoom(id: number): Promise<ApiResult<any>> {
  return request.delete(`/exam/rooms/${id}`)
}

// ===== 考试场次 =====
export function listSessions(params?: Record<string, any>): Promise<ApiResult<any>> {
  return request.get('/exam/sessions', { params })
}
export function createSession(data: Record<string, any>): Promise<ApiResult<any>> {
  return request.post('/exam/sessions', data)
}
export function updateSession(data: Record<string, any>): Promise<ApiResult<any>> {
  return request.put(`/exam/sessions/${data.id}`, data)
}
export function deleteSession(id: number): Promise<ApiResult<any>> {
  return request.delete(`/exam/sessions/${id}`)
}
export function publishSession(id: number): Promise<ApiResult<any>> {
  return request.put(`/exam/sessions/${id}/publish`)
}
export function closeEnrollment(id: number): Promise<ApiResult<any>> {
  return request.put(`/exam/sessions/${id}/close-enrollment`)
}

// ===== 考试批次 =====
export function listBatches(params?: Record<string, any>): Promise<ApiResult<any>> {
  return request.get('/exam/batches', { params })
}
export function listBatchesBySession(sessionId: number): Promise<ApiResult<any>> {
  return request.get(`/exam/batches/session/${sessionId}`)
}
export function createBatch(data: Record<string, any>): Promise<ApiResult<any>> {
  return request.post('/exam/batches', data)
}
export function updateBatch(data: Record<string, any>): Promise<ApiResult<any>> {
  return request.put(`/exam/batches/${data.id}`, data)
}
export function deleteBatch(id: number): Promise<ApiResult<any>> {
  return request.delete(`/exam/batches/${id}`)
}

// ===== 报名管理 =====
export function listRegistrations(params?: Record<string, any>): Promise<ApiResult<any>> {
  return request.get('/exam/registrations', { params })
}
export function createRegistration(data: Record<string, any>): Promise<ApiResult<any>> {
  return request.post('/exam/registrations', data)
}
export function approveRegistration(id: number): Promise<ApiResult<any>> {
  return request.put(`/exam/registrations/${id}/approve`)
}
export function rejectRegistration(id: number, reason: string): Promise<ApiResult<any>> {
  return request.put(`/exam/registrations/${id}/reject`, { reason })
}
export function cancelRegistration(id: number): Promise<ApiResult<any>> {
  return request.put(`/exam/registrations/${id}/cancel`)
}
export function arrangeRegistration(id: number, batchId: number): Promise<ApiResult<any>> {
  return request.put(`/exam/registrations/${id}/arrange`, null, { params: { batchId } })
}

// ===== 成绩 =====
export function listScores(params?: Record<string, any>): Promise<ApiResult<any>> {
  return request.get('/exam/scores', { params })
}
export function createScore(data: Record<string, any>): Promise<ApiResult<any>> {
  return request.post('/exam/scores', data)
}
export function updateScore(data: Record<string, any>): Promise<ApiResult<any>> {
  return request.put(`/exam/scores/${data.id}`, data)
}
export function submitScore(id: number): Promise<ApiResult<any>> {
  return request.put(`/exam/scores/${id}/submit`)
}
export function auditScore(id: number, data: Record<string, any>): Promise<ApiResult<any>> {
  return request.put(`/exam/scores/${id}/audit`, data)
}
