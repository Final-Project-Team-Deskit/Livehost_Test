<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import PageHeader from '../../components/PageHeader.vue'
import { getScheduledBroadcasts } from '../../composables/useSellerBroadcasts'
import { sellerReservationSummaries } from '../../lib/mocks/sellerReservations'
import { getSellerVodDetail, sellerVodSummaries } from '../../lib/mocks/sellerVods'

const router = useRouter()

const scheduled = ref(getScheduledBroadcasts())
const seededReservations = ref(sellerReservationSummaries)
const vods = ref(sellerVodSummaries)

const summaryCards = computed(() => [
  { label: '예약', value: scheduled.value.length + seededReservations.value.length },
  { label: 'VOD', value: vods.value.length },
  {
    label: '평균 최대 시청자',
    value:
      vods.value.length > 0
        ? Math.round(
            vods.value.reduce((sum, item) => sum + (getSellerVodDetail(item.id)?.metrics?.maxViewers ?? 0), 0) /
              vods.value.length,
          )
        : 0,
  },
])

const topVodByLikes = computed(() =>
  vods.value
    .map((item) => {
      const detail = getSellerVodDetail(item.id)
      return {
        ...item,
        likes: detail?.metrics?.likes ?? 0,
        maxViewers: detail?.metrics?.maxViewers ?? 0,
        revenue: detail?.metrics?.totalRevenue ?? 0,
      }
    })
    .sort((a, b) => b.likes - a.likes)
    .slice(0, 5),
)

const goSection = (value: 'list' | 'stats') => {
  if (value === 'list') {
    router.push('/seller/live').catch(() => {})
    return
  }
}

onMounted(() => {
  scheduled.value = getScheduledBroadcasts()
  seededReservations.value = sellerReservationSummaries
  vods.value = sellerVodSummaries
})
</script>

<template>
  <div>
    <PageHeader eyebrow="DESKIT" title="방송 통계" />

    <header class="live-header">
      <div class="live-header__spacer" aria-hidden="true"></div>
      <div></div>
      <div class="live-header__right">
        <label class="inline-filter">
          <span>섹션</span>
          <select value="stats" @change="goSection(($event.target as HTMLSelectElement).value as any)">
            <option value="list">방송 목록</option>
            <option value="stats" selected>방송 통계</option>
          </select>
        </label>
      </div>
    </header>

    <section class="stat-grid">
      <article v-for="card in summaryCards" :key="card.label" class="ds-surface stat-card">
        <p class="stat-label">{{ card.label }}</p>
        <p class="stat-value">{{ card.value.toLocaleString('ko-KR') }}</p>
      </article>
    </section>

    <article class="ds-surface panel">
      <div class="panel__head">
        <div>
          <h3>인기 VOD</h3>
          <p class="ds-section-sub">좋아요 기준 상위 5개 VOD입니다.</p>
        </div>
      </div>
      <div class="list">
        <div v-for="item in topVodByLikes" :key="item.id" class="list-row">
          <div>
            <p class="list-title">{{ item.title }}</p>
            <p class="list-sub">업로드: {{ item.startedAt }} · 최대 {{ item.maxViewers.toLocaleString('ko-KR') }}명</p>
          </div>
          <div class="list-meta">
            <span class="badge-chip">👍 {{ item.likes.toLocaleString('ko-KR') }}</span>
            <span class="badge-chip">₩{{ (item.revenue ?? 0).toLocaleString('ko-KR') }}</span>
          </div>
        </div>
        <p v-if="!topVodByLikes.length" class="empty">데이터가 없습니다.</p>
      </div>
    </article>
  </div>
</template>

<style scoped>
.live-header {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  gap: 14px;
  margin: 12px 0 18px;
}

.live-header__spacer {
  min-height: 1px;
}

.live-header__right {
  display: flex;
  justify-content: flex-end;
}

.inline-filter {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-weight: 800;
  color: var(--text-strong);
}

.inline-filter select {
  border: 1px solid var(--border-color);
  border-radius: 10px;
  padding: 8px 10px;
  font-weight: 700;
  color: var(--text-strong);
  background: var(--surface);
}

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

.list-meta {
  display: flex;
  gap: 8px;
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
