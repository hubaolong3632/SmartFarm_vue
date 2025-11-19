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
    temperatureHighThreshold: 35,
    temperatureLowThreshold: 10,
    humidityHighThreshold: 80,
    humidityLowThreshold: 30,
    oxygenLowThreshold: 18,
    co2HighThreshold: 1000,
    autoLightEnabled: true,
    autoPumpEnabled: true,
    imageUploadIntervalSeconds: 3600, // 图片上传间隔时间（秒），默认1小时
  })
  
  // 图片数据
  const imagesByDate = ref({})
  const selectedDate = ref(new Date().toISOString().slice(0, 10))
  const images = ref([])
  const latestImage = ref(null) // 最新图片
  
  // ========== 计算属性 ==========
  
  // 最新传感器数据
  const latest = computed(() => hourly.value[hourly.value.length - 1] || null)
  
  // 最近24小时执行统计（按小时聚合）
  // 使用缓存优化，避免频繁重新计算
  let executionsLast24Cache = null
  let executionsLast24CacheTime = 0
  const executionsLast24 = computed(() => {
    // 如果缓存有效（5秒内），直接返回缓存
    const now = Date.now()
    if (executionsLast24Cache && (now - executionsLast24CacheTime) < 5000) {
      return executionsLast24Cache
    }
    
    const dateNow = new Date()
    const start = new Date(dateNow.getTime() - 23 * 3600_000)
    const buckets = Array.from({ length: 24 }).map((_, i) => {
      const t = new Date(start.getTime() + i * 3600_000)
      return { time: t.toISOString(), count: 0 }
    })
    
    // 优化：如果日志数量很大，使用更高效的方式
    const logs = executionLogs.value
    const startTime = start.getTime()
    const endTime = dateNow.getTime()
    const hourMs = 3600_000
    
    for (let i = 0; i < logs.length; i++) {
      const log = logs[i]
      const t = new Date(log.executedAt || log.time).getTime()
      if (t < startTime || t > endTime) continue
      const idx = Math.floor((t - startTime) / hourMs)
      if (idx >= 0 && idx < buckets.length) {
        buckets[idx].count += Number(log.executions || 1)
      }
    }
    
    // 更新缓存
    executionsLast24Cache = buckets
    executionsLast24CacheTime = now
    return buckets
  })
  
  // 植物状态日志（基于最新30条图片数据）
  const imageStatusLogs = computed(() => {
    // 获取最新30条图片数据
    const latestImages = images.value.slice(0, 30)
    if (latestImages.length === 0) return []
    
    // 按时间排序（从旧到新）
    const sortedImages = [...latestImages].sort((a, b) => {
      const timeA = new Date(a.time || a.recordTime).getTime()
      const timeB = new Date(b.time || b.recordTime).getTime()
      return timeA - timeB
    })
    
    // 统计每个时间点的正常和异常数量
    return sortedImages.map(img => {
      const isAbnormal = img.isAbnormal || false
      return {
        time: img.time || img.recordTime,
        normal: isAbnormal ? 0 : 1,
        abnormal: isAbnormal ? 1 : 0
      }
    })
  })
  
  // ========== 数据加载函数 ==========
  
  /**
   * 加载传感器数据（最新的30条记录，用于对比）
   */
  async function loadSensorData() {
    try {
      const data = await request.get('/sensor-data/today')
      // console.log('收到最新30条传感器数据:', data)
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
              humidityPct: Number(item.humidityPct || 0),
              soilMoisturePct: Number(item.soilMoisturePct || 0),
              isRaining: isRainingValue, // 保持为布尔值，折线图显示时转换为数字
              lightLux: Number(item.lightLux || 0),
              oxygenPct: Number(item.oxygenPct || 0),
              co2Ppm: Number(item.co2Ppm || 0),
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
        // console.log('处理后的折线图数据:', hourly.value)
        console.log('数据点数:', hourly.value.length)
        if (hourly.value.length > 0) {
          // console.log('最早数据时间:', new Date(hourly.value[0].time).toLocaleString())
          // console.log('最新数据时间:', new Date(hourly.value[hourly.value.length - 1].time).toLocaleString())
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
      
      // 并行请求所有地块的定时计划，而不是串行（解决路由切换卡顿问题）
      const requests = plots.map(async (plot) => {
        try {
          // 需要先获取地块ID，这里假设 plot number 就是 plotId
          const data = await request.get('/plots/schedules', { plotId: plot })
          if (data && Array.isArray(data)) {
            return {
              plot,
              schedules: data.map(item => {
                // 处理时间格式：scheduleTime 可能是 LocalTime 格式 (HH:mm:ss) 或字符串
                let timeHHmm = item.timeHHmm || item.scheduleTime
                if (timeHHmm && typeof timeHHmm === 'string' && timeHHmm.includes(':')) {
                  // 如果是 "HH:mm:ss" 格式，只取前5个字符 "HH:mm"
                  if (timeHHmm.length > 5) {
                    timeHHmm = timeHHmm.substring(0, 5)
                  }
                }
                return {
                  id: String(item.id),
                  timeHHmm: timeHHmm || '',
                  recipeId: String(item.recipeId),
                  executions: Number(item.executions || 1),
                  scheduleType: item.scheduleType || 'daily',
                  dayOfWeek: item.dayOfWeek !== null && item.dayOfWeek !== undefined ? item.dayOfWeek : null,
                  scheduleDatetime: item.scheduleDatetime || null,
                }
              })
            }
          } else {
            return { plot, schedules: [] }
          }
        } catch (error) {
          console.error(`加载地块${plot}的定时计划失败:`, error)
          return { plot, schedules: [] }
        }
      })
      
      // 等待所有请求并行完成
      const results = await Promise.all(requests)
      
      // 将结果合并到 schedules 对象中
      results.forEach(({ plot, schedules: plotScheds }) => {
        schedules[plot] = plotScheds
      })
      
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
          temperatureHighThreshold: Number(data.temperatureHighThreshold || 35),
          temperatureLowThreshold: Number(data.temperatureLowThreshold || 10),
          humidityHighThreshold: Number(data.humidityHighThreshold || 80),
          humidityLowThreshold: Number(data.humidityLowThreshold || 30),
          oxygenLowThreshold: Number(data.oxygenLowThreshold || 18),
          co2HighThreshold: Number(data.co2HighThreshold || 1000),
          autoLightEnabled: data.autoLightEnabled !== false,
          autoPumpEnabled: data.autoPumpEnabled !== false,
          imageUploadIntervalSeconds: Number(data.imageUploadIntervalSeconds || data.imageUploadIntervalHours * 3600 || 3600),
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
          id: item.id,
          time: item.recordTime || item.time,
          url: formatImageUrl(item.imageUrl || item.url),
          temperatureC: Number(item.temperatureC || 0),
          humidityPct: Number(item.humidityPct || 0),
          soilMoisturePct: Number(item.soilMoisturePct || 0),
          lightLux: Number(item.lightLux || 0),
          isRaining: item.isRaining ? true : false,
          oxygenPct: Number(item.oxygenPct || 0),
          co2Ppm: Number(item.co2Ppm || 0),
          plotId: item.plotId,
          isAbnormal: item.isAbnormal || false,
          abnormalReason: item.abnormalReason,
        }))
      } else {
        imagesByDate.value[date] = []
      }
    } catch (error) {
      console.error('加载图片失败:', error)
      imagesByDate.value[date] = []
    }
  }
  
  /**
   * 加载所有图片
   */
  async function loadAllImages() {
    try {
      const data = await request.get('/images')
      if (data && Array.isArray(data)) {
        images.value = data.map(item => ({
          id: item.id,
          time: item.recordTime || item.time,
          url: formatImageUrl(item.imageUrl || item.url),
          temperatureC: Number(item.temperatureC || 0),
          humidityPct: Number(item.humidityPct || 0),
          soilMoisturePct: Number(item.soilMoisturePct || 0),
          lightLux: Number(item.lightLux || 0),
          isRaining: item.isRaining ? true : false,
          oxygenPct: Number(item.oxygenPct || 0),
          co2Ppm: Number(item.co2Ppm || 0),
          plotId: item.plotId,
          isAbnormal: item.isAbnormal || false,
          abnormalReason: item.abnormalReason,
        }))
        // 更新最新图片
        if (images.value.length > 0) {
          latestImage.value = images.value[0]
        }
      } else {
        images.value = []
      }
    } catch (error) {
      console.error('加载所有图片失败:', error)
      images.value = []
    }
  }
  
  /**
   * 加载最新图片
   */
  async function loadLatestImage() {
    try {
      const data = await request.get('/images')
      if (data && Array.isArray(data) && data.length > 0) {
        // 按时间排序，获取最新的
        const sorted = data.sort((a, b) => {
          const timeA = new Date(a.recordTime || a.time || 0).getTime()
          const timeB = new Date(b.recordTime || b.time || 0).getTime()
          return timeB - timeA
        })
        const latest = sorted[0]
        latestImage.value = {
          id: latest.id,
          time: latest.recordTime || latest.time,
          url: formatImageUrl(latest.imageUrl || latest.url),
          temperatureC: Number(latest.temperatureC || 0),
          humidityPct: Number(latest.humidityPct || 0),
          soilMoisturePct: Number(latest.soilMoisturePct || 0),
          lightLux: Number(latest.lightLux || 0),
          isRaining: latest.isRaining ? true : false,
          oxygenPct: Number(latest.oxygenPct || 0),
          co2Ppm: Number(latest.co2Ppm || 0),
          plotId: latest.plotId,
          isAbnormal: latest.isAbnormal || false,
          abnormalReason: latest.abnormalReason,
        }
      }
    } catch (error) {
      console.error('加载最新图片失败:', error)
    }
  }
  
  /**
   * 加载异常图片
   */
  async function loadAbnormalImages() {
    try {
      const data = await request.get('/images/abnormal')
      if (data && Array.isArray(data)) {
        images.value = data.map(item => ({
          id: item.id,
          time: item.recordTime || item.time,
          url: formatImageUrl(item.imageUrl || item.url),
          temperatureC: Number(item.temperatureC || 0),
          humidityPct: Number(item.humidityPct || 0),
          soilMoisturePct: Number(item.soilMoisturePct || 0),
          lightLux: Number(item.lightLux || 0),
          isRaining: item.isRaining ? true : false,
          oxygenPct: Number(item.oxygenPct || 0),
          co2Ppm: Number(item.co2Ppm || 0),
          plotId: item.plotId,
          isAbnormal: item.isAbnormal || false,
          abnormalReason: item.abnormalReason,
        }))
      } else {
        images.value = []
      }
    } catch (error) {
      console.error('加载异常图片失败:', error)
      images.value = []
    }
  }
  
  /**
   * 格式化图片URL
   * 如果是相对路径，添加API基础URL
   */
  function formatImageUrl(url) {
    if (!url) return ''
    // 如果已经是完整URL，直接返回
    if (url.startsWith('http://') || url.startsWith('https://')) {
      return url
    }
    // 如果是相对路径（以/api开头），添加后端基础URL
    if (url.startsWith('/api/')) {
      return `http://localhost:11000${url}`
    }
    // 如果是其他相对路径，也添加基础URL
    return `http://localhost:11000/api${url.startsWith('/') ? '' : '/'}${url}`
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
  async function addSchedule(plotNumber, recipeId, timeHHmm, executions = 1, scheduleType = 'daily', dayOfWeek = null, scheduleDatetime = null) {
    try {
      // 使用 URL 参数传递 plotId
      const url = `/plots/schedules?plotId=${plotNumber}`
      const payload = {
        recipeId,
        timeHHmm: timeHHmm || null,
        executions: Number(executions || 1),
        scheduleType: scheduleType || 'daily',
        dayOfWeek: dayOfWeek !== null ? dayOfWeek : null,
        scheduleDatetime: scheduleDatetime || null,
      }
      const data = await request.post(url, payload)
      if (data) {
        await loadPlotSchedules()
        let message = `地块${plotNumber} 添加定时任务`
        if (scheduleType === 'daily') {
          message += `（每天 ${timeHHmm}）`
        } else if (scheduleType === 'weekly') {
          const weekDays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
          message += `（每周${weekDays[dayOfWeek] || '?'} ${timeHHmm}）`
        } else if (scheduleType === 'monthly') {
          if (scheduleDatetime) {
            message += `（每月 ${new Date(scheduleDatetime).toLocaleDateString('zh-CN')} ${new Date(scheduleDatetime).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })}）`
          } else {
            message += `（每月同一天 ${timeHHmm}）`
          }
        }
        message += ` 执行 ${executions} 次`
        pushAlert('info', message)
        return String(data.id)
      }
    } catch (error) {
      console.error('添加定时计划失败:', error)
    }
  }
  
  /**
   * 立即执行定时计划
   */
  async function executeSchedule(scheduleId) {
    try {
      const data = await request.post(`/plots/schedules/execute?scheduleId=${scheduleId}`)
      await loadExecutionLogs()
      pushAlert('success', data || '执行成功，已发送MQTT消息')
      return true
    } catch (error) {
      console.error('立即执行定时计划失败:', error)
      pushAlert('error', '执行失败: ' + (error.message || '未知错误'))
      return false
    }
  }
  
  /**
   * 立即执行配方分配
   */
  async function executeAssignment(plotNumber, executions = 1) {
    try {
      const data = await request.post(`/plots/assign/execute?plotId=${plotNumber}&executions=${executions}`)
      await loadExecutionLogs()
      pushAlert('success', data || `地块${plotNumber}执行成功，已发送MQTT消息`)
      return true
    } catch (error) {
      console.error('立即执行配方分配失败:', error)
      pushAlert('error', '执行失败: ' + (error.message || '未知错误'))
      return false
    }
  }
  
  /**
   * 根据执行日志立即执行（重新执行）
   */
  async function executeFromLog(plotNumber, recipeId, executions = 1) {
    try {
      const data = await request.post(`/plots/assign/execute?plotId=${plotNumber}&executions=${executions}`)
      await loadExecutionLogs()
      pushAlert('success', data || `地块${plotNumber}执行成功，已发送MQTT消息`)
      return true
    } catch (error) {
      console.error('立即执行失败:', error)
      pushAlert('error', '执行失败: ' + (error.message || '未知错误'))
      return false
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
        pushAlert('warning', `自动化：土壤湿度(${Math.round(last.soilMoisturePct)}%) 低，执行抽水`)
      }
    }
    
    // Temperature alerts
    if (last.temperatureC > automation.value.temperatureHighThreshold) {
      pushAlert('warning', `温度报警：温度(${last.temperatureC.toFixed(1)}°C) 高于阈值(${automation.value.temperatureHighThreshold}°C)`)
    }
    if (last.temperatureC < automation.value.temperatureLowThreshold) {
      pushAlert('warning', `温度报警：温度(${last.temperatureC.toFixed(1)}°C) 低于阈值(${automation.value.temperatureLowThreshold}°C)`)
    }
    
    // Humidity alerts
    if (last.humidityPct > automation.value.humidityHighThreshold) {
      pushAlert('warning', `湿度报警：湿度(${Math.round(last.humidityPct)}%) 高于阈值(${automation.value.humidityHighThreshold}%)`)
    }
    if (last.humidityPct < automation.value.humidityLowThreshold) {
      pushAlert('warning', `湿度报警：湿度(${Math.round(last.humidityPct)}%) 低于阈值(${automation.value.humidityLowThreshold}%)`)
    }
    
    // Oxygen alerts
    if (last.oxygenPct < automation.value.oxygenLowThreshold) {
      pushAlert('error', `氧气报警：氧气含量(${last.oxygenPct.toFixed(1)}%) 低于阈值(${automation.value.oxygenLowThreshold}%)`)
    }
    
    // CO2 alerts
    if (last.co2Ppm > automation.value.co2HighThreshold) {
      pushAlert('error', `二氧化碳报警：二氧化碳含量(${last.co2Ppm}ppm) 高于阈值(${automation.value.co2HighThreshold}ppm)`)
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
      await loadLatestImage() // 每5秒刷新最新图片
      await loadAllImages() // 每5秒刷新图片数据（用于植物状态日志）
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
  
  // ========== AI功能 ==========
  
  /**
   * 分析图片集（超时时间5分钟）
   */
  async function analyzeImages(limit = 30, startDate = null, endDate = null) {
    try {
      let url = `/ai/analyze-images?limit=${limit}`
      if (startDate) url += `&startDate=${startDate}`
      if (endDate) url += `&endDate=${endDate}`
      const data = await request.post(url, {}, { timeout: 300000 })
      return data
    } catch (error) {
      console.error('AI分析图片失败:', error)
      return null
    }
  }
  
  /**
   * 分析传感器数据（超时时间5分钟）
   */
  async function analyzeSensorData(limit = 30, startDate = null, endDate = null) {
    try {
      let url = `/ai/analyze-sensor-data?limit=${limit}`
      if (startDate) url += `&startDate=${startDate}`
      if (endDate) url += `&endDate=${endDate}`
      const data = await request.post(url, {}, { timeout: 300000 })
      return data
    } catch (error) {
      console.error('AI分析传感器数据失败:', error)
      return null
    }
  }
  
  /**
   * 获取自动化建议（超时时间5分钟）
   */
  async function getAutomationAdvice() {
    try {
      const data = await request.post('/ai/automation-advice', {}, { timeout: 300000 })
      return data
    } catch (error) {
      console.error('获取AI自动化建议失败:', error)
      return null
    }
  }
  
  /**
   * 生成综合报告（超时时间5分钟）
   */
  async function generateComprehensiveReport(startDate = null, endDate = null) {
    try {
      let url = '/ai/comprehensive-report'
      if (startDate) url += `?startDate=${startDate}`
      if (endDate) {
        url += startDate ? `&endDate=${endDate}` : `?endDate=${endDate}`
      }
      const data = await request.post(url, {}, { timeout: 300000 })
      return data
    } catch (error) {
      console.error('生成AI综合报告失败:', error)
      return null
    }
  }
  
  /**
   * 获取AI自动执行建议（超时时间5分钟）
   */
  async function getAutoExecutionAdvice() {
    try {
      const data = await request.post('/ai/auto-execution-advice', {}, { timeout: 300000 })
      return data
    } catch (error) {
      console.error('获取AI自动执行建议失败:', error)
      return null
    }
  }
  
  // ========== AI报告管理 ==========
  
  /**
   * 保存AI报告
   */
  async function saveAiReport(reportData) {
    try {
      const data = await request.post('/ai/reports', reportData)
      return data
    } catch (error) {
      console.error('保存AI报告失败:', error)
      return null
    }
  }
  
  /**
   * 获取所有AI报告
   */
  async function getAllAiReports(reportType = null, startDate = null, endDate = null) {
    try {
      let url = '/ai/reports'
      const params = []
      if (reportType) params.push(`reportType=${reportType}`)
      if (startDate) params.push(`startDate=${startDate}`)
      if (endDate) params.push(`endDate=${endDate}`)
      if (params.length > 0) url += '?' + params.join('&')
      const data = await request.get(url)
      return data
    } catch (error) {
      console.error('获取AI报告失败:', error)
      return null
    }
  }
  
  /**
   * 根据ID获取AI报告
   */
  async function getAiReportById(id) {
    try {
      const data = await request.get(`/ai/reports/${id}`)
      return data
    } catch (error) {
      console.error('获取AI报告失败:', error)
      return null
    }
  }
  
  /**
   * 删除AI报告
   */
  async function deleteAiReport(id) {
    try {
      await request.delete(`/ai/reports/${id}`)
      return true
    } catch (error) {
      console.error('删除AI报告失败:', error)
      return false
    }
  }
  
  /**
   * 获取自动报告开关状态
   */
  async function getAutoReportEnabled() {
    try {
      const data = await request.get('/ai/auto-report/enabled')
      // 处理可能的布尔值或字符串
      if (data === true || data === 'true' || data === 1) {
        return true
      }
      return false
    } catch (error) {
      console.error('获取自动报告开关失败:', error)
      return false
    }
  }
  
  /**
   * 设置自动报告开关
   */
  async function setAutoReportEnabled(enabled) {
    try {
      const data = await request.put(`/ai/auto-report/enabled?enabled=${enabled}`)
      // 处理可能的布尔值或字符串
      const result = data === true || data === 'true' || data === enabled
      return result
    } catch (error) {
      console.error('设置自动报告开关失败:', error)
      return false
    }
  }
  
  /**
   * 执行AI自动执行建议的操作（推送到MQTT）
   */
  async function executeAiAction(action) {
    try {
      const payload = { ...action }
      if (payload.type === 'light') {
        payload.type = 1
      } else if (payload.type === 'pump' || payload.type === 'water') {
        payload.type = 2
      } else if (payload.type === 'recipe' || payload.type === 'nutrient') {
        payload.type = 3
      }
      const data = await request.post('/ai/auto-execution-advice/execute', payload, { timeout: 10000 })
      return data !== null
    } catch (error) {
      console.error('执行AI操作失败:', error)
      return false
    }
  }
  
  // ========== AI托管功能 ==========
  
  /**
   * 获取AI托管配置
   */
  async function getAiHostingConfig() {
    try {
      const data = await request.get('/ai-hosting/config')
      return data
    } catch (error) {
      console.error('获取AI托管配置失败:', error)
      return null
    }
  }
  
  /**
   * 更新AI托管配置
   */
  async function updateAiHostingConfig(config) {
    try {
      const data = await request.put('/ai-hosting/config', config)
      return data
    } catch (error) {
      console.error('更新AI托管配置失败:', error)
      throw error
    }
  }
  
  /**
   * 获取AI托管执行日志
   */
  async function getAiHostingLogs(limit = 50) {
    try {
      const data = await request.get('/ai-hosting/logs', { limit })
      return data
    } catch (error) {
      console.error('获取AI托管日志失败:', error)
      return []
    }
  }
  
  /**
   * 获取AI托管统计信息
   */
  async function getAiHostingStats() {
    try {
      const data = await request.get('/ai-hosting/stats')
      return data
    } catch (error) {
      console.error('获取AI托管统计失败:', error)
      return {
        totalExecutions: 0,
        successCount: 0,
        failedCount: 0,
        partialCount: 0,
        successRate: 0
      }
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
    latestImage,
    imageStatusLogs,
    loadAllImages,
    loadAbnormalImages,
    loadLatestImage,
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
    executeSchedule,
    executeAssignment,
    executeFromLog,
    
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
    
    // AI功能
    analyzeImages,
    analyzeSensorData,
    getAutomationAdvice,
    generateComprehensiveReport,
    getAutoExecutionAdvice,
    
    // AI报告管理
    saveAiReport,
    getAllAiReports,
    getAiReportById,
    deleteAiReport,
    
    // AI自动报告
    getAutoReportEnabled,
    setAutoReportEnabled,
    
    // AI执行操作
    executeAiAction,
    
    // AI托管功能
    getAiHostingConfig,
    updateAiHostingConfig,
    getAiHostingLogs,
    getAiHostingStats
  }
})
