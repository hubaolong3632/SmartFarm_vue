// 温室管理系统状态管理
// 使用 Pinia 管理全局状态，并通过 API 与后端交互

import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import request from '../utils/request'

export const useGreenhouseStore = defineStore('greenhouse', () => {
  // ========== 状态定义 ==========
  
  // 传感器时间序列数据（最近24小时）
  const hourly = ref([])
  
  // 报警列表
  const alerts = ref([])
  
  // 配方列表
  const recipes = ref([])
  
  // 地块数量
  const numPlots = ref(4)
  
  // 地块到配方的映射 (plot number -> recipe id)
  const plotToRecipeId = ref({})
  
  // 执行日志
  const executionLogs = ref([])
  
  // 每个地块的定时计划: { [plot]: [{ id, timeHHmm, recipeId, executions }] }
  const plotSchedules = ref({})
  
  // 控制状态
  const lightOn = ref(false)
  const cleaningInProgress = ref(false)
  
  // 自动化设置
  const automation = ref({
    lightLuxThreshold: 8000,
    soilMoistureLowThreshold: 35,
    autoLightEnabled: true,
    autoPumpEnabled: true,
  })
  
  // 图片数据
  const imagesByDate = ref({})
  const selectedDate = ref(new Date().toISOString().slice(0, 10))
  const images = ref([])
  
  // ========== 计算属性 ==========
  
  // 最新传感器数据
  const latest = computed(() => hourly.value[hourly.value.length - 1] || null)
  
  // 最近24小时执行统计（按小时聚合）
  const executionsLast24 = computed(() => {
    const now = new Date()
    const start = new Date(now.getTime() - 23 * 3600_000)
    const buckets = Array.from({ length: 24 }).map((_, i) => {
      const t = new Date(start.getTime() + i * 3600_000)
      return { time: t.toISOString(), count: 0 }
    })
    executionLogs.value.forEach(log => {
      const t = new Date(log.executedAt || log.time).getTime()
      if (t < start.getTime() || t > now.getTime()) return
      const idx = Math.floor((t - start.getTime()) / 3600_000)
      if (idx >= 0 && idx < buckets.length) {
        buckets[idx].count += Number(log.executions || 1)
      }
    })
    return buckets
  })
  
  // ========== 数据加载函数 ==========
  
  /**
   * 加载传感器数据（最新的30条记录，用于对比）
   */
  async function loadSensorData() {
    try {
      const data = await request.get('/sensor-data/today')
      console.log('收到最新30条传感器数据:', data)
      if (data && Array.isArray(data)) {
        const processed = data
          .map(item => {
            // 处理时间格式：可能是字符串、Date对象或时间戳
            let timeValue = item.recordTime || item.time
            let dateObj = null
            
            if (timeValue instanceof Date) {
              dateObj = timeValue
              timeValue = timeValue.toISOString()
            } else if (typeof timeValue === 'number') {
              dateObj = new Date(timeValue)
              timeValue = dateObj.toISOString()
            } else if (typeof timeValue === 'string') {
              if (!timeValue.includes('T')) {
                // 如果是 "2024-11-14 11:30:00" 格式，转换为 Date
                dateObj = new Date(timeValue.replace(' ', 'T'))
                timeValue = dateObj.toISOString()
              } else {
                dateObj = new Date(timeValue)
              }
            }
            
            // 处理 isRaining：支持布尔值、数字、字符串
            let isRainingValue = false
            if (item.isRaining !== null && item.isRaining !== undefined) {
              if (typeof item.isRaining === 'boolean') {
                isRainingValue = item.isRaining
              } else if (typeof item.isRaining === 'number') {
                isRainingValue = item.isRaining !== 0
              } else if (typeof item.isRaining === 'string') {
                isRainingValue = item.isRaining.toLowerCase() === 'true' || item.isRaining === '1'
              }
            }
            
            return {
              time: timeValue,
              dateObj: dateObj,
              temperatureC: Number(item.temperatureC || 0),
              soilMoisturePct: Number(item.soilMoisturePct || 0),
              isRaining: isRainingValue, // 保持为布尔值，折线图显示时转换为数字
              lightLux: Number(item.lightLux || 0),
              imageUrl: item.imageUrl || '',
            }
          })
          // 按时间排序（确保折线图按时间顺序正确显示，后端已排序但前端再次确认）
          .sort((a, b) => {
            if (!a.dateObj || !b.dateObj) return 0
            return a.dateObj - b.dateObj
          })
          // 移除临时的 dateObj 字段
          .map(({ dateObj, ...rest }) => rest)
        
        hourly.value = processed
        console.log('处理后的折线图数据:', hourly.value)
        console.log('数据点数:', hourly.value.length)
        if (hourly.value.length > 0) {
          console.log('最早数据时间:', new Date(hourly.value[0].time).toLocaleString())
          console.log('最新数据时间:', new Date(hourly.value[hourly.value.length - 1].time).toLocaleString())
        }
      } else {
        console.warn('传感器数据格式不正确:', data)
        hourly.value = []
      }
    } catch (error) {
      console.error('加载传感器数据失败:', error)
      hourly.value = []
    }
  }
  
  /**
   * 加载最新传感器数据
   */
  async function loadLatestSensorData() {
    try {
      const data = await request.get('/sensor-data/latest')
      if (data) {
        const newItem = {
          time: data.recordTime || data.time,
          temperatureC: Number(data.temperatureC || 0),
          soilMoisturePct: Number(data.soilMoisturePct || 0),
          isRaining: data.isRaining ? 1 : 0,
          lightLux: Number(data.lightLux || 0),
          imageUrl: data.imageUrl || '',
        }
        // 更新或添加到 hourly
        const existingIdx = hourly.value.findIndex(h => h.time === newItem.time)
        if (existingIdx >= 0) {
          hourly.value[existingIdx] = newItem
        } else {
          hourly.value.push(newItem)
          // 保持最近24小时
          if (hourly.value.length > 24) {
            hourly.value = hourly.value.slice(-24)
          }
        }
      }
    } catch (error) {
      console.error('加载最新传感器数据失败:', error)
    }
  }
  
  /**
   * 加载所有配方
   */
  async function loadRecipes() {
    try {
      const data = await request.get('/recipes')
      if (data && Array.isArray(data)) {
        recipes.value = data.map(item => ({
          id: String(item.id),
          name: item.name,
          waterMl: Number(item.waterMl || 0),
          nutrientMl: Number(item.nutrientMl || 0),
          rootingPowderMl: Number(item.rootingPowderMl || 0),
          specialMl: Number(item.specialMl || 0),
        }))
      }
    } catch (error) {
      console.error('加载配方失败:', error)
    }
  }
  
  /**
   * 加载报警列表
   */
  async function loadAlerts() {
    try {
      const data = await request.get('/alerts')
      if (data && Array.isArray(data)) {
        alerts.value = data.map(item => ({
          id: String(item.id),
          level: item.level || 'info',
          message: item.message,
          time: item.createdAt || item.time,
        })).sort((a, b) => new Date(b.time) - new Date(a.time))
      } else {
        // 如果没有数据，保持空数组
        alerts.value = []
      }
    } catch (error) {
      console.error('加载报警失败:', error)
      // 发生错误时保持空数组，不中断应用
      alerts.value = []
    }
  }
  
  /**
   * 加载执行日志
   */
  async function loadExecutionLogs() {
    try {
      const data = await request.get('/execution-logs')
      if (data && Array.isArray(data)) {
        executionLogs.value = data.map(item => ({
          time: item.executedAt || item.time,
          plot: item.plotId || item.plot,
          recipeId: String(item.recipeId),
          executions: Number(item.executions || 1),
        })).sort((a, b) => new Date(b.time) - new Date(a.time))
      } else {
        // 如果没有数据，保持空数组
        executionLogs.value = []
      }
    } catch (error) {
      console.error('加载执行日志失败:', error)
      // 发生错误时保持空数组，不中断应用
      executionLogs.value = []
    }
  }
  
  /**
   * 加载最近24小时执行统计
   */
  async function loadExecutionsLast24() {
    try {
      const data = await request.get('/execution-logs/last-24-hours')
      if (data && Array.isArray(data)) {
        // 转换为统一的格式
        const now = new Date()
        const start = new Date(now.getTime() - 23 * 3600_000)
        const buckets = Array.from({ length: 24 }).map((_, i) => {
          const t = new Date(start.getTime() + i * 3600_000)
          const hourStr = t.toISOString().slice(0, 13) + ':00:00'
          const found = data.find(d => {
            const dTime = new Date(d.time || d.recordTime)
            return dTime.getHours() === t.getHours() && dTime.getDate() === t.getDate()
          })
          return {
            time: t.toISOString(),
            count: found ? Number(found.count || 0) : 0
          }
        })
        // 更新 executionLogs 以便 executionsLast24 计算属性能正确工作
        executionLogs.value = data.map(item => ({
          time: item.time || item.recordTime,
          plot: 0,
          recipeId: '',
          executions: Number(item.count || 0),
          executedAt: item.time || item.recordTime
        }))
      }
    } catch (error) {
      console.error('加载执行统计失败:', error)
    }
  }
  
  /**
   * 加载地块分配情况
   */
  async function loadPlotAssignments() {
    try {
      const data = await request.get('/plots/assignments')
      if (data && typeof data === 'object') {
        const assignments = {}
        Object.keys(data).forEach(plotNum => {
          const assignment = data[plotNum]
          if (assignment && assignment.recipeId) {
            assignments[plotNum] = String(assignment.recipeId)
          }
        })
        plotToRecipeId.value = assignments
      }
    } catch (error) {
      console.error('加载地块分配失败:', error)
    }
  }
  
  /**
   * 加载所有地块的定时计划
   */
  async function loadPlotSchedules() {
    try {
      const plots = Array.from({ length: numPlots.value }, (_, i) => i + 1)
      const schedules = {}
      for (const plot of plots) {
        try {
          // 需要先获取地块ID，这里假设 plot number 就是 plotId
          const data = await request.get('/plots/schedules', { plotId: plot })
          if (data && Array.isArray(data)) {
            schedules[plot] = data.map(item => ({
              id: String(item.id),
              timeHHmm: item.scheduleTime || item.timeHHmm,
              recipeId: String(item.recipeId),
              executions: Number(item.executions || 1),
            }))
          } else {
            schedules[plot] = []
          }
        } catch (error) {
          schedules[plot] = []
        }
      }
      plotSchedules.value = schedules
    } catch (error) {
      console.error('加载定时计划失败:', error)
    }
  }
  
  /**
   * 加载自动化设置
   */
  async function loadAutomationSettings() {
    try {
      const data = await request.get('/automation')
      if (data && typeof data === 'object') {
        automation.value = {
          lightLuxThreshold: Number(data.lightLuxThreshold || 8000),
          soilMoistureLowThreshold: Number(data.soilMoistureLowThreshold || 35),
          autoLightEnabled: data.autoLightEnabled !== false,
          autoPumpEnabled: data.autoPumpEnabled !== false,
        }
      }
    } catch (error) {
      console.error('加载自动化设置失败:', error)
    }
  }
  
  /**
   * 加载指定日期的图片
   */
  async function loadImagesByDate(date) {
    try {
      const data = await request.get('/images/date', { date })
      if (data && Array.isArray(data)) {
        imagesByDate.value[date] = data.map(item => ({
          time: item.recordTime || item.time,
          url: item.imageUrl || item.url,
          temperatureC: Number(item.temperatureC || 0),
          soilMoisturePct: Number(item.soilMoisturePct || 0),
          lightLux: Number(item.lightLux || 0),
        }))
      }
    } catch (error) {
      console.error('加载图片失败:', error)
    }
  }
  
  /**
   * 初始化加载所有数据
   */
  async function loadAllData() {
    await Promise.all([
      loadSensorData(),
      loadRecipes(),
      loadAlerts(),
      loadExecutionLogs(),
      loadPlotAssignments(),
      loadPlotSchedules(),
      loadAutomationSettings(),
    ])
  }
  
  // ========== 配方管理 ==========
  
  /**
   * 添加配方
   */
  async function addRecipe(payload) {
    try {
      const data = await request.post('/recipes', {
        name: payload.name,
        waterMl: payload.waterMl || 0,
        nutrientMl: payload.nutrientMl || 0,
        rootingPowderMl: payload.rootingPowderMl || 0,
        specialMl: payload.specialMl || 0,
      })
      if (data) {
        await loadRecipes()
        return String(data.id)
      }
    } catch (error) {
      console.error('添加配方失败:', error)
    }
  }
  
  /**
   * 更新配方
   */
  async function updateRecipe(id, payload) {
    try {
      const url = `/recipes?id=${id}`
      const data = await request.put(url, {
        name: payload.name,
        waterMl: payload.waterMl || 0,
        nutrientMl: payload.nutrientMl || 0,
        rootingPowderMl: payload.rootingPowderMl || 0,
        specialMl: payload.specialMl || 0,
      })
      if (data) {
        await loadRecipes()
      }
    } catch (error) {
      console.error('更新配方失败:', error)
    }
  }
  
  /**
   * 删除配方
   */
  async function removeRecipe(id) {
    try {
      await request.delete('/recipes', { id })
      await loadRecipes()
      // 清除相关的地块分配
      const updated = { ...plotToRecipeId.value }
      Object.keys(updated).forEach(k => {
        if (updated[k] === id) {
          delete updated[k]
        }
      })
      plotToRecipeId.value = updated
    } catch (error) {
      console.error('删除配方失败:', error)
    }
  }
  
  // ========== 地块管理 ==========
  
  /**
   * 分配配方到地块
   */
  async function assignRecipeToPlot(plotNumber, recipeId, executions = 1) {
    try {
      // 使用 URL 参数传递 plotId
      const url = `/plots/assign?plotId=${plotNumber}`
      const data = await request.post(url, {
        recipeId,
        executions: Number(executions || 1),
      })
      if (data) {
        plotToRecipeId.value = { ...plotToRecipeId.value, [plotNumber]: recipeId }
        await loadExecutionLogs()
        pushAlert('info', `地块${plotNumber} 分配配方后执行 ${executions} 次`)
      }
    } catch (error) {
      console.error('分配配方失败:', error)
    }
  }
  
  /**
   * 添加定时计划
   */
  async function addSchedule(plotNumber, recipeId, timeHHmm, executions = 1) {
    try {
      // 使用 URL 参数传递 plotId
      const url = `/plots/schedules?plotId=${plotNumber}`
      const data = await request.post(url, {
        recipeId,
        timeHHmm,
        executions: Number(executions || 1),
      })
      if (data) {
        await loadPlotSchedules()
        pushAlert('info', `地块${plotNumber} 添加定时 ${timeHHmm} 执行 ${executions} 次`)
        return String(data.id)
      }
    } catch (error) {
      console.error('添加定时计划失败:', error)
    }
  }
  
  /**
   * 删除定时计划
   */
  async function removeSchedule(plotNumber, entryId) {
    try {
      await request.delete('/plots/schedules', { scheduleId: entryId })
      await loadPlotSchedules()
    } catch (error) {
      console.error('删除定时计划失败:', error)
    }
  }
  
  // ========== 控制操作 ==========
  
  /**
   * 触发清理操作
   */
  async function triggerCleaning() {
    if (cleaningInProgress.value) return
    try {
      cleaningInProgress.value = true
      const data = await request.post('/control/cleaning')
      if (data) {
        setTimeout(() => {
          cleaningInProgress.value = false
        }, 1500)
      } else {
        cleaningInProgress.value = false
      }
    } catch (error) {
      console.error('清理操作失败:', error)
      cleaningInProgress.value = false
    }
  }
  
  /**
   * 切换补光灯
   */
  async function toggleLight(on) {
    try {
      const action = typeof on === 'boolean' ? (on ? 1 : 0) : (lightOn.value ? 0 : 1)
      const url = `/control/light?action=${action}`
      const data = await request.post(url)
      if (data) {
        lightOn.value = action === 1
      }
    } catch (error) {
      console.error('切换补光灯失败:', error)
    }
  }
  
  // ========== 自动化 ==========
  
  /**
   * 保存自动化设置
   */
  async function saveAutomationSettings() {
    try {
      await request.put('/automation', automation.value)
      await loadAutomationSettings()
    } catch (error) {
      console.error('保存自动化设置失败:', error)
    }
  }
  
  /**
   * 评估自动化规则（前端模拟，实际应由后端处理）
   */
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
  
  // ========== 报警管理 ==========
  
  /**
   * 添加报警（本地，实际应由后端推送）
   */
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
  
  // ========== 图片管理 ==========
  
  /**
   * 设置外部图片数组
   */
  function setImages(list) {
    images.value = Array.isArray(list) ? list : []
  }
  
  // ========== 数据轮询 ==========
  
  let sensorTimer  // 传感器数据定时器（1秒刷新）
  let otherTimer   // 其他数据定时器（5秒刷新）
  
  function startSimulation() {
    if (sensorTimer || otherTimer) return
    
    // 传感器数据每1秒刷新一次（实时折线图）
    sensorTimer = setInterval(async () => {
      await loadSensorData() // 重新加载最新的30条数据，确保折线图数据最新
    }, 1000) // 每1秒刷新一次
    
    // 执行日志和报警每5秒刷新一次
    otherTimer = setInterval(async () => {
      await loadExecutionLogs()
      await loadAlerts()
      evaluateAutomation()
    }, 5000) // 每5秒刷新一次
  }
  
  function stopSimulation() {
    if (sensorTimer) {
      clearInterval(sensorTimer)
      sensorTimer = undefined
    }
    if (otherTimer) {
      clearInterval(otherTimer)
      otherTimer = undefined
    }
  }
  
  // ========== 导出 ==========
  
  return {
    // 状态
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
    
    // 数据加载
    loadAllData,
    loadSensorData,
    loadLatestSensorData,
    loadRecipes,
    loadAlerts,
    loadExecutionLogs,
    loadExecutionsLast24,
    loadPlotAssignments,
    loadPlotSchedules,
    loadAutomationSettings,
    loadImagesByDate,
    
    // 配方管理
    addRecipe,
    updateRecipe,
    removeRecipe,
    
    // 地块管理
    assignRecipeToPlot,
    addSchedule,
    removeSchedule,
    
    // 控制操作
    triggerCleaning,
    toggleLight,
    
    // 自动化
    saveAutomationSettings,
    evaluateAutomation,
    
    // 报警
    pushAlert,
    
    // 图片
    setImages,
    
    // 轮询
    startSimulation,
    stopSimulation,
  }
})
