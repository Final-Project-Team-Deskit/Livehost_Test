<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import PageContainer from '../components/PageContainer.vue'
import PageHeader from '../components/PageHeader.vue'
import { fetchBroadcasts, type BroadcastListItem, type BroadcastTab } from '../api/liveApi'

const router = useRouter()
const today = new Date()

const TAB_OPTIONS: { label: string; value: BroadcastTab }[] = [
  { label: '전체', value: 'ALL' },
  { label: 'LIVE', value: 'LIVE' },
  { label: '예약', value: 'RESERVED' },
  { label: 'VOD', value: 'VOD' },
]

const normalizeDay = (d: Date) => new Date(d.getFullYear(), d.getMonth(), d.getDate())
const getDayWindow = (base: Date) => {
  const result: Date[] = []
  for (let offset = -3; offset <= 3; offset += 1) {
    const day = new Date(base)
    day.setDate(base.getDate() + offset)
    result.push(normalizeDay(day))
  }
  return result
}

const dayWindow = computed(() => getDayWindow(today))
const selectedDay = ref<Date>(normalizeDay(dayWindow.value[3]))
const activeTab = ref<BroadcastTab>('ALL')

const broadcasts = ref<BroadcastListItem[]>([])
const loading = ref(false)
const errorMessage = ref<string | null>(null)
const toast = ref<{ message: string; variant: 'success' | 'neutral' } | null>(null)
let toastTimer: ReturnType<typeof setTimeout> | null = null

const formatTime = (value?: string) => {
  if (!value) return ''
  const time = new Date(value)
  const hours = time.getHours().toString().padStart(2, '0')
  const minutes = time.getMinutes().toString().padStart(2, '0')
  return `${hours}:${minutes}`
}

const getStartDateForFilter = () => {
  const start = new Date(selectedDay.value)
  start.setHours(0, 0, 0, 0)
  return start
}

const getEndDateForFilter = () => {
  const end = new Date(selectedDay.value)
  end.setHours(23, 59, 59, 999)
  return end
}

const statusToDisplay = (status: BroadcastListItem['status']) => {
  if (status === 'ON_AIR') return 'LIVE'
  if (status === 'RESERVED') return '예정'
  if (status === 'VOD') return 'VOD'
  return '종료'
}

const getStatus = (item: BroadcastListItem) => {
  if (item.status === 'ON_AIR') return 'LIVE'
  if (item.status === 'RESERVED') return 'UPCOMING'
  return 'ENDED'
}

const showToast = (message: string, variant: 'success' | 'neutral') => {
  toast.value = { message, variant }
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => {
    toast.value = null
    toastTimer = null
  }, 2200)
}

const fetchForDay = async () => {
  loading.value = true
  errorMessage.value = null
  try {
    const { content } = await fetchBroadcasts({
      tab: activeTab.value,
      startDate: getStartDateForFilter(),
      endDate: getEndDateForFilter(),
      page: 0,
      size: 200,
    })
    broadcasts.value = content
  } catch (error) {
    console.error('Failed to fetch live schedule', error)
    errorMessage.value = '라이브 일정 정보를 불러오지 못했습니다. 잠시 후 다시 시도해주세요.'
  } finally {
    loading.value = false
  }
}

const handleRowClick = (item: BroadcastListItem) => {
  const id = item.id ?? item.broadcastId
  if (!id) return
  if (item.status === 'ON_AIR') {
    router.push({ name: 'live-detail', params: { id } })
  } else if (item.status === 'VOD' || item.status === 'ENDED') {
    router.push({ name: 'vod', params: { id } })
  }
}

const groupedByTime = computed(() => {
  const map = new Map<string, BroadcastListItem[]>()
  broadcasts.value.forEach((item) => {
    const key = formatTime(item.startedAt ?? item.scheduledAt ?? item.startAt)
    const bucket = map.get(key) ?? []
    bucket.push(item)
    map.set(key, bucket)
  })
  return Array.from(map.entries()).sort(([a], [b]) => (a < b ? -1 : 1))
})

const isSelectedDay = (day: Date) =>
  day.getFullYear() === selectedDay.value.getFullYear() &&
  day.getMonth() === selectedDay.value.getMonth() &&
  day.getDate() === selectedDay.value.getDate()

const formatDayLabel = (day: Date) => {
  const dayNames = ['일', '월', '화', '수', '목', '금', '토']
  const label = day.getTime() === normalizeDay(today).getTime() ? '오늘' : dayNames[day.getDay()]
  const date = `${day.getMonth() + 1}.${day.getDate()}`
  return { label, date }
}

