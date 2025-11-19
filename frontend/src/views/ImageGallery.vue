<script setup>
import { computed, ref, onMounted } from 'vue'
import { useGreenhouseStore } from '../stores/greenhouse'
import { 
  Picture as PictureIcon,
  Calendar as CalendarIcon,
  Search as SearchIcon,
  Warning as WarningIcon,
  Refresh as RefreshIcon,
  Filter as FilterIcon,
  Clock as ClockIcon,

  Box as BoxIcon,
  Sunny as SunnyIcon,
} from '@element-plus/icons-vue'

const store = useGreenhouseStore()

// 选择日期后，点击"搜索"才应用
const pendingDate = ref(store.selectedDate)
const showAll = ref(true) // 是否显示所有图片（默认显示所有图片）

async function applySearch() {
  store.selectedDate = pendingDate.value
  // 加载指定日期的图片
  await store.loadImagesByDate(pendingDate.value)
  showAll.value = false
}

// 加载所有图片
async function loadAll() {
  await store.loadAllImages()
  showAll.value = true
}

// 加载异常图片
async function loadAbnormal() {
  await store.loadAbnormalImages()
  showAll.value = true
}

// 组件挂载时默认加载所有图片
onMounted(() => {
  const today = new Date().toISOString().split('T')[0]
  store.selectedDate = today
  pendingDate.value = today
  // 异步加载所有图片，不阻塞界面渲染
  store.loadAllImages().catch(err => console.error('加载图片失败:', err))
})

const images = computed(() => {
  // 如果显示所有图片
  if (showAll.value && Array.isArray(store.images) && store.images.length) {
    return store.images
  }
  // 否则显示指定日期的图片
  const date = store.selectedDate
  const byDate = store.imagesByDate[date]
  if (Array.isArray(byDate) && byDate.length) {
    return byDate
  }
  // 如果没有数据，返回空数组
  return []
})

// 图片预览相关 - 生成预览图片列表
const previewImageList = computed(() => {
  return images.value.map(img => img.url)
})

function isAbnormal(img) {
  // 如果数据库已标记为异常，直接使用
  if (img.isAbnormal) {
    return { 
      flag: true,
      tempAbnormal: false, 
      moistureAbnormal: false,
      reason: img.abnormalReason
    }
  }
  
  // 否则根据阈值判断
  const tempHigh = store.automation.temperatureHighThreshold ?? 35
  const tempAbnormal = (img.temperatureC ?? 0) > tempHigh
  
  const moistureThreshold = 10 // 土壤湿度低于10%才异常
  const moistureAbnormal = (img.soilMoisturePct ?? 0) < moistureThreshold
  
  return { 
    flag: tempAbnormal || moistureAbnormal,
    tempAbnormal, 
    moistureAbnormal,
    reason: tempAbnormal && moistureAbnormal ? '温度异常, 土壤湿度异常' : 
            tempAbnormal ? '温度异常' : 
            moistureAbnormal ? '土壤湿度异常' : null
  }
}

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

// 统计信息
const stats = computed(() => {
  const total = images.value.length
  const abnormal = images.value.filter(img => isAbnormal(img).flag).length
  return {
    total,
    abnormal,
    normal: total - abnormal
  }
})
</script>

