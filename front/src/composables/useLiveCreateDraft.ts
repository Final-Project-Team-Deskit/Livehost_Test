import { getSellerBroadcastDetail } from '../api/live'
import { resolveSellerId } from '../lib/live/ids'

export type LiveCreateProduct = {
  id: string
  name: string
  option: string
  price: number
  broadcastPrice: number
  stock: number
  quantity: number
  thumb?: string
}

export type LiveCreateDraft = {
  questions: Array<{ id: string; text: string }>
  title: string
  subtitle: string
  categoryId: number | ''
  categoryName: string
  notice: string
  date: string
  time: string
  thumb: string
  standbyThumb: string
  termsAgreed: boolean
  products: LiveCreateProduct[]
  reservationId?: string
}

export const DRAFT_KEY = 'deskit_seller_broadcast_draft_v2'

const createId = () => `q-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`

const createQuestion = (text: string) => ({ id: createId(), text })

const mapQuestions = (seeds: string[]) => (seeds.length ? seeds : ['']).map((text) => createQuestion(text))

export const createDefaultQuestions = () => mapQuestions([])

export const createEmptyDraft = (): LiveCreateDraft => ({
  questions: createDefaultQuestions(),
  title: '',
  subtitle: '',
  categoryId: '',
  categoryName: '',
  notice: '',
  date: '',
  time: '',
  thumb: '',
  standbyThumb: '',
  termsAgreed: false,
  products: [],
  reservationId: undefined,
})

export const loadDraft = (): LiveCreateDraft | null => {
  const raw = localStorage.getItem(DRAFT_KEY)
  if (!raw) return null
  try {
    const parsed = JSON.parse(raw)
    if (!parsed || typeof parsed !== 'object') return null

    const { cueTitle: _ignoredCueTitle, cueNotes: _ignoredCueNotes, ...rest } = parsed as Record<string, any>

    return {
      ...createEmptyDraft(),
      ...rest,
      questions: Array.isArray(rest.questions)
        ? rest.questions
            .filter((item: any) => item && typeof item.id === 'string' && typeof item.text === 'string')
            .map((item: any) => ({ id: item.id, text: item.text }))
        : createEmptyDraft().questions,
      categoryId: typeof rest.categoryId === 'number' ? rest.categoryId : '',
      categoryName:
        typeof rest.categoryName === 'string'
          ? rest.categoryName
          : typeof (rest as any).category === 'string'
            ? (rest as any).category
            : '',
      products: Array.isArray(rest.products)
        ? rest.products
            .filter((item: any) => item && typeof item.id === 'string')
            .map((item: any) => ({
              id: item.id,
              name: item.name ?? '',
              option: item.option ?? '',
              price: typeof item.price === 'number' ? item.price : 0,
              broadcastPrice: typeof item.broadcastPrice === 'number' ? item.broadcastPrice : 0,
              stock: typeof item.stock === 'number' ? item.stock : 0,
              quantity: typeof item.quantity === 'number' ? item.quantity : 1,
              thumb: item.thumb ?? '',
            }))
        : [],
    }
  } catch (e) {
    console.error('Failed to load draft', e)
    return null
  }
}

export const saveDraft = (draft: LiveCreateDraft) => {
  localStorage.setItem(DRAFT_KEY, JSON.stringify(draft))
}

export const buildDraftFromReservation = async (reservationId: string): Promise<LiveCreateDraft> => {
  const sellerId = resolveSellerId()
  if (!sellerId) {
    return createEmptyDraft()
  }
  const detail = await getSellerBroadcastDetail(sellerId, Number.parseInt(reservationId, 10))
  const [datePart, timePart] = (detail.scheduledAt ?? '').replace(/-/g, '.').split(' ')
  const mappedDate = datePart?.replace(/\./g, '-') ?? ''
  const mappedTime = timePart ?? ''

  return {
    ...createEmptyDraft(),
    title: detail.title ?? '',
    subtitle: detail.categoryName ?? '',
    categoryId: detail.categoryId ?? '',
    categoryName: detail.categoryName ?? '',
    notice: detail.notice ?? '',
    date: mappedDate,
    time: mappedTime,
    thumb: detail.thumbnailUrl ?? '',
    standbyThumb: detail.waitScreenUrl ?? '',
    products: (detail.products ?? []).map((item) => ({
      id: String(item.bpId ?? item.productId),
      name: item.name ?? '',
      option: item.name ?? '',
      price: item.originalPrice ?? 0,
      broadcastPrice: item.bpPrice ?? item.originalPrice ?? 0,
      stock: item.bpQuantity ?? 0,
      quantity: item.bpQuantity ?? 1,
      thumb: item.imageUrl ?? '',
    })),
    questions: mapQuestions((detail.qcards ?? []).map((card) => card.question ?? '').filter((q) => q)),
    reservationId,
  }
}
