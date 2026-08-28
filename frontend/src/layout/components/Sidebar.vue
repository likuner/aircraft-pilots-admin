<template>
  <aside class="h-full shrink-0 bg-white dark:bg-[#14181f] flex flex-col transition-all duration-200"
    :style="{ width: collapsed ? '4rem' : '14rem' }">
    <!-- Logo -->
    <div class="h-14 flex items-center justify-center gap-2 border-b border-gray-200 dark:border-gray-800 shrink-0">
      <el-icon class="text-gray-900 dark:text-white shrink-0" :size="20"><Promotion /></el-icon>
      <span v-if="!collapsed" class="text-gray-900 dark:text-white font-semibold text-sm tracking-wide whitespace-nowrap">无人机驾驶员管理</span>
    </div>
    <!-- 菜单 -->
    <el-scrollbar class="flex-1">
      <el-menu
        :default-active="activeMenu"
        :default-openeds="openeds"
        :collapse="collapsed"
        :collapse-transition="false"
        background-color="transparent"
        :text-color="isDark ? '#a6adbb' : '#374151'"
        :active-text-color="isDark ? '#ffffff' : '#2f6fed'"
        router
        class="border-r-0 bg-transparent uav-menu"
      >
        <template v-for="menu in menus" :key="menu.id">
          <el-sub-menu v-if="menu.children && menu.children.length" :index="menu.path">
            <template #title>
              <el-icon v-if="menu.icon"><component :is="menu.icon" /></el-icon>
              <span>{{ menu.menuName }}</span>
            </template>
            <el-menu-item v-for="child in menu.children" :key="child.id" :index="resolvePath(menu.path, child.path)">
              <el-icon v-if="child.icon"><component :is="child.icon" /></el-icon>
              <template #title>{{ child.menuName }}</template>
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="menu.path">
            <el-icon v-if="menu.icon"><component :is="menu.icon" /></el-icon>
            <template #title>{{ menu.menuName }}</template>
          </el-menu-item>
        </template>
      </el-menu>
    </el-scrollbar>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useStore } from 'vuex'
import type { MenuNode } from '@/store/modules/permission'

const route = useRoute()
const store = useStore()

const collapsed = computed(() => store.state.app.sidebarCollapsed)
const isDark = computed(() => store.state.app.theme === 'dark')
// 菜单树：优先用后端返回的动态菜单，空时展示默认首页
const menus = computed<MenuNode[]>(() => {
  const dynamic = store.state.permission.menus
  if (dynamic && dynamic.length) return dynamic
  return [{ id: 0, menuName: '首页', path: '/dashboard', icon: 'Odometer' }]
})

const activeMenu = computed(() => route.path)

// 自动展开当前路由所属的父级菜单（一级目录）
const openeds = computed(() => {
  const segments = route.path.split('/').filter(Boolean)
  return segments.length > 1 ? ['/' + segments[0]] : []
})

// 拼接菜单完整路径（兼容子菜单相对/绝对路径）
function resolvePath(parentPath: string, childPath: string): string {
  if (!childPath) return parentPath
  if (childPath.startsWith('/')) return childPath
  return `${parentPath}/${childPath}`.replace(/\/+/g, '/')
}
</script>

<style>
/* 菜单 hover/active 背景（非 scoped：需要 html.dark 祖先选择器，.uav-menu 前缀限定作用范围） */
.uav-menu .el-sub-menu__title:hover,
.uav-menu .el-menu-item:hover {
  background-color: #f3f4f6 !important;
}
.uav-menu .el-menu-item.is-active {
  background-color: #eaf1fe !important;
}
html.dark .uav-menu .el-sub-menu__title:hover,
html.dark .uav-menu .el-menu-item:hover {
  background-color: #1e2430 !important;
}
html.dark .uav-menu .el-menu-item.is-active {
  background-color: #252c3a !important;
}
</style>
