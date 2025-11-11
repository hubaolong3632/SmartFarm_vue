<script setup>
// 概览页面：执行日志图表 + 传感数据折线图 + 控制面板 + 当前状态/报警
import { onMounted } from 'vue'
import LineChart from '../components/LineChart.vue'
import AlertsPanel from '../components/AlertsPanel.vue'
import ControlsPanel from '../components/ControlsPanel.vue'
import { useGreenhouseStore } from '../stores/greenhouse'

const store = useGreenhouseStore()

onMounted(() => {
  store.startSimulation()
})
</script>

<template>
  <el-row :gutter="16" align="top">
    <el-col :md="16" :xs="24">
      <!-- 执行日志图表（最近24小时，每小时执行次数） -->
      <el-card shadow="never" style="margin-bottom:16px;">
        <template #header>执行日志（次数/小时，最近24小时）</template>
        <LineChart
          :points="store.executionsLast24"
          :series="[
            { label: '执行次数', color: '#6366f1', accessor: (p) => p.count },
          ]"
          :width="800"
          :height="220"
        />
      </el-card>
      <!-- 小时检测数据折线图（温度/湿度/光照/降雨） -->
      <el-card shadow="never">
        <template #header>小时检测折线图</template>
        <LineChart
          :points="store.hourly"
          :series="[
            { label: '温度(°C)', color: '#ef4444', accessor: (p) => p.temperatureC },
            { label: '土壤湿度(%)', color: '#3b82f6', accessor: (p) => p.soilMoisturePct },
            { label: '光照(lux)', color: '#10b981', accessor: (p) => p.lightLux },
            { label: '是否下雨(0/1)', color: '#f59e0b', accessor: (p) => p.isRaining },
          ]"
          :width="800"
          :height="320"
        />
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


