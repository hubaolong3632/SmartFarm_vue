import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

// 路由：定义页面路径与对应视图组件，用于顶部导航切换
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/Login.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/',
      name: 'dashboard',
      // 概览：仪表盘（折线图、当前状态、报警、控制面板）
      component: () => import('../views/Dashboard.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/recipes',
      name: 'recipes',
      // 配方管理：新增/查看营养配方
      component: () => import('../views/RecipeEditor.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/soil-assignment',
      name: 'soilAssignment',
      // 土壤分配：为地块分配配方，设置定时计划，查看执行日志
      component: () => import('../views/SoilAssignment.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/automation',
      name: 'automation',
      // 自动化控制：阈值配置（光照/湿度），自动策略开关
      component: () => import('../views/AutomationSettings.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/gallery',
      name: 'gallery',
      // 图片集：按日期查看每小时图片与指标，并标记异常
      component: () => import('../views/ImageGallery.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/ai-analysis',
      name: 'aiAnalysis',
      // AI自动化分析：图片分析、传感器数据分析、自动化建议、综合报告
      component: () => import('../views/AiAnalysis.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/ai-reports',
      name: 'aiReports',
      // AI分析报告历史：查看和管理保存的报告
      component: () => import('../views/AiReportHistory.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/ai-hosting',
      name: 'aiHosting',
      // AI自动托管：配置和管理AI自动托管功能
      component: () => import('../views/AiHosting.vue'),
      meta: { requiresAuth: true }
    },
  ],
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  const token = localStorage.getItem('jwt')
  
  // 检查路由是否需要认证
  if (to.meta.requiresAuth) {
    if (token) {
      // 有token，允许访问
      next()
    } else {
      // 没有token，跳转到登录页
      next({ path: '/login', query: { redirect: to.fullPath } })
    }
  } else {
    // 不需要认证的路由（如登录页）
    if (to.path === '/login' && token) {
      // 已登录用户访问登录页，跳转到首页
      next({ path: '/' })
    } else {
      next()
    }
  }
})

export default router
