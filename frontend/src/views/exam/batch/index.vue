<template>
  <div class="page-card">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input v-model="queryParams.keyword" placeholder="批次编号" clearable class="!w-48" @keyup.enter="handleQuery" />
      <el-select v-model="queryParams.sessionId" placeholder="所属场次" clearable filterable class="!w-52">
        <el-option v-for="s in sessionOptions" :key="s.id" :label="`${s.sessionName}（${s.sessionCode}）`" :value="s.id" />
      </el-select>
      <el-select v-model="queryParams.status" placeholder="状态" clearable class="!w-32">
        <el-option label="待开考" value="PLANNED" />
        <el-option label="已完成" value="COMPLETED" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="handleQuery">查询</el-button>
      <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
    </div>

    <div class="toolbar">
      <span class="text-sm text-gray-500 dark:text-gray-400">共 {{ total }} 个批次</span>
      <el-button v-hasPermi="'exam:batch:add'" type="primary" :icon="Plus" @click="handleAdd">新增批次</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="batchCode" label="批次编号" min-width="130" show-overflow-tooltip />
      <el-table-column label="所属场次" min-width="150" show-overflow-tooltip>
        <template #default="{ row }">
          <span class="text-gray-700 dark:text-gray-200">{{ sessionName(row.sessionId) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="考场" min-width="120" show-overflow-tooltip>
        <template #default="{ row }">
          <span class="text-gray-700 dark:text-gray-200">{{ roomName(row.roomId) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="开考时间" width="160">
        <template #default="{ row }">
          <span class="text-gray-600 dark:text-gray-300">{{ formatTime(row.batchTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="监考人" width="100" show-overflow-tooltip>
        <template #default="{ row }">{{ userName(row.invigilatorId) }}</template>
      </el-table-column>
      <el-table-column label="考官" width="100" show-overflow-tooltip>
        <template #default="{ row }">{{ userName(row.examinerId) }}</template>
      </el-table-column>
      <el-table-column label="人数" width="90" align="center">
        <template #default="{ row }">
          <span>{{ row.enrolledCount ?? 0 }} / {{ row.capacity }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <StatusTag :status="row.status" :map="statusMap" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140" align="center" fixed="right">
        <template #default="{ row }">
          <el-button v-hasPermi="'exam:batch:edit'" link type="primary" @click="handleEdit(row)">编辑</el-button>
          <el-button v-hasPermi="'exam:batch:delete'" link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

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

    <!-- 新增/编辑批次弹窗 -->
    <el-dialog v-model="dialog.visible" :title="dialog.mode === 'add' ? '新增批次' : '编辑批次'" width="600px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="所属场次" prop="sessionId">
              <el-select v-model="form.sessionId" placeholder="请选择" filterable class="!w-full">
                <el-option v-for="s in sessionOptions" :key="s.id" :label="`${s.sessionName}（${s.sessionCode}）`" :value="s.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="考场" prop="roomId">
              <el-select v-model="form.roomId" placeholder="请选择" filterable class="!w-full">
                <el-option v-for="r in roomOptions" :key="r.id" :label="`${r.roomName}（${r.roomCode}）`" :value="r.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="开考时间" prop="batchTime">
              <el-date-picker v-model="form.batchTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择时间" class="!w-full" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="容量" prop="capacity">
              <el-input-number v-model="form.capacity" :min="1" :precision="0" class="!w-full" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="监考人" prop="invigilatorId">
              <el-select v-model="form.invigilatorId" placeholder="请选择" filterable clearable class="!w-full">
                <el-option v-for="u in userOptions" :key="u.id" :label="u.realName || u.username" :value="u.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="考官" prop="examinerId">
              <el-select v-model="form.examinerId" placeholder="请选择" filterable clearable class="!w-full">
                <el-option v-for="u in userOptions" :key="u.id" :label="u.realName || u.username" :value="u.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="dialog.submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import StatusTag from '@/components/StatusTag.vue'
import { listBatches, createBatch, updateBatch, deleteBatch, listSessions, listRooms } from '@/api/exam'
import { listUsers } from '@/api/system'

const statusMap: Record<string, { label: string; type: string }> = {
  PLANNED: { label: '待开考', type: 'primary' },
  COMPLETED: { label: '已完成', type: 'success' }
}

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({ pageNum: 1, pageSize: 10, keyword: '', sessionId: undefined as number | undefined, status: '' })

const sessionOptions = ref<any[]>([])
const roomOptions = ref<any[]>([])
const userOptions = ref<any[]>([])

const dialog = reactive({ visible: false, mode: 'add' as 'add' | 'edit', submitting: false })
const formRef = ref<FormInstance>()
const form = reactive<any>({})
const rules: FormRules = {
  sessionId: [{ required: true, message: '请选择所属场次', trigger: 'change' }],
  roomId: [{ required: true, message: '请选择考场', trigger: 'change' }],
  batchTime: [{ required: true, message: '请选择开考时间', trigger: 'change' }],
  capacity: [{ required: true, message: '请输入容量', trigger: 'blur' }]
}

function formatTime(v?: string): string {
  if (!v) return '-'
  return String(v).replace('T', ' ').slice(0, 19)
}

function sessionName(id?: number): string {
  if (!id) return '-'
  return sessionOptions.value.find((s) => s.id === id)?.sessionName || `#${id}`
}
function roomName(id?: number): string {
  if (!id) return '-'
  return roomOptions.value.find((r) => r.id === id)?.roomName || `#${id}`
}
function userName(id?: number): string {
  if (!id) return '-'
  const u = userOptions.value.find((x) => x.id === id)
  return u ? u.realName || u.username : `#${id}`
}

async function loadData() {
  loading.value = true
  try {
    const res = await listBatches({
      pageNum: queryParams.pageNum,
      pageSize: queryParams.pageSize,
      sessionId: queryParams.sessionId,
      status: queryParams.status || undefined
    })
    list.value = res.data?.rows || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

async function loadOptions() {
  const [s, r, u] = await Promise.all([
    listSessions({ pageNum: 1, pageSize: 200 }),
    listRooms({ pageNum: 1, pageSize: 200 }),
    listUsers({ pageNum: 1, pageSize: 200 })
  ])
  sessionOptions.value = s.data?.rows || []
  roomOptions.value = r.data?.rows || []
  userOptions.value = u.data?.rows || []
}

function handleQuery() {
  queryParams.pageNum = 1
  loadData()
}
function resetQuery() {
  queryParams.keyword = ''
  queryParams.sessionId = undefined
  queryParams.status = ''
  handleQuery()
}

function handleAdd() {
  Object.keys(form).forEach((k) => delete form[k])
  form.capacity = 50
  dialog.mode = 'add'
  dialog.visible = true
}

function handleEdit(row: any) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, { ...row })
  dialog.mode = 'edit'
  dialog.visible = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate()
  dialog.submitting = true
  try {
    if (dialog.mode === 'add') {
      await createBatch(form)
      ElMessage.success('新增成功')
    } else {
      await updateBatch({ ...form, id: form.id })
      ElMessage.success('保存成功')
    }
    dialog.visible = false
    loadData()
  } finally {
    dialog.submitting = false
  }
}

async function handleDelete(row: any) {
  await ElMessageBox.confirm(`确定删除批次「${row.batchCode}」吗？`, '提示', { type: 'warning' })
  await deleteBatch(row.id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(async () => {
  await loadOptions()
  await loadData()
})
</script>
