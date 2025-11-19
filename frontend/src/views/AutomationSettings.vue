<script setup>
import { onMounted, computed } from 'vue'
import { useGreenhouseStore } from '../stores/greenhouse'
import { ElMessage } from 'element-plus'
import {
  Setting as SettingIcon,
  Switch as SwitchIcon,
  Operation as OperationIcon,
  Sunny as SunnyIcon,
  Box as BoxIcon,
  InfoFilled as ThermometerIcon,
  SuccessFilled as WaterIcon,
  WarningFilled as WindPowerIcon,
  Warning as WarningIcon,
  Check as CheckIcon,
  Picture as PictureIcon
} from '@element-plus/icons-vue'

const store = useGreenhouseStore()

onMounted(async () => {
  // 异步加载自动化设置，不阻塞界面渲染
  try {
    await store.loadAutomationSettings()
  } catch (err) {
    console.error('加载自动化设置失败:', err)
  }
})

// 间隔时间选项配置（秒数和显示标签）
const intervalOptions = [
  { value: 60, label: '1分钟' },
  { value: 600, label: '10分钟' },
  { value: 1800, label: '30分钟' },
  ...Array.from({ length: 24 }, (_, i) => ({
    value: (i + 1) * 3600,
    label: `${i + 1}小时`
  }))
]

// 监听间隔时间变化
function onIntervalChange() {
  // 值已经直接存储在 store.automation.imageUploadIntervalSeconds 中
  // 可以在这里添加额外的逻辑，比如调用后端接口验证
}

// 格式化间隔时间显示
function formatIntervalTime(seconds) {
  if (seconds < 3600) {
    // 小于1小时，显示分钟
    const minutes = Math.floor(seconds / 60)
    return `${minutes}分钟`
  } else {
    // 大于等于1小时，显示小时和分钟
    const hours = Math.floor(seconds / 3600)
    const minutes = Math.floor((seconds % 3600) / 60)
    if (minutes === 0) {
      return `${hours}小时`
    }
    return `${hours}小时${minutes}分钟`
  }
}

