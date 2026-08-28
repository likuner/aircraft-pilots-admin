<template>
  <div class="page-card">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-select v-model="queryParams.sessionId" placeholder="所属场次" clearable filterable class="!w-52">
        <el-option v-for="s in sessionOptions" :key="s.id" :label="`${s.sessionName}（${s.sessionCode}）`" :value="s.id" />
      </el-select>
      <el-select v-model="queryParams.status" placeholder="状态" clearable class="!w-32">
        <el-option label="待审核" value="SUBMITTED" />
        <el-option label="已通过" value="APPROVED" />
        <el-option label="已驳回" value="REJECTED" />
      </el-select>
      <el-select v-model="queryParams.passStatus" placeholder="判定" clearable class="!w-28">
        <el-option label="通过" value="PASS" />
        <el-option label="不通过" value="FAIL" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="handleQuery">查询</el-button>
      <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
    </div>

    <div class="toolbar">
      <span class="text-sm text-gray-500 dark:text-gray-400">共 {{ total }} 条成绩</span>
      <span class="text-xs text-gray-400 dark:text-gray-500">审核通过后系统将自动完成合格判定并通知考生</span>
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
          <span class="font-semibold">{{ row.score }}</span>
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
      <el-table-column prop="auditRemark" label="审核意见" min-width="130" show-overflow-tooltip />
      <el-table-column label="操作" width="170" align="center" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === 'SUBMITTED'">
            <el-button link type="success" @click="handleAudit(row, 'PASS')">通过</el-button>
            <el-button link type="danger" @click="handleAudit(row, 'REJECT')">驳回</el-button>
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

    <!-- 审核弹窗 -->
    <el-dialog v-model="dialog.visible" :title="dialog.action === 'PASS' ? '成绩审核通过' : '成绩驳回'" width="460px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="审核意见" prop="comment">
          <el-input v-model="form.comment" type="textarea" :rows="3" :placeholder="dialog.action === 'PASS' ? '选填，如：成绩真实有效' : '请填写驳回原因'" maxlength="200" />
        </el-form-item>
        <el-alert
          v-if="dialog.action === 'PASS'"
          title="通过后系统将按场次及格线自动判定（PASS/FAIL）并通过消息队列通知考生。"
          type="info"
          :closable="false"
          show-icon
        />
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button :type="dialog.action === 'PASS' ? 'success' : 'danger'" :loading="dialog.submitting" @click="handleSubmit">
          {{ dialog.action === 'PASS' ? '确定通过' : '确定驳回' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import StatusTag from '@/components/StatusTag.vue'
import { listScores, auditScore, listSessions } from '@/api/exam'
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
  status: 'SUBMITTED', passStatus: ''
})

const sessionOptions = ref<any[]>([])
const profileOptions = ref<any[]>([])

const dialog = reactive({ visible: false, action: 'PASS' as 'PASS' | 'REJECT', id: 0, submitting: false })
const formRef = ref<FormInstance>()
const form = reactive<any>({})
const rules: FormRules = {}

function examTypeText(t?: string): string {
  if (t === 'THEORY') return '理论'
  if (t === 'PRACTICAL') return '实操'
  if (t === 'BOTH') return '理论与实操'
  return t || '-'
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
  const [s, p] = await Promise.all([
    listSessions({ pageNum: 1, pageSize: 200 }),
    listProfiles({ pageNum: 1, pageSize: 200 })
  ])
  sessionOptions.value = s.data?.rows || []
  profileOptions.value = p.data?.rows || []
}

function handleQuery() {
  queryParams.pageNum = 1
  loadData()
}
function resetQuery() {
  queryParams.sessionId = undefined
  queryParams.status = 'SUBMITTED'
  queryParams.passStatus = ''
  handleQuery()
}

function handleAudit(row: any, action: 'PASS' | 'REJECT') {
  Object.keys(form).forEach((k) => delete form[k])
  dialog.action = action
  dialog.id = row.id
  dialog.visible = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate()
  dialog.submitting = true
  try {
    await auditScore(dialog.id, { action: dialog.action, comment: form.comment })
    ElMessage.success(dialog.action === 'PASS' ? '已通过，合格判定已自动完成' : '已驳回')
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
