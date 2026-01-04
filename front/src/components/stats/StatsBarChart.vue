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

const toWidth = (value: number) => {
  if (!maxValue.value) return '0%'
  return `${Math.round((value / maxValue.value) * 100)}%`
}
</script>

<template>
  <div class="bar-chart">
    <div v-for="(item, index) in data" :key="`${item.label}-${index}`" class="bar-row">
      <span class="bar-label">{{ item.label }}</span>
      <div class="bar-track">
        <div class="bar-fill" :style="{ width: toWidth(item.value) }"></div>
        <span class="bar-value">{{ formatValue(item.value) }}</span>
      </div>
    </div>
    <p v-if="!data.length" class="empty">데이터가 없습니다.</p>
  </div>
</template>

<style scoped>
.bar-chart {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.bar-row {
  display: grid;
  grid-template-columns: 92px 1fr;
  gap: 10px;
  align-items: center;
}

.bar-label {
  font-weight: 800;
  color: var(--text-muted);
  font-size: 0.95rem;
}

.bar-track {
  position: relative;
  background: var(--surface-weak);
  border-radius: 999px;
  min-height: 36px;
  display: flex;
  align-items: center;
  overflow: hidden;
}

.bar-fill {
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, rgba(var(--primary-rgb), 0.9), rgba(var(--primary-rgb), 0.6));
  border-radius: 999px;
  transition: width 0.2s ease;
}

.bar-value {
  position: relative;
  padding: 0 12px;
  font-weight: 800;
  color: var(--text-strong);
  text-shadow: 0 1px 0 rgba(0, 0, 0, 0.06);
}

.empty {
  margin: 0;
  color: var(--text-muted);
  font-weight: 700;
}
</style>
