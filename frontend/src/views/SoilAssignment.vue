<script setup>
import { computed, ref, reactive, watchEffect, onMounted } from 'vue'
import { useGreenhouseStore } from '../stores/greenhouse'
import { ElMessage } from 'element-plus'
import {
  Location as LocationIcon,
  Document as DocumentIcon,
  Clock as ClockIcon,
  Plus as PlusIcon,
  Delete as DeleteIcon,
  Operation as OperationIcon,
  Calendar as CalendarIcon,
  List as ListIcon,
  Refresh as RefreshIcon
} from '@element-plus/icons-vue'

const store = useGreenhouseStore()

onMounted(() => {
  // 异步加载相关数据，不阻塞界面渲染
  Promise.all([
    store.loadRecipes(),
    store.loadPlotAssignments(),
    store.loadPlotSchedules(),
    store.loadExecutionLogs(),
  ]).catch(err => console.error('加载数据失败:', err))
})

const plots = computed(() => Array.from({ length: store.numPlots }, (_, i) => i + 1))
const selectedRecipe = ref('')

// 每个地块独立的"分配执行次数"与"定时（时间/次数）"
const assignForm = reactive({}) // { [plot]: { times: number } }
const schedForm = reactive({}) // { [plot]: { time: 'HH:mm', times: number, scheduleType: string, dayOfWeek: number, scheduleDatetime: string } }
const plotRecipeSelect = reactive({}) // { [plot]: { id: string } }

// 时间选择优化：使用小时和分钟输入框
const timeInputs = reactive({}) // { [plot]: { hour: number, minute: number } }

function ensureAssign(plot) {
  if (!assignForm[plot]) assignForm[plot] = { times: 1 }
  return assignForm[plot]
}

function ensureSched(plot) {
  if (!schedForm[plot]) {
    schedForm[plot] = {
      time: '12:00',
      times: 1,
      scheduleType: 'daily',
      dayOfWeek: null,
      scheduleDatetime: ''
    }
  }
  return schedForm[plot]
}

function ensureTimeInput(plot) {
  if (!timeInputs[plot]) {
    const currentTime = ensureSched(plot).time || '12:00'
    const [hour, minute] = currentTime.split(':').map(Number)
    timeInputs[plot] = { hour: hour || 12, minute: minute || 0 }
  }
  return timeInputs[plot]
}

// 同步时间输入到表单
function syncTimeToForm(plot) {
  const timeInput = ensureTimeInput(plot)
  const hour = String(timeInput.hour || 0).padStart(2, '0')
  const minute = String(timeInput.minute || 0).padStart(2, '0')
  ensureSched(plot).time = `${hour}:${minute}`
}

function ensurePlotSelect(plot) {
  if (!plotRecipeSelect[plot]) {
    // 默认优先使用顶部选择的配方，否则使用当前已分配的配方
    const currentId = selectedRecipe.value || store.plotToRecipeId[plot] || (store.recipes[0] && store.recipes[0].id) || ''
    plotRecipeSelect[plot] = { id: currentId }
  }
  return plotRecipeSelect[plot]
}

// 默认选择第一个配方
watchEffect(() => {
  if (!selectedRecipe.value && store.recipes.length) {
    selectedRecipe.value = store.recipes[0].id
  }
})

function assign(plot) {
  const per = ensurePlotSelect(plot)
  if (!per.id) {
    ElMessage.warning('请选择配方')
    return
  }
  const a = ensureAssign(plot)
  store.assignRecipeToPlot(plot, per.id, a.times)
  ElMessage.success(`地块 ${plot} 配方分配成功`)
}

function addSchedule(plot) {
  const s = ensureSched(plot)
  const per = ensurePlotSelect(plot)
  if (!per.id) {
    ElMessage.warning('请选择配方')
    return
  }

  // 同步时间
  syncTimeToForm(plot)

  // 如果是每天或每周，需要时间
  if ((s.scheduleType === 'daily' || s.scheduleType === 'weekly') && !s.time) {
    ElMessage.warning('请选择执行时间')
    return
  }

  // 如果是每月，需要精确时间或时间
  if (s.scheduleType === 'monthly' && !s.scheduleDatetime && !s.time) {
    ElMessage.warning('请选择执行时间')
    return
  }

  if (s.scheduleType === 'weekly' && s.dayOfWeek === null) {
    ElMessage.warning('请选择周几执行')
    return
  }

  store.addSchedule(plot, per.id, s.time, s.times, s.scheduleType, s.dayOfWeek, s.scheduleDatetime)
  ElMessage.success('定时任务添加成功')

  // 重置表单
  schedForm[plot] = {
    time: '12:00',
    times: 1,
    scheduleType: 'daily',
    dayOfWeek: null,
    scheduleDatetime: ''
  }
  timeInputs[plot] = { hour: 12, minute: 0 }
}

