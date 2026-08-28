import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

// 静态路由：所有角色可见
export const constantRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', hidden: true }
  },
  {
    path: '/404',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '404', hidden: true }
  }
]

// 异步路由：依据后端菜单权限动态注册（component 与后端 component 字段对应）
// 后端菜单 component 存形如 "system/user/index" 的路径，这里用 import.meta.glob 映射
export const asyncRoutes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '首页', icon: 'Odometer', permission: 'dashboard:view' }
      }
    ]
  },
  {
    path: '/system',
    component: () => import('@/layout/index.vue'),
    meta: { title: '系统管理', icon: 'Setting' },
    children: [
      {
        path: 'user',
        component: () => import('@/views/system/user/index.vue'),
        meta: { title: '用户管理', permission: 'system:user:list' }
      },
      {
        path: 'role',
        component: () => import('@/views/system/role/index.vue'),
        meta: { title: '角色管理', permission: 'system:role:list' }
      },
      {
        path: 'menu',
        component: () => import('@/views/system/menu/index.vue'),
        meta: { title: '菜单管理', permission: 'system:menu:list' }
      },
      {
        path: 'log',
        component: () => import('@/views/system/log/index.vue'),
        meta: { title: '操作日志', permission: 'system:log:list' }
      },
      {
        path: 'notice',
        component: () => import('@/views/system/notice/index.vue'),
        meta: { title: '公告管理', permission: 'system:notice:list' }
      }
    ]
  },
  {
    path: '/student',
    component: () => import('@/layout/index.vue'),
    meta: { title: '考生档案', icon: 'User' },
    children: [
      {
        path: 'profile',
        component: () => import('@/views/student/profileList/index.vue'),
        meta: { title: '考生列表', permission: 'student:profile:list' }
      }
    ]
  },
  {
    path: '/exam',
    component: () => import('@/layout/index.vue'),
    meta: { title: '考试管理', icon: 'Tickets' },
    children: [
      {
        path: 'plan',
        component: () => import('@/views/exam/plan/index.vue'),
        meta: { title: '考试计划', permission: 'exam:plan:list' }
      },
      {
        path: 'session',
        component: () => import('@/views/exam/session/index.vue'),
        meta: { title: '考试场次', permission: 'exam:session:list' }
      },
      {
        path: 'room',
        component: () => import('@/views/exam/room/index.vue'),
        meta: { title: '考场管理', permission: 'exam:room:list' }
      },
      {
        path: 'batch',
        component: () => import('@/views/exam/batch/index.vue'),
        meta: { title: '批次编排', permission: 'exam:batch:list' }
      },
      {
        path: 'registration',
        component: () => import('@/views/exam/registration/index.vue'),
        meta: { title: '报名管理', permission: 'exam:registration:list' }
      },
      {
        path: 'score',
        component: () => import('@/views/exam/score/index.vue'),
        meta: { title: '成绩录入', permission: 'exam:score:list' }
      },
      {
        path: 'scoreAudit',
        component: () => import('@/views/exam/scoreAudit/index.vue'),
        meta: { title: '成绩审核', permission: 'exam:scoreAudit:list' }
      }
    ]
  },
  {
    path: '/certificate',
    component: () => import('@/layout/index.vue'),
    meta: { title: '证书管理', icon: 'Medal' },
    children: [
      {
        path: 'apply',
        component: () => import('@/views/certificate/apply/index.vue'),
        meta: { title: '证书申请', permission: 'cert:apply:list' }
      },
      {
        path: 'certificate',
        component: () => import('@/views/certificate/certificate/index.vue'),
        meta: { title: '合格证管理', permission: 'cert:certificate:list' }
      }
    ]
  },
  {
    path: '/institution',
    component: () => import('@/layout/index.vue'),
    meta: { title: '机构认证', icon: 'OfficeBuilding' },
    children: [
      {
        path: 'institution',
        component: () => import('@/views/institution/institution/index.vue'),
        meta: { title: '机构列表', permission: 'inst:institution:list' }
      },
      {
        path: 'application',
        component: () => import('@/views/institution/application/index.vue'),
        meta: { title: '认证申请', permission: 'inst:application:list' }
      },
      {
        path: 'materialReview',
        component: () => import('@/views/institution/materialReview/index.vue'),
        meta: { title: '材料审查', permission: 'inst:material:list' }
      },
      {
        path: 'inspection',
        component: () => import('@/views/institution/inspection/index.vue'),
        meta: { title: '现场核查', permission: 'inst:inspection:list' }
      },
      {
        path: 'qualification',
        component: () => import('@/views/institution/qualification/index.vue'),
        meta: { title: '资质证书', permission: 'inst:qualification:list' }
      }
    ]
  },
  // 404 兜底放最后
  {
    path: '/:pathMatch(.*)*',
    redirect: '/404',
    meta: { hidden: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes: constantRoutes,
  scrollBehavior: () => ({ top: 0 })
})

export default router
