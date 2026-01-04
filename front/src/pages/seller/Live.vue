<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PageHeader from '../../components/PageHeader.vue'
import DeviceSetupModal from '../../components/DeviceSetupModal.vue'
import { getScheduledBroadcasts } from '../../composables/useSellerBroadcasts'
import { getSellerReservationDetail, sellerReservationSummaries } from '../../lib/mocks/sellerReservations'
import { getSellerVodDetail, sellerVodSummaries } from '../../lib/mocks/sellerVods'

const router = useRouter()
const route = useRoute()

type LiveTab = 'all' | 'scheduled' | 'live' | 'vod'
type CarouselKind = 'live' | 'scheduled' | 'vod'

type LiveItem = {
  id: string
  title: string
  subtitle: string
  thumb: string
  datetime: string
  statusBadge?: string
  viewerBadge?: string
  ctaLabel: string
  likes?: number
  viewers?: number
  visibility?: string | boolean
  createdAt?: string
  category?: string
  status?: string
  startAtMs?: number
  revenue?: number
}

const activeTab = ref<LiveTab>('all')
const liveSort = ref<'viewers_desc' | 'likes_desc' | 'latest'>('viewers_desc')

const scheduledStatus = ref<'all' | 'reserved' | 'canceled'>('all')
const scheduledCategory = ref<string>('all')
const scheduledSort = ref<'nearest' | 'latest' | 'oldest'>('nearest')
const scheduledVisibleCount = ref(8)

const vodStartDate = ref('')
const vodEndDate = ref('')
const vodVisibility = ref<'all' | 'public' | 'private'>('all')
const vodSort = ref<'latest' | 'oldest' | 'likes_desc' | 'likes_asc' | 'viewers_desc' | 'viewers_asc' | 'revenue_desc' | 'revenue_asc'>(
  'latest',
)
const vodCategory = ref<string>('all')
const vodVisibleCount = ref(8)

const showDeviceModal = ref(false)
const selectedScheduled = ref<LiveItem | null>(null)

const liveItems = ref<LiveItem[]>([])
const liveProducts = ref(
  [
    {
      id: 'p-1',
      title: '모던 스탠딩 데스크',
      optionLabel: '1200mm · 오프화이트',
      status: '판매중',
      priceOriginal: 289000,
      priceSale: 229000,
      soldCount: 48,
      stockTotal: 120,
      pinned: true,
      thumb: '',
    },
    {
      id: 'p-2',
      title: '로우 프로파일 키보드',
      optionLabel: '무선 · 베이지',
      status: '판매중',
      priceOriginal: 139000,
      priceSale: 99000,
      soldCount: 72,
      stockTotal: 180,
      pinned: false,
      thumb: '',
    },
    {
      id: 'p-3',
      title: '미니멀 데스크 매트',
      optionLabel: '900mm · 샌드',
      status: '품절',
      priceOriginal: 59000,
      priceSale: 45000,
      soldCount: 110,
      stockTotal: 110,
      pinned: false,
      thumb: '',
    },
    {
      id: 'p-4',
      title: '알루미늄 모니터암',
      optionLabel: '싱글 · 블랙',
      status: '판매중',
      priceOriginal: 169000,
      priceSale: 129000,
      soldCount: 39,
      stockTotal: 95,
      pinned: true,
      thumb: '',
    },
  ],
)

const liveStats = ref({
  status: '방송 중',
  viewers: '1,248명',
  likes: '3,420',
  revenue: '₩4,920,000',
})

const scheduledItems = ref<LiveItem[]>([])
const vodItems = ref<LiveItem[]>([])

const liveTicker = ref<number | null>(null)

const gradientPalette = ['111827', '0f172a', '1f2937', '334155'] as const

const gradientThumb = (from: string, to: string) =>
  `data:image/svg+xml;utf8,` +
  `<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 320 200'>` +
  `<defs><linearGradient id='g' x1='0' x2='1' y1='0' y2='1'>` +
  `<stop offset='0' stop-color='%23${from}'/>` +
  `<stop offset='1' stop-color='%23${to}'/>` +
  `</linearGradient></defs>` +
  `<rect width='320' height='200' fill='url(%23g)'/>` +
  `</svg>`

const liveCategories = ['홈오피스', '주변기기', '조명', '정리/수납']

