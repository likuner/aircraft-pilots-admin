import { defineStore } from 'pinia'

// 考试域业务状态（Pinia）
export interface ListQuery {
  page: number
  size: number
  status: string
  keyword: string
}

export const useExamStore = defineStore('exam', {
  state: () => ({
    listQuery: { page: 1, size: 10, status: '', keyword: '' } as ListQuery,
    list: [] as any[],
    total: 0,
    selectedRows: [] as any[],
    currentSession: null as any
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
