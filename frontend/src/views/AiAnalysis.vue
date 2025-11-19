<script setup>
import { ref, computed, onMounted } from 'vue'
import { useGreenhouseStore } from '../stores/greenhouse'
import { ElMessage } from 'element-plus'
import { MagicStick } from '@element-plus/icons-vue'
import { marked } from 'marked'

const store = useGreenhouseStore()

// 配置marked选项
marked.setOptions({
  breaks: true, // 支持换行
  gfm: true, // 支持GitHub风格的Markdown
})

// 报告内容
const imageReport = ref('')
const sensorReport = ref('')
const automationAdvice = ref('')
const comprehensiveReport = ref('')
const autoExecutionAdvice = ref(null)

// 日期选择
const dateRange = ref([])

// 自动报告开关
const autoReportEnabled = ref(false)
const loadingAutoReport = ref(false)

// 加载状态
const loading = ref({
  images: false,
  sensor: false,
  automation: false,
  comprehensive: false,
  autoExecution: false
})

// 格式化日期为 YYYY-MM-DD
function formatDate(date) {
  if (!date) return null
  const d = new Date(date)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

// 设置今天
function setToday() {
  const today = new Date()
  dateRange.value = [formatDate(today), formatDate(today)]
}

// 设置昨天
function setYesterday() {
  const yesterday = new Date()
  yesterday.setDate(yesterday.getDate() - 1)
  dateRange.value = [formatDate(yesterday), formatDate(yesterday)]
}

// 设置最近7天
function setLast7Days() {
  const end = new Date()
  const start = new Date()
  start.setDate(start.getDate() - 6) // 包括今天，所以是6天前
  dateRange.value = [formatDate(start), formatDate(end)]
}

// 加载自动报告开关状态
async function loadAutoReportStatus() {
  try {
    const enabled = await store.getAutoReportEnabled()
    console.log('获取到的自动报告状态:', enabled, typeof enabled)
    autoReportEnabled.value = enabled === true || enabled === 'true' || enabled === 1
  } catch (error) {
    console.error('加载自动报告状态失败:', error)
    autoReportEnabled.value = false
  }
}

// 切换自动报告开关
async function toggleAutoReport(newValue) {
  loadingAutoReport.value = true
  try {
    const success = await store.setAutoReportEnabled(newValue)
    if (success) {
      autoReportEnabled.value = newValue
      ElMessage.success(newValue ? '自动报告已开启' : '自动报告已关闭')
    } else {
      // 如果设置失败，恢复原状态
      autoReportEnabled.value = !newValue
      ElMessage.error('设置失败')
    }
  } catch (error) {
    // 如果出错，恢复原状态
    autoReportEnabled.value = !newValue
    ElMessage.error('设置失败: ' + error.message)
  } finally {
    loadingAutoReport.value = false
  }
}

// 分析图片集
async function handleAnalyzeImages() {
  loading.value.images = true
  imageReport.value = ''
  try {
    const startDate = dateRange.value && dateRange.value.length > 0 ? formatDate(dateRange.value[0]) : null
    const endDate = dateRange.value && dateRange.value.length > 1 ? formatDate(dateRange.value[1]) : null
    await startSseRequest('/ai/analyze-images-stream', {
      limit: 30,
      startDate,
      endDate
    }, {
      onChunk: data => {
        if (data?.content) {
          imageReport.value += data.content
        }
      }
    })
    ElMessage.success('图片分析完成')
  } catch (error) {
    ElMessage.error('图片分析失败: ' + (error?.message || '未知错误'))
  } finally {
    loading.value.images = false
  }
}

// 保存图片分析报告
async function saveImageReport() {
  if (!imageReport.value) {
    ElMessage.warning('请先生成报告')
    return
  }
  try {
    const startDate = dateRange.value && dateRange.value.length > 0 ? formatDate(dateRange.value[0]) : null
    const endDate = dateRange.value && dateRange.value.length > 1 ? formatDate(dateRange.value[1]) : null
    const reportData = {
      reportType: 'image_analysis',
      reportTitle: `图片分析报告${startDate && endDate ? ` (${startDate} 至 ${endDate})` : ''}`,
      reportContent: imageReport.value,
      startDate: startDate,
      endDate: endDate,
      dataCount: 30
    }
    const result = await store.saveAiReport(reportData)
    if (result) {
      ElMessage.success('报告保存成功')
    } else {
      ElMessage.error('报告保存失败')
    }
  } catch (error) {
    ElMessage.error('保存报告失败: ' + error.message)
  }
}

// 分析传感器数据
async function handleAnalyzeSensorData() {
  loading.value.sensor = true
  sensorReport.value = ''
  try {
    const startDate = dateRange.value && dateRange.value.length > 0 ? formatDate(dateRange.value[0]) : null
    const endDate = dateRange.value && dateRange.value.length > 1 ? formatDate(dateRange.value[1]) : null
    await startSseRequest('/ai/analyze-sensor-data-stream', {
      limit: 30,
      startDate,
      endDate
    }, {
      onChunk: data => {
        if (data?.content) {
          sensorReport.value += data.content
        }
      }
    })
    ElMessage.success('传感器数据分析完成')
  } catch (error) {
    ElMessage.error('传感器数据分析失败: ' + (error?.message || '未知错误'))
  } finally {
    loading.value.sensor = false
  }
}

// 保存传感器数据分析报告
async function saveSensorReport() {
  if (!sensorReport.value) {
    ElMessage.warning('请先生成报告')
    return
  }
  try {
    const startDate = dateRange.value && dateRange.value.length > 0 ? formatDate(dateRange.value[0]) : null
    const endDate = dateRange.value && dateRange.value.length > 1 ? formatDate(dateRange.value[1]) : null
    const reportData = {
      reportType: 'sensor_analysis',
      reportTitle: `传感器数据分析报告${startDate && endDate ? ` (${startDate} 至 ${endDate})` : ''}`,
      reportContent: sensorReport.value,
      startDate: startDate,
      endDate: endDate,
      dataCount: 30
    }
    const result = await store.saveAiReport(reportData)
    if (result) {
      ElMessage.success('报告保存成功')
    } else {
      ElMessage.error('报告保存失败')
    }
  } catch (error) {
    ElMessage.error('保存报告失败: ' + error.message)
  }
}

// 获取自动化建议
async function handleGetAutomationAdvice() {
  loading.value.automation = true
  automationAdvice.value = ''
  try {
    await startSseRequest('/ai/automation-advice-stream', {}, {
      onChunk: data => {
        if (data?.content) {
          automationAdvice.value += data.content
        }
      }
    })
    ElMessage.success('自动化建议生成完成')
  } catch (error) {
    ElMessage.error('获取自动化建议失败: ' + (error?.message || '未知错误'))
  } finally {
    loading.value.automation = false
  }
}

// 保存自动化建议报告
async function saveAutomationReport() {
  if (!automationAdvice.value) {
    ElMessage.warning('请先生成建议')
    return
  }
  try {
    const reportData = {
      reportType: 'automation_advice',
      reportTitle: '自动化控制建议',
      reportContent: automationAdvice.value,
      startDate: null,
      endDate: null,
      dataCount: 0
    }
    const result = await store.saveAiReport(reportData)
    if (result) {
      ElMessage.success('报告保存成功')
    } else {
      ElMessage.error('报告保存失败')
    }
  } catch (error) {
    ElMessage.error('保存报告失败: ' + error.message)
  }
}

// 生成综合报告
async function handleGenerateComprehensiveReport() {
  loading.value.comprehensive = true
  comprehensiveReport.value = ''
  try {
    const startDate = dateRange.value && dateRange.value.length > 0 ? formatDate(dateRange.value[0]) : null
    const endDate = dateRange.value && dateRange.value.length > 1 ? formatDate(dateRange.value[1]) : null
    await startSseRequest('/ai/comprehensive-report-stream', {
      startDate,
      endDate
    }, {
      onChunk: data => {
        if (data?.content) {
          comprehensiveReport.value += data.content
        }
      }
    })
    ElMessage.success('综合报告生成完成')
  } catch (error) {
    ElMessage.error('生成综合报告失败: ' + (error?.message || '未知错误'))
  } finally {
    loading.value.comprehensive = false
  }
}

// 保存综合报告
async function saveComprehensiveReport() {
  if (!comprehensiveReport.value) {
    ElMessage.warning('请先生成报告')
    return
  }
  try {
    const startDate = dateRange.value && dateRange.value.length > 0 ? formatDate(dateRange.value[0]) : null
    const endDate = dateRange.value && dateRange.value.length > 1 ? formatDate(dateRange.value[1]) : null
    const reportData = {
      reportType: 'comprehensive_report',
      reportTitle: `综合管理报告${startDate && endDate ? ` (${startDate} 至 ${endDate})` : ''}`,
      reportContent: comprehensiveReport.value,
      startDate: startDate,
      endDate: endDate,
      dataCount: 0
    }
    const result = await store.saveAiReport(reportData)
    if (result) {
      ElMessage.success('报告保存成功')
    } else {
      ElMessage.error('报告保存失败')
    }
  } catch (error) {
    ElMessage.error('保存报告失败: ' + error.message)
  }
}

// 获取AI自动执行建议
async function handleGetAutoExecutionAdvice() {
  loading.value.autoExecution = true
  autoExecutionAdvice.value = null
  let summaryContent = ''
  try {
    await startSseRequest('/ai/auto-execution-advice-stream', {}, {
      onChunk: data => {
        if (data?.content) {
          summaryContent += data.content
          autoExecutionAdvice.value = {
            summary: summaryContent,
            actions: []
          }
        }
      },
      onComplete: data => {
        if (data?.data) {
          autoExecutionAdvice.value = data.data
        } else if (summaryContent) {
          autoExecutionAdvice.value = {
            summary: summaryContent,
            actions: []
          }
        }
      }
    })
    ElMessage.success('AI自动执行建议获取完成')
  } catch (error) {
    ElMessage.error('获取AI自动执行建议失败: ' + (error?.message || '未知错误'))
  } finally {
    loading.value.autoExecution = false
  }
}

// 执行AI建议的操作（推送到MQTT）
async function executeAiAction(action) {
  try {
    const result = await store.executeAiAction(action)
    if (result) {
      ElMessage.success('操作已发送到MQTT')
    } else {
      ElMessage.error('发送操作失败')
    }
  } catch (error) {
    ElMessage.error('执行操作失败: ' + error.message)
  }
}

// 保存AI自动执行建议报告
async function saveAutoExecutionReport() {
  if (!autoExecutionAdvice.value) {
    ElMessage.warning('请先生成建议')
    return
  }
  try {
    // 将建议转换为Markdown格式
    let reportContent = ''
    if (autoExecutionAdvice.value.summary) {
      reportContent += `## 执行建议总结\n\n${autoExecutionAdvice.value.summary}\n\n`
    }
    if (autoExecutionAdvice.value.actions && autoExecutionAdvice.value.actions.length > 0) {
      reportContent += `## 具体操作建议\n\n`
      autoExecutionAdvice.value.actions.forEach((action, index) => {
        reportContent += `### 操作 ${index + 1}\n\n`
        reportContent += `- **操作类型**: ${action.type}\n`
        if (action.action) {
          reportContent += `- **动作**: ${action.action}\n`
        }
        if (action.plotId) {
          reportContent += `- **地块ID**: ${action.plotId}\n`
        }
        if (action.recipeId) {
          reportContent += `- **配方ID**: ${action.recipeId}\n`
        }
        if (action.executions) {
          reportContent += `- **执行次数**: ${action.executions}\n`
        }
        if (action.reason) {
          reportContent += `- **原因**: ${action.reason}\n`
        }
        reportContent += `\n`
      })
    }
    
    const reportData = {
      reportType: 'auto_execution',
      reportTitle: 'AI自动执行建议',
      reportContent: reportContent,
      startDate: null,
      endDate: null,
      dataCount: autoExecutionAdvice.value.actions ? autoExecutionAdvice.value.actions.length : 0
    }
    const result = await store.saveAiReport(reportData)
    if (result) {
      ElMessage.success('报告保存成功')
    } else {
      ElMessage.error('报告保存失败')
    }
  } catch (error) {
    ElMessage.error('保存报告失败: ' + error.message)
  }
}

// 将Markdown转换为HTML
function renderMarkdown(text) {
  if (!text) return ''
  try {
    return marked.parse(text)
  } catch (error) {
    console.error('Markdown渲染失败:', error)
    return text
  }
}

// 计算属性：渲染后的HTML
const imageReportHtml = computed(() => renderMarkdown(imageReport.value))
const sensorReportHtml = computed(() => renderMarkdown(sensorReport.value))
const automationAdviceHtml = computed(() => renderMarkdown(automationAdvice.value))
const comprehensiveReportHtml = computed(() => renderMarkdown(comprehensiveReport.value))
const autoExecutionAdviceHtml = computed(() => {
  if (!autoExecutionAdvice.value?.summary) return ''
  return renderMarkdown(autoExecutionAdvice.value.summary)
})

const apiBase = import.meta.env.VITE_API_BASE_URL || '/api'

function buildSseUrl(path, params = {}) {
  const base = apiBase.endsWith('/') ? apiBase.slice(0, -1) : apiBase
  // 为SSE请求添加token参数（EventSource不支持自定义header）
  const token = localStorage.getItem('jwt')
  if (token) {
    params.token = token
  }
  const finalPath = path.startsWith('/') ? path : `/${path}`
  const url = new URL(`${base}${finalPath}`, window.location.origin)
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      url.searchParams.append(key, value)
    }
  })
  return url.toString()
}

