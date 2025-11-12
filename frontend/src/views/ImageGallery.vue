<script setup>
import { computed, ref } from 'vue'
import { useGreenhouseStore } from '../stores/greenhouse'
const store = useGreenhouseStore()

// 选择日期后，点击"搜索"才应用
const pendingDate = ref(store.selectedDate)
async function applySearch() {
  store.selectedDate = pendingDate.value
  // 加载指定日期的图片
  await store.loadImagesByDate(pendingDate.value)
}

const images = computed(() => {
  // 1) If external images array provided, use it directly
  if (Array.isArray(store.images) && store.images.length) {
    return store.images
  }
  // 2) Otherwise fallback to imagesByDate[selectedDate] or synthesize from hourly
  const date = store.selectedDate
  const byDate = store.imagesByDate[date]
  if (Array.isArray(byDate) && byDate.length) {
    return byDate
  }
  // 3) Fallback demo from hourly data
  return store.hourly.map(h => ({
    time: h.time,
    url: `https://placehold.co/320x200?text=${encodeURIComponent(new Date(h.time).getHours() + ':00')}`,
    temperatureC: h.temperatureC,
    soilMoisturePct: h.soilMoisturePct,
    lightLux: h.lightLux,
  }))
})

function isAbnormal(img) {
  const tempAbnormal = (img.temperatureC ?? 0) < 10 || (img.temperatureC ?? 0) > 35
  const moistureThreshold = store.automation.soilMoistureLowThreshold ?? 35
  const moistureAbnormal = (img.soilMoisturePct ?? 0) < moistureThreshold
  return { flag: tempAbnormal || moistureAbnormal, tempAbnormal, moistureAbnormal }
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
        <el-button type="primary" @click="applySearch">搜索</el-button>
      </el-form-item>
    </el-form>
    <el-row :gutter="12" style="margin-top:8px;">
      <el-col v-for="(img, idx) in images" :key="idx" :md="6" :sm="8" :xs="12">
        <el-card
          shadow="hover"
          :body-style="{ padding: '8px' }"
          :style="isAbnormal(img).flag ? 'background:#ffcccc;' : ''"
        >
          <div style="position: relative;">
            <img :src="img.url" alt="" style="width: 100%; display: block; border-radius: 4px;">
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
                 ">
              异常
            </div>
            <div
              style="
                position: absolute;
                left: 6px;
                bottom: 6px;
                background: #fffbeb;
                color: #b45309;
                padding: 2px 6px;
                border-radius: 4px;
                font-size: 12px;
                border: 1px solid #fde68a;
              "
            >
              {{ new Date(img.time).toLocaleString() }}
            </div>
          </div>
          <div style="padding: 6px 4px; font-size: 12px; color: #666; display: grid; row-gap: 2px;">
            <div>温度：{{ (img.temperatureC ?? 0).toFixed(1) }}°C</div>
            <div>土壤湿度：{{ Math.round(img.soilMoisturePct ?? 0) }}%</div>
            <div>光照：{{ Math.round(img.lightLux ?? 0) }} lux</div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </el-card>
</template>

<style scoped></style>