const buildLiveItems = () => {
  liveItems.value = [
    {
      id: 'live-1',
      title: '진행 중인 방송 제목',
      subtitle: '셋업 추천 라이브',
      thumb: gradientThumb('0f172a', '1f2937'),
      datetime: '오늘 14:00 - 15:00',
      statusBadge: 'LIVE',
      viewerBadge: '500명 시청 중',
      ctaLabel: '방송 입장',
      viewers: 500,
      likes: 320,
      category: liveCategories[0],
    },
    {
      id: 'live-2',
      title: '게이밍 데스크 셋업',
      subtitle: '조명 & 모니터암',
      thumb: gradientThumb('111827', '0f172a'),
      datetime: '오늘 16:30 - 17:10',
      statusBadge: 'LIVE',
      viewerBadge: '214명 시청 중',
      ctaLabel: '방송 입장',
      viewers: 214,
      likes: 190,
      category: liveCategories[1],
    },
    {
      id: 'live-3',
      title: '미니멀 오피스 데스크',
      subtitle: '수납/정리 팁',
      thumb: gradientThumb('1f2937', '111827'),
      datetime: '오늘 19:00 - 20:00',
      statusBadge: 'LIVE',
      viewerBadge: '89명 시청 중',
      ctaLabel: '방송 입장',
      viewers: 89,
      likes: 120,
      category: liveCategories[2],
    },
    {
      id: 'live-4',
      title: '홈카페 코너 셋업',
      subtitle: '바 스툴 & 선반',
      thumb: gradientThumb('0b1324', '0f172a'),
      datetime: '오늘 20:30 - 21:20',
      statusBadge: 'LIVE',
      viewerBadge: '132명 시청 중',
      ctaLabel: '방송 입장',
      viewers: 132,
      likes: 140,
      category: liveCategories[3],
    },
    {
      id: 'live-5',
      title: '작업실 조명 추천',
      subtitle: '데스크 램프 비교',
      thumb: gradientThumb('0f172a', '111827'),
      datetime: '오늘 21:40 - 22:10',
      statusBadge: 'LIVE',
      viewerBadge: '76명 시청 중',
      ctaLabel: '방송 입장',
      viewers: 76,
      likes: 90,
      category: liveCategories[1],
    },
    {
      id: 'live-6',
      title: '듀얼 모니터 세팅',
      subtitle: '케이블 정리 포함',
      thumb: gradientThumb('111827', '1f2937'),
      datetime: '오늘 22:20 - 22:50',
      statusBadge: 'LIVE',
      viewerBadge: '54명 시청 중',
      ctaLabel: '방송 입장',
      viewers: 54,
      likes: 60,
      category: liveCategories[0],
    },
  ]

  liveProducts.value = liveProducts.value.map((item, index) => ({
    ...item,
    thumb: gradientThumb(gradientPalette[index % gradientPalette.length], '0f172a'),
  }))
}

const liveItemsSorted = computed(() => {
  const sorted = [...liveItems.value]
  if (liveSort.value === 'viewers_desc') {
    sorted.sort((a, b) => (b.viewers ?? 0) - (a.viewers ?? 0))
  } else if (liveSort.value === 'likes_desc') {
    sorted.sort((a, b) => (b.likes ?? 0) - (a.likes ?? 0))
  } else if (liveSort.value === 'latest') {
    sorted.sort((a, b) => toDateMs(b) - toDateMs(a))
  }
  return sorted
})

const currentLive = computed(() => liveItemsSorted.value[0] ?? null)

const vodCategories = ['홈오피스', '주변기기', '정리/수납', '조명']

const loadScheduled = () => {
  const fromStorage = getScheduledBroadcasts().map((item, index) => {
    const detail = getSellerReservationDetail(item.id)
    const startAtMs = Date.parse(item.datetime.replace(/\./g, '-').replace(' ', 'T'))
    return {
      ...item,
      category: detail?.category ?? liveCategories[index % liveCategories.length],
      status: detail?.status ?? '예약됨',
      startAtMs: Number.isNaN(startAtMs) ? undefined : startAtMs,
    }
  })

  const seeded = sellerReservationSummaries.map((item, index) => {
    const detail = getSellerReservationDetail(item.id)
    const startAtMs = Date.parse(item.datetime.replace(/\./g, '-').replace(' ', 'T'))
    return {
      ...item,
      category: detail?.category ?? liveCategories[index % liveCategories.length],
      status: detail?.status ?? '예약됨',
      startAtMs: Number.isNaN(startAtMs) ? undefined : startAtMs,
    }
  })

  scheduledItems.value = [...fromStorage, ...seeded]
}

const loadVods = () => {
  vodItems.value = sellerVodSummaries.map((item, index) => {
    const detail = getSellerVodDetail(item.id)
    const visibility = detail?.vod?.visibility === '비공개' ? 'private' : 'public'
    const startMs = Date.parse(item.startedAt.replace(/\./g, '-').replace(' ', 'T'))
    const endMs = Date.parse(item.endedAt.replace(/\./g, '-').replace(' ', 'T'))
    return {
      id: item.id,
      title: item.title,
      subtitle: '',
      thumb: item.thumb,
      datetime: `업로드: ${item.startedAt}`,
      ctaLabel: '상세보기',
      visibility,
      createdAt: item.startedAt,
      likes: detail?.metrics?.likes ?? 0,
      viewers: detail?.metrics?.maxViewers ?? 0,
      revenue: detail?.metrics?.totalRevenue ?? 0,
      category: vodCategories[index % vodCategories.length],
      startAtMs: Number.isNaN(startMs) ? undefined : startMs,
      statusBadge: detail?.vod?.visibility === '비공개' ? '비공개' : 'VOD',
      viewerBadge: detail?.metrics?.maxViewers ? `${detail.metrics.maxViewers}명 시청` : undefined,
      status: 'VOD',
      endAtMs: Number.isNaN(endMs) ? undefined : endMs,
    }
  })
}

