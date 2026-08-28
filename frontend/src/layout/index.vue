<template>
  <div class="h-screen flex bg-gray-100 dark:bg-[#14181f]">
    <!-- 侧边栏 -->
    <Sidebar />
    <!-- 主区域 -->
    <div class="flex-1 flex flex-col min-w-0">
      <Navbar />
      <AppMain />
    </div>
  </div>
</template>

<script setup lang="ts">
import Sidebar from './components/Sidebar.vue'
import Navbar from './components/Navbar.vue'
import AppMain from './components/AppMain.vue'
</script>
