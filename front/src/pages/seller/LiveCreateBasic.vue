<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PageContainer from '../../components/PageContainer.vue'
import PageHeader from '../../components/PageHeader.vue'
import { addScheduledBroadcast } from '../../composables/useSellerBroadcasts'
import {
  DRAFT_KEY,
  buildDraftFromReservation,
  createEmptyDraft,
  loadDraft,
  saveDraft,
  type LiveCreateDraft,
  type LiveCreateProduct,
} from '../../composables/useLiveCreateDraft'
import { productsData } from '../../lib/products-data'

const router = useRouter()
const route = useRoute()

const draft = ref<LiveCreateDraft>(createEmptyDraft())
const productSearch = ref('')
const thumbError = ref('')
const standbyError = ref('')
const error = ref('')

const reservationId = computed(() => (typeof route.query.reservationId === 'string' ? route.query.reservationId : ''))
const isEditMode = computed(() => route.query.mode === 'edit' && !!reservationId.value)

const availableProducts = computed(() =>
  productsData.map<LiveCreateProduct>((product) => ({
    id: `prod-${product.product_id}`,
    name: product.name,
    option: product.short_desc ?? '-',
    price: product.price,
    broadcastPrice: product.price,
    stock: product.salesVolume ?? 100,
    quantity: 1,
    thumb: product.imageUrl,
  })),
)

const filteredProducts = computed(() => {
  const q = productSearch.value.trim().toLowerCase()
  if (!q) return availableProducts.value
  return availableProducts.value.filter((product) => {
    const name = product.name.toLowerCase()
    const option = product.option.toLowerCase()
    return name.includes(q) || option.includes(q)
  })
})

const isSelected = (productId: string) => draft.value.products.some((item) => item.id === productId)

const toggleProduct = (product: LiveCreateProduct) => {
  if (!isSelected(product.id) && draft.value.products.length >= 10) {
    error.value = '상품은 최대 10개까지 등록할 수 있습니다.'
    return
  }
  if (isSelected(product.id)) {
    draft.value.products = draft.value.products.filter((item) => item.id !== product.id)
    return
  }
  draft.value.products.push({ ...product })
}

const updateProductPrice = (productId: string, value: number) => {
  draft.value.products = draft.value.products.map((product) =>
    product.id === productId ? { ...product, broadcastPrice: value < 0 ? 0 : value } : product,
  )
}

const updateProductQuantity = (productId: string, value: number) => {
  const qty = Number.isNaN(value) || value < 1 ? 1 : value
  draft.value.products = draft.value.products.map((product) =>
    product.id === productId ? { ...product, quantity: qty } : product,
  )
}

const syncDraft = () => {
  saveDraft({
    ...draft.value,
    title: draft.value.title.trim(),
    subtitle: draft.value.subtitle?.trim() ?? '',
    category: draft.value.category.trim(),
    notice: draft.value.notice.trim(),
    questions: draft.value.questions.map((q) => ({ ...q, text: q.text.trim() })),
  })
}

const restoreDraft = () => {
  const saved = loadDraft()
  if (saved && (!isEditMode.value || saved.reservationId === reservationId.value)) {
    draft.value = { ...draft.value, ...saved }
  }

  if (isEditMode.value) {
    draft.value = { ...draft.value, ...buildDraftFromReservation(reservationId.value) }
  }

  syncDraft()
}

const handleThumbUpload = (event: Event) => {
  thumbError.value = ''
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) {
    thumbError.value = '이미지 파일만 업로드할 수 있습니다.'
    input.value = ''
    return
  }
  const reader = new FileReader()
  reader.onload = () => {
    draft.value.thumb = typeof reader.result === 'string' ? reader.result : ''
  }
  reader.readAsDataURL(file)
}

const handleStandbyUpload = (event: Event) => {
  standbyError.value = ''
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) {
    standbyError.value = '이미지 파일만 업로드할 수 있습니다.'
    input.value = ''
    return
  }
  const reader = new FileReader()
  reader.onload = () => {
    draft.value.standbyThumb = typeof reader.result === 'string' ? reader.result : ''
  }
  reader.readAsDataURL(file)
}

