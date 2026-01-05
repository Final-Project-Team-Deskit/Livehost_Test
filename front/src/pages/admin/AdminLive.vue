<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PageHeader from '../../components/PageHeader.vue'
import {
  ADMIN_RESERVATIONS_EVENT,
  getAdminReservationSummaries,
  type AdminReservationSummary,
} from '../../lib/mocks/adminReservations'
import { ADMIN_LIVES_EVENT, getAdminLiveSummaries, type AdminLiveSummary } from '../../lib/mocks/adminLives'
import { ADMIN_VODS_EVENT, getAdminVodSummaries, type AdminVodSummary } from '../../lib/mocks/adminVods'

type LiveTab = 'all' | 'scheduled' | 'live' | 'vod'

type LiveItem = {
  id: string
  title: string
  subtitle: string
  thumb: string
  datetime: string
  statusBadge?: string
  viewerBadge?: string
  ctaLabel: string
  sellerName?: string
  status?: string
  viewers?: number
  reports?: number
  category?: string
  startedAtMs?: number
}

type ReservationItem = LiveItem & {
  sellerName: string
  status: string
  category: string
  startAtMs?: number
}

type AdminVodItem = LiveItem & {
  sellerName: string
  statusLabel: string
  category: string
  metrics: AdminVodSummary['metrics']
  startAtMs?: number
  endAtMs?: number
  visibility?: 'public' | 'private'
}

const router = useRouter()
const route = useRoute()

const activeTab = ref<LiveTab>('all')

const liveCategory = ref<string>('all')
const liveSort = ref<'reports_desc' | 'latest' | 'viewers_desc' | 'viewers_asc'>('reports_desc')
const liveVisibleCount = ref(5)
const liveCarouselRef = ref<HTMLElement | null>(null)

const scheduledStatus = ref<'all' | 'reserved' | 'canceled'>('all')
const scheduledCategory = ref<string>('all')
const scheduledSort = ref<'nearest' | 'latest' | 'oldest'>('nearest')
const scheduledVisibleCount = ref(8)

const vodStartDate = ref('')
const vodEndDate = ref('')
const vodVisibility = ref<'all' | 'public' | 'private'>('all')
const vodSort = ref<
  | 'latest'
  | 'oldest'
  | 'reports_desc'
  | 'likes_desc'
  | 'likes_asc'
  | 'revenue_desc'
  | 'revenue_asc'
  | 'viewers_desc'
  | 'viewers_asc'
>('latest')
const vodCategory = ref<string>('all')
const vodVisibleCount = ref(8)

const liveItems = ref<AdminLiveSummary[]>([])
const scheduledItems = ref<ReservationItem[]>([])
const vodItems = ref<AdminVodItem[]>([])
const liveAutoTimer = ref<number | null>(null)

const visibleLive = computed(() => activeTab.value === 'all' || activeTab.value === 'live')
const visibleScheduled = computed(() => activeTab.value === 'all' || activeTab.value === 'scheduled')
const visibleVod = computed(() => activeTab.value === 'all' || activeTab.value === 'vod')

const setTab = (tab: LiveTab) => {
  activeTab.value = tab
}

const toDateMs = (raw: string) => {
  const parsed = Date.parse(raw.replace(/\./g, '-').replace(' ', 'T'))
  return Number.isNaN(parsed) ? 0 : parsed
}

const syncScheduled = () => {
  const items = getAdminReservationSummaries()
  scheduledItems.value = items
    .filter((item) => item.status !== '취소됨')
    .map((item: AdminReservationSummary) => ({
      id: item.id,
      title: item.title,
      subtitle: item.subtitle,
      thumb: item.thumb,
      datetime: item.datetime,
      ctaLabel: item.ctaLabel,
      sellerName: item.sellerName,
      status: item.status,
      category: (item as any).category ?? '기타',
      startAtMs: toDateMs(item.datetime),
    }))
}

const syncLives = () => {
  const items = getAdminLiveSummaries().filter((item) => item.status === '방송중')
  liveItems.value = items
}