function safeJsonParse(text) {
  if (!text) return null
  try {
    return JSON.parse(text)
  } catch (error) {
    return { content: text }
  }
}

function startSseRequest(path, params = {}, handlers = {}) {
  return new Promise((resolve, reject) => {
    const url = buildSseUrl(path, params)
    const eventSource = new EventSource(url)

    const cleanup = () => {
      eventSource.close()
    }

    eventSource.addEventListener('start', event => {
      const data = safeJsonParse(event.data)
      handlers.onStart?.(data)
    })

    eventSource.addEventListener('chunk', event => {
      const data = safeJsonParse(event.data)
      handlers.onChunk?.(data)
    })

    eventSource.addEventListener('complete', event => {
      const data = safeJsonParse(event.data)
      handlers.onComplete?.(data)
      cleanup()
      resolve(data)
    })

    eventSource.addEventListener('error', event => {
      const data = safeJsonParse(event.data)
      handlers.onError?.(data)
      cleanup()
      reject(data?.error ? new Error(data.error) : new Error('分析失败'))
    })

    eventSource.onerror = () => {
      handlers.onError?.()
      cleanup()
      reject(new Error('SSE 连接异常'))
    }
  })
}

// 一键分析（流式）
const analyzingAll = ref(false)
const analyzeProgress = ref({
  image_analysis: false,
  sensor_analysis: false,
  automation_advice: false,
  comprehensive_report: false,
  auto_execution: false
})

