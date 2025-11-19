<script setup>
import { ref, onMounted, computed } from 'vue'
import { useGreenhouseStore } from '../stores/greenhouse'
import { ElMessage } from 'element-plus'
import {
  Setting as SettingIcon,
  Switch as SwitchIcon,
  Message as MessageIcon,
  Clock as ClockIcon,
  SuccessFilled as SuccessIcon,
  WarningFilled as WarningIcon,
  CircleCloseFilled as ErrorIcon,
  Document as DocumentIcon,
  Operation as OperationIcon
} from '@element-plus/icons-vue'

const store = useGreenhouseStore()

const config = ref({
  enabled: false,
  emailEnabled: true,
  emailAddress: '',
  checkIntervalMinutes: 10,
  waterControlEnabled: true,
  lightControlEnabled: true,
  recipeExecutionEnabled: true
})

const logs = ref([])
const stats = ref({
  totalExecutions: 0,
  successCount: 0,
  failedCount: 0,
  partialCount: 0,
  successRate: 0
})

const loading = ref(false)

onMounted(() => {
  loadConfig()
  loadLogs()
  loadStats()
})

async function loadConfig() {
  try {
    const data = await store.getAiHostingConfig()
    if (data) {
      config.value = data
    }
  } catch (error) {
    console.error('加载配置失败:', error)
  }
}

async function loadLogs() {
  try {
    const data = await store.getAiHostingLogs(50)
    if (data && Array.isArray(data)) {
      logs.value = data
    }
  } catch (error) {
    console.error('加载日志失败:', error)
  }
}

async function loadStats() {
  try {
    const data = await store.getAiHostingStats()
    if (data) {
      stats.value = data
    }
  } catch (error) {
    console.error('加载统计失败:', error)
  }
}

