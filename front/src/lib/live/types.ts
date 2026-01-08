export type LiveItem = {
  id: string
  title: string
  description: string
  thumbnailUrl: string
  startAt: string
  endAt: string
  viewerCount?: number
  vodUrl?: string
  streamUrl?: string
  sellerName?: string
}

export type LiveProductItem = {
  id: string
  name: string
  imageUrl: string
  price: number
  status: string
  isSoldOut: boolean
  isPinned?: boolean
}