const syncVods = () => {
  const items = getAdminVodSummaries()
  vodItems.value = items.map((item: AdminVodSummary) => {
    const startAtMs = toDateMs(item.startedAt)
    const endAtMs = toDateMs(item.endedAt)
    const visibility = item.statusLabel === '비공개' ? 'private' : 'public'
    return {
      ...item,
      subtitle: '',
      datetime: `업로드: ${item.startedAt}`,
      ctaLabel: '상세보기',
      startAtMs,
      endAtMs,
      visibility,
    }
  })
}

const filteredLive = computed(() => {
  let filtered = [...liveItems.value]
  if (liveCategory.value !== 'all') {
    filtered = filtered.filter((item) => item.category === liveCategory.value)
  }
  filtered.sort((a, b) => {
    if (liveSort.value === 'reports_desc') return (b.reports ?? 0) - (a.reports ?? 0)
    if (liveSort.value === 'latest') return toDateMs(b.startedAt) - toDateMs(a.startedAt)
    if (liveSort.value === 'viewers_desc') return (b.viewers ?? 0) - (a.viewers ?? 0)
    if (liveSort.value === 'viewers_asc') return (a.viewers ?? 0) - (b.viewers ?? 0)
    return 0
  })
  return filtered
})

const filteredScheduled = computed(() => {
  let filtered = [...scheduledItems.value]
  if (scheduledStatus.value === 'reserved') {
    filtered = filtered.filter((item) => item.status !== '취소됨')
  } else if (scheduledStatus.value === 'canceled') {
    filtered = filtered.filter((item) => item.status === '취소됨')
  }
  if (scheduledCategory.value !== 'all') {
    filtered = filtered.filter((item) => item.category === scheduledCategory.value)
  }
  filtered.sort((a, b) => {
    const aDate = a.startAtMs ?? toDateMs(a.datetime)
    const bDate = b.startAtMs ?? toDateMs(b.datetime)
    if (scheduledSort.value === 'latest') return bDate - aDate
    if (scheduledSort.value === 'oldest') return aDate - bDate
    return aDate - bDate
  })
  return filtered
})

const filteredVods = computed(() => {
  const startMs = vodStartDate.value ? Date.parse(`${vodStartDate.value}T00:00:00`) : null
  const endMs = vodEndDate.value ? Date.parse(`${vodEndDate.value}T23:59:59`) : null

  let filtered = [...vodItems.value].filter((item) => {
    const dateMs = item.startAtMs ?? toDateMs(item.startedAt)
    if (startMs && dateMs < startMs) return false
    if (endMs && dateMs > endMs) return false
    if (vodVisibility.value !== 'all' && vodVisibility.value !== item.visibility) return false
    if (vodCategory.value !== 'all' && item.category !== vodCategory.value) return false
    return true
  })

  filtered.sort((a, b) => {
    if (vodSort.value === 'latest') return (b.startAtMs ?? 0) - (a.startAtMs ?? 0)
    if (vodSort.value === 'oldest') return (a.startAtMs ?? 0) - (b.startAtMs ?? 0)
    if (vodSort.value === 'reports_desc') return (b.metrics.reports ?? 0) - (a.metrics.reports ?? 0)
    if (vodSort.value === 'likes_desc') return (b.metrics.likes ?? 0) - (a.metrics.likes ?? 0)
    if (vodSort.value === 'likes_asc') return (a.metrics.likes ?? 0) - (b.metrics.likes ?? 0)
    if (vodSort.value === 'revenue_desc') return (b.metrics.totalRevenue ?? 0) - (a.metrics.totalRevenue ?? 0)
    if (vodSort.value === 'revenue_asc') return (a.metrics.totalRevenue ?? 0) - (b.metrics.totalRevenue ?? 0)
    if (vodSort.value === 'viewers_desc') return (b.metrics.maxViewers ?? 0) - (a.metrics.maxViewers ?? 0)
    if (vodSort.value === 'viewers_asc') return (a.metrics.maxViewers ?? 0) - (b.metrics.maxViewers ?? 0)
    return 0
  })

  return filtered
})

