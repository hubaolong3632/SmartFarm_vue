<script setup>
import { ref, onMounted, computed } from 'vue'
import { useGreenhouseStore } from '../stores/greenhouse'
import { ElMessage, ElMessageBox } from 'element-plus'
import { marked } from 'marked'

const store = useGreenhouseStore()

// 配置marked选项
marked.setOptions({
  breaks: true,
  gfm: true,
})

// 报告列表
const reports = ref([])
const loading = ref(false)
const selectedReport = ref(null)
const showDetail = ref(false)

// 筛选条件
const filterType = ref('')
const filterStartDate = ref('')
const filterEndDate = ref('')

// 报告类型选项
const reportTypes = [
  { label: '全部', value: '' },
  { label: '图片分析', value: 'image_analysis' },
  { label: '传感器数据分析', value: 'sensor_analysis' },
  { label: '自动化建议', value: 'automation_advice' },
  { label: '综合报告', value: 'comprehensive_report' },
  { label: '自动执行建议', value: 'auto_execution' }
]

// 加载报告列表
async function loadReports() {
  loading.value = true
  try {
    const startDate = filterStartDate.value || null
    const endDate = filterEndDate.value || null
    const type = filterType.value || null
    const data = await store.getAllAiReports(type, startDate, endDate)
    if (data) {
      reports.value = data
    }
  } catch (error) {
    ElMessage.error('加载报告失败: ' + error.message)
  } finally {
    loading.value = false
  }
}

// 查看报告详情
function viewReport(report) {
  selectedReport.value = report
  showDetail.value = true
}

// 删除报告
async function deleteReport(id) {
  try {
    await ElMessageBox.confirm('确定要删除此报告吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const success = await store.deleteAiReport(id)
    if (success) {
      ElMessage.success('删除成功')
      loadReports()
    } else {
      ElMessage.error('删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败: ' + error.message)
    }
  }
}

// 获取报告类型名称
function getReportTypeName(type) {
  const typeMap = {
    'image_analysis': '图片分析',
    'sensor_analysis': '传感器数据分析',
    'automation_advice': '自动化建议',
    'comprehensive_report': '综合报告',
    'auto_execution': '自动执行建议'
  }
  return typeMap[type] || type
}

// 格式化日期
function formatDate(date) {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

// 渲染Markdown
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
const selectedReportHtml = computed(() => {
  if (!selectedReport.value) return ''
  return renderMarkdown(selectedReport.value.reportContent)
})

onMounted(() => {
  loadReports()
})
</script>

<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <div style="display: flex; align-items: center; justify-content: space-between;">
          <span style="font-weight: 600; font-size: 18px;">AI分析报告历史</span>
          <el-button type="primary" size="small" @click="loadReports">刷新</el-button>
        </div>
      </template>
      
      <!-- 筛选条件 -->
      <el-form inline style="margin-bottom: 16px;">
        <el-form-item label="报告类型">
          <el-select v-model="filterType" placeholder="全部" style="width: 150px;" clearable>
            <el-option
              v-for="type in reportTypes"
              :key="type.value"
              :label="type.label"
              :value="type.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="开始日期">
          <el-date-picker
            v-model="filterStartDate"
            type="date"
            placeholder="选择日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 150px;"
            clearable
          />
        </el-form-item>
        <el-form-item label="结束日期">
          <el-date-picker
            v-model="filterEndDate"
            type="date"
            placeholder="选择日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 150px;"
            clearable
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="small" @click="loadReports">查询</el-button>
          <el-button size="small" @click="filterType = ''; filterStartDate = ''; filterEndDate = ''; loadReports()">重置</el-button>
        </el-form-item>
      </el-form>
      
      <!-- 报告列表 -->
      <el-table :data="reports" border v-loading="loading" style="width: 100%;">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="报告类型" width="150">
          <template #default="{ row }">
            {{ getReportTypeName(row.reportType) }}
          </template>
        </el-table-column>
        <el-table-column prop="reportTitle" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column label="数据日期" width="200">
          <template #default="{ row }">
            <span v-if="row.startDate && row.endDate">
              {{ formatDate(row.startDate) }} 至 {{ formatDate(row.endDate) }}
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="dataCount" label="数据条数" width="100" />
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="viewReport(row)">查看</el-button>
            <el-button link type="danger" size="small" @click="deleteReport(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    
    <!-- 报告详情对话框 -->
    <el-dialog
      v-model="showDetail"
      title="报告详情"
      width="80%"
      :close-on-click-modal="false"
    >
      <div v-if="selectedReport">
        <el-descriptions :column="2" border style="margin-bottom: 16px;">
          <el-descriptions-item label="报告类型">
            {{ getReportTypeName(selectedReport.reportType) }}
          </el-descriptions-item>
          <el-descriptions-item label="标题">
            {{ selectedReport.reportTitle || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="数据日期">
            <span v-if="selectedReport.startDate && selectedReport.endDate">
              {{ formatDate(selectedReport.startDate) }} 至 {{ formatDate(selectedReport.endDate) }}
            </span>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="数据条数">
            {{ selectedReport.dataCount || 0 }}
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ formatDate(selectedReport.createdAt) }}
          </el-descriptions-item>
        </el-descriptions>
        
        <div class="markdown-content" v-html="selectedReportHtml"></div>
      </div>
      
      <template #footer>
        <el-button @click="showDetail = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
/* Markdown内容样式 */
.markdown-content {
  max-height: 600px;
  overflow-y: auto;
  padding: 16px;
  background: #ffffff;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
  line-height: 1.8;
  color: #303133;
}

.markdown-content :deep(h1),
.markdown-content :deep(h2),
.markdown-content :deep(h3) {
  margin-top: 20px;
  margin-bottom: 12px;
  font-weight: 600;
  color: #303133;
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

.markdown-content :deep(p) {
  margin: 12px 0;
}

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
}

.markdown-content :deep(table tr:nth-child(even)) {
  background-color: #fafafa;
}

.markdown-content :deep(ul),
.markdown-content :deep(ol) {
  margin: 12px 0;
  padding-left: 24px;
}

.markdown-content :deep(li) {
  margin: 6px 0;
}
</style>

