<template>
  <div class="page-card">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-select v-model="queryParams.status" placeholder="状态" clearable class="!w-36">
        <el-option label="待审核" value="PENDING_AUDIT" />
        <el-option label="审核通过" value="AUDIT_PASSED" />
        <el-option label="审核驳回" value="AUDIT_REJECTED" />
        <el-option label="已签发" value="ISSUED" />
      </el-select>
      <el-select v-model="queryParams.studentUserId" placeholder="考生" clearable filterable class="!w-48">
        <el-option v-for="p in profileOptions" :key="p.id" :label="p.realName || p.name" :value="p.userId" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="handleQuery">查询</el-button>
      <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
    </div>

    <div class="toolbar">
      <span class="text-sm text-gray-500 dark:text-gray-400">共 {{ total }} 条申请</span>
      <el-button v-hasPermi="'cert:apply:list'" type="primary" :icon="Plus" @click="handleAdd">提交申请</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="applyNo" label="申请编号" min-width="140" show-overflow-tooltip />
      <el-table-column label="考生" min-width="110" show-overflow-tooltip>
        <template #default="{ row }">{{ profileNameByUser(row.studentUserId) }}</template>
      </el-table-column>
      <el-table-column prop="certificateType" label="证类别" min-width="110" show-overflow-tooltip />
      <el-table-column prop="registrationId" label="报名单ID" width="90" align="center" />
      <el-table-column prop="scoreId" label="成绩ID" width="80" align="center" />
      <el-table-column label="申请时间" width="160">
        <template #default="{ row }">
          <span class="text-gray-600 dark:text-gray-300">{{ formatTime(row.applyTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <StatusTag :status="row.status" :map="statusMap" />
        </template>
      </el-table-column>
      <el-table-column prop="auditRemark" label="审核意见" min-width="120" show-overflow-tooltip />
      <el-table-column label="操作" width="170" align="center" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === 'PENDING_AUDIT'">
            <el-button v-hasPermi="'cert:apply:audit'" link type="success" @click="handleAudit(row, 'PASS')">通过</el-button>
            <el-button v-hasPermi="'cert:apply:audit'" link type="danger" @click="handleAudit(row, 'REJECT')">驳回</el-button>
          </template>
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

    <!-- 提交申请弹窗 -->
    <el-dialog v-model="dialog.visible" :title="dialog.mode === 'add' ? '提交合格证申请' : '申请审核'" width="540px" destroy-on-close>
      <el-form v-if="dialog.mode === 'add'" ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="合格成绩" prop="scoreId">
          <el-select v-model="form.scoreId" placeholder="选择已审核通过的成绩" filterable class="!w-full" @change="onScoreChange">
            <el-option
              v-for="sc in passScoreOptions"
              :key="sc.id"
              :label="`${scoreLabel(sc)}｜${formatTime(sc.auditTime)}`"
              :value="sc.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="报名单" prop="registrationId">
          <el-select v-model="form.registrationId" placeholder="自动带出，可改" filterable class="!w-full">
            <el-option
              v-for="r in regOptions"
              :key="r.id"
              :label="`${r.registrationNo}｜${profileName(r.studentProfileId)}`"
              :value="r.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="证类别" prop="certificateType">
          <el-input v-model="form.certificateType" placeholder="如 MULTIROTOR / FIXED_WING" maxlength="30" />
        </el-form-item>
        <el-alert title="申请审核通过后，系统将通过消息队列异步签发合格证并生成证号（UVA-年份-序号）。" type="info" :closable="false" show-icon />
      </el-form>
      <el-form v-else ref="auditFormRef" :model="auditForm" label-width="110px">
        <el-form-item label="审核意见">
          <el-input v-model="auditForm.comment" type="textarea" :rows="3" :placeholder="auditForm.action === 'PASS' ? '选填' : '请填写驳回原因'" maxlength="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button
          v-if="dialog.mode === 'add'"
          type="primary"
          :loading="dialog.submitting"
          @click="handleSubmit"
        >提交申请</el-button>
        <el-button
          v-else
          :type="auditForm.action === 'PASS' ? 'success' : 'danger'"
          :loading="dialog.submitting"
          @click="handleAuditSubmit"
        >{{ auditForm.action === 'PASS' ? '确定通过' : '确定驳回' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import StatusTag from '@/components/StatusTag.vue'
import { listApply, createApply, auditApply } from '@/api/certificate'
import { listRegistrations, listScores } from '@/api/exam'
import { listProfiles } from '@/api/student'

const statusMap: Record<string, { label: string; type: string }> = {
  SUBMITTED: { label: '已提交', type: 'info' },
  PENDING_AUDIT: { label: '待审核', type: 'warning' },
  AUDIT_PASSED: { label: '审核通过', type: 'primary' },
  AUDIT_REJECTED: { label: '审核驳回', type: 'danger' },
  ISSUED: { label: '已签发', type: 'success' },
  CANCELLED: { label: '已撤销', type: 'info' }
}

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({ pageNum: 1, pageSize: 10, status: '', studentUserId: undefined as number | undefined })

const profileOptions = ref<any[]>([])
const passScoreOptions = ref<any[]>([])
const regOptions = ref<any[]>([])

const dialog = reactive({ visible: false, mode: 'add' as 'add' | 'audit', submitting: false })
const formRef = ref<FormInstance>()
const form = reactive<any>({})
const rules: FormRules = {
  scoreId: [{ required: true, message: '请选择合格成绩', trigger: 'change' }],
  registrationId: [{ required: true, message: '请选择报名单', trigger: 'change' }],
  certificateType: [{ required: true, message: '请输入证类别', trigger: 'blur' }]
}
const auditFormRef = ref<FormInstance>()
const auditForm = reactive<any>({})

function formatTime(v?: string): string {
  if (!v) return '-'
  return String(v).replace('T', ' ').slice(0, 19)
}

function profileName(id?: number): string {
  if (!id) return '-'
  const p = profileOptions.value.find((x) => x.id === id)
  return p ? p.realName || p.name || `#${id}` : `#${id}`
}
function profileNameByUser(userId?: number): string {
  if (!userId) return '-'
  const p = profileOptions.value.find((x) => x.userId === userId)
  return p ? p.realName || p.name || `#${userId}` : `#${userId}`
}
function scoreLabel(sc: any): string {
  return `${profileNameByUser(sc.studentUserId)}｜${sc.examType === 'THEORY' ? '理论' : '实操'} ${sc.score}分`
}

function onScoreChange(scoreId: number) {
  const sc = passScoreOptions.value.find((x) => x.id === scoreId)
  if (sc) {
    // 自动带出该成绩对应的报名单
    form.registrationId = sc.registrationId
  }
}

async function loadData() {
  loading.value = true
  try {
    const res = await listApply({
      pageNum: queryParams.pageNum,
      pageSize: queryParams.pageSize,
      status: queryParams.status || undefined,
      studentUserId: queryParams.studentUserId
    })
    list.value = res.data?.rows || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

async function loadOptions() {
  const [p, sc, r] = await Promise.all([
    listProfiles({ pageNum: 1, pageSize: 200 }),
    listScores({ pageNum: 1, pageSize: 200, passStatus: 'PASS', status: 'APPROVED' }),
    listRegistrations({ pageNum: 1, pageSize: 200, status: 'COMPLETED' })
  ])
  profileOptions.value = p.data?.rows || []
  passScoreOptions.value = sc.data?.rows || []
  regOptions.value = r.data?.rows || []
}

function handleQuery() {
  queryParams.pageNum = 1
  loadData()
}
function resetQuery() {
  queryParams.status = ''
  queryParams.studentUserId = undefined
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
    await createApply({
      registrationId: form.registrationId,
      scoreId: form.scoreId,
      certificateType: form.certificateType
    })
    ElMessage.success('申请已提交，待审核')
    dialog.visible = false
    loadData()
  } finally {
    dialog.submitting = false
  }
}

function handleAudit(row: any, action: 'PASS' | 'REJECT') {
  Object.keys(auditForm).forEach((k) => delete auditForm[k])
  auditForm.action = action
  auditForm.id = row.id
  dialog.mode = 'audit'
  dialog.visible = true
}

async function handleAuditSubmit() {
  dialog.submitting = true
  try {
    await auditApply(auditForm.id, { action: auditForm.action, comment: auditForm.comment })
    ElMessage.success(auditForm.action === 'PASS' ? '审核通过，将异步签发合格证' : '已驳回')
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
