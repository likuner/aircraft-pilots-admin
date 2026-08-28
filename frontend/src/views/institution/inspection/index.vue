<template>
  <div class="page-card">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-select v-model="queryParams.inspectorId" placeholder="核查人员" clearable filterable class="!w-48">
        <el-option v-for="u in userOptions" :key="u.id" :label="u.realName || u.username" :value="u.id" />
      </el-select>
      <el-select v-model="queryParams.status" placeholder="状态" clearable class="!w-32">
        <el-option label="待核查" value="ASSIGNED" />
        <el-option label="已完成" value="COMPLETED" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="handleQuery">查询</el-button>
      <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
    </div>

    <div class="toolbar">
      <span class="text-sm text-gray-500 dark:text-gray-400">共 {{ total }} 个核查任务</span>
      <span class="text-xs text-gray-400 dark:text-gray-500">核查通过后申请进入「资质评定」环节</span>
    </div>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column label="机构" min-width="170" show-overflow-tooltip>
        <template #default="{ row }">{{ instName(row.institutionId) }}</template>
      </el-table-column>
      <el-table-column prop="inspectionDate" label="核查日期" width="110" />
      <el-table-column prop="address" label="核查地点" min-width="150" show-overflow-tooltip />
      <el-table-column label="核查人员" width="100" show-overflow-tooltip>
        <template #default="{ row }">{{ userName(row.inspectorId) }}</template>
      </el-table-column>
      <el-table-column prop="checklist" label="核查清单" min-width="140" show-overflow-tooltip />
      <el-table-column label="结果" width="90" align="center">
        <template #default="{ row }">
          <StatusTag :status="row.result || 'PENDING'" :map="resultMap" />
        </template>
      </el-table-column>
      <el-table-column label="状态" width="95" align="center">
        <template #default="{ row }">
          <StatusTag :status="row.status" :map="statusMap" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="110" align="center" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 'ASSIGNED'"
            v-hasPermi="'inst:inspection:complete'"
            link type="primary"
            @click="handleComplete(row)"
          >完成核查</el-button>
          <span v-else class="text-gray-400 text-xs">已处理</span>
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

    <!-- 完成核查弹窗 -->
    <el-dialog v-model="dialog.visible" :title="`完成实地核查（${dialog.id}）`" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="核查结果" prop="result">
          <el-radio-group v-model="form.result">
            <el-radio-button label="PASS">通过</el-radio-button>
            <el-radio-button label="FAIL">不通过</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="核查结论" prop="summary">
          <el-input v-model="form.summary" type="textarea" :rows="3" placeholder="请填写核查结论" maxlength="255" />
        </el-form-item>
        <el-alert
          v-if="form.result === 'PASS'"
          title="核查通过后申请将进入「资质评定」环节，由评定人评估后发证。"
          type="info" :closable="false" show-icon
        />
        <el-alert
          v-else
          title="核查不通过后申请将退回「材料提交」环节。"
          type="warning" :closable="false" show-icon
        />
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button
          :type="form.result === 'PASS' ? 'success' : 'danger'"
          :loading="dialog.submitting"
          @click="handleSubmit"
        >{{ form.result === 'PASS' ? '确定通过' : '确定不通过' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import StatusTag from '@/components/StatusTag.vue'
import { listInspections, completeInspection } from '@/api/institution'
import { listInstitutions } from '@/api/institution'
import { listUsers } from '@/api/system'

const statusMap: Record<string, { label: string; type: string }> = {
  ASSIGNED: { label: '待核查', type: 'warning' },
  COMPLETED: { label: '已完成', type: 'success' }
}
const resultMap: Record<string, { label: string; type: string }> = {
  PASS: { label: '通过', type: 'success' },
  FAIL: { label: '不通过', type: 'danger' },
  PENDING: { label: '未核查', type: 'info' }
}

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({ pageNum: 1, pageSize: 10, inspectorId: undefined as number | undefined, status: '' })

const instOptions = ref<any[]>([])
const userOptions = ref<any[]>([])

const dialog = reactive({ visible: false, submitting: false, id: 0 })
const formRef = ref<FormInstance>()
const form = reactive<any>({})
const rules: FormRules = {
  result: [{ required: true, message: '请选择核查结果', trigger: 'change' }],
  summary: [{ required: true, message: '请填写核查结论', trigger: 'blur' }]
}

function instName(id?: number): string {
  if (!id) return '-'
  return instOptions.value.find((i) => i.id === id)?.instName || `#${id}`
}
function userName(id?: number): string {
  if (!id) return '-'
  const u = userOptions.value.find((x) => x.id === id)
  return u ? u.realName || u.username : `#${id}`
}

async function loadData() {
  loading.value = true
  try {
    const res = await listInspections({
      pageNum: queryParams.pageNum,
      pageSize: queryParams.pageSize,
      inspectorId: queryParams.inspectorId,
      status: queryParams.status || undefined
    })
    list.value = res.data?.rows || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

async function loadOptions() {
  const [i, u] = await Promise.all([
    listInstitutions({ pageNum: 1, pageSize: 200 }),
    listUsers({ pageNum: 1, pageSize: 200 })
  ])
  instOptions.value = i.data?.rows || []
  userOptions.value = u.data?.rows || []
}

function handleQuery() {
  queryParams.pageNum = 1
  loadData()
}
function resetQuery() {
  queryParams.inspectorId = undefined
  queryParams.status = ''
  handleQuery()
}

function handleComplete(row: any) {
  Object.keys(form).forEach((k) => delete form[k])
  form.id = row.id
  form.result = 'PASS'
  dialog.id = row.id
  dialog.visible = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate()
  dialog.submitting = true
  try {
    await completeInspection(form.id, { result: form.result, summary: form.summary })
    ElMessage.success(form.result === 'PASS' ? '核查完成，申请进入资质评定' : '已记录不通过')
    dialog.visible = false
    loadData()
  } finally {
    dialog.submitting = false
  }
}

onMounted(async () => {
  await loadOptions()
  await loadData()
})
</script>
