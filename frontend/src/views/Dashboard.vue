<script setup>
// 概览页面：执行日志图表 + 传感数据折线图 + 控制面板 + 当前状态/报警 + 最新图片
import { onMounted, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { 
  Box as BoxIcon,
  Sunny as SunnyIcon,
  Warning as WarningIcon,
  Operation as OperationIcon,
  DataLine as DataLineIcon,
  TrendCharts as TrendChartsIcon,
  Picture as PictureIcon,
  Clock as ClockIcon,
  Monitor as MonitorIcon,
  Bell as BellIcon,
  InfoFilled as Temperature,
  SuccessFilled as Water
} from '@element-plus/icons-vue'
import EChartLine from '../components/EChartLine.vue'
import AlertsPanel from '../components/AlertsPanel.vue'
import ControlsPanel from '../components/ControlsPanel.vue'
import { useGreenhouseStore } from '../stores/greenhouse'

const store = useGreenhouseStore()
const router = useRouter()

onMounted(() => {
  // 立即开始数据轮询，数据在后台加载
  store.startSimulation()
  // 异步加载数据，不阻塞界面渲染
  store.loadAllData().catch(err => console.error('加载数据失败:', err))
  store.loadExecutionsLast24().catch(err => console.error('加载执行日志失败:', err))
  store.loadLatestImage().catch(err => console.error('加载最新图片失败:', err))
})

onUnmounted(() => {
  // 停止数据轮询
  store.stopSimulation()
})

// 计算统计数据
const stats = computed(() => {
  if (!store.latest) return null
  return {
    temperature: store.latest.temperatureC?.toFixed(1) || '0.0',
    humidity: Math.round(store.latest.humidityPct || 0),
    soilMoisture: Math.round(store.latest.soilMoisturePct || 0),
    light: Math.round(store.latest.lightLux || 0),
    alerts: store.alerts.length,
    executions: store.executionsLast24.reduce((sum, item) => sum + (item.count || 0), 0)
  }
})

// 获取状态颜色
function getStatusColor(value, type) {
  if (type === 'temperature') {
    if (value > 35) return '#ef4444'
    if (value > 30) return '#f59e0b'
    return '#10b981'
  }
  if (type === 'humidity') {
    if (value < 30) return '#ef4444'
    if (value < 50) return '#f59e0b'
    return '#10b981'
  }
  if (type === 'soilMoisture') {
    if (value < 20) return '#ef4444'
    if (value < 40) return '#f59e0b'
    return '#10b981'
  }
  return '#6366f1'
}
</script>

<template>
  <div class="dashboard-container">
    <!-- 顶部统计卡片 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :xs="12" :sm="8" :md="4" v-if="stats">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" style="background: linear-gradient(135deg, #ef4444 0%, #f87171 100%);">
              <el-icon><Temperature /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value" :style="{ color: getStatusColor(parseFloat(stats.temperature), 'temperature') }">
                {{ stats.temperature }}°C
              </div>
              <div class="stat-label">温度</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4" v-if="stats">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" style="background: linear-gradient(135deg, #3b82f6 0%, #60a5fa 100%);">
              <el-icon><Water /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value" :style="{ color: getStatusColor(stats.humidity, 'humidity') }">
                {{ stats.humidity }}%
              </div>
              <div class="stat-label">湿度</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4" v-if="stats">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" style="background: linear-gradient(135deg, #8b5cf6 0%, #a78bfa 100%);">
              <el-icon><BoxIcon /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value" :style="{ color: getStatusColor(stats.soilMoisture, 'soilMoisture') }">
                {{ stats.soilMoisture }}%
              </div>
              <div class="stat-label">土壤湿度</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4" v-if="stats">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" style="background: linear-gradient(135deg, #10b981 0%, #34d399 100%);">
              <el-icon><SunnyIcon /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value" style="color: #10b981;">
                {{ stats.light }}
              </div>
              <div class="stat-label">光照 (lux)</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4" v-if="stats">
        <el-card class="stat-card" shadow="hover" @click="router.push('/gallery')" style="cursor: pointer;">
          <div class="stat-content">
            <div class="stat-icon" style="background: linear-gradient(135deg, #f59e0b 0%, #fbbf24 100%);">
              <el-icon><WarningIcon /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value" :style="{ color: stats.alerts > 0 ? '#ef4444' : '#10b981' }">
                {{ stats.alerts }}
              </div>
              <div class="stat-label">异常报警</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="4" v-if="stats">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" style="background: linear-gradient(135deg, #6366f1 0%, #818cf8 100%);">
              <el-icon><OperationIcon /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value" style="color: #6366f1;">
                {{ stats.executions }}
              </div>
              <div class="stat-label">今日执行</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="main-content">
      <!-- 左侧主要内容区 -->
      <el-col :md="16" :xs="24">
        <!-- 实时传感器数据折线图 -->
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <el-icon><TrendChartsIcon /></el-icon>
              <span>实时传感器数据（最新30条记录）</span>
              <span class="data-count">数据点数: {{ store.hourly.length }}/30</span>
            </div>
          </template>
          <el-row :gutter="16">
            <el-col :span="8">
              <EChartLine
                title="温度"
                :data="store.hourly"
                data-key="temperatureC"
                unit="°C"
                color="#ef4444"
                height="200px"
              />
            </el-col>
            <el-col :span="8">
              <EChartLine
                title="湿度"
                :data="store.hourly"
                data-key="humidityPct"
                unit="%"
                color="#8b5cf6"
                height="200px"
              />
            </el-col>
            <el-col :span="8">
              <EChartLine
                title="土壤湿度"
                :data="store.hourly"
                data-key="soilMoisturePct"
                unit="%"
                color="#3b82f6"
                height="200px"
              />
            </el-col>
          </el-row>
          <el-row :gutter="16" style="margin-top: 16px;">
            <el-col :span="8">
              <EChartLine
                title="光照强度"
                :data="store.hourly"
                data-key="lightLux"
                unit=" lux"
                color="#10b981"
                height="200px"
              />
            </el-col>
            <el-col :span="8">
              <EChartLine
                title="是否下雨"
                :data="store.hourly"
                data-key="isRaining"
                unit=""
                color="#f59e0b"
                height="200px"
              />
            </el-col>
            <el-col :span="8">
              <EChartLine
                title="氧气含量"
                :data="store.hourly"
                data-key="oxygenPct"
                unit="%"
                color="#06b6d4"
                height="200px"
              />
            </el-col>
          </el-row>
          <el-row :gutter="16" style="margin-top: 16px;">
            <el-col :span="8">
              <EChartLine
                title="二氧化碳"
                :data="store.hourly"
                data-key="co2Ppm"
                unit=" ppm"
                color="#ec4899"
                height="200px"
              />
            </el-col>
          </el-row>
        </el-card>

        <!-- 执行日志图表 -->
        <el-card class="chart-card" shadow="hover" style="margin-top: 16px;">
          <template #header>
            <div class="card-header">
              <el-icon><DataLineIcon /></el-icon>
              <span>执行日志（最近24小时）</span>
            </div>
          </template>
          <EChartLine
            title="执行次数"
            :data="store.executionsLast24"
            data-key="count"
            unit=" 次"
            color="#6366f1"
            height="240px"
          />
        </el-card>

        <!-- 控制面板 -->
        <el-card class="control-card" shadow="hover" style="margin-top: 16px;">
          <ControlsPanel
            :light-on="store.lightOn"
            :cleaning="store.cleaningInProgress"
            @clean="store.triggerCleaning"
            @toggle-light="store.toggleLight()"
          />
        </el-card>
      </el-col>

      <!-- 右侧信息区 -->
      <el-col :md="8" :xs="24">
        <!-- 最新图片 -->
        <el-card class="image-card" shadow="hover" v-if="store.latestImage">
          <template #header>
            <div class="card-header">
              <el-icon><PictureIcon /></el-icon>
              <span>最新图片</span>
              <el-button 
                text 
                type="primary" 
                size="small" 
                @click="router.push('/gallery')"
                style="margin-left: auto;"
              >
                查看全部
              </el-button>
            </div>
          </template>
          <div class="latest-image-container">
            <el-image
              :src="store.latestImage.url"
              fit="cover"
              class="latest-image"
              :preview-src-list="[store.latestImage.url]"
              :preview-teleported="true"
              lazy
            >
              <template #error>
                <div class="image-error">
                  <el-icon><PictureIcon /></el-icon>
                  <span>图片加载失败</span>
                </div>
              </template>
            </el-image>
            <div class="image-info">
              <div class="image-time">
                <el-icon><ClockIcon /></el-icon>
                {{ new Date(store.latestImage.time).toLocaleString('zh-CN') }}
              </div>
              <div v-if="store.latestImage.isAbnormal" class="image-badge abnormal">
                异常
              </div>
            </div>
          </div>
        </el-card>

        <!-- 异常报警列表 -->
        <el-card class="alerts-card" shadow="hover" style="margin-top: 16px;">
          <template #header>
            <div class="card-header">
              <el-icon><BellIcon /></el-icon>
              <span>异常报警</span>
              <el-badge :value="store.alerts.length" :hidden="store.alerts.length === 0" class="alert-badge" />
            </div>
          </template>
          <div class="alerts-content">
            <AlertsPanel :alerts="store.alerts" />
          </div>
        </el-card>

        <!-- 当前状态 -->
        <el-card class="status-card" shadow="hover" style="margin-top: 16px;">
          <template #header>
            <div class="card-header">
              <el-icon><MonitorIcon /></el-icon>
              <span>当前状态</span>
            </div>
          </template>
          <div v-if="store.latest" class="status-content">
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="温度">
                <span :style="{ color: getStatusColor(store.latest.temperatureC, 'temperature') }">
                  {{ store.latest.temperatureC.toFixed(1) }}°C
                </span>
              </el-descriptions-item>
              <el-descriptions-item label="湿度">
                <span :style="{ color: getStatusColor(store.latest.humidityPct || 0, 'humidity') }">
                  {{ Math.round(store.latest.humidityPct || 0) }}%
                </span>
              </el-descriptions-item>
              <el-descriptions-item label="土壤湿度">
                <span :style="{ color: getStatusColor(store.latest.soilMoisturePct, 'soilMoisture') }">
                  {{ Math.round(store.latest.soilMoisturePct) }}%
                </span>
              </el-descriptions-item>
              <el-descriptions-item label="光照">
                {{ Math.round(store.latest.lightLux) }} lux
              </el-descriptions-item>
              <el-descriptions-item label="是否下雨">
                <el-tag :type="store.latest.isRaining ? 'warning' : 'success'" size="small">
                  {{ store.latest.isRaining ? '是' : '否' }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="氧气含量">
                {{ (store.latest.oxygenPct || 0).toFixed(1) }}%
              </el-descriptions-item>
              <el-descriptions-item label="二氧化碳">
                {{ store.latest.co2Ppm || 0 }} ppm
              </el-descriptions-item>
              <el-descriptions-item label="补光灯">
                <el-tag :type="store.lightOn ? 'success' : 'info'" size="small">
                  {{ store.lightOn ? '开启' : '关闭' }}
                </el-tag>
              </el-descriptions-item>
            </el-descriptions>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.dashboard-container {
  padding: 0;
}

.stats-row {
  margin-bottom: 16px;
}

.stat-card {
  border-radius: 12px;
  border: none;
  transition: all 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12) !important;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 12px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.stat-info {
  flex: 1;
  min-width: 0;
}

.stat-value {
  font-size: 20px;
  font-weight: 700;
  line-height: 1.2;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 12px;
  color: #6b7280;
  line-height: 1.2;
}

.main-content {
  margin-top: 0;
}

.chart-card,
.control-card,
.image-card,
.status-card,
.alerts-card {
  border-radius: 12px;
  border: none;
  transition: all 0.3s ease;
}

.chart-card:hover,
.control-card:hover,
.image-card:hover,
.status-card:hover,
.alerts-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1) !important;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 15px;
}

.card-header .el-icon {
  font-size: 18px;
  color: #6366f1;
}

.data-count {
  margin-left: auto;
  font-size: 12px;
  color: #9ca3af;
  font-weight: normal;
}

.latest-image-container {
  position: relative;
  border-radius: 8px;
  overflow: hidden;
}

.latest-image {
  width: 100%;
  height: 240px;
  border-radius: 8px;
  cursor: pointer;
  transition: transform 0.3s ease;
}

.latest-image:hover {
  transform: scale(1.02);
}

.image-error {
  width: 100%;
  height: 240px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #f3f4f6;
  color: #9ca3af;
  gap: 8px;
}

.image-info {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.7), transparent);
  padding: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.image-time {
  color: white;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.image-badge {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
}

.image-badge.abnormal {
  background: #fee2e2;
  color: #b91c1c;
}

.status-content {
  padding: 4px 0;
}

.alert-badge {
  margin-left: auto;
}

.alerts-content {
  max-height: 300px;
  overflow: hidden;
}

.alerts-content :deep(.el-empty) {
  padding: 20px 0;
}

.alerts-content :deep(div[style*="max-height"]) {
  max-height: 280px !important;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .stat-value {
    font-size: 18px;
  }
  
  .stat-icon {
    width: 40px;
    height: 40px;
  }
  
  .stat-icon .el-icon {
    font-size: 20px;
  }
}
</style>
