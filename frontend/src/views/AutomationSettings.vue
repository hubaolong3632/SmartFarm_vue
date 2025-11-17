<script setup>
import { onMounted } from 'vue'
import { useGreenhouseStore } from '../stores/greenhouse'
import { ElMessage } from 'element-plus'
const store = useGreenhouseStore()

onMounted(() => {
  // 异步加载自动化设置，不阻塞界面渲染
  store.loadAutomationSettings().catch(err => console.error('加载自动化设置失败:', err))
})

async function saveAutomation() {
  try {
    await store.saveAutomationSettings()
    ElMessage.success('已保存自动化设置')
  } catch (e) {
    ElMessage.error('保存失败')
  }
}
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <div style="display:flex; align-items:center;">
        <div style="flex:1;">自动化控制</div>
        <el-button type="primary" size="small" @click="saveAutomation">保存</el-button>
      </div>
    </template>
    <el-form label-width="180px" style="max-width:700px;">
      <el-divider content-position="left">自动控制开关</el-divider>
      <el-form-item label="自动补光">
        <el-switch v-model="store.automation.autoLightEnabled" />
      </el-form-item>
      <el-form-item label="自动抽水">
        <el-switch v-model="store.automation.autoPumpEnabled" />
      </el-form-item>
      
      <el-divider content-position="left">阈值设置</el-divider>
      <el-form-item label="光照阈值 (lux)">
        <el-input-number v-model="store.automation.lightLuxThreshold" :min="0" :step="500" />
        <span style="margin-left:8px;color:#999;font-size:12px;">低于此值自动开灯</span>
      </el-form-item>
      <el-form-item label="土壤湿度低阈值 (%)">
        <el-input-number v-model="store.automation.soilMoistureLowThreshold" :min="0" :max="100" :step="1" />
        <span style="margin-left:8px;color:#999;font-size:12px;">低于此值自动抽水</span>
      </el-form-item>
      <el-form-item label="温度高阈值 (°C)">
        <el-input-number v-model="store.automation.temperatureHighThreshold" :min="-50" :max="100" :step="1" />
        <span style="margin-left:8px;color:#999;font-size:12px;">高于此值报警</span>
      </el-form-item>
      <el-form-item label="温度低阈值 (°C)">
        <el-input-number v-model="store.automation.temperatureLowThreshold" :min="-50" :max="100" :step="1" />
        <span style="margin-left:8px;color:#999;font-size:12px;">低于此值报警</span>
      </el-form-item>
      <el-form-item label="湿度高阈值 (%)">
        <el-input-number v-model="store.automation.humidityHighThreshold" :min="0" :max="100" :step="1" />
        <span style="margin-left:8px;color:#999;font-size:12px;">高于此值报警</span>
      </el-form-item>
      <el-form-item label="湿度低阈值 (%)">
        <el-input-number v-model="store.automation.humidityLowThreshold" :min="0" :max="100" :step="1" />
        <span style="margin-left:8px;color:#999;font-size:12px;">低于此值报警</span>
      </el-form-item>
      <el-form-item label="氧气含量低阈值 (%)">
        <el-input-number v-model="store.automation.oxygenLowThreshold" :min="0" :max="100" :step="0.1" :precision="1" />
        <span style="margin-left:8px;color:#999;font-size:12px;">低于此值报警</span>
      </el-form-item>
      <el-form-item label="二氧化碳高阈值 (ppm)">
        <el-input-number v-model="store.automation.co2HighThreshold" :min="0" :max="10000" :step="50" />
        <span style="margin-left:8px;color:#999;font-size:12px;">高于此值报警</span>
      </el-form-item>
    </el-form>
    <el-alert
      title="当传感器数据超出阈值范围时，系统将自动触发相应的控制操作或报警。"
      type="info"
      :closable="false"
      show-icon
      style="margin-top:8px;"
    />
  </el-card>
</template>

<style scoped></style>


