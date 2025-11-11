<script setup>
import { useGreenhouseStore } from '../stores/greenhouse'
import { ElMessage } from 'element-plus'
const store = useGreenhouseStore()

function saveAutomation() {
  try {
    const value = JSON.stringify(store.automation)
    localStorage.setItem('automation-settings', value)
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
    <el-form label-width="160px" style="max-width:600px;">
      <el-form-item label="自动补光">
        <el-switch v-model="store.automation.autoLightEnabled" />
      </el-form-item>
      <el-form-item label="光照阈值 (lux)">
        <el-input-number v-model="store.automation.lightLuxThreshold" :min="0" :step="500" />
      </el-form-item>
      <el-form-item label="自动抽水">
        <el-switch v-model="store.automation.autoPumpEnabled" />
      </el-form-item>
      <el-form-item label="湿度低阈值 (%)">
        <el-input-number v-model="store.automation.soilMoistureLowThreshold" :min="0" :max="100" :step="1" />
      </el-form-item>
    </el-form>
    <el-alert
      title="当光照低于阈值将自动开启补光灯；当湿度低于阈值将执行抽水（示例中仅触发报警）。"
      type="info"
      :closable="false"
      show-icon
      style="margin-top:8px;"
    />
  </el-card>
</template>

<style scoped></style>