function removeSchedule(plot, scheduleId) {
  store.removeSchedule(plot, scheduleId)
  ElMessage.success('定时任务删除成功')
}

function executeSchedule(scheduleId) {
  store.executeSchedule(scheduleId)
  ElMessage.success('任务执行成功')
}

// 获取地块当前分配的配方名称
function getCurrentRecipeName(plot) {
  const recipeId = store.plotToRecipeId[plot]
  if (!recipeId) return '未分配'
  const recipe = store.recipes.find(r => r.id === recipeId)
  return recipe ? recipe.name : recipeId
}

// 统计信息
const stats = computed(() => {
  const totalSchedules = Object.values(store.plotSchedules || {}).reduce((sum, schedules) => sum + (schedules?.length || 0), 0)
  return {
    totalSchedules,
    totalLogs: store.executionLogs?.length || 0
  }
})
</script>

<template>
  <div class="soil-container">
    <!-- 顶部标题和统计 -->
    <div class="soil-header">
      <div class="header-content">
        <div class="header-title">
          <el-icon class="title-icon"><LocationIcon /></el-icon>
          <h2>土壤配方分配管理</h2>
        </div>
        <div class="header-stats">
          <div class="stat-item">
            <span class="stat-label">定时任务</span>
            <span class="stat-value">{{ stats.totalSchedules }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">执行日志</span>
            <span class="stat-value">{{ stats.totalLogs }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 地块分配区域 -->
    <el-card class="plots-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <el-icon><LocationIcon /></el-icon>
          <span>地块配方分配</span>
        </div>
      </template>

      <el-row :gutter="20">
        <el-col
          v-for="plot in plots"
          :key="plot"
          :xl="12"
          :lg="12"
          :md="24"
          :sm="24"
          :xs="24"
        >
          <el-card class="plot-card" shadow="hover">
            <template #header>
              <div class="plot-header">
                <div class="plot-title">
                  <el-icon class="plot-icon"><LocationIcon /></el-icon>
                  <span>地块 {{ plot }}</span>
                  <el-tag 
                    :type="store.plotToRecipeId[plot] ? 'success' : 'info'" 
                    size="small"
                    style="margin-left: 8px;"
                  >
                    {{ getCurrentRecipeName(plot) }}
                  </el-tag>
                </div>
              </div>
            </template>

            <!-- 配方选择 -->
            <div class="plot-section">
              <div class="section-title">
                <el-icon><DocumentIcon /></el-icon>
                <span>配方分配</span>
              </div>
              <el-form inline size="default" class="plot-form">
                <el-form-item label="选择配方">
                  <el-select 
                    v-model="ensurePlotSelect(plot).id" 
                    placeholder="请选择配方" 
                    style="width: 200px;"
                    clearable
                  >
                    <el-option 
                      v-for="r in store.recipes" 
                      :key="r.id" 
                      :value="r.id" 
                      :label="r.name" 
                    />
                  </el-select>
                </el-form-item>
                <el-form-item label="执行次数">
                  <el-input-number 
                    v-model="ensureAssign(plot).times" 
                    :min="1" 
                    :max="20"
                    size="default"
                  />
                </el-form-item>
                <el-form-item>
                  <el-button 
                    type="primary" 
                    :icon="OperationIcon"
                    @click="assign(plot)"
                    :disabled="!ensurePlotSelect(plot).id"
                  >
                    立即分配
                  </el-button>
                </el-form-item>
              </el-form>
            </div>

            <!-- 定时执行 -->
            <el-divider />
            <div class="plot-section">
              <div class="section-title">
                <el-icon><ClockIcon /></el-icon>
                <span>定时执行计划</span>
              </div>

              <el-form size="default" label-width="100px" class="schedule-form">
                <el-form-item label="执行周期">
                  <el-radio-group v-model="ensureSched(plot).scheduleType">
                    <el-radio label="daily">每天</el-radio>
                    <el-radio label="weekly">每周</el-radio>
                    <el-radio label="monthly">每月</el-radio>
                  </el-radio-group>
                </el-form-item>
                
                <!-- 每周执行：选择周几 -->
                <el-form-item v-if="ensureSched(plot).scheduleType === 'weekly'" label="选择周几">
                  <el-select 
                    v-model="ensureSched(plot).dayOfWeek" 
                    placeholder="选择周几" 
                    style="width: 200px;"
                  >
                    <el-option label="周日" :value="0" />
                    <el-option label="周一" :value="1" />
                    <el-option label="周二" :value="2" />
                    <el-option label="周三" :value="3" />
                    <el-option label="周四" :value="4" />
                    <el-option label="周五" :value="5" />
                    <el-option label="周六" :value="6" />
                  </el-select>
                </el-form-item>
                
                <!-- 每月执行：精确时间 -->
                <el-form-item v-if="ensureSched(plot).scheduleType === 'monthly'" label="精确时间">
                  <el-date-picker
                    v-model="ensureSched(plot).scheduleDatetime"
                    type="datetime"
                    value-format="YYYY-MM-DD HH:mm:ss"
                    format="YYYY-MM-DD HH:mm:ss"
                    placeholder="选择年月日时分秒"
                    style="width: 250px;"
                  />
                  <span style="margin-left: 8px; color: #999; font-size: 12px;">或使用下面的时间（每月同一天）</span>
                </el-form-item>

                <!-- 时间选择（优化版：使用小时和分钟输入框） -->
                <el-form-item label="执行时间">
                  <div class="time-input-group">
                    <el-input-number
                      v-model="ensureTimeInput(plot).hour"
                      :min="0"
                      :max="23"
                      :step="1"
                      size="small"
                      :controls="false"
                      style="width: 70px;"
                      @change="syncTimeToForm(plot)"
                    />
                    <span class="time-separator">:</span>
                    <el-input-number
                      v-model="ensureTimeInput(plot).minute"
                      :min="0"
                      :max="59"
                      :step="1"
                      size="small"
                      :controls="false"
                      style="width: 70px;"
                      @change="syncTimeToForm(plot)"
                    />
                    <span class="time-hint">时:分</span>
                  </div>
                  <span v-if="ensureSched(plot).scheduleType === 'monthly' && !ensureSched(plot).scheduleDatetime" 
                        style="margin-left: 8px; color: #999; font-size: 12px;">
                    每月同一天此时间执行
                  </span>
                </el-form-item>
                
                <el-form-item label="执行次数">
                  <el-input-number 
                    v-model="ensureSched(plot).times" 
                    :min="1" 
                    :max="20"
                    size="default"
                  />
                </el-form-item>
                
                <el-form-item>
                  <el-button 
                    type="success" 
                    :icon="PlusIcon"
                    @click="addSchedule(plot)"
                    :disabled="!ensurePlotSelect(plot).id || 
                      (ensureSched(plot).scheduleType === 'weekly' && ensureSched(plot).dayOfWeek === null) ||
                      ((ensureSched(plot).scheduleType === 'daily' || ensureSched(plot).scheduleType === 'weekly') && !ensureSched(plot).time) ||
                      (ensureSched(plot).scheduleType === 'monthly' && !ensureSched(plot).scheduleDatetime && !ensureSched(plot).time)"
                  >
                    添加定时任务
                  </el-button>
                </el-form-item>
              </el-form>
            </div>

            <!-- 定时任务列表 -->
            <el-divider />
            <div class="schedule-list">
              <div class="section-title" style="margin-bottom: 12px;">
                <el-icon><ListIcon /></el-icon>
                <span>定时任务列表</span>
                <el-tag type="info" size="small" style="margin-left: 8px;">
                  {{ (store.plotSchedules[plot] || []).length }} 个任务
                </el-tag>
              </div>
              
              <div v-if="!store.plotSchedules[plot] || store.plotSchedules[plot].length === 0" class="empty-schedule">
                <el-empty description="暂无定时任务" :image-size="60" />
              </div>

              <div v-else class="schedule-items">
                <div 
                  v-for="schedule in store.plotSchedules[plot]" 
                  :key="schedule.id"
                  class="schedule-item"
                >
                  <div class="schedule-info">
                    <div class="schedule-time">
                      <el-icon><ClockIcon /></el-icon>
                      <span>{{ schedule.timeHHmm || schedule.time }}</span>
                    </div>
                    <div class="schedule-type">
                      <el-tag v-if="schedule.scheduleType === 'daily'" type="success" size="small">每天</el-tag>
                      <el-tag v-else-if="schedule.scheduleType === 'weekly'" type="warning" size="small">每周</el-tag>
                      <el-tag v-else-if="schedule.scheduleType === 'monthly'" type="info" size="small">每月</el-tag>
                      <span v-if="schedule.scheduleType === 'weekly' && schedule.dayOfWeek !== null" style="margin-left: 8px; font-size: 12px; color: #666;">
                        {{ ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][schedule.dayOfWeek] }}
                      </span>
                    </div>
                    <div class="schedule-recipe">
                      <el-icon><DocumentIcon /></el-icon>
                      <span>{{ (store.recipes.find(r => r.id === schedule.recipeId) || {}).name || schedule.recipeId }}</span>
                    </div>
                    <div class="schedule-executions">
                      <span>执行 {{ schedule.executions }} 次</span>
                    </div>
                  </div>
                  <div class="schedule-actions">
                    <el-button 
                      link 
                      type="primary" 
                      size="small" 
                      :icon="OperationIcon"
                      @click="executeSchedule(schedule.id)"
                    >
                      立即执行
                    </el-button>
                    <el-button 
                      link 
                      type="danger" 
                      size="small" 
                      :icon="DeleteIcon"
                      @click="removeSchedule(plot, schedule.id)"
                    >
                      删除
                    </el-button>
                  </div>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </el-card>

    <!-- 执行日志 -->
    <el-card class="logs-card" shadow="hover" style="margin-top: 20px;">
      <template #header>
        <div class="card-header">
          <el-icon><ListIcon /></el-icon>
          <span>执行日志</span>
          <el-tag type="info" size="small" style="margin-left: auto;">
            共 {{ stats.totalLogs }} 条记录
          </el-tag>
        </div>
      </template>

      <el-table 
        :data="store.executionLogs" 
        border 
        size="default" 
        empty-text="暂无日志"
        style="width: 100%;"
        max-height="500"
      >
        <el-table-column label="时间" width="180">
          <template #default="{ row }">
            <div class="log-time">
              <el-icon><ClockIcon /></el-icon>
              <span>{{ new Date(row.time).toLocaleString('zh-CN') }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="plot" label="地块" width="100">
          <template #default="{ row }">
            <el-tag type="primary" size="small">地块 {{ row.plot }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="配方" width="150">
          <template #default="{ row }">
            {{ (store.recipes.find(r => r.id === row.recipeId) || {}).name || row.recipeId }}
          </template>
        </el-table-column>
        <el-table-column prop="executions" label="执行次数" width="100" align="center" />
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button 
              link 
              type="primary" 
              size="small" 
              :icon="RefreshIcon"
              @click="store.executeFromLog(row.plot, row.recipeId, row.executions)"
            >
              重新执行
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.soil-container {
  padding: 0;
}

.soil-header {
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

.stat-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.8);
  margin-bottom: 4px;
}

.stat-value {
  font-size: 20px;
  font-weight: 700;
  color: white;
}

.plots-card,
.logs-card {
  border-radius: 12px;
  border: none;
  transition: all 0.3s ease;
}

.plots-card:hover,
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

.plot-card {
  margin-bottom: 20px;
  border-radius: 12px;
  border: none;
  transition: all 0.3s ease;
}

.plot-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1) !important;
  transform: translateY(-2px);
}

