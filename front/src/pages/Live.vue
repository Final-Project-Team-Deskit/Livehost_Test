<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import PageContainer from '../components/PageContainer.vue'
import PageHeader from '../components/PageHeader.vue'
import {
  fetchBroadcasts,
  type BroadcastListItem,
  type BroadcastSearch,
  type BroadcastTab,
} from '../api/liveApi'

const router = useRouter()

const TAB_OPTIONS: { label: string; value: BroadcastTab }[] = [
  { label: '전체', value: 'ALL' },
  { label: 'LIVE', value: 'LIVE' },
  { label: '예약', value: 'RESERVED' },
  { label: 'VOD', value: 'VOD' },
]

const filters = reactive<BroadcastSearch>({
  tab: 'ALL',
  keyword: '',
  sortType: 'LATEST',
  startDate: '',
  endDate: '',
})

const pageSize = 12
const broadcasts = ref<BroadcastListItem[]>([])
const hasNext = ref(true)
const page = ref(0)
const loading = ref(false)
const errorMessage = ref<string | null>(null)
const pendingKeyword = ref('')
const observerTarget = ref<HTMLElement | null>(null)
let intersectionObserver: IntersectionObserver | null = null

const statusLabel = (status: BroadcastListItem['status']) => {
  if (status === 'ON_AIR') return 'LIVE'
  if (status === 'RESERVED') return '예정'
  if (status === 'VOD') return 'VOD'
  return '종료'
}