const visibleLiveItems = computed(() => filteredLive.value.slice(0, liveVisibleCount.value))
const visibleScheduledItems = computed(() => filteredScheduled.value.slice(0, scheduledVisibleCount.value))
const visibleVodItems = computed(() => filteredVods.value.slice(0, vodVisibleCount.value))

const liveCategories = computed(() => Array.from(new Set(liveItems.value.map((item) => item.category ?? '기타'))))
const scheduledCategories = computed(() => Array.from(new Set(scheduledItems.value.map((item) => item.category ?? '기타'))))
const vodCategories = computed(() => Array.from(new Set(vodItems.value.map((item) => item.category ?? '기타'))))

const liveCarouselItems = computed(() => visibleLiveItems.value.slice(0, 5))

const filteredLive = computed(() => {
  let filtered = [...liveItems.value]
  if (liveCategory.value !== 'all') {
    filtered = filtered.filter((item) => item.category === liveCategory.value)
  }
  filtered.sort((a, b) => {
    if (liveSort.value === 'reports_desc') return (b.reports ?? 0) - (a.reports ?? 0)
    if (liveSort.value === 'latest') return toDateMs(b.startedAt) - toDateMs(a.startedAt)
    if (liveSort.value === 'viewers_desc') return (b.viewers ?? 0) - (a.viewers ?? 0)
    if (liveSort.value === 'viewers_asc') return (a.viewers ?? 0) - (b.viewers ?? 0)
    return 0
  })
  return filtered
})

const filteredScheduled = computed(() => {
  let filtered = [...scheduledItems.value]
  if (scheduledStatus.value === 'reserved') {
    filtered = filtered.filter((item) => item.status !== '취소됨')
  } else if (scheduledStatus.value === 'canceled') {
    filtered = filtered.filter((item) => item.status === '취소됨')
  }
  if (scheduledCategory.value !== 'all') {
    filtered = filtered.filter((item) => item.category === scheduledCategory.value)
  }
  filtered.sort((a, b) => {
    const aDate = a.startAtMs ?? toDateMs(a.datetime)
    const bDate = b.startAtMs ?? toDateMs(b.datetime)
    if (scheduledSort.value === 'latest') return bDate - aDate
    if (scheduledSort.value === 'oldest') return aDate - bDate
    return aDate - bDate
  })
  return filtered
})

const filteredVods = computed(() => {
  const startMs = vodStartDate.value ? Date.parse(`${vodStartDate.value}T00:00:00`) : null
  const endMs = vodEndDate.value ? Date.parse(`${vodEndDate.value}T23:59:59`) : null

  let filtered = [...vodItems.value].filter((item) => {
    const dateMs = item.startAtMs ?? toDateMs(item.startedAt)
    if (startMs && dateMs < startMs) return false
    if (endMs && dateMs > endMs) return false
    if (vodVisibility.value !== 'all' && vodVisibility.value !== item.visibility) return false
    if (vodCategory.value !== 'all' && item.category !== vodCategory.value) return false
    return true
  })

  filtered.sort((a, b) => {
    if (vodSort.value === 'latest') return (b.startAtMs ?? 0) - (a.startAtMs ?? 0)
    if (vodSort.value === 'oldest') return (a.startAtMs ?? 0) - (b.startAtMs ?? 0)
    if (vodSort.value === 'reports_desc') return (b.metrics.reports ?? 0) - (a.metrics.reports ?? 0)
    if (vodSort.value === 'likes_desc') return (b.metrics.likes ?? 0) - (a.metrics.likes ?? 0)
    if (vodSort.value === 'likes_asc') return (a.metrics.likes ?? 0) - (b.metrics.likes ?? 0)
    if (vodSort.value === 'revenue_desc') return (b.metrics.totalRevenue ?? 0) - (a.metrics.totalRevenue ?? 0)
    if (vodSort.value === 'revenue_asc') return (a.metrics.totalRevenue ?? 0) - (b.metrics.totalRevenue ?? 0)
    if (vodSort.value === 'viewers_desc') return (b.metrics.maxViewers ?? 0) - (a.metrics.maxViewers ?? 0)
    if (vodSort.value === 'viewers_asc') return (a.metrics.maxViewers ?? 0) - (b.metrics.maxViewers ?? 0)
    return 0
  })

  return filtered
})