const filteredVodItems = computed(() => {
  const startMs = vodStartDate.value ? Date.parse(`${vodStartDate.value}T00:00:00`) : null
  const endMs = vodEndDate.value ? Date.parse(`${vodEndDate.value}T23:59:59`) : null

  let filtered = vodItems.value.filter((item) => {
    const dateMs = item.startAtMs ?? toDateMs(item)
    if (startMs && dateMs < startMs) return false
    if (endMs && dateMs > endMs) return false
    const visibility = getVisibility(item)
    if (vodVisibility.value !== 'all' && vodVisibility.value !== visibility) return false
    if (vodCategory.value !== 'all' && item.category !== vodCategory.value) return false
    return true
  })

  filtered = filtered.slice().sort((a, b) => {
    if (vodSort.value === 'latest') return toDateMs(b) - toDateMs(a)
    if (vodSort.value === 'oldest') return toDateMs(a) - toDateMs(b)
    if (vodSort.value === 'likes_desc') return getLikes(b) - getLikes(a)
    if (vodSort.value === 'likes_asc') return getLikes(a) - getLikes(b)
    if (vodSort.value === 'viewers_desc') return getViewers(b) - getViewers(a)
    if (vodSort.value === 'viewers_asc') return getViewers(a) - getViewers(b)
    if (vodSort.value === 'revenue_desc') return (b.revenue ?? 0) - (a.revenue ?? 0)
    if (vodSort.value === 'revenue_asc') return (a.revenue ?? 0) - (b.revenue ?? 0)
    return 0
  })

  return filtered
})

const filteredScheduledItems = computed(() => {
  let filtered = scheduledItems.value

  if (scheduledStatus.value === 'reserved') {
    filtered = filtered.filter((item) => item.status !== '취소됨')
  } else if (scheduledStatus.value === 'canceled') {
    filtered = filtered.filter((item) => item.status === '취소됨')
  }

  if (scheduledCategory.value !== 'all') {
    filtered = filtered.filter((item) => item.category === scheduledCategory.value)
  }

  filtered = filtered.slice().sort((a, b) => {
    const aDate = a.startAtMs ?? toDateMs(a)
    const bDate = b.startAtMs ?? toDateMs(b)
    if (scheduledSort.value === 'latest') return bDate - aDate
    if (scheduledSort.value === 'oldest') return aDate - bDate
    return aDate - bDate
  })

  return filtered
})

const scheduledCategories = computed(() =>
  Array.from(new Set(filteredScheduledItems.value.map((item) => item.category ?? '기타'))),
)

const scheduledLoop = computed(() => (filteredScheduledItems.value.length ? [...filteredScheduledItems.value, ...filteredScheduledItems.value] : []))

const visibleScheduledItems = computed(() => filteredScheduledItems.value.slice(0, scheduledVisibleCount.value))
const visibleVodItems = computed(() => filteredVodItems.value.slice(0, vodVisibleCount.value))

const getLikes = (item: LiveItem) => (typeof item.likes === 'number' ? item.likes : 0)
const getViewers = (item: LiveItem) => (typeof item.viewers === 'number' ? item.viewers : 0)
const getVisibility = (item: LiveItem): 'public' | 'private' => {
  if (typeof item.visibility === 'boolean') return item.visibility ? 'public' : 'private'
  if (typeof item.visibility === 'string') {
    if (item.visibility === 'public' || item.visibility === '공개') return 'public'
    if (item.visibility === 'private' || item.visibility === '비공개') return 'private'
  }
  if ((item as any)?.isPublic === true) return 'public'
  return 'public'
}

const toDateMs = (item: LiveItem) => {
  const raw = item.createdAt || item.datetime || ''
  const parsed = Date.parse(raw.replace(/\./g, '-').replace(' ', 'T'))
  return Number.isNaN(parsed) ? 0 : parsed
}

const visibleLive = computed(() => activeTab.value === 'all' || activeTab.value === 'live')
const visibleScheduled = computed(() => activeTab.value === 'all' || activeTab.value === 'scheduled')
const visibleVod = computed(() => activeTab.value === 'all' || activeTab.value === 'vod')

const carouselRefs = ref<Record<CarouselKind, HTMLElement | null>>({
  live: null,
  scheduled: null,
  vod: null,
})

const setCarouselRef = (kind: CarouselKind) => (el: Element | null) => {
  carouselRefs.value[kind] = (el as HTMLElement) || null
}

const scrollCarousel = (kind: CarouselKind, dir: -1 | 1) => {
  const el = carouselRefs.value[kind]
  if (!el) return
  const first = el.querySelector<HTMLElement>('.live-card')
  const gap = 14
  const cardW = first?.offsetWidth ?? 320
  el.scrollBy({ left: dir * (cardW + gap) * 2, behavior: 'smooth' })
}

