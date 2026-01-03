<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PageContainer from '../components/PageContainer.vue'
import PageHeader from '../components/PageHeader.vue'
import { fetchBroadcastDetail, type BroadcastResponse } from '../api/liveApi'
import { getProductsForLive, type LiveProductItem } from '../lib/live/detail'

const route = useRoute()
const router = useRouter()

const broadcast = ref<BroadcastResponse | null>(null)
const loading = ref(false)
const errorMessage = ref<string | null>(null)

const playerPanelRef = ref<HTMLElement | null>(null)
const chatPanelRef = ref<HTMLElement | null>(null)
const playerHeight = ref<number | null>(null)
let panelResizeObserver: ResizeObserver | null = null

const messages = ref(
  [
    {
      id: 'sys-1',
      user: 'system',
      text: 'VOD 채팅 기록을 보고 계십니다.',
      at: new Date(Date.now() - 1000 * 60 * 6),
      kind: 'system',
    },
    {
      id: 'msg-1',
      user: 'desklover',
      text: '이 라이브 제품 너무 좋았어요!',
      at: new Date(Date.now() - 1000 * 60 * 4),
      kind: 'user',
    },
    {
      id: 'msg-2',
      user: 'setup_master',
      text: '배송도 빨랐습니다 👍',
      at: new Date(Date.now() - 1000 * 60 * 2),
      kind: 'user',
    },
  ] as Array<{ id: string; user: string; text: string; at: Date; kind?: 'system' | 'user' }>,
)

const chatListRef = ref<HTMLDivElement | null>(null)

const vodId = computed(() => {
  const value = route.params.id
  return Array.isArray(value) ? value[0] : value
})

const loadDetail = async () => {
  if (!vodId.value) {
    errorMessage.value = '잘못된 방송 ID입니다.'
    return
  }

  broadcast.value = null
  loading.value = true
  errorMessage.value = null

  try {
    const response = await fetchBroadcastDetail(vodId.value)
    broadcast.value = response
  } catch (error) {
    console.error('Failed to fetch VOD detail', error)
    errorMessage.value = 'VOD 정보를 불러오지 못했습니다. 잠시 후 다시 시도해주세요.'
  } finally {
    loading.value = false
  }
}

watch(
  () => route.params.id,
  () => {
    loadDetail()
  },
  { immediate: true },
)

watch(
  () => broadcast.value?.status,
  (status) => {
    if (status === 'ON_AIR') {
      router.replace({ name: 'live-detail', params: { id: vodId.value } })
    }
  },
)

const statusLabel = computed(() => {
  const map = { ON_AIR: 'LIVE', RESERVED: '예정', ENDED: 'VOD', VOD: 'VOD' } as const
  return map[broadcast.value?.status ?? 'VOD']
})

const statusBadgeClass = computed(() => {
  const map = {
    ON_AIR: 'status-badge--live',
    RESERVED: 'status-badge--upcoming',
    ENDED: 'status-badge--vod',
    VOD: 'status-badge--vod',
  } as const
  return map[broadcast.value?.status ?? 'VOD']
})

const formatPrice = (price: number) => `${price.toLocaleString('ko-KR')}원`

const formatDateTimeRange = (start?: string, end?: string) => {
  if (!start) return ''
  const startDate = new Date(start)
  const endDate = end ? new Date(end) : undefined
  const dayNames = ['일', '월', '화', '수', '목', '금', '토']
  const year = startDate.getFullYear()
  const month = String(startDate.getMonth() + 1).padStart(2, '0')
  const day = String(startDate.getDate()).padStart(2, '0')
  const dayLabel = dayNames[startDate.getDay()]
  const startHours = String(startDate.getHours()).padStart(2, '0')
  const startMinutes = String(startDate.getMinutes()).padStart(2, '0')
  const endHours = endDate ? String(endDate.getHours()).padStart(2, '0') : ''
  const endMinutes = endDate ? String(endDate.getMinutes()).padStart(2, '0') : ''
  return endDate
    ? `${year}.${month}.${day} (${dayLabel}) ${startHours}:${startMinutes} ~ ${endHours}:${endMinutes}`
    : `${year}.${month}.${day} (${dayLabel}) ${startHours}:${startMinutes}`
}