async function handleAnalyzeAll() {
  analyzingAll.value = true
  // 清空之前的报告
  imageReport.value = ''
  sensorReport.value = ''
  automationAdvice.value = ''
  comprehensiveReport.value = ''
  autoExecutionAdvice.value = null
  
  // 重置进度
  analyzeProgress.value = {
    image_analysis: false,
    sensor_analysis: false,
    automation_advice: false,
    comprehensive_report: false,
    auto_execution: false
  }
  
  try {
    const startDate = dateRange.value && dateRange.value.length > 0 ? formatDate(dateRange.value[0]) : null
    const endDate = dateRange.value && dateRange.value.length > 1 ? formatDate(dateRange.value[1]) : null
    
    const baseURL = apiBase
    const token = localStorage.getItem('jwt')
    const tokenParam = token ? `&token=${encodeURIComponent(token)}` : ''
    
    // 同时调用5个流式接口
    const eventSources = []
    const completedTasks = { count: 0, total: 5 }
    
    // 1. 图片分析
    let imageUrl = `${baseURL}/ai/analyze-images-stream?limit=30${tokenParam}`
    if (startDate && endDate) {
      imageUrl += `&startDate=${startDate}&endDate=${endDate}`
    }
    const es1 = new EventSource(imageUrl)
    let imageContent = ''
    es1.addEventListener('start', () => {
      analyzeProgress.value.image_analysis = true
      ElMessage.info('开始图片分析...')
    })
    es1.addEventListener('chunk', (event) => {
      const data = JSON.parse(event.data)
      if (data.content) {
        imageContent += data.content
        imageReport.value = imageContent
      }
    })
    es1.addEventListener('complete', () => {
      analyzeProgress.value.image_analysis = false
      es1.close()
      completedTasks.count++
      ElMessage.success('图片分析完成')
      if (completedTasks.count === completedTasks.total) {
        analyzingAll.value = false
        ElMessage.success('所有分析完成！')
      }
    })
    es1.addEventListener('error', (event) => {
      const data = JSON.parse(event.data)
      ElMessage.error('图片分析失败: ' + (data.error || '未知错误'))
      es1.close()
      completedTasks.count++
      if (completedTasks.count === completedTasks.total) {
        analyzingAll.value = false
      }
    })
    eventSources.push(es1)
    
    // 2. 传感器数据分析
    let sensorUrl = `${baseURL}/ai/analyze-sensor-data-stream?limit=30${tokenParam}`
    if (startDate && endDate) {
      sensorUrl += `&startDate=${startDate}&endDate=${endDate}`
    }
    const es2 = new EventSource(sensorUrl)
    let sensorContent = ''
    es2.addEventListener('start', () => {
      analyzeProgress.value.sensor_analysis = true
      ElMessage.info('开始传感器数据分析...')
    })
    es2.addEventListener('chunk', (event) => {
      const data = JSON.parse(event.data)
      if (data.content) {
        sensorContent += data.content
        sensorReport.value = sensorContent
      }
    })
    es2.addEventListener('complete', () => {
      analyzeProgress.value.sensor_analysis = false
      es2.close()
      completedTasks.count++
      ElMessage.success('传感器数据分析完成')
      if (completedTasks.count === completedTasks.total) {
        analyzingAll.value = false
        ElMessage.success('所有分析完成！')
      }
    })
    es2.addEventListener('error', (event) => {
      const data = JSON.parse(event.data)
      ElMessage.error('传感器数据分析失败: ' + (data.error || '未知错误'))
      es2.close()
      completedTasks.count++
      if (completedTasks.count === completedTasks.total) {
        analyzingAll.value = false
      }
    })
    eventSources.push(es2)
    
    // 3. 自动化建议
    const es3 = new EventSource(`${baseURL}/ai/automation-advice-stream${tokenParam ? '?' + tokenParam.substring(1) : ''}`)
    let automationContent = ''
    es3.addEventListener('start', () => {
      analyzeProgress.value.automation_advice = true
      ElMessage.info('开始自动化建议分析...')
    })
    es3.addEventListener('chunk', (event) => {
      const data = JSON.parse(event.data)
      if (data.content) {
        automationContent += data.content
        automationAdvice.value = automationContent
      }
    })
    es3.addEventListener('complete', () => {
      analyzeProgress.value.automation_advice = false
      es3.close()
      completedTasks.count++
      ElMessage.success('自动化建议完成')
      if (completedTasks.count === completedTasks.total) {
        analyzingAll.value = false
        ElMessage.success('所有分析完成！')
      }
    })
    es3.addEventListener('error', (event) => {
      const data = JSON.parse(event.data)
      ElMessage.error('自动化建议失败: ' + (data.error || '未知错误'))
      es3.close()
      completedTasks.count++
      if (completedTasks.count === completedTasks.total) {
        analyzingAll.value = false
      }
    })
    eventSources.push(es3)
    
    // 4. 综合报告
    let comprehensiveUrl = `${baseURL}/ai/comprehensive-report-stream`
    if (startDate && endDate) {
      comprehensiveUrl += `?startDate=${startDate}&endDate=${endDate}${tokenParam}`
    } else {
      comprehensiveUrl += tokenParam ? `?${tokenParam.substring(1)}` : ''
    }
    const es4 = new EventSource(comprehensiveUrl)
    let comprehensiveContent = ''
    es4.addEventListener('start', () => {
      analyzeProgress.value.comprehensive_report = true
      ElMessage.info('开始综合报告生成...')
    })
    es4.addEventListener('chunk', (event) => {
      const data = JSON.parse(event.data)
      if (data.content) {
        comprehensiveContent += data.content
        comprehensiveReport.value = comprehensiveContent
      }
    })
    es4.addEventListener('complete', () => {
      analyzeProgress.value.comprehensive_report = false
      es4.close()
      completedTasks.count++
      ElMessage.success('综合报告完成')
      if (completedTasks.count === completedTasks.total) {
        analyzingAll.value = false
        ElMessage.success('所有分析完成！')
      }
    })
    es4.addEventListener('error', (event) => {
      const data = JSON.parse(event.data)
      ElMessage.error('综合报告失败: ' + (data.error || '未知错误'))
      es4.close()
      completedTasks.count++
      if (completedTasks.count === completedTasks.total) {
        analyzingAll.value = false
      }
    })
    eventSources.push(es4)
    
    // 5. AI自动执行建议
    const es5 = new EventSource(`${baseURL}/ai/auto-execution-advice-stream${tokenParam ? '?' + tokenParam.substring(1) : ''}`)
    let autoExecutionContent = ''
    es5.addEventListener('start', () => {
      analyzeProgress.value.auto_execution = true
      ElMessage.info('开始AI自动执行建议分析...')
    })
    es5.addEventListener('chunk', (event) => {
      const data = JSON.parse(event.data)
      if (data.content) {
        autoExecutionContent += data.content
        // 对于auto-execution，我们只显示summary部分
        if (!autoExecutionAdvice.value) {
          autoExecutionAdvice.value = { summary: '', actions: [] }
        }
        autoExecutionAdvice.value.summary = autoExecutionContent
      }
    })
    es5.addEventListener('complete', (event) => {
      try {
        const data = JSON.parse(event.data)
        if (data.data) {
          autoExecutionAdvice.value = data.data
        }
      } catch (e) {
        console.error('解析auto-execution数据失败:', e)
      }
      analyzeProgress.value.auto_execution = false
      es5.close()
      completedTasks.count++
      ElMessage.success('AI自动执行建议完成')
      if (completedTasks.count === completedTasks.total) {
        analyzingAll.value = false
        ElMessage.success('所有分析完成！')
      }
    })
    es5.addEventListener('error', (event) => {
      const data = JSON.parse(event.data)
      ElMessage.error('AI自动执行建议失败: ' + (data.error || '未知错误'))
      es5.close()
      completedTasks.count++
      if (completedTasks.count === completedTasks.total) {
        analyzingAll.value = false
      }
    })
    eventSources.push(es5)
    
    // 错误处理
    eventSources.forEach(es => {
      es.onerror = (error) => {
        console.error('SSE连接错误:', error)
      }
    })
    
  } catch (error) {
    analyzingAll.value = false
    ElMessage.error('一键分析失败: ' + error.message)
  }
}