const isQuestionValid = (text: string) => {
  const trimmed = text.trim()
  return !!trimmed && trimmed.includes('?')
}

const submit = () => {
  error.value = ''
  thumbError.value = ''
  standbyError.value = ''

  if (draft.value.questions.some((q) => !isQuestionValid(q.text))) {
    error.value = '큐카드 질문을 모두 등록해주세요. ( ? 포함 필수 )'
    router.push({ path: '/seller/live/create', query: route.query }).catch(() => {})
    return
  }

  if (!draft.value.title.trim() || !draft.value.category || !draft.value.date || !draft.value.time) {
    error.value = '방송 제목, 카테고리, 일정을 입력해주세요.'
    return
  }

  if (!draft.value.products.length) {
    error.value = '최소 1개의 판매 상품을 등록해주세요.'
    return
  }

  if (!draft.value.termsAgreed) {
    error.value = '약관에 동의해주세요.'
    return
  }

  const id = draft.value.reservationId || `schedule-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`
  const datetime = `${draft.value.date} ${draft.value.time}`
  const scheduled = {
    id,
    title: draft.value.title.trim(),
    subtitle: draft.value.subtitle?.trim() || '예약 방송',
    thumb: draft.value.thumb,
    datetime,
    ctaLabel: '방송 시작',
    products: draft.value.products,
    standbyThumb: draft.value.standbyThumb || undefined,
    termsAgreed: draft.value.termsAgreed,
    category: draft.value.category,
    notice: draft.value.notice,
  }

  addScheduledBroadcast(scheduled)
  localStorage.removeItem(DRAFT_KEY)
  const redirectPath = isEditMode.value
    ? `/seller/broadcasts/reservations/${id}`
    : '/seller/live?tab=scheduled'
  router.push(redirectPath).catch(() => {})
}

const goPrev = () => {
  router.push({ path: '/seller/live/create', query: route.query }).catch(() => {})
}

const cancel = () => {
  const redirect = isEditMode.value && reservationId.value
    ? `/seller/broadcasts/reservations/${reservationId.value}`
    : '/seller/live?tab=scheduled'
  router.push(redirect).catch(() => {})
}

onMounted(() => {
  restoreDraft()
})

watch(
  draft,
  () => {
    syncDraft()
  },
  { deep: true },
)
</script>

