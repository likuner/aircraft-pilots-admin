import request from './request'

// ===== 类型定义 =====
export interface LoginForm {
  username: string
  password: string
  captchaKey: string
  captchaCode: string
}

export interface CaptchaData {
  captchaKey: string
  img: string
}

export interface TokenData {
  accessToken: string
  refreshToken: string
}

export interface UserInfo {
  userId: number
  username: string
  nickname: string
  avatar?: string
  roles: string[]
  permissions: string[]
  menus: any[]
  [key: string]: any
}

// 获取图形验证码
export function getCaptcha(): Promise<ApiResult<CaptchaData>> {
  return request.get('/auth/captcha')
}

// 登录
export function login(data: LoginForm): Promise<ApiResult<TokenData>> {
  return request.post('/auth/login', data)
}

// 刷新 token
export function refreshToken(data: { refreshToken: string }): Promise<ApiResult<TokenData>> {
  return request.post('/auth/refresh', data)
}

// 登出
export function logout(): Promise<ApiResult<null>> {
  return request.post('/auth/logout')
}

// 获取当前用户信息（角色/权限/菜单）
export function getInfo(): Promise<ApiResult<UserInfo>> {
  return request.get('/auth/me')
}

// 修改密码
export function changePassword(data: { oldPassword: string; newPassword: string }): Promise<ApiResult<null>> {
  return request.put('/auth/password', data)
}