function getTypeName(type) {
  const names = {
    'image_analysis': '图片分析',
    'sensor_analysis': '传感器数据分析',
    'automation_advice': '自动化建议',
    'comprehensive_report': '综合报告'
  }
  return names[type] || type
}

// 页面加载时获取自动报告状态并设置默认日期为今天
onMounted(() => {
  loadAutoReportStatus()
  setToday() // 默认设置为今天
})
</script>

<template>
  <div>
    <el-card shadow="never" style="margin-bottom: 16px;">
      <template #header>
        <div style="display: flex; align-items: center; justify-content: space-between;">
          <span style="font-weight: 600; font-size: 18px;">AI自动化分析</span>
          <div style="display: flex; align-items: center; gap: 12px;">
            <el-button 
              type="primary" 
              size="default"
              :loading="analyzingAll"
              @click="handleAnalyzeAll"
              style="margin-right: 16px;"
            >
              <el-icon style="margin-right: 4px;"><MagicStick /></el-icon>
              一键分析
            </el-button>
            <span style="font-size: 14px; color: #606266;">每日自动生成报告：</span>
            <el-switch
              v-model="autoReportEnabled"
              :loading="loadingAutoReport"
              @change="toggleAutoReport"
              active-text="开启"
              inactive-text="关闭"
            />
          </div>
        </div>
      </template>
      
      <!-- 日期选择器 -->
      <el-card shadow="never" style="margin-bottom: 16px;">
        <el-form inline>
          <el-form-item label="时间范围">
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              style="width: 300px;"
            />
          </el-form-item>
          <el-form-item>
            <el-button size="small" @click="dateRange = []">清除选择</el-button>
            <el-button size="small" type="primary" @click="setToday">今天</el-button>
            <el-button size="small" type="primary" @click="setYesterday">昨天</el-button>
            <el-button size="small" type="primary" @click="setLast7Days">最近7天</el-button>
          </el-form-item>
        </el-form>
        <div v-if="dateRange && dateRange.length === 2" style="margin-top: 8px; color: #909399; font-size: 12px;">
          已选择：{{ dateRange[0] }} 至 {{ dateRange[1] }}
        </div>
        <div v-else style="margin-top: 8px; color: #909399; font-size: 12px;">
          未选择日期范围，将使用全部数据
        </div>
      </el-card>
      
      <el-row :gutter="16">
        <!-- 图片分析 -->
        <el-col :span="12">
          <el-card shadow="never" style="margin-bottom: 16px;">
            <template #header>
              <div style="display: flex; align-items: center; justify-content: space-between;">
                <span>图片集分析报告</span>
                <div>
                  <el-button 
                    type="primary" 
                    size="small" 
                    :loading="loading.images"
                    @click="handleAnalyzeImages"
                  >
                    生成报告
                  </el-button>
                  <el-button 
                    v-if="imageReport"
                    type="success" 
                    size="small" 
                    @click="saveImageReport"
                    style="margin-left: 8px;"
                  >
                    保存报告
                  </el-button>
                </div>
              </div>
            </template>
            <div 
              v-if="imageReport" 
              class="markdown-content"
              v-html="imageReportHtml"
            ></div>
            <el-empty v-else description="点击按钮生成图片分析报告" />
          </el-card>
        </el-col>
        
        <!-- 传感器数据分析 -->
        <el-col :span="12">
          <el-card shadow="never" style="margin-bottom: 16px;">
            <template #header>
              <div style="display: flex; align-items: center; justify-content: space-between;">
                <span>传感器数据分析报告</span>
                <div>
                  <el-button 
                    type="primary" 
                    size="small" 
                    :loading="loading.sensor"
                    @click="handleAnalyzeSensorData"
                  >
                    生成报告
                  </el-button>
                  <el-button 
                    v-if="sensorReport"
                    type="success" 
                    size="small" 
                    @click="saveSensorReport"
                    style="margin-left: 8px;"
                  >
                    保存报告
                  </el-button>
                </div>
              </div>
            </template>
            <div 
              v-if="sensorReport" 
              class="markdown-content"
              v-html="sensorReportHtml"
            ></div>
            <el-empty v-else description="点击按钮生成传感器数据分析报告" />
          </el-card>
        </el-col>
      </el-row>
      
      <!-- 自动化建议 -->
      <el-card shadow="never" style="margin-bottom: 16px;">
        <template #header>
          <div style="display: flex; align-items: center; justify-content: space-between;">
            <span>自动化控制建议</span>
            <div>
              <el-button 
                type="success" 
                size="small" 
                :loading="loading.automation"
                @click="handleGetAutomationAdvice"
              >
                获取建议
              </el-button>
              <el-button 
                v-if="automationAdvice"
                type="primary" 
                size="small" 
                @click="saveAutomationReport"
                style="margin-left: 8px;"
              >
                保存报告
              </el-button>
            </div>
          </div>
        </template>
        <div 
          v-if="automationAdvice" 
          class="markdown-content markdown-content-small"
          v-html="automationAdviceHtml"
        ></div>
        <el-empty v-else description="点击按钮获取自动化控制建议" />
      </el-card>
      
      <!-- AI自动执行建议 -->
      <el-card shadow="never" style="margin-bottom: 16px;">
        <template #header>
          <div style="display: flex; align-items: center; justify-content: space-between;">
            <span>AI自动执行建议</span>
            <div>
              <el-button 
                type="warning" 
                size="small" 
                :loading="loading.autoExecution"
                @click="handleGetAutoExecutionAdvice"
              >
                获取执行建议
              </el-button>
              <el-button 
                v-if="autoExecutionAdvice"
                type="success" 
                size="small" 
                @click="saveAutoExecutionReport"
                style="margin-left: 8px;"
              >
                保存报告
              </el-button>
            </div>
          </div>
        </template>
        <div v-if="autoExecutionAdvice">
          <div 
            v-if="autoExecutionAdvice.summary" 
            class="markdown-content"
            style="margin-bottom: 16px;"
          >
            <strong>建议总结：</strong><br>
            <div v-html="autoExecutionAdviceHtml"></div>
          </div>
          <div v-if="autoExecutionAdvice.actions && autoExecutionAdvice.actions.length > 0">
            <el-table :data="autoExecutionAdvice.actions" border size="small">
              <el-table-column prop="type" label="操作类型" width="120" />
              <el-table-column prop="action" label="动作" width="100" />
              <el-table-column prop="plotId" label="地块" width="80" />
              <el-table-column prop="recipeId" label="配方ID" width="120" />
              <el-table-column prop="executions" label="执行次数" width="100" />
              <el-table-column prop="reason" label="原因" />
              <el-table-column label="操作" width="120">
                <template #default="{ row }">
                  <el-button 
                    link 
                    type="primary" 
                    size="small" 
                    @click="executeAiAction(row)"
                  >
                    执行
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
        <el-empty v-else description="点击按钮获取AI自动执行建议" />
      </el-card>
      
      <!-- 综合报告 -->
      <el-card shadow="never">
        <template #header>
          <div style="display: flex; align-items: center; justify-content: space-between;">
            <span>综合管理报告</span>
            <div>
              <el-button 
                type="info" 
                size="small" 
                :loading="loading.comprehensive"
                @click="handleGenerateComprehensiveReport"
              >
                生成综合报告
              </el-button>
              <el-button 
                v-if="comprehensiveReport"
                type="success" 
                size="small" 
                @click="saveComprehensiveReport"
                style="margin-left: 8px;"
              >
                保存报告
              </el-button>
            </div>
          </div>
        </template>
        <div 
          v-if="comprehensiveReport" 
          class="markdown-content markdown-content-large"
          v-html="comprehensiveReportHtml"
        ></div>
        <el-empty v-else description="点击按钮生成综合管理报告" />
      </el-card>
    </el-card>
  </div>
