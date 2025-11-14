<script setup>
import { computed, ref, onMounted } from 'vue'
import { useGreenhouseStore } from '../stores/greenhouse'
const store = useGreenhouseStore()

// 选择日期后，点击"搜索"才应用
const pendingDate = ref(store.selectedDate)
const showAll = ref(false) // 是否显示所有图片

async function applySearch() {
  store.selectedDate = pendingDate.value
  // 加载指定日期的图片
  await store.loadImagesByDate(pendingDate.value)
  showAll.value = false
}

// 加载所有图片
async function loadAll() {
  await store.loadAllImages()
  showAll.value = true
}

// 加载异常图片
async function loadAbnormal() {
  await store.loadAbnormalImages()
  showAll.value = true
}

// 组件挂载时加载今天的图片
onMounted(async () => {
  const today = new Date().toISOString().split('T')[0]
  store.selectedDate = today
  pendingDate.value = today
  await store.loadImagesByDate(today)
})

const images = computed(() => {
  // 如果显示所有图片
  if (showAll.value && Array.isArray(store.images) && store.images.length) {
    return store.images
  }
  // 否则显示指定日期的图片
  const date = store.selectedDate
  const byDate = store.imagesByDate[date]
  if (Array.isArray(byDate) && byDate.length) {
    return byDate
  }
  // 如果没有数据，返回空数组
  return []
})

function isAbnormal(img) {
  // 只有温度高于舒适温度（高阈值）或土壤湿度低于10%才标记为异常
  const tempHigh = store.automation.temperatureHighThreshold ?? 35
  const tempAbnormal = (img.temperatureC ?? 0) > tempHigh
  
  const moistureThreshold = 10 // 土壤湿度低于10%才异常
  const moistureAbnormal = (img.soilMoisturePct ?? 0) < moistureThreshold
  
  return { 
    flag: tempAbnormal || moistureAbnormal,
    tempAbnormal, 
    moistureAbnormal
  }
}
</script>

<template>
  <el-card shadow="never">
    <template #header>图片卡片集</template>
    <el-form inline>
      <el-form-item label="选择日期">
        <el-date-picker v-model="pendingDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="applySearch">按日期搜索</el-button>
      </el-form-item>
      <el-form-item>
        <el-button type="success" @click="loadAll">显示所有图片</el-button>
      </el-form-item>
      <el-form-item>
        <el-button type="warning" @click="loadAbnormal">显示异常图片</el-button>
      </el-form-item>
    </el-form>
    <div v-if="images.length === 0" style="text-align: center; padding: 40px; color: #999;">
      暂无图片数据
    </div>
    <el-row :gutter="12" style="margin-top:8px;">
      <el-col v-for="(img, idx) in images" :key="idx" :md="6" :sm="8" :xs="12">
        <el-card
          shadow="hover"
          :body-style="{ padding: '8px' }"
          :style="isAbnormal(img).flag ? 'background:#ffcccc;' : ''"
        >
          <div style="position: relative;">
            <img 
              :src="img.url" 
              alt="" 
              style="width: 100%; height: 200px; object-fit: cover; display: block; border-radius: 4px;"
              @error="handleImageError"
              onerror="this.src='https://placehold.co/320x200?text=图片加载失败'"
            />
            <div v-if="isAbnormal(img).flag"
                 style="
                   position:absolute;
                   top:6px;
                   left:6px;
                   background:#fee2e2;
                   color:#b91c1c;
                   border:1px solid #fecaca;
                   padding:2px 6px;
                   border-radius:4px;
                   font-size:12px;
                   z-index: 10;
                 ">
              异常
            </div>
            <div
              style="
                position: absolute;
                left: 6px;
                bottom: 6px;
                background: rgba(255, 251, 235, 0.95);
                color: #b45309;
                padding: 2px 6px;
                border-radius: 4px;
                font-size: 12px;
                border: 1px solid #fde68a;
                z-index: 10;
              "
            >
              {{ new Date(img.time).toLocaleString('zh-CN') }}
            </div>
          </div>
          <div style="padding: 6px 4px; font-size: 12px; color: #666; display: grid; row-gap: 2px;">
            <div>温度：{{ (img.temperatureC ?? 0).toFixed(1) }}°C</div>
            <div>湿度：{{ Math.round(img.humidityPct ?? 0) }}%</div>
            <div>土壤湿度：{{ Math.round(img.soilMoisturePct ?? 0) }}%</div>
            <div>光照：{{ Math.round(img.lightLux ?? 0) }} lux</div>
            <div>是否下雨：{{ img.isRaining ? '是' : '否' }}</div>
            <div>氧气：{{ (img.oxygenPct ?? 0).toFixed(1) }}%</div>
            <div>二氧化碳：{{ img.co2Ppm ?? 0 }} ppm</div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </el-card>
</template>

<style scoped></style>


