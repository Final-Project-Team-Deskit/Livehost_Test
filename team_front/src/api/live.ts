import { http } from './http'

export type ApiResult<T> = {
  success: boolean
  data: T
  error?: { code?: string; message?: string }
}

export type BroadcastListResponse = {
  broadcastId: number
  title: string
  notice?: string | null
  sellerName?: string | null
  categoryName?: string | null
  thumbnailUrl?: string | null
  status?: string | null
  startAt?: string | null
  endAt?: string | null
  viewerCount?: number | null
  liveViewerCount?: number | null
  reportCount?: number | null
  totalSales?: number | null
  totalLikes?: number | null
  isPublic?: boolean | null
  adminLock?: boolean | null
  products?: Array<{ name: string; stock: number; soldOut?: boolean; isSoldOut?: boolean }>
}

export type BroadcastAllResponse = {
  onAir: BroadcastListResponse[]
  reserved: BroadcastListResponse[]
  vod: BroadcastListResponse[]
}

export type BroadcastProductResponse = {
  bpId: number
  productId: number
  name: string
  imageUrl?: string | null
  originalPrice: number
  bpPrice: number
  bpQuantity: number
  displayOrder?: number
  isPinned?: boolean
  status?: string | null
}

export type BroadcastResponse = {
  broadcastId: number
  sellerId?: number
  sellerName?: string | null
  sellerProfileUrl?: string | null
  title: string
  notice?: string | null
  status?: string | null
  layout?: string | null
  categoryName?: string | null
  categoryId?: number | null
  scheduledAt?: string | null
  startedAt?: string | null
  thumbnailUrl?: string | null
  waitScreenUrl?: string | null
  streamKey?: string | null
  vodUrl?: string | null
  totalViews?: number | null
  totalLikes?: number | null
  totalReports?: number | null
  products?: BroadcastProductResponse[]
  qcards?: Array<{ qcardId: number; sortOrder?: number; question?: string }>
}

export type BroadcastStatsResponse = {
  viewerCount: number
  likeCount: number
  reportCount: number
}

export type StatisticsResponse = {
  salesChart: Array<{ label: string; value: number }>
  arpuChart: Array<{ label: string; value: number }>
  bestBroadcasts: Array<{ broadcastId: number; title: string; totalSales: number; totalViews: number }>
  worstBroadcasts: Array<{ broadcastId: number; title: string; totalSales: number; totalViews: number }>
  topViewerBroadcasts?: Array<{ broadcastId: number; title: string; totalSales: number; totalViews: number }>
}

export type BroadcastResultResponse = {
  broadcastId: number
  title: string
  startAt?: string | null
  endAt?: string | null
  durationMinutes?: number
  status?: string | null
  stoppedReason?: string | null
  totalViews?: number
  totalLikes?: number
  totalSales?: number
  totalChats?: number
  maxViewers?: number
  maxViewerTime?: string | null
  avgWatchTime?: number
  reportCount?: number
  sanctionCount?: number
  vodUrl?: string | null
  vodStatus?: string | null
  isEncoding?: boolean
  productStats?: Array<{
    productId: number
    productName: string
    imageUrl?: string | null
    price: number
    salesQuantity: number
    salesAmount: number
  }>
}

export type CategoryResponse = {
  categoryId: number
  categoryName: string
}

export type ReservationSlotResponse = {
  slotDateTime: string
  remainingCapacity: number
  selectable: boolean
}

export type ProductSelectResponse = {
  productId: number
  productName: string
  price: number
  stockQty: number
  imageUrl?: string | null
}

export type ImageUploadResponse = {
  originalFileName: string
  storedFileName: string
  fileUrl: string
  fileSize: number
}

export type BroadcastCreateRequest = {
  title: string
  notice?: string | null
  categoryId: number
  scheduledAt: string
  thumbnailUrl: string
  waitScreenUrl?: string | null
  broadcastLayout: 'FULL' | 'LAYOUT_4' | 'LAYOUT_3' | 'LAYOUT_2'
  products: Array<{ productId: number; bpPrice: number; bpQuantity: number }>
  qcards?: Array<{ question: string }>
}

export type SanctionStatisticsResponse = {
  forceStopChart: Array<{ label: string; count: number }>
  viewerBanChart: Array<{ label: string; count: number }>
  worstSellers: Array<{ sellerId: number; sellerName: string; phone: string; sanctionCount: number }>
  worstViewers: Array<{ viewerId: string; name: string; sanctionCount: number }>
}

const unwrap = <T>(payload: ApiResult<T>): T => payload.data

export const listPublicBroadcasts = async (params?: Record<string, unknown>) => {
  const response = await http.get<ApiResult<BroadcastAllResponse | { content: BroadcastListResponse[] }>>("/api/broadcasts", {
    params,
  })
  return unwrap(response.data)
}

export const getPublicBroadcastDetail = async (broadcastId: number) => {
  const response = await http.get<ApiResult<BroadcastResponse>>(`/api/broadcasts/${broadcastId}`)
  return unwrap(response.data)
}

export const listPublicBroadcastProducts = async (broadcastId: number) => {
  const response = await http.get<ApiResult<BroadcastProductResponse[]>>(`/api/broadcasts/${broadcastId}/products`)
  return unwrap(response.data)
}

export const getPublicBroadcastStats = async (broadcastId: number) => {
  const response = await http.get<ApiResult<BroadcastStatsResponse>>(`/api/broadcasts/${broadcastId}/stats`)
  return unwrap(response.data)
}