const visibleLiveItems = computed(() => filteredLive.value.slice(0, liveVisibleCount.value))
const visibleScheduledItems = computed(() => filteredScheduled.value.slice(0, scheduledVisibleCount.value))
const visibleVodItems = computed(() => filteredVods.value.slice(0, vodVisibleCount.value))

const liveCategories = computed(() => Array.from(new Set(liveItems.value.map((item) => item.category ?? '기타'))))
const scheduledCategories = computed(() => Array.from(new Set(scheduledItems.value.map((item) => item.category ?? '기타'))))
const vodCategories = computed(() => Array.from(new Set(vodItems.value.map((item) => item.category ?? '기타'))))

const liveCarouselItems = computed(() => visibleLiveItems.value.slice(0, 5))

const openReservationDetail = (id: string) => {
  if (!id) return
  router.push(`/admin/live/reservations/${id}`).catch(() => {})
}

const openLiveDetail = (id: string) => {
  if (!id) return
  router.push(`/admin/live/now/${id}`).catch(() => {})
}

const openVodDetail = (id: string) => {
  if (!id) return
  router.push(`/admin/live/vods/${id}`).catch(() => {})
}

const formatDDay = (item: { startAtMs?: number }) => {
  if (!item.startAtMs) return ''
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const start = new Date(item.startAtMs)
  start.setHours(0, 0, 0, 0)
  const diffDays = Math.round((start.getTime() - today.getTime()) / (1000 * 60 * 60 * 24))
  if (diffDays === 0) return 'D-Day'
  if (diffDays > 0) return `D-${diffDays}`
  return `D+${Math.abs(diffDays)}`
}

const refreshTabFromQuery = () => {
  const tab = route.query.tab
  if (tab === 'scheduled' || tab === 'live' || tab === 'vod' || tab === 'all') {
    activeTab.value = tab
  }
}

const scrollLiveCarousel = (dir: 'prev' | 'next') => {
  const el = liveCarouselRef.value
  if (!el) return
  const offset = el.clientWidth * 0.9
  el.scrollBy({ left: dir === 'next' ? offset : -offset, behavior: 'smooth' })
}

const stopLiveAutoSlide = () => {
  if (liveAutoTimer.value) {
    window.clearInterval(liveAutoTimer.value)
    liveAutoTimer.value = null
  }
}

const startLiveAutoSlide = () => {
  stopLiveAutoSlide()
  if (!liveCarouselItems.value.length) return
  liveAutoTimer.value = window.setInterval(() => scrollLiveCarousel('next'), 3200)
}

watch(
  () => route.query.tab,
  () => refreshTabFromQuery(),
)

watch([liveCategory, liveSort], () => {
  liveVisibleCount.value = 5
  startLiveAutoSlide()
})

watch([scheduledStatus, scheduledCategory, scheduledSort], () => {
  scheduledVisibleCount.value = 8
})

watch([vodStartDate, vodEndDate, vodVisibility, vodCategory, vodSort], () => {
  vodVisibleCount.value = 8
})

onMounted(() => {
  refreshTabFromQuery()
  syncLives()
  syncScheduled()
  syncVods()
  startLiveAutoSlide()
  window.addEventListener(ADMIN_LIVES_EVENT, syncLives)
  window.addEventListener(ADMIN_RESERVATIONS_EVENT, syncScheduled)
  window.addEventListener(ADMIN_VODS_EVENT, syncVods)
})

onBeforeUnmount(() => {
  window.removeEventListener(ADMIN_LIVES_EVENT, syncLives)
  window.removeEventListener(ADMIN_RESERVATIONS_EVENT, syncScheduled)
  window.removeEventListener(ADMIN_VODS_EVENT, syncVods)
  stopLiveAutoSlide()
})
</script>

