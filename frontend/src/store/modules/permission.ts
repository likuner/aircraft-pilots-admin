import { constantRoutes, asyncRoutes } from '@/router'
import router from '@/router'
import type { RouteRecordRaw } from 'vue-router'

// 后端菜单节点（与 sys_menu 表字段对应）
export interface MenuNode {
  id: number
  menuName: string
  path: string
  icon?: string
  perms?: string
  menuType?: string // M:目录 C:菜单 F:按钮
  children?: MenuNode[]
}

export interface PermissionState {
  routes: RouteRecordRaw[] // 完整路由（静态 + 动态）
  menus: MenuNode[] // 侧边栏菜单树
  addedRoutes: RouteRecordRaw[]
}

const state: PermissionState = {
  routes: [],
  menus: [],
  addedRoutes: []
}

type CommitFn = (mutation: string, payload?: unknown) => void

const mutations = {
  SET_ROUTES: (state: PermissionState, routes: RouteRecordRaw[]) => {
    state.routes = routes
  },
  SET_ADDED_ROUTES: (state: PermissionState, routes: RouteRecordRaw[]) => {
    state.addedRoutes = routes
  },
  SET_MENUS: (state: PermissionState, menus: MenuNode[]) => {
    state.menus = menus
  }
}

// 已注册的动态路由移除函数（vue-router addRoute 返回的卸载回调）
let removeRouteFns: Array<() => void> = []

const actions = {
  // 根据后端菜单树生成动态路由
  async generateRoutes({ commit }: { commit: CommitFn }, menus: MenuNode[]) {
    // 先卸载上一次注册的动态路由，防止重复注册/路由叠加
    removeRouteFns.forEach((fn) => fn())
    removeRouteFns = []

    const accessible = filterAsyncRoutes(asyncRoutes, menus)
    accessible.forEach((route) => {
      removeRouteFns.push(router.addRoute(route))
    })
    commit('SET_ADDED_ROUTES', accessible)
    commit('SET_ROUTES', constantRoutes.concat(accessible))
    commit('SET_MENUS', menus)
    return accessible
  },

  // 重置动态路由与菜单状态（登出时调用）
  resetRoutes({ commit }: { commit: CommitFn }) {
    removeRouteFns.forEach((fn) => fn())
    removeRouteFns = []
    commit('SET_ADDED_ROUTES', [])
    commit('SET_ROUTES', constantRoutes)
    commit('SET_MENUS', [])
  }
}

// 依据后端权限标识过滤异步路由表
function filterAsyncRoutes(routes: RouteRecordRaw[], menus: MenuNode[]): RouteRecordRaw[] {
  const perms = collectPerms(menus)
  const res: RouteRecordRaw[] = []
  routes.forEach((route) => {
    const tmp: RouteRecordRaw = { ...route }
    const permVal = tmp.meta?.permission
    if (permVal) {
      const need = String(permVal).split(',')
      const hasPerm = perms.includes('*:*:*') || perms.some((p) => need.includes(p))
      if (!hasPerm) return
    }
    if (tmp.children) {
      tmp.children = filterAsyncRoutes(tmp.children, menus)
      // 子路由全部被过滤且父级本身无权限要求 → 父级一并丢弃，避免注册空壳路由
      if (tmp.children.length === 0 && !permVal) return
    }
    res.push(tmp)
  })
  return res
}

// 收集后端菜单里的权限码
function collectPerms(menus: MenuNode[]): string[] {
  const perms: string[] = []
  const walk = (list: MenuNode[]) => {
    list.forEach((m) => {
      if (m.perms) perms.push(m.perms)
      if (m.children && m.children.length) walk(m.children)
    })
  }
  walk(menus || [])
  return perms
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}
