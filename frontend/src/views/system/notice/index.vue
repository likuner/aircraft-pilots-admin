<template>
  <div class="page-card">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input
        v-model="queryParams.keyword"
        placeholder="公告标题"
        clearable
        class="!w-56"
        @keyup.enter="handleQuery"
      />
      <el-select v-model="queryParams.status" placeholder="状态" clearable class="!w-32">
        <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
      </el-select>
      <el-select v-model="queryParams.noticeType" placeholder="类型" clearable class="!w-32">
        <el-option v-for="t in typeOptions" :key="t.value" :label="t.label" :value="t.value" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="handleQuery">查询</el-button>
      <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
    </div>

    <!-- 工具栏 -->
    <div class="toolbar">
      <span class="text-sm text-gray-500 dark:text-gray-400">共 {{ total }} 条公告</span>
      <el-button v-hasPermi="'system:notice:add'" type="primary" :icon="Plus" @click="handleAdd">新增公告</el-button>
    </div>

    <!-- 表格 -->
    <el-table v-loading="loading" :data="noticeList" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
      <el-table-column label="类型" width="100" align="center">
        <template #default="{ row }">
          <StatusTag :status="row.noticeType" :map="typeMap" />
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <StatusTag :status="row.status" :map="statusMap" />
        </template>
      </el-table-column>
      <el-table-column prop="targetRole" label="可见角色" width="130" align="center">
        <template #default="{ row }">
          <span>{{ row.targetRole || '全部' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="publishTime" label="发布时间" width="160">
        <template #default="{ row }">
          <span class="text-gray-600 dark:text-gray-300">{{ formatTime(row.publishTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="160">
        <template #default="{ row }">
          <span class="text-gray-600 dark:text-gray-300">{{ formatTime(row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="230" align="center" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status !== 'PUBLISHED'" v-hasPermi="'system:notice:edit'" link type="primary" @click="handleEdit(row)">编辑</el-button>
          <el-button v-if="row.status !== 'PUBLISHED'" v-hasPermi="'system:notice:publish'" link type="success" @click="handlePublish(row)">发布</el-button>
          <el-button v-else v-hasPermi="'system:notice:publish'" link type="warning" @click="handleUnpublish(row)">撤下</el-button>
          <el-button v-hasPermi="'system:notice:delete'" link type="danger" @click="handleDelete(row)">删除</el-button>
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
    <el-dialog v-model="dialog.visible" :title="dialog.mode === 'add' ? '新增公告' : '编辑公告'" width="600px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入公告标题" maxlength="100" />
        </el-form-item>
        <el-form-item label="类型" prop="noticeType">
          <el-radio-group v-model="form.noticeType">
            <el-radio v-for="t in typeOptions" :key="t.value" :value="t.value">{{ t.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="可见角色" prop="targetRole">
          <el-select v-model="form.targetRole" clearable placeholder="不选则全部可见" class="!w-full">
            <el-option label="全部角色" value="" />
            <el-option v-for="r in roleOptions" :key="r.roleCode" :label="r.roleName" :value="r.roleCode" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="6" placeholder="请输入公告内容" />
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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { createNotice, deleteNotice, listAllRoles, listNotices, publishNotice, unpublishNotice, updateNotice } from '@/api/system'

const statusOptions = [
  { label: '草稿', value: 'DRAFT' },
  { label: '已发布', value: 'PUBLISHED' },
  { label: '已关闭', value: 'CLOSED' }
]
const statusMap = {
  DRAFT: { label: '草稿', type: 'info' },
  PUBLISHED: { label: '已发布', type: 'success' },
  CLOSED: { label: '已关闭', type: 'info' }
}

const typeOptions = [
  { label: '公告', value: 'ANNOUNCEMENT' },
  { label: '通知', value: 'NOTICE' },
  { label: '预警', value: 'WARNING' }
]
const typeMap = {
  ANNOUNCEMENT: { label: '公告', type: 'primary' },
  NOTICE: { label: '通知', type: 'info' },
  WARNING: { label: '预警', type: 'danger' }
}

const queryParams = reactive({
  keyword: '',
  status: undefined as string | undefined,
  noticeType: undefined as string | undefined,
  pageNum: 1,
  pageSize: 10
})

const loading = ref(false)
const noticeList = ref<any[]>([])
const total = ref(0)
const roleOptions = ref<any[]>([])

function formatTime(value: string | null | undefined): string {
  if (!value) return '-'
  const s = value.replace('T', ' ')
  return s.length > 19 ? s.substring(0, 19) : s
}

async function loadData() {
  loading.value = true
  try {
    const res = await listNotices({
      ...queryParams,
      keyword: queryParams.keyword || undefined
    })
    noticeList.value = res.data.rows || []
    total.value = res.data.total || 0
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
  queryParams.noticeType = undefined
  queryParams.pageNum = 1
  loadData()
}

// 新增/编辑
interface NoticeForm {
  id?: number
  title: string
  noticeType: string
  targetRole: string
  content: string
}

const dialog = reactive({ visible: false, mode: 'add' as 'add' | 'edit', submitting: false })
const formRef = ref<FormInstance>()
const form = reactive<NoticeForm>({
  title: '',
  noticeType: 'ANNOUNCEMENT',
  targetRole: '',
  content: ''
})

const rules = computed<FormRules>(() => ({
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }]
}))

async function loadRoles() {
  if (roleOptions.value.length) return
  const res = await listAllRoles()
  roleOptions.value = res.data || []
}

function handleAdd() {
  dialog.mode = 'add'
  Object.assign(form, { id: undefined, title: '', noticeType: 'ANNOUNCEMENT', targetRole: '', content: '' })
  dialog.visible = true
  loadRoles()
}

function handleEdit(row: any) {
  dialog.mode = 'edit'
  Object.assign(form, {
    id: row.id,
    title: row.title,
    noticeType: row.noticeType || 'ANNOUNCEMENT',
    targetRole: row.targetRole || '',
    content: row.content || ''
  })
  dialog.visible = true
  loadRoles()
}

function handleSubmit() {
  if (!formRef.value) return
  formRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    dialog.submitting = true
    try {
      const payload: Record<string, any> = {
        title: form.title,
        noticeType: form.noticeType,
        targetRole: form.targetRole || null,
        content: form.content
      }
      if (dialog.mode === 'add') {
        await createNotice(payload)
        ElMessage.success('新增成功')
      } else {
        payload.id = form.id
        await updateNotice(payload)
        ElMessage.success('更新成功')
      }
      dialog.visible = false
      loadData()
    } finally {
      dialog.submitting = false
    }
  })
}

// 发布/撤下/删除
function handlePublish(row: any) {
  ElMessageBox.confirm(`确定发布公告「${row.title}」吗？`, '发布确认', {
    type: 'warning',
    confirmButtonText: '确定发布',
    cancelButtonText: '取消'
  })
    .then(async () => {
      await publishNotice(row.id)
      ElMessage.success('发布成功')
      loadData()
    })
    .catch(() => {})
}

function handleUnpublish(row: any) {
  ElMessageBox.confirm(`确定撤下公告「${row.title}」吗？`, '撤下确认', {
    type: 'warning',
    confirmButtonText: '确定撤下',
    cancelButtonText: '取消'
  })
    .then(async () => {
      await unpublishNotice(row.id)
      ElMessage.success('已撤下')
      loadData()
    })
    .catch(() => {})
}

function handleDelete(row: any) {
  ElMessageBox.confirm(`确定删除公告「${row.title}」吗？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '确定删除',
    cancelButtonText: '取消'
  })
    .then(async () => {
      await deleteNotice(row.id)
      ElMessage.success('删除成功')
      if (noticeList.value.length === 1 && queryParams.pageNum > 1) {
        queryParams.pageNum -= 1
      }
      loadData()
    })
    .catch(() => {})
}

onMounted(loadData)
</script>