const setTab = (tab: LiveTab) => {
  activeTab.value = tab
}

const handleCreate = () => {
  router.push('/seller/live/create').catch(() => {})
}

const syncTabFromRoute = () => {
  const tab = route.query.tab
  if (tab === 'scheduled' || tab === 'live' || tab === 'vod' || tab === 'all') {
    activeTab.value = tab
  }
}

watch(
  () => route.query.tab,
  () => {
    syncTabFromRoute()
  },
)

const handleNavChange = (value: 'list' | 'stats') => {
  if (value === 'stats') {
    router.push('/seller/live/stats').catch(() => {})
    return
  }
  router.push('/seller/live').catch(() => {})
}

const handleCta = (kind: CarouselKind, item: LiveItem) => {
  if (kind === 'live') {
    router.push(`/seller/live/stream/${item.id}`).catch(() => {})
    return
  }
  if (kind === 'scheduled') {
    if (canStartNow(item)) {
      selectedScheduled.value = item
      showDeviceModal.value = true
      return
    }
    router.push(`/seller/broadcasts/reservations/${item.id}`).catch(() => {})
    return
  }
  router.push(`/seller/broadcasts/vods/${item.id}`).catch(() => {})
}

const handleDeviceStart = () => {
  const target = selectedScheduled.value
  if (!target) return
  router.push(`/seller/live/stream/${target.id}`).catch(() => {})
}

const canStartNow = (item: LiveItem) => {
  if (item.ctaLabel?.includes('시작')) return true
  if (!item.startAtMs) return false
  const now = Date.now()
  const diff = item.startAtMs - now
  return diff <= 1000 * 60 * 120 && diff >= -1000 * 60 * 30
}

const openReservationDetail = (item: LiveItem) => {
  router.push(`/seller/broadcasts/reservations/${item.id}`).catch(() => {})
}

const openVodDetail = (item: LiveItem) => {
  router.push(`/seller/broadcasts/vods/${item.id}`).catch(() => {})
}

const startLiveTicker = () => {
  liveTicker.value = window.setInterval(() => {
    liveStats.value = {
      status: liveStats.value.status,
      viewers: `${(parseInt(liveStats.value.viewers.replace(/[^0-9]/g, ''), 10) || 1200) + Math.floor(Math.random() * 30)}명`,
      likes: `${(parseInt(liveStats.value.likes.replace(/[^0-9]/g, ''), 10) || 3000) + Math.floor(Math.random() * 10)}`,
      revenue: `₩${(parseInt(liveStats.value.revenue.replace(/[^0-9]/g, ''), 10) || 4920000 + Math.floor(Math.random() * 50000)).toLocaleString()}`,
    }

    liveProducts.value = liveProducts.value.map((product) => {
      if (product.status === '품절') return product
      const delta = Math.random() < 0.2 ? 1 : 0
      const soldCount = product.soldCount + delta
      const stockTotal = Math.max(product.stockTotal - delta, 0)
      return {
        ...product,
        soldCount,
        stockTotal,
        status: stockTotal === 0 ? '품절' : product.status,
      }
    })
  }, 3200)
}

onMounted(() => {
  buildLiveItems()
  loadScheduled()
  loadVods()
  syncTabFromRoute()
  startLiveTicker()
})

