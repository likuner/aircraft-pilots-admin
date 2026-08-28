<template>
  <div class="h-screen flex flex-col items-center justify-center bg-gray-100 dark:bg-[#14181f]">
    <el-icon class="mb-4" :size="88" color="#9ca3af"><Compass /></el-icon>
    <h1 class="text-6xl font-bold text-gray-300 dark:text-gray-600 mb-2">404</h1>
    <p class="text-gray-500 mb-6">页面不存在或您没有访问权限</p>
    <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
  </div>
</template>

<script setup lang="ts">
</script>
