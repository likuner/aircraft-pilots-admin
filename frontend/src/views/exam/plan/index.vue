<template>
  <div class="page-card">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input v-model="queryParams.keyword" placeholder="计划名称" clearable class="!w-52" @keyup.enter="handleQuery" />
      <el-select v-model="queryParams.status" placeholder="状态" clearable class="!w-36">
        <el-option label="草稿" value="DRAFT" />
        <el-option label="已发布" value="PUBLISHED" />
        <el-option label="已取消" value="CANCELED" />
        <el-option label="已结束" value="CLOSED" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="handleQuery">查询</el-button>
      <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
    </div>

    <div class="toolbar">
      <span class="text-sm text-gray-500 dark:text-gray-400">共 {{ total }} 条计划</span>
      <el-button v-hasPermi="'exam:plan:add'" type="primary" :icon="Plus" @click="handleAdd">新增计划</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="planCode" label="计划编号" min-width="120" show-overflow-tooltip />
      <el-table-column prop="planName" label="计划名称" min-width="200" show-overflow-tooltip />
      <el-table-column label="考试类型" width="110" align="center">
        <template #default="{ row }">{{ examTypeText(row.examType) }}</template>
      </el-table-column>
      <el-table-column label="考试时间" width="200">
        <template #default="{ row }">
          <span class="text-gray-600 dark:text-gray-300">{{ row.startDate }} ~ {{ row.endDate }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="region" label="区域" width="110" show-overflow-tooltip />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <StatusTag :status="row.status" :map="statusMap" />
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="160">
        <template #default="{ row }">
          <span class="text-gray-600 dark:text-gray-300">{{ formatTime(row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="230" align="center" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 'DRAFT'" link type="success" @click="handlePublish(row)">发布</el-button>
          <el-button v-if="row.status === 'PUBLISHED'" link type="warning" @click="handleCancel(row)">取消</el-button>
          <el-button v-hasPermi="'exam:plan:edit'" link type="primary" @click="handleEdit(row)">编辑</el-button>
          <el-button v-hasPermi="'exam:plan:delete'" link type="danger" @click="handleDelete(row)">删除</el-button>
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialog.visible" :title="dialog.mode === 'add' ? '新增计划' : '编辑计划'" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="计划名称" prop="planName">
          <el-input v-model="form.planName" placeholder="请输入计划名称" maxlength="100" />
        </el-form-item>
        <el-form-item label="考试类型" prop="examType">
          <el-select v-model="form.examType" placeholder="请选择" class="!w-full">
            <el-option v-for="t in examTypes" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="考试周期" prop="startDate">
          <el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始日期" end-placeholder="结束日期" class="!w-full" />
        </el-form-item>
        <el-form-item label="区域" prop="region">
          <el-input v-model="form.region" placeholder="如：广东省" maxlength="50" />
        </el-form-item>
        <el-form-item label="说明" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="计划说明" maxlength="255" />
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
import { listPlans, createPlan, updatePlan, deletePlan, publishPlan, cancelPlan } from '@/api/exam'

const examTypes = [
  { label: '理论', value: 'THEORY' },
  { label: '实操', value: 'PRACTICAL' },
  { label: '理论与实操', value: 'BOTH' }
]

function examTypeText(t?: string): string {
  if (t === 'THEORY') return '理论'
  if (t === 'PRACTICAL') return '实操'
  if (t === 'BOTH') return '理论与实操'
  return t || '-'
}

const statusMap: Record<string, { label: string; type: string }> = {
  DRAFT: { label: '草稿', type: 'info' },
  PUBLISHED: { label: '已发布', type: 'success' },
  CANCELED: { label: '已取消', type: 'danger' },
  CLOSED: { label: '已结束', type: 'info' }
}

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({ pageNum: 1, pageSize: 10, keyword: '', status: '' })
const dateRange = ref<[string, string] | null>(null)

const dialog = reactive({ visible: false, mode: 'add' as 'add' | 'edit', submitting: false })
const formRef = ref<FormInstance>()
const form = reactive<any>({})
const rules: FormRules = {
  planName: [{ required: true, message: '请输入计划名称', trigger: 'blur' }],
  examType: [{ required: true, message: '请选择考试类型', trigger: 'change' }]
}

function formatTime(v?: string): string {
  if (!v) return '-'
  return String(v).replace('T', ' ').slice(0, 19)
}

async function loadData() {
  loading.value = true
  try {
    const res = await listPlans({
      pageNum: queryParams.pageNum,
      pageSize: queryParams.pageSize,
      keyword: queryParams.keyword || undefined,
      status: queryParams.status || undefined
    })
    list.value = res.data?.rows || []
    total.value = res.data?.total || 0
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
  queryParams.status = ''
  handleQuery()
}

function handleAdd() {
  Object.keys(form).forEach((k) => delete form[k])
  form.examType = 'THEORY'
  dateRange.value = null
  dialog.mode = 'add'
  dialog.visible = true
}

function handleEdit(row: any) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, { ...row })
  dateRange.value = row.startDate && row.endDate ? [row.startDate, row.endDate] : null
  dialog.mode = 'edit'
  dialog.visible = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate()
  const payload: Record<string, any> = { ...form }
  if (dateRange.value) {
    payload.startDate = dateRange.value[0]
    payload.endDate = dateRange.value[1]
  }
  dialog.submitting = true
  try {
    if (dialog.mode === 'add') {
      await createPlan(payload)
      ElMessage.success('新增成功')
    } else {
      await updatePlan(payload)
      ElMessage.success('保存成功')
    }
    dialog.visible = false
    loadData()
  } finally {
    dialog.submitting = false
  }
}

async function handlePublish(row: any) {
  await ElMessageBox.confirm(`确定发布计划「${row.planName}」吗？`, '提示', { type: 'warning' })
  await publishPlan(row.id)
  ElMessage.success('发布成功')
  loadData()
}

async function handleCancel(row: any) {
  await ElMessageBox.confirm(`确定取消计划「${row.planName}」吗？取消后不可恢复。`, '提示', { type: 'warning' })
  await cancelPlan(row.id)
  ElMessage.success('已取消')
  loadData()
}

async function handleDelete(row: any) {
  await ElMessageBox.confirm(`确定删除计划「${row.planName}」吗？`, '提示', { type: 'warning' })
  await deletePlan(row.id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(loadData)
</script>
