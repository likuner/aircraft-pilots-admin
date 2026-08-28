import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'
import './router/guard' // 全局路由守卫
import store from './store'
import { getStoredTheme } from './store/modules/app'
import { hasPermi } from './directives/permission'

import './styles/index.css'

// 启动时同步主题类（挂载前执行，避免主题闪烁）
document.documentElement.classList.toggle('dark', getStoredTheme() === 'dark')

const app = createApp(App)

// 全局注册 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(createPinia())
app.use(store)
app.use(router)
app.use(ElementPlus, { locale: zhCn })

// 按钮级权限指令 v-hasPermi
app.directive('hasPermi', hasPermi)

app.mount('#app')
