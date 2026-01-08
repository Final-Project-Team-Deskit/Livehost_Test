<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PageContainer from '../../components/PageContainer.vue'
import PageHeader from '../../components/PageHeader.vue'
import {
  DRAFT_KEY,
  buildDraftFromReservation,
  createEmptyDraft,
  loadDraft,
  saveDraft,
  type LiveCreateDraft,
  type LiveCreateProduct,
} from '../../composables/useLiveCreateDraft'
import {
  createSellerBroadcast,
  listCategories,
  listReservationSlots,
  listSellerProducts,
  updateSellerBroadcast,
  uploadSellerImage,
  type BroadcastCreateRequest,
  type CategoryResponse,
  type ReservationSlotResponse,
} from '../../api/live'
import { resolveSellerId } from '../../lib/live/ids'
import { applyImageFallback } from '../../lib/live/image'

const router = useRouter()
const route = useRoute()

const draft = ref<LiveCreateDraft>(createEmptyDraft())
const productSearch = ref('')
const thumbError = ref('')
const standbyError = ref('')
const error = ref('')
const showTermsModal = ref(false)
const showProductModal = ref(false)
const modalProducts = ref<LiveCreateProduct[]>([])
const categories = ref<CategoryResponse[]>([])
const isLoadingCategories = ref(false)
const availableProducts = ref<LiveCreateProduct[]>([])
const reservationSlots = ref<ReservationSlotResponse[]>([])
const isLoadingSlots = ref(false)
const minDate = ref('')
const maxDate = ref('')

type CropState = {
  file: File | null
  url: string
  imageWidth: number
  imageHeight: number
  frameWidth: number
  frameHeight: number
  fitScale: number
  userScale: number
  offsetX: number
  offsetY: number
  dragging: boolean
  dragStartX: number
  dragStartY: number
  pointerId: number | null
}

const createCropState = (): CropState => ({
  file: null,
  url: '',
  imageWidth: 0,
  imageHeight: 0,
  frameWidth: 0,
  frameHeight: 0,
  fitScale: 1,
  userScale: 1,
  offsetX: 0,
  offsetY: 0,
  dragging: false,
  dragStartX: 0,
  dragStartY: 0,
  pointerId: null,
})

const thumbCrop = reactive(createCropState())
const standbyCrop = reactive(createCropState())
const thumbFrameRef = ref<HTMLElement | null>(null)
const standbyFrameRef = ref<HTMLElement | null>(null)

const reservationId = computed(() => {
  const queryValue = route.query.reservationId
  if (Array.isArray(queryValue)) return queryValue[0] ?? ''
  return typeof queryValue === 'string' ? queryValue : ''
})
const isEditMode = computed(() => route.query.mode === 'edit' && !!reservationId.value)
const modalCount = computed(() => modalProducts.value.length)

