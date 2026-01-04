<script setup lang="ts">
import { ref } from 'vue'
import PageHeader from '../../../components/PageHeader.vue'
import StatsBarChart from '../../../components/stats/StatsBarChart.vue'
import StatsRankList from '../../../components/stats/StatsRankList.vue'

type Metric = 'daily' | 'monthly' | 'yearly'

const stopMetric = ref<Metric>('daily')
const viewerMetric = ref<Metric>('daily')

const stopCountData: Record<Metric, Array<{ label: string; value: number }>> = {
  daily: [
    { label: '월', value: 6 },
    { label: '화', value: 4 },
    { label: '수', value: 7 },
    { label: '목', value: 5 },
    { label: '금', value: 3 },
  ],
  monthly: [
    { label: '8월', value: 12 },
    { label: '9월', value: 16 },
    { label: '10월', value: 18 },
    { label: '11월', value: 21 },
    { label: '12월', value: 14 },
  ],
  yearly: [
    { label: '2021', value: 64 },
    { label: '2022', value: 73 },
    { label: '2023', value: 88 },
    { label: '2024', value: 95 },
    { label: '2025', value: 52 },
  ],
}

const viewerSanctionData: Record<Metric, Array<{ label: string; value: number }>> = {
  daily: [
    { label: '월', value: 18 },
    { label: '화', value: 12 },
    { label: '수', value: 15 },
    { label: '목', value: 9 },
    { label: '금', value: 10 },
  ],
  monthly: [
    { label: '8월', value: 210 },
    { label: '9월', value: 240 },
    { label: '10월', value: 265 },
    { label: '11월', value: 292 },
    { label: '12월', value: 188 },
  ],
  yearly: [
    { label: '2021', value: 1020 },
    { label: '2022', value: 1180 },
    { label: '2023', value: 1340 },
    { label: '2024', value: 1280 },
    { label: '2025', value: 640 },
  ],
}

const topSellerStops = [
  { rank: 1, title: '판매자 A', value: 12 },
  { rank: 2, title: '판매자 B', value: 9 },
  { rank: 3, title: '판매자 C', value: 7 },
  { rank: 4, title: '판매자 D', value: 6 },
  { rank: 5, title: '판매자 E', value: 5 },
]

const topViewerSanctions = [
  { rank: 1, title: '시청자1', value: 18 },
  { rank: 2, title: '시청자2', value: 16 },
  { rank: 3, title: '시청자3', value: 14 },
  { rank: 4, title: '시청자4', value: 12 },
  { rank: 5, title: '시청자5', value: 10 },
]

const summaryCards = [
  { label: '총 송출 중지', value: '95건' },
  { label: '시청자 제재', value: '1,280건' },
  { label: '최근 제재일', value: '2025-12-12' },
]
</script>

<template>
  <div>
    <PageHeader eyebrow="DESKIT" title="제재 통계" />

    <section class="stat-grid">
      <article v-for="card in summaryCards" :key="card.label" class="ds-surface stat-card">
        <p class="stat-label">{{ card.label }}</p>
        <p class="stat-value">{{ card.value }}</p>
      </article>
    </section>

    <section class="chart-grid">
      <article class="ds-surface panel">
        <header class="panel__head">
          <div>
            <h3>송출 중지 횟수</h3>
            <p class="ds-section-sub">기간별 송출 중지 추이</p>
          </div>
          <div class="toggle-group" role="tablist" aria-label="송출 중지 기간">
            <button type="button" class="toggle-btn" :class="{ 'toggle-btn--active': stopMetric === 'daily' }" @click="stopMetric = 'daily'">
              일별
            </button>
            <button type="button" class="toggle-btn" :class="{ 'toggle-btn--active': stopMetric === 'monthly' }" @click="stopMetric = 'monthly'">
              월별
            </button>
            <button type="button" class="toggle-btn" :class="{ 'toggle-btn--active': stopMetric === 'yearly' }" @click="stopMetric = 'yearly'">
              연도별
            </button>
          </div>
        </header>
        <StatsBarChart :data="stopCountData[stopMetric]" />
      </article>

      <article class="ds-surface panel">
        <header class="panel__head">
          <div>
            <h3>시청자 제재 횟수</h3>
            <p class="ds-section-sub">기간별 시청자 제재 추이</p>
          </div>
          <div class="toggle-group" role="tablist" aria-label="시청자 제재 기간">
            <button type="button" class="toggle-btn" :class="{ 'toggle-btn--active': viewerMetric === 'daily' }" @click="viewerMetric = 'daily'">
              일별
            </button>
            <button type="button" class="toggle-btn" :class="{ 'toggle-btn--active': viewerMetric === 'monthly' }" @click="viewerMetric = 'monthly'">
              월별
            </button>
            <button type="button" class="toggle-btn" :class="{ 'toggle-btn--active': viewerMetric === 'yearly' }" @click="viewerMetric = 'yearly'">
              연도별
            </button>
          </div>
        </header>
        <StatsBarChart :data="viewerSanctionData[viewerMetric]" />
      </article>
    </section>

    <section class="ranks-grid">
      <article class="ds-surface panel">
        <h3 class="panel__title">송출 중지 횟수 많은 판매자 TOP 5</h3>
        <StatsRankList :items="topSellerStops" />
      </article>
      <article class="ds-surface panel">
        <h3 class="panel__title">제재 당한 횟수 많은 시청자 TOP 5</h3>
        <StatsRankList :items="topViewerSanctions" />
      </article>
    </section>
  </div>
</template>

<style scoped>
.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
  margin-bottom: 18px;
}

.stat-card {
  padding: 14px;
  border-radius: 12px;
}

.stat-label {
  margin: 0 0 6px;
  color: var(--text-muted);
  font-weight: 800;
}

.stat-value {
  margin: 0;
  font-size: 1.4rem;
  font-weight: 900;
  color: var(--text-strong);
}

.chart-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(380px, 1fr));
  gap: 14px;
}

.ranks-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 14px;
  margin-top: 14px;
}

.panel {
  padding: 16px;
  border-radius: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.panel__head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
  flex-wrap: wrap;
}

.panel__title {
  margin: 0 0 6px;
  font-weight: 900;
  color: var(--text-strong);
}

.toggle-group {
  display: inline-flex;
  gap: 8px;
}

.toggle-btn {
  border: 1px solid var(--border-color);
  background: var(--surface);
  color: var(--text-strong);
  border-radius: 999px;
  padding: 8px 12px;
  font-weight: 800;
  cursor: pointer;
}

.toggle-btn--active {
  border-color: var(--primary-color);
  color: var(--primary-color);
  background: rgba(var(--primary-rgb), 0.08);
}
</style>
