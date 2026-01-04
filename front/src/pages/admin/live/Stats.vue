<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import PageHeader from '../../../components/PageHeader.vue'
import { getAdminLiveSummaries } from '../../../lib/mocks/adminLives'
import { getAdminReservationSummaries } from '../../../lib/mocks/adminReservations'
import { getAdminVodSummaries } from '../../../lib/mocks/adminVods'

const router = useRouter()

const lives = ref(getAdminLiveSummaries())
const reservations = ref(getAdminReservationSummaries())
const vods = ref(getAdminVodSummaries())

const summaryCards = computed(() => [
  { label: '방송 중', value: lives.value.filter((item) => item.status === '방송중').length },
  { label: '예약 건', value: reservations.value.length },
  { label: 'VOD', value: vods.value.length },
  {
    label: '평균 시청자',
    value:
      lives.value.length > 0
        ? Math.round(lives.value.reduce((sum, item) => sum + item.viewers, 0) / lives.value.length)
        : 0,
  },
])

const topLiveByView = computed(() =>
  lives.value
    .slice()
    .sort((a, b) => b.viewers - a.viewers)
    .slice(0, 5),
)

const latestReservations = computed(() =>
  reservations.value.slice().sort((a, b) => b.datetime.localeCompare(a.datetime)).slice(0, 5),
)

const goSection = (value: 'list' | 'stats' | 'sanctions') => {
  if (value === 'list') {
    router.push('/admin/live').catch(() => {})
    return
  }
  if (value === 'sanctions') {
    router.push('/admin/live/sanctions').catch(() => {})
    return
  }
}

onMounted(() => {
  lives.value = getAdminLiveSummaries()
  reservations.value = getAdminReservationSummaries()
  vods.value = getAdminVodSummaries()
})
</script>

<template>
  <div>
    <PageHeader eyebrow="DESKIT" title="방송 통계" />

    <section class="stat-grid">
      <article v-for="card in summaryCards" :key="card.label" class="ds-surface stat-card">
        <p class="stat-label">{{ card.label }}</p>
        <p class="stat-value">{{ card.value.toLocaleString('ko-KR') }}</p>
      </article>
    </section>

    <section class="ds-stack-lg">
      <article class="ds-surface panel">
        <div class="panel__head">
          <div>
            <h3>시청자 상위 방송</h3>
            <p class="ds-section-sub">최근 목록 기준 상위 5개 방송이에요.</p>
          </div>
        </div>
        <div class="list">
          <div v-for="item in topLiveByView" :key="item.id" class="list-row">
            <div>
              <p class="list-title">{{ item.title }}</p>
              <p class="list-sub">{{ item.sellerName }} · {{ item.startedAt }}</p>
            </div>
            <span class="badge-chip">시청자 {{ item.viewers.toLocaleString('ko-KR') }}명</span>
          </div>
          <p v-if="!topLiveByView.length" class="empty">데이터가 없습니다.</p>
        </div>
      </article>

      <article class="ds-surface panel">
        <div class="panel__head">
          <div>
            <h3>최근 예약</h3>
            <p class="ds-section-sub">가장 최신 예약 5건을 보여줍니다.</p>
          </div>
        </div>
        <div class="list">
          <div v-for="item in latestReservations" :key="item.id" class="list-row">
            <div>
              <p class="list-title">{{ item.title }}</p>
              <p class="list-sub">{{ item.sellerName }} · {{ item.datetime }}</p>
            </div>
            <span class="badge-chip">상태: {{ item.status }}</span>
          </div>
          <p v-if="!latestReservations.length" class="empty">데이터가 없습니다.</p>
        </div>
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

.panel {
  padding: 16px;
  border-radius: 14px;
}

.panel__head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 10px;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.list-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid var(--border-color);
}

.list-row:last-child {
  border-bottom: none;
}

.list-title {
  margin: 0 0 4px;
  font-weight: 900;
  color: var(--text-strong);
}

.list-sub {
  margin: 0;
  color: var(--text-muted);
  font-weight: 700;
}

.badge-chip {
  display: inline-flex;
  align-items: center;
  padding: 8px 10px;
  border-radius: 10px;
  background: var(--surface-weak);
  font-weight: 800;
  color: var(--text-strong);
  white-space: nowrap;
}

.empty {
  margin: 0;
  color: var(--text-muted);
  font-weight: 700;
}
</style>