export const listSellerBroadcasts = async (sellerId: number, params?: Record<string, unknown>) => {
  const response = await http.get<ApiResult<BroadcastAllResponse | { content: BroadcastListResponse[] }>>(
    '/seller/api/broadcasts',
    {
      params,
      headers: { 'X-Seller-Id': String(sellerId) },
    },
  )
  return unwrap(response.data)
}

export const createSellerBroadcast = async (sellerId: number, payload: BroadcastCreateRequest) => {
  const response = await http.post<ApiResult<number>>('/seller/api/broadcasts', payload, {
    headers: { 'X-Seller-Id': String(sellerId) },
  })
  return unwrap(response.data)
}

export const updateSellerBroadcast = async (sellerId: number, broadcastId: number, payload: BroadcastCreateRequest) => {
  const response = await http.put<ApiResult<number>>(`/seller/api/broadcasts/${broadcastId}`, payload, {
    headers: { 'X-Seller-Id': String(sellerId) },
  })
  return unwrap(response.data)
}

export const getSellerBroadcastDetail = async (sellerId: number, broadcastId: number) => {
  const response = await http.get<ApiResult<BroadcastResponse>>(`/seller/api/broadcasts/${broadcastId}`, {
    headers: { 'X-Seller-Id': String(sellerId) },
  })
  return unwrap(response.data)
}

export const listSellerProducts = async (sellerId: number, keyword?: string) => {
  const response = await http.get<ApiResult<ProductSelectResponse[]>>('/seller/api/broadcasts/products', {
    params: keyword ? { keyword } : undefined,
    headers: { 'X-Seller-Id': String(sellerId) },
  })
  return unwrap(response.data)
}

export const listReservationSlots = async (sellerId: number, date: string) => {
  const response = await http.get<ApiResult<ReservationSlotResponse[]>>('/seller/api/broadcasts/reservation-slots', {
    params: { date },
    headers: { 'X-Seller-Id': String(sellerId) },
  })
  return unwrap(response.data)
}

export const cancelSellerBroadcast = async (sellerId: number, broadcastId: number) => {
  const response = await http.delete<ApiResult<void>>(`/seller/api/broadcasts/${broadcastId}`, {
    headers: { 'X-Seller-Id': String(sellerId) },
  })
  return unwrap(response.data)
}

export const endSellerBroadcast = async (sellerId: number, broadcastId: number) => {
  const response = await http.post<ApiResult<void>>(`/seller/api/broadcasts/${broadcastId}/end`, null, {
    headers: { 'X-Seller-Id': String(sellerId) },
  })
  return unwrap(response.data)
}

export const pinSellerProduct = async (sellerId: number, broadcastId: number, productId: number) => {
  const response = await http.post<ApiResult<void>>(`/seller/api/broadcasts/${broadcastId}/pin/${productId}`, null, {
    headers: { 'X-Seller-Id': String(sellerId) },
  })
  return unwrap(response.data)
}

export const getSellerStatistics = async (sellerId: number, period: string) => {
  const response = await http.get<ApiResult<StatisticsResponse>>('/seller/api/broadcasts/statistics', {
    params: { period },
    headers: { 'X-Seller-Id': String(sellerId) },
  })
  return unwrap(response.data)
}

export const listAdminBroadcasts = async (params?: Record<string, unknown>) => {
  const response = await http.get<ApiResult<{ content: BroadcastListResponse[] }>>('/admin/api/broadcasts', {
    params,
  })
  return unwrap(response.data)
}

export const stopAdminBroadcast = async (broadcastId: number, reason: string) => {
  const response = await http.put<ApiResult<void>>(`/admin/api/broadcasts/${broadcastId}/stop`, { reason })
  return unwrap(response.data)
}

export const cancelAdminBroadcast = async (broadcastId: number, reason: string) => {
  const response = await http.put<ApiResult<void>>(`/admin/api/broadcasts/${broadcastId}/cancel`, { reason })
  return unwrap(response.data)
}

export const getAdminStatistics = async (period: string) => {
  const response = await http.get<ApiResult<StatisticsResponse>>('/admin/api/statistics', {
    params: { period },
  })
  return unwrap(response.data)
}

export const getAdminSanctionStatistics = async (period: string) => {
  const response = await http.get<ApiResult<SanctionStatisticsResponse>>('/admin/api/sanctions/statistics', {
    params: { period },
  })
  return unwrap(response.data)
}

export const getAdminBroadcastReport = async (broadcastId: number) => {
  const response = await http.get<ApiResult<BroadcastResultResponse>>(`/admin/api/${broadcastId}/report`)
  return unwrap(response.data)
}

export const getSellerBroadcastReport = async (sellerId: number, broadcastId: number) => {
  const response = await http.get<ApiResult<BroadcastResultResponse>>(`/seller/api/broadcasts/${broadcastId}/report`, {
    headers: { 'X-Seller-Id': String(sellerId) },
  })
  return unwrap(response.data)
}

export const listCategories = async () => {
  const response = await http.get<ApiResult<CategoryResponse[]>>('/api/categories')
  return unwrap(response.data)
}

export const uploadSellerImage = async (sellerId: number, type: 'THUMBNAIL' | 'WAIT_SCREEN', file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  const response = await http.post<ApiResult<ImageUploadResponse>>(`/seller/api/uploads/${type}`, formData, {
    headers: { 'X-Seller-Id': String(sellerId) },
  })
  return unwrap(response.data)
}
