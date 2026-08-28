/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{vue,js,ts,jsx,tsx}'],
  // 深色模式基于 .dark 类切换（Navbar 按钮），而非系统偏好
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        // 与 Element Plus 主题色保持一致（--el-color-primary 覆盖值）
        primary: {
          DEFAULT: '#2f6fed',
          50: '#eaf1fe',
          100: '#d6e3fd',
          500: '#2f6fed',
          600: '#2563eb',
          700: '#1d4ed8'
        }
      }
    }
  },
  plugins: []
}
