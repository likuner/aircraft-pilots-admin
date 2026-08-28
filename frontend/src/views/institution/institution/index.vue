<template>
  <div class="page-card">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input v-model="queryParams.keyword" placeholder="机构名称" clearable class="!w-48" @keyup.enter="handleQuery" />
      <el-select v-model="queryParams.qualificationStatus" placeholder="认证状态" clearable class="!w-32">
        <el-option label="已认证" value="CERTIFIED" />
        <el-option label="认证中" value="PENDING" />
        <el-option label="未认证" value="NONE" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="handleQuery">查询</el-button>
      <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
    </div>

    <div class="toolbar">
      <span class="text-sm text-gray-500 dark:text-gray-400">共 {{ total }} 家机构</span>
      <el-button v-hasPermi="'inst:institution:add'" type="primary" :icon="Plus" @click="handleAdd">新增机构</el-button>
    </div>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="instCode" label="机构代码" min-width="110" show-overflow-tooltip />
      <el-table-column prop="instName" label="机构名称" min-width="180" show-overflow-tooltip />
      <el-table-column prop="orgType" label="类型" width="80" align="center" />
      <el-table-column prop="legalPerson" label="法人" width="90" show-overflow-tooltip />
      <el-table-column label="注册资本" width="100" align="right">
        <template #default="{ row }">
          <span class="text-gray-700 dark:text-gray-200">{{ row.registeredCapital != null ? `${row.registeredCapital} 万` : '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="contactName" label="联系人" width="90" show-overflow-tooltip />
      <el-table-column prop="contactPhone" label="电话" width="130" show-overflow-tooltip />
      <el-table-column label="认证状态" width="95" align="center">
        <template #default="{ row }">
          <StatusTag :status="row.qualificationStatus || 'NONE'" :map="qualMap" />
        </template>
      </el-table-column>
      <el-table-column label="启用" width="80" align="center">
        <template #default="{ row }">
          <StatusTag :status="row.status === 1 ? 'ACTIVE' : 'INACTIVE'" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140" align="center" fixed="right">
        <template #default="{ row }">
          <el-button v-hasPermi="'inst:institution:edit'" link type="primary" @click="handleEdit(row)">编辑</el-button>
          <el-button v-hasPermi="'inst:institution:delete'" link type="danger" @click="handleDelete(row)">删除</el-button>
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

    <!-- 新增/编辑机构弹窗 -->
    <el-dialog v-model="dialog.visible" :title="dialog.mode === 'add' ? '新增机构' : '编辑机构'" width="640px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="机构名称" prop="instName">
              <el-input v-model="form.instName" placeholder="请输入" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="统一信用代码" prop="creditCode">
              <el-input v-model="form.creditCode" placeholder="请输入" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="机构类型" prop="orgType">
              <el-select v-model="form.orgType" placeholder="请选择" class="!w-full">
                <el-option label="企业" value="企业" />
                <el-option label="事业单位" value="事业单位" />
                <el-option label="社会组织" value="社会组织" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="法人代表" prop="legalPerson">
              <el-input v-model="form.legalPerson" placeholder="请输入" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="注册资本(万)" prop="registeredCapital">
              <el-input-number v-model="form.registeredCapital" :min="0" :precision="2" :step="10" class="!w-full" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系人" prop="contactName">
              <el-input v-model="form.contactName" placeholder="请输入" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话" prop="contactPhone">
              <el-input v-model="form.contactPhone" placeholder="请输入" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="电子邮箱" prop="email">
              <el-input v-model="form.email" placeholder="请输入" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="地址" prop="address">
              <el-input v-model="form.address" placeholder="请输入" maxlength="150" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="经营范围" prop="businessScope">
              <el-input v-model="form.businessScope" type="textarea" :rows="2" placeholder="请输入" maxlength="255" />
            </el-form-item>
          </el-col>
        </el-row>
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
import { listInstitutions, createInstitution, updateInstitution, deleteInstitution } from '@/api/institution'

const qualMap: Record<string, { label: string; type: string }> = {
  CERTIFIED: { label: '已认证', type: 'success' },
  PENDING: { label: '认证中', type: 'warning' },
  NONE: { label: '未认证', type: 'info' }
}

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const queryParams = reactive({ pageNum: 1, pageSize: 10, keyword: '', qualificationStatus: '' })

const dialog = reactive({ visible: false, mode: 'add' as 'add' | 'edit', submitting: false })
const formRef = ref<FormInstance>()
const form = reactive<any>({})
const rules: FormRules = {
  instName: [{ required: true, message: '请输入机构名称', trigger: 'blur' }],
  creditCode: [{ required: true, message: '请输入统一信用代码', trigger: 'blur' }],
  orgType: [{ required: true, message: '请选择机构类型', trigger: 'change' }],
  legalPerson: [{ required: true, message: '请输入法人代表', trigger: 'blur' }],
  contactName: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
  contactPhone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }]
}

async function loadData() {
  loading.value = true
  try {
    const res = await listInstitutions({
      pageNum: queryParams.pageNum,
      pageSize: queryParams.pageSize,
      keyword: queryParams.keyword || undefined,
      qualificationStatus: queryParams.qualificationStatus || undefined
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
  queryParams.qualificationStatus = ''
  handleQuery()
}

function handleAdd() {
  Object.keys(form).forEach((k) => delete form[k])
  form.orgType = '企业'
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
      await createInstitution(form)
      ElMessage.success('新增成功')
    } else {
      await updateInstitution({ ...form, id: form.id })
      ElMessage.success('保存成功')
    }
    dialog.visible = false
    loadData()
  } finally {
    dialog.submitting = false
  }
}

async function handleDelete(row: any) {
  await ElMessageBox.confirm(`确定删除机构「${row.instName}」吗？`, '提示', { type: 'warning' })
  await deleteInstitution(row.id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(loadData)
</script>