<template>
  <PageContainer>
    <PageHeader :eyebrow="isEditMode ? 'DESKIT' : 'DESKIT'" :title="isEditMode ? '예약 수정 - 기본 정보' : '방송 등록 - 기본 정보'" />
    <section class="create-card ds-surface">
      <div class="step-meta">
        <span class="step-label">2 / 2 단계</span>
        <div class="step-actions">
          <button type="button" class="btn ghost" @click="goPrev">이전</button>
          <button type="button" class="btn ghost" @click="cancel">취소</button>
        </div>
      </div>
      <label class="field">
        <span class="field__label">방송 제목</span>
        <input v-model="draft.title" type="text" maxlength="30" placeholder="예: 홈오피스 라이브" />
        <span class="field__hint">{{ draft.title.length }}/30</span>
      </label>
      <div class="field-grid">
        <label class="field">
          <span class="field__label">카테고리</span>
          <select v-model="draft.category">
            <option value="" disabled>카테고리를 선택하세요</option>
            <option value="가구">가구</option>
            <option value="전자기기">전자기기</option>
            <option value="패션">패션</option>
            <option value="뷰티">뷰티</option>
            <option value="악세사리">악세사리</option>
          </select>
        </label>
        <label class="field">
          <span class="field__label">간단 설명</span>
          <input v-model="draft.subtitle" type="text" placeholder="예: 셋업 추천" />
        </label>
      </div>
      <label class="field">
        <span class="field__label">공지사항</span>
        <textarea
          v-model="draft.notice"
          rows="3"
          maxlength="50"
          placeholder="시청자에게 안내할 공지를 입력하세요 (최대 50자)"
        ></textarea>
        <span class="field__hint">{{ draft.notice.length }}/50</span>
      </label>
      <div class="field-grid">
        <label class="field">
          <span class="field__label">방송 날짜</span>
          <input v-model="draft.date" type="date" />
        </label>
        <label class="field">
          <span class="field__label">방송 시간</span>
          <input v-model="draft.time" type="time" />
        </label>
      </div>
      <div class="section-block">
        <div class="section-head">
          <h3>판매 상품 등록</h3>
          <span class="count-pill">선택 {{ draft.products.length }}개</span>
        </div>
        <label class="field">
          <span class="field__label">상품 검색</span>
          <input v-model="productSearch" type="text" placeholder="상품명을 검색하세요" />
        </label>
        <div class="product-select">
          <label v-for="product in filteredProducts" :key="product.id" class="product-option">
            <input type="checkbox" :checked="isSelected(product.id)" @change="toggleProduct(product)" />
            <span class="product-option__text">
              <strong>{{ product.name }}</strong>
              <span class="product-option__meta">{{ product.option }}</span>
            </span>
          </label>
        </div>
        <div v-if="draft.products.length" class="product-table-wrap">
          <table>
            <thead>
              <tr>
                <th>상품명</th>
                <th>정가</th>
                <th>방송 할인가</th>
                <th>판매 수량</th>
                <th>재고</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="product in draft.products" :key="product.id">
                <td>
                  <div class="product-cell">
                    <div class="thumb" v-if="product.thumb">
                      <img :src="product.thumb" :alt="product.name" />
                    </div>
                    <div class="product-text">
                      <strong>{{ product.name }}</strong>
                      <span class="product-option__meta">{{ product.option }}</span>
                    </div>
                  </div>
                </td>
                <td class="numeric">{{ product.price.toLocaleString() }}원</td>
                <td>
                  <input
                    class="table-input"
                    type="number"
                    min="0"
                    :value="product.broadcastPrice"
                    @input="updateProductPrice(product.id, Number(($event.target as HTMLInputElement).value))"
                  />
                </td>
                <td>
                  <input
                    class="table-input"
                    type="number"
                    min="1"
                    :value="product.quantity"
                    @input="updateProductQuantity(product.id, Number(($event.target as HTMLInputElement).value))"
                  />
                </td>
                <td class="numeric">{{ product.stock }}</td>
                <td>
                  <button type="button" class="btn ghost" @click="toggleProduct(product)">제거</button>
                </td>
              </tr>
            </tbody>
          </table>
          <p class="table-hint">{{ draft.products.length }}/10 개 선택됨 (최소 1개 필수)</p>
        </div>
      </div>
      <div class="section-block">
        <div class="section-head">
          <h3>썸네일/대기화면</h3>
        </div>
        <div class="field-grid">
          <label class="field">
            <span class="field__label">방송 썸네일 업로드</span>
            <input type="file" accept="image/*" @change="handleThumbUpload" />
            <span v-if="thumbError" class="error">{{ thumbError }}</span>
            <div v-if="draft.thumb" class="preview">
              <img :src="draft.thumb" alt="방송 썸네일 미리보기" />
            </div>
          </label>
          <label class="field">
            <span class="field__label">대기화면 업로드</span>
            <input type="file" accept="image/*" @change="handleStandbyUpload" />
            <span v-if="standbyError" class="error">{{ standbyError }}</span>
            <div v-if="draft.standbyThumb" class="preview">
              <img :src="draft.standbyThumb" alt="대기화면 미리보기" />
            </div>
          </label>
        </div>
      </div>
      <div class="section-block">
        <label class="checkbox">
          <input v-model="draft.termsAgreed" type="checkbox" />
          <span>방송 운영 및 약관에 동의합니다. (필수)</span>
        </label>
      </div>
      <p v-if="error" class="error">{{ error }}</p>
      <div class="actions">
        <div class="step-indicator">
          <span class="step-label">2 / 2 단계</span>
        </div>
        <div class="action-buttons">
          <button type="button" class="btn ghost" @click="goPrev">이전</button>
          <button type="button" class="btn" @click="cancel">취소</button>
          <button type="button" class="btn primary" @click="submit">{{ isEditMode ? '저장' : '방송 등록' }}</button>
        </div>
      </div>
    </section>
  </PageContainer>