</template>

<style scoped>
/* Markdown内容样式 */
.markdown-content {
  max-height: 400px;
  overflow-y: auto;
  padding: 16px;
  background: #ffffff;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
  line-height: 1.8;
  color: #303133;
}

.markdown-content-small {
  max-height: 300px;
}

.markdown-content-large {
  max-height: 500px;
}

/* Markdown标题样式 */
.markdown-content :deep(h1),
.markdown-content :deep(h2),
.markdown-content :deep(h3),
.markdown-content :deep(h4),
.markdown-content :deep(h5),
.markdown-content :deep(h6) {
  margin-top: 20px;
  margin-bottom: 12px;
  font-weight: 600;
  color: #303133;
  line-height: 1.4;
}

.markdown-content :deep(h1) {
  font-size: 24px;
  border-bottom: 2px solid #e4e7ed;
  padding-bottom: 8px;
}

.markdown-content :deep(h2) {
  font-size: 20px;
  border-bottom: 1px solid #e4e7ed;
  padding-bottom: 6px;
}

.markdown-content :deep(h3) {
  font-size: 18px;
}

.markdown-content :deep(h4) {
  font-size: 16px;
}

/* 段落样式 */
.markdown-content :deep(p) {
  margin: 12px 0;
  line-height: 1.8;
}