async function saveAutomation() {
  try {
    await store.saveAutomationSettings()
    ElMessage.success('自动化设置已保存')
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

// 计算当前状态
const currentStatus = computed(() => {
  if (!store.latest) return null
  return {
    light: store.latest.lightLux || 0,
    soilMoisture: store.latest.soilMoisturePct || 0,
    temperature: store.latest.temperatureC || 0,
    humidity: store.latest.humidityPct || 0,
    oxygen: store.latest.oxygenPct || 0,
    co2: store.latest.co2Ppm || 0
  }
})

// 检查阈值状态
function getThresholdStatus(value, type, isHigh = true) {
  if (!currentStatus.value) return 'normal'
  const current = currentStatus.value[type]
  
  if (type === 'light') {
    return current < value ? 'warning' : 'normal'
  }
  if (type === 'soilMoisture') {
    return current < value ? 'warning' : 'normal'
  }
  if (type === 'temperature') {
    return isHigh ? (current > value ? 'danger' : 'normal') : (current < value ? 'danger' : 'normal')
  }
  if (type === 'humidity') {
    return isHigh ? (current > value ? 'danger' : 'normal') : (current < value ? 'danger' : 'normal')
  }
  if (type === 'oxygen') {
    return current < value ? 'danger' : 'normal'
  }
  if (type === 'co2') {
    return current > value ? 'danger' : 'normal'
  }
  return 'normal'
}
</script>

<template>
  <div class="automation-container">
    <!-- 顶部标题区域 -->
    <div class="automation-header">
      <div class="header-content">
        <div class="header-title">
          <el-icon class="title-icon"><SettingIcon /></el-icon>
          <h2>智能自动化控制</h2>
        </div>
        <el-button 
          type="primary" 
          size="large" 
          :icon="CheckIcon"
          @click="saveAutomation"
          class="save-button"
        >
          保存设置
        </el-button>
      </div>
    </div>

    <el-row :gutter="20">
      <!-- 左侧：自动控制开关 -->
      <el-col :md="12" :xs="24">
        <el-card class="control-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <el-icon><SwitchIcon /></el-icon>
              <span>自动控制开关</span>
            </div>
          </template>
          
          <div class="switch-group">
            <div class="switch-item">
              <div class="switch-info">
                <div class="switch-icon-wrapper" :class="{ active: store.automation.autoLightEnabled }">
                  <el-icon class="switch-icon"><SunnyIcon /></el-icon>
                </div>
                <div class="switch-content">
                  <div class="switch-title">自动补光</div>
                  <div class="switch-desc">当光照低于阈值时自动开启补光灯</div>
                </div>
              </div>
              <el-switch 
                v-model="store.automation.autoLightEnabled"
                size="large"
                active-color="#10b981"
                inactive-color="#e5e7eb"
              />
            </div>

            <div class="switch-item">
              <div class="switch-info">
                <div class="switch-icon-wrapper" :class="{ active: store.automation.autoPumpEnabled }">
                  <el-icon class="switch-icon"><WaterIcon /></el-icon>
                </div>
                <div class="switch-content">
                  <div class="switch-title">自动抽水</div>
                  <div class="switch-desc">当土壤湿度低于阈值时自动抽水</div>
                </div>
              </div>
              <el-switch 
                v-model="store.automation.autoPumpEnabled"
                size="large"
                active-color="#10b981"
                inactive-color="#e5e7eb"
              />
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：当前状态 -->
      <el-col :md="12" :xs="24">
        <el-card class="status-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <el-icon><OperationIcon /></el-icon>
              <span>当前传感器状态</span>
            </div>
          </template>
          
          <div v-if="currentStatus" class="status-grid">
            <div class="status-item">
              <div class="status-icon" style="background: linear-gradient(135deg, #10b981 0%, #34d399 100%);">
                <el-icon><SunnyIcon /></el-icon>
              </div>
              <div class="status-content">
                <div class="status-label">光照</div>
                <div class="status-value">{{ currentStatus.light }} lux</div>
              </div>
            </div>
            <div class="status-item">
              <div class="status-icon" style="background: linear-gradient(135deg, #3b82f6 0%, #60a5fa 100%);">
                <el-icon><BoxIcon /></el-icon>
              </div>
              <div class="status-content">
                <div class="status-label">土壤湿度</div>
                <div class="status-value">{{ currentStatus.soilMoisture }}%</div>
              </div>
            </div>
            <div class="status-item">
              <div class="status-icon" style="background: linear-gradient(135deg, #ef4444 0%, #f87171 100%);">
                <el-icon><ThermometerIcon /></el-icon>
              </div>
              <div class="status-content">
                <div class="status-label">温度</div>
                <div class="status-value">{{ currentStatus.temperature.toFixed(1) }}°C</div>
              </div>
            </div>
            <div class="status-item">
              <div class="status-icon" style="background: linear-gradient(135deg, #8b5cf6 0%, #a78bfa 100%);">
                <el-icon><WaterIcon /></el-icon>
              </div>
              <div class="status-content">
                <div class="status-label">湿度</div>
                <div class="status-value">{{ currentStatus.humidity }}%</div>
              </div>
            </div>
          </div>
          <div v-else class="empty-status">
            <el-empty description="暂无数据" :image-size="80" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 阈值设置区域 -->
    <el-card class="threshold-card" shadow="hover" style="margin-top: 20px;">
      <template #header>
        <div class="card-header">
          <el-icon><WarningIcon /></el-icon>
          <span>阈值设置</span>
        </div>
      </template>

      <el-row :gutter="20">
        <!-- 光照阈值 -->
        <el-col :md="12" :xs="24">
          <div class="threshold-item" :class="getThresholdStatus(store.automation.lightLuxThreshold, 'light')">
            <div class="threshold-header">
              <div class="threshold-icon-wrapper">
                <el-icon class="threshold-icon"><SunnyIcon /></el-icon>
              </div>
              <div class="threshold-info">
                <div class="threshold-title">光照阈值</div>
                <div class="threshold-desc">低于此值自动开灯</div>
              </div>
            </div>
            <div class="threshold-input">
              <el-input-number 
                v-model="store.automation.lightLuxThreshold" 
                :min="0" 
                :step="500"
                size="large"
                style="width: 100%;"
              />
              <span class="threshold-unit">lux</span>
            </div>
            <div v-if="currentStatus" class="threshold-status">
              <span class="status-text">当前: {{ currentStatus.light }} lux</span>
              <el-tag 
                :type="getThresholdStatus(store.automation.lightLuxThreshold, 'light') === 'warning' ? 'warning' : 'success'"
                size="small"
              >
                {{ getThresholdStatus(store.automation.lightLuxThreshold, 'light') === 'warning' ? '需开灯' : '正常' }}
              </el-tag>
            </div>
          </div>
        </el-col>

        <!-- 土壤湿度阈值 -->
        <el-col :md="12" :xs="24">
          <div class="threshold-item" :class="getThresholdStatus(store.automation.soilMoistureLowThreshold, 'soilMoisture')">
            <div class="threshold-header">
              <div class="threshold-icon-wrapper">
                <el-icon class="threshold-icon"><BoxIcon /></el-icon>
              </div>
              <div class="threshold-info">
                <div class="threshold-title">土壤湿度低阈值</div>
                <div class="threshold-desc">低于此值自动抽水</div>
              </div>
            </div>
            <div class="threshold-input">
              <el-input-number 
                v-model="store.automation.soilMoistureLowThreshold" 
                :min="0" 
                :max="100" 
                :step="1"
                size="large"
                style="width: 100%;"
              />
              <span class="threshold-unit">%</span>
            </div>
            <div v-if="currentStatus" class="threshold-status">
              <span class="status-text">当前: {{ currentStatus.soilMoisture }}%</span>
              <el-tag 
                :type="getThresholdStatus(store.automation.soilMoistureLowThreshold, 'soilMoisture') === 'warning' ? 'warning' : 'success'"
                size="small"
              >
                {{ getThresholdStatus(store.automation.soilMoistureLowThreshold, 'soilMoisture') === 'warning' ? '需抽水' : '正常' }}
              </el-tag>
            </div>
          </div>
        </el-col>

        <!-- 温度高阈值 -->
        <el-col :md="12" :xs="24">
          <div class="threshold-item" :class="getThresholdStatus(store.automation.temperatureHighThreshold, 'temperature', true)">
            <div class="threshold-header">
              <div class="threshold-icon-wrapper">
                <el-icon class="threshold-icon"><ThermometerIcon /></el-icon>
              </div>
              <div class="threshold-info">
                <div class="threshold-title">温度高阈值</div>
                <div class="threshold-desc">高于此值报警</div>
              </div>
            </div>
            <div class="threshold-input">
              <el-input-number 
                v-model="store.automation.temperatureHighThreshold" 
                :min="-50" 
                :max="100" 
                :step="1"
                size="large"
                style="width: 100%;"
              />
              <span class="threshold-unit">°C</span>
            </div>
            <div v-if="currentStatus" class="threshold-status">
              <span class="status-text">当前: {{ currentStatus.temperature.toFixed(1) }}°C</span>
              <el-tag 
                :type="getThresholdStatus(store.automation.temperatureHighThreshold, 'temperature', true) === 'danger' ? 'danger' : 'success'"
                size="small"
              >
                {{ getThresholdStatus(store.automation.temperatureHighThreshold, 'temperature', true) === 'danger' ? '超限' : '正常' }}
              </el-tag>
            </div>
          </div>
        </el-col>

        <!-- 温度低阈值 -->
        <el-col :md="12" :xs="24">
          <div class="threshold-item" :class="getThresholdStatus(store.automation.temperatureLowThreshold, 'temperature', false)">
            <div class="threshold-header">
              <div class="threshold-icon-wrapper">
                <el-icon class="threshold-icon"><ThermometerIcon /></el-icon>
              </div>
              <div class="threshold-info">
                <div class="threshold-title">温度低阈值</div>
                <div class="threshold-desc">低于此值报警</div>
              </div>
            </div>
            <div class="threshold-input">
              <el-input-number 
                v-model="store.automation.temperatureLowThreshold" 
                :min="-50" 
                :max="100" 
                :step="1"
                size="large"
                style="width: 100%;"
              />
              <span class="threshold-unit">°C</span>
            </div>
            <div v-if="currentStatus" class="threshold-status">
              <span class="status-text">当前: {{ currentStatus.temperature.toFixed(1) }}°C</span>
              <el-tag 
                :type="getThresholdStatus(store.automation.temperatureLowThreshold, 'temperature', false) === 'danger' ? 'danger' : 'success'"
                size="small"
              >
                {{ getThresholdStatus(store.automation.temperatureLowThreshold, 'temperature', false) === 'danger' ? '超限' : '正常' }}
              </el-tag>
            </div>
          </div>
        </el-col>

        <!-- 湿度高阈值 -->
        <el-col :md="12" :xs="24">
          <div class="threshold-item" :class="getThresholdStatus(store.automation.humidityHighThreshold, 'humidity', true)">
            <div class="threshold-header">
              <div class="threshold-icon-wrapper">
                <el-icon class="threshold-icon"><WaterIcon /></el-icon>
              </div>
              <div class="threshold-info">
                <div class="threshold-title">湿度高阈值</div>
                <div class="threshold-desc">高于此值报警</div>
              </div>
            </div>
            <div class="threshold-input">
              <el-input-number 
                v-model="store.automation.humidityHighThreshold" 
                :min="0" 
                :max="100" 
                :step="1"
                size="large"
                style="width: 100%;"
              />
              <span class="threshold-unit">%</span>
            </div>
            <div v-if="currentStatus" class="threshold-status">
              <span class="status-text">当前: {{ currentStatus.humidity }}%</span>
              <el-tag 
                :type="getThresholdStatus(store.automation.humidityHighThreshold, 'humidity', true) === 'danger' ? 'danger' : 'success'"
                size="small"
              >
                {{ getThresholdStatus(store.automation.humidityHighThreshold, 'humidity', true) === 'danger' ? '超限' : '正常' }}
              </el-tag>
            </div>
          </div>
        </el-col>

        <!-- 湿度低阈值 -->
        <el-col :md="12" :xs="24">
          <div class="threshold-item" :class="getThresholdStatus(store.automation.humidityLowThreshold, 'humidity', false)">
            <div class="threshold-header">
              <div class="threshold-icon-wrapper">
                <el-icon class="threshold-icon"><WaterIcon /></el-icon>
              </div>
              <div class="threshold-info">
                <div class="threshold-title">湿度低阈值</div>
                <div class="threshold-desc">低于此值报警</div>
              </div>
            </div>
            <div class="threshold-input">
              <el-input-number 
                v-model="store.automation.humidityLowThreshold" 
                :min="0" 
                :max="100" 
                :step="1"
                size="large"
                style="width: 100%;"
              />
              <span class="threshold-unit">%</span>
            </div>
            <div v-if="currentStatus" class="threshold-status">
              <span class="status-text">当前: {{ currentStatus.humidity }}%</span>
              <el-tag 
                :type="getThresholdStatus(store.automation.humidityLowThreshold, 'humidity', false) === 'danger' ? 'danger' : 'success'"
                size="small"
              >
                {{ getThresholdStatus(store.automation.humidityLowThreshold, 'humidity', false) === 'danger' ? '超限' : '正常' }}
              </el-tag>
            </div>
          </div>
        </el-col>

        <!-- 氧气低阈值 -->
        <el-col :md="12" :xs="24">
          <div class="threshold-item" :class="getThresholdStatus(store.automation.oxygenLowThreshold, 'oxygen')">
            <div class="threshold-header">
              <div class="threshold-icon-wrapper">
                <el-icon class="threshold-icon"><WindPowerIcon /></el-icon>
              </div>
              <div class="threshold-info">
                <div class="threshold-title">氧气含量低阈值</div>
                <div class="threshold-desc">低于此值报警</div>
              </div>
            </div>
            <div class="threshold-input">
              <el-input-number 
                v-model="store.automation.oxygenLowThreshold" 
                :min="0" 
                :max="100" 
                :step="0.1" 
                :precision="1"
                size="large"
                style="width: 100%;"
              />
              <span class="threshold-unit">%</span>
            </div>
            <div v-if="currentStatus" class="threshold-status">
              <span class="status-text">当前: {{ currentStatus.oxygen.toFixed(1) }}%</span>
              <el-tag 
                :type="getThresholdStatus(store.automation.oxygenLowThreshold, 'oxygen') === 'danger' ? 'danger' : 'success'"
                size="small"
              >
                {{ getThresholdStatus(store.automation.oxygenLowThreshold, 'oxygen') === 'danger' ? '超限' : '正常' }}
              </el-tag>
            </div>
          </div>
        </el-col>

        <!-- 二氧化碳高阈值 -->
        <el-col :md="12" :xs="24">
          <div class="threshold-item" :class="getThresholdStatus(store.automation.co2HighThreshold, 'co2')">
            <div class="threshold-header">
              <div class="threshold-icon-wrapper">
                <el-icon class="threshold-icon"><WindPowerIcon /></el-icon>
              </div>
              <div class="threshold-info">
                <div class="threshold-title">二氧化碳高阈值</div>
                <div class="threshold-desc">高于此值报警</div>
              </div>
            </div>
            <div class="threshold-input">
              <el-input-number 
                v-model="store.automation.co2HighThreshold" 
                :min="0" 
                :max="10000" 
                :step="50"
                size="large"
                style="width: 100%;"
              />
              <span class="threshold-unit">ppm</span>
            </div>
            <div v-if="currentStatus" class="threshold-status">
              <span class="status-text">当前: {{ currentStatus.co2 }} ppm</span>
              <el-tag 
                :type="getThresholdStatus(store.automation.co2HighThreshold, 'co2') === 'danger' ? 'danger' : 'success'"
                size="small"
              >
                {{ getThresholdStatus(store.automation.co2HighThreshold, 'co2') === 'danger' ? '超限' : '正常' }}
              </el-tag>
            </div>
          </div>
        </el-col>

        <!-- 图片上传间隔时间 -->
        <el-col :md="12" :xs="24">
          <div class="threshold-item">
            <div class="threshold-header">
              <div class="threshold-icon-wrapper">
                <el-icon class="threshold-icon"><PictureIcon /></el-icon>
              </div>
              <div class="threshold-info">
                <div class="threshold-title">图片上传间隔时间</div>
                <div class="threshold-desc">设置图片自动上传的时间间隔</div>
              </div>
            </div>
            <div class="threshold-input">
              <el-select 
                v-model="store.automation.imageUploadIntervalSeconds" 
                @change="onIntervalChange"
                size="large"
                style="width: 100%;"
              >
                <el-option 
                  v-for="option in intervalOptions" 
                  :key="option.value" 
                  :label="option.label" 
                  :value="option.value"
                />
              </el-select>
            </div>
            <div class="threshold-status">
              <span class="status-text">对应秒数: {{ store.automation.imageUploadIntervalSeconds }} 秒</span>
              <el-tag type="info" size="small">
                {{ formatIntervalTime(store.automation.imageUploadIntervalSeconds) }}
              </el-tag>
            </div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 提示信息 -->
    <el-alert
      title="当传感器数据超出阈值范围时，系统将自动触发相应的控制操作或报警。"
      type="info"
      :closable="false"
      show-icon
      class="info-alert"
    />
  </div>
</template>

<style scoped>
.automation-container {
  padding: 0;
}

.automation-header {
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
  gap: 16px;
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

.save-button {
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.4);
}

.control-card,
.status-card,
.threshold-card {
  border-radius: 12px;
  border: none;
  transition: all 0.3s ease;
}

.control-card:hover,
.status-card:hover,
.threshold-card:hover {
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

.switch-group {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.switch-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  background: linear-gradient(to right, #f9fafb 0%, #ffffff 100%);
  border-radius: 12px;
  border: 2px solid #e5e7eb;
  transition: all 0.3s ease;
}

.switch-item:hover {
  border-color: #6366f1;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.1);
}

.switch-info {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: 1;
}

.switch-icon-wrapper {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  background: linear-gradient(135deg, #e5e7eb 0%, #d1d5db 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.switch-icon-wrapper.active {
  background: linear-gradient(135deg, #10b981 0%, #34d399 100%);
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.3);
}

.switch-icon {
  font-size: 24px;
  color: #6b7280;
  transition: color 0.3s ease;
}

.switch-icon-wrapper.active .switch-icon {
  color: white;
}

.switch-content {
  flex: 1;
}

.switch-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 4px;
}

.switch-desc {
  font-size: 13px;
  color: #6b7280;
  line-height: 1.4;
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.status-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: linear-gradient(to right, #f9fafb 0%, #ffffff 100%);
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  transition: all 0.3s ease;
}

.status-item:hover {
  border-color: #6366f1;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.1);
  transform: translateY(-2px);
}

.status-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.status-content {
  flex: 1;
  min-width: 0;
}

.status-label {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 4px;
}

.status-value {
  font-size: 18px;
  font-weight: 700;
  color: #1f2937;
}

.empty-status {
  padding: 40px 0;
}

.threshold-item {
  padding: 20px;
  background: linear-gradient(to bottom, #ffffff 0%, #f9fafb 100%);
  border-radius: 12px;
  border: 2px solid #e5e7eb;
  margin-bottom: 20px;
  transition: all 0.3s ease;
}

.threshold-item:hover {
  border-color: #6366f1;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.1);
  transform: translateY(-2px);
}

.threshold-item.warning {
  border-color: #f59e0b;
  background: linear-gradient(to bottom, #fffbeb 0%, #fef3c7 100%);
}

.threshold-item.danger {
  border-color: #ef4444;
  background: linear-gradient(to bottom, #fef2f2 0%, #fee2e2 100%);
}

.threshold-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.threshold-icon-wrapper {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: linear-gradient(135deg, #6366f1 0%, #818cf8 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.threshold-icon {
  font-size: 20px;
  color: white;
}

.threshold-info {
  flex: 1;
}

.threshold-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 4px;
}

.threshold-desc {
  font-size: 12px;
  color: #6b7280;
}

.threshold-input {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.threshold-unit {
  font-size: 14px;
  color: #6b7280;
  font-weight: 500;
  min-width: 40px;
}

.threshold-status {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #e5e7eb;
}

.status-text {
  font-size: 13px;
  color: #6b7280;
}

.info-alert {
  margin-top: 20px;
  border-radius: 12px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .header-content {
    flex-direction: column;
    align-items: flex-start;
  }

  .save-button {
    width: 100%;
  }

  .status-grid {
    grid-template-columns: 1fr;
  }

  .switch-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
}
</style>
