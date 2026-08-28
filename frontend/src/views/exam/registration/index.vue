<template>
  <div class="page-card">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input v-model="queryParams.keyword" placeholder="报名编号" clearable class="!w-48" @keyup.enter="handleQuery" />
      <el-select v-model="queryParams.sessionId" placeholder="所属场次" clearable filterable class="!w-52">
        <el-option v-for="s in sessionOptions" :key="s.id" :label="`${s.sessionName}（${s.sessionCode}）`" :value="s.id" />
      </el-select>
      <el-select v-model="queryParams.status" placeholder="状态" clearable class="!w-36">
        <el-option label="待审核" value="PENDING" />
        <el-option label="已通过" value="APPROVED" />
        <el-option label="已排考" value="SCHEDULED" />
        <el-option label="已完成" value="COMPLETED" />
        <el-option label="已驳回" value="REJECTED" />
        <el-option label="已取消" value="CANCELED" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="handleQuery">查询</el-button>
      <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
    </div>

    <div class="toolbar">
      <span class="text-sm text-gray-500 dark:text-gray-400">共 {{ total }} 条报名</span>
      <el-button v-hasPermi="'exam:registration:add'" type="primary" :icon="Plus" @click="handleAdd">新增报名</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="registrationNo" label="报名编号" min-width="140" show-overflow-tooltip />
      <el-table-column label="考生" min-width="110" show-overflow-tooltip>
        <template #default="{ row }">{{ profileName(row.studentProfileId) }}</template>
      </el-table-column>
      <el-table-column label="场次" min-width="140" show-overflow-tooltip>
        <template #default="{ row }">{{ sessionName(row.sessionId) }}</template>
      </el-table-column>
      <el-table-column label="批次" min-width="120" show-overflow-tooltip>
        <template #default="{ row }">{{ batchName(row.batchId) }}</template>
      </el-table-column>
      <el-table-column label="报名时间" width="160">
        <template #default="{ row }">
          <span class="text-gray-600 dark:text-gray-300">{{ formatTime(row.applyTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="rejectReason" label="驳回原因" min-width="110" show-overflow-tooltip />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <StatusTag :status="row.status" :map="statusMap" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="210" align="center" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === 'PENDING'">
            <el-button v-hasPermi="'exam:registration:approve'" link type="success" @click="handleApprove(row)">通过</el-button>
            <el-button v-hasPermi="'exam:registration:reject'" link type="danger" @click="handleReject(row)">驳回</el-button>
          </template>
          <template v-else-if="row.status === 'APPROVED'">
            <el-button v-hasPermi="'exam:registration:schedule'" link type="primary" @click="handleArrange(row)">排考</el-button>
            <el-button v-hasPermi="'exam:registration:add'" link type="warning" @click="handleCancel(row)">取消</el-button>
          </template>
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

    <!-- 新增报名弹窗 -->
    <el-dialog v-model="dialog.visible" :title="dialog.mode === 'add' ? '新增报名' : '报名详情'" width="540px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="考试场次" prop="sessionId">
          <el-select v-model="form.sessionId" placeholder="请选择已发布的场次" filterable class="!w-full">
            <el-option v-for="s in sessionOptions" :key="s.id" :label="`${s.sessionName}（${s.sessionCode}）`" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="考生档案" prop="studentProfileId">
          <el-select v-model="form.studentProfileId" placeholder="请选择" filterable class="!w-full">
            <el-option v-for="p in profileOptions" :key="p.id" :label="`${p.realName || p.name}（${p.idCard}）`" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="所在机构" prop="institutionId">
          <el-select v-model="form.institutionId" placeholder="可选" clearable filterable class="!w-full">
            <el-option v-for="i in instOptions" :key="i.id" :label="i.instName" :value="i.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="dialog.submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 驳回弹窗 -->
    <el-dialog v-model="rejectDialog.visible" title="驳回报名" width="440px" destroy-on-close>
      <el-form ref="rejectFormRef" :model="rejectForm" :rules="rejectRules" label-width="90px">
        <el-form-item label="驳回原因" prop="reason">
          <el-input v-model="rejectForm.reason" type="textarea" :rows="3" placeholder="请填写驳回原因" maxlength="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialog.visible = false">取消</el-button>
        <el-button type="danger" :loading="rejectDialog.submitting" @click="handleRejectSubmit">确定驳回</el-button>
      </template>
    </el-dialog>

    <!-- 排考弹窗 -->
    <el-dialog v-model="arrangeDialog.visible" title="编排考试批次" width="440px" destroy-on-close>
      <el-form ref="arrangeFormRef" :model="arrangeForm" :rules="arrangeRules" label-width="90px">
        <el-form-item label="考试批次" prop="batchId">
          <el-select v-model="arrangeForm.batchId" placeholder="请选择批次" class="!w-full">
            <el-option v-for="b in batchOptions" :key="b.id" :label="`${b.batchCode}（${formatTime(b.batchTime)}）`" :value="b.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="arrangeDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="arrangeDialog.submitting" @click="handleArrangeSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import StatusTag from '@/components/StatusTag.vue'
import {
  listRegistrations, createRegistration, approveRegistration, rejectRegistration,
  cancelRegistration, arrangeRegistration, listSessions, listBatchesBySession
} from '@/api/exam'
import { listProfiles } from '@/api/student'
import { listInstitutions } from '@/api/institution'

const statusMap: Record<string, { label: string; type: string }> = {
  PENDING: { label: '待审核', type: 'warning' },
  APPROVED: { label: '已通过', type: 'success' },
  SCHEDULED: { label: '已排考', type: 'primary' },
  COMPLETED: { label: '已完成', type: 'success' },
  REJECTED: { label: '已驳回', type: 'danger' },
  CANCELED: { label: '已取消', type: 'info' }
}

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({ pageNum: 1, pageSize: 10, keyword: '', sessionId: undefined as number | undefined, status: '' })

const sessionOptions = ref<any[]>([])
const profileOptions = ref<any[]>([])
const instOptions = ref<any[]>([])
const batchOptions = ref<any[]>([])

const dialog = reactive({ visible: false, mode: 'add' as 'add' | 'edit', submitting: false })
const formRef = ref<FormInstance>()
const form = reactive<any>({})
const rules: FormRules = {
  sessionId: [{ required: true, message: '请选择考试场次', trigger: 'change' }],
  studentProfileId: [{ required: true, message: '请选择考生档案', trigger: 'change' }]
}

const rejectDialog = reactive({ visible: false, submitting: false })
const rejectFormRef = ref<FormInstance>()
const rejectForm = reactive<any>({})
const rejectRules: FormRules = {
  reason: [{ required: true, message: '请填写驳回原因', trigger: 'blur' }]
}

const arrangeDialog = reactive({ visible: false, submitting: false })
const arrangeFormRef = ref<FormInstance>()
const arrangeForm = reactive<any>({})
const arrangeRules: FormRules = {
  batchId: [{ required: true, message: '请选择批次', trigger: 'change' }]
}

function formatTime(v?: string): string {
  if (!v) return '-'
  return String(v).replace('T', ' ').slice(0, 19)
}

function profileName(id?: number): string {
  if (!id) return '-'
  const p = profileOptions.value.find((x) => x.id === id)
  return p ? p.realName || p.name || `#${id}` : `#${id}`
}
function sessionName(id?: number): string {
  if (!id) return '-'
  return sessionOptions.value.find((s) => s.id === id)?.sessionName || `#${id}`
}
function batchName(id?: number): string {
  if (!id) return '-'
  return batchOptions.value.find((b) => b.id === id)?.batchCode || `#${id}`
}

async function loadData() {
  loading.value = true
  try {
    const res = await listRegistrations({
      pageNum: queryParams.pageNum,
      pageSize: queryParams.pageSize,
      keyword: queryParams.keyword || undefined,
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
  const [s, p, i] = await Promise.all([
    listSessions({ pageNum: 1, pageSize: 200 }),
    listProfiles({ pageNum: 1, pageSize: 200 }),
    listInstitutions({ pageNum: 1, pageSize: 200 })
  ])
  sessionOptions.value = s.data?.rows || []
  profileOptions.value = p.data?.rows || []
  instOptions.value = i.data?.rows || []
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
  dialog.mode = 'add'
  dialog.visible = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate()
  dialog.submitting = true
  try {
    await createRegistration(form)
    ElMessage.success('报名成功（名额已原子扣减）')
    dialog.visible = false
    loadData()
  } finally {
    dialog.submitting = false
  }
}

async function handleApprove(row: any) {
  await ElMessageBox.confirm(`确定通过报名「${row.registrationNo}」吗？`, '提示', { type: 'warning' })
  await approveRegistration(row.id)
  ElMessage.success('已通过')
  loadData()
}

function handleReject(row: any) {
  Object.keys(rejectForm).forEach((k) => delete rejectForm[k])
  rejectForm.id = row.id
  rejectDialog.visible = true
}

async function handleRejectSubmit() {
  if (!rejectFormRef.value) return
  await rejectFormRef.value.validate()
  rejectDialog.submitting = true
  try {
    await rejectRegistration(rejectForm.id, rejectForm.reason)
    ElMessage.success('已驳回')
    rejectDialog.visible = false
    loadData()
  } finally {
    rejectDialog.submitting = false
  }
}

async function handleArrange(row: any) {
  Object.keys(arrangeForm).forEach((k) => delete arrangeForm[k])
  arrangeForm.id = row.id
  const res = await listBatchesBySession(row.sessionId)
  batchOptions.value = res.data || []
  arrangeDialog.visible = true
}

async function handleArrangeSubmit() {
  if (!arrangeFormRef.value) return
  await arrangeFormRef.value.validate()
  arrangeDialog.submitting = true
  try {
    await arrangeRegistration(arrangeForm.id, arrangeForm.batchId)
    ElMessage.success('排考成功')
    arrangeDialog.visible = false
    loadData()
  } finally {
    arrangeDialog.submitting = false
  }
}

async function handleCancel(row: any) {
  await ElMessageBox.confirm(`确定取消报名「${row.registrationNo}」吗？名额将被释放。`, '提示', { type: 'warning' })
  await cancelRegistration(row.id)
  ElMessage.success('已取消')
  loadData()
}

onMounted(async () => {
  await loadOptions()
  await loadData()
})
</script>
