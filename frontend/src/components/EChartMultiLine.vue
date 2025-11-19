<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  title: { type: String, required: true },
  data: { type: Array, required: true },
  series: { type: Array, required: true }, // [{ name, dataKey, color }]
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
  
  // 准备时间轴数据
  const times = props.data.map(item => {
    const date = new Date(item.time)
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  })
  
  // 准备多条线数据
  const seriesData = props.series.map(serie => ({
    name: serie.name,
    type: 'line',
    smooth: true,
    data: props.data.map(item => Number(item[serie.dataKey] || 0)),
    itemStyle: {
      color: serie.color
    },
    lineStyle: {
      color: serie.color,
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
          { offset: 0, color: serie.color + '80' },
          { offset: 1, color: serie.color + '10' }
        ]
      }
    }
  }))
  
  const option = {
    title: {
      text: props.title,
      left: 'center',
      textStyle: {
        fontSize: 14,
        fontWeight: 'normal'
      }
    },
    tooltip: {
      trigger: 'axis',
      formatter: (params) => {
        let result = params[0].name + '<br/>'
        params.forEach(param => {
          result += `${param.marker}${param.seriesName}: ${param.value}<br/>`
        })
        return result
      }
    },
    legend: {
      data: props.series.map(s => s.name),
      bottom: 0
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
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
      name: '数量'
    },
    series: seriesData
  }
  
  chartInstance.setOption(option, true)
}

watch(() => [props.data, props.series], () => {
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

