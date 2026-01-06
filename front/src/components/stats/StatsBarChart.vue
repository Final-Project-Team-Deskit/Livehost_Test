<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  data: Array<{ label: string; value: number }>
  valueFormatter?: (value: number) => string
}>()

const dataLength = computed(() => props.data.length || 1)

const maxValue = computed(() => {
  if (!props.data.length) return 0
  return Math.max(...props.data.map((item) => item.value))
})

const getStep = (max: number) => {
  if (max <= 0) return 1
  const rawStep = Math.max(1, Math.ceil(max / 9))
  const magnitude = 10 ** Math.floor(Math.log10(rawStep))
  const rounded = Math.max(1, Math.round(rawStep / magnitude)) * magnitude
  return rounded
}

const formatValue = (value: number) => {
  if (props.valueFormatter) return props.valueFormatter(value)
  return value.toLocaleString('ko-KR')
}

const toHeight = (value: number) => {
  if (!topTick.value) return '0%'
  return `${Math.round((value / topTick.value) * 100)}%`
}

const yTicks = computed(() => {
  if (!maxValue.value) return [0]
  const step = getStep(maxValue.value)
  let top = step * 9
  while (top < maxValue.value) {
    top += step
  }
  const ticks: number[] = []
  for (let value = top; value >= 0; value -= step) {
    ticks.push(value)
    if (ticks.length === 9) break
  }
  if (ticks[ticks.length - 1] !== 0) ticks.push(0)
  return ticks
})

const topTick = computed(() => (yTicks.value.length ? yTicks.value[0] : maxValue.value || 1))

const barGap = computed(() => `${Math.max(6, Math.min(14, Math.floor(160 / (dataLength.value + 5))))}px`)
const minBarWidth = computed(() => Math.max(16, Math.min(60, Math.floor(520 / (dataLength.value + 3)))))
const barLayoutStyle = computed(() => ({
  gap: barGap.value,
  gridTemplateColumns: `repeat(${dataLength.value}, minmax(${minBarWidth.value}px, 1fr))`,
}))
</script>

<template>
  <div class="bar-chart">
    <div v-if="data.length" class="bar-chart__grid">
      <div class="bar-chart__bars" :style="barLayoutStyle">
        <div v-for="(item, index) in data" :key="`${item.label}-${index}`" class="bar-chart__item">
          <div
            class="bar-chart__bar"
            :style="{ height: toHeight(item.value) }"
            :data-value="formatValue(item.value)"
            tabindex="0"
          ></div>
          <span class="bar-chart__label">{{ item.label }}</span>
        </div>
      </div>
      <div class="bar-chart__y-axis" aria-hidden="true">
        <span v-for="tick in yTicks" :key="tick" class="bar-chart__y-label">{{ formatValue(tick) }}</span>
      </div>
    </div>
    <p v-else class="empty">데이터가 없습니다.</p>
  </div>
</template>

<style scoped>
.bar-chart {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.bar-chart__grid {
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: flex-end;
  gap: 10px;
  width: 100%;
  max-width: 100%;
}

.bar-chart__y-axis {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  align-self: stretch;
  min-height: 240px;
  padding: 0 0 0 6px;
  text-align: right;
}

.bar-chart__y-label {
  font-size: 0.9rem;
  color: var(--text-muted);
  font-weight: 700;
  white-space: nowrap;
}

.bar-chart__bars {
  display: grid;
  align-items: end;
  min-height: 240px;
  padding: 8px 4px 0;
  width: 100%;
  box-sizing: border-box;
}

.bar-chart__item {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.bar-chart__bar {
  position: relative;
  width: 100%;
  background: linear-gradient(180deg, rgba(var(--primary-rgb), 0.9), rgba(var(--primary-rgb), 0.6));
  border-radius: 14px 14px 8px 8px;
  transition: height 0.2s ease, box-shadow 0.2s ease;
  min-height: 10px;
}

.bar-chart__bar::after {
  content: attr(data-value);
  position: absolute;
  left: 50%;
  bottom: 100%;
  transform: translate(-50%, -6px);
  background: var(--surface);
  color: var(--text-strong);
  border: 1px solid var(--border-color);
  padding: 6px 10px;
  border-radius: 10px;
  font-weight: 800;
  white-space: nowrap;
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.08);
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.15s ease, transform 0.15s ease;
}

.bar-chart__bar:hover,
.bar-chart__bar:focus-visible {
  box-shadow: 0 12px 24px rgba(var(--primary-rgb), 0.16);
}

.bar-chart__bar:hover::after,
.bar-chart__bar:focus-visible::after {
  opacity: 1;
  transform: translate(-50%, -10px);
}

.bar-chart__label {
  font-weight: 800;
  color: var(--text-muted);
  font-size: 0.95rem;
}

.empty {
  display: flex;
  margin: 0;
  color: var(--text-muted);
  font-weight: 700;
  justify-content: center;
}
</style>
