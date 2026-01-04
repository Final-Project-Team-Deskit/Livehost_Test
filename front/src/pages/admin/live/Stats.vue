<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import PageHeader from '../../../components/PageHeader.vue'
import StatsBarChart from '../../../components/stats/StatsBarChart.vue'
import StatsRankList from '../../../components/stats/StatsRankList.vue'
import {
  getAdminProductRevenueRankings,
  getAdminRevenueRankings,
  revenueData,
  revenuePerViewerData,
  type RankGroup,
  type StatsRange,
} from '../../../lib/mocks/liveStats'

type RankView = 'best' | 'worst'

const revenueRange = ref<StatsRange>('monthly')
const perViewerRange = ref<StatsRange>('monthly')
const broadcastRankView = ref<RankView>('best')
const productRankView = ref<RankView>('best')

const broadcastRanks = ref<RankGroup>({ best: [], worst: [] })
const productRanks = ref<RankGroup>({ best: [], worst: [] })

const revenueChart = computed(() => revenueData[revenueRange.value])
const perViewerChart = computed(() => revenuePerViewerData[perViewerRange.value])

const formatCurrency = (value: number) => `₩${value.toLocaleString('ko-KR')}`

onMounted(() => {
  broadcastRanks.value = getAdminRevenueRankings()
  productRanks.value = getAdminProductRevenueRankings()
})
</script>

<template>
  <div class="stats-page">
    <PageHeader eyebrow="DESKIT" title="방송 통계" />

    <section class="stats-grid stats-grid--charts">
      <article class="ds-surface stats-card">
        <header class="stats-card__head">
          <div>
            <h3>매출 추이</h3>
            <p class="stats-card__sub">기간별 매출 지표</p>
          </div>
          <div class="toggle-group" role="tablist" aria-label="매출 기간">
            <button
              type="button"
              class="toggle-btn"
              :class="{ 'toggle-btn--active': revenueRange === 'daily' }"
              @click="revenueRange = 'daily'"
            >
              일별
            </button>
            <button
              type="button"
              class="toggle-btn"
              :class="{ 'toggle-btn--active': revenueRange === 'monthly' }"
              @click="revenueRange = 'monthly'"
            >
              월별
            </button>
            <button
              type="button"
              class="toggle-btn"
              :class="{ 'toggle-btn--active': revenueRange === 'yearly' }"
              @click="revenueRange = 'yearly'"
            >
              연도별
            </button>
          </div>
        </header>
        <StatsBarChart :data="revenueChart" :value-formatter="formatCurrency" />
      </article>

      <article class="ds-surface stats-card">
        <header class="stats-card__head">
          <div>
            <h3>시청자 당 매출 추이</h3>
            <p class="stats-card__sub">시청자 1명당 매출 흐름을 확인하세요.</p>
          </div>
          <div class="toggle-group" role="tablist" aria-label="시청자당 매출 기간">
            <button
              type="button"
              class="toggle-btn"
              :class="{ 'toggle-btn--active': perViewerRange === 'daily' }"
              @click="perViewerRange = 'daily'"
            >
              일별
            </button>
            <button
              type="button"
              class="toggle-btn"
              :class="{ 'toggle-btn--active': perViewerRange === 'monthly' }"
              @click="perViewerRange = 'monthly'"
            >
              월별
            </button>
            <button
              type="button"
              class="toggle-btn"
              :class="{ 'toggle-btn--active': perViewerRange === 'yearly' }"
              @click="perViewerRange = 'yearly'"
            >
              연도별
            </button>
          </div>
        </header>
        <StatsBarChart :data="perViewerChart" :value-formatter="formatCurrency" />
      </article>
    </section>

    <section class="stats-grid stats-grid--lists">
      <article class="ds-surface stats-card">
        <header class="stats-card__head">
          <div>
            <h3>방송 매출 순위 TOP 5</h3>
            <p class="stats-card__sub">최근 목록을 기준으로 베스트/워스트를 확인하세요.</p>
          </div>
          <div class="toggle-group" role="tablist" aria-label="방송 매출 순위">
            <button
              type="button"
              class="toggle-btn"
              :class="{ 'toggle-btn--active': broadcastRankView === 'best' }"
              @click="broadcastRankView = 'best'"
            >
              베스트
            </button>
            <button
              type="button"
              class="toggle-btn"
              :class="{ 'toggle-btn--active': broadcastRankView === 'worst' }"
              @click="broadcastRankView = 'worst'"
            >
              워스트
            </button>
          </div>
        </header>
        <StatsRankList :items="broadcastRanks[broadcastRankView]" :value-formatter="formatCurrency" />
      </article>

      <article class="ds-surface stats-card">
        <header class="stats-card__head">
          <div>
            <h3>상품 매출 순위 TOP 5</h3>
            <p class="stats-card__sub">판매된 상품 중 매출 상·하위 항목입니다.</p>
          </div>
          <div class="toggle-group" role="tablist" aria-label="상품 매출 순위">
            <button
              type="button"
              class="toggle-btn"
              :class="{ 'toggle-btn--active': productRankView === 'best' }"
              @click="productRankView = 'best'"
            >
              베스트
            </button>
            <button
              type="button"
              class="toggle-btn"
              :class="{ 'toggle-btn--active': productRankView === 'worst' }"
              @click="productRankView = 'worst'"
            >
              워스트
            </button>
          </div>
        </header>
        <StatsRankList :items="productRanks[productRankView]" :value-formatter="formatCurrency" />
      </article>
    </section>
  </div>
</template>

<style scoped>
.stats-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.stats-grid {
  display: grid;
  gap: 14px;
}

.stats-grid--charts {
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
}

.stats-grid--lists {
  grid-template-columns: repeat(auto-fit, minmax(340px, 1fr));
}

.stats-card {
  padding: 16px;
  border-radius: 14px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.stats-card__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.stats-card__sub {
  margin: 4px 0 0;
  color: var(--text-muted);
  font-weight: 700;
}

.toggle-group {
  display: inline-flex;
  gap: 8px;
}

.toggle-btn {
  border: 1px solid var(--border-color);
  background: var(--surface);
  color: var(--text-strong);
  padding: 8px 12px;
  border-radius: 10px;
  font-weight: 800;
  cursor: pointer;
  transition: all 0.15s ease;
}

.toggle-btn--active {
  background: rgba(var(--primary-rgb), 0.12);
  border-color: rgba(var(--primary-rgb), 0.6);
  color: var(--primary-color);
  box-shadow: 0 4px 12px rgba(var(--primary-rgb), 0.14);
}

@media (max-width: 640px) {
  .stats-card__head {
    flex-direction: column;
    align-items: flex-start;
  }

  .toggle-group {
    width: 100%;
    flex-wrap: wrap;
  }
}
</style>
