import { createStore } from 'vuex'
import type { Store } from 'vuex'
import user from './modules/user'
import type { UserState } from './modules/user'
import permission from './modules/permission'
import type { PermissionState } from './modules/permission'
import app from './modules/app'
import type { AppState } from './modules/app'

// Vuex：全局基础设施（token / 用户 / 权限 / 菜单 / 布局状态）
export interface RootState {
  user: UserState
  permission: PermissionState
  app: AppState
}

export default createStore<RootState>({
  modules: {
    user,
    permission,
    app
  }
}) as Store<RootState>
