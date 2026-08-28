<template>
  <div v-loading="loading">
    <h2 class="text-lg font-semibold text-gray-800 dark:text-gray-100 mb-4">工作台</h2>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="mb-4">
      <el-col :span="4" v-for="card in statCards" :key="card.label">
        <div class="page-card flex items-center gap-3 !p-4">
          <div class="w-10 h-10 rounded-lg flex items-center justify-center" :style="{ background: card.bg, color: card.color }">
            <el-icon :size="20"><component :is="card.icon" /></el-icon>
          </div>
          <div>
            <div class="text-2xl font-bold leading-none">{{ card.value ?? '-' }}</div>
            <div class="text-xs text-gray-400 mt-1">{{ card.label }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 公告列表 + 快捷入口 -->
    <el-row :gutter="16">
      <el-col :span="14">
        <div class="page-card">
          <div class="flex items-center justify-between mb-3">
            <span class="font-medium">最新公告</span>
            <el-button link type="primary" @click="$router.push('/system/notice')">更多</el-button>
          </div>
          <el-empty v-if="!notices.length" description="暂无公告" :image-size="60" />
          <div v-else>
            <div v-for="n in notices" :key="n.id" class="flex items-center justify-between py-2 border-b border-gray-100 dark:border-gray-700 last:border-0">
              <span class="text-sm text-gray-700 dark:text-gray-300 cursor-pointer hover:text-primary truncate max-w-[70%]" @click="$router.push('/system/notice')">
                {{ n.title }}
              </span>
              <span class="text-xs text-gray-400 shrink-0">{{ formatTime(n.publishTime) }}</span>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :span="10">
        <div class="page-card">
          <div class="font-medium mb-3">快捷入口</div>
          <el-row :gutter="12">
            <el-col :span="8" v-for="entry in quickEntries" :key="entry.path">
              <div class="flex flex-col items-center gap-1 py-3 rounded-lg cursor-pointer hover:bg-gray-50 dark:hover:bg-gray-800" @click="$router.push(entry.path)">
                <el-icon :size="22" color="#2f6fed"><component :is="entry.icon" /></el-icon>
                <span class="text-xs text-gray-500 dark:text-gray-400">{{ entry.label }}</span>
              </div>
            </el-col>
          </el-row>
        </div>
      </el-col>
    </el-row>

    <!-- 状态分布 -->
    <el-row :gutter="16" class="mt-4">
      <el-col :span="12">
        <div class="page-card">
          <div class="font-medium mb-3">报名状态分布</div>
          <div v-if="registrationStatusStats.length">
            <div v-for="(item, index) in registrationStatusStats" :key="index" class="py-2">
              <div class="flex items-center justify-between mb-1">
                <StatusTag :status="item.status" />
                <span class="text-sm font-medium text-gray-700 dark:text-gray-300">{{ item.cnt }}（{{ percent(item.cnt, registrationTotal) }}%）</span>
              </div>
              <div class="h-2 rounded-full bg-gray-100 dark:bg-gray-700 overflow-hidden">
                <div class="h-full rounded-full transition-all duration-500" :style="{ width: percent(item.cnt, registrationTotal) + '%', background: regBarColors[index % regBarColors.length] }"></div>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无数据" :image-size="60" />
        </div>
      </el-col>
      <el-col :span="12">
        <div class="page-card">
          <div class="font-medium mb-3">成绩通过分布</div>
          <div v-if="passStatusStats.length">
            <div v-for="(item, index) in passStatusStats" :key="index" class="py-2">
              <div class="flex items-center justify-between mb-1">
                <StatusTag :status="item.pass_status" :map="passStatusMap" />
                <span class="text-sm font-medium text-gray-700 dark:text-gray-300">{{ item.cnt }}（{{ percent(item.cnt, passTotal) }}%）</span>
              </div>
              <div class="h-2 rounded-full bg-gray-100 dark:bg-gray-700 overflow-hidden">
                <div class="h-full rounded-full transition-all duration-500" :style="{ width: percent(item.cnt, passTotal) + '%', background: passBarColors[item.pass_status] || '#909399' }"></div>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无数据" :image-size="60" />
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getDashboardStats, listLatestNotices } from '@/api/dashboard'
import StatusTag from '@/components/StatusTag.vue'

interface StatCard {
  key: string
  label: string
  icon: string
  value: number | null
  bg: string
  color: string
}

const loading = ref(false)
const statCards = ref<StatCard[]>([
  { key: 'registrationCount', label: '报名总数', icon: 'Document', value: null, bg: '#eaf1fe', color: '#2f6fed' },
  { key: 'pendingScoreCount', label: '待审成绩', icon: 'Timer', value: null, bg: '#fff7e6', color: '#fa8c16' },
  { key: 'pendingCertApplyCount', label: '待审证申请', icon: 'Tickets', value: null, bg: '#fff1f0', color: '#f5222d' },
  { key: 'validCertCount', label: '合格证数', icon: 'Medal', value: null, bg: '#f6ffed', color: '#52c41a' },
  { key: 'pendingInspectionCount', label: '待核查机构', icon: 'OfficeBuilding', value: null, bg: '#f0f5ff', color: '#722ed1' }
])

const notices = ref<any[]>([])
const registrationStatusStats = ref<any[]>([])
const passStatusStats = ref<any[]>([])

const passStatusMap = {
  PASS: { label: '通过', type: 'success' },
  FAIL: { label: '未通过', type: 'danger' },
  NOT_EVALUATED: { label: '未评定', type: 'info' }
}

// 分布条配色
const regBarColors = ['#2f6fed', '#52c41a', '#fa8c16', '#f5222d', '#722ed1', '#13c2c2']
const passBarColors: Record<string, string> = {
  PASS: '#52c41a',
  FAIL: '#f5222d',
  NOT_EVALUATED: '#909399'
}

const registrationTotal = computed(() => registrationStatusStats.value.reduce((sum, i) => sum + Number(i.cnt || 0), 0))
const passTotal = computed(() => passStatusStats.value.reduce((sum, i) => sum + Number(i.cnt || 0), 0))

function percent(cnt: number, total: number): number {
  if (!total) return 0
  return Math.round((Number(cnt) / total) * 100)
}

const quickEntries = ref([
  { label: '考试计划', icon: 'Calendar', path: '/exam/plan' },
  { label: '报名管理', icon: 'EditPen', path: '/exam/registration' },
  { label: '成绩审核', icon: 'DataAnalysis', path: '/exam/scoreAudit' },
  { label: '证书申请', icon: 'Medal', path: '/certificate/apply' },
  { label: '认证申请', icon: 'OfficeBuilding', path: '/institution/application' },
  { label: '用户管理', icon: 'User', path: '/system/user' }
])

function formatTime(value: string | null | undefined): string {
  if (!value) return '-'
  const s = value.replace('T', ' ')
  return s.length > 19 ? s.substring(0, 19) : s
}

async function loadData() {
  loading.value = true
  try {
    const [statsRes, noticeRes] = await Promise.all([getDashboardStats(), listLatestNotices(5)])
    const stats = statsRes.data || {}
    statCards.value.forEach((card) => {
      if (stats[card.key] != null) card.value = stats[card.key]
    })
    registrationStatusStats.value = stats.registrationStatusStats || []
    passStatusStats.value = stats.passStatusStats || []
    notices.value = noticeRes.data?.rows || []
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>
