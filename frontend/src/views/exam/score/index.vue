<template>
  <div class="page-card">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-select v-model="queryParams.sessionId" placeholder="所属场次" clearable filterable class="!w-52">
        <el-option v-for="s in sessionOptions" :key="s.id" :label="`${s.sessionName}（${s.sessionCode}）`" :value="s.id" />
      </el-select>
      <el-select v-model="queryParams.status" placeholder="状态" clearable class="!w-32">
        <el-option label="草稿" value="DRAFT" />
        <el-option label="待审核" value="SUBMITTED" />
        <el-option label="已通过" value="APPROVED" />
        <el-option label="已驳回" value="REJECTED" />
      </el-select>
      <el-select v-model="queryParams.passStatus" placeholder="判定" clearable class="!w-28">
        <el-option label="通过" value="PASS" />
        <el-option label="不通过" value="FAIL" />
        <el-option label="待判定" value="NOT_EVALUATED" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="handleQuery">查询</el-button>
      <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
    </div>

    <div class="toolbar">
      <span class="text-sm text-gray-500 dark:text-gray-400">共 {{ total }} 条成绩</span>
      <el-button v-hasPermi="'exam:score:add'" type="primary" :icon="Plus" @click="handleAdd">录入成绩</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column label="考生" min-width="110" show-overflow-tooltip>
        <template #default="{ row }">{{ profileNameByUser(row.studentUserId) }}</template>
      </el-table-column>
      <el-table-column label="场次" min-width="140" show-overflow-tooltip>
        <template #default="{ row }">{{ sessionName(row.sessionId) }}</template>
      </el-table-column>
      <el-table-column label="科目" width="90" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="row.examType === 'THEORY' ? 'primary' : 'warning'">{{ examTypeText(row.examType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="得分" width="90" align="center">
        <template #default="{ row }">
          <span class="font-semibold" :class="row.score != null && row.score >= 0 ? 'text-gray-800 dark:text-gray-100' : ''">{{ row.score }}</span>
        </template>
      </el-table-column>
      <el-table-column label="判定" width="95" align="center">
        <template #default="{ row }">
          <StatusTag :status="row.passStatus || 'NOT_EVALUATED'" :map="passMap" />
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <StatusTag :status="row.status" :map="statusMap" />
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
      <el-table-column label="操作" width="140" align="center" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === 'DRAFT'">
            <el-button v-hasPermi="'exam:score:add'" link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button v-hasPermi="'exam:score:submit'" link type="success" @click="handleSubmitScore(row)">提交</el-button>
          </template>
          <span v-else class="text-gray-400 text-xs">已提交</span>
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

    <!-- 录入/编辑成绩弹窗 -->
    <el-dialog v-model="dialog.visible" :title="dialog.mode === 'add' ? '录入成绩' : '编辑成绩'" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item v-if="dialog.mode === 'add'" label="报名单" prop="registrationId">
          <el-select v-model="form.registrationId" placeholder="选择待考试报名" filterable class="!w-full">
            <el-option
              v-for="r in regOptions"
              :key="r.id"
              :label="`${r.registrationNo}｜${profileName(r.studentProfileId)}｜${sessionName(r.sessionId)}`"
              :value="r.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="科目" prop="examType">
          <el-radio-group v-model="form.examType">
            <el-radio-button label="THEORY">理论</el-radio-button>
            <el-radio-button label="PRACTICAL">实操</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="分数" prop="score">
          <el-input-number v-model="form.score" :min="0" :max="100" :precision="1" :step="1" class="!w-full" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="选填" maxlength="200" />
        </el-form-item>
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
import { listScores, createScore, updateScore, submitScore, listSessions, listRegistrations } from '@/api/exam'
import { listProfiles } from '@/api/student'

const statusMap: Record<string, { label: string; type: string }> = {
  DRAFT: { label: '草稿', type: 'info' },
  SUBMITTED: { label: '待审核', type: 'warning' },
  APPROVED: { label: '已通过', type: 'success' },
  REJECTED: { label: '已驳回', type: 'danger' }
}
const passMap: Record<string, { label: string; type: string }> = {
  PASS: { label: '通过', type: 'success' },
  FAIL: { label: '不通过', type: 'danger' },
  NOT_EVALUATED: { label: '待判定', type: 'info' }
}

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNum: 1, pageSize: 10,
  sessionId: undefined as number | undefined,
  status: 'DRAFT', passStatus: ''
})

const sessionOptions = ref<any[]>([])
const profileOptions = ref<any[]>([])
const regOptions = ref<any[]>([])

const dialog = reactive({ visible: false, mode: 'add' as 'add' | 'edit', submitting: false })
const formRef = ref<FormInstance>()
const form = reactive<any>({})
const rules: FormRules = {
  registrationId: [{ required: true, message: '请选择报名单', trigger: 'change' }],
  examType: [{ required: true, message: '请选择科目', trigger: 'change' }],
  score: [{ required: true, message: '请输入分数', trigger: 'blur' }]
}

function examTypeText(t?: string): string {
  if (t === 'THEORY') return '理论'
  if (t === 'PRACTICAL') return '实操'
  if (t === 'BOTH') return '理论与实操'
  return t || '-'
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
function profileNameByUser(userId?: number): string {
  if (!userId) return '-'
  const p = profileOptions.value.find((x) => x.userId === userId)
  return p ? p.realName || p.name || `#${userId}` : `#${userId}`
}
function sessionName(id?: number): string {
  if (!id) return '-'
  return sessionOptions.value.find((s) => s.id === id)?.sessionName || `#${id}`
}

async function loadData() {
  loading.value = true
  try {
    const res = await listScores({
      pageNum: queryParams.pageNum,
      pageSize: queryParams.pageSize,
      sessionId: queryParams.sessionId,
      status: queryParams.status || undefined,
      passStatus: queryParams.passStatus || undefined
    })
    list.value = res.data?.rows || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

async function loadOptions() {
  const [s, p, r] = await Promise.all([
    listSessions({ pageNum: 1, pageSize: 200 }),
    listProfiles({ pageNum: 1, pageSize: 200 }),
    listRegistrations({ pageNum: 1, pageSize: 200 })
  ])
  sessionOptions.value = s.data?.rows || []
  profileOptions.value = p.data?.rows || []
  regOptions.value = (r.data?.rows || []).filter((x: any) => ['APPROVED', 'SCHEDULED'].includes(x.status))
}

function handleQuery() {
  queryParams.pageNum = 1
  loadData()
}
function resetQuery() {
  queryParams.sessionId = undefined
  queryParams.status = 'DRAFT'
  queryParams.passStatus = ''
  handleQuery()
}

function handleAdd() {
  Object.keys(form).forEach((k) => delete form[k])
  form.examType = 'THEORY'
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
      await createScore({ registrationId: form.registrationId, examType: form.examType, score: form.score, remark: form.remark })
      ElMessage.success('录入成功')
    } else {
      await updateScore({ id: form.id, score: form.score, remark: form.remark })
      ElMessage.success('保存成功')
    }
    dialog.visible = false
    loadData()
  } finally {
    dialog.submitting = false
  }
}

async function handleSubmitScore(row: any) {
  await ElMessageBox.confirm(`确定提交该成绩进入审核吗？提交后不可修改。`, '提示', { type: 'warning' })
  await submitScore(row.id)
  ElMessage.success('已提交审核')
  loadData()
}

onMounted(async () => {
  await loadOptions()
  await loadData()
})
</script>
