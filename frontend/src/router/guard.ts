import router from './index'
import store from '@/store'
import { getToken } from '@/utils/auth'
import type { NavigationGuardNext, RouteLocationNormalized } from 'vue-router'

const WHITE_LIST = ['/login', '/404']

router.beforeEach(async (to: RouteLocationNormalized, from: RouteLocationNormalized, next: NavigationGuardNext) => {
  const token = getToken()
  // 设置页面标题
  const title = to.meta?.title
  document.title = title ? `${String(title)} - 无人机驾驶员管理后台` : '无人机驾驶员管理后台'

  if (token) {
    if (to.path === '/login') {
      next({ path: '/' })
      return
    }
    // 已登录但未拉取用户信息
    if (!store.state.user.userInfo) {
      try {
        const info = await store.dispatch('user/getInfo')
        // 依据后端菜单树过滤并注册动态路由（generateRoutes 内部会先卸载旧路由）
        await store.dispatch('permission/generateRoutes', info.menus || [])
        // 动态路由注册完成后重进当前路由（replace 避免重复执行守卫）
        next({ ...to, replace: true })
      } catch (e) {
        // token 失效或接口异常 -> 清空并回登录页
        await store.dispatch('user/logout')
        next(`/login?redirect=${encodeURIComponent(to.fullPath)}`)
      }
    } else {
      next()
    }
  } else {
    if (WHITE_LIST.includes(to.path)) {
      next()
    } else {
      next(`/login?redirect=${encodeURIComponent(to.fullPath)}`)
    }
  }
})

router.afterEach(() => {
  // 可在此埋点
})