onBeforeUnmount(() => {
  if (liveTicker.value) {
    window.clearInterval(liveTicker.value)
  }
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

      <div class="live-header__right">
        <label v-if="activeTab === 'live'" class="inline-filter">
          <span>정렬</span>
          <select v-model="liveSort">
            <option value="viewers_desc">시청자 많은 순</option>
            <option value="likes_desc">좋아요 많은 순</option>
            <option value="latest">최신 순</option>
          </select>
        </label>
        <button type="button" class="live-create-btn" @click="handleCreate">방송 등록</button>
      </div>
    </header>

    <section v-if="visibleLive" class="live-section">
      <div class="live-section__head">
        <div class="live-section__title">
          <h3>방송 중</h3>
          <button class="link-more" type="button" @click="setTab('live')">+ 더보기</button>
        </div>
        <p class="ds-section-sub">현재 진행 중인 라이브 방송입니다.</p>
      </div>

      <div v-if="activeTab === 'live'" class="live-livegrid">
        <article v-if="currentLive" class="live-feature ds-surface">
          <div class="live-feature__top">
            <div class="live-feature__content">
              <div class="live-feature__title-row">
                <h4>{{ currentLive.title }}</h4>
              </div>
              <p class="live-feature__seller">{{ currentLive.subtitle }}</p>
              <div class="live-feature__thumb">
                <img :src="currentLive.thumb" :alt="currentLive.title" loading="lazy" />
                <span v-if="currentLive.statusBadge" class="badge badge--live live-feature__badge">
                  {{ currentLive.statusBadge }}
                </span>
              </div>
              <div class="live-feature__meta">
                <span v-if="currentLive.viewerBadge" class="meta-pill">{{ currentLive.viewerBadge }}</span>
                <span class="meta-pill">경과 시간 · 00:32:18</span>
                <span class="meta-pill">시작 · 13:24</span>
              </div>
            </div>
          </div>
          <button type="button" class="live-feature__cta" @click="handleCta('live', currentLive!)">방송 입장</button>
        </article>
        <article v-else class="live-feature ds-surface live-feature--empty">
          <p class="live-card__title">현재 진행 중인 방송이 없습니다.</p>
          <p class="live-card__meta">라이브를 시작하면 여기에서 실시간 상태를 볼 수 있어요.</p>
        </article>

        <article class="live-products ds-surface">
          <div class="live-products__head">
            <div>
              <h4>판매 상품</h4>
              <p class="ds-section-sub">라이브에 등록된 상품이에요.</p>
            </div>
            <span class="live-products__count">{{ liveProducts.length }}개</span>
          </div>
          <div class="live-products__list">
            <div v-for="item in liveProducts" :key="item.id" class="product-row">
              <div class="product-thumb">
                <img :src="item.thumb" :alt="item.title" loading="lazy" />
                <span v-if="item.pinned" class="product-pin">PIN</span>
              </div>
              <div class="product-meta">
                <p class="product-title">{{ item.title }}</p>
                <p class="product-option">{{ item.optionLabel }}</p>
                <div class="product-badges">
                  <span class="product-status" :class="{ 'is-soldout': item.status === '품절' }">{{ item.status }}</span>
                </div>
              </div>
              <div class="product-right">
                <p class="product-price">
                  <span class="product-sale">{{ item.priceSale.toLocaleString('ko-KR') }}원</span>
                  <span class="product-origin">{{ item.priceOriginal.toLocaleString('ko-KR') }}원</span>
                </p>
                <p class="product-stock">판매 {{ item.soldCount }} · 재고 {{ item.stockTotal }}</p>
              </div>
            </div>
          </div>
        </article>
      </div>

      <div v-else class="live-feature-wrap">
        <article v-if="currentLive" class="live-feature ds-surface">
          <div class="live-feature__top">
            <div class="live-feature__content">
              <div class="live-feature__title-row">
                <h4>{{ currentLive.title }}</h4>
              </div>
              <p class="live-feature__seller">{{ currentLive.subtitle }}</p>
              <div class="live-feature__thumb">
                <img :src="currentLive.thumb" :alt="currentLive.title" loading="lazy" />
                <span v-if="currentLive.statusBadge" class="badge badge--live live-feature__badge">
                  {{ currentLive.statusBadge }}
                </span>
              </div>
              <div class="live-feature__meta">
                <span v-if="currentLive.viewerBadge" class="meta-pill">{{ currentLive.viewerBadge }}</span>
                <span class="meta-pill">경과 시간 · 00:32:18</span>
                <span class="meta-pill">시작 · 13:24</span>
              </div>
            </div>
          </div>
          <button type="button" class="live-feature__cta" @click="handleCta('live', currentLive!)">방송 입장</button>
        </article>
        <article v-else class="live-feature ds-surface live-feature--empty">
          <p class="live-card__title">등록된 방송이 없습니다.</p>
          <p class="live-card__meta">새 방송을 등록해보세요.</p>
        </article>
      </div>

      <section v-if="activeTab === 'live'" class="live-stats">
        <div class="live-stats__head">
          <h4>실시간 통계</h4>
          <span class="live-stats__badge">
            <span class="live-stats__dot"></span>
            실시간 업데이트 중
          </span>
        </div>
        <div class="live-stats-grid">
          <article class="live-stat-card ds-surface">
            <p class="stat-label">방송 상태</p>
            <p class="stat-value">{{ liveStats.status }}</p>
            <p class="stat-sub">정상 송출 중</p>
          </article>
          <article class="live-stat-card ds-surface">
            <p class="stat-label">시청자 수</p>
            <p class="stat-value">{{ liveStats.viewers }}</p>
            <p class="stat-sub">누적 기준</p>
          </article>
          <article class="live-stat-card ds-surface">
            <p class="stat-label">좋아요 수</p>
            <p class="stat-value">{{ liveStats.likes }}</p>
            <p class="stat-sub">최근 5분</p>
          </article>
          <article class="live-stat-card ds-surface">
            <p class="stat-label">현재 매출</p>
            <p class="stat-value">{{ liveStats.revenue }}</p>
            <p class="stat-sub">실시간 집계</p>
          </article>
        </div>
      </section>
    </section>

    <section v-if="visibleScheduled" class="live-section">
      <div class="live-section__head">
        <div class="live-section__title">
          <h3>예약된 방송</h3>
          <button class="link-more" type="button" @click="setTab('scheduled')">+ 더보기</button>
        </div>
        <p class="ds-section-sub">예정된 라이브 스케줄을 관리하세요.</p>
      </div>

      <div v-if="activeTab === 'scheduled'" class="filter-bar">
        <label class="filter-field">
          <span class="filter-label">상태</span>
          <select v-model="scheduledStatus">
            <option value="all">전체</option>
            <option value="reserved">예약 중</option>
            <option value="canceled">취소됨</option>
          </select>
        </label>
        <label class="filter-field">
          <span class="filter-label">카테고리</span>
          <select v-model="scheduledCategory">
            <option value="all">전체</option>
            <option v-for="category in scheduledCategories" :key="category" :value="category">
              {{ category }}
            </option>
          </select>
        </label>
        <label class="filter-field">
          <span class="filter-label">정렬</span>
          <select v-model="scheduledSort">
            <option value="nearest">방송 시간이 가까운 순</option>
            <option value="latest">최신 순</option>
            <option value="oldest">오래된 순</option>
          </select>
        </label>
      </div>

      <div v-if="activeTab === 'scheduled'" class="scheduled-grid" aria-label="예약 방송 목록">
        <template v-if="visibleScheduledItems.length">
          <article
            v-for="item in visibleScheduledItems"
            :key="item.id"
            class="live-card ds-surface live-card--clickable"
            @click="openReservationDetail(item)"
          >
            <div class="live-thumb">
              <img class="live-thumb__img" :src="item.thumb" :alt="item.title" loading="lazy" />
              <div class="live-badges">
                <span class="badge badge--scheduled" :class="{ 'badge--cancelled': item.status === '취소됨' }">{{ item.status ?? '예약' }}</span>
              </div>
            </div>
            <div class="live-body">
              <div class="live-meta">
                <p class="live-title">{{ item.title }}</p>
                <p class="live-date">{{ item.datetime }}</p>
                <p class="live-seller">{{ item.category }}</p>
              </div>
              <button
                v-if="canStartNow(item)"
                type="button"
                class="live-cta live-cta--ghost"
                @click.stop="handleCta('scheduled', item)"
              >
                {{ item.ctaLabel }}
              </button>
            </div>
          </article>

          <button
            v-if="filteredScheduledItems.length > visibleScheduledItems.length"
            type="button"
            class="live-more"
            @click="scheduledVisibleCount += 6"
          >
            더 보기
          </button>
        </template>

        <article v-else class="live-card ds-surface live-card--empty">
          <p class="live-card__title">등록된 방송이 없습니다.</p>
          <p class="live-card__meta">예약 방송을 추가해보세요.</p>
        </article>
      </div>

      <div v-else class="carousel-wrap">
        <div class="live-carousel live-carousel--loop" :ref="setCarouselRef('scheduled')" aria-label="예약 방송 목록">
          <template v-if="scheduledLoop.length">
            <article
              v-for="(item, idx) in scheduledLoop"
              :key="`${item.id}-${idx}`"
              class="live-card ds-surface live-card--clickable"
              @click="openReservationDetail(item)"
            >
              <div class="live-thumb">
                <img class="live-thumb__img" :src="item.thumb" :alt="item.title" loading="lazy" />
                <div class="live-badges">
                  <span class="badge badge--scheduled" :class="{ 'badge--cancelled': item.status === '취소됨' }">{{ item.status ?? '예약' }}</span>
                </div>
              </div>
              <div class="live-body">
                <div class="live-meta">
                  <p class="live-title">{{ item.title }}</p>
                  <p class="live-date">{{ item.datetime }}</p>
                  <p class="live-seller">{{ item.category }}</p>
                </div>
                <button
                  v-if="canStartNow(item)"
                  type="button"
                  class="live-cta live-cta--ghost"
                  @click.stop="handleCta('scheduled', item)"
                >
                  {{ item.ctaLabel }}
                </button>
              </div>
            </article>
          </template>

          <article v-else class="live-card ds-surface live-card--empty">
            <p class="live-card__title">등록된 방송이 없습니다.</p>
            <p class="live-card__meta">예약 방송을 추가해보세요.</p>
          </article>
        </div>
      </div>
    </section>

    <section v-if="visibleVod" class="live-section">
      <div class="live-section__head">
        <div class="live-section__title">
          <h3>VOD</h3>
          <button class="link-more" type="button" @click="setTab('vod')">+ 더보기</button>
        </div>
        <p class="ds-section-sub">저장된 다시보기 콘텐츠를 확인합니다.</p>
      </div>

      <div v-if="activeTab === 'vod'" class="vod-filters">
        <label class="filter-field">
          <span class="filter-label">시작일</span>
          <input v-model="vodStartDate" type="date" />
        </label>
        <label class="filter-field">
          <span class="filter-label">종료일</span>
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
            <option value="all">전체</option>
            <option v-for="category in vodCategories" :key="category" :value="category">{{ category }}</option>
          </select>
        </label>
        <label class="filter-field">
          <span class="filter-label">정렬</span>
          <select v-model="vodSort">
            <option value="latest">최신 순</option>
            <option value="oldest">오래된 순</option>
            <option value="likes_desc">좋아요 높은 순</option>
            <option value="likes_asc">좋아요 낮은 순</option>
            <option value="viewers_desc">시청자 수 높은 순</option>
            <option value="viewers_asc">시청자 수 낮은 순</option>
            <option value="revenue_desc">매출 높은 순</option>
            <option value="revenue_asc">매출 낮은 순</option>
          </select>
        </label>
      </div>

      <div v-if="activeTab === 'vod'" class="vod-grid" aria-label="VOD 목록">
        <template v-if="visibleVodItems.length">
            <article
              v-for="item in visibleVodItems"
              :key="item.id"
              class="live-card ds-surface live-card--clickable"
              @click="openVodDetail(item)"
            >
            <div class="live-thumb">
              <img class="live-thumb__img" :src="item.thumb" :alt="item.title" loading="lazy" />
              <div class="live-badges">
                <span class="badge badge--vod">{{ item.statusBadge ?? 'VOD' }}</span>
              </div>
            </div>
            <div class="live-body">
              <div class="live-meta">
                <p class="live-title">{{ item.title }}</p>
                <p class="live-date">{{ item.datetime }}</p>
                <p class="live-seller">{{ item.category }}</p>
              </div>
            </div>
          </article>

          <button
            v-if="filteredVodItems.length > visibleVodItems.length"
            type="button"
            class="live-more"
            @click="vodVisibleCount += 6"
          >
            더 보기
          </button>
        </template>

        <article v-else class="live-card ds-surface live-card--empty">
          <p class="live-card__title">등록된 VOD가 없습니다.</p>
          <p class="live-card__meta">방송이 종료되면 자동 등록됩니다.</p>
        </article>
      </div>

      <div v-else class="carousel-wrap">
        <div class="live-carousel live-carousel--loop" :ref="setCarouselRef('vod')" aria-label="VOD 목록">
          <template v-if="filteredVodItems.length">
            <article
              v-for="(item, idx) in filteredVodItems"
              :key="`${item.id}-${idx}`"
              class="live-card ds-surface live-card--clickable"
              @click="openVodDetail(item)"
            >
              <div class="live-thumb">
                <img class="live-thumb__img" :src="item.thumb" :alt="item.title" loading="lazy" />
                <div class="live-badges">
                  <span class="badge badge--vod">{{ item.statusBadge ?? 'VOD' }}</span>
                </div>
              </div>
              <div class="live-body">
                <div class="live-meta">
                  <p class="live-title">{{ item.title }}</p>
                  <p class="live-date">{{ item.datetime }}</p>
                  <p class="live-seller">{{ item.category }}</p>
                </div>
              </div>
            </article>
          </template>

          <article v-else class="live-card ds-surface live-card--empty">
            <p class="live-card__title">등록된 VOD가 없습니다.</p>
            <p class="live-card__meta">방송이 종료되면 자동 등록됩니다.</p>
          </article>
        </div>
      </div>
    </section>

    <DeviceSetupModal
      v-model="showDeviceModal"
      :broadcast-title="selectedScheduled?.title"
      @start="handleDeviceStart"
    />
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
  gap: 10px;
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

.live-create-btn {
  border: 1px solid var(--border-color);
  background: #fff;
  color: var(--text-strong);
  border-radius: 10px;
  padding: 10px 14px;
  font-weight: 900;
  cursor: pointer;
  transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}

.live-create-btn:hover {
  border-color: var(--primary-color);
  box-shadow: 0 8px 18px rgba(var(--primary-rgb), 0.12);
  transform: translateY(-1px);
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
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.live-section__title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.live-section__head h3 {
  margin: 0;
  font-size: 1.3rem;
  font-weight: 900;
  color: var(--text-strong);
}

.link-more {
  border: none;
  background: transparent;
  color: var(--primary-color);
  font-weight: 900;
  cursor: pointer;
  padding: 4px 6px;
}

.vod-filters,
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

.filter-field input,
.filter-field select {
  border: 1px solid var(--border-color);
  border-radius: 10px;
  padding: 8px 10px;
  font-weight: 700;
  color: var(--text-strong);
  background: var(--surface);
}

.live-feature-wrap {
  display: block;
  width: 100%;
}

.live-feature {
  width: 100%;
  border-radius: 14px;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.live-feature__top {
  display: flex;
  gap: 12px;
}

.live-feature__content {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
}

.live-feature__title-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.live-feature__title-row h4 {
  margin: 0;
  font-size: 1.4rem;
  font-weight: 900;
  color: var(--text-strong);
}

.live-feature__seller {
  margin: 0;
  color: var(--text-muted);
  font-weight: 800;
}

.live-feature__thumb {
  position: relative;
  width: 100%;
  aspect-ratio: 16 / 9;
  border-radius: 12px;
  overflow: hidden;
  background: var(--surface-weak);
}

.live-feature__thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.live-feature__badge {
  position: absolute;
  top: 10px;
  left: 10px;
}

.live-feature__meta {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 8px;
}

.meta-pill {
  display: inline-flex;
  align-items: center;
  background: rgba(15, 23, 42, 0.06);
  border-radius: 999px;
  padding: 8px 10px;
  font-weight: 800;
  color: var(--text-muted);
}

.live-feature__cta {
  align-self: flex-start;
  border-radius: 10px;
  padding: 10px 14px;
  background: var(--primary-color);
  color: #fff;
  font-weight: 900;
  border: none;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.live-feature__cta:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 22px rgba(var(--primary-rgb), 0.2);
}

.live-feature--empty {
  align-items: center;
  text-align: center;
}

.live-livegrid {
  display: grid;
  grid-template-columns: 2fr 1.2fr;
  gap: 12px;
}

.live-products {
  padding: 14px;
  border-radius: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.live-products__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.live-products__count {
  font-weight: 900;
  color: var(--text-muted);
}

.live-products__list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.product-row {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 10px;
  align-items: center;
}

.product-thumb {
  position: relative;
  width: 72px;
  height: 72px;
  border-radius: 12px;
  overflow: hidden;
  background: var(--surface-weak);
}

.product-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.product-pin {
  position: absolute;
  top: 6px;
  left: 6px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 8px;
  padding: 4px 6px;
  font-size: 0.75rem;
  font-weight: 900;
  color: var(--text-strong);
}

.product-meta {
  min-width: 0;
}

.product-title {
  margin: 0 0 4px;
  font-weight: 900;
  color: var(--text-strong);
}

.product-option {
  margin: 0 0 8px;
  color: var(--text-muted);
  font-weight: 700;
}

.product-badges {
  display: flex;
  gap: 8px;
}

.product-status {
  display: inline-flex;
  align-items: center;
  border-radius: 8px;
  padding: 6px 8px;
  background: rgba(15, 23, 42, 0.06);
  font-weight: 800;
  color: var(--text-strong);
}

.product-status.is-soldout {
  background: #fee2e2;
  color: #991b1b;
}

.product-right {
  text-align: right;
}

.product-price {
  margin: 0 0 6px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  align-items: flex-end;
}

.product-sale {
  font-weight: 900;
  color: var(--text-strong);
}

.product-origin {
  color: var(--text-muted);
  font-weight: 700;
  text-decoration: line-through;
}

.product-stock {
  margin: 0;
  color: var(--text-muted);
  font-weight: 700;
}

.live-stats {
  margin-top: 14px;
}

.live-stats__head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.live-stats__badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: rgba(34, 197, 94, 0.12);
  color: #15803d;
  border-radius: 999px;
  padding: 6px 10px;
  font-weight: 900;
}

.live-stats__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #22c55e;
  display: inline-block;
  animation: pulse 1.4s infinite;
}

@keyframes pulse {
  0% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.1);
    opacity: 0.65;
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}

.live-stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.live-stat-card {
  padding: 14px;
  border-radius: 12px;
}

.stat-label {
  margin: 0 0 8px;
  font-weight: 800;
  color: var(--text-muted);
}

.stat-value {
  margin: 0 0 4px;
  font-weight: 900;
  font-size: 1.4rem;
  color: var(--text-strong);
}

.stat-sub {
  margin: 0;
  color: var(--text-muted);
  font-weight: 700;
}

.carousel-wrap {
  position: relative;
}

.carousel-btn {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid var(--border-color);
  background: #fff;
  font-weight: 900;
  cursor: pointer;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.12);
}

.carousel-btn--left {
  left: -6px;
}

.carousel-btn--right {
  right: -6px;
}

.live-carousel {
  display: grid;
  grid-auto-flow: column;
  grid-auto-columns: minmax(280px, 320px);
  gap: 14px;
  overflow-x: auto;
  padding: 10px 10px;
  scroll-snap-type: x mandatory;
  -webkit-overflow-scrolling: touch;
}

.live-carousel::-webkit-scrollbar {
  height: 10px;
}

.live-carousel::-webkit-scrollbar-thumb {
  background: rgba(15, 23, 42, 0.12);
  border-radius: 999px;
}

.live-carousel--loop {
  grid-auto-columns: minmax(260px, 280px);
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

.live-cta {
  border-radius: 10px;
  padding: 10px 12px;
  border: 1px solid var(--border-color);
  background: #fff;
  font-weight: 900;
  cursor: pointer;
}

.live-cta--ghost {
  background: var(--surface);
}

.live-grid,
.scheduled-grid,
.vod-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.live-more {
  grid-column: 1 / -1;
  padding: 12px;
  border-radius: 12px;
  border: 1px dashed var(--border-color);
  background: var(--surface);
  font-weight: 900;
  cursor: pointer;
}

@media (max-width: 1200px) {
  .live-grid,
  .scheduled-grid,
  .vod-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .live-livegrid {
    grid-template-columns: 1fr;
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

  .live-carousel {
    padding: 10px 10px;
  }

  .live-grid,
  .scheduled-grid,
  .vod-grid {
    grid-template-columns: 1fr;
  }
}
</style>
