<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PageContainer from '../../../components/PageContainer.vue'
import { getSellerVodDetail } from '../../../lib/mocks/sellerVods'

const route = useRoute()
const router = useRouter()

const vodId = typeof route.params.vodId === 'string' ? route.params.vodId : ''
const detail = ref(getSellerVodDetail(vodId))

const goBack = () => {
  router.back()
}

const goToList = () => {
  router.push('/seller/live?tab=vod').catch(() => {})
}

const toggleVisibility = () => {
  const next = detail.value.vod.visibility === '공개' ? '비공개' : '공개'
  detail.value = { ...detail.value, vod: { ...detail.value.vod, visibility: next } }
}

const handleDownload = () => {
  window.alert('VOD 파일을 다운로드합니다. (데모)')
}

const handleDelete = () => {
  const ok = window.confirm('해당 VOD를 삭제하시겠습니까?')
  if (!ok) return
  window.alert('VOD가 삭제되었습니다. (데모)')
}

const showChat = ref(false)
const chatText = ref('')
const chatMessages = ref([
  { id: 'c1', user: '시청자A', text: '잘 봤어요!', time: '오후 2:10' },
  { id: 'c2', user: '관리자', text: '채팅은 보관용입니다.', time: '오후 2:12' },
])

const sendChat = () => {
  if (!chatText.value.trim()) return
  chatMessages.value = [...chatMessages.value, { id: `c-${Date.now()}`, user: '관리자', text: chatText.value, time: '방금' }]
  chatText.value = ''
}

const videoRef = ref<HTMLVideoElement | null>(null)

const requestFullscreen = () => {
  const el = videoRef.value
  if (!el) return
  if (document.fullscreenElement) {
    document.exitFullscreen().catch(() => {})
    return
  }
  el.requestFullscreen?.()
}

const requestMini = () => {
  const el = videoRef.value
  if (!el) return
  if ('requestPictureInPicture' in el) {
    // @ts-ignore
    el.requestPictureInPicture().catch(() => {})
  }
}
</script>

