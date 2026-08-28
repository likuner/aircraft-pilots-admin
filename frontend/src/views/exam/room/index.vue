<template>
  <div class="page-card">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input v-model="queryParams.keyword" placeholder="考场编号 / 名称" clearable class="!w-56" @keyup.enter="handleQuery" />
      <el-select v-model="queryParams.status" placeholder="状态" clearable class="!w-36">
        <el-option label="启用" :value="1" />
        <el-option label="停用" :value="0" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="handleQuery">查询</el-button>
      <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
    </div>

    <div class="toolbar">
      <span class="text-sm text-gray-500 dark:text-gray-400">共 {{ total }} 个考场</span>
      <div>
        <el-button v-hasPermi="'exam:room:add'" type="primary" :icon="Plus" @click="handleAdd">新增考场</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="roomCode" label="考场编号" min-width="110" show-overflow-tooltip />
      <el-table-column prop="roomName" label="考场名称" min-width="150" show-overflow-tooltip />
      <el-table-column prop="location" label="地点" min-width="160" show-overflow-tooltip />
      <el-table-column prop="capacity" label="容量" width="80" align="center" />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <StatusTag :status="row.status === 1 ? 'ACTIVE' : 'INACTIVE'" />
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="170">
        <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="130" align="center" fixed="right">
        <template #default="{ row }">
          <el-button v-hasPermi="'exam:room:edit'" link type="primary" @click="handleEdit(row)">编辑</el-button>
          <el-button v-hasPermi="'exam:room:delete'" link type="danger" @click="handleDelete(row)">删除</el-button>
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

    <!-- 新增/编辑考场弹窗 -->
    <el-dialog v-model="dialog.visible" :title="dialog.mode === 'add' ? '新增考场' : '编辑考场'" width="480px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="考场编号" prop="roomCode">
          <el-input v-model="form.roomCode" placeholder="如 T-L01" maxlength="30" />
        </el-form-item>
        <el-form-item label="考场名称" prop="roomName">
          <el-input v-model="form.roomName" placeholder="如 理论考场A" maxlength="50" />
        </el-form-item>
        <el-form-item label="地点" prop="location">
          <el-input v-model="form.location" placeholder="如 北京考试中心 3F" maxlength="100" />
        </el-form-item>
        <el-form-item label="容量" prop="capacity">
          <el-input-number v-model="form.capacity" :min="1" :precision="0" class="!w-full" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
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
import { listRooms, createRoom, updateRoom, deleteRoom } from '@/api/exam'

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({ pageNum: 1, pageSize: 10, keyword: '', status: undefined as number | undefined })

const dialog = reactive({ visible: false, mode: 'add' as 'add' | 'edit', submitting: false })
const formRef = ref<FormInstance>()
const form = reactive<any>({})
const rules: FormRules = {
  roomCode: [{ required: true, message: '请输入考场编号', trigger: 'blur' }],
  roomName: [{ required: true, message: '请输入考场名称', trigger: 'blur' }],
  location: [{ required: true, message: '请输入地点', trigger: 'blur' }],
  capacity: [{ required: true, message: '请输入容量', trigger: 'blur' }]
}

function formatTime(v?: string): string {
  if (!v) return '-'
  return String(v).replace('T', ' ').slice(0, 19)
}

async function loadData() {
  loading.value = true
  try {
    const res = await listRooms({
      pageNum: queryParams.pageNum,
      pageSize: queryParams.pageSize,
      keyword: queryParams.keyword || undefined,
      status: queryParams.status
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
  queryParams.status = undefined
  handleQuery()
}

function handleAdd() {
  Object.keys(form).forEach((k) => delete form[k])
  form.capacity = 50
  form.status = 1
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
      await createRoom(form)
      ElMessage.success('新增成功')
    } else {
      await updateRoom({ ...form, id: form.id })
      ElMessage.success('保存成功')
    }
    dialog.visible = false
    loadData()
  } finally {
    dialog.submitting = false
  }
}

async function handleDelete(row: any) {
  await ElMessageBox.confirm(`确定删除考场「${row.roomName}」吗？删除后不可恢复。`, '提示', { type: 'warning' })
  await deleteRoom(row.id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(() => {
  loadData()
})
</script>
