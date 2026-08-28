<template>
  <el-tag :type="tagType" :effect="effect" size="small">
    {{ label }}
  </el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface StatusMapItem {
  label: string
  type: string
}

const props = defineProps({
  // 状态值（英文枚举，与后端统一）
  status: { type: String, default: '' },
  // 可选：直接传入中文文案覆盖映射
  text: { type: String, default: '' },
  // 状态 -> 文案/颜色 映射（可按模块覆盖）
  map: { type: Object, default: () => ({}) },
  effect: { type: String, default: 'light' }
})

const DEFAULT_MAP: Record<string, StatusMapItem> = {
  // 通用
  ACTIVE: { label: '启用', type: 'success' },
  INACTIVE: { label: '停用', type: 'info' },
  DISABLED: { label: '禁用', type: 'danger' },
  // 报名
  PENDING: { label: '待审核', type: 'warning' },
  APPROVED: { label: '已通过', type: 'success' },
  REJECTED: { label: '已驳回', type: 'danger' },
  SCHEDULED: { label: '已排考', type: 'primary' },
  // 场次
  DRAFT: { label: '草稿', type: 'info' },
  PUBLISHED: { label: '已发布', type: 'success' },
  ENROLLMENT_CLOSED: { label: '报名截止', type: 'warning' },
  IN_PROGRESS: { label: '进行中', type: 'primary' },
  COMPLETED: { label: '已完成', type: 'success' },
  CANCELED: { label: '已取消', type: 'danger' },
  // 成绩
  SUBMITTED: { label: '已提交', type: 'warning' },
  // 证书
  PENDING_AUDIT: { label: '待审核', type: 'warning' },
  AUDIT_PASSED: { label: '审核通过', type: 'success' },
  ISSUED: { label: '已签发', type: 'success' },
  VALID: { label: '有效', type: 'success' },
  EXPIRED: { label: '已过期', type: 'info' },
  REVOKED: { label: '已吊销', type: 'danger' },
  // 机构认证
  MATERIAL_REVIEWING: { label: '材料审查中', type: 'warning' },
  INSPECTION_PENDING: { label: '待核查', type: 'warning' },
  QUALIFICATION_REVIEWING: { label: '资质评审中', type: 'warning' },
  CERTIFIED: { label: '已认证', type: 'success' },
  NONE: { label: '未认证', type: 'info' }
}

const resolved = computed<StatusMapItem>(() => {
  if (props.text) return { label: props.text, type: 'info' }
  const custom = (props.map as Record<string, StatusMapItem>)[props.status]
  if (custom) return custom
  return DEFAULT_MAP[props.status] || { label: props.status || '-', type: 'info' }
})

const label = computed(() => resolved.value.label)
const tagType = computed(() => resolved.value.type)
</script>
