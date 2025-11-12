<script setup>
const props = defineProps({
  alerts: { type: Array, required: true },
})
</script>

<template>
  <div>
    <div style="margin-bottom: 8px; font-weight: 600;">异常报警</div>
    <el-empty description="暂无报警" v-if="!alerts.length" />
    <div v-else style="max-height: 600px; overflow: auto; padding-right: 4px;">
      <el-timeline>
        <el-timeline-item
          v-for="a in alerts"
          :key="a.id"
          :type="a.level === 'warning' ? 'warning' : (a.level === 'error' ? 'danger' : 'primary')"
          :timestamp="new Date(a.time).toLocaleString()"
        >
          {{ a.message }}
        </el-timeline-item>
      </el-timeline>
    </div>
  </div>
  </template>

<style scoped></style>


