import { http } from './http'
import { endpoints } from './endpoints'

export type BroadcastStatus = 'ON_AIR' | 'RESERVED' | 'ENDED' | 'VOD'
export type BroadcastTab = 'ALL' | 'LIVE' | 'RESERVED' | 'VOD'

export type BroadcastSearch = {
  tab?: BroadcastTab
  keyword?: string
  sortType?: 'LATEST' | 'POPULAR' | 'UPCOMING'
  categoryId?: number
  startDate?: string | Date
  endDate?: string | Date
  page?: number
  size?: number
}

export type BroadcastListItem = {
  broadcastId: number
  id?: number
  title: string
  status: BroadcastStatus
  startAt: string
  scheduledAt?: string
  startedAt?: string
  endedAt?: string
  viewerCount?: number
  totalViews?: number
  thumbnailUrl?: string
  description?: string
  sellerName?: string
}

export type BroadcastListResponse = {
  content: BroadcastListItem[]
  hasNext: boolean
  page: number
  size: number
}

export type BroadcastDetail = {
  broadcastId: number
  title: string
  status: BroadcastStatus
  description?: string
  thumbnailUrl?: string
  startAt: string
  scheduledAt?: string
  startedAt?: string
  endedAt?: string
  vodUrl?: string
  streamUrl?: string
  viewerCount?: number
  totalViews?: number
  sellerName?: string
  sellerProfileImageUrl?: string
  categoryName?: string
}

export type CreateBroadcastPayload = {
  title: string
  description?: string
  categoryId?: number
  scheduledAt?: string | Date
  startAt?: string | Date
  thumbnailUrl?: string
}

const formatDateTimeParam = (value?: string | Date) => {
  if (!value) return undefined
  if (typeof value === 'string') return value
  const yyyy = value.getFullYear()
  const MM = String(value.getMonth() + 1).padStart(2, '0')
  const dd = String(value.getDate()).padStart(2, '0')
  const HH = String(value.getHours()).padStart(2, '0')
  const mm = String(value.getMinutes()).padStart(2, '0')
  const ss = String(value.getSeconds()).padStart(2, '0')
  return `${yyyy}-${MM}-${dd} ${HH}:${mm}:${ss}`
}

const normalizeListItem = (item: BroadcastListItem): BroadcastListItem => ({
  ...item,
  id: item.id ?? item.broadcastId,
})

export const fetchBroadcasts = async (
  params: BroadcastSearch = {},
): Promise<BroadcastListResponse> => {
  const response = await http.get<BroadcastListResponse>(endpoints.broadcasts, {
    params: {
      ...params,
      startDate: formatDateTimeParam(params.startDate),
      endDate: formatDateTimeParam(params.endDate),
    },
  })
  return {
    ...response.data,
    content: (response.data.content ?? []).map(normalizeListItem),
  }
}

export const fetchBroadcastDetail = async (
  id: number | string,
): Promise<BroadcastDetail> => {
  const response = await http.get<BroadcastDetail>(endpoints.broadcastDetail(id))
  return response.data
}

export const createBroadcast = async (payload: CreateBroadcastPayload): Promise<BroadcastDetail> => {
  const body = {
    ...payload,
    scheduledAt: formatDateTimeParam(payload.scheduledAt),
    startAt: formatDateTimeParam(payload.startAt),
  }
  const response = await http.post<BroadcastDetail>(endpoints.sellerBroadcasts, body)
  return response.data
}

export const startBroadcast = async (broadcastId: number | string): Promise<void> => {
  await http.post(endpoints.sellerBroadcastStart(broadcastId))
}

export const fetchSellerBroadcasts = async (
  params: BroadcastSearch = {},
): Promise<BroadcastListResponse> => {
  const response = await http.get<BroadcastListResponse>(endpoints.sellerBroadcasts, {
    params: {
      ...params,
      startDate: formatDateTimeParam(params.startDate),
      endDate: formatDateTimeParam(params.endDate),
    },
  })
  return {
    ...response.data,
    content: (response.data.content ?? []).map(normalizeListItem),
  }
}
