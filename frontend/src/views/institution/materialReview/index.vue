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
      <span class="text-sm text-gray-500 dark:text-gray-400">共 {{ total }} 条待办/历史申请</span>
      <span class="text-xs text-gray-400 dark:text-gray-500">材料审查通过后进入下一步：指派现场核查</span>
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
      <el-table-column label="操作" width="110" align="center" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 'MATERIAL_REVIEWING'"
            v-hasPermi="'inst:material:review'"
            link type="primary"
            @click="handleReview(row)"
          >审查材料</el-button>
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

    <!-- 材料审查弹窗 -->
    <el-dialog v-model="dialog.visible" :title="`材料审查（${dialog.applyNo}）`" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="审查轮次" prop="reviewStep">
          <el-input-number v-model="form.reviewStep" :min="1" :max="5" :precision="0" class="!w-full" />
        </el-form-item>
        <el-form-item label="审查结果" prop="result">
          <el-radio-group v-model="form.result">
            <el-radio-button label="PASS">通过</el-radio-button>
            <el-radio-button label="REJECT">退回补充</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审查意见" prop="comment">
          <el-input v-model="form.comment" type="textarea" :rows="3" :placeholder="form.result === 'PASS' ? '选填' : '请说明退回原因'" maxlength="200" />
        </el-form-item>
        <el-alert
          v-if="form.result === 'PASS'"
          title="通过后申请将进入「指派现场核查」环节。"
          type="info" :closable="false" show-icon
        />
        <el-alert
          v-else
          title="退回后申请回到「材料提交」环节，机构需补充材料后重新提交。"
          type="warning" :closable="false" show-icon
        />
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button
          :type="form.result === 'PASS' ? 'success' : 'danger'"
          :loading="dialog.submitting"
          @click="handleSubmit"
        >{{ form.result === 'PASS' ? '确定通过' : '确定退回' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import StatusTag from '@/components/StatusTag.vue'
import { listApplications, reviewMaterial } from '@/api/institution'
import { listInstitutions } from '@/api/institution'

const statusMap: Record<string, { label: string; type: string }> = {
  SUBMITTED: { label: '已提交', type: 'info' },
  MATERIAL_REVIEWING: { label: '材料审查中', type: 'warning' },
  MATERIAL_PASSED: { label: '材料已通过', type: 'success' },
  MATERIAL_REJECTED: { label: '材料已退回', type: 'danger' }
}
const applyTypeMap: Record<string, { label: string; type: string }> = {
  NEW: { label: '首次认证', type: 'primary' },
  RENEW: { label: '续期认证', type: 'warning' },
  CHANGE: { label: '变更认证', type: 'info' }
}

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({ pageNum: 1, pageSize: 10, institutionId: undefined as number | undefined, status: 'MATERIAL_REVIEWING' })

const instOptions = ref<any[]>([])

const dialog = reactive({ visible: false, submitting: false, applyNo: '' })
const formRef = ref<FormInstance>()
const form = reactive<any>({})
const rules: FormRules = {
  result: [{ required: true, message: '请选择审查结果', trigger: 'change' }],
  reviewStep: [{ required: true, message: '请输入审查轮次', trigger: 'blur' }]
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
  const i = await listInstitutions({ pageNum: 1, pageSize: 200 })
  instOptions.value = i.data?.rows || []
}

function handleQuery() {
  queryParams.pageNum = 1
  loadData()
}
function resetQuery() {
  queryParams.institutionId = undefined
  queryParams.status = 'MATERIAL_REVIEWING'
  handleQuery()
}

function handleReview(row: any) {
  Object.keys(form).forEach((k) => delete form[k])
  form.id = row.id
  form.reviewStep = row.currentStep || 1
  form.result = 'PASS'
  dialog.applyNo = row.applyNo
  dialog.visible = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate()
  dialog.submitting = true
  try {
    await reviewMaterial(form.id, {
      result: form.result,
      comment: form.comment,
      reviewStep: form.reviewStep
    })
    ElMessage.success(form.result === 'PASS' ? '审查通过' : '已退回补充材料')
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
