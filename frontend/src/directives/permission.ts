import type { Directive, DirectiveBinding } from 'vue'
import store from '@/store'

// 按钮级权限指令：v-hasPermi="'system:user:add'"
// 支持数组形式：v-hasPermi="['system:user:add','system:user:edit']"（任一命中即显示）
export const hasPermi: Directive<HTMLElement, string | string[]> = {
  mounted(el: HTMLElement, binding: DirectiveBinding<string | string[]>) {
    const { value } = binding
    const permissions: string[] = (store.state as any).user.permissions || []
    const allPerm = permissions.includes('*:*:*')

    if (value && Array.isArray(value) && value.length > 0) {
      const hasPermission = allPerm || permissions.some((p) => value.includes(p))
      if (!hasPermission) {
        el.parentNode && el.parentNode.removeChild(el)
      }
    } else if (typeof value === 'string' && value.length > 0) {
      const hasPermission = allPerm || permissions.includes(value)
      if (!hasPermission) {
        el.parentNode && el.parentNode.removeChild(el)
      }
    } else {
      throw new Error('v-hasPermi 需要权限标识字符串或数组，如 v-hasPermi="system:user:add"')
    }
  }
}