const scheduleLabel = computed(() => {
  if (!broadcast.value) return ''
  const start = broadcast.value.startedAt ?? broadcast.value.startAt
  const end = broadcast.value.endedAt ?? broadcast.value.startAt
  return formatDateTimeRange(start, end)
})

const products = computed<LiveProductItem[]>(() => {
  if (!broadcast.value) {
    return []
  }
  return getProductsForLive(String(broadcast.value.id))
})

const isEmbedUrl = (url: string) => url.includes('youtube.com/embed') || url.includes('player.vimeo.com')

const handleProductClick = (productId: string) => {
  router.push({ name: 'product-detail', params: { id: productId } })
}

const formatChatTime = (value: Date) => {
  const hours = String(value.getHours()).padStart(2, '0')
  const minutes = String(value.getMinutes()).padStart(2, '0')
  return `${hours}:${minutes}`
}

const scrollChatToBottom = () => {
  nextTick(() => {
    if (chatListRef.value) {
      chatListRef.value.scrollTop = chatListRef.value.scrollHeight
    }
  })
}

const syncChatHeight = () => {
  if (!playerPanelRef.value) {
    return
  }
  playerHeight.value = playerPanelRef.value.getBoundingClientRect().height
}

onMounted(() => {
  scrollChatToBottom()
})

onMounted(() => {
  panelResizeObserver = new ResizeObserver(() => {
    syncChatHeight()
  })
  if (playerPanelRef.value) {
    panelResizeObserver.observe(playerPanelRef.value)
  }
  nextTick(() => {
    syncChatHeight()
  })
})

onBeforeUnmount(() => {
  if (panelResizeObserver && playerPanelRef.value) {
    panelResizeObserver.unobserve(playerPanelRef.value)
  }
  panelResizeObserver?.disconnect()
})
</script>