<template>
  <div>
    <PageHeader eyebrow="DESKIT" title="방송관리" />

    <header class="live-header">
      <div class="live-header__spacer" aria-hidden="true"></div>

      <div class="live-tabs" role="tablist" aria-label="방송 상태">
        <button
          type="button"
          class="live-tab"
          :class="{ 'live-tab--active': activeTab === 'all' }"
          @click="setTab('all')"
        >
          전체
        </button>
        <button
          type="button"
          class="live-tab"
          :class="{ 'live-tab--active': activeTab === 'scheduled' }"
          @click="setTab('scheduled')"
        >
          예약
        </button>
        <button
          type="button"
          class="live-tab"
          :class="{ 'live-tab--active': activeTab === 'live' }"
          @click="setTab('live')"
        >
          방송 중
        </button>
        <button
          type="button"
          class="live-tab"
          :class="{ 'live-tab--active': activeTab === 'vod' }"
          @click="setTab('vod')"
        >
          VOD
        </button>
      </div>

      <div class="live-header__right"></div>
    </header>

    <section v-if="visibleLive" class="live-section">
      <div class="live-section__head">
        <div class="live-section__title">
          <h3>방송 중</h3>
        </div>
        <div class="live-section__desc">
          <p v-if="activeTab !== 'all'" class="ds-section-sub">현재 진행 중인 라이브 방송입니다.</p>
          <span v-else class="link-more" role="button" tabindex="0" @click="setTab('live')">+ 더보기</span>
        </div>
      </div>

      <div v-if="activeTab === 'live'" class="filter-bar">
        <label class="filter-field">
          <span class="filter-label">카테고리</span>
          <select v-model="liveCategory">
            <option value="all">모든 카테고리</option>
            <option v-for="category in liveCategories" :key="category" :value="category">{{ category }}</option>
          </select>
        </label>
        <label class="filter-field">
          <span class="filter-label">정렬</span>
          <select v-model="liveSort">
            <option value="reports_desc">신고가 많은 순</option>
            <option value="latest">최신순</option>
            <option value="viewers_desc">시청자가 많은 순</option>
            <option value="viewers_asc">시청자가 적은 순</option>
          </select>
        </label>
      </div>

      <div class="carousel-wrap">
        <button v-if="liveCarouselItems.length" type="button" class="carousel-btn prev" @click="scrollLiveCarousel('prev')" aria-label="이전">
          ‹
        </button>
        <div ref="liveCarouselRef" class="live-carousel" aria-label="방송 중 목록">
          <template v-if="liveCarouselItems.length">
            <article
              v-for="item in liveCarouselItems"
              :key="item.id"
              class="live-card ds-surface live-card--clickable"
              @click="openLiveDetail(item.id)"
            >
              <div class="live-thumb">
                <img class="live-thumb__img" :src="item.thumb" :alt="item.title" loading="lazy" />
                <div class="live-badges">
                  <span class="badge badge--live">{{ item.status }}</span>
                  <span class="badge badge--viewer">시청자 {{ item.viewers }}명</span>
                </div>
              </div>
              <div class="live-body">
                <div class="live-meta">
                  <p class="live-title">{{ item.title }}</p>
                  <p class="live-date">시작: {{ item.startedAt }}</p>
                  <p class="live-seller">{{ item.sellerName }}</p>
                  <p class="live-viewers">신고 {{ item.reports ?? 0 }}건 · 좋아요 {{ item.likes }}</p>
                </div>
              </div>
            </article>
          </template>

          <p v-else class="empty-section">진행 중인 방송이 없습니다.</p>
        </div>
        <button v-if="liveCarouselItems.length" type="button" class="carousel-btn next" @click="scrollLiveCarousel('next')" aria-label="다음">
          ›
        </button>
      </div>
    </section>

    <section v-if="visibleScheduled" class="live-section">
      <div class="live-section__head">
        <div class="live-section__title">
          <h3>예약된 방송</h3>
        </div>
        <div class="live-section__desc">
          <p v-if="activeTab !== 'all'" class="ds-section-sub">예정된 라이브 스케줄을 관리하세요.</p>
          <span
            v-else
            class="link-more"
            role="button"
            tabindex="0"
            @click="setTab('scheduled')"
          >
            + 더보기
          </span>
        </div>
      </div>

      <div v-if="activeTab === 'scheduled'" class="filter-bar">
        <label class="filter-field">
          <span class="filter-label">상태</span>
          <select v-model="scheduledStatus">
            <option value="all">전체</option>
            <option value="reserved">예약중</option>
            <option value="canceled">취소됨</option>
          </select>
        </label>
        <label class="filter-field">
          <span class="filter-label">카테고리</span>
          <select v-model="scheduledCategory">
            <option value="all">전체</option>
            <option v-for="category in scheduledCategories" :key="category" :value="category">{{ category }}</option>
          </select>
        </label>
        <label class="filter-field">
          <span class="filter-label">정렬</span>
          <select v-model="scheduledSort">
            <option value="nearest">방송 시간이 가까운 순</option>
            <option value="latest">최신순</option>
            <option value="oldest">오래된 순</option>
          </select>
        </label>
      </div>

      <div class="scheduled-grid" aria-label="예약 방송 목록">
        <template v-if="visibleScheduledItems.length">
          <article
            v-for="item in visibleScheduledItems"
            :key="item.id"
            class="live-card ds-surface live-card--clickable"
            @click="openReservationDetail(item.id)"
          >
              <div class="live-thumb">
                <img class="live-thumb__img" :src="item.thumb" :alt="item.title" loading="lazy" />
                <div class="live-badges">
                  <span class="badge badge--scheduled" :class="{ 'badge--cancelled': item.status === '취소됨' }">
                    {{ item.status }}
                  </span>
                  <span class="badge badge--viewer">{{ formatDDay(item) }}</span>
                </div>
              </div>
            <div class="live-body">
              <div class="live-meta">
                <p class="live-title">{{ item.title }}</p>
                <p class="live-date">{{ item.datetime }}</p>
                <p class="live-seller">{{ item.sellerName }}</p>
                <p class="live-viewers">{{ item.category }}</p>
              </div>
            </div>
          </article>
        </template>

        <p v-else class="empty-section">예약된 방송이 없습니다.</p>
      </div>
    </section>

    <section v-if="visibleVod" class="live-section">
      <div class="live-section__head">
        <div class="live-section__title">
          <h3>VOD</h3>
        </div>
        <div class="live-section__desc">
          <p v-if="activeTab !== 'all'" class="ds-section-sub">저장된 다시보기 콘텐츠를 확인합니다.</p>
          <span
            v-else
            class="link-more"
            role="button"
            tabindex="0"
            @click="setTab('vod')"
          >
            + 더보기
          </span>
        </div>
      </div>

      <div v-if="activeTab === 'vod'" class="filter-bar">
        <label class="filter-field">
          <span class="filter-label">날짜 시작</span>
          <input v-model="vodStartDate" type="date" />
        </label>
        <label class="filter-field">
          <span class="filter-label">날짜 종료</span>
          <input v-model="vodEndDate" type="date" />
        </label>
        <label class="filter-field">
          <span class="filter-label">공개 여부</span>
          <select v-model="vodVisibility">
            <option value="all">전체</option>
            <option value="public">공개</option>
            <option value="private">비공개</option>
          </select>
        </label>
        <label class="filter-field">
          <span class="filter-label">카테고리</span>
          <select v-model="vodCategory">
            <option value="all">모든 카테고리</option>
            <option v-for="category in vodCategories" :key="category" :value="category">{{ category }}</option>
          </select>
        </label>
        <label class="filter-field">
          <span class="filter-label">정렬</span>
          <select v-model="vodSort">
            <option value="latest">최신순</option>
            <option value="reports_desc">신고 건수가 많은 순</option>
            <option value="oldest">오래된 순</option>
            <option value="likes_desc">좋아요가 높은 순</option>
            <option value="likes_asc">좋아요가 낮은 순</option>
            <option value="revenue_desc">매출액이 높은 순</option>
            <option value="revenue_asc">매출액이 낮은 순</option>
            <option value="viewers_desc">총 시청자 수가 높은 순</option>
            <option value="viewers_asc">총 시청자 수가 낮은 순</option>
          </select>
        </label>
      </div>

      <div class="vod-grid" aria-label="VOD 목록">
        <template v-if="visibleVodItems.length">
          <article
            v-for="item in visibleVodItems"
            :key="item.id"
            class="live-card ds-surface live-card--clickable"
            @click="openVodDetail(item.id)"
          >
            <div class="live-thumb">
              <img class="live-thumb__img" :src="item.thumb" :alt="item.title" loading="lazy" />
              <div class="live-badges">
                <span class="badge badge--vod">{{ item.statusLabel }}</span>
                <span class="badge badge--viewer">신고 {{ item.metrics.reports }}</span>
              </div>
            </div>
            <div class="live-body">
              <div class="live-meta">
                <p class="live-title">{{ item.title }}</p>
                <p class="live-date">{{ item.datetime }}</p>
                <p class="live-seller">{{ item.sellerName }}</p>
                <p class="live-viewers">좋아요 {{ item.metrics.likes }} · 시청 {{ item.metrics.maxViewers }}</p>
              </div>
            </div>
          </article>
        </template>

        <p v-else class="empty-section">등록된 VOD가 없습니다.</p>
      </div>
    </section>
  </div>
