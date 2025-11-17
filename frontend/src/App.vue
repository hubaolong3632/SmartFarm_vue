<script setup></script>

<!-- 应用布局：头部导航 + 主内容区域，承载所有页面 -->
<template>
  <el-container style="min-height: 100vh;">
    <el-header height="56px" style="display:flex;align-items:center;gap:16px;">
      <!-- 标题：系统名称 -->
      <div style="font-weight: 700; font-size: 18px;">智能温室</div>
      <!-- 顶部导航：切换各功能模块页面 -->
      <el-menu mode="horizontal" :default-active="$route.path" router
               style="border-bottom: none; flex: 1;">
        <el-menu-item index="/">概览</el-menu-item>
        <el-menu-item index="/recipes">配方管理</el-menu-item>
        <el-menu-item index="/soil-assignment">土壤分配</el-menu-item>
        <el-menu-item index="/automation">自动化</el-menu-item>
        <el-menu-item index="/gallery">图片集</el-menu-item>
        <el-menu-item index="/ai-analysis">AI自动化分析</el-menu-item>
      </el-menu>
    </el-header>
    <el-main style="background:#f6f7f9;">
      <!-- 页面内容容器：所有路由视图在此渲染 -->
      <div style="max-width: 1200px; margin: 0 auto;">
        <router-view />
      </div>
    </el-main>
  </el-container>
  </template>

<style scoped></style>
