<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Menu as MenuIcon } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const drawerVisible = ref(false)
const isMobile = ref(false)

function checkMobile() {
  isMobile.value = window.innerWidth < 768
}

function handleResize() {
  checkMobile()
  if (!isMobile.value) {
    drawerVisible.value = false
  }
}

function handleMenuClick(path) {
  router.push(path)
  drawerVisible.value = false
}

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})
</script>

<!-- 应用布局：头部导航 + 主内容区域，承载所有页面 -->
<template>
  <el-container style="min-height: 100vh;">
    <el-header class="fixed-header">
      <!-- 标题：系统名称 -->
      <div class="header-title">智能温室</div>
      
      <!-- 桌面端导航菜单 -->
      <el-menu 
        v-if="!isMobile"
        mode="horizontal" 
        :default-active="route.path" 
        router
        class="desktop-menu"
      >
        <el-menu-item index="/">概览</el-menu-item>
        <el-menu-item index="/recipes">配方管理</el-menu-item>
        <el-menu-item index="/soil-assignment">土壤分配</el-menu-item>
        <el-menu-item index="/automation">自动化</el-menu-item>
        <el-menu-item index="/gallery">图片集</el-menu-item>
        <el-menu-item index="/ai-analysis">AI自动化分析</el-menu-item>
        <el-menu-item index="/ai-reports">AI报告历史</el-menu-item>
        <el-menu-item index="/ai-hosting">AI自动托管</el-menu-item>
      </el-menu>

      <!-- 移动端菜单按钮 -->
      <el-button 
        v-else
        :icon="MenuIcon"
        circle
        @click="drawerVisible = true"
        class="mobile-menu-btn"
      />
    </el-header>

    <!-- 移动端侧边栏菜单 -->
    <el-drawer
      v-model="drawerVisible"
      title="菜单"
      direction="rtl"
      size="280px"
      class="mobile-drawer"
    >
      <el-menu
        :default-active="route.path"
        class="mobile-menu"
        @select="handleMenuClick"
      >
        <el-menu-item index="/">
          <span>概览</span>
        </el-menu-item>
        <el-menu-item index="/recipes">
          <span>配方管理</span>
        </el-menu-item>
        <el-menu-item index="/soil-assignment">
          <span>土壤分配</span>
        </el-menu-item>
        <el-menu-item index="/automation">
          <span>自动化</span>
        </el-menu-item>
        <el-menu-item index="/gallery">
          <span>图片集</span>
        </el-menu-item>
        <el-menu-item index="/ai-analysis">
          <span>AI自动化分析</span>
        </el-menu-item>
        <el-menu-item index="/ai-reports">
          <span>AI报告历史</span>
        </el-menu-item>
        <el-menu-item index="/ai-hosting">
          <span>AI自动托管</span>
        </el-menu-item>
      </el-menu>
    </el-drawer>

    <el-main class="main-content" style="background:#f6f7f9;">
      <!-- 页面内容容器：所有路由视图在此渲染 -->
      <div style="max-width: 1200px; margin: 0 auto;">
        <router-view />
      </div>
    </el-main>
  </el-container>
</template>

<style scoped>
.fixed-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  gap: 16px;
  height: 56px;
  padding: 0 16px;
}

.header-title {
  font-weight: 700;
  font-size: 18px;
  white-space: nowrap;
  flex-shrink: 0;
}

.desktop-menu {
  border-bottom: none;
  flex: 1;
}

.mobile-menu-btn {
  margin-left: auto;
}

.main-content {
  margin-top: 56px;
}

.mobile-menu {
  border-right: none;
}

.mobile-menu :deep(.el-menu-item) {
  height: 48px;
  line-height: 48px;
  font-size: 15px;
}

.mobile-drawer :deep(.el-drawer__header) {
  margin-bottom: 0;
  padding: 20px;
  border-bottom: 1px solid #e5e7eb;
}

.mobile-drawer :deep(.el-drawer__body) {
  padding: 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .header-title {
    font-size: 16px;
  }

  .fixed-header {
    padding: 0 12px;
    gap: 12px;
  }
}

@media (max-width: 480px) {
  .header-title {
    font-size: 15px;
  }

  .fixed-header {
    height: 52px;
  }

  .main-content {
    margin-top: 52px;
  }
}
</style>