async function saveConfig() {
  loading.value = true
  try {
    await store.updateAiHostingConfig(config.value)
    ElMessage.success('配置已保存')
    await loadConfig()
  } catch (error) {
    ElMessage.error('保存失败: ' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

function getStatusIcon(status) {
  if (status === 'success') return SuccessIcon
  if (status === 'failed') return ErrorIcon
  return WarningIcon
}

function getStatusColor(status) {
  if (status === 'success') return '#10b981'
  if (status === 'failed') return '#ef4444'
  return '#f59e0b'
}

function getStatusText(status) {
  if (status === 'success') return '成功'
  if (status === 'failed') return '失败'
  return '部分成功'
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

function parseJson(str) {
  try {
    return JSON.parse(str)
  } catch {
    return []
  }
}
</script>

<template>
  <div class="ai-hosting-container">
    <!-- 顶部标题区域 -->
    <div class="hosting-header">
      <div class="header-content">
        <div class="header-title">
          <el-icon class="title-icon"><OperationIcon /></el-icon>
          <h2>AI自动托管管理</h2>
        </div>
        <div class="header-stats">
          <div class="stat-item">
            <span class="stat-label">总执行次数</span>
            <span class="stat-value">{{ stats.totalExecutions }}</span>
          </div>
          <div class="stat-item success">
            <span class="stat-label">成功</span>
            <span class="stat-value">{{ stats.successCount }}</span>
          </div>
          <div class="stat-item" :class="stats.failedCount > 0 ? 'error' : ''">
            <span class="stat-label">失败</span>
            <span class="stat-value">{{ stats.failedCount }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">成功率</span>
            <span class="stat-value">{{ stats.successRate.toFixed(1) }}%</span>
          </div>
        </div>
      </div>
    </div>

    <el-row :gutter="20">
      <!-- 左侧：配置区域 -->
      <el-col :md="12" :xs="24">
        <el-card class="config-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <el-icon><SettingIcon /></el-icon>
              <span>AI托管配置</span>
            </div>
          </template>

          <div class="config-form">
            <!-- 启用开关 -->
            <div class="config-item">
              <div class="config-label">
                <el-icon><SwitchIcon /></el-icon>
                <span>启用AI自动托管</span>
              </div>
              <el-switch
                v-model="config.enabled"
                size="large"
                active-color="#10b981"
                inactive-color="#e5e7eb"
              />
            </div>

            <el-divider />

            <!-- 邮件配置 -->
            <div class="config-section">
              <div class="section-title">
                <el-icon><MessageIcon /></el-icon>
                <span>邮件报警设置</span>
              </div>
              <div class="config-item">
                <div class="config-label">启用邮件报警</div>
                <el-switch
                  v-model="config.emailEnabled"
                  size="large"
                  active-color="#10b981"
                  inactive-color="#e5e7eb"
                />
              </div>
              <div class="config-item">
                <div class="config-label">接收邮箱地址</div>
                <el-input
                  v-model="config.emailAddress"
                  placeholder="请输入邮箱地址"
                  :disabled="!config.emailEnabled"
                />
              </div>
            </div>

            <el-divider />

            <!-- 控制功能开关 -->
            <div class="config-section">
              <div class="section-title">
                <el-icon><OperationIcon /></el-icon>
                <span>控制功能</span>
              </div>
              <div class="config-item">
                <div class="config-label">自动控制水</div>
                <el-switch
                  v-model="config.waterControlEnabled"
                  size="large"
                  active-color="#10b981"
                  inactive-color="#e5e7eb"
                  :disabled="!config.enabled"
                />
              </div>
              <div class="config-item">
                <div class="config-label">自动控制补光</div>
                <el-switch
                  v-model="config.lightControlEnabled"
                  size="large"
                  active-color="#10b981"
                  inactive-color="#e5e7eb"
                  :disabled="!config.enabled"
                />
              </div>
              <div class="config-item">
                <div class="config-label">自动执行土壤配方</div>
                <el-switch
                  v-model="config.recipeExecutionEnabled"
                  size="large"
                  active-color="#10b981"
                  inactive-color="#e5e7eb"
                  :disabled="!config.enabled"
                />
              </div>
            </div>

            <el-divider />

            <!-- 执行间隔 -->
            <div class="config-item">
              <div class="config-label">
                <el-icon><ClockIcon /></el-icon>
                <span>检查间隔（分钟）</span>
              </div>
              <el-input-number
                v-model="config.checkIntervalMinutes"
                :min="1"
                :max="60"
                :disabled="!config.enabled"
              />
            </div>

            <!-- 保存按钮 -->
            <div class="config-actions">
              <el-button
                type="primary"
                size="large"
                :loading="loading"
                @click="saveConfig"
                style="width: 100%;"
              >
                保存配置
              </el-button>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：执行日志 -->
      <el-col :md="12" :xs="24">
        <el-card class="logs-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <el-icon><DocumentIcon /></el-icon>
              <span>执行日志</span>
              <el-button
                text
                size="small"
                @click="loadLogs"
                style="margin-left: auto;"
              >
                刷新
              </el-button>
            </div>
          </template>

          <div class="logs-list">
            <div
              v-for="log in logs"
              :key="log.id"
              class="log-item"
            >
              <div class="log-header">
                <div class="log-status">
                  <el-icon :style="{ color: getStatusColor(log.status) }">
                    <component :is="getStatusIcon(log.status)" />
                  </el-icon>
                  <span :style="{ color: getStatusColor(log.status) }">
                    {{ getStatusText(log.status) }}
                  </span>
                </div>
                <div class="log-time">
                  {{ formatDate(log.executionTime) }}
                </div>
              </div>

              <div v-if="log.executionDurationMs" class="log-duration">
                耗时: {{ log.executionDurationMs }}ms
              </div>

              <div v-if="log.actionsTaken" class="log-actions">
                <div class="log-section-title">执行的操作:</div>
                <ul>
                  <li v-for="(action, idx) in parseJson(log.actionsTaken)" :key="idx">
                    {{ action }}
                  </li>
                </ul>
              </div>

              <div v-if="log.issuesDetected" class="log-issues">
                <div class="log-section-title">检测到的问题:</div>
                <ul>
                  <li v-for="(issue, idx) in parseJson(log.issuesDetected)" :key="idx">
                    {{ issue }}
                  </li>
                </ul>
              </div>

              <div v-if="log.emailSent" class="log-email">
                <el-icon><MessageIcon /></el-icon>
                <span>已发送邮件报警</span>
              </div>

              <div v-if="log.errorMessage" class="log-error">
                <el-icon><ErrorIcon /></el-icon>
                <span>{{ log.errorMessage }}</span>
              </div>
            </div>

            <div v-if="logs.length === 0" class="empty-logs">
              <el-empty description="暂无执行日志" :image-size="80" />
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 提示信息 -->
    <el-alert
      title="AI自动托管功能每10分钟自动执行一次，检查传感器数据并执行相应的控制操作。如果检测到问题，将自动发送邮件报警。"
      type="info"
      :closable="false"
      show-icon
      class="info-alert"
    />
  </div>
</template>

<style scoped>
.ai-hosting-container {
  padding: 0;
}

.hosting-header {
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
  flex-wrap: wrap;
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

.stat-item.success {
  background: rgba(16, 185, 129, 0.2);
  border-color: rgba(16, 185, 129, 0.3);
}

.stat-item.error {
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

.config-card,
.logs-card {
  border-radius: 12px;
  border: none;
  transition: all 0.3s ease;
}

.config-card:hover,
.logs-card:hover {
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

.config-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.config-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 14px;
  color: #1f2937;
  margin-bottom: 8px;
}

.config-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  background: #f9fafb;
  border-radius: 8px;
}

.config-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  color: #374151;
}

.config-actions {
  margin-top: 8px;
}

.logs-list {
  max-height: 600px;
  overflow-y: auto;
}

.log-item {
  padding: 16px;
  background: #f9fafb;
  border-radius: 8px;
  margin-bottom: 12px;
  border-left: 3px solid #e5e7eb;
}

.log-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.log-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
}

.log-time {
  font-size: 12px;
  color: #6b7280;
}

.log-duration {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 8px;
}

.log-actions,
.log-issues {
  margin-top: 8px;
}

.log-section-title {
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 4px;
}

.log-actions ul,
.log-issues ul {
  margin: 0;
  padding-left: 20px;
  font-size: 13px;
  color: #6b7280;
}

.log-email,
.log-error {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  font-size: 12px;
  color: #6b7280;
}

.log-error {
  color: #ef4444;
}

.empty-logs {
  padding: 40px 0;
  text-align: center;
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

  .header-stats {
    width: 100%;
    justify-content: space-around;
  }

  .config-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
}
</style>

