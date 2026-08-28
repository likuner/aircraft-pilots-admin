<template>
  <div class="page-card">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-select v-model="queryParams.institutionId" placeholder="训练机构" clearable filterable class="!w-52">
        <el-option v-for="i in instOptions" :key="i.id" :label="i.instName" :value="i.id" />
      </el-select>
      <el-select v-model="queryParams.status" placeholder="状态" clearable class="!w-40">
        <el-option v-for="(v, k) in statusMap" :key="k" :label="v.label" :value="k" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="handleQuery">查询</el-button>
      <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
    </div>

    <div class="toolbar">
      <span class="text-sm text-gray-500 dark:text-gray-400">共 {{ total }} 条认证申请</span>
      <el-button v-hasPermi="'inst:application:submit'" type="primary" :icon="Plus" @click="handleAdd">提交认证申请</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="applyNo" label="申请编号" min-width="140" show-overflow-tooltip />
      <el-table-column label="机构" min-width="170" show-overflow-tooltip>
        <template #default="{ row }">{{ instName(row.institutionId) }}</template>
      </el-table-column>
      <el-table-column label="申请类型" width="100" align="center">
        <template #default="{ row }">
          <StatusTag :status="row.applyType" :map="applyTypeMap" />
        </template>
      </el-table-column>
      <el-table-column prop="category" label="认证类别" min-width="120" show-overflow-tooltip />
      <el-table-column label="阶段" width="80" align="center">
        <template #default="{ row }">
          <el-tag size="small" effect="plain">{{ row.currentStep }}/5</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="110" align="center">
        <template #default="{ row }">
          <StatusTag :status="row.status" :map="statusMap" />
        </template>
      </el-table-column>
      <el-table-column label="申请时间" width="160">
        <template #default="{ row }">
          <span class="text-gray-600 dark:text-gray-300">{{ formatTime(row.applyTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" align="center" fixed="right">
        <template #default="{ row }">
          <el-button v-hasPermi="'inst:application:list'" link type="primary" @click="handleDetail(row)">详情</el-button>
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

    <!-- 新增认证申请弹窗 -->
    <el-dialog v-model="dialog.visible" title="提交认证申请" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="训练机构" prop="institutionId">
          <el-select v-model="form.institutionId" placeholder="请选择" filterable class="!w-full">
            <el-option v-for="i in instOptions" :key="i.id" :label="i.instName" :value="i.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="申请类型" prop="applyType">
          <el-select v-model="form.applyType" placeholder="请选择" class="!w-full">
            <el-option label="首次认证" value="NEW" />
            <el-option label="续期认证" value="RENEW" />
            <el-option label="变更认证" value="CHANGE" />
          </el-select>
        </el-form-item>
        <el-form-item label="认证类别" prop="category">
          <el-input v-model="form.category" placeholder="如：多旋翼驾驶员培训" maxlength="50" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="dialog.submitting" @click="handleSubmit">提交</el-button>
      </template>
    </el-dialog>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detail.visible" :title="`认证申请详情（${detail.applyNo}）`" size="720px" destroy-on-close>
      <div v-loading="detail.loading">
        <template v-if="detail.data">
          <!-- 基本信息 -->
          <el-descriptions :column="2" border size="small" class="mb-4">
            <el-descriptions-item label="申请编号">{{ detail.data.apply?.applyNo }}</el-descriptions-item>
            <el-descriptions-item label="机构">{{ instName(detail.data.apply?.institutionId) }}</el-descriptions-item>
            <el-descriptions-item label="申请类型">
              <StatusTag :status="detail.data.apply?.applyType" :map="applyTypeMap" />
            </el-descriptions-item>
            <el-descriptions-item label="认证类别">{{ detail.data.apply?.category || '-' }}</el-descriptions-item>
            <el-descriptions-item label="当前阶段">{{ detail.data.apply?.currentStep }}/5</el-descriptions-item>
            <el-descriptions-item label="状态">
              <StatusTag :status="detail.data.apply?.status" :map="statusMap" />
            </el-descriptions-item>
            <el-descriptions-item label="申请时间">{{ formatTime(detail.data.apply?.applyTime) }}</el-descriptions-item>
          </el-descriptions>

          <!-- 操作按钮（按状态显示） -->
          <div class="mb-4 flex flex-wrap gap-2">
            <el-button
              v-if="['SUBMITTED', 'MATERIAL_REJECTED'].includes(detail.data.apply?.status)"
              v-hasPermi="'inst:application:submit'"
              type="primary" size="small" :icon="Upload"
              @click="materialDialog.visible = true"
            >提交申请材料</el-button>
            <el-button
              v-if="['MATERIAL_PASSED', 'INSPECTION_SCHEDULED'].includes(detail.data.apply?.status)"
              v-hasPermi="'inst:inspection:assign'"
              type="warning" size="small" :icon="Van"
              @click="handleAssignOpen"
            >指派现场核查</el-button>
            <el-button
              v-if="detail.data.apply?.status === 'QUALIFICATION_REVIEWING'"
              v-hasPermi="'inst:qualification:list'"
              type="success" size="small" :icon="Medal"
              @click="handleQualifyOpen"
            >资质评定</el-button>
            <el-button
              v-if="detail.data.apply?.status === 'INSPECTION_SCHEDULED'"
              size="small" type="info" plain
            >已派发核查任务，请前往「实地核查」页完成</el-button>
          </div>

          <el-divider content-position="left">申请材料（{{ detail.data.materials?.length || 0 }}）</el-divider>
          <el-table :data="detail.data.materials || []" border size="small" class="mb-4">
            <el-table-column prop="materialType" label="材料类型" width="120" />
            <el-table-column prop="fileName" label="文件名称" min-width="140" show-overflow-tooltip />
            <el-table-column prop="fileUrl" label="文件地址" min-width="160" show-overflow-tooltip />
            <el-table-column label="上传时间" width="160">
              <template #default="{ row }">
                <span class="text-gray-600 dark:text-gray-300">{{ formatTime(row.uploadTime) }}</span>
              </template>
            </el-table-column>
          </el-table>

          <el-divider content-position="left">材料审查记录（{{ detail.data.reviews?.length || 0 }}）</el-divider>
          <el-table :data="detail.data.reviews || []" border size="small" class="mb-4">
            <el-table-column prop="reviewStep" label="轮次" width="70" align="center" />
            <el-table-column prop="result" label="结果" width="90" align="center">
              <template #default="{ row }">
                <StatusTag :status="row.result" :map="passFailMap" />
              </template>
            </el-table-column>
            <el-table-column prop="comment" label="意见" min-width="140" show-overflow-tooltip />
            <el-table-column label="审查时间" width="160">
              <template #default="{ row }">
                <span class="text-gray-600 dark:text-gray-300">{{ formatTime(row.reviewTime) }}</span>
              </template>
            </el-table-column>
          </el-table>

          <el-divider content-position="left">现场核查记录（{{ detail.data.inspections?.length || 0 }}）</el-divider>
          <el-table :data="detail.data.inspections || []" border size="small" class="mb-4">
            <el-table-column prop="inspectionDate" label="核查日期" width="110" />
            <el-table-column prop="address" label="地点" min-width="130" show-overflow-tooltip />
            <el-table-column prop="result" label="结果" width="90" align="center">
              <template #default="{ row }">
                <StatusTag :status="row.result || 'PENDING'" :map="passFailPendingMap" />
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100" align="center">
              <template #default="{ row }">
                <StatusTag :status="row.status" :map="inspectionStatusMap" />
              </template>
            </el-table-column>
            <el-table-column prop="summary" label="结论" min-width="120" show-overflow-tooltip />
          </el-table>

          <el-divider content-position="left">资质评定记录（{{ detail.data.qualificationReviews?.length || 0 }}）</el-divider>
          <el-table :data="detail.data.qualificationReviews || []" border size="small">
            <el-table-column prop="evaluationScore" label="评定分" width="80" align="center" />
            <el-table-column prop="result" label="结果" width="90" align="center">
              <template #default="{ row }">
                <StatusTag :status="row.result" :map="passFailMap" />
              </template>
            </el-table-column>
            <el-table-column prop="suggestion" label="建议" min-width="140" show-overflow-tooltip />
            <el-table-column label="评定时间" width="160">
              <template #default="{ row }">
                <span class="text-gray-600 dark:text-gray-300">{{ formatTime(row.reviewTime) }}</span>
              </template>
            </el-table-column>
          </el-table>
        </template>
      </div>
    </el-drawer>

    <!-- 提交材料弹窗 -->
    <el-dialog v-model="materialDialog.visible" title="提交申请材料" width="720px" append-to-body destroy-on-close>
      <div class="mb-3">
        <el-button type="primary" size="small" :icon="Plus" @click="addMaterialRow">添加材料</el-button>
      </div>
      <el-table :data="materialRows" border size="small">
        <el-table-column label="材料类型" min-width="140">
          <template #default="{ row }">
            <el-input v-model="row.materialType" placeholder="如：营业执照 / 场地证明" maxlength="30" />
          </template>
        </el-table-column>
        <el-table-column label="文件名称" min-width="150">
          <template #default="{ row }">
            <el-input v-model="row.fileName" placeholder="文件名" maxlength="100" />
          </template>
        </el-table-column>
        <el-table-column label="文件地址" min-width="180">
          <template #default="{ row }">
            <el-input v-model="row.fileUrl" placeholder="URL" maxlength="255" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="70" align="center">
          <template #default="{ $index }">
            <el-button link type="danger" @click="materialRows.splice($index, 1)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="materialDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="materialDialog.submitting" @click="handleMaterialSubmit">提交</el-button>
      </template>
    </el-dialog>

    <!-- 指派核查弹窗 -->
    <el-dialog v-model="assignDialog.visible" title="指派现场核查" width="520px" append-to-body destroy-on-close>
      <el-form ref="assignFormRef" :model="assignForm" :rules="assignRules" label-width="90px">
        <el-form-item label="核查人员" prop="inspectorId">
          <el-select v-model="assignForm.inspectorId" placeholder="请选择" filterable class="!w-full">
            <el-option v-for="u in userOptions" :key="u.id" :label="u.realName || u.username" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="核查日期" prop="inspectionDate">
          <el-date-picker v-model="assignForm.inspectionDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" class="!w-full" />
        </el-form-item>
        <el-form-item label="核查地点" prop="address">
          <el-input v-model="assignForm.address" placeholder="请输入" maxlength="150" />
        </el-form-item>
        <el-form-item label="核查清单" prop="checklist">
          <el-input v-model="assignForm.checklist" type="textarea" :rows="3" placeholder="核查要点，逗号分隔" maxlength="255" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignDialog.visible = false">取消</el-button>
        <el-button type="warning" :loading="assignDialog.submitting" @click="handleAssignSubmit">派发任务</el-button>
      </template>
    </el-dialog>

    <!-- 资质评定弹窗 -->
    <el-dialog v-model="qualifyDialog.visible" title="资质评定" width="520px" append-to-body destroy-on-close>
      <el-form ref="qualifyFormRef" :model="qualifyForm" :rules="qualifyRules" label-width="90px">
        <el-form-item label="评定结果" prop="result">
          <el-radio-group v-model="qualifyForm.result">
            <el-radio-button label="PASS">通过并发证</el-radio-button>
            <el-radio-button label="REJECT">不通过</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="评定分数" prop="evaluationScore">
          <el-input-number v-model="qualifyForm.evaluationScore" :min="0" :max="100" :precision="1" class="!w-full" />
        </el-form-item>
        <el-form-item label="评定建议" prop="suggestion">
          <el-input v-model="qualifyForm.suggestion" type="textarea" :rows="3" placeholder="请填写评定建议" maxlength="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="qualifyDialog.visible = false">取消</el-button>
        <el-button type="success" :loading="qualifyDialog.submitting" @click="handleQualifySubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh, Plus, Upload, Van, Medal } from '@element-plus/icons-vue'
import StatusTag from '@/components/StatusTag.vue'
import {
  listApplications, getApplication, createApplication,
  submitMaterials, assignInspection, qualifyApplication
} from '@/api/institution'
import { listInstitutions } from '@/api/institution'
import { listUsers } from '@/api/system'

const statusMap: Record<string, { label: string; type: string }> = {
  SUBMITTED: { label: '已提交', type: 'info' },
  MATERIAL_REVIEWING: { label: '材料审查中', type: 'warning' },
  MATERIAL_PASSED: { label: '材料已通过', type: 'success' },
  MATERIAL_REJECTED: { label: '材料已退回', type: 'danger' },
  INSPECTION_PENDING: { label: '待核查', type: 'warning' },
  INSPECTION_SCHEDULED: { label: '核查已派', type: 'primary' },
  INSPECTED: { label: '核查完成', type: 'success' },
  QUALIFICATION_REVIEWING: { label: '资质评审中', type: 'warning' },
  APPROVED: { label: '已通过', type: 'success' },
  REJECTED: { label: '已驳回', type: 'danger' },
  CANCELLED: { label: '已撤销', type: 'info' }
}
const applyTypeMap: Record<string, { label: string; type: string }> = {
  NEW: { label: '首次认证', type: 'primary' },
  RENEW: { label: '续期认证', type: 'warning' },
  CHANGE: { label: '变更认证', type: 'info' }
}
const passFailMap: Record<string, { label: string; type: string }> = {
  PASS: { label: '通过', type: 'success' },
  FAIL: { label: '不通过', type: 'danger' }
}
const passFailPendingMap: Record<string, { label: string; type: string }> = {
  PASS: { label: '通过', type: 'success' },
  FAIL: { label: '不通过', type: 'danger' },
  PENDING: { label: '未核查', type: 'info' }
}
const inspectionStatusMap: Record<string, { label: string; type: string }> = {
  ASSIGNED: { label: '待核查', type: 'warning' },
  COMPLETED: { label: '已完成', type: 'success' }
}

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({ pageNum: 1, pageSize: 10, institutionId: undefined as number | undefined, status: '' })

const instOptions = ref<any[]>([])
const userOptions = ref<any[]>([])

const dialog = reactive({ visible: false, submitting: false })
const formRef = ref<FormInstance>()
const form = reactive<any>({})
const rules: FormRules = {
  institutionId: [{ required: true, message: '请选择训练机构', trigger: 'change' }],
  applyType: [{ required: true, message: '请选择申请类型', trigger: 'change' }],
  category: [{ required: true, message: '请输入认证类别', trigger: 'blur' }]
}

const detail = reactive({ visible: false, loading: false, applyNo: '', data: null as any })
let detailId = 0

const materialDialog = reactive({ visible: false, submitting: false })
const materialRows = ref<any[]>([])

const assignDialog = reactive({ visible: false, submitting: false })
const assignFormRef = ref<FormInstance>()
const assignForm = reactive<any>({})
const assignRules: FormRules = {
  inspectorId: [{ required: true, message: '请选择核查人员', trigger: 'change' }],
  inspectionDate: [{ required: true, message: '请选择核查日期', trigger: 'change' }],
  address: [{ required: true, message: '请输入核查地点', trigger: 'blur' }]
}

const qualifyDialog = reactive({ visible: false, submitting: false })
const qualifyFormRef = ref<FormInstance>()
const qualifyForm = reactive<any>({})
const qualifyRules: FormRules = {
  result: [{ required: true, message: '请选择评定结果', trigger: 'change' }],
  suggestion: [{ required: true, message: '请填写评定建议', trigger: 'blur' }]
}

function formatTime(v?: string): string {
  if (!v) return '-'
  return String(v).replace('T', ' ').slice(0, 19)
}
function instName(id?: number): string {
  if (!id) return '-'
  return instOptions.value.find((i) => i.id === id)?.instName || `#${id}`
}

async function loadData() {
  loading.value = true
  try {
    const res = await listApplications({
      pageNum: queryParams.pageNum,
      pageSize: queryParams.pageSize,
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
  queryParams.institutionId = undefined
  queryParams.status = ''
  handleQuery()
}

function handleAdd() {
  Object.keys(form).forEach((k) => delete form[k])
  form.applyType = 'NEW'
  dialog.visible = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate()
  dialog.submitting = true
  try {
    await createApplication(form)
    ElMessage.success('认证申请已提交')
    dialog.visible = false
    loadData()
  } finally {
    dialog.submitting = false
  }
}

async function handleDetail(row: any) {
  detailId = row.id
  detail.applyNo = row.applyNo
  detail.visible = true
  detail.loading = true
  try {
    const res = await getApplication(row.id)
    detail.data = res.data
  } finally {
    detail.loading = false
  }
}

// ---- 提交材料 ----
function addMaterialRow() {
  materialRows.value.push({ materialType: '', fileName: '', fileUrl: '' })
}
async function handleMaterialSubmit() {
  const valid = materialRows.value.every((r) => r.materialType && r.fileName)
  if (!valid) {
    ElMessage.warning('请完整填写材料类型与文件名称')
    return
  }
  materialDialog.submitting = true
  try {
    await submitMaterials(detailId, materialRows.value)
    ElMessage.success('材料已提交，进入审查')
    materialDialog.visible = false
    materialRows.value = []
    await handleDetail({ id: detailId, applyNo: detail.applyNo })
  } finally {
    materialDialog.submitting = false
  }
}

// ---- 指派核查 ----
function handleAssignOpen() {
  Object.keys(assignForm).forEach((k) => delete assignForm[k])
  assignDialog.visible = true
}
async function handleAssignSubmit() {
  if (!assignFormRef.value) return
  await assignFormRef.value.validate()
  assignDialog.submitting = true
  try {
    await assignInspection(detailId, { ...assignForm })
    ElMessage.success('核查任务已派发')
    assignDialog.visible = false
    await handleDetail({ id: detailId, applyNo: detail.applyNo })
  } finally {
    assignDialog.submitting = false
  }
}

// ---- 资质评定 ----
function handleQualifyOpen() {
  Object.keys(qualifyForm).forEach((k) => delete qualifyForm[k])
  qualifyForm.result = 'PASS'
  qualifyDialog.visible = true
}
async function handleQualifySubmit() {
  if (!qualifyFormRef.value) return
  await qualifyFormRef.value.validate()
  qualifyDialog.submitting = true
  try {
    await qualifyApplication(detailId, { ...qualifyForm })
    ElMessage.success(qualifyForm.result === 'PASS' ? '评定通过，资质证已签发' : '评定不通过')
    qualifyDialog.visible = false
    await handleDetail({ id: detailId, applyNo: detail.applyNo })
    loadData()
  } finally {
    qualifyDialog.submitting = false
  }
}

onMounted(async () => {
  await loadOptions()
  await loadData()
})
</script>