const selectDay = async (day: Date) => {
  selectedDay.value = normalizeDay(day)
  await fetchForDay()
}

const handleTabChange = async (tab: BroadcastTab) => {
  activeTab.value = tab
  await fetchForDay()
}

onMounted(() => {
  fetchForDay()
})

watch(
  () => [selectedDay.value, activeTab.value],
  () => {},
)

onBeforeUnmount(() => {
  if (toastTimer) clearTimeout(toastTimer)
})
</script>

<template>
  <PageContainer>
    <PageHeader title="라이브 일정" eyebrow="DESKIT LIVE" />

    <div class="tabs" role="tablist">
      <button
        v-for="tab in TAB_OPTIONS"
        :key="tab.value"
        type="button"
        class="tab-btn"
        :class="{ 'tab-btn--active': activeTab === tab.value }"
        @click="handleTabChange(tab.value)"
      >
        {{ tab.label }}
      </button>
    </div>

    <div class="date-strip">
      <button
        v-for="day in dayWindow"
        :key="day.toISOString()"
        type="button"
        class="date-pill"
        :class="{ 'date-pill--active': isSelectedDay(day) }"
        @click="selectDay(day)"
      >
        <span class="date-pill__label">{{ formatDayLabel(day).label }}</span>
        <span class="date-pill__date">{{ formatDayLabel(day).date }}</span>
      </button>
    </div>

    <div v-if="loading" class="empty-state-box">라이브 일정을 불러오는 중입니다...</div>
    <div v-else-if="errorMessage" class="empty-state-box empty-state-box--error">
      <p>{{ errorMessage }}</p>
      <button type="button" class="action-btn action-btn--ghost" @click="fetchForDay">다시 시도</button>
    </div>
    <div v-else-if="!broadcasts.length" class="empty-state-box">
      No broadcasts scheduled for this date.
    </div>

    <div v-else class="timeline">
      <div v-for="[time, items] in groupedByTime" :key="time" class="time-group">
        <div class="time-label">{{ time }}</div>
        <div class="time-group__list">
          <article
            v-for="item in items"
            :key="item.broadcastId"
            class="live-card-row"
            :class="{
              'row--clickable': getStatus(item) !== 'UPCOMING',
              'row--disabled': getStatus(item) === 'UPCOMING',
            }"
            :aria-disabled="getStatus(item) === 'UPCOMING' ? 'true' : undefined"
            :tabindex="getStatus(item) === 'UPCOMING' ? -1 : 0"
            @click="handleRowClick(item)"
          >
            <img class="thumb" :src="item.thumbnailUrl || '/placeholder-live-thumb.jpg'" :alt="item.title" />
            <div class="meta">
              <div class="meta__title-row">
                <h4 class="meta__title">{{ item.title }}</h4>
                <span v-if="item.status === 'ON_AIR'" class="status-pill status-pill--live">
                  LIVE
                  <span v-if="item.viewerCount" class="status-viewers">
                    {{ item.viewerCount.toLocaleString() }}명
                  </span>
                </span>
                <span v-else-if="item.status === 'RESERVED'" class="status-pill">예정</span>
                <span v-else class="status-pill status-pill--ended">{{ statusToDisplay(item.status) }}</span>
              </div>
              <p v-if="item.sellerName" class="meta__seller">{{ item.sellerName }}</p>
              <p v-if="item.notice" class="meta__desc">{{ item.notice }}</p>
              <p v-else-if="item.description" class="meta__desc">{{ item.description }}</p>
            </div>
            <div class="right-slot">
              <span class="meta__time">{{ formatTime(item.scheduledAt ?? item.startedAt ?? item.startAt) }}</span>
            </div>
          </article>
        </div>
      </div>
    </div>

    <div v-if="toast" class="toast" :class="`toast--${toast.variant}`" role="status" aria-live="polite">
      {{ toast.message }}
    </div>
  </PageContainer>
</template>