</template>

<style scoped>
.live-header {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  gap: 14px;
  margin-bottom: 18px;
}

.live-header__spacer {
  min-height: 1px;
}

.live-header__right {
  display: flex;
  justify-content: flex-end;
  align-items: center;
}

.inline-filter {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-weight: 800;
  color: var(--text-strong);
}

.inline-filter select,
.inline-filter input {
  border: 1px solid var(--border-color);
  border-radius: 10px;
  padding: 8px 10px;
  font-weight: 700;
  color: var(--text-strong);
  background: var(--surface);
}

.live-tabs {
  display: inline-flex;
  gap: 10px;
  justify-content: center;
}

.live-tab {
  border: 1px solid var(--border-color);
  background: #fff;
  color: var(--text-strong);
  border-radius: 999px;
  padding: 10px 18px;
  font-weight: 800;
  cursor: pointer;
  transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}

.live-tab:hover {
  border-color: var(--primary-color);
  box-shadow: 0 8px 18px rgba(var(--primary-rgb), 0.12);
  transform: translateY(-1px);
}

.live-tab--active {
  background: var(--surface-weak);
  border-color: var(--primary-color);
}

.live-section {
  margin-top: 18px;
  padding-top: 18px;
  border-top: 1px solid rgba(15, 23, 42, 0.08);
}

