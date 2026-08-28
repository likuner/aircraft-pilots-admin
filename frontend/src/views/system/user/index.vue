<template>
  <div class="page-card">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input
        v-model="queryParams.keyword"
        placeholder="用户名 / 姓名 / 手机号"
        clearable
        class="!w-56"
        @keyup.enter="handleQuery"
      />
      <el-select v-model="queryParams.status" placeholder="状态" clearable class="!w-32">
        <el-option label="启用" :value="1" />
        <el-option label="停用" :value="0" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="handleQuery">查询</el-button>
      <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
    </div>

    <!-- 工具栏 -->
    <div class="toolbar">
      <span class="text-sm text-gray-500 dark:text-gray-400">共 {{ total }} 个用户</span>
      <el-button v-hasPermi="'system:user:add'" type="primary" :icon="Plus" @click="handleAdd">新增用户</el-button>
    </div>

    <!-- 表格 -->
    <el-table v-loading="loading" :data="userList" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="username" label="用户名" min-width="110" show-overflow-tooltip />
      <el-table-column prop="realName" label="姓名" min-width="100" show-overflow-tooltip />
      <el-table-column prop="phone" label="手机号" min-width="120" />
      <el-table-column prop="email" label="邮箱" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">
          <span>{{ row.email || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80" align="center">
        <template #default="{ row }">
          <StatusTag :status="String(row.status)" :map="statusMap" />
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="160">
        <template #default="{ row }">
          <span class="text-gray-600 dark:text-gray-300">{{ formatTime(row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="250" align="center" fixed="right">
        <template #default="{ row }">
          <el-button v-hasPermi="'system:user:edit'" link type="primary" @click="handleEdit(row)">编辑</el-button>
          <el-button v-hasPermi="'system:user:resetPwd'" link type="warning" @click="handleResetPwd(row)">重置密码</el-button>
          <el-button v-hasPermi="'system:user:assignRole'" link type="primary" @click="handleAssignRole(row)">分配角色</el-button>
          <el-button v-hasPermi="'system:user:delete'" link type="danger" @click="handleDelete(row)">删除</el-button>
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
    <el-dialog v-model="dialog.visible" :title="dialog.mode === 'add' ? '新增用户' : '编辑用户'" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" maxlength="50" />
        </el-form-item>
        <el-form-item v-if="dialog.mode === 'add'" label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="默认 123456" maxlength="20" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入姓名" maxlength="50" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" maxlength="20" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" maxlength="100" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" maxlength="255" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="dialog.submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码弹窗 -->
    <el-dialog v-model="pwdDialog.visible" title="重置密码" width="420px" destroy-on-close>
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="90px">
        <el-form-item label="用户名">
          <span class="text-gray-600 dark:text-gray-300">{{ pwdDialog.username }}</span>
        </el-form-item>
        <el-form-item label="新密码" prop="password">
          <el-input v-model="pwdForm.password" type="password" show-password placeholder="请输入新密码（默认 123456）" maxlength="20" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="pwdDialog.submitting" @click="handleResetPwdSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配角色弹窗 -->
    <el-dialog v-model="roleDialog.visible" title="分配角色" width="460px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="用户名">
          <span class="text-gray-600 dark:text-gray-300">{{ roleDialog.username }}</span>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="roleDialog.roleIds" multiple placeholder="请选择角色" class="!w-full">
            <el-option v-for="r in roleOptions" :key="r.id" :label="r.roleName" :value="r.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="roleDialog.submitting" @click="handleAssignRoleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import {
  assignRoles,
  createUser,
  deleteUser,
  getUserRoleIds,
  listAllRoles,
  listUsers,
  resetUserPassword,
  updateUser
} from '@/api/system'

// 状态映射
const statusMap = {
  '1': { label: '启用', type: 'success' },
  '0': { label: '停用', type: 'danger' }
}

// 查询参数
const queryParams = reactive({
  keyword: '',
  status: undefined as number | undefined,
  pageNum: 1,
  pageSize: 10
})

const loading = ref(false)
const userList = ref<any[]>([])
const total = ref(0)

function formatTime(value: string | null | undefined): string {
  if (!value) return '-'
  const s = value.replace('T', ' ')
  return s.length > 19 ? s.substring(0, 19) : s
}

async function loadData() {
  loading.value = true
  try {
    const res = await listUsers({ ...queryParams, keyword: queryParams.keyword || undefined })
    userList.value = res.data.rows || []
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
  queryParams.pageNum = 1
  loadData()
}

// 新增/编辑
interface UserForm {
  id?: number
  username: string
  password: string
  realName: string
  phone: string
  email: string
  status: number
  remark: string
}

const dialog = reactive({
  visible: false,
  mode: 'add' as 'add' | 'edit',
  submitting: false
})
const formRef = ref<FormInstance>()
const form = reactive<UserForm>({
  username: '',
  password: '',
  realName: '',
  phone: '',
  email: '',
  status: 1,
  remark: ''
})

const rules = computed<FormRules>(() => ({
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password:
    dialog.mode === 'add'
      ? [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, max: 20, message: '密码长度 6-20 位', trigger: 'blur' }]
      : []
}))

function handleAdd() {
  dialog.mode = 'add'
  Object.assign(form, { id: undefined, username: '', password: '', realName: '', phone: '', email: '', status: 1, remark: '' })
  dialog.visible = true
}

function handleEdit(row: any) {
  dialog.mode = 'edit'
  Object.assign(form, {
    id: row.id,
    username: row.username,
    password: '',
    realName: row.realName || '',
    phone: row.phone || '',
    email: row.email || '',
    status: row.status,
    remark: row.remark || ''
  })
  dialog.visible = true
}

function handleSubmit() {
  if (!formRef.value) return
  formRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    dialog.submitting = true
    try {
      const payload: Record<string, any> = {
        username: form.username,
        realName: form.realName,
        phone: form.phone,
        email: form.email,
        status: form.status,
        remark: form.remark
      }
      if (dialog.mode === 'add') {
        payload.password = form.password
        await createUser(payload)
        ElMessage.success('新增成功')
      } else {
        payload.id = form.id
        await updateUser(payload)
        ElMessage.success('更新成功')
      }
      dialog.visible = false
      loadData()
    } finally {
      dialog.submitting = false
    }
  })
}

// 重置密码
const pwdDialog = reactive({ visible: false, id: 0, username: '', submitting: false })
const pwdFormRef = ref<FormInstance>()
const pwdForm = reactive({ password: '' })
const pwdRules = computed<FormRules>(() => ({
  password: [{ required: true, message: '请输入新密码', trigger: 'blur' }, { min: 6, max: 20, message: '密码长度 6-20 位', trigger: 'blur' }]
}))

function handleResetPwd(row: any) {
  pwdDialog.id = row.id
  pwdDialog.username = row.username
  pwdForm.password = ''
  pwdDialog.visible = true
}

function handleResetPwdSubmit() {
  if (!pwdFormRef.value) return
  pwdFormRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    pwdDialog.submitting = true
    try {
      await resetUserPassword(pwdDialog.id, pwdForm.password)
      ElMessage.success('密码已重置')
      pwdDialog.visible = false
    } finally {
      pwdDialog.submitting = false
    }
  })
}

// 分配角色
const roleOptions = ref<any[]>([])
const roleDialog = reactive({ visible: false, id: 0, username: '', roleIds: [] as number[], submitting: false })

async function loadRoles() {
  if (roleOptions.value.length) return
  const res = await listAllRoles()
  roleOptions.value = res.data || []
}

async function handleAssignRole(row: any) {
  roleDialog.id = row.id
  roleDialog.username = row.username
  roleDialog.roleIds = []
  roleDialog.visible = true
  await loadRoles()
  const res = await getUserRoleIds(row.id)
  roleDialog.roleIds = (res.data || []) as number[]
}

async function handleAssignRoleSubmit() {
  roleDialog.submitting = true
  try {
    await assignRoles(roleDialog.id, roleDialog.roleIds)
    ElMessage.success('角色分配成功')
    roleDialog.visible = false
  } finally {
    roleDialog.submitting = false
  }
}

// 删除
function handleDelete(row: any) {
  ElMessageBox.confirm(`确定删除用户「${row.username}」吗？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '确定删除',
    cancelButtonText: '取消'
  })
    .then(async () => {
      await deleteUser(row.id)
      ElMessage.success('删除成功')
      if (userList.value.length === 1 && queryParams.pageNum > 1) {
        queryParams.pageNum -= 1
      }
      loadData()
    })
    .catch(() => {})
}

onMounted(loadData)
</script>
