<script setup>
import { computed, ref, reactive, watchEffect, onMounted } from 'vue'
import { useGreenhouseStore } from '../stores/greenhouse'
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
  if (!per.id) return
  const a = ensureAssign(plot)
  store.assignRecipeToPlot(plot, per.id, a.times)
}

function addSchedule(plot) {
  const s = ensureSched(plot)
  const per = ensurePlotSelect(plot)
  if (!per.id) return

  // 如果是每天或每周，需要时间
  if ((s.scheduleType === 'daily' || s.scheduleType === 'weekly') && !s.time) {
    return
  }

  // 如果是每月，需要精确时间或时间
  if (s.scheduleType === 'monthly' && !s.scheduleDatetime && !s.time) {
    return
  }

  store.addSchedule(plot, per.id, s.time, s.times, s.scheduleType, s.dayOfWeek, s.scheduleDatetime)

  // 重置表单
  schedForm[plot] = {
    time: '12:00',
    times: 1,
    scheduleType: 'daily',
    dayOfWeek: null,
    scheduleDatetime: ''
  }
}
</script>

<template>
  <div>
    <el-card shadow="never">
      <template #header>土壤配方分配</template>
      <el-form inline>
     
       
      </el-form>
      <el-row :gutter="12" style="margin-top:8px; flex-wrap: wrap;">
        <el-col
          v-for="plot in plots"
          :key="plot"
          :xl="12"
          :lg="12"
          :md="12"
          :sm="24"
          :xs="24"
        >
          <el-card shadow="never" :body-style="{ padding: '12px' }">
            <template #header>
              <div style="display:flex; align-items:center; justify-content:space-between;">
                <div style="font-weight:600;">地块 {{ plot }}</div>
              
              </div>
            </template>

            <el-form inline size="small" label-width="70px" style="margin-bottom: 6px;">
              <el-form-item label="配方">
                <el-select v-model="ensurePlotSelect(plot).id" placeholder="请选择" style="min-width: 220px;">
                  <el-option v-for="r in store.recipes" :key="r.id" :value="r.id" :label="r.name" />
                </el-select>
              </el-form-item>
           
          
            </el-form>

            <el-divider content-position="left">定时执行</el-divider>

            <el-form size="small" label-width="100px" style="margin-bottom: 8px;">
              <el-form-item label="执行周期">
                <el-radio-group v-model="ensureSched(plot).scheduleType">
                  <el-radio label="daily">每天</el-radio>
                  <el-radio label="weekly">每周</el-radio>
                  <el-radio label="monthly">每月</el-radio>
                </el-radio-group>
              </el-form-item>
              
              <!-- 每周执行：选择周几 -->
              <el-form-item v-if="ensureSched(plot).scheduleType === 'weekly'" label="周几">
                <el-select v-model="ensureSched(plot).dayOfWeek" placeholder="选择周几" style="width: 200px;">
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
                  style="width: 200px;"
                />
                <span style="margin-left: 8px; color: #999; font-size: 12px;">或使用下面的时间（每月同一天）</span>
              </el-form-item>
              
              <!-- 时间选择（每天/每周/每月都可以用） -->
              <el-form-item label="时间">
                <el-time-select
                  v-model="ensureSched(plot).time"
                  start="00:00"
                  step="00:01"
                  end="23:59"
                  placeholder="选择时间（HH:mm）"
                  style="width: 150px;"
                />
                <span v-if="ensureSched(plot).scheduleType === 'monthly' && !ensureSched(plot).scheduleDatetime" style="margin-left: 8px; color: #999; font-size: 12px;">每月同一天此时间执行</span>
              </el-form-item>
              
              <el-form-item label="执行次数">
                <el-input-number v-model="ensureSched(plot).times" :min="1" :max="20" />
              </el-form-item>
              
              <el-form-item>
                <el-button 
                  type="success" 
                  :disabled="!ensurePlotSelect(plot).id || 
                    (ensureSched(plot).scheduleType === 'weekly' && ensureSched(plot).dayOfWeek === null) ||
                    ((ensureSched(plot).scheduleType === 'daily' || ensureSched(plot).scheduleType === 'weekly') && !ensureSched(plot).time) ||
                    (ensureSched(plot).scheduleType === 'monthly' && !ensureSched(plot).scheduleDatetime && !ensureSched(plot).time)" 
                  @click="addSchedule(plot)">
                  添加定时任务
                </el-button>
              </el-form-item>
            </el-form>

            <el-table
              :data="store.plotSchedules[plot] || []"
              size="small"
              border
              stripe
              empty-text="暂无定时"
              height="300"
            >
              <el-table-column prop="timeHHmm" label="时间" width="100" />
              <el-table-column label="执行周期" width="100">
                <template #default="{ row }">
                  <el-tag v-if="row.scheduleType === 'daily'" type="success" size="small">每天</el-tag>
                  <el-tag v-else-if="row.scheduleType === 'weekly'" type="warning" size="small">每周</el-tag>
                  <el-tag v-else-if="row.scheduleType === 'monthly'" type="info" size="small">每月</el-tag>
                  <span v-else>每天</span>
                </template>
              </el-table-column>
              <el-table-column label="周几" width="80">
                <template #default="{ row }">
                  <span v-if="row.scheduleType === 'weekly' && row.dayOfWeek !== null">
                    {{ ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][row.dayOfWeek] }}
                  </span>
                  <span v-else>-</span>
                </template>
              </el-table-column>
              <el-table-column label="精确时间" width="160">
                <template #default="{ row }">
                  <span v-if="row.scheduleDatetime">{{ new Date(row.scheduleDatetime).toLocaleString('zh-CN') }}</span>
                  <span v-else>-</span>
                </template>
              </el-table-column>
              <el-table-column label="配方">
                <template #default="{ row }">
                  {{ (store.recipes.find(r => r.id === row.recipeId) || {}).name || row.recipeId }}
                </template>
              </el-table-column>
              <el-table-column prop="executions" label="次数" width="90" />
              <el-table-column label="操作" width="150">
                <template #default="{ row }">
                  <el-button link type="primary" size="small" @click="store.executeSchedule(row.id)">立即执行</el-button>
                  <el-button link type="danger" size="small" @click="store.removeSchedule(plot, row.id)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
      </el-row>
    </el-card>

    <el-card shadow="never" style="margin-top:16px;">
      <template #header>地块区执行日志</template>
      <el-table 
        :data="store.executionLogs" 
        border 
        size="small" 
        empty-text="暂无日志"
        height="600"
      >
        <el-table-column label="时间" width="180">
          <template #default="{ row }">{{ new Date(row.time).toLocaleString() }}</template>
        </el-table-column>
        <el-table-column prop="plot" label="地块" width="80" />
        <el-table-column label="配方">
          <template #default="{ row }">
            {{ (store.recipes.find(r => r.id === row.recipeId) || {}).name || row.recipeId }}
          </template>
        </el-table-column>
        <el-table-column prop="executions" label="执行次数" width="100" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="store.executeFromLog(row.plot, row.recipeId, row.executions)">重新执行</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped></style>