.live-section:first-of-type {
  margin-top: 0;
  padding-top: 0;
  border-top: none;
}

.live-section__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.live-section__head h3 {
  margin: 0;
  font-size: 1.3rem;
  font-weight: 900;
  color: var(--text-strong);
}

.live-section__title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.live-section__desc {
  display: flex;
  align-items: center;
  gap: 10px;
}

.live-section__controls {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: flex-end;
}

.filter-row {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.vod-filter-row {
  align-items: flex-end;
}

.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  padding: 12px;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  background: var(--surface);
  margin-bottom: 12px;
}

.filter-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 140px;
}

.filter-label {
  font-weight: 800;
  color: var(--text-strong);
  font-size: 0.85rem;
}

.filter-field select,
.filter-field input {
  border: 1px solid var(--border-color);
  border-radius: 10px;
  padding: 10px 12px;
  font-weight: 700;
  color: var(--text-strong);
  background: var(--surface);
}

.more-row {
  width: 100%;
  display: flex;
  justify-content: flex-end;
}

.carousel-wrap {
  position: relative;
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 10px;
}

.live-carousel {
  display: grid;
  grid-auto-flow: column;
  grid-auto-columns: minmax(280px, 320px);
  gap: 14px;
  overflow-x: auto;
  padding: 10px 4px;
  scroll-snap-type: x mandatory;
  -webkit-overflow-scrolling: touch;
}

.vod-filter-row {
  align-items: flex-end;
}

.more-row {
  width: 100%;
  display: flex;
  justify-content: flex-end;
}

.carousel-wrap {
  position: relative;
}

