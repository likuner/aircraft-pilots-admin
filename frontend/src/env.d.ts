/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}

// 后端统一响应结构
interface ApiResult<T = any> {
  code: number
  msg: string
  data: T
}
