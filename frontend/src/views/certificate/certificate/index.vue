<template>
  <div class="page-card">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input v-model="queryParams.certNo" placeholder="证号" clearable class="!w-48" @keyup.enter="handleQuery" />
      <el-select v-model="queryParams.status" placeholder="状态" clearable class="!w-32">
        <el-option label="有效" value="VALID" />
        <el-option label="已换发" value="REISSUED" />
        <el-option label="已吊销" value="REVOKED" />
        <el-option label="已过期" value="EXPIRED" />
      </el-select>
      <el-select v-model="queryParams.studentUserId" placeholder="考生" clearable filterable class="!w-48">
        <el-option v-for="p in profileOptions" :key="p.id" :label="p.realName || p.name" :value="p.userId" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="handleQuery">查询</el-button>
      <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
    </div>

    <div class="toolbar">
      <span class="text-sm text-gray-500 dark:text-gray-400">共 {{ total }} 本合格证</span>
      <span class="text-xs text-gray-400 dark:text-gray-500">证号由系统在申请审核通过后异步签发</span>
    </div>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="certNo" label="证号" min-width="150" show-overflow-tooltip>
        <template #default="{ row }">
          <span class="font-mono text-xs">{{ row.certNo }}</span>
        </template>
      </el-table-column>
      <el-table-column label="考生" min-width="110" show-overflow-tooltip>
        <template #default="{ row }">{{ profileNameByUser(row.studentUserId) }}</template>
      </el-table-column>
      <el-table-column prop="certificateType" label="证类别" min-width="110" show-overflow-tooltip />
      <el-table-column label="签发日期" width="110">
        <template #default="{ row }">
          <span class="text-gray-600 dark:text-gray-300">{{ row.issueDate || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="有效期" width="200">
        <template #default="{ row }">
          <span class="text-gray-600 dark:text-gray-300">{{ row.validFrom || '-' }} ~ {{ row.validUntil || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <StatusTag :status="row.status" :map="statusMap" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" align="center" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === 'VALID'">
            <el-button v-hasPermi="'cert:certificate:reissue'" link type="primary" @click="handleReissue(row)">换发</el-button>
            <el-button v-hasPermi="'cert:certificate:revoke'" link type="danger" @click="handleRevoke(row)">吊销</el-button>
          </template>
          <el-button link type="info" @click="handleChanges(row)">变更记录</el-button>
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

    <!-- 换发弹窗 -->
    <el-dialog v-model="reissueDialog.visible" title="换发合格证" width="460px" destroy-on-close>
      <el-form ref="reissueFormRef" :model="reissueForm" :rules="reissueRules" label-width="90px">
        <el-form-item label="原证号">
          <el-input :model-value="reissueForm.certNo" disabled />
        </el-form-item>
        <el-form-item label="换发原因" prop="reason">
          <el-input v-model="reissueForm.reason" type="textarea" :rows="3" placeholder="如：证书信息变更 / 证书遗失" maxlength="200" />
        </el-form-item>
        <el-alert title="换发后原证标记为「已换发」，系统生成新证号并保留变更记录。" type="info" :closable="false" show-icon />
      </el-form>
      <template #footer>
        <el-button @click="reissueDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="reissueDialog.submitting" @click="handleReissueSubmit">确定换发</el-button>
      </template>
    </el-dialog>

    <!-- 吊销弹窗 -->
    <el-dialog v-model="revokeDialog.visible" title="吊销合格证" width="460px" destroy-on-close>
      <el-form ref="revokeFormRef" :model="revokeForm" :rules="revokeRules" label-width="90px">
        <el-form-item label="原证号">
          <el-input :model-value="revokeForm.certNo" disabled />
        </el-form-item>
        <el-form-item label="操作类型" prop="changeType">
          <el-radio-group v-model="revokeForm.changeType">
            <el-radio-button label="REVOKE">吊销</el-radio-button>
            <el-radio-button label="VOID">作废</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="原因" prop="reason">
          <el-input v-model="revokeForm.reason" type="textarea" :rows="3" placeholder="请填写吊销原因" maxlength="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="revokeDialog.visible = false">取消</el-button>
        <el-button type="danger" :loading="revokeDialog.submitting" @click="handleRevokeSubmit">确定吊销</el-button>
      </template>
    </el-dialog>

    <!-- 变更记录弹窗 -->
    <el-dialog v-model="changesDialog.visible" :title="`变更记录（${changesDialog.certNo}）`" width="720px" destroy-on-close>
      <el-table v-loading="changesDialog.loading" :data="changesList" border size="small">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="changeType" label="类型" width="100" align="center">
          <template #default="{ row }">
            <StatusTag :status="row.changeType" :map="changeTypeMap" />
          </template>
        </el-table-column>
        <el-table-column prop="beforeStatus" label="变更前" width="90" align="center">
          <template #default="{ row }">{{ statusText(row.beforeStatus) }}</template>
        </el-table-column>
        <el-table-column prop="afterStatus" label="变更后" width="90" align="center">
          <template #default="{ row }">{{ statusText(row.afterStatus) }}</template>
        </el-table-column>
        <el-table-column prop="newCertId" label="新证ID" width="80" align="center">
          <template #default="{ row }">{{ row.newCertId || '-' }}</template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" min-width="140" show-overflow-tooltip />
        <el-table-column label="操作时间" width="160">
          <template #default="{ row }">
            <span class="text-gray-600 dark:text-gray-300">{{ formatTime(row.operateTime) }}</span>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="changesDialog.visible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import StatusTag from '@/components/StatusTag.vue'
import { listCertificates, getCertificateChanges, reissueCertificate, revokeCertificate } from '@/api/certificate'
import { listProfiles } from '@/api/student'

const statusMap: Record<string, { label: string; type: string }> = {
  VALID: { label: '有效', type: 'success' },
  REISSUED: { label: '已换发', type: 'warning' },
  REVOKED: { label: '已吊销', type: 'danger' },
  EXPIRED: { label: '已过期', type: 'info' },
  VOID: { label: '已作废', type: 'danger' }
}
const changeTypeMap: Record<string, { label: string; type: string }> = {
  REISSUE: { label: '换发', type: 'warning' },
  REVOKE: { label: '吊销', type: 'danger' },
  VOID: { label: '作废', type: 'danger' }
}

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({ pageNum: 1, pageSize: 10, certNo: '', status: '', studentUserId: undefined as number | undefined })

const profileOptions = ref<any[]>([])

const reissueDialog = reactive({ visible: false, submitting: false })
const reissueFormRef = ref<FormInstance>()
const reissueForm = reactive<any>({})
const reissueRules: FormRules = {
  reason: [{ required: true, message: '请填写换发原因', trigger: 'blur' }]
}

const revokeDialog = reactive({ visible: false, submitting: false })
const revokeFormRef = ref<FormInstance>()
const revokeForm = reactive<any>({})
const revokeRules: FormRules = {
  reason: [{ required: true, message: '请填写吊销原因', trigger: 'blur' }]
}

const changesDialog = reactive({ visible: false, loading: false, certNo: '' })
const changesList = ref<any[]>([])

function formatTime(v?: string): string {
  if (!v) return '-'
  return String(v).replace('T', ' ').slice(0, 19)
}
function statusText(s?: string): string {
  return statusMap[s || '']?.label || s || '-'
}
function profileNameByUser(userId?: number): string {
  if (!userId) return '-'
  const p = profileOptions.value.find((x) => x.userId === userId)
  return p ? p.realName || p.name || `#${userId}` : `#${userId}`
}

async function loadData() {
  loading.value = true
  try {
    const res = await listCertificates({
      pageNum: queryParams.pageNum,
      pageSize: queryParams.pageSize,
      certNo: queryParams.certNo || undefined,
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
  const p = await listProfiles({ pageNum: 1, pageSize: 200 })
  profileOptions.value = p.data?.rows || []
}

function handleQuery() {
  queryParams.pageNum = 1
  loadData()
}
function resetQuery() {
  queryParams.certNo = ''
  queryParams.status = ''
  queryParams.studentUserId = undefined
  handleQuery()
}

function handleReissue(row: any) {
  Object.keys(reissueForm).forEach((k) => delete reissueForm[k])
  reissueForm.id = row.id
  reissueForm.certNo = row.certNo
  reissueDialog.visible = true
}
async function handleReissueSubmit() {
  if (!reissueFormRef.value) return
  await reissueFormRef.value.validate()
  reissueDialog.submitting = true
  try {
    await reissueCertificate(reissueForm.id, reissueForm.reason)
    ElMessage.success('换发成功，新证号已生成')
    reissueDialog.visible = false
    loadData()
  } finally {
    reissueDialog.submitting = false
  }
}

function handleRevoke(row: any) {
  Object.keys(revokeForm).forEach((k) => delete revokeForm[k])
  revokeForm.id = row.id
  revokeForm.certNo = row.certNo
  revokeForm.changeType = 'REVOKE'
  revokeDialog.visible = true
}
async function handleRevokeSubmit() {
  if (!revokeFormRef.value) return
  await revokeFormRef.value.validate()
  revokeDialog.submitting = true
  try {
    await revokeCertificate(revokeForm.id, revokeForm.reason, revokeForm.changeType)
    ElMessage.success('操作成功')
    revokeDialog.visible = false
    loadData()
  } finally {
    revokeDialog.submitting = false
  }
}

async function handleChanges(row: any) {
  changesDialog.certNo = row.certNo
  changesDialog.visible = true
  changesDialog.loading = true
  try {
    const res = await getCertificateChanges(row.id)
    changesList.value = res.data || []
  } finally {
    changesDialog.loading = false
  }
}

onMounted(async () => {
  await loadOptions()
  await loadData()
})
</script>
