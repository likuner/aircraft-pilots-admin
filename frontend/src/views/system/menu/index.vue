<template>
  <div class="page-card">
    <!-- 工具栏 -->
    <div class="toolbar">
      <span class="text-sm text-gray-500 dark:text-gray-400">菜单/权限树（目录 / 菜单 / 按钮）</span>
      <el-button v-hasPermi="'system:menu:add'" type="primary" :icon="Plus" @click="handleAdd()">新增菜单</el-button>
    </div>

    <!-- 表格（树形） -->
    <el-table
      v-loading="loading"
      :data="menuList"
      border
      row-key="id"
      :tree-props="{ children: 'children' }"
      default-expand-all
    >
      <el-table-column label="菜单名称" min-width="220">
        <template #default="{ row }">
          <span class="inline-flex items-center gap-1.5">
            <el-icon v-if="row.icon"><component :is="row.icon" /></el-icon>
            <span>{{ row.menuName }}</span>
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="icon" label="图标" width="120" show-overflow-tooltip>
        <template #default="{ row }">
          <span>{{ row.icon || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="类型" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="menuTypeTag(row.menuType)" size="small">{{ menuTypeText(row.menuType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="orderNum" label="排序" width="70" align="center" />
      <el-table-column prop="perms" label="权限标识" min-width="150" show-overflow-tooltip>
        <template #default="{ row }">
          <span>{{ row.perms || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="path" label="路由路径" min-width="130" show-overflow-tooltip>
        <template #default="{ row }">
          <span>{{ row.path || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="component" label="组件路径" min-width="170" show-overflow-tooltip>
        <template #default="{ row }">
          <span>{{ row.component || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="可见" width="80" align="center">
        <template #default="{ row }">
          <StatusTag :status="String(row.visible)" :map="visibleMap" />
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80" align="center">
        <template #default="{ row }">
          <StatusTag :status="String(row.status)" :map="statusMap" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" align="center" fixed="right">
        <template #default="{ row }">
          <el-button v-hasPermi="'system:menu:add'" link type="primary" @click="handleAdd(row)">新增</el-button>
          <el-button v-hasPermi="'system:menu:edit'" link type="primary" @click="handleEdit(row)">编辑</el-button>
          <el-button v-hasPermi="'system:menu:delete'" link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialog.visible" :title="dialog.mode === 'add' ? '新增菜单' : '编辑菜单'" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级菜单" prop="parentId">
          <el-tree-select
            v-model="form.parentId"
            :data="parentOptions"
            :props="treeProps"
            node-key="id"
            check-strictly
            default-expand-all
            :render-after-expand="false"
            placeholder="选择上级菜单（0 为根目录）"
            class="!w-full"
          />
        </el-form-item>
        <el-form-item label="菜单类型" prop="menuType">
          <el-radio-group v-model="form.menuType">
            <el-radio :value="1">目录</el-radio>
            <el-radio :value="2">菜单</el-radio>
            <el-radio :value="3">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="菜单名称" prop="menuName">
          <el-input v-model="form.menuName" placeholder="请输入菜单名称" maxlength="50" />
        </el-form-item>
        <el-form-item label="路由路径" prop="path">
          <el-input v-model="form.path" placeholder="如 user / /system" maxlength="200" />
        </el-form-item>
        <el-form-item label="组件路径" prop="component">
          <el-input v-model="form.component" placeholder="如 system/user/index" maxlength="200" />
        </el-form-item>
        <el-form-item label="权限标识" prop="perms">
          <el-input v-model="form.perms" placeholder="如 system:user:add" maxlength="100" />
        </el-form-item>
        <el-form-item label="图标" prop="icon">
          <el-input v-model="form.icon" placeholder="如 Setting / User" maxlength="100" />
        </el-form-item>
        <el-form-item label="排序" prop="orderNum">
          <el-input-number v-model="form.orderNum" :min="0" :max="999" controls-position="right" />
        </el-form-item>
        <el-form-item label="显示" prop="visible">
          <el-radio-group v-model="form.visible">
            <el-radio :value="1">显示</el-radio>
            <el-radio :value="0">隐藏</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
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
import { Plus } from '@element-plus/icons-vue'
import { createMenu, deleteMenu, listMenus, updateMenu } from '@/api/system'

const statusMap = {
  '1': { label: '启用', type: 'success' },
  '0': { label: '停用', type: 'danger' }
}

const visibleMap = {
  '1': { label: '显示', type: 'success' },
  '0': { label: '隐藏', type: 'info' }
}

const loading = ref(false)
const menuList = ref<any[]>([])

function menuTypeText(type: number | string | null | undefined): string {
  const map: Record<string, string> = { '1': '目录', '2': '菜单', '3': '按钮' }
  return map[String(type)] || '-'
}

function menuTypeTag(type: number | string | null | undefined): string {
  const map: Record<string, string> = { '1': 'warning', '2': 'primary', '3': 'info' }
  return map[String(type)] || 'info'
}

async function loadData() {
  loading.value = true
  try {
    const res = await listMenus()
    menuList.value = res.data || []
  } finally {
    loading.value = false
  }
}

// 新增/编辑
interface MenuForm {
  id?: number
  parentId: number
  menuName: string
  menuType: number
  path: string
  component: string
  perms: string
  icon: string
  orderNum: number
  visible: number
  status: number
}

const dialog = reactive({ visible: false, mode: 'add' as 'add' | 'edit', submitting: false })
const formRef = ref<FormInstance>()
const form = reactive<MenuForm>({
  parentId: 0,
  menuName: '',
  menuType: 2,
  path: '',
  component: '',
  perms: '',
  icon: '',
  orderNum: 0,
  visible: 1,
  status: 1
})

const rules = computed<FormRules>(() => ({
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }]
}))

const treeProps = { label: 'menuName', children: 'children' }
// 上级菜单选项：根(0) + 菜单树
const parentOptions = computed<any[]>(() => [
  { id: 0, menuName: '根目录', children: menuList.value }
])

function handleAdd(parent?: any) {
  dialog.mode = 'add'
  Object.assign(form, {
    id: undefined,
    parentId: parent && parent.id != null ? parent.id : 0,
    menuName: '',
    menuType: parent ? 2 : 1,
    path: '',
    component: '',
    perms: '',
    icon: '',
    orderNum: 0,
    visible: 1,
    status: 1
  })
  dialog.visible = true
}

function handleEdit(row: any) {
  dialog.mode = 'edit'
  Object.assign(form, {
    id: row.id,
    parentId: row.parentId,
    menuName: row.menuName,
    menuType: row.menuType,
    path: row.path || '',
    component: row.component || '',
    perms: row.perms || '',
    icon: row.icon || '',
    orderNum: row.orderNum ?? 0,
    visible: row.visible,
    status: row.status
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
        parentId: form.parentId,
        menuName: form.menuName,
        menuType: form.menuType,
        path: form.path || null,
        component: form.component || null,
        perms: form.perms || null,
        icon: form.icon || null,
        orderNum: form.orderNum,
        visible: form.visible,
        status: form.status
      }
      if (dialog.mode === 'add') {
        await createMenu(payload)
        ElMessage.success('新增成功')
      } else {
        payload.id = form.id
        await updateMenu(payload)
        ElMessage.success('更新成功')
      }
      dialog.visible = false
      loadData()
    } finally {
      dialog.submitting = false
    }
  })
}

// 删除
function handleDelete(row: any) {
  ElMessageBox.confirm(`确定删除菜单「${row.menuName}」吗？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '确定删除',
    cancelButtonText: '取消'
  })
    .then(async () => {
      await deleteMenu(row.id)
      ElMessage.success('删除成功')
      loadData()
    })
    .catch(() => {})
}

onMounted(loadData)
</script>