</template>

<style scoped>
.create-card {
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.step-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.step-label {
  font-weight: 800;
  color: var(--text-muted);
}

.step-actions {
  display: flex;
  gap: 8px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field__label {
  font-weight: 800;
  color: var(--text-strong);
}

.field__hint {
  color: var(--text-muted);
  font-weight: 700;
  font-size: 0.85rem;
}

.field-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

input,
select,
textarea {
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 10px 12px;
  font-weight: 700;
  color: var(--text-strong);
  background: var(--surface);
}

input[type='file'] {
  padding: 8px 0;
}

.section-block {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.section-head h3 {
  margin: 0;
  font-size: 1rem;
  font-weight: 900;
  color: var(--text-strong);
}

.count-pill {
  border: 1px solid var(--border-color);
  background: var(--surface-weak);
  color: var(--text-strong);
  padding: 6px 10px;
  border-radius: 999px;
  font-weight: 800;
  font-size: 0.85rem;
}

.empty-hint {
  color: var(--text-muted);
  font-weight: 700;
  font-size: 0.9rem;
}

.product-select {
  max-height: 220px;
  overflow: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-right: 4px;
}

.product-option {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px;
  border-radius: 12px;
  border: 1px solid var(--border-color);
  background: var(--surface);
  cursor: pointer;
}

.product-option input {
  margin-top: 4px;
}

.product-option__text {
  display: flex;
  flex-direction: column;
  gap: 4px;
  color: var(--text-strong);
  font-weight: 800;
}

.product-option__meta {
  font-size: 0.85rem;
  color: var(--text-muted);
  font-weight: 700;
}

.product-table-wrap {
  border: 1px solid var(--border-color);
  border-radius: 12px;
  overflow: hidden;
}

.product-table-wrap table {
  width: 100%;
  border-collapse: collapse;
}

.product-table-wrap th,
.product-table-wrap td {
  padding: 10px;
  border-bottom: 1px solid var(--border-color);
  text-align: left;
  font-weight: 700;
}

.product-table-wrap th {
  background: var(--surface-weak);
  font-weight: 900;
}

.product-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.thumb {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  overflow: hidden;
  background: var(--surface-weak);
  border: 1px solid var(--border-color);
  flex-shrink: 0;
}

.thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.numeric {
  text-align: right;
}

.table-input {
  width: 120px;
}

.table-hint {
  margin: 0;
  padding: 8px 12px;
  color: var(--text-muted);
  font-weight: 700;
}

.preview {
  border: 1px solid var(--border-color);
  border-radius: 12px;
  overflow: hidden;
  background: var(--surface-weak);
}

.preview img {
  width: 100%;
  height: 140px;
  object-fit: cover;
  display: block;
}

.checkbox {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-weight: 700;
  color: var(--text-strong);
}

.error {
  margin: 0;
  color: #ef4444;
  font-weight: 700;
}

.actions {
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: center;
  gap: 10px;
}

.action-buttons {
  display: flex;
  gap: 8px;
}

.btn {
  border-radius: 999px;
  padding: 10px 18px;
  font-weight: 900;
  border: 1px solid var(--border-color);
  background: var(--surface);
  color: var(--text-strong);
  cursor: pointer;
}

.btn.ghost {
  color: var(--text-muted);
}

.btn.primary {
  border-color: var(--primary-color);
  color: var(--primary-color);
}

@media (max-width: 720px) {
  .field-grid {
    grid-template-columns: 1fr;
  }

  .actions {
    grid-template-columns: 1fr;
  }

  .action-buttons {
    justify-content: flex-start;
    flex-wrap: wrap;
  }
}
</style>
