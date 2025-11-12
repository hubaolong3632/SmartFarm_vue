<script setup>
import { computed, ref, reactive, watchEffect, onMounted } from 'vue'
import { useGreenhouseStore } from '../stores/greenhouse'
const store = useGreenhouseStore()

onMounted(async () => {
  // 加载相关数据
  await Promise.all([
    store.loadRecipes(),
    store.loadPlotAssignments(),
    store.loadPlotSchedules(),
    store.loadExecutionLogs(),
  ])
})

const plots = computed(() => Array.from({ length: store.numPlots }, (_, i) => i + 1))
const selectedRecipe = ref('')

// 每个地块独立的“分配执行次数”与“定时（时间/次数）”
const assignForm = reactive({}) // { [plot]: { times: number } }
const schedForm = reactive({}) // { [plot]: { time: 'HH:mm', times: number } }
const plotRecipeSelect = reactive({}) // { [plot]: { id: string } }

function ensureAssign(plot) {
  if (!assignForm[plot]) assignForm[plot] = { times: 1 }
  return assignForm[plot]
}
function ensureSched(plot) {
  if (!schedForm[plot]) schedForm[plot] = { time: '12:00', times: 1 }
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
  if (!per.id || !s.time) return
  store.addSchedule(plot, per.id, s.time, s.times)
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

            <el-form inline size="small" label-width="70px" style="margin-bottom: 8px;">
              <el-form-item label="时间">
                <el-time-select
                  v-model="ensureSched(plot).time"
                  start="00:00"
                  step="00:30"
                  end="23:30"
                  placeholder="选择时间"
                />
              </el-form-item>
              <el-form-item label="次数">
                <el-input-number v-model="ensureSched(plot).times" :min="1" :max="20" />
              </el-form-item>
              <el-form-item>
                <el-button type="success" :disabled="!ensurePlotSelect(plot).id || !ensureSched(plot).time" @click="addSchedule(plot)">
                  添加
                </el-button>
              </el-form-item>
            </el-form>

            <el-table
              :data="store.plotSchedules[plot] || []"
              size="small"
              border
              stripe
              empty-text="暂无定时"
            >
              <el-table-column prop="timeHHmm" label="时间" width="100" />
              <el-table-column label="配方">
                <template #default="{ row }">
                  {{ (store.recipes.find(r => r.id === row.recipeId) || {}).name || row.recipeId }}
                </template>
              </el-table-column>
              <el-table-column prop="executions" label="次数" width="90" />
              <el-table-column label="操作" width="90">
                <template #default="{ row }">
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
      <el-table :data="store.executionLogs" border size="small" empty-text="暂无日志">
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
      </el-table>
    </el-card>
  </div>
</template>

<style scoped></style>


