<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  data: Array<{ label: string; value: number }>
  valueFormatter?: (value: number) => string
}>()

const maxValue = computed(() => {
  if (!props.data.length) return 0
  return Math.max(...props.data.map((item) => item.value))
})

const formatValue = (value: number) => {
  if (props.valueFormatter) return props.valueFormatter(value)
  return value.toLocaleString('ko-KR')
}

const toHeight = (value: number) => {
  if (!maxValue.value) return '0%'
  return `${Math.round((value / maxValue.value) * 100)}%`
}
</script>

<template>
  <div class="bar-chart">
    <div v-if="data.length" class="bar-chart__bars">
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
    <p v-else class="empty">데이터가 없습니다.</p>
  </div>
</template>

<style scoped>
.bar-chart {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.bar-chart__bars {
  display: flex;
  gap: 14px;
  align-items: flex-end;
  min-height: 220px;
  padding: 8px 4px 0;
}

.bar-chart__item {
  flex: 1;
  min-width: 56px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.bar-chart__bar {
  position: relative;
  width: 100%;
  max-width: 68px;
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