.plot-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.plot-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 16px;
}

.plot-icon {
  font-size: 20px;
  color: #6366f1;
}

.plot-section {
  margin-bottom: 20px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 14px;
  color: #1f2937;
  margin-bottom: 12px;
}

.section-title .el-icon {
  font-size: 16px;
  color: #6366f1;
}

.plot-form,
.schedule-form {
  padding: 8px 0;
}

.time-input-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.time-input-group :deep(.el-input-number) {
  width: 70px;
}

.time-input-group :deep(.el-input-number__decrease),
.time-input-group :deep(.el-input-number__increase) {
  display: none;
}

.time-input-group :deep(.el-input__inner) {
  text-align: center;
  padding: 0 8px;
  font-size: 14px;
  font-weight: 600;
}

.time-separator {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 4px;
}

.time-hint {
  font-size: 12px;
  color: #6b7280;
  margin-left: 4px;
}

.schedule-list {
  margin-top: 16px;
}

.empty-schedule {
  padding: 20px 0;
  text-align: center;
}

.schedule-items {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 400px;
  overflow-y: auto;
  padding-right: 4px;
}

.schedule-item {
  padding: 16px;
  background: linear-gradient(to bottom, #ffffff 0%, #f9fafb 100%);
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: all 0.3s ease;
}

.schedule-item:hover {
  border-color: #6366f1;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.1);
}

.schedule-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
}

.schedule-time,
.schedule-recipe,
.schedule-executions {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #1f2937;
}

.schedule-time {
  font-weight: 600;
  font-size: 14px;
}

.schedule-type {
  display: flex;
  align-items: center;
}

.schedule-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.log-time {
  display: flex;
  align-items: center;
  gap: 6px;
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

  .schedule-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .schedule-actions {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>