<template>
  <div class="gallery-container">
    <!-- 顶部标题和统计 -->
    <div class="gallery-header">
      <div class="header-content">
        <div class="header-title">
          <el-icon class="title-icon"><PictureIcon /></el-icon>
          <h2>智能温室图片集</h2>
        </div>
        <div class="header-stats">
          <div class="stat-item">
            <span class="stat-label">总数</span>
            <span class="stat-value">{{ stats.total }}</span>
          </div>
          <div class="stat-item normal">
            <span class="stat-label">正常</span>
            <span class="stat-value">{{ stats.normal }}</span>
          </div>
          <div class="stat-item abnormal">
            <span class="stat-label">异常</span>
            <span class="stat-value">{{ stats.abnormal }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 搜索和筛选区域 -->
    <el-card class="filter-card" shadow="hover">
      <el-form inline class="filter-form">
        <el-form-item label="选择日期">
          <el-date-picker 
            v-model="pendingDate" 
            type="date" 
            value-format="YYYY-MM-DD" 
            placeholder="选择日期"
            :prefix-icon="CalendarIcon"
            style="width: 200px;"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="SearchIcon" @click="applySearch">
            按日期搜索
          </el-button>
        </el-form-item>
        <el-form-item>
          <el-button type="success" :icon="RefreshIcon" @click="loadAll">
            显示所有图片
          </el-button>
        </el-form-item>
        <el-form-item>
          <el-button type="warning" :icon="FilterIcon" @click="loadAbnormal">
            显示异常图片
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 图片网格 -->
    <div v-if="images.length === 0" class="empty-state">
      <el-empty description="暂无图片数据" :image-size="120">
        <el-button type="primary" @click="loadAll">加载图片</el-button>
      </el-empty>
    </div>

    <div v-else class="image-grid">
      <div 
        v-for="(img, idx) in images" 
        :key="idx" 
        class="image-card-wrapper"
        :class="{ 'abnormal-card': isAbnormal(img).flag }"
      >
        <el-card class="image-card" shadow="hover">
          <div class="image-container">
            <el-image
              :src="img.url"
              :preview-src-list="previewImageList"
              :initial-index="idx"
              fit="cover"
              class="gallery-image"
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
            
            <!-- 异常标签 -->
            <div v-if="isAbnormal(img).flag" class="abnormal-badge">
              <el-icon><WarningIcon /></el-icon>
              <span>异常</span>
            </div>

            <!-- 时间标签 -->
            <div class="time-badge">
              <el-icon><ClockIcon /></el-icon>
              <span>{{ new Date(img.time).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }) }}</span>
            </div>

            <!-- 异常信息提示 -->
            <div v-if="isAbnormal(img).flag && isAbnormal(img).reason" class="abnormal-tooltip">
              <div class="tooltip-content">
                <el-icon class="tooltip-icon"><WarningIcon /></el-icon>
                <div class="tooltip-text">{{ isAbnormal(img).reason }}</div>
              </div>
            </div>
          </div>

          <!-- 数据信息 -->
          <div class="image-data">
            <div class="data-row">
              <div class="data-item">
                <el-icon class="data-icon" :style="{ color: getStatusColor(img.temperatureC ?? 0, 'temperature') }">
                  <ThermometerIcon />
                </el-icon>
                <div class="data-content">
                  <span class="data-label">温度</span>
                  <span class="data-value" :style="{ color: getStatusColor(img.temperatureC ?? 0, 'temperature') }">
                    {{ (img.temperatureC ?? 0).toFixed(1) }}°C
                  </span>
                </div>
              </div>
              <div class="data-item">
                <el-icon class="data-icon" :style="{ color: getStatusColor(img.humidityPct ?? 0, 'humidity') }">
                  <WaterIcon />
                </el-icon>
                <div class="data-content">
                  <span class="data-label">湿度</span>
                  <span class="data-value" :style="{ color: getStatusColor(img.humidityPct ?? 0, 'humidity') }">
                    {{ Math.round(img.humidityPct ?? 0) }}%
                  </span>
                </div>
              </div>
            </div>
            <div class="data-row">
              <div class="data-item">
                <el-icon class="data-icon" :style="{ color: getStatusColor(img.soilMoisturePct ?? 0, 'soilMoisture') }">
                  <BoxIcon />
                </el-icon>
                <div class="data-content">
                  <span class="data-label">土壤湿度</span>
                  <span class="data-value" :style="{ color: getStatusColor(img.soilMoisturePct ?? 0, 'soilMoisture') }">
                    {{ Math.round(img.soilMoisturePct ?? 0) }}%
                  </span>
                </div>
              </div>
              <div class="data-item">
                <el-icon class="data-icon" style="color: #10b981;">
                  <SunnyIcon />
                </el-icon>
                <div class="data-content">
                  <span class="data-label">光照</span>
                  <span class="data-value">{{ Math.round(img.lightLux ?? 0) }} lux</span>
                </div>
              </div>
            </div>
            <div class="data-row">
              <div class="data-item">
                <el-icon class="data-icon" style="color: #6366f1;">
                  <CloudyIcon />
                </el-icon>
                <div class="data-content">
                  <span class="data-label">天气</span>
                  <el-tag :type="img.isRaining ? 'warning' : 'success'" size="small">
                    {{ img.isRaining ? '下雨' : '晴天' }}
                  </el-tag>
                </div>
              </div>
              <div class="data-item">
                <el-icon class="data-icon" style="color: #06b6d4;">
                  <WindPowerIcon />
                </el-icon>
                <div class="data-content">
                  <span class="data-label">O₂/CO₂</span>
                  <span class="data-value">{{ (img.oxygenPct ?? 0).toFixed(1) }}% / {{ img.co2Ppm ?? 0 }}ppm</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 异常详情 -->
          <div v-if="isAbnormal(img).flag && isAbnormal(img).reason" class="abnormal-detail">
            <div class="abnormal-detail-header">
              <el-icon><WarningIcon /></el-icon>
              <span>异常详情</span>
            </div>
            <div class="abnormal-detail-content">
              {{ isAbnormal(img).reason }}
            </div>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<style scoped>