<template>
  <PageContainer>
    <PageHeader title="VOD 다시보기" eyebrow="DESKIT VOD" />

    <div v-if="loading" class="empty-state">
      <p>VOD 정보를 불러오는 중입니다...</p>
    </div>

    <div v-else-if="errorMessage" class="empty-state">
      <p>{{ errorMessage }}</p>
      <RouterLink to="/live" class="link-back">라이브 일정으로 돌아가기</RouterLink>
    </div>

    <div v-else-if="!broadcast" class="empty-state">
      <p>VOD를 찾을 수 없습니다.</p>
      <RouterLink to="/live" class="link-back">라이브 일정으로 돌아가기</RouterLink>
    </div>

    <section v-else class="live-detail-layout">
      <div class="live-detail-main">
        <section ref="playerPanelRef" class="panel panel--player">
          <div class="player-meta">
            <div class="status-row">
              <span class="status-badge" :class="statusBadgeClass">{{ statusLabel }}</span>
              <span class="status-schedule">{{ scheduleLabel }}</span>
            </div>
            <h3 class="player-title">{{ broadcast.title }}</h3>
            <p v-if="broadcast.sellerName" class="player-seller">{{ broadcast.sellerName }}</p>
            <p v-if="broadcast.description" class="player-desc">{{ broadcast.description }}</p>
          </div>

          <div class="player-frame">
            <span v-if="broadcast.status === 'RESERVED'" class="player-frame__label">아직 시작 전입니다</span>
            <iframe
              v-else-if="broadcast.vodUrl && isEmbedUrl(broadcast.vodUrl)"
              class="player-embed"
              :src="broadcast.vodUrl"
              title="VOD 플레이어"
              frameborder="0"
              allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
              allowfullscreen
            />
            <video v-else-if="broadcast.vodUrl" class="player-video" :src="broadcast.vodUrl" controls />
            <span v-else class="player-frame__label">VOD 준비 중</span>
          </div>

          <div class="player-toolbar">
            <div class="player-toolbar__group player-toolbar__group--left">
              <button type="button" class="toolbar-btn" aria-disabled="true" disabled>
                <svg class="toolbar-svg" viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M12.1 21.35l-1.1-1.02C5.14 15.24 2 12.39 2 8.99 2 6.42 4.02 4.5 6.58 4.5c1.54 0 3.04.74 3.92 1.91C11.38 5.24 12.88 4.5 14.42 4.5 16.98 4.5 19 6.42 19 8.99c0 3.4-3.14 6.25-8.9 11.34l-1.1 1.02z" fill="none" stroke="currentColor" stroke-width="1.8" />
                </svg>
                <span class="toolbar-label">좋아요</span>
              </button>
            </div>
            <div class="player-toolbar__group player-toolbar__group--right">
              <button type="button" class="toolbar-btn" aria-disabled="true" disabled>
                <svg class="toolbar-svg" viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M4 6h16M4 12h16M4 18h16" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
                  <circle cx="9" cy="6" r="2" fill="none" stroke="currentColor" stroke-width="1.8" />
                  <circle cx="14" cy="12" r="2" fill="none" stroke="currentColor" stroke-width="1.8" />
                  <circle cx="7" cy="18" r="2" fill="none" stroke="currentColor" stroke-width="1.8" />
                </svg>
                <span class="toolbar-label">설정</span>
              </button>
              <button type="button" class="toolbar-btn" aria-disabled="true" disabled>
                <svg class="toolbar-svg" viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M4 9V4h5M20 9V4h-5M4 15v5h5M20 15v5h-5" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
                </svg>
                <span class="toolbar-label">전체화면</span>
              </button>
            </div>
          </div>
        </section>

        <aside
          ref="chatPanelRef"
          class="panel panel--chat"
          :style="{ height: playerHeight ? `${playerHeight}px` : undefined }"
        >
          <div class="panel__header">
            <h3 class="panel__title">채팅 기록</h3>
          </div>
          <div ref="chatListRef" class="chat-list">
            <div
              v-for="message in messages"
              :key="message.id"
              class="chat-message"
              :class="{ 'chat-message--system': message.kind === 'system' }"
            >
              <span class="chat-message__user">{{ message.user }}</span>
              <p class="chat-message__text">{{ message.text }}</p>
              <span class="chat-message__time">{{ formatChatTime(message.at) }}</span>
            </div>
          </div>
          <div class="chat-input">
            <input type="text" value="" readonly placeholder="VOD에서는 채팅을 보실 수만 있어요" />
            <button type="button" disabled>전송</button>
          </div>
        </aside>
      </div>

      <section class="panel panel--products">
        <div class="panel__header">
          <h3 class="panel__title">라이브 상품</h3>
          <span class="panel__count">{{ products.length }}개</span>
        </div>
        <div v-if="!products.length" class="panel__empty">상품이 없습니다.</div>
        <div v-else class="product-list product-list--grid">
          <button
            v-for="product in products"
            :key="product.id"
            type="button"
            class="product-card"
            :class="{ 'product-card--sold-out': product.isSoldOut }"
            @click="handleProductClick(product.id)"
          >
            <img class="product-card__thumb" :src="product.imageUrl" :alt="product.name" />
            <div class="product-card__info">
              <p class="product-card__name">{{ product.name }}</p>
              <p class="product-card__price">{{ formatPrice(product.price) }}</p>
              <span v-if="product.isSoldOut" class="product-card__badge">품절</span>
            </div>
          </button>
        </div>
      </section>
    </section>
  </PageContainer>
</template>

