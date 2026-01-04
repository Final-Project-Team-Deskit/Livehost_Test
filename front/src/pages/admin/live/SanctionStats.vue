<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import PageHeader from '../../../components/PageHeader.vue'

type SanctionRow = {
  id: string
  user: string
  reason: string
  action: string
  count: number
  recentAt: string
}

const router = useRouter()

const sanctions = ref<SanctionRow[]>([
  { id: 'san-1', user: '판매자 A', reason: '음란물', action: '송출 중지', count: 12, recentAt: '2025-01-06' },
  { id: 'san-2', user: '판매자 B', reason: '폭력적 표현', action: '채널 제재', count: 7, recentAt: '2025-01-04' },
  { id: 'san-3', user: '판매자 C', reason: '불법 상품', action: '채널 제재', count: 5, recentAt: '2025-01-02' },
  { id: 'san-4', user: '판매자 D', reason: '비방/혐오', action: '경고', count: 9, recentAt: '2025-01-01' },
])

const summaryCards = computed(() => [
  { label: '총 제재 건수', value: sanctions.value.reduce((sum, item) => sum + item.count, 0) },
  { label: '대상 수', value: sanctions.value.length },
  { label: '최근 제재', value: sanctions.value[0]?.recentAt ?? '-' },
])

const goSection = (value: 'list' | 'stats' | 'sanctions') => {
  if (value === 'list') {
    router.push('/admin/live').catch(() => {})
    return
  }
  if (value === 'stats') {
    router.push('/admin/live/stats').catch(() => {})
    return
  }
}
</script>

<template>
  <div>
    <PageHeader eyebrow="DESKIT" title="제재 통계" />

    <header class="live-header">
      <div class="live-header__spacer" aria-hidden="true"></div>
      <div></div>
      <div class="live-header__right">
        <label class="inline-filter">
          <span>섹션</span>
          <select value="sanctions" @change="goSection(($event.target as HTMLSelectElement).value as any)">
            <option value="list">방송 목록</option>
            <option value="stats">방송 통계</option>
            <option value="sanctions" selected>제재 통계</option>
          </select>
        </label>
      </div>
    </header>

    <section class="stat-grid">
      <article v-for="card in summaryCards" :key="card.label" class="ds-surface stat-card">
        <p class="stat-label">{{ card.label }}</p>
        <p class="stat-value">{{ card.value }}</p>
      </article>
    </section>

    <article class="ds-surface panel">
      <div class="panel__head">
        <div>
          <h3>제재 현황</h3>
          <p class="ds-section-sub">최근 제재 건 목록입니다.</p>
        </div>
      </div>
      <div class="table">
        <div class="table-head">
          <span>대상</span>
          <span>사유</span>
          <span>조치</span>
          <span>누적</span>
          <span>최근 일자</span>
        </div>
        <div v-for="row in sanctions" :key="row.id" class="table-row">
          <span class="cell-strong">{{ row.user }}</span>
          <span>{{ row.reason }}</span>
          <span>{{ row.action }}</span>
          <span>{{ row.count }}건</span>
          <span>{{ row.recentAt }}</span>
        </div>
        <p v-if="!sanctions.length" class="empty">데이터가 없습니다.</p>
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

.table {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.table-head,
.table-row {
  display: grid;
  grid-template-columns: 1.2fr 1fr 1fr 0.8fr 1fr;
  gap: 10px;
  align-items: center;
}

.table-head {
  font-weight: 900;
  color: var(--text-strong);
  border-bottom: 1px solid var(--border-color);
  padding-bottom: 6px;
}

.table-row {
  padding: 8px 0;
  border-bottom: 1px solid var(--border-color);
}

.table-row:last-child {
  border-bottom: none;
}

.cell-strong {
  font-weight: 900;
  color: var(--text-strong);
}

.empty {
  margin: 0;
  color: var(--text-muted);
  font-weight: 700;
}
</style>