const formatDateTime = (value?: string) => {
  if (!value) return ''
  const date = new Date(value)
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${month}.${day} ${hours}:${minutes}`
}

const getDisplayTime = (item: BroadcastListItem) => {
  if (item.status === 'RESERVED') {
    return formatDateTime(item.scheduledAt ?? item.startAt)
  }
  if (item.status === 'ON_AIR') {
    return formatDateTime(item.startedAt ?? item.startAt)
  }
  return formatDateTime(item.startAt)
}

const resetAndFetch = async () => {
  page.value = 0
  broadcasts.value = []
  hasNext.value = true
  await fetchList()
}

const fetchList = async (append = false) => {
  if (loading.value) return
  loading.value = true
  errorMessage.value = null

  try {
    const requestedPage = append ? page.value + 1 : 0
    const response = await fetchBroadcasts({
      ...filters,
      keyword: filters.keyword?.trim(),
      page: requestedPage,
      size: pageSize,
    })

    if (append) {
      broadcasts.value = [...broadcasts.value, ...response.content]
    } else {
      broadcasts.value = response.content
    }

    page.value = response.page ?? requestedPage
    hasNext.value = response.hasNext
  } catch (error) {
    console.error('Failed to fetch broadcasts', error)
    errorMessage.value = '라이브 목록을 불러오지 못했어요. 잠시 후 다시 시도해주세요.'
  } finally {
    loading.value = false
  }
}

const loadMore = async () => {
  if (!hasNext.value || loading.value) return
  await fetchList(true)
}

const handleSearchSubmit = async () => {
  filters.keyword = pendingKeyword.value
  await resetAndFetch()
}

const handleTabChange = async (value: BroadcastTab) => {
  filters.tab = value
  await resetAndFetch()
}

const handleRowClick = (item: BroadcastListItem) => {
  const id = item.id ?? item.broadcastId
  router.push({ name: 'live-detail', params: { id } })
}

const observeInfiniteScroll = () => {
  if (intersectionObserver) {
    intersectionObserver.disconnect()
  }

  intersectionObserver = new IntersectionObserver((entries) => {
    entries.forEach((entry) => {
      if (entry.isIntersecting) {
        loadMore()
      }
    })
  })

  if (observerTarget.value) {
    intersectionObserver.observe(observerTarget.value)
  }
}

onMounted(async () => {
  pendingKeyword.value = filters.keyword ?? ''
  await resetAndFetch()
  await nextTick()
  observeInfiniteScroll()
})

watch(
  () => [filters.sortType, filters.startDate, filters.endDate, filters.categoryId],
  async () => {
    await resetAndFetch()
  },
)

watch(
  () => filters.categoryId,
  (value) => {
    if (!Number.isFinite(value as number)) {
      filters.categoryId = undefined
    }
  },
)

onBeforeUnmount(() => {
  if (intersectionObserver) {
    intersectionObserver.disconnect()
  }
})

const liveCount = computed(() => broadcasts.value.filter((item) => item.status === 'ON_AIR').length)
const reservedCount = computed(() => broadcasts.value.filter((item) => item.status === 'RESERVED').length)
</script>

<template>
  <PageContainer>
    <PageHeader title="라이브 방송" eyebrow="DESKIT LIVE" />

    <div class="filter-bar">
      <div class="tabs" role="tablist">
        <button
          v-for="tab in TAB_OPTIONS"
          :key="tab.value"
          type="button"
          role="tab"
          :aria-selected="filters.tab === tab.value"
          :class="['tab-btn', { 'tab-btn--active': filters.tab === tab.value }]"
          @click="handleTabChange(tab.value)"
        >
          {{ tab.label }}
        </button>
      </div>

      <form class="search-form" @submit.prevent="handleSearchSubmit">
        <input
          v-model="pendingKeyword"
          class="search-input"
          type="search"
          placeholder="방송 제목, 판매자 검색"
        />
        <select v-model="filters.sortType" class="sort-select">
          <option value="LATEST">최신순</option>
          <option value="POPULAR">인기순</option>
          <option value="UPCOMING">방송 임박순</option>
        </select>
        <label class="number-field">
          <span>카테고리</span>
          <input v-model.number="filters.categoryId" type="number" min="0" placeholder="ID" />
        </label>
        <label class="date-field">
          <span>시작</span>
          <input v-model="filters.startDate" type="date" />
        </label>
        <label class="date-field">
          <span>종료</span>
          <input v-model="filters.endDate" type="date" />
        </label>
        <button type="submit" class="action-btn action-btn--primary">검색</button>
      </form>
    </div>

    <section class="live-grid" aria-live="polite">
      <article
        v-for="item in broadcasts"
        :key="item.id"
        class="live-card"
        @click="handleRowClick(item)"
      >
        <div class="thumb-wrap">
          <img
            class="thumb"
            :src="item.thumbnailUrl || '/placeholder-live-thumb.jpg'"
            :alt="item.title"
            loading="lazy"
          />
          <span class="status-pill" :class="`status-pill--${item.status.toLowerCase()}`">
            {{ statusLabel(item.status) }}
          </span>
        </div>
        <div class="meta">
          <div class="meta__title-row">
            <h4 class="meta__title">{{ item.title }}</h4>
            <span v-if="item.status === 'ON_AIR' && item.viewerCount" class="status-viewers">
              {{ item.viewerCount.toLocaleString() }}명 시청 중
            </span>
          </div>
          <p v-if="item.sellerName" class="meta__seller">{{ item.sellerName }}</p>
          <p v-if="item.description" class="meta__desc">{{ item.description }}</p>
          <p class="meta__time">{{ getDisplayTime(item) }}</p>
        </div>
      </article>
    </section>

    <div v-if="!broadcasts.length && !loading && !errorMessage" class="empty-state-box">
      <p>조건에 맞는 라이브가 없습니다.</p>
    </div>

    <div v-if="errorMessage" class="error-box">
      <p>{{ errorMessage }}</p>
      <button type="button" class="action-btn action-btn--ghost" @click="resetAndFetch">
        다시 시도
      </button>
    </div>

    <div class="load-more">
      <button
        v-if="hasNext"
        type="button"
        class="action-btn action-btn--ghost"
        :disabled="loading"
        @click="loadMore"
      >
        {{ loading ? '불러오는 중...' : '더 불러오기' }}
      </button>
      <p v-else class="load-more__end">모든 방송을 불러왔습니다.</p>
      <div ref="observerTarget" class="observer-target" aria-hidden="true"></div>
    </div>

    <div class="summary">
      <span>LIVE {{ liveCount }}</span>
      <span>예약 {{ reservedCount }}</span>
    </div>
  </PageContainer>
</template>

<style scoped>
.filter-bar {
  display: grid;
  gap: 12px;
  margin-bottom: 18px;
}

.tabs {
  display: inline-flex;
  gap: 10px;
  padding: 6px;
  background: var(--surface);
  border: 1px solid var(--border-color);
  border-radius: 12px;
}

.tab-btn {
  border: none;
  background: transparent;
  padding: 10px 16px;
  border-radius: 10px;
  font-weight: 800;
  cursor: pointer;
  color: var(--text-muted);
}

.tab-btn--active {
  background: var(--primary-color);
  color: #fff;
}

.search-form {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 10px;
  align-items: center;
}

.search-input,
.sort-select,
.date-field input,
.number-field input {
  width: 100%;
  border: 1px solid var(--border-color);
  border-radius: 10px;
  padding: 10px 12px;
  font-size: 0.95rem;
}

.sort-select {
  background: var(--surface);
}

.date-field {
  display: grid;
  gap: 6px;
  color: var(--text-muted);
  font-weight: 700;
}

.number-field {
  display: grid;
  gap: 6px;
  color: var(--text-muted);
  font-weight: 700;
}

.live-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 14px;
}

.live-card {
  display: grid;
  gap: 10px;
  border: 1px solid var(--border-color);
  border-radius: 16px;
  padding: 10px;
  background: var(--surface);
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.live-card:hover {
  border-color: var(--primary-color);
  box-shadow: 0 10px 22px rgba(119, 136, 115, 0.12);
  transform: translateY(-1px);
}

.thumb-wrap {
  position: relative;
  overflow: hidden;
  border-radius: 12px;
}

.thumb {
  width: 100%;
  height: 160px;
  object-fit: cover;
  display: block;
}

.status-pill {
  position: absolute;
  left: 10px;
  top: 10px;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 0.8rem;
  font-weight: 800;
  background: var(--surface);
  color: var(--text-strong);
}

.status-pill--on_air {
  background: var(--live-color-soft);
  color: var(--live-color);
}

.status-pill--reserved {
  background: var(--hover-bg);
  color: var(--primary-color);
}

.status-pill--ended {
  background: var(--border-color);
  color: var(--text-muted);
}

.status-pill--vod {
  background: var(--surface-weak);
}

.meta {
  display: grid;
  gap: 6px;
}

.meta__title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.meta__title {
  margin: 0;
  font-size: 1rem;
  font-weight: 800;
  color: var(--text-strong);
  line-height: 1.35;
}

.meta__seller {
  margin: 0;
  color: var(--text-muted);
  font-size: 0.9rem;
}

.meta__desc {
  margin: 0;
  color: var(--text-soft);
  font-size: 0.9rem;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.meta__time {
  margin: 0;
  color: var(--text-muted);
  font-weight: 700;
}

.status-viewers {
  font-size: 0.85rem;
  color: var(--live-color);
  font-weight: 800;
}

.empty-state-box,
.error-box {
  margin-top: 14px;
  padding: 18px;
  border: 1px dashed var(--border-color);
  border-radius: 14px;
  background: var(--surface);
  color: var(--text-muted);
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.load-more {
  display: grid;
  place-items: center;
  gap: 8px;
  margin: 16px 0;
}

.load-more__end {
  color: var(--text-muted);
}

.observer-target {
  width: 100%;
  height: 1px;
}

.action-btn {
  border: none;
  background: var(--primary-color);
  color: #fff;
  font-weight: 800;
  border-radius: 12px;
  padding: 10px 14px;
  cursor: pointer;
}

.action-btn--ghost {
  background: var(--surface);
  color: var(--text-strong);
  border: 1px solid var(--border-color);
}

.action-btn--primary {
  background: var(--primary-color);
}

.summary {
  display: flex;
  gap: 12px;
  color: var(--text-soft);
  font-weight: 700;
}

@media (max-width: 640px) {
  .search-form {
    grid-template-columns: 1fr;
  }
}
</style>
