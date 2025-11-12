<script setup>
const emit = defineEmits(['clean', 'toggle-light'])
const props = defineProps({
  lightOn: { type: Boolean, default: false },
  cleaning: { type: Boolean, default: false },
})
</script>

<template>
  <div>
    <div style="margin-bottom: 8px; font-weight: 600;">控制</div>
    <div style="display: flex; gap: 8px; flex-wrap: wrap;">
      <el-button type="warning" :disabled="cleaning" @click="emit('clean')">
        {{ cleaning ? '清理中...' : '清理搅拌熔炉' }}
      </el-button>
      <el-button :type="lightOn ? 'danger' : 'primary'" @click="emit('toggle-light')">
        {{ lightOn ? '关闭补光灯' : '打开补光灯' }}
      </el-button>
    </div>
  </div>
</template>

<style scoped></style>