/* 列表样式 */
.markdown-content :deep(ul),
.markdown-content :deep(ol) {
  margin: 12px 0;
  padding-left: 24px;
}

.markdown-content :deep(li) {
  margin: 6px 0;
  line-height: 1.8;
}

/* 表格样式 */
.markdown-content :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 16px 0;
  font-size: 14px;
}

.markdown-content :deep(table th),
.markdown-content :deep(table td) {
  border: 1px solid #e4e7ed;
  padding: 10px 12px;
  text-align: left;
}

.markdown-content :deep(table th) {
  background-color: #f5f7fa;
  font-weight: 600;
  color: #303133;
}

.markdown-content :deep(table tr:nth-child(even)) {
  background-color: #fafafa;
}

.markdown-content :deep(table tr:hover) {
  background-color: #f0f9ff;
}

/* 强调样式 */
.markdown-content :deep(strong) {
  font-weight: 600;
  color: #303133;
}

.markdown-content :deep(em) {
  font-style: italic;
}

/* 代码样式 */
.markdown-content :deep(code) {
  background-color: #f5f5f5;
  padding: 2px 6px;
  border-radius: 3px;
  font-family: 'Courier New', monospace;
  font-size: 0.9em;
  color: #e83e8c;
}

.markdown-content :deep(pre) {
  background-color: #f5f5f5;
  padding: 12px;
  border-radius: 4px;
  overflow-x: auto;
  margin: 12px 0;
}

.markdown-content :deep(pre code) {
  background-color: transparent;
  padding: 0;
  color: #303133;
}

/* 引用样式 */
.markdown-content :deep(blockquote) {
  border-left: 4px solid #409eff;
  padding-left: 16px;
  margin: 12px 0;
  color: #606266;
  background-color: #f0f9ff;
  padding: 12px 16px;
  border-radius: 4px;
}

/* 分隔线 */
.markdown-content :deep(hr) {
  border: none;
  border-top: 1px solid #e4e7ed;
  margin: 20px 0;
}

/* 链接样式 */
.markdown-content :deep(a) {
  color: #409eff;
  text-decoration: none;
}

.markdown-content :deep(a:hover) {
  text-decoration: underline;
}
</style>

