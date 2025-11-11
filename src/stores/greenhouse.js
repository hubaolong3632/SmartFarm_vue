import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

function generateHourlySeries() {
  const now = new Date()
  // start from now - 23 hours, step by 1 hour -> rolling last 24 hours
  const start = new Date(now.getTime() - 23 * 3600_000)
  const hours = Array.from({ length: 24 }).map((_, i) => {
    const t = new Date(start.getTime() + i * 3600_000)
    return {
      time: t.toISOString(),
      temperatureC: 18 + Math.sin(i / 3) * 6 + (Math.random() - 0.5) * 1.5,
      soilMoisturePct: 40 + Math.cos(i / 4) * 25 + (Math.random() - 0.5) * 5,
      isRaining: Math.random() < 0.1 ? 1 : 0,
      lightLux: Math.max(0, Math.sin(((t.getHours()) - 6) / 4) * 35000 + (Math.random() - 0.5) * 2000),
      imageUrl: '',
    }
  })
  return hours
}

export const useGreenhouseStore = defineStore('greenhouse', () => {
  // Sensor time-series
  const hourly = ref(generateHourlySeries())

  // Alerts
  const alerts = ref([
    // { id: 1, level: 'warning', message: '水箱液位偏低(<20%)', time: new Date().toISOString() }
  ])

  // Recipes
  const recipes = ref([
    { id: 'r1', name: '基础配方', waterMl: 500, nutrientMl: 50, rootingPowderMl: 0, specialMl: 0 },
  ])
  const nextRecipeId = ref(2)

  // Soil plot assignment (plot number -> recipe id)
  const numPlots = ref(4)
  const plotToRecipeId = ref({ 1: 'r1' })

  // Execution logs
  const executionLogs = ref([]) // { time, plot, recipeId, executions }

  // Per-plot schedules: { [plot]: [{ id, timeHHmm, recipeId, executions }] }
  const plotSchedules = ref({})

  // Controls
  const lightOn = ref(false)
  const cleaningInProgress = ref(false)

  // Automation thresholds
  const automation = ref({
    lightLuxThreshold: 8000, // below -> turn on
    soilMoistureLowThreshold: 35, // below -> pump
    autoLightEnabled: true,
    autoPumpEnabled: true,
  })

  // Images per date
  const imagesByDate = ref({})
  const selectedDate = ref(new Date().toISOString().slice(0, 10))
  // External images array source (if provided, gallery will use this directly)
  // Each item suggested shape: { time: ISOString, url: string, temperatureC?: number, soilMoisturePct?: number, lightLux?: number }
  const images = ref([])
  function setImages(list) {
    images.value = Array.isArray(list) ? list : []
  }

  const latest = computed(() => hourly.value[hourly.value.length - 1] || null)

  function addRecipe(payload) {
    const id = `r${nextRecipeId.value++}`
    recipes.value.push({ id, ...payload })
    return id
  }

  function updateRecipe(id, payload) {
    const idx = recipes.value.findIndex(r => r.id === id)
    if (idx >= 0) recipes.value[idx] = { ...recipes.value[idx], ...payload }
  }

  function removeRecipe(id) {
    recipes.value = recipes.value.filter(r => r.id !== id)
    const updated = { ...plotToRecipeId.value }
    Object.keys(updated).forEach(k => {
      if (updated[k] === id) {
        delete updated[k]
      }
    })
    plotToRecipeId.value = updated
  }

  function logExecution(plotNumber, recipeId, executions) {
    executionLogs.value.unshift({
      time: new Date().toISOString(),
      plot: plotNumber,
      recipeId,
      executions: Math.max(1, Number(executions || 1)),
    })
    // keep last 500 logs
    executionLogs.value = executionLogs.value.slice(0, 500)
  }

  function assignRecipeToPlot(plotNumber, recipeId, executions = 1) {
    plotToRecipeId.value = { ...plotToRecipeId.value, [plotNumber]: recipeId }
    logExecution(plotNumber, recipeId, executions)
    pushAlert('info', `地块${plotNumber} 分配配方后执行 ${executions} 次`)
  }

  function ensurePlotSchedule(plotNumber) {
    if (!plotSchedules.value[plotNumber]) {
      plotSchedules.value = { ...plotSchedules.value, [plotNumber]: [] }
    }
  }

  function addSchedule(plotNumber, recipeId, timeHHmm, executions = 1) {
    ensurePlotSchedule(plotNumber)
    const entry = {
      id: `${plotNumber}-${Date.now()}`,
      timeHHmm,
      recipeId,
      executions: Math.max(1, Number(executions || 1)),
    }
    plotSchedules.value[plotNumber] = [...plotSchedules.value[plotNumber], entry]
    pushAlert('info', `地块${plotNumber} 添加定时 ${timeHHmm} 执行 ${entry.executions} 次`)
    return entry.id
  }

  function removeSchedule(plotNumber, entryId) {
    ensurePlotSchedule(plotNumber)
    plotSchedules.value[plotNumber] = plotSchedules.value[plotNumber].filter(e => e.id !== entryId)
  }

  function triggerCleaning() {
    if (cleaningInProgress.value) return
    cleaningInProgress.value = true
    setTimeout(() => {
      cleaningInProgress.value = false
    }, 1500)
  }

  function toggleLight(on) {
    lightOn.value = typeof on === 'boolean' ? on : !lightOn.value
  }

  function pushAlert(level, message) {
    alerts.value.unshift({
      id: `${Date.now()}`,
      level,
      message,
      time: new Date().toISOString(),
    })
    // keep last 100
    alerts.value = alerts.value.slice(0, 100)
  }

  // Simple automation simulation hooks
  function evaluateAutomation() {
    const last = latest.value
    if (!last) return
    // Light control
    if (automation.value.autoLightEnabled) {
      if (last.lightLux < automation.value.lightLuxThreshold && !lightOn.value) {
        toggleLight(true)
        pushAlert('info', `自动化：光照(${Math.round(last.lightLux)}) 低于阈值，已开启补光灯`)
      }
      if (last.lightLux >= automation.value.lightLuxThreshold && lightOn.value) {
        toggleLight(false)
        pushAlert('info', '自动化：光照恢复，已关闭补光灯')
      }
    }
    // Moisture control
    if (automation.value.autoPumpEnabled) {
      if (last.soilMoisturePct < automation.value.soilMoistureLowThreshold) {
        pushAlert('warning', `自动化：湿度(${Math.round(last.soilMoisturePct)}%) 低，执行抽水`)
      }
    }
  }

  // For demo: periodically add a new hour sample
  let timer
  function startSimulation() {
    if (timer) return
    timer = setInterval(() => {
      const prev = hourly.value[hourly.value.length - 1]
      const nextTime = new Date(new Date(prev.time).getTime() + 3600_000)
      const sample = {
        time: nextTime.toISOString(),
        temperatureC: Math.max(10, Math.min(35, prev.temperatureC + (Math.random() - 0.5) * 2)),
        soilMoisturePct: Math.max(10, Math.min(90, prev.soilMoisturePct + (Math.random() - 0.5) * 5)),
        isRaining: Math.random() < 0.08 ? 1 : 0,
        lightLux: Math.max(0, prev.lightLux + (Math.random() - 0.5) * 4000),
        imageUrl: '',
      }
      hourly.value = [...hourly.value.slice(-23), sample]
      evaluateAutomation()
    }, 4000)
  }

  function stopSimulation() {
    if (timer) {
      clearInterval(timer)
      timer = undefined
    }
  }

  // Aggregated executions per hour for last 24 hours
  const executionsLast24 = computed(() => {
    const now = new Date()
    const start = new Date(now.getTime() - 23 * 3600_000)
    // build 24 buckets
    const buckets = Array.from({ length: 24 }).map((_, i) => {
      const t = new Date(start.getTime() + i * 3600_000)
      return { time: t.toISOString(), count: 0 }
    })
    executionLogs.value.forEach(log => {
      const t = new Date(log.time).getTime()
      if (t < start.getTime() || t > now.getTime()) return
      const idx = Math.floor((t - start.getTime()) / 3600_000)
      if (idx >= 0 && idx < buckets.length) {
        buckets[idx].count += Number(log.executions || 1)
      }
    })
    return buckets
  })

  // Seed a few demo abnormalities so the gallery can show warning styles
  function seedDemoAbnormalities() {
    const n = hourly.value.length
    const targets = [n - 3, n - 8, n - 12].filter(i => i >= 0)
    targets.forEach((i, idx) => {
      const orig = hourly.value[i]
      const sample = { ...orig }
      if (idx % 2 === 0) {
        sample.temperatureC = 41 // high temperature
        pushAlert('warning', `温度异常：${new Date(sample.time).toLocaleString()} 温度 ${sample.temperatureC.toFixed(1)}°C`)
      } else {
        sample.soilMoisturePct = 20 // low moisture
        pushAlert('warning', `湿度异常：${new Date(sample.time).toLocaleString()} 湿度 ${Math.round(sample.soilMoisturePct)}%`)
      }
      hourly.value[i] = sample
    })
  }
  // run once on store init
  seedDemoAbnormalities()

  return {
    hourly,
    latest,
    alerts,
    recipes,
    plotToRecipeId,
    plotSchedules,
    numPlots,
    lightOn,
    cleaningInProgress,
    automation,
    imagesByDate,
    selectedDate,
    images,
    executionLogs,
    executionsLast24,
    addRecipe,
    updateRecipe,
    removeRecipe,
    assignRecipeToPlot,
    addSchedule,
    removeSchedule,
    triggerCleaning,
    toggleLight,
    pushAlert,
    startSimulation,
    stopSimulation,
    setImages,
  }
})


