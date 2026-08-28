// 后端菜单树 -> 前端路由的工具函数
// 后端菜单表 sys_menu 的 component 字段形如 "system/user/index"，映射到 views 目录
// 前端异步路由在 permission store 中已按权限码过滤，这里将后端菜单树转换为侧边栏渲染数据

import type { MenuNode } from '@/store/modules/permission'

export interface MenuView {
  id: number
  name: string
  path: string
  icon?: string
  perms?: string
  type?: string
  children: MenuView[]
}

export function convertMenus(menus: MenuNode[]): MenuView[] {
  return menus.map((m) => ({
    id: m.id,
    name: m.menuName,
    path: m.path,
    icon: m.icon,
    perms: m.perms,
    type: m.menuType, // M:目录 C:菜单 F:按钮
    children: m.children && m.children.length ? convertMenus(m.children) : []
  }))
}
