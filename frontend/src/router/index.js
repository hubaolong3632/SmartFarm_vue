import { createRouter, createWebHistory } from 'vue-router'

// 路由：定义页面路径与对应视图组件，用于顶部导航切换
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'dashboard',
      // 概览：仪表盘（折线图、当前状态、报警、控制面板）
      component: () => import('../views/Dashboard.vue'),
    },
    {
      path: '/recipes',
      name: 'recipes',
      // 配方管理：新增/查看营养配方
      component: () => import('../views/RecipeEditor.vue'),
    },
    {
      path: '/soil-assignment',
      name: 'soilAssignment',
      // 土壤分配：为地块分配配方，设置定时计划，查看执行日志
      component: () => import('../views/SoilAssignment.vue'),
    },
    {
      path: '/automation',
      name: 'automation',
      // 自动化控制：阈值配置（光照/湿度），自动策略开关
      component: () => import('../views/AutomationSettings.vue'),
    },
    {
      path: '/gallery',
      name: 'gallery',
      // 图片集：按日期查看每小时图片与指标，并标记异常
      component: () => import('../views/ImageGallery.vue'),
    },
    {
      path: '/ai-analysis',
      name: 'aiAnalysis',
      // AI自动化分析：图片分析、传感器数据分析、自动化建议、综合报告
      component: () => import('../views/AiAnalysis.vue'),
    },
  ],
})

export default router
