<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import PageHeader from '../../../components/PageHeader.vue'
import StatsBarChart from '../../../components/stats/StatsBarChart.vue'
import StatsRankList from '../../../components/stats/StatsRankList.vue'
import { getAdminSanctionStatistics, type SanctionStatisticsResponse } from '../../../api/live'

type Metric = 'daily' | 'monthly' | 'yearly'

const stopMetric = ref<Metric>('daily')
const viewerMetric = ref<Metric>('daily')

const statsCache = ref<Record<Metric, SanctionStatisticsResponse | null>>({
  daily: null,
  monthly: null,
  yearly: null,
})

const periodMap: Record<Metric, string> = {
  daily: 'DAILY',
  monthly: 'MONTHLY',
  yearly: 'YEARLY',
}

const loadStats = async (range: Metric) => {
  if (statsCache.value[range]) return
  try {
    const data = await getAdminSanctionStatistics(periodMap[range])
    statsCache.value = { ...statsCache.value, [range]: data }
  } catch (error) {
    console.error('Failed to load sanction stats', error)
  }
}

const mapChart = (items: Array<{ label: string; count: number }> | undefined) =>
  (items ?? []).map((item) => ({ label: item.label, value: Number(item.count ?? 0) }))

const stopCountData = computed(() => mapChart(statsCache.value[stopMetric.value]?.forceStopChart))
const viewerSanctionData = computed(() => mapChart(statsCache.value[viewerMetric.value]?.viewerBanChart))

const buildRanks = (items: Array<{ name?: string; sellerName?: string; sanctionCount: number }>) =>
  items.slice(0, 5).map((item, index) => ({
    rank: index + 1,
    title: item.sellerName ?? item.name ?? '알 수 없음',
    value: Number(item.sanctionCount ?? 0),
  }))

const topSellerStops = computed(() =>
  buildRanks(statsCache.value[stopMetric.value]?.worstSellers ?? []),
)
const topViewerSanctions = computed(() =>
  buildRanks(statsCache.value[viewerMetric.value]?.worstViewers ?? []),
)

const summaryCards = computed(() => {
  const stopChart = statsCache.value[stopMetric.value]?.forceStopChart ?? []
  const viewerChart = statsCache.value[viewerMetric.value]?.viewerBanChart ?? []
  const stopTotal = stopChart.reduce((sum, item) => sum + Number(item.count ?? 0), 0)
  const viewerTotal = viewerChart.reduce((sum, item) => sum + Number(item.count ?? 0), 0)
  const latestLabel = viewerChart[viewerChart.length - 1]?.label ?? '-'
  return [
    { label: '총 송출 중지', value: `${stopTotal.toLocaleString('ko-KR')}건` },
    { label: '시청자 제재', value: `${viewerTotal.toLocaleString('ko-KR')}건` },
    { label: '최근 제재일', value: latestLabel },
  ]
})

onMounted(() => {
  loadStats(stopMetric.value)
  loadStats(viewerMetric.value)
})

watch(stopMetric, (range) => {
  loadStats(range)
})

watch(viewerMetric, (range) => {
  loadStats(range)
})
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
        <StatsBarChart :data="stopCountData" />
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
        <StatsBarChart :data="viewerSanctionData" />
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