.live-grid,
.scheduled-grid,
.vod-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.carousel-btn {
  border: 1px solid var(--border-color);
  border-radius: 50%;
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  background: var(--surface);
  font-weight: 900;
  color: var(--text-strong);
  cursor: pointer;
}

.carousel-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.live-grid,
.scheduled-grid,
.vod-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.carousel-btn {
  border: 1px solid var(--border-color);
  border-radius: 50%;
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  background: var(--surface);
  font-weight: 900;
  color: var(--text-strong);
  cursor: pointer;
}

.carousel-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.live-grid,
.scheduled-grid,
.vod-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.carousel-btn {
  border: 1px solid var(--border-color);
  border-radius: 50%;
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  background: var(--surface);
  font-weight: 900;
  color: var(--text-strong);
  cursor: pointer;
}

.carousel-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.live-grid,
.scheduled-grid,
.vod-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.carousel-btn {
  border: 1px solid var(--border-color);
  border-radius: 50%;
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  background: var(--surface);
  font-weight: 900;
  color: var(--text-strong);
  cursor: pointer;
}

.carousel-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.live-grid,
.scheduled-grid,
.vod-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.carousel-btn {
  border: 1px solid var(--border-color);
  border-radius: 50%;
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  background: var(--surface);
  font-weight: 900;
  color: var(--text-strong);
  cursor: pointer;
}

.carousel-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.live-grid,
.scheduled-grid,
.vod-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.live-card {
  scroll-snap-align: start;
  border-radius: 14px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  min-height: 260px;
}

.live-card--clickable {
  cursor: pointer;
}

.live-thumb {
  position: relative;
  height: 170px;
  background: var(--surface-weak);
}

.live-thumb__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.live-badges {
  position: absolute;
  right: 10px;
  top: 10px;
  display: inline-flex;
  gap: 8px;
  align-items: center;
}

.badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  padding: 8px 10px;
  font-weight: 900;
  line-height: 1;
  font-size: 0.9rem;
}

.badge--viewer {
  background: rgba(15, 23, 42, 0.85);
  color: #fff;
}

.badge--live {
  background: #ef4444;
  color: #fff;
}

.badge--scheduled {
  background: rgba(15, 23, 42, 0.8);
  color: #fff;
}

.badge--cancelled {
  background: var(--surface-weak);
  color: var(--text-muted);
  border: 1px solid var(--border-color);
}

.badge--vod {
  background: rgba(15, 23, 42, 0.8);
  color: #fff;
}

.live-body {
  padding: 14px;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
  flex: 1;
}

.live-meta {
  min-width: 0;
}

.live-title {
  margin: 0 0 8px;
  font-weight: 900;
  color: var(--text-strong);
  font-size: 1.02rem;
}

.live-date {
  margin: 0 0 6px;
  color: var(--text-soft);
  font-weight: 700;
  font-size: 0.95rem;
}

.live-seller {
  margin: 0;
  color: var(--text-muted);
  font-weight: 800;
  font-size: 0.9rem;
}

.live-viewers {
  margin: 6px 0 0;
  color: var(--text-muted);
  font-weight: 700;
  font-size: 0.85rem;
}

.live-card--empty {
  justify-content: center;
  padding: 18px;
}

.live-card__title {
  margin: 0;
  font-weight: 900;
  color: var(--text-strong);
}

.live-card__meta {
  margin: 8px 0 0;
  color: var(--text-muted);
  font-weight: 700;
}

.empty-section {
  margin: 12px auto;
  text-align: center;
  color: var(--text-muted);
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 160px;
}

.link-more {
  border: none;
  background: transparent;
  color: var(--primary-color);
  font-weight: 900;
  cursor: pointer;
  padding: 4px 6px;
}

@media (max-width: 1200px) {
  .live-grid,
  .scheduled-grid,
  .vod-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .live-grid,
  .scheduled-grid,
  .vod-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .live-header {
    grid-template-columns: 1fr;
    justify-items: start;
  }

  .live-grid,
  .scheduled-grid,
  .vod-grid {
    grid-template-columns: 1fr;
  }

  .live-section__controls {
    align-items: flex-start;
  }

  .more-row {
    justify-content: flex-start;
  }
}
</style>