.gallery-container {
  padding: 0;
}

.gallery-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 20px;
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.3);
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 20px;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 12px;
  color: white;
}

.title-icon {
  font-size: 32px;
}

.header-title h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: white;
}

.header-stats {
  display: flex;
  gap: 20px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px 20px;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.3);
}

.stat-item.normal {
  background: rgba(16, 185, 129, 0.2);
  border-color: rgba(16, 185, 129, 0.3);
}

.stat-item.abnormal {
  background: rgba(239, 68, 68, 0.2);
  border-color: rgba(239, 68, 68, 0.3);
}

.stat-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.8);
  margin-bottom: 4px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: white;
}

.filter-card {
  border-radius: 12px;
  margin-bottom: 20px;
  border: none;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.filter-form {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.image-card-wrapper {
  transition: transform 0.3s ease;
}

.image-card-wrapper:hover {
  transform: translateY(-4px);
}

.image-card-wrapper.abnormal-card {
  position: relative;
}

.image-card-wrapper.abnormal-card::before {
  content: '';
  position: absolute;
  top: -2px;
  left: -2px;
  right: -2px;
  bottom: -2px;
  background: linear-gradient(135deg, #ef4444, #f59e0b);
  border-radius: 12px;
  z-index: -1;
  opacity: 0.3;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% {
    opacity: 0.3;
  }
  50% {
    opacity: 0.5;
  }
}

.image-card {
  border-radius: 12px;
  border: none;
  overflow: hidden;
  transition: all 0.3s ease;
  background: white;
}

.image-card:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15) !important;
}

.image-container {
  position: relative;
  width: 100%;
  height: 240px;
  overflow: hidden;
  background: #f3f4f6;
}

.gallery-image {
  width: 100%;
  height: 100%;
  cursor: pointer;
  transition: transform 0.3s ease;
}

.gallery-image:hover {
  transform: scale(1.05);
}

.image-error {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f3f4f6 0%, #e5e7eb 100%);
  color: #9ca3af;
  gap: 8px;
}

.abnormal-badge {
  position: absolute;
  top: 12px;
  left: 12px;
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  color: white;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 4px;
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.4);
  z-index: 10;
  animation: shake 0.5s ease-in-out;
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-4px); }
  75% { transform: translateX(4px); }
}

.time-badge {
  position: absolute;
  bottom: 12px;
  left: 12px;
  background: rgba(0, 0, 0, 0.7);
  backdrop-filter: blur(10px);
  color: white;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
  z-index: 10;
}

.abnormal-tooltip {
  position: absolute;
  top: 12px;
  right: 12px;
  z-index: 10;
}

.tooltip-content {
  background: rgba(239, 68, 68, 0.95);
  backdrop-filter: blur(10px);
  color: white;
  padding: 8px 12px;
  border-radius: 8px;
  font-size: 11px;
  display: flex;
  align-items: center;
  gap: 6px;
  max-width: 200px;
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.3);
}

.tooltip-icon {
  font-size: 14px;
  flex-shrink: 0;
}

.tooltip-text {
  line-height: 1.4;
}

.image-data {
  padding: 16px;
  background: linear-gradient(to bottom, #fafafa 0%, #ffffff 100%);
}

.data-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 12px;
}

.data-row:last-child {
  margin-bottom: 0;
}

.data-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  background: white;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  transition: all 0.2s ease;
}

.data-item:hover {
  border-color: #6366f1;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.1);
}

.data-icon {
  font-size: 20px;
  flex-shrink: 0;
}

.data-content {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
  min-width: 0;
}

.data-label {
  font-size: 11px;
  color: #6b7280;
  line-height: 1.2;
}

.data-value {
  font-size: 13px;
  font-weight: 600;
  line-height: 1.2;
  color: #1f2937;
}

.abnormal-detail {
  margin-top: 12px;
  padding: 12px;
  background: linear-gradient(135deg, #fef2f2 0%, #fee2e2 100%);
  border-left: 3px solid #ef4444;
  border-radius: 8px;
}

.abnormal-detail-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #b91c1c;
  margin-bottom: 6px;
}

.abnormal-detail-content {
  font-size: 12px;
  color: #991b1b;
  line-height: 1.5;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .image-grid {
    grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
    gap: 16px;
  }

  .header-content {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-stats {
    width: 100%;
    justify-content: space-around;
  }

  .data-row {
    grid-template-columns: 1fr;
  }
}
</style>
