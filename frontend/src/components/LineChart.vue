<script setup>
import { computed } from 'vue'

const props = defineProps({
  points: { type: Array, required: true }, // [{ x:number, y:number }]
  series: { type: Array, required: true }, // [{ label, color, accessor: (p) => number }]
  width: { type: Number, default: 640 },
  height: { type: Number, default: 240 },
  padding: { type: Number, default: 32 },
})

const domain = computed(() => {
  const xs = props.points.map((_, i) => i)
  const ys = []
  props.series.forEach(s => {
    props.points.forEach(p => ys.push(s.accessor(p)))
  })
  const minX = 0
  const maxX = Math.max(0, xs.length - 1)
  const minY = Math.min(...ys)
  const maxY = Math.max(...ys)
  return { minX, maxX, minY, maxY }
})

function scaleX(i) {
  const { minX, maxX } = domain.value
  const w = props.width - props.padding * 2
  if (maxX === minX) return props.padding
  return props.padding + ((i - minX) / (maxX - minX)) * w
}
function scaleY(v) {
  const { minY, maxY } = domain.value
  const h = props.height - props.padding * 2
  if (maxY === minY) return props.height - props.padding
  return props.height - props.padding - ((v - minY) / (maxY - minY)) * h
}

function pathForSeries(s) {
  return props.points
    .map((p, i) => {
      const x = scaleX(i)
      const y = scaleY(s.accessor(p))
      return `${i === 0 ? 'M' : 'L'} ${x} ${y}`
    })
    .join(' ')
}
</script>

<template>
  <svg :width="width" :height="height" style="max-width: 100%; background: #fff; border: 1px solid #eee;">
    <g>
      <line
        :x1="padding"
        :y1="height - padding"
        :x2="width - padding"
        :y2="height - padding"
        stroke="#ccc"
      />
      <line
        :x1="padding"
        :y1="padding"
        :x2="padding"
        :y2="height - padding"
        stroke="#ccc"
      />
    </g>
    <g v-for="s in series" :key="s.label">
      <path :d="pathForSeries(s)" :stroke="s.color" fill="none" stroke-width="2" />
    </g>
    <g>
      <rect x="padding" y="6" rx="4" ry="4" :width="width - padding * 2" height="20" fill="#fafafa" stroke="#eee" />
      <g v-for="(s, idx) in series" :key="s.label" :transform="`translate(${padding + 8 + idx * 140}, 20)`">
        <rect x="-6" y="-8" width="12" height="12" :fill="s.color" />
        <text x="10" y="2" font-size="12" fill="#333">{{ s.label }}</text>
      </g>
    </g>
  </svg>
</template>

<style scoped></style>