<style scoped>
.live-detail-layout {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.live-detail-main {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.panel {
  border: 1px solid var(--border-color);
  background: var(--surface);
  border-radius: 16px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
}

.panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.panel__title {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 800;
  color: var(--text-strong);
}

.panel__count {
  font-weight: 700;
  color: var(--text-soft);
}

.panel__empty {
  color: var(--text-muted);
  padding: 10px 0;
}

.product-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.product-list--grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
}

.product-card {
  border: 1px solid var(--border-color);
  background: var(--surface);
  border-radius: 14px;
  padding: 10px;
  display: grid;
  grid-template-columns: 64px 1fr;
  gap: 12px;
  cursor: pointer;
  text-align: left;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.product-card:hover {
  border-color: var(--primary-color);
  box-shadow: 0 10px 22px rgba(119, 136, 115, 0.12);
  transform: translateY(-1px);
}

.product-card--sold-out {
  opacity: 0.7;
}

.product-card__thumb {
  width: 64px;
  height: 64px;
  border-radius: 10px;
  object-fit: cover;
}

.product-card__info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.product-card__name {
  margin: 0;
  font-weight: 700;
  color: var(--text-strong);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.product-card__price {
  margin: 0;
  color: var(--text-muted);
  font-size: 0.95rem;
}

.product-card__badge {
  align-self: flex-start;
  padding: 2px 8px;
  border-radius: 999px;
  background: var(--surface-weak);
  color: var(--text-muted);
  font-size: 0.75rem;
  font-weight: 700;
}

.panel--player {
  gap: 16px;
}

.panel--chat {
  gap: 12px;
  min-height: 0;
}

.player-meta {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.status-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.status-badge {
  padding: 4px 10px;
  border-radius: 999px;
  font-weight: 800;
  font-size: 0.85rem;
  background: var(--surface-weak);
  color: var(--text-strong);
}

.status-badge--live {
  background: var(--live-color-soft);
  color: var(--live-color);
}

.status-badge--upcoming {
  background: var(--hover-bg);
  color: var(--primary-color);
}

.status-badge--vod {
  background: var(--border-color);
  color: var(--text-muted);
}

.status-schedule {
  color: var(--text-muted);
  font-weight: 700;
}

.player-title {
  margin: 0;
  font-size: 1.3rem;
  font-weight: 800;
}

.player-seller {
  margin: 0;
  font-weight: 700;
  color: var(--text-strong);
}

.player-desc {
  margin: 0;
  color: var(--text-muted);
}

.player-frame {
  width: 100%;
  aspect-ratio: 16 / 9;
  background: #10131b;
  border-radius: 16px;
  display: grid;
  place-items: center;
  color: #fff;
  font-weight: 700;
  overflow: hidden;
}

.player-frame__label {
  opacity: 0.8;
}

.player-embed,
.player-video {
  width: 100%;
  height: 100%;
  border: 0;
  display: block;
}

.player-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border-top: 1px solid var(--border-color);
  background: var(--surface-weak);
  border-radius: 12px;
  flex-wrap: nowrap;
}

.player-toolbar__group {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 0;
}

.player-toolbar__group--left {
  justify-content: flex-start;
}

.player-toolbar__group--right {
  justify-content: flex-end;
}

.toolbar-btn {
  border: 1px solid var(--border-color);
  background: var(--surface);
  color: var(--text-strong);
  border-radius: 10px;
  height: 36px;
  padding: 0 12px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.toolbar-btn[disabled] {
  opacity: 0.7;
  cursor: not-allowed;
}

.toolbar-svg {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

.toolbar-label {
  white-space: nowrap;
}

.chat-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-right: 4px;
}

.chat-message {
  display: grid;
  gap: 4px;
  padding: 8px 10px;
  border-radius: 12px;
  background: var(--surface-weak);
}

.chat-message--system {
  background: var(--hover-bg);
  color: var(--text-muted);
}

.chat-message__user {
  font-weight: 800;
  font-size: 0.85rem;
}

.chat-message__text {
  margin: 0;
  color: var(--text-strong);
}

.chat-message__time {
  font-size: 0.75rem;
  color: var(--text-soft);
}

.chat-input {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
}

.chat-input input {
  border: 1px solid var(--border-color);
  border-radius: 10px;
  padding: 10px 12px;
  font-size: 0.95rem;
  background: var(--surface);
}

.chat-input button {
  border: none;
  background: var(--primary-color);
  color: #fff;
  font-weight: 800;
  border-radius: 10px;
  padding: 10px 16px;
  cursor: not-allowed;
  opacity: 0.7;
}

.empty-state {
  display: flex;
  flex-direction: column;
  gap: 12px;
  color: var(--text-muted);
}

.link-back {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-weight: 700;
  color: var(--primary-color);
}

@media (max-width: 1080px) {
  .live-detail-main {
    grid-template-columns: 1fr;
  }

  .panel--chat {
    order: 2;
  }

  .panel--player {
    order: 1;
  }
}

@media (max-width: 640px) {
  .live-detail-main {
    gap: 14px;
  }

  .product-list--grid {
    grid-template-columns: 1fr;
  }

  .product-card {
    grid-template-columns: 1fr;
  }

  .product-card__thumb {
    width: 100%;
    height: 160px;
  }

  .toolbar-label {
    display: none;
  }

  .toolbar-btn {
    height: 36px;
    padding: 0 8px;
  }

  .chat-input {
    grid-template-columns: 1fr;
  }
}
</style>
