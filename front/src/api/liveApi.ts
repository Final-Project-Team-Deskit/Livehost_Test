import { http } from './http'
import { endpoints } from './endpoints'

export type BroadcastStatus = 'ON_AIR' | 'RESERVED' | 'ENDED' | 'VOD'
export type BroadcastTab = 'ALL' | 'LIVE' | 'RESERVED' | 'VOD'

export type BroadcastSearch = {
  tab?: BroadcastTab
  keyword?: string
  sortType?: 'LATEST' | 'POPULAR' | 'UPCOMING'
  categoryId?: number
  startDate?: string
  endDate?: string
  page?: number
  size?: number
}

export type BroadcastListItem = {
  id: number
  title: string
  status: BroadcastStatus
  startAt: string
  scheduledAt?: string
  startedAt?: string
  viewerCount?: number
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
  id: number
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
  sellerName?: string
  sellerProfileImageUrl?: string
  categoryName?: string
}

export const fetchBroadcasts = async (
  params: BroadcastSearch = {},
): Promise<BroadcastListResponse> => {
  const response = await http.get<BroadcastListResponse>(endpoints.broadcasts, {
    params,
  })
  return response.data
}

export const fetchBroadcastDetail = async (
  id: number | string,
): Promise<BroadcastDetail> => {
  const response = await http.get<BroadcastDetail>(endpoints.broadcastDetail(id))
  return response.data
}
