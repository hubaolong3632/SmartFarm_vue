<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  title: { type: String, required: true },
  data: { type: Array, required: true },
  dataKey: { type: String, required: true },
  unit: { type: String, default: '' },
  color: { type: String, default: '#409EFF' },
  height: { type: String, default: '300px' }
})

const chartRef = ref(null)
let chartInstance = null

function initChart() {
  if (!chartRef.value) return
  
  chartInstance = echarts.init(chartRef.value)
  updateChart()
}

function updateChart() {
  if (!chartInstance || !props.data || props.data.length === 0) return
  
  // 准备数据
  const times = props.data.map(item => {
    const date = new Date(item.time)
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
  })
  
  const values = props.data.map(item => {
    const value = item[props.dataKey]
    return typeof value === 'boolean' ? (value ? 1 : 0) : Number(value || 0)
  })
  
  // 计算当前值
  const currentValue = values.length > 0 ? values[values.length - 1] : 0
  const displayValue = typeof props.data[props.data.length - 1]?.[props.dataKey] === 'boolean' 
    ? (props.data[props.data.length - 1][props.dataKey] ? '是' : '否')
    : currentValue.toFixed(1) + props.unit
  
  const option = {
    title: {
      text: `${props.title}: ${displayValue}`,
      left: 'center',
      textStyle: {
        fontSize: 14,
        fontWeight: 'normal'
      }
    },
    tooltip: {
      trigger: 'axis',
      formatter: (params) => {
        const param = params[0]
        const value = param.value
        const display = typeof props.data[param.dataIndex]?.[props.dataKey] === 'boolean'
          ? (props.data[param.dataIndex][props.dataKey] ? '是' : '否')
          : value + props.unit
        return `${param.name}<br/>${props.title}: ${display}`
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: times,
      axisLabel: {
        rotate: 45,
        fontSize: 10
      }
    },
    yAxis: {
      type: 'value',
      name: props.unit,
      nameTextStyle: {
        padding: [0, 0, 0, 10]
      }
    },
    series: [
      {
        name: props.title,
        type: 'line',
        smooth: true,
        data: values,
        itemStyle: {
          color: props.color
        },
        lineStyle: {
          color: props.color,
          width: 2
        },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: props.color + '80' },
              { offset: 1, color: props.color + '10' }
            ]
          }
        }
      }
    ]
  }
  
  chartInstance.setOption(option, true)
}

watch(() => props.data, () => {
  nextTick(() => {
    updateChart()
  })
}, { deep: true })

onMounted(() => {
  nextTick(() => {
    initChart()
    window.addEventListener('resize', handleResize)
  })
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
})

function handleResize() {
  if (chartInstance) {
    chartInstance.resize()
  }
}
</script>

<template>
  <div ref="chartRef" :style="{ width: '100%', height: height }"></div>
</template>

<style scoped>
</style>

