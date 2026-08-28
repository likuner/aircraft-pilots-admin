<template>
  <div class="page-card">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input v-model="queryParams.keyword" placeholder="资质证号" clearable class="!w-48" @keyup.enter="handleQuery" />
      <el-select v-model="queryParams.institutionId" placeholder="训练机构" clearable filterable class="!w-52">
        <el-option v-for="i in instOptions" :key="i.id" :label="i.instName" :value="i.id" />
      </el-select>
      <el-select v-model="queryParams.status" placeholder="状态" clearable class="!w-32">
        <el-option label="有效" value="VALID" />
        <el-option label="已暂停" value="SUSPENDED" />
        <el-option label="已吊销" value="REVOKED" />
        <el-option label="已过期" value="EXPIRED" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="handleQuery">查询</el-button>
      <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
    </div>

    <div class="toolbar">
      <span class="text-sm text-gray-500 dark:text-gray-400">共 {{ total }} 张资质证</span>
      <span class="text-xs text-gray-400 dark:text-gray-500">资质证在认证申请评定通过后自动签发</span>
    </div>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="qualificationNo" label="资质证号" min-width="150" show-overflow-tooltip>
        <template #default="{ row }">
          <span class="font-mono text-xs">{{ row.qualificationNo }}</span>
        </template>
      </el-table-column>
      <el-table-column label="机构" min-width="170" show-overflow-tooltip>
        <template #default="{ row }">{{ instName(row.institutionId) }}</template>
      </el-table-column>
      <el-table-column prop="qualificationLevel" label="资质等级" min-width="100" show-overflow-tooltip />
      <el-table-column prop="category" label="认证类别" min-width="120" show-overflow-tooltip />
      <el-table-column label="签发日期" width="110">
        <template #default="{ row }">
          <span class="text-gray-600 dark:text-gray-300">{{ row.issueDate || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="有效期至" width="110">
        <template #default="{ row }">
          <span class="text-gray-600 dark:text-gray-300">{{ row.validUntil || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <StatusTag :status="row.status" :map="statusMap" />
        </template>
      </el-table-column>
      <el-table-column prop="revokeReason" label="吊销原因" min-width="120" show-overflow-tooltip />
      <el-table-column label="操作" width="150" align="center" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === 'VALID'">
            <el-button v-hasPermi="'inst:qualification:list'" link type="primary" @click="handleRenew(row)">续期</el-button>
            <el-button v-hasPermi="'inst:qualification:revoke'" link type="danger" @click="handleRevoke(row)">吊销</el-button>
          </template>
          <span v-else class="text-gray-400 text-xs">无操作</span>
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

    <!-- 吊销弹窗 -->
    <el-dialog v-model="revokeDialog.visible" title="吊销资质证" width="460px" destroy-on-close>
      <el-form ref="revokeFormRef" :model="revokeForm" :rules="revokeRules" label-width="90px">
        <el-form-item label="资质证号">
          <el-input :model-value="revokeForm.qualificationNo" disabled />
        </el-form-item>
        <el-form-item label="操作类型" prop="changeType">
          <el-radio-group v-model="revokeForm.changeType">
            <el-radio-button label="REVOKE">吊销</el-radio-button>
            <el-radio-button label="SUSPENDED">暂停</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="原因" prop="reason">
          <el-input v-model="revokeForm.reason" type="textarea" :rows="3" placeholder="请填写原因" maxlength="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="revokeDialog.visible = false">取消</el-button>
        <el-button type="danger" :loading="revokeDialog.submitting" @click="handleRevokeSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import StatusTag from '@/components/StatusTag.vue'
import { listQualifications, renewQualification, revokeQualification } from '@/api/institution'
import { listInstitutions } from '@/api/institution'

const statusMap: Record<string, { label: string; type: string }> = {
  VALID: { label: '有效', type: 'success' },
  SUSPENDED: { label: '已暂停', type: 'warning' },
  REVOKED: { label: '已吊销', type: 'danger' },
  EXPIRED: { label: '已过期', type: 'info' }
}

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({ pageNum: 1, pageSize: 10, keyword: '', institutionId: undefined as number | undefined, status: '' })

const instOptions = ref<any[]>([])

const revokeDialog = reactive({ visible: false, submitting: false })
const revokeFormRef = ref<FormInstance>()
const revokeForm = reactive<any>({})
const revokeRules: FormRules = {
  reason: [{ required: true, message: '请填写原因', trigger: 'blur' }]
}

function instName(id?: number): string {
  if (!id) return '-'
  return instOptions.value.find((i) => i.id === id)?.instName || `#${id}`
}

async function loadData() {
  loading.value = true
  try {
    const res = await listQualifications({
      pageNum: queryParams.pageNum,
      pageSize: queryParams.pageSize,
      keyword: queryParams.keyword || undefined,
      institutionId: queryParams.institutionId,
      status: queryParams.status || undefined
    })
    list.value = res.data?.rows || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

async function loadOptions() {
  const i = await listInstitutions({ pageNum: 1, pageSize: 200 })
  instOptions.value = i.data?.rows || []
}

function handleQuery() {
  queryParams.pageNum = 1
  loadData()
}
function resetQuery() {
  queryParams.keyword = ''
  queryParams.institutionId = undefined
  queryParams.status = ''
  handleQuery()
}

async function handleRenew(row: any) {
  await ElMessageBox.confirm(`确定为机构资质证「${row.qualificationNo}」续期吗？`, '提示', { type: 'warning' })
  await renewQualification(row.id)
  ElMessage.success('续期成功')
  loadData()
}

function handleRevoke(row: any) {
  Object.keys(revokeForm).forEach((k) => delete revokeForm[k])
  revokeForm.id = row.id
  revokeForm.qualificationNo = row.qualificationNo
  revokeForm.changeType = 'REVOKE'
  revokeDialog.visible = true
}

async function handleRevokeSubmit() {
  if (!revokeFormRef.value) return
  await revokeFormRef.value.validate()
  revokeDialog.submitting = true
  try {
    await revokeQualification(revokeForm.id, revokeForm.reason, revokeForm.changeType)
    ElMessage.success('操作成功')
    revokeDialog.visible = false
    loadData()
  } finally {
    revokeDialog.submitting = false
  }
}

onMounted(async () => {
  await loadOptions()
  await loadData()
})
</script>
