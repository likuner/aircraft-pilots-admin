<template>
  <div class="page-card">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input
        v-model="queryParams.keyword"
        placeholder="操作人 / 操作内容"
        clearable
        class="!w-52"
        @keyup.enter="handleQuery"
      />
      <el-select v-model="queryParams.module" placeholder="模块" clearable class="!w-40">
        <el-option v-for="m in moduleOptions" :key="m" :label="m" :value="m" />
      </el-select>
      <el-select v-model="queryParams.status" placeholder="结果" clearable class="!w-28">
        <el-option label="成功" :value="1" />
        <el-option label="失败" :value="0" />
      </el-select>
      <el-date-picker
        v-model="dateRange"
        type="datetimerange"
        range-separator="至"
        start-placeholder="开始时间"
        end-placeholder="结束时间"
        value-format="YYYY-MM-DD HH:mm:ss"
        class="!w-80"
      />
      <el-button type="primary" :icon="Search" @click="handleQuery">查询</el-button>
      <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
    </div>

    <!-- 工具栏 -->
    <div class="toolbar">
      <span class="text-sm text-gray-500 dark:text-gray-400">共 {{ total }} 条日志</span>
    </div>

    <!-- 表格 -->
    <el-table v-loading="loading" :data="logList" border stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="操作人" width="110" show-overflow-tooltip>
        <template #default="{ row }">
          <span>{{ row.username || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="module" label="模块" width="110" show-overflow-tooltip>
        <template #default="{ row }">
          <span>{{ row.module || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="operation" label="操作" min-width="120" show-overflow-tooltip>
        <template #default="{ row }">
          <span>{{ row.operation || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="requestUrl" label="请求地址" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">
          <span>{{ row.requestUrl || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="requestMethod" label="方法" width="80" align="center">
        <template #default="{ row }">
          <span>{{ row.requestMethod || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="ip" label="IP" width="130" show-overflow-tooltip>
        <template #default="{ row }">
          <span>{{ row.ip || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="结果" width="80" align="center">
        <template #default="{ row }">
          <StatusTag :status="String(row.status)" :map="statusMap" />
        </template>
      </el-table-column>
      <el-table-column prop="costTime" label="耗时(ms)" width="90" align="right">
        <template #default="{ row }">
          <span>{{ row.costTime ?? '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="操作时间" width="160">
        <template #default="{ row }">
          <span class="text-gray-600 dark:text-gray-300">{{ formatTime(row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="80" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="table-footer">
      <el-pagination
        v-model:current-page="queryParams.pageNum"
        v-model:page-size="queryParams.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="loadData"
        @size-change="loadData"
      />
    </div>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detail.visible" title="日志详情" width="640px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="操作人">{{ detail.row.username || '-' }}</el-descriptions-item>
        <el-descriptions-item label="模块">{{ detail.row.module || '-' }}</el-descriptions-item>
        <el-descriptions-item label="操作">{{ detail.row.operation || '-' }}</el-descriptions-item>
        <el-descriptions-item label="IP">{{ detail.row.ip || '-' }}</el-descriptions-item>
        <el-descriptions-item label="请求方法">{{ detail.row.requestMethod || '-' }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ detail.row.costTime != null ? detail.row.costTime + ' ms' : '-' }}</el-descriptions-item>
        <el-descriptions-item label="请求地址" :span="2">{{ detail.row.requestUrl || '-' }}</el-descriptions-item>
        <el-descriptions-item label="请求参数" :span="2">
          <pre class="text-xs whitespace-pre-wrap break-all">{{ detail.row.params || '-' }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="错误信息" :span="2">
          <pre class="text-xs whitespace-pre-wrap break-all text-red-500">{{ detail.row.errorMsg || '-' }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="操作时间" :span="2">{{ formatTime(detail.row.createTime) }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import { listLogs } from '@/api/system'

const statusMap = {
  '1': { label: '成功', type: 'success' },
  '0': { label: '失败', type: 'danger' }
}

const moduleOptions = ['system', 'exam', 'cert', 'institution', 'student']

const queryParams = reactive({
  keyword: '',
  module: undefined as string | undefined,
  status: undefined as number | undefined,
  pageNum: 1,
  pageSize: 10
})
const dateRange = ref<string[]>([])

const loading = ref(false)
const logList = ref<any[]>([])
const total = ref(0)

function formatTime(value: string | null | undefined): string {
  if (!value) return '-'
  const s = value.replace('T', ' ')
  return s.length > 19 ? s.substring(0, 19) : s
}

async function loadData() {
  loading.value = true
  try {
    const params: Record<string, any> = {
      ...queryParams,
      keyword: queryParams.keyword || undefined,
      begin: dateRange.value && dateRange.value.length === 2 ? dateRange.value[0] : undefined,
      end: dateRange.value && dateRange.value.length === 2 ? dateRange.value[1] : undefined
    }
    const res = await listLogs(params)
    logList.value = res.data.rows || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  queryParams.pageNum = 1
  loadData()
}

function resetQuery() {
  queryParams.keyword = ''
  queryParams.module = undefined
  queryParams.status = undefined
  dateRange.value = []
  queryParams.pageNum = 1
  loadData()
}

// 详情
const detail = reactive({ visible: false, row: {} as any })

function handleDetail(row: any) {
  detail.row = row
  detail.visible = true
}

onMounted(loadData)
</script>
