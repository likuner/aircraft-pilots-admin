import { login as loginApi, logout as logoutApi, getInfo } from '@/api/auth'
import type { LoginForm, UserInfo } from '@/api/auth'
import { getToken, setToken, setRefreshToken, clearTokens } from '@/utils/auth'

export interface UserState {
  token: string | null
  userInfo: UserInfo | null
  roles: string[]
  permissions: string[]
}

const state: UserState = {
  token: getToken(),
  userInfo: null,
  roles: [],
  permissions: []
}

type CommitFn = (mutation: string, payload?: unknown) => void

const mutations = {
  SET_TOKEN: (state: UserState, token: string) => {
    state.token = token
  },
  SET_USER_INFO: (state: UserState, userInfo: UserInfo) => {
    state.userInfo = userInfo
  },
  SET_ROLES: (state: UserState, roles: string[]) => {
    state.roles = roles
  },
  SET_PERMISSIONS: (state: UserState, permissions: string[]) => {
    state.permissions = permissions
  }
}

const actions = {
  // 登录
  async login({ commit }: { commit: CommitFn }, loginForm: LoginForm) {
    const res = await loginApi(loginForm)
    const { accessToken, refreshToken } = res.data
    setToken(accessToken)
    setRefreshToken(refreshToken)
    commit('SET_TOKEN', accessToken)
    return res
  },

  // 获取用户信息（角色/权限/菜单）
  async getInfo({ commit }: { commit: CommitFn }) {
    const res = await getInfo()
    const { roles, permissions, menus } = res.data
    commit('SET_USER_INFO', res.data)
    commit('SET_ROLES', roles || [])
    commit('SET_PERMISSIONS', permissions || [])
    return res.data
  },

  // 登出
  async logout({ commit, dispatch }: { commit: CommitFn; dispatch: (action: string, payload?: unknown, options?: object) => Promise<void> }) {
    try {
      await logoutApi()
    } catch (e) {
      // 忽略登出接口错误
    }
    clearTokens()
    commit('SET_TOKEN', '')
    commit('SET_USER_INFO', null)
    commit('SET_ROLES', [])
    commit('SET_PERMISSIONS', [])
    // 卸载动态路由、清空菜单状态
    dispatch('permission/resetRoutes', null, { root: true })
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}