<style scoped>
.tabs {
  display: inline-flex;
  gap: 10px;
  padding: 6px;
  background: var(--surface);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  margin-bottom: 12px;
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

.date-strip {
  display: flex;
  gap: 10px;
  width: 100%;
  justify-content: center;
  flex-wrap: wrap;
  padding: 6px 2px 18px;
  margin: 0 auto 18px;
}

.date-pill {
  border: 1px solid var(--border-color);
  background: var(--surface);
  border-radius: 12px;
  padding: 10px 12px;
  min-width: 76px;
  display: grid;
  gap: 4px;
  text-align: center;
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
  flex: 0 0 auto;
}

.date-pill--active {
  border-color: var(--primary-color);
  box-shadow: 0 10px 22px rgba(119, 136, 115, 0.12);
  transform: translateY(-1px);
}

.date-pill__label {
  font-weight: 800;
  font-size: 0.9rem;
  color: var(--text-strong);
}

.date-pill__date {
  font-size: 0.85rem;
  color: var(--text-muted);
  min-height: 1.1em;
}

.timeline {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.time-group {
  display: grid;
  grid-template-columns: 96px 1fr;
  gap: 16px;
  align-items: start;
}

.time-label {
  font-size: 1.2rem;
  font-weight: 800;
  color: var(--text-strong);
  padding-top: 8px;
}

.time-group__list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.live-card-row {
  display: grid;
  grid-template-columns: 180px 1fr auto;
  gap: 18px;
  align-items: center;
  padding: 14px;
  border-radius: 16px;
  border: 1px solid var(--border-color);
  background: var(--surface);
}

.row--clickable {
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.row--clickable:hover {
  border-color: var(--primary-color);
  box-shadow: 0 10px 22px rgba(119, 136, 115, 0.12);
  transform: translateY(-1px);
}

.row--disabled {
  cursor: default;
  opacity: 0.66;
}

.thumb {
  width: 180px;
  height: 140px;
  border-radius: 16px;
  object-fit: cover;
}

.meta {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.meta__title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.meta__title {
  margin: 0;
  font-size: 1.15rem;
  font-weight: 800;
  color: var(--text-strong);
  line-height: 1.35;
}

.meta__seller {
  margin: 0;
  color: var(--text-muted);
  font-size: 0.95rem;
}

.meta__desc {
  margin: 0;
  color: var(--text-soft);
  font-size: 0.85rem;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  white-space: normal;
  line-height: 1.4;
}

.status-pill {
  padding: 2px 10px;
  border-radius: 999px;
  background: var(--surface-weak);
  color: var(--text-muted);
  font-size: 0.8rem;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.status-pill--live {
  background: var(--live-color-soft);
  color: var(--live-color);
}

.status-pill--ended {
  background: var(--border-color);
  color: var(--text-muted);
}

.status-viewers {
  font-size: 0.75rem;
  font-weight: 700;
}

.right-slot {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  min-width: 132px;
}

.meta__time {
  font-weight: 800;
  color: var(--text-strong);
}

.action-btn {
  border: none;
  background: var(--primary-color);
  color: #fff;
  font-weight: 800;
  border-radius: 12px;
  padding: 8px 14px;
  cursor: pointer;
}

.action-btn--ghost {
  background: var(--surface);
  color: var(--text-strong);
  border: 1px solid var(--border-color);
}

.toast {
  position: fixed;
  left: 50%;
  bottom: 24px;
  transform: translateX(-50%);
  padding: 10px 16px;
  border-radius: 999px;
  font-weight: 700;
  font-size: 0.9rem;
  box-shadow: 0 10px 22px rgba(0, 0, 0, 0.12);
  z-index: 20;
}

.toast--success {
  background: var(--primary-color);
  color: #fff;
}

.toast--neutral {
  background: var(--surface);
  color: var(--text-strong);
  border: 1px solid var(--border-color);
}

.empty-state-box {
  padding: 18px;
  border: 1px dashed var(--border-color);
  border-radius: 14px;
  background: var(--surface);
  color: var(--text-muted);
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.empty-state-box--error {
  color: var(--text-strong);
}

@media (max-width: 960px) {
  .time-group {
    grid-template-columns: 72px 1fr;
  }

  .live-card-row {
    grid-template-columns: 150px 1fr auto;
  }

  .thumb {
    width: 150px;
    height: 120px;
  }
}

@media (max-width: 640px) {
  .time-group {
    grid-template-columns: 1fr;
  }

  .time-label {
    padding-top: 0;
  }

  .live-card-row {
    grid-template-columns: 1fr;
    align-items: flex-start;
  }

  .thumb {
    width: 100%;
    height: 180px;
  }

  .right-slot {
    width: 100%;
  }

  .action-btn {
    width: 100%;
  }
}
</style>