const computeDateRange = () => {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const start = new Date(today)
  start.setDate(today.getDate() + 1)
  const end = new Date(today)
  end.setDate(today.getDate() + 14)
  const toInput = (date: Date) =>
    `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
  minDate.value = toInput(start)
  maxDate.value = toInput(end)
}

const loadCategories = async () => {
  isLoadingCategories.value = true
  try {
    categories.value = await listCategories()
  } catch (loadError) {
    console.error('Failed to load categories', loadError)
    categories.value = []
  } finally {
    isLoadingCategories.value = false
  }
}

const loadSellerProducts = async (keyword?: string) => {
  const sellerId = resolveSellerId()
  if (!sellerId) {
    availableProducts.value = []
    return
  }
  try {
    const data = await listSellerProducts(sellerId, keyword)
    const mapped = data
      .filter((item) => (item.stockQty ?? 0) > 0)
      .map<LiveCreateProduct>((item) => ({
        id: String(item.productId),
        name: item.productName,
        option: item.productName,
        price: item.price,
        broadcastPrice: item.price,
        stock: item.stockQty,
        quantity: 1,
        thumb: item.imageUrl ?? undefined,
      }))
    availableProducts.value = mapped
  } catch (loadError) {
    console.error('Failed to load seller products', loadError)
    availableProducts.value = []
  }
}

const normalizeSlotTime = (slotDateTime: string) => {
  const [datePart, timePart] = slotDateTime.replace('T', ' ').split(' ')
  const time = timePart?.slice(0, 5) ?? ''
  return { datePart, time }
}

const loadReservationSlots = async (date: string) => {
  const sellerId = resolveSellerId()
  if (!sellerId || !date) {
    reservationSlots.value = []
    return
  }
  isLoadingSlots.value = true
  try {
    const slots = await listReservationSlots(sellerId, date)
    reservationSlots.value = slots.filter((slot) => slot.selectable)
  } catch (loadError) {
    console.error('Failed to load reservation slots', loadError)
    reservationSlots.value = []
  } finally {
    isLoadingSlots.value = false
  }
}

const syncCategoryFromName = () => {
  if (draft.value.categoryId || !draft.value.categoryName || !categories.value.length) return
  const match = categories.value.find((item) => item.categoryName === draft.value.categoryName)
  if (match) {
    draft.value.categoryId = match.categoryId
  }
}

const syncCategoryFromId = () => {
  const match = categories.value.find((item) => item.categoryId === draft.value.categoryId)
  draft.value.categoryName = match?.categoryName ?? ''
}

const filteredProducts = computed(() => {
  const q = productSearch.value.trim().toLowerCase()
  if (!q) return availableProducts.value
  return availableProducts.value.filter((product) => {
    const name = product.name.toLowerCase()
    const option = product.option.toLowerCase()
    return name.includes(q) || option.includes(q)
  })
})

const isSelected = (productId: string, source: LiveCreateProduct[] = draft.value.products) =>
  source.some((item) => item.id === productId)

const addProduct = (product: LiveCreateProduct, target: LiveCreateProduct[]) => {
  if (!isSelected(product.id, target) && target.length >= 10) {
    error.value = '상품은 최대 10개까지 등록할 수 있습니다.'
    return target
  }
  if (isSelected(product.id, target)) return target
  return [...target, { ...product }]
}

const removeProduct = (productId: string, target: LiveCreateProduct[]) => target.filter((item) => item.id !== productId)

const toggleProductInModal = (product: LiveCreateProduct) => {
  modalProducts.value = isSelected(product.id, modalProducts.value)
    ? removeProduct(product.id, modalProducts.value)
    : addProduct(product, modalProducts.value)
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
  const trimmedQuestions = draft.value.questions.map((q) => ({ ...q, text: q.text.trim() })).filter((q) => q.text.length > 0)
  const shouldUpdateQuestions =
    trimmedQuestions.length !== draft.value.questions.length ||
    trimmedQuestions.some((item, index) => item.text !== draft.value.questions[index]?.text)

  if (shouldUpdateQuestions) {
    draft.value.questions = trimmedQuestions
  }

  saveDraft({
    ...draft.value,
    title: draft.value.title.trim(),
    subtitle: draft.value.subtitle?.trim() ?? '',
    categoryId: draft.value.categoryId,
    categoryName: draft.value.categoryName.trim(),
    notice: draft.value.notice.trim(),
    questions: trimmedQuestions,
    reservationId: reservationId.value || draft.value.reservationId,
  })
}

const restoreDraft = async () => {
  const savedDraft = loadDraft()
  const baseDraft = savedDraft && (!isEditMode.value || savedDraft.reservationId === reservationId.value)
    ? { ...createEmptyDraft(), ...savedDraft }
    : createEmptyDraft()

  const reservationDraft = isEditMode.value
    ? { ...baseDraft, ...(await buildDraftFromReservation(reservationId.value)), reservationId: reservationId.value }
    : baseDraft

  draft.value = reservationDraft
  modalProducts.value = reservationDraft.products.map((p) => ({ ...p }))
  syncCategoryFromName()
}

const updateCropFrame = (state: CropState, frameRef: HTMLElement | null) => {
  if (!frameRef) return
  const rect = frameRef.getBoundingClientRect()
  state.frameWidth = rect.width
  state.frameHeight = rect.height
}

const prepareCropState = (state: CropState, file: File, frameRef: HTMLElement | null) => {
  const reader = new FileReader()
  reader.onload = () => {
    const result = typeof reader.result === 'string' ? reader.result : ''
    if (!result) return
    state.url = result
    state.file = file
    state.userScale = 1
    state.offsetX = 0
    state.offsetY = 0
    updateCropFrame(state, frameRef)
    const image = new Image()
    image.onload = () => {
      state.imageWidth = image.width
      state.imageHeight = image.height
      if (state.frameWidth && state.frameHeight) {
        const scaleX = state.frameWidth / state.imageWidth
        const scaleY = state.frameHeight / state.imageHeight
        state.fitScale = Math.min(scaleX, scaleY)
      } else {
        state.fitScale = 1
      }
    }
    image.src = result
  }
  reader.readAsDataURL(file)
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
  prepareCropState(thumbCrop, file, thumbFrameRef.value)
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
  prepareCropState(standbyCrop, file, standbyFrameRef.value)
}

const handleCropPointerDown = (event: PointerEvent, state: CropState) => {
  if (!state.url) return
  state.dragging = true
  state.pointerId = event.pointerId
  state.dragStartX = event.clientX - state.offsetX
  state.dragStartY = event.clientY - state.offsetY
}

const handleCropPointerMove = (event: PointerEvent, state: CropState) => {
  if (!state.dragging || state.pointerId !== event.pointerId) return
  state.offsetX = event.clientX - state.dragStartX
  state.offsetY = event.clientY - state.dragStartY
}

const handleCropPointerUp = (event: PointerEvent, state: CropState) => {
  if (state.pointerId !== event.pointerId) return
  state.dragging = false
  state.pointerId = null
}

const dataUrlToFile = async (dataUrl: string, filename: string) => {
  const response = await fetch(dataUrl)
  const blob = await response.blob()
  return new File([blob], filename, { type: blob.type })
}

const applyCrop = async (state: CropState, type: 'THUMBNAIL' | 'WAIT_SCREEN') => {
  if (!state.url || !state.file) return
  updateCropFrame(state, type === 'THUMBNAIL' ? thumbFrameRef.value : standbyFrameRef.value)
  const frameWidth = state.frameWidth || 640
  const frameHeight = state.frameHeight || 360
  const canvas = document.createElement('canvas')
  canvas.width = 1280
  canvas.height = 720
  const ctx = canvas.getContext('2d')
  if (!ctx) return
  ctx.fillStyle = '#ffffff'
  ctx.fillRect(0, 0, canvas.width, canvas.height)
  const img = new Image()
  await new Promise<void>((resolve) => {
    img.onload = () => resolve()
    img.src = state.url
  })
  const scale = state.fitScale * state.userScale
  const ratioX = canvas.width / frameWidth
  const ratioY = canvas.height / frameHeight
  const drawWidth = img.width * scale * ratioX
  const drawHeight = img.height * scale * ratioY
  const centerX = canvas.width / 2 + state.offsetX * ratioX
  const centerY = canvas.height / 2 + state.offsetY * ratioY
  ctx.drawImage(img, centerX - drawWidth / 2, centerY - drawHeight / 2, drawWidth, drawHeight)
  const dataUrl = canvas.toDataURL('image/jpeg', 0.92)
  const sellerId = resolveSellerId()
  if (!sellerId) return
  const file = await dataUrlToFile(dataUrl, type === 'THUMBNAIL' ? 'thumbnail.jpg' : 'waitscreen.jpg')
  try {
    const response = await uploadSellerImage(sellerId, type, file)
    if (type === 'THUMBNAIL') {
      draft.value.thumb = response.fileUrl
    } else {
      draft.value.standbyThumb = response.fileUrl
    }
  } catch (uploadError) {
    console.error('Failed to upload image', uploadError)
    if (type === 'THUMBNAIL') {
      thumbError.value = '이미지 업로드에 실패했습니다.'
    } else {
      standbyError.value = '이미지 업로드에 실패했습니다.'
    }
  }
}

const cropImageStyle = (state: CropState) => ({
  transform: `translate(-50%, -50%) translate(${state.offsetX}px, ${state.offsetY}px) scale(${state.fitScale * state.userScale})`,
})

const extractErrorCode = (err: unknown) => {
  const payload = (err as any)?.response?.data
  return payload?.error?.code ?? null
}

const submit = async () => {
  error.value = ''
  thumbError.value = ''
  standbyError.value = ''

  const trimmedQuestions = draft.value.questions.map((q) => ({ ...q, text: q.text.trim() })).filter((q) => q.text.length > 0)
  draft.value.questions = trimmedQuestions

  if (!draft.value.title.trim() || !draft.value.categoryId || !draft.value.date || !draft.value.time) {
    error.value = '방송 제목, 카테고리, 일정을 입력해주세요.'
    return
  }

  if (!draft.value.thumb) {
    thumbError.value = '방송 썸네일을 업로드해주세요.'
    return
  }

  const slotTimes = timeOptions.value
  if (slotTimes.length && !slotTimes.includes(draft.value.time)) {
    error.value = '선택한 시간은 예약 가능한 시간이 아닙니다.'
    await loadReservationSlots(draft.value.date)
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

  const confirmed = window.confirm(isEditMode.value ? '예약 수정을 진행할까요?' : '방송 등록을 진행할까요?')
  if (!confirmed) return

  const sellerId = resolveSellerId()
  if (!sellerId) return
  const scheduledAt = `${draft.value.date} ${draft.value.time}:00`
  const payload: BroadcastCreateRequest = {
    title: draft.value.title.trim(),
    notice: draft.value.notice.trim(),
    categoryId: Number(draft.value.categoryId),
    scheduledAt,
    thumbnailUrl: draft.value.thumb,
    waitScreenUrl: draft.value.standbyThumb || null,
    broadcastLayout: 'FULL',
    products: draft.value.products.map((product) => ({
      productId: Number.parseInt(product.id, 10),
      bpPrice: product.broadcastPrice,
      bpQuantity: product.quantity,
    })),
    qcards: trimmedQuestions.map((q) => ({ question: q.text })),
  }

  try {
    const broadcastId = isEditMode.value && reservationId.value
      ? await updateSellerBroadcast(sellerId, Number(reservationId.value), payload)
      : await createSellerBroadcast(sellerId, payload)
    localStorage.removeItem(DRAFT_KEY)
    alert(isEditMode.value ? '예약 수정이 완료되었습니다.' : '방송 등록이 완료되었습니다.')
    const redirectPath = `/seller/broadcasts/reservations/${broadcastId}`
    router.push(redirectPath).catch(() => {})
  } catch (submitError) {
    const code = extractErrorCode(submitError)
    if (code === 'B005') {
      error.value = '해당 시간대 예약이 마감되었습니다. 다시 시간을 선택해주세요.'
      alert('해당 시간대 예약이 마감되었습니다. 다시 시간을 선택해주세요.')
      await loadReservationSlots(draft.value.date)
      return
    }
    if (code === 'B004') {
      error.value = '예약은 최대 7개까지만 가능합니다.'
      return
    }
    if (code === 'SY001') {
      error.value = '요청이 많습니다. 잠시 후 다시 시도해주세요.'
      return
    }
    error.value = '방송 등록에 실패했습니다. 다시 시도해주세요.'
  }
}

const goPrev = () => {
  router.push({ path: '/seller/live/create', query: route.query }).catch(() => {})
}

const cancel = () => {
  const ok = window.confirm('작성 중인 내용을 취소하시겠어요?')
  if (!ok) return
  const redirect = isEditMode.value && reservationId.value
    ? `/seller/broadcasts/reservations/${reservationId.value}`
    : '/seller/live?tab=scheduled'
  router.push(redirect).catch(() => {})
}

const openProductModal = () => {
  if (!availableProducts.value.length) {
    loadSellerProducts()
  }
  modalProducts.value = draft.value.products.map((p) => ({ ...p }))
  productSearch.value = ''
  showProductModal.value = true
}

const cancelProductSelection = () => {
  modalProducts.value = draft.value.products.map((p) => ({ ...p }))
  showProductModal.value = false
}

const saveProductSelection = () => {
  draft.value.products = modalProducts.value.map((p) => ({ ...p }))
  showProductModal.value = false
  alert('상품 선택이 저장되었습니다.')
}

const confirmRemoveProduct = (productId: string) => {
  const ok = window.confirm('이 상품을 리스트에서 제거하시겠어요?')
  if (ok) {
    draft.value.products = removeProduct(productId, draft.value.products)
  }
}

const timeOptions = computed(() => {
  if (!draft.value.date) return []
  return reservationSlots.value
    .map((slot) => normalizeSlotTime(slot.slotDateTime))
    .filter((slot) => slot.datePart === draft.value.date)
    .map((slot) => slot.time)
    .filter(Boolean)
})

onMounted(() => {
  computeDateRange()
  loadCategories().then(() => {
    syncCategoryFromName()
  })
})

watch(
  () => [isEditMode.value, reservationId.value],
  () => {
    restoreDraft()
  },
  { immediate: true },
)

watch(
  () => draft.value.categoryId,
  () => {
    syncCategoryFromId()
  },
)

watch(
  categories,
  () => {
    syncCategoryFromId()
  },
  { deep: true },
)

watch(
  () => draft.value.date,
  (date) => {
    if (!date) return
    loadReservationSlots(date)
  },
)

watch(timeOptions, (options) => {
  if (draft.value.time && !options.includes(draft.value.time)) {
    draft.value.time = ''
  }
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
        <span class="step-indicator">2 / 2 단계</span>
        <button type="button" class="btn ghost" @click="goPrev">이전</button>
      </div>
      <label class="field">
        <span class="field__label">방송 제목</span>
        <input v-model="draft.title" type="text" maxlength="30" placeholder="예: 홈오피스 라이브" />
        <span class="field__hint">{{ draft.title.length }}/30</span>
      </label>
      <div class="field-grid">
        <label class="field">
          <span class="field__label">카테고리</span>
          <select v-model="draft.categoryId" :disabled="isLoadingCategories">
            <option value="" disabled>카테고리를 선택하세요</option>
            <option v-for="category in categories" :key="category.categoryId" :value="category.categoryId">
              {{ category.categoryName }}
            </option>
          </select>
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
          <input v-model="draft.date" type="date" :min="minDate" :max="maxDate" />
        </label>
        <label class="field">
          <span class="field__label">방송 시간</span>
          <select v-model="draft.time" :disabled="isLoadingSlots || !draft.date">
            <option value="" disabled>시간을 선택하세요</option>
            <option v-for="time in timeOptions" :key="time" :value="time">{{ time }}</option>
          </select>
        </label>
      </div>
      <div class="section-block">
        <div class="section-head">
          <h3>판매 상품 등록</h3>
          <span class="count-pill">선택 {{ draft.products.length }}개</span>
        </div>
        <div class="product-search-bar">
          <button type="button" class="btn" @click="openProductModal">상품 선택</button>
          <span class="search-hint">최소 1개, 최대 10개 선택</span>
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
                      <img :src="product.thumb" :alt="product.name" @error="(event) => applyImageFallback(event, '/placeholder-product.jpg')" />
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
                  <button type="button" class="btn ghost" @click="confirmRemoveProduct(product.id)">
                  제거
                </button>
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
            <div v-if="thumbCrop.url" class="cropper">
              <div
                ref="thumbFrameRef"
                class="cropper-frame"
                @pointerdown="(event) => handleCropPointerDown(event, thumbCrop)"
                @pointermove="(event) => handleCropPointerMove(event, thumbCrop)"
                @pointerup="(event) => handleCropPointerUp(event, thumbCrop)"
                @pointerleave="(event) => handleCropPointerUp(event, thumbCrop)"
              >
                <img
                  class="cropper-image"
                  :src="thumbCrop.url"
                  alt="방송 썸네일 조정"
                  :style="cropImageStyle(thumbCrop)"
                />
              </div>
              <div class="cropper-controls">
                <input v-model.number="thumbCrop.userScale" type="range" min="1" max="3" step="0.05" />
                <button type="button" class="btn ghost" @click="applyCrop(thumbCrop, 'THUMBNAIL')">적용</button>
              </div>
            </div>
            <div v-else-if="draft.thumb" class="preview">
              <img :src="draft.thumb" alt="방송 썸네일 미리보기" @error="(event) => applyImageFallback(event, '/placeholder-live.jpg')" />
            </div>
          </label>
          <label class="field">
            <span class="field__label">대기화면 업로드</span>
            <input type="file" accept="image/*" @change="handleStandbyUpload" />
            <span v-if="standbyError" class="error">{{ standbyError }}</span>
            <div v-if="standbyCrop.url" class="cropper">
              <div
                ref="standbyFrameRef"
                class="cropper-frame"
                @pointerdown="(event) => handleCropPointerDown(event, standbyCrop)"
                @pointermove="(event) => handleCropPointerMove(event, standbyCrop)"
                @pointerup="(event) => handleCropPointerUp(event, standbyCrop)"
                @pointerleave="(event) => handleCropPointerUp(event, standbyCrop)"
              >
                <img
                  class="cropper-image"
                  :src="standbyCrop.url"
                  alt="대기화면 조정"
                  :style="cropImageStyle(standbyCrop)"
                />
              </div>
              <div class="cropper-controls">
                <input v-model.number="standbyCrop.userScale" type="range" min="1" max="3" step="0.05" />
                <button type="button" class="btn ghost" @click="applyCrop(standbyCrop, 'WAIT_SCREEN')">적용</button>
              </div>
            </div>
            <div v-else-if="draft.standbyThumb" class="preview">
              <img :src="draft.standbyThumb" alt="대기화면 미리보기" @error="(event) => applyImageFallback(event, '/placeholder-live.jpg')" />
            </div>
          </label>
        </div>
      </div>
      <div class="section-block">
        <label class="checkbox">
          <input v-model="draft.termsAgreed" type="checkbox" />
          <span>방송 운영 및 약관에 동의합니다. (필수)</span>
          <button type="button" class="link" @click="showTermsModal = true">자세히보기</button>
        </label>
      </div>
      <p v-if="error" class="error">{{ error }}</p>
      <div class="actions">
        <div class="action-buttons">
          <button type="button" class="btn" @click="cancel">취소</button>
          <button type="button" class="btn primary" @click="submit">{{ isEditMode ? '저장' : '방송 등록' }}</button>
        </div>
      </div>
      <teleport to="body">
        <div v-if="showProductModal" class="modal">
          <div class="modal__backdrop" @click="cancelProductSelection"></div>
          <div class="modal__content">
            <div class="modal__header">
              <h3>상품 선택</h3>
              <button type="button" class="btn ghost" aria-label="닫기" @click="cancelProductSelection">×</button>
            </div>
            <div class="modal__body">
              <div class="product-search-bar modal-search">
                <input v-model="productSearch" class="search-input__plain" type="text" placeholder="상품명을 검색하세요" />
                <span class="search-hint">체크박스로 선택 후 저장을 누르면 반영됩니다.</span>
              </div>
              <div class="product-grid">
                <label
                  v-for="product in filteredProducts"
                  :key="product.id"
                  class="product-card"
                  :class="{ checked: isSelected(product.id, modalProducts) }"
                >
                  <input
                    type="checkbox"
                    :checked="isSelected(product.id, modalProducts)"
                    @change="toggleProductInModal(product)"
                  />
                  <div class="product-thumb" v-if="product.thumb">
                    <img :src="product.thumb" :alt="product.name" @error="(event) => applyImageFallback(event, '/placeholder-product.jpg')" />
                  </div>
                  <div class="product-content">
                    <div class="product-name">{{ product.name }}</div>
                    <div class="product-meta">
                      <span>{{ product.option }}</span>
                      <span>정가 {{ product.price.toLocaleString() }}원</span>
                      <span>재고 {{ product.stock }}</span>
                    </div>
                  </div>
                </label>
              </div>
            </div>
            <div class="modal__footer">
              <span class="modal__count">선택 {{ modalCount }}개</span>
              <div class="modal__actions">
                <button type="button" class="btn ghost" @click="cancelProductSelection">취소</button>
                <button type="button" class="btn primary" @click="saveProductSelection">저장</button>
              </div>
            </div>
          </div>
        </div>
      </teleport>
      <div v-if="showTermsModal" class="modal">
        <div class="modal__content">
          <div class="modal__header">
            <h3>방송 운영 및 약관</h3>
            <button type="button" class="btn ghost" @click="showTermsModal = false">닫기</button>
          </div>
          <div class="modal__body">
            <p>방송 운영 시 상품 정보, 가격, 재고를 정확히 안내해야 하며 허위 광고가 금지됩니다.</p>
            <p>방송 중 욕설, 비방 등 운영 정책에 어긋나는 행위는 제재될 수 있습니다.</p>
            <p>취소 및 환불 정책을 명확히 안내하고, 방송 종료 후 문의에 신속히 응답해주세요.</p>
          </div>
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

.cropper {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 8px;
}

.cropper-frame {
  position: relative;
  width: 100%;
  max-width: 360px;
  aspect-ratio: 16 / 9;
  background: #ffffff;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  overflow: hidden;
  touch-action: none;
}

.cropper-image {
  position: absolute;
  top: 50%;
  left: 50%;
  transform-origin: center center;
  max-width: none;
  max-height: none;
  user-select: none;
  pointer-events: none;
}

.cropper-controls {
  display: flex;
  align-items: center;
  gap: 12px;
}

.step-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.step-indicator {
  color: var(--text-muted);
  font-weight: 800;
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
  display: none;
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
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.action-buttons {
  display: flex;
  gap: 8px;
}

.modal__body {
  max-height: 520px;
  overflow: auto;
}

.modal__footer {
  margin-top: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.modal__count {
  font-weight: 800;
  color: var(--text-strong);
}

.modal__actions {
  display: flex;
  gap: 8px;
  align-items: center;
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

.product-search-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.modal-search {
  align-items: flex-start;
  flex-direction: column;
}

.search-input {
  position: relative;
  flex: 1 1 320px;
}

.search-input__plain {
  width: 100%;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 10px 12px;
  font-weight: 700;
  color: var(--text-strong);
  background: var(--surface);
}

.search-input input {
  width: 100%;
  padding-left: 34px;
}

.search-icon {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
}

.search-hint {
  color: var(--text-muted);
  font-weight: 700;
  font-size: 0.9rem;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 12px;
}

.product-card {
  display: grid;
  grid-template-columns: auto 60px 1fr;
  gap: 12px;
  padding: 12px;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  background: var(--surface);
  align-items: center;
  cursor: pointer;
}

.product-card input {
  justify-self: center;
}

.product-card.checked {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 2px var(--primary-color-light, rgba(45, 127, 249, 0.2));
}

.product-thumb {
  width: 60px;
  height: 60px;
  border-radius: 10px;
  overflow: hidden;
  background: var(--surface-weak);
  border: 1px solid var(--border-color);
}

.product-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-content {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.product-name {
  font-weight: 900;
  color: var(--text-strong);
}

.product-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  color: var(--text-muted);
  font-weight: 700;
  font-size: 0.9rem;
}

.link {
  background: none;
  border: none;
  padding: 0;
  color: var(--primary-color);
  cursor: pointer;
  font-weight: 800;
  text-decoration: underline;
}

.modal {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1300;
  padding: 16px;
}

.modal__content {
  background: var(--surface);
  border-radius: 16px;
  padding: 18px;
  max-width: 520px;
  width: 100%;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.modal__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.modal__body {
  display: flex;
  flex-direction: column;
  gap: 8px;
  color: var(--text-strong);
  font-weight: 700;
  line-height: 1.5;
}

@media (max-width: 720px) {
  .field-grid {
    grid-template-columns: 1fr;
  }

  .product-card {
    grid-template-columns: auto 50px 1fr;
  }

  .product-grid {
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  }
}
</style>
