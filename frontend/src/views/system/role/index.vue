<template>
  <div class="page-card">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input
        v-model="queryParams.keyword"
        placeholder="角色名称 / 角色编码"
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
      <span class="text-sm text-gray-500 dark:text-gray-400">共 {{ total }} 个角色</span>
      <el-button v-hasPermi="'system:role:add'" type="primary" :icon="Plus" @click="handleAdd">新增角色</el-button>
    </div>

    <!-- 表格 -->
    <el-table v-loading="loading" :data="roleList" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="roleName" label="角色名称" min-width="120" show-overflow-tooltip />
      <el-table-column prop="roleCode" label="角色编码" min-width="160" show-overflow-tooltip />
      <el-table-column prop="dataScope" label="数据范围" width="110" align="center">
        <template #default="{ row }">
          <span>{{ dataScopeMap[row.dataScope] || row.dataScope || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80" align="center">
        <template #default="{ row }">
          <StatusTag :status="String(row.status)" :map="statusMap" />
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">
          <span>{{ row.description || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="160">
        <template #default="{ row }">
          <span class="text-gray-600 dark:text-gray-300">{{ formatTime(row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" align="center" fixed="right">
        <template #default="{ row }">
          <el-button v-hasPermi="'system:role:edit'" link type="primary" @click="handleEdit(row)">编辑</el-button>
          <el-button v-hasPermi="'system:role:assignMenu'" link type="warning" @click="handleAssignMenu(row)">分配菜单</el-button>
          <el-button v-hasPermi="'system:role:delete'" link type="danger" @click="handleDelete(row)">删除</el-button>
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
    <el-dialog v-model="dialog.visible" :title="dialog.mode === 'add' ? '新增角色' : '编辑角色'" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="请输入角色名称" maxlength="50" />
        </el-form-item>
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="form.roleCode" placeholder="如 ADMIN / EXAMINER" maxlength="50" />
        </el-form-item>
        <el-form-item label="数据范围" prop="dataScope">
          <el-select v-model="form.dataScope" class="!w-full">
            <el-option label="全部数据" value="ALL" />
            <el-option label="本机构" value="INSTITUTION" />
            <el-option label="仅本人" value="SELF" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入描述" maxlength="255" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="dialog.submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配菜单弹窗 -->
    <el-dialog v-model="menuDialog.visible" :title="`分配菜单 - ${menuDialog.roleName}`" width="460px" destroy-on-close>
      <div v-loading="menuDialog.loading">
        <el-tree
          ref="menuTreeRef"
          :data="menuOptions"
          :props="treeProps"
          node-key="id"
          show-checkbox
          default-expand-all
          :check-strictly="false"
        />
      </div>
      <template #footer>
        <el-button @click="menuDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="menuDialog.submitting" @click="handleAssignMenuSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { assignMenus, createRole, deleteRole, getRoleMenuIds, listMenus, listRoles, updateRole } from '@/api/system'

const statusMap = {
  '1': { label: '启用', type: 'success' },
  '0': { label: '停用', type: 'danger' }
}

const dataScopeMap: Record<string, string> = {
  ALL: '全部数据',
  INSTITUTION: '本机构',
  SELF: '仅本人'
}

const queryParams = reactive({
  keyword: '',
  status: undefined as number | undefined,
  pageNum: 1,
  pageSize: 10
})

const loading = ref(false)
const roleList = ref<any[]>([])
const total = ref(0)

function formatTime(value: string | null | undefined): string {
  if (!value) return '-'
  const s = value.replace('T', ' ')
  return s.length > 19 ? s.substring(0, 19) : s
}

async function loadData() {
  loading.value = true
  try {
    const res = await listRoles({ ...queryParams, keyword: queryParams.keyword || undefined })
    roleList.value = res.data.rows || []
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
interface RoleForm {
  id?: number
  roleName: string
  roleCode: string
  dataScope: string
  status: number
  description: string
}

const dialog = reactive({ visible: false, mode: 'add' as 'add' | 'edit', submitting: false })
const formRef = ref<FormInstance>()
const form = reactive<RoleForm>({
  roleName: '',
  roleCode: '',
  dataScope: 'ALL',
  status: 1,
  description: ''
})

const rules = computed<FormRules>(() => ({
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }]
}))

function handleAdd() {
  dialog.mode = 'add'
  Object.assign(form, { id: undefined, roleName: '', roleCode: '', dataScope: 'ALL', status: 1, description: '' })
  dialog.visible = true
}

function handleEdit(row: any) {
  dialog.mode = 'edit'
  Object.assign(form, {
    id: row.id,
    roleName: row.roleName,
    roleCode: row.roleCode,
    dataScope: row.dataScope || 'ALL',
    status: row.status,
    description: row.description || ''
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
        roleName: form.roleName,
        roleCode: form.roleCode,
        dataScope: form.dataScope,
        status: form.status,
        description: form.description
      }
      if (dialog.mode === 'add') {
        await createRole(payload)
        ElMessage.success('新增成功')
      } else {
        payload.id = form.id
        await updateRole(payload)
        ElMessage.success('更新成功')
      }
      dialog.visible = false
      loadData()
    } finally {
      dialog.submitting = false
    }
  })
}

// 分配菜单
const treeProps = { label: 'menuName', children: 'children' }
const menuOptions = ref<any[]>([])
const menuTreeRef = ref<any>()
const menuDialog = reactive({ visible: false, id: 0, roleName: '', loading: false, submitting: false })

async function loadMenus() {
  if (menuOptions.value.length) return
  const res = await listMenus()
  menuOptions.value = res.data || []
}

async function handleAssignMenu(row: any) {
  menuDialog.id = row.id
  menuDialog.roleName = row.roleName
  menuDialog.visible = true
  menuDialog.loading = true
  try {
    await loadMenus()
    const res = await getRoleMenuIds(row.id)
    const ids = (res.data || []) as number[]
    await nextTick()
    menuTreeRef.value?.setCheckedKeys(ids)
  } finally {
    menuDialog.loading = false
  }
}

function handleAssignMenuSubmit() {
  const tree = menuTreeRef.value
  if (!tree) return
  const checkedIds: number[] = tree.getCheckedKeys()
  const halfCheckedIds: number[] = tree.getHalfCheckedKeys()
  const menuIds = [...halfCheckedIds, ...checkedIds]
  menuDialog.submitting = true
  assignMenus(menuDialog.id, menuIds)
    .then(() => {
      ElMessage.success('菜单分配成功')
      menuDialog.visible = false
    })
    .finally(() => {
      menuDialog.submitting = false
    })
}

// 删除
function handleDelete(row: any) {
  ElMessageBox.confirm(`确定删除角色「${row.roleName}」吗？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '确定删除',
    cancelButtonText: '取消'
  })
    .then(async () => {
      await deleteRole(row.id)
      ElMessage.success('删除成功')
      if (roleList.value.length === 1 && queryParams.pageNum > 1) {
        queryParams.pageNum -= 1
      }
      loadData()
    })
    .catch(() => {})
}

onMounted(loadData)
</script>
