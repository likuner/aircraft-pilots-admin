import request from './request'

// ===== 用户管理 =====
export function listUsers(params?: Record<string, any>): Promise<ApiResult<any>> {
  return request.get('/system/users', { params })
}
export function getUser(id: number): Promise<ApiResult<any>> {
  return request.get(`/system/users/${id}`)
}
export function createUser(data: Record<string, any>): Promise<ApiResult<any>> {
  return request.post('/system/users', data)
}
export function updateUser(data: Record<string, any>): Promise<ApiResult<any>> {
  return request.put(`/system/users/${data.id}`, data)
}
export function deleteUser(id: number): Promise<ApiResult<any>> {
  return request.delete(`/system/users/${id}`)
}
export function resetUserPassword(id: number, password: string): Promise<ApiResult<any>> {
  return request.put(`/system/users/${id}/password`, { password })
}
export function assignRoles(id: number, roleIds: number[]): Promise<ApiResult<any>> {
  return request.put(`/system/users/${id}/roles`, roleIds)
}
export function getUserRoleIds(id: number): Promise<ApiResult<any>> {
  return request.get(`/system/users/${id}/roles`)
}

// ===== 角色管理 =====
export function listRoles(params?: Record<string, any>): Promise<ApiResult<any>> {
  return request.get('/system/roles', { params })
}
export function listAllRoles(): Promise<ApiResult<any>> {
  return request.get('/system/roles/all')
}
export function getRoleMenuIds(id: number): Promise<ApiResult<any>> {
  return request.get(`/system/roles/${id}/menus`)
}
export function createRole(data: Record<string, any>): Promise<ApiResult<any>> {
  return request.post('/system/roles', data)
}
export function updateRole(data: Record<string, any>): Promise<ApiResult<any>> {
  return request.put(`/system/roles/${data.id}`, data)
}
export function deleteRole(id: number): Promise<ApiResult<any>> {
  return request.delete(`/system/roles/${id}`)
}
export function assignMenus(id: number, menuIds: number[]): Promise<ApiResult<any>> {
  return request.put(`/system/roles/${id}/menus`, menuIds)
}

// ===== 菜单管理 =====
export function listMenus(): Promise<ApiResult<any>> {
  return request.get('/system/menus')
}
export function createMenu(data: Record<string, any>): Promise<ApiResult<any>> {
  return request.post('/system/menus', data)
}
export function updateMenu(data: Record<string, any>): Promise<ApiResult<any>> {
  return request.put(`/system/menus/${data.id}`, data)
}
export function deleteMenu(id: number): Promise<ApiResult<any>> {
  return request.delete(`/system/menus/${id}`)
}

// ===== 操作日志 =====
export function listLogs(params?: Record<string, any>): Promise<ApiResult<any>> {
  return request.get('/system/logs', { params })
}

// ===== 公告 =====
export function listNotices(params?: Record<string, any>): Promise<ApiResult<any>> {
  return request.get('/system/notices', { params })
}
export function createNotice(data: Record<string, any>): Promise<ApiResult<any>> {
  return request.post('/system/notices', data)
}
export function updateNotice(data: Record<string, any>): Promise<ApiResult<any>> {
  return request.put(`/system/notices/${data.id}`, data)
}
export function deleteNotice(id: number): Promise<ApiResult<any>> {
  return request.delete(`/system/notices/${id}`)
}
export function publishNotice(id: number): Promise<ApiResult<any>> {
  return request.put(`/system/notices/${id}/publish`)
}
export function unpublishNotice(id: number): Promise<ApiResult<any>> {
  return request.put(`/system/notices/${id}/unpublish`)
}
