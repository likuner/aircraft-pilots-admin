<template>
  <div class="page-card">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input v-model="queryParams.keyword" placeholder="场次名称" clearable class="!w-48" @keyup.enter="handleQuery" />
      <el-select v-model="queryParams.planId" placeholder="所属计划" clearable filterable class="!w-52">
        <el-option v-for="p in planOptions" :key="p.id" :label="p.planName" :value="p.id" />
      </el-select>
      <el-select v-model="queryParams.status" placeholder="状态" clearable class="!w-36">
        <el-option label="草稿" value="DRAFT" />
        <el-option label="已发布" value="PUBLISHED" />
        <el-option label="报名截止" value="ENROLLMENT_CLOSED" />
        <el-option label="进行中" value="IN_PROGRESS" />
        <el-option label="已完成" value="COMPLETED" />
        <el-option label="已取消" value="CANCELED" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="handleQuery">查询</el-button>
      <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
    </div>

    <div class="toolbar">
      <span class="text-sm text-gray-500 dark:text-gray-400">共 {{ total }} 个场次</span>
      <div>
        <el-button :icon="OfficeBuilding" @click="roomDialog.visible = true">考场管理</el-button>
        <el-button v-hasPermi="'exam:session:add'" type="primary" :icon="Plus" @click="handleAdd">新增场次</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="sessionCode" label="场次编号" min-width="120" show-overflow-tooltip />
      <el-table-column prop="sessionName" label="场次名称" min-width="160" show-overflow-tooltip />
      <el-table-column label="考试类型" width="100" align="center">
        <template #default="{ row }">{{ examTypeText(row.examType) }}</template>
      </el-table-column>
      <el-table-column label="考试时间" width="170">
        <template #default="{ row }">
          <span class="text-gray-600 dark:text-gray-300">{{ row.examDate }} {{ row.startTime }}-{{ row.endTime }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="location" label="地点" min-width="110" show-overflow-tooltip />
      <el-table-column label="及格线" width="80" align="center">
        <template #default="{ row }">
          <span>{{ row.passScore }} / {{ row.fullScore }}</span>
        </template>
      </el-table-column>
      <el-table-column label="名额" width="90" align="center">
        <template #default="{ row }">
          <span>{{ row.enrolledCount ?? 0 }} / {{ row.capacity }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="95" align="center">
        <template #default="{ row }">
          <StatusTag :status="row.status" :map="statusMap" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="250" align="center" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 'DRAFT'" link type="success" @click="handlePublish(row)">发布</el-button>
          <el-button v-if="row.status === 'PUBLISHED'" link type="warning" @click="handleCloseEnroll(row)">关闭报名</el-button>
          <el-button v-hasPermi="'exam:session:edit'" link type="primary" @click="handleEdit(row)">编辑</el-button>
          <el-button v-hasPermi="'exam:session:delete'" link type="danger" @click="handleDelete(row)">删除</el-button>
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

    <!-- 新增/编辑场次弹窗 -->
    <el-dialog v-model="dialog.visible" :title="dialog.mode === 'add' ? '新增场次' : '编辑场次'" width="620px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="所属计划" prop="planId">
              <el-select v-model="form.planId" placeholder="请选择" filterable class="!w-full">
                <el-option v-for="p in planOptions" :key="p.id" :label="p.planName" :value="p.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="场次名称" prop="sessionName">
              <el-input v-model="form.sessionName" placeholder="请输入" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="考试类型" prop="examType">
              <el-select v-model="form.examType" placeholder="请选择" class="!w-full">
                <el-option label="理论" value="THEORY" />
                <el-option label="实操" value="PRACTICAL" />
                <el-option label="理论与实操" value="BOTH" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="考试日期" prop="examDate">
              <el-date-picker v-model="form.examDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" class="!w-full" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="开始时间" prop="startTime">
              <el-time-picker v-model="form.startTime" value-format="HH:mm" placeholder="开始" class="!w-full" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束时间" prop="endTime">
              <el-time-picker v-model="form.endTime" value-format="HH:mm" placeholder="结束" class="!w-full" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="考场" prop="roomId">
              <el-select v-model="form.roomId" placeholder="请选择" filterable class="!w-full">
                <el-option v-for="r in roomOptions" :key="r.id" :label="`${r.roomName}（${r.roomCode}）`" :value="r.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="地点" prop="location">
              <el-input v-model="form.location" placeholder="请输入" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="满分" prop="fullScore">
              <el-input-number v-model="form.fullScore" :min="0" :precision="0" class="!w-full" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="及格分" prop="passScore">
              <el-input-number v-model="form.passScore" :min="0" :precision="0" class="!w-full" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="名额" prop="capacity">
              <el-input-number v-model="form.capacity" :min="1" :precision="0" class="!w-full" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="dialog.submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 考场管理弹窗 -->
    <el-dialog v-model="roomDialog.visible" title="考场管理" width="720px" destroy-on-close>
      <div class="flex justify-end mb-3">
        <el-button type="primary" size="small" :icon="Plus" @click="handleRoomAdd">新增考场</el-button>
      </div>
      <el-table v-loading="roomDialog.loading" :data="roomOptions" border size="small">
        <el-table-column prop="roomCode" label="考场编号" min-width="100" />
        <el-table-column prop="roomName" label="考场名称" min-width="120" />
        <el-table-column prop="location" label="地点" min-width="140" show-overflow-tooltip />
        <el-table-column prop="capacity" label="容量" width="70" align="center" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <StatusTag :status="row.status === 1 ? 'ACTIVE' : 'INACTIVE'" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleRoomEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleRoomDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="roomDialog.visible = false">关闭</el-button>
      </template>

      <!-- 考场新增/编辑子弹窗 -->
      <el-dialog v-model="roomFormDialog.visible" :title="roomFormDialog.mode === 'add' ? '新增考场' : '编辑考场'" width="460px" append-to-body destroy-on-close>
        <el-form ref="roomFormRef" :model="roomForm" :rules="roomRules" label-width="80px">
          <el-form-item label="编号" prop="roomCode">
            <el-input v-model="roomForm.roomCode" placeholder="如 T-L01" maxlength="30" />
          </el-form-item>
          <el-form-item label="名称" prop="roomName">
            <el-input v-model="roomForm.roomName" placeholder="如 理论考场A" maxlength="50" />
          </el-form-item>
          <el-form-item label="地点" prop="location">
            <el-input v-model="roomForm.location" placeholder="如 北京考试中心 3F" maxlength="100" />
          </el-form-item>
          <el-form-item label="容量" prop="capacity">
            <el-input-number v-model="roomForm.capacity" :min="1" :precision="0" class="!w-full" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="roomFormDialog.visible = false">取消</el-button>
          <el-button type="primary" :loading="roomFormDialog.submitting" @click="handleRoomSubmit">确定</el-button>
        </template>
      </el-dialog>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh, Plus, OfficeBuilding } from '@element-plus/icons-vue'
import StatusTag from '@/components/StatusTag.vue'
import {
  listSessions, createSession, updateSession, deleteSession, publishSession, closeEnrollment,
  listPlans, listRooms, createRoom, updateRoom, deleteRoom
} from '@/api/exam'

const statusMap: Record<string, { label: string; type: string }> = {
  DRAFT: { label: '草稿', type: 'info' },
  PUBLISHED: { label: '已发布', type: 'success' },
  ENROLLMENT_CLOSED: { label: '报名截止', type: 'warning' },
  IN_PROGRESS: { label: '进行中', type: 'primary' },
  COMPLETED: { label: '已完成', type: 'success' },
  CANCELED: { label: '已取消', type: 'danger' }
}

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({ pageNum: 1, pageSize: 10, keyword: '', planId: undefined as number | undefined, status: '' })
const planOptions = ref<any[]>([])
const roomOptions = ref<any[]>([])

const dialog = reactive({ visible: false, mode: 'add' as 'add' | 'edit', submitting: false })
const formRef = ref<FormInstance>()
const form = reactive<any>({})
const rules: FormRules = {
  planId: [{ required: true, message: '请选择所属计划', trigger: 'change' }],
  sessionName: [{ required: true, message: '请输入场次名称', trigger: 'blur' }],
  examDate: [{ required: true, message: '请选择考试日期', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  roomId: [{ required: true, message: '请选择考场', trigger: 'change' }],
  capacity: [{ required: true, message: '请输入名额', trigger: 'blur' }]
}

// 考场管理
const roomDialog = reactive({ visible: false, loading: false })
const roomFormDialog = reactive({ visible: false, mode: 'add' as 'add' | 'edit', submitting: false })
const roomFormRef = ref<FormInstance>()
const roomForm = reactive<any>({})
const roomRules: FormRules = {
  roomCode: [{ required: true, message: '请输入考场编号', trigger: 'blur' }],
  roomName: [{ required: true, message: '请输入考场名称', trigger: 'blur' }]
}

function formatTime(v?: string): string {
  if (!v) return '-'
  return String(v).replace('T', ' ').slice(0, 19)
}

function examTypeText(t?: string): string {
  if (t === 'THEORY') return '理论'
  if (t === 'PRACTICAL') return '实操'
  if (t === 'BOTH') return '理论与实操'
  return t || '-'
}

async function loadRoomOptions() {
  const res = await listRooms({ pageNum: 1, pageSize: 200 })
  roomOptions.value = res.data?.rows || []
}

async function loadPlanOptions() {
  const res = await listPlans({ pageNum: 1, pageSize: 200 })
  planOptions.value = res.data?.rows || []
}

async function loadData() {
  loading.value = true
  try {
    const res = await listSessions({
      pageNum: queryParams.pageNum,
      pageSize: queryParams.pageSize,
      keyword: queryParams.keyword || undefined,
      planId: queryParams.planId,
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
  queryParams.planId = undefined
  queryParams.status = ''
  handleQuery()
}

function handleAdd() {
  Object.keys(form).forEach((k) => delete form[k])
  form.examType = 'THEORY'
  form.fullScore = 100
  form.passScore = 80
  form.capacity = 50
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
      await createSession(form)
      ElMessage.success('新增成功')
    } else {
      await updateSession({ ...form, id: form.id })
      ElMessage.success('保存成功')
    }
    dialog.visible = false
    loadData()
  } finally {
    dialog.submitting = false
  }
}

async function handlePublish(row: any) {
  await ElMessageBox.confirm(`确定发布场次「${row.sessionName}」吗？`, '提示', { type: 'warning' })
  await publishSession(row.id)
  ElMessage.success('发布成功')
  loadData()
}

async function handleCloseEnroll(row: any) {
  await ElMessageBox.confirm(`确定关闭场次「${row.sessionName}」的报名吗？`, '提示', { type: 'warning' })
  await closeEnrollment(row.id)
  ElMessage.success('已关闭报名')
  loadData()
}

async function handleDelete(row: any) {
  await ElMessageBox.confirm(`确定删除场次「${row.sessionName}」吗？`, '提示', { type: 'warning' })
  await deleteSession(row.id)
  ElMessage.success('删除成功')
  loadData()
}

// ---- 考场管理 ----
function handleRoomAdd() {
  Object.keys(roomForm).forEach((k) => delete roomForm[k])
  roomForm.capacity = 50
  roomForm.status = 1
  roomFormDialog.mode = 'add'
  roomFormDialog.visible = true
}
function handleRoomEdit(row: any) {
  Object.keys(roomForm).forEach((k) => delete roomForm[k])
  Object.assign(roomForm, { ...row })
  roomFormDialog.mode = 'edit'
  roomFormDialog.visible = true
}
async function handleRoomSubmit() {
  if (!roomFormRef.value) return
  await roomFormRef.value.validate()
  roomFormDialog.submitting = true
  try {
    if (roomFormDialog.mode === 'add') {
      await createRoom(roomForm)
      ElMessage.success('新增成功')
    } else {
      await updateRoom({ ...roomForm, id: roomForm.id })
      ElMessage.success('保存成功')
    }
    roomFormDialog.visible = false
    await loadRoomOptions()
  } finally {
    roomFormDialog.submitting = false
  }
}
async function handleRoomDelete(row: any) {
  await ElMessageBox.confirm(`确定删除考场「${row.roomName}」吗？`, '提示', { type: 'warning' })
  await deleteRoom(row.id)
  ElMessage.success('删除成功')
  await loadRoomOptions()
}

onMounted(async () => {
  await Promise.all([loadData(), loadPlanOptions(), loadRoomOptions()])
})
</script>
