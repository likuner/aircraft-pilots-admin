<template>
  <div class="page-card">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input
        v-model="queryParams.keyword"
        placeholder="姓名 / 身份证 / 手机号"
        clearable
        class="!w-52"
        @keyup.enter="handleQuery"
      />
      <el-select v-model="queryParams.pilotType" placeholder="飞行器类别" clearable class="!w-40">
        <el-option v-for="t in pilotTypes" :key="t" :label="t" :value="t" />
      </el-select>
      <el-select v-model="queryParams.status" placeholder="状态" clearable class="!w-32">
        <el-option label="正常" value="ACTIVE" />
        <el-option label="停用" value="INACTIVE" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="handleQuery">查询</el-button>
      <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
    </div>

    <!-- 工具栏 -->
    <div class="toolbar">
      <span class="text-sm text-gray-500 dark:text-gray-400">共 {{ total }} 名考生</span>
      <el-button v-hasPermi="'student:profile:add'" type="primary" :icon="Plus" @click="handleAdd">新增档案</el-button>
    </div>

    <!-- 表格 -->
    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="姓名" min-width="90" show-overflow-tooltip />
      <el-table-column label="性别" width="60" align="center">
        <template #default="{ row }">
          <span>{{ row.gender === 1 ? '男' : row.gender === 2 ? '女' : '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="idCard" label="身份证号" min-width="170" show-overflow-tooltip />
      <el-table-column prop="phone" label="联系电话" min-width="120" />
      <el-table-column prop="pilotType" label="飞行器类别" min-width="100" show-overflow-tooltip />
      <el-table-column prop="aircraftModel" label="准驾机型" min-width="110" show-overflow-tooltip />
      <el-table-column prop="flyingHours" label="飞行时长(h)" width="105" align="right" />
      <el-table-column prop="examCategory" label="报考类别" min-width="90" show-overflow-tooltip />
      <el-table-column label="状态" width="80" align="center">
        <template #default="{ row }">
          <StatusTag :status="row.status" />
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="160">
        <template #default="{ row }">
          <span class="text-gray-600 dark:text-gray-300">{{ formatTime(row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="210" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleDetail(row)">详情</el-button>
          <el-button v-hasPermi="'student:profile:edit'" link type="primary" @click="handleEdit(row)">编辑</el-button>
          <el-button v-hasPermi="'student:profile:delete'" link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
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
    <el-dialog v-model="dialog.visible" :title="dialog.mode === 'add' ? '新增档案' : '编辑档案'" width="640px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="form.name" placeholder="请输入姓名" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别" prop="gender">
              <el-radio-group v-model="form.gender">
                <el-radio :value="1">男</el-radio>
                <el-radio :value="2">女</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="身份证号" prop="idCard">
              <el-input v-model="form.idCard" placeholder="请输入身份证号" maxlength="18" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="出生日期" prop="birthDate">
              <el-date-picker v-model="form.birthDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" class="!w-full" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入联系电话" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="飞行器类别" prop="pilotType">
              <el-select v-model="form.pilotType" placeholder="请选择" class="!w-full">
                <el-option v-for="t in pilotTypes" :key="t" :label="t" :value="t" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="准驾机型" prop="aircraftModel">
              <el-input v-model="form.aircraftModel" placeholder="如 DJI M350" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="飞行时长(h)" prop="flyingHours">
              <el-input-number v-model="form.flyingHours" :min="0" :precision="1" :step="1" class="!w-full" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="报考类别" prop="examCategory">
              <el-select v-model="form.examCategory" placeholder="请选择" class="!w-full">
                <el-option label="视距内" value="视距内" />
                <el-option label="超视距" value="超视距" />
                <el-option label="驾驶员" value="驾驶员" />
                <el-option label="教员" value="教员" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="学历" prop="education">
              <el-select v-model="form.education" placeholder="请选择" clearable class="!w-full">
                <el-option v-for="e in ['高中','大专','本科','硕士','博士']" :key="e" :label="e" :value="e" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="紧急联系人" prop="emergencyContact">
              <el-input v-model="form.emergencyContact" placeholder="请输入紧急联系人" maxlength="50" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="请输入备注" maxlength="255" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="dialog.submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗（基本信息 + 时间线） -->
    <el-dialog v-model="detailDialog.visible" title="档案详情" width="680px" destroy-on-close>
      <el-descriptions :column="2" border size="small" class="mb-4">
        <el-descriptions-item label="姓名">{{ detail.name }}</el-descriptions-item>
        <el-descriptions-item label="性别">{{ detail.gender === 1 ? '男' : detail.gender === 2 ? '女' : '-' }}</el-descriptions-item>
        <el-descriptions-item label="身份证号" :span="2">{{ detail.idCard || '-' }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ detail.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="飞行器类别">{{ detail.pilotType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="准驾机型">{{ detail.aircraftModel || '-' }}</el-descriptions-item>
        <el-descriptions-item label="飞行时长(h)">{{ detail.flyingHours ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="报考类别">{{ detail.examCategory || '-' }}</el-descriptions-item>
        <el-descriptions-item label="学历">{{ detail.education || '-' }}</el-descriptions-item>
      </el-descriptions>
      <div class="font-medium mb-2 text-sm">考试/证书记录</div>
      <el-timeline v-if="records.length">
        <el-timeline-item v-for="(r, i) in records" :key="i" :timestamp="formatTime(r.time)" placement="top">
          <div class="flex items-center gap-2">
            <span class="text-sm">{{ r.title }}</span>
            <StatusTag :status="r.status" />
          </div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无记录" :image-size="60" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import StatusTag from '@/components/StatusTag.vue'
import { listProfiles, getProfile, getProfileRecords, createProfile, updateProfile, deleteProfile } from '@/api/student'

const pilotTypes = ['多旋翼', '固定翼', '直升机', '垂直起降']

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({ pageNum: 1, pageSize: 10, keyword: '', pilotType: '', status: '' })

const dialog = reactive({ visible: false, mode: 'add' as 'add' | 'edit', submitting: false })
const detailDialog = reactive({ visible: false })
const detail = ref<any>({})
const records = ref<any[]>([])

const formRef = ref<FormInstance>()
const form = reactive<any>({})
const rules: FormRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }]
}

function formatTime(v?: string): string {
  if (!v) return '-'
  return String(v).replace('T', ' ').slice(0, 19)
}

async function loadData() {
  loading.value = true
  try {
    const res = await listProfiles({
      pageNum: queryParams.pageNum,
      pageSize: queryParams.pageSize,
      keyword: queryParams.keyword || undefined,
      pilotType: queryParams.pilotType || undefined,
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
  queryParams.pilotType = ''
  queryParams.status = ''
  handleQuery()
}

function handleAdd() {
  Object.keys(form).forEach((k) => delete form[k])
  form.gender = 1
  form.flyingHours = 0
  dialog.mode = 'add'
  dialog.visible = true
}

async function handleEdit(row: any) {
  const res = await getProfile(row.id)
  const d = res.data || {}
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, d)
  dialog.mode = 'edit'
  dialog.visible = true
}

async function handleDetail(row: any) {
  const res = await getProfile(row.id)
  detail.value = res.data || row
  const rec = await getProfileRecords(row.id)
  records.value = rec.data || []
  detailDialog.visible = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate()
  dialog.submitting = true
  try {
    if (dialog.mode === 'add') {
      await createProfile(form)
      ElMessage.success('新增成功')
    } else {
      await updateProfile({ ...form, id: form.id })
      ElMessage.success('保存成功')
    }
    dialog.visible = false
    loadData()
  } finally {
    dialog.submitting = false
  }
}

async function handleDelete(row: any) {
  await ElMessageBox.confirm(`确定删除考生「${row.name}」的档案吗？`, '提示', { type: 'warning' })
  await deleteProfile(row.id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(loadData)
</script>