<template>
  <PageContainer>
    <header class="detail-header">
      <button type="button" class="back-link" @click="goBack">← 뒤로 가기</button>
      <button type="button" class="btn ghost" @click="goToList">목록으로</button>
    </header>

    <h2 class="page-title">방송 결과 리포트</h2>

    <section class="detail-card ds-surface">
      <div class="info-grid">
        <div class="thumb-box">
          <img :src="detail.thumb" :alt="detail.title" />
        </div>
        <div class="info-meta">
          <h3>{{ detail.title }}</h3>
          <p><span>방송 시작 시간</span>{{ detail.startedAt }}</p>
          <p><span>방송 종료 시간</span>{{ detail.endedAt }}</p>
          <p><span>상태</span>{{ detail.statusLabel }}</p>
        </div>
      </div>
    </section>

    <section class="kpi-grid">
      <article class="kpi-card ds-surface">
        <p class="kpi-label">최대 시청자 수</p>
        <p class="kpi-value">{{ detail.metrics.maxViewers.toLocaleString('ko-KR') }}</p>
      </article>
      <article class="kpi-card ds-surface">
        <p class="kpi-label">신고 건수</p>
        <p class="kpi-value">{{ detail.metrics.reports }}</p>
      </article>
      <article class="kpi-card ds-surface">
        <p class="kpi-label">시청자 제재 건수</p>
        <p class="kpi-value">{{ detail.metrics.sanctions }}</p>
      </article>
      <article class="kpi-card ds-surface">
        <p class="kpi-label">좋아요</p>
        <p class="kpi-value">{{ detail.metrics.likes.toLocaleString('ko-KR') }}</p>
      </article>
      <article class="kpi-card ds-surface">
        <p class="kpi-label">총 매출</p>
        <p class="kpi-value">₩{{ detail.metrics.totalRevenue.toLocaleString('ko-KR') }}</p>
      </article>
    </section>

    <section class="detail-card ds-surface">
      <div class="card-head">
        <h3>VOD</h3>
        <div class="vod-actions">
          <label class="toggle">
            <input type="checkbox" :checked="detail.vod.visibility === '공개'" @change="toggleVisibility" />
            <span>{{ detail.vod.visibility === '공개' ? '공개' : '비공개' }}</span>
          </label>
          <button type="button" class="icon-btn" @click="handleDownload">⬇ 다운로드</button>
          <button type="button" class="icon-btn danger" @click="handleDelete">🗑 삭제</button>
        </div>
      </div>
      <div class="vod-player">
        <div class="player-shell">
          <video v-if="detail.vod.url" ref="videoRef" :src="detail.vod.url" controls></video>
          <div v-else class="vod-placeholder">
            <span>재생할 VOD가 없습니다.</span>
          </div>
          <div class="player-controls">
            <button type="button" class="icon-btn" @click="requestFullscreen">⛶ 전체화면</button>
            <button type="button" class="icon-btn" @click="requestMini">▣ 축소화면</button>
            <button type="button" class="icon-btn" @click="showChat = !showChat">{{ showChat ? '채팅 닫기' : '채팅 보기' }}</button>
          </div>
        </div>
        <aside v-if="showChat" class="chat-panel ds-surface">
          <header class="chat-head">
            <h4>채팅</h4>
            <button type="button" class="icon-btn" @click="showChat = false">✕</button>
          </header>
          <div class="chat-list">
            <div v-for="msg in chatMessages" :key="msg.id" class="chat-row">
              <div class="chat-meta">
                <span class="chat-user">{{ msg.user }}</span>
                <span class="chat-time">{{ msg.time }}</span>
              </div>
              <p class="chat-text">{{ msg.text }}</p>
            </div>
          </div>
          <div class="chat-input">
            <input v-model="chatText" type="text" placeholder="메시지 입력" />
            <button type="button" class="btn primary" @click="sendChat">전송</button>
          </div>
        </aside>
      </div>
    </section>

    <section class="detail-card ds-surface">
      <div class="card-head">
        <h3>상품별 성과</h3>
      </div>
      <div class="table-wrap">
        <table class="product-table">
          <thead>
            <tr>
              <th>상품명</th>
              <th>가격</th>
              <th>판매 수량</th>
              <th>매출</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in detail.productResults" :key="item.id">
              <td>{{ item.name }}</td>
              <td>₩{{ item.price.toLocaleString('ko-KR') }}</td>
              <td>{{ item.soldQty }}</td>
              <td>₩{{ item.revenue.toLocaleString('ko-KR') }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </PageContainer>
</template>

<style scoped>
.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.page-title {
  margin: 0 0 16px;
  font-size: 1.5rem;
  font-weight: 900;
  color: var(--text-strong);
}

.back-link {
  border: none;
  background: transparent;
  color: var(--text-muted);
  font-weight: 800;
  cursor: pointer;
  padding: 6px 0;
}

.btn {
  border: 1px solid var(--border-color);
  background: var(--surface);
  color: var(--text-strong);
  border-radius: 999px;
  padding: 10px 16px;
  font-weight: 800;
  cursor: pointer;
}

.btn.ghost {
  background: transparent;
  color: var(--text-muted);
}

.detail-card {
  padding: 18px;
  border-radius: 16px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-bottom: 16px;
}

.info-grid {
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.thumb-box {
  width: 180px;
  height: 120px;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid var(--border-color);
  background: var(--surface-weak);
}

.thumb-box img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.info-meta h3 {
  margin: 0 0 10px;
  font-size: 1.1rem;
  font-weight: 900;
  color: var(--text-strong);
}

.info-meta p {
  margin: 0 0 6px;
  color: var(--text-muted);
  font-weight: 700;
  display: flex;
  gap: 12px;
}

.info-meta span {
  min-width: 120px;
  color: var(--text-strong);
  font-weight: 800;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.kpi-card {
  padding: 16px;
  border-radius: 14px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.kpi-label {
  margin: 0;
  color: var(--text-muted);
  font-weight: 700;
  font-size: 0.85rem;
}

.kpi-value {
  margin: 0;
  color: var(--text-strong);
  font-weight: 900;
  font-size: 1.2rem;
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.card-head h3 {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 900;
  color: var(--text-strong);
}

.vod-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.toggle {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 700;
  color: var(--text-muted);
}

.icon-btn {
  border: 1px solid var(--border-color);
  background: var(--surface);
  color: var(--text-strong);
  border-radius: 999px;
  padding: 6px 12px;
  font-weight: 800;
  cursor: pointer;
}

.icon-btn.danger {
  background: #ef4444;
  border-color: transparent;
  color: #fff;
}

.vod-player {
  border-radius: 14px;
  border: 1px solid var(--border-color);
  background: var(--surface-weak);
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 12px;
  overflow: hidden;
  align-items: start;
}

.player-shell {
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.vod-player video {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 12px;
}

.vod-placeholder {
  color: var(--text-muted);
  font-weight: 700;
  display: grid;
  place-items: center;
  min-height: 260px;
  border-radius: 12px;
}

.player-controls {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.chat-panel {
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  border-radius: 0 12px 12px 0;
}

.chat-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chat-list {
  max-height: 240px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.chat-row {
  background: var(--surface-weak);
  border-radius: 10px;
  padding: 8px;
}

.chat-meta {
  display: flex;
  gap: 6px;
  align-items: center;
  font-weight: 800;
  color: var(--text-muted);
  margin-bottom: 4px;
}

.chat-text {
  margin: 0;
  font-weight: 700;
  color: var(--text-strong);
}

.chat-input {
  display: flex;
  gap: 6px;
}

.chat-input input {
  flex: 1;
  border: 1px solid var(--border-color);
  border-radius: 10px;
  padding: 8px 10px;
}

.table-wrap {
  overflow-x: auto;
}

.product-table {
  width: 100%;
  min-width: 560px;
  border-collapse: collapse;
}

.product-table th,
.product-table td {
  padding: 12px;
  border-bottom: 1px solid var(--border-color);
  text-align: left;
  font-weight: 700;
  color: var(--text-strong);
}

.product-table thead th {
  background: var(--surface-weak);
  font-weight: 900;
}

@media (max-width: 960px) {
  .kpi-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .vod-player {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .detail-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .info-grid {
    grid-template-columns: 1fr;
  }

  .thumb-box {
    width: 100%;
    height: 160px;
  }

  .kpi-grid {
    grid-template-columns: 1fr;
  }
}
</style>
