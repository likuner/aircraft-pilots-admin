<template>
  <header class="h-14 shrink-0 bg-white dark:bg-[#1e2430] border-b border-gray-200 dark:border-[#333b4c] flex items-center justify-between px-4 text-gray-900 dark:text-gray-100">
    <div class="flex items-center gap-3">
      <el-icon class="cursor-pointer text-lg text-gray-600 dark:text-gray-300 hover:text-primary dark:hover:text-primary" @click="toggleSidebar">
        <Fold v-if="!collapsed" />
        <Expand v-else />
      </el-icon>
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item v-if="route.meta.title">{{ route.meta.title }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>
    <div class="flex items-center gap-4">
      <el-tooltip :content="isDark ? '切换到浅色模式' : '切换到深色模式'" placement="bottom">
        <button
          class="w-9 h-9 rounded-full flex items-center justify-center cursor-pointer border border-gray-300 dark:border-[#3a4356] bg-gray-50 dark:bg-[#252c3a] hover:bg-gray-200 dark:hover:bg-[#2f3748] transition-colors"
          @click="toggleTheme"
        >
          <el-icon :size="20" class="text-amber-500 dark:text-amber-300">
            <Moon v-if="!isDark" />
            <Sunny v-else />
          </el-icon>
        </button>
      </el-tooltip>
      <el-dropdown @command="handleCommand">
        <span class="flex items-center gap-2 cursor-pointer outline-none">
          <el-avatar :size="30" class="bg-primary">{{ avatarText }}</el-avatar>
          <span class="text-sm">{{ nickname }}</span>
          <el-icon class="text-gray-500 dark:text-gray-400"><ArrowDown /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="password">修改密码</el-dropdown-item>
            <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { UserInfo } from '@/api/auth'

const route = useRoute()
const router = useRouter()
const store = useStore()

const collapsed = computed(() => store.state.app.sidebarCollapsed)
const userInfo = computed<UserInfo>(() => (store.state.user.userInfo || {}) as UserInfo)
const nickname = computed(() => userInfo.value.nickname || userInfo.value.username || '用户')
const avatarText = computed(() => (nickname.value || 'U').charAt(0).toUpperCase())
const isDark = computed(() => store.state.app.theme === 'dark')

function toggleSidebar() {
  store.dispatch('app/toggleSidebar')
}

function toggleTheme() {
  const next = isDark.value ? 'light' : 'dark'
  store.dispatch('app/setTheme', next)
}

async function handleCommand(command: string) {
  if (command === 'logout') {
    await ElMessageBox.confirm('确定退出登录吗？', '提示', { type: 'warning' })
    await store.dispatch('user/logout')
    router.push('/login')
  } else if (command === 'password') {
    ElMessage.info('修改密码功能开发中')
  }
}
</script>
