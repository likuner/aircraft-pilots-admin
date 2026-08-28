<template>
  <div class="h-screen flex items-center justify-center bg-gradient-to-br from-slate-900 via-blue-900 to-slate-800">
    <div class="w-[400px] bg-white dark:bg-[#1e2430] rounded-xl shadow-2xl p-8">
      <div class="text-center mb-8">
        <div class="mb-2 flex justify-center">
          <el-icon :size="42" color="#2f6fed"><Promotion /></el-icon>
        </div>
        <h1 class="text-xl font-bold text-gray-800 dark:text-gray-100">无人机驾驶员管理后台</h1>
        <p class="text-xs text-gray-400 mt-1">考试管理 · 合格证颁发 · 机构认证</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" clearable />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" :prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item prop="captchaCode">
          <div class="flex items-center gap-3 w-full">
            <el-input v-model="form.captchaCode" placeholder="验证码" :prefix-icon="Key" clearable class="flex-1" />
            <img
              :src="captchaImg"
              alt="验证码"
              class="h-10 w-28 cursor-pointer rounded border border-gray-200 dark:border-gray-600"
              title="点击刷新"
              @click="fetchCaptcha"
            />
          </div>
        </el-form-item>
        <el-button type="primary" class="w-full mt-2" :loading="loading" @click="handleLogin">
          登 录
        </el-button>
      </el-form>

      <p class="text-xs text-gray-400 text-center mt-6">
        默认账号：admin / 123456（管理员） · examiner1 / 123456（考官）
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { User, Lock, Key } from '@element-plus/icons-vue'
import { getCaptcha } from '@/api/auth'
import type { LoginForm } from '@/api/auth'

const route = useRoute()
const router = useRouter()
const store = useStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const captchaImg = ref('')
const captchaKey = ref('')

const form = reactive<LoginForm>({
  username: '',
  password: '',
  captchaCode: '',
  captchaKey: ''
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

async function fetchCaptcha() {
  const res = await getCaptcha()
  const img = res.data.img || ''
  captchaImg.value = img.startsWith('data:') ? img : `data:image/png;base64,${img}`
  captchaKey.value = res.data.captchaKey
  form.captchaCode = ''
}

async function handleLogin() {
  await formRef.value?.validate()
  loading.value = true
  try {
    form.captchaKey = captchaKey.value
    await store.dispatch('user/login', { ...form })
    ElMessage.success('登录成功')
    const redirect = route.query.redirect
    router.push(redirect ? decodeURIComponent(String(redirect)) : '/')
  } catch (e) {
    // 登录失败刷新验证码
    fetchCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(fetchCaptcha)
</script>
