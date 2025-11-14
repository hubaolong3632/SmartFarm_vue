<script setup>
// 概览页面：执行日志图表 + 传感数据折线图 + 控制面板 + 当前状态/报警
import { onMounted, onUnmounted } from 'vue'
import LineChart from '../components/LineChart.vue'
import EChartLine from '../components/EChartLine.vue'
import AlertsPanel from '../components/AlertsPanel.vue'
import ControlsPanel from '../components/ControlsPanel.vue'
import { useGreenhouseStore } from '../stores/greenhouse'

const store = useGreenhouseStore()

onMounted(async () => {
  // 加载所有数据
  await store.loadAllData()
  await store.loadExecutionsLast24()
  // 开始数据轮询
  store.startSimulation()
})

onUnmounted(() => {
  // 停止数据轮询
  store.stopSimulation()
})
</script>

<template>
  <el-row :gutter="16" align="top">
    <el-col :md="16" :xs="24">
      <!-- 执行日志图表（最近24小时，每小时执行次数） -->
      <el-card shadow="never" style="margin-bottom:16px;">
        <template #header>执行日志（次数/小时，最近24小时）</template>
        <EChartLine
          title="执行次数"
          :data="store.executionsLast24"
          data-key="count"
          unit=" 次"
          color="#6366f1"
          height="220px"
        />
      </el-card>
      <!-- 实时传感器数据折线图（使用ECharts，每个数据项独立显示） -->
      <el-card shadow="never" style="margin-bottom: 16px;">
        <template #header>
          <div style="display: flex; justify-content: space-between; align-items: center;">
            <span>实时传感器数据折线图（最新30条记录对比）</span>
            <span style="font-size: 12px; color: #999;">数据点数: {{ store.hourly.length }}/30</span>
          </div>
        </template>
        <el-row :gutter="16">
          <!-- 温度图表 -->
          <el-col :span="12">
            <EChartLine
              title="温度"
              :data="store.hourly"
              data-key="temperatureC"
              unit="°C"
              color="#ef4444"
              height="250px"
            />
          </el-col>
          <!-- 土壤湿度图表 -->
          <el-col :span="12">
            <EChartLine
              title="土壤湿度"
              :data="store.hourly"
              data-key="soilMoisturePct"
              unit="%"
              color="#3b82f6"
              height="250px"
            />
          </el-col>
        </el-row>
        <el-row :gutter="16" style="margin-top: 16px;">
          <!-- 光照图表 -->
          <el-col :span="12">
            <EChartLine
              title="光照强度"
              :data="store.hourly"
              data-key="lightLux"
              unit=" lux"
              color="#10b981"
              height="250px"
            />
          </el-col>
          <!-- 是否下雨图表 -->
          <el-col :span="12">
            <EChartLine
              title="是否下雨"
              :data="store.hourly"
              data-key="isRaining"
              unit=""
              color="#f59e0b"
              height="250px"
            />
          </el-col>
        </el-row>
      </el-card>
      <!-- 控制面板（手动清理/补光灯开关） -->
      <el-card shadow="never" style="margin-top:16px;">
        <ControlsPanel
          :light-on="store.lightOn"
          :cleaning="store.cleaningInProgress"
          @clean="store.triggerCleaning"
          @toggle-light="store.toggleLight()"
        />
      </el-card>
    </el-col>
    <el-col :md="8" :xs="24">
      <!-- 当前传感状态快照 -->
      <el-card shadow="never">
        <template #header>当前状态</template>
        <div v-if="store.latest" style="display:grid;row-gap:6px;">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="温度">{{ store.latest.temperatureC.toFixed(1) }}°C</el-descriptions-item>
            <el-descriptions-item label="土壤湿度">{{ Math.round(store.latest.soilMoisturePct) }}%</el-descriptions-item>
            <el-descriptions-item label="光照">{{ Math.round(store.latest.lightLux) }} lux</el-descriptions-item>
            <el-descriptions-item label="是否下雨">{{ store.latest.isRaining ? '是' : '否' }}</el-descriptions-item>
            <el-descriptions-item label="补光灯">{{ store.lightOn ? '开启' : '关闭' }}</el-descriptions-item>
          </el-descriptions>
        </div>
      </el-card>
      <!-- 异常报警列表（可滚动） -->
      <el-card shadow="never" style="margin-top:16px;">
        <AlertsPanel :alerts="store.alerts" />
      </el-card>
    </el-col>
  </el-row>
</template>

<style scoped></style>


