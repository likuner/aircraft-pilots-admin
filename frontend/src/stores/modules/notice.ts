import { defineStore } from 'pinia'

// 公告域业务状态（Pinia）
export interface ListQuery {
  page: number
  size: number
  status: string
  keyword: string
}

export const useNoticeStore = defineStore('notice', {
  state: () => ({
    listQuery: { page: 1, size: 10, status: '', keyword: '' } as ListQuery,
    list: [] as any[],
    total: 0
  }),
  actions: {
    resetQuery() {
      this.listQuery = { page: 1, size: 10, status: '', keyword: '' }
    },
    setList(list: any[], total: number) {
      this.list = list
      this.total = total
    }
  }
})
