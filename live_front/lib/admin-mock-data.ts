export type BroadcastStatus = "RESERVED" | "CANCELED" | "READY" | "ON_AIR" | "ENDED" | "ENCODING" | "VOD" | "STOPED"
export type VodVisibility = "PUBLIC" | "PRIVATE" | null

export interface Broadcast {
  id: string
  title: string
  category: string
  thumbnailUrl: string
  status: BroadcastStatus
  vodVisibility: VodVisibility
  startAt: Date
  endAt: Date | null
  viewersCurrent: number
  viewersTotal: number
  reportCount: number
  likeCount: number
  revenueTotal: number
  sellerName: string
  stopReason?: string
  cancelReason?: string
  sanctionsViewerCount: number
  sanctionsSellerCount: number
}

export interface Product {
  id: string
  name: string
  price: number
  soldCount: number
  revenue: number
  imageUrl: string
}

import { normalizeBroadcastData } from "./utils"

const rawMockBroadcasts: Broadcast[] = [
  // ON_AIR broadcasts (10)
  ...Array.from({ length: 10 }, (_, i) => ({
    id: `live-${i + 1}`,
    title: `실시간 라이브 방송 ${i + 1}`,
    category: ["가구", "전자기기", "악세사리", "패션", "뷰티"][i % 5],
    thumbnailUrl: `/fashion-live-show.jpg`,
    status: "ON_AIR" as BroadcastStatus,
    vodVisibility: null,
    startAt: new Date(Date.now() - (i + 1) * 15 * 60 * 1000), // started 15-150 mins ago
    endAt: null,
    viewersCurrent: Math.floor(Math.random() * 2000) + 100,
    viewersTotal: Math.floor(Math.random() * 5000) + 500,
    reportCount: Math.floor(Math.random() * 50),
    likeCount: Math.floor(Math.random() * 1000) + 100,
    revenueTotal: Math.floor(Math.random() * 10000000) + 1000000,
    sellerName: `판매자${i + 1}`,
    sanctionsViewerCount: Math.floor(Math.random() * 5),
    sanctionsSellerCount: 0,
  })),

  // READY broadcasts (10)
  ...Array.from({ length: 10 }, (_, i) => ({
    id: `ready-${i + 1}`,
    title: `대기 중인 방송 ${i + 1}`,
    category: ["가구", "전자기기", "악세사리", "패션", "뷰티"][i % 5],
    thumbnailUrl: `/beauty-products-collection.png`,
    status: "READY" as BroadcastStatus,
    vodVisibility: null,
    startAt: new Date(Date.now() - (i + 1) * 5 * 60 * 1000),
    endAt: null,
    viewersCurrent: Math.floor(Math.random() * 100) + 10,
    viewersTotal: Math.floor(Math.random() * 200) + 50,
    reportCount: Math.floor(Math.random() * 20),
    likeCount: Math.floor(Math.random() * 100),
    revenueTotal: 0,
    sellerName: `판매자${i + 11}`,
    sanctionsViewerCount: Math.floor(Math.random() * 3),
    sanctionsSellerCount: 0,
  })),

  // RESERVED broadcasts (20)
  ...Array.from({ length: 20 }, (_, i) => ({
    id: `reserved-${i + 1}`,
    title: `예약된 방송 ${i + 1}`,
    category: ["가구", "전자기기", "악세사리", "패션", "뷰티"][i % 5],
    thumbnailUrl: `/scheduled-broadcast.jpg`,
    status: "RESERVED" as BroadcastStatus,
    vodVisibility: null,
    startAt: new Date(Date.now() + (i + 1) * 60 * 60 * 1000), // 1-20 hours from now
    endAt: null,
    viewersCurrent: 0,
    viewersTotal: 0,
    reportCount: 0,
    likeCount: 0,
    revenueTotal: 0,
    sellerName: `판매자${i + 21}`,
    sanctionsViewerCount: 0,
    sanctionsSellerCount: 0,
  })),

  // CANCELED broadcasts (5)
  ...Array.from({ length: 5 }, (_, i) => ({
    id: `canceled-${i + 1}`,
    title: `취소된 방송 ${i + 1}`,
    category: ["가구", "전자기기", "악세사리"][i % 3],
    thumbnailUrl: `/scheduled-broadcast.jpg`,
    status: "CANCELED" as BroadcastStatus,
    vodVisibility: null,
    startAt: new Date(Date.now() - (i + 1) * 24 * 60 * 60 * 1000),
    endAt: null,
    viewersCurrent: 0,
    viewersTotal: 0,
    reportCount: 0,
    likeCount: 0,
    revenueTotal: 0,
    sellerName: `판매자${i + 41}`,
    cancelReason: i === 4 ? "기타: 개인 사정으로 취소" : "판매자 요청",
    sanctionsViewerCount: 0,
    sanctionsSellerCount: 0,
  })),

  // ENDED broadcasts (5) - just finished, still part of live flow
  ...Array.from({ length: 5 }, (_, i) => ({
    id: `ended-${i + 1}`,
    title: `종료된 방송 ${i + 1}`,
    category: ["가구", "전자기기", "악세사리", "패션", "뷰티"][i % 5],
    thumbnailUrl: `/beauty-products-collection.png`,
    status: "ENDED" as BroadcastStatus,
    vodVisibility: null,
    startAt: new Date(Date.now() - (i + 1) * 120 * 60 * 1000), // ended 2-10 hours ago
    endAt: new Date(Date.now() - (i + 1) * 60 * 60 * 1000),
    viewersCurrent: 0,
    viewersTotal: Math.floor(Math.random() * 1500) + 500,
    reportCount: Math.floor(Math.random() * 25),
    likeCount: Math.floor(Math.random() * 300) + 50,
    revenueTotal: Math.floor(Math.random() * 5000000) + 1000000,
    sellerName: `판매자${i + 66}`,
    sanctionsViewerCount: Math.floor(Math.random() * 4),
    sanctionsSellerCount: 0,
  })),

  // VOD/ENDED with PUBLIC visibility (10)
  ...Array.from({ length: 10 }, (_, i) => ({
    id: `vod-public-${i + 1}`,
    title: `VOD 공개 방송 ${i + 1}`,
    category: ["가구", "전자기기", "악세사리", "패션", "뷰티"][i % 5],
    thumbnailUrl: `/vod-replay.jpg`,
    status: "VOD" as BroadcastStatus,
    vodVisibility: "PUBLIC" as VodVisibility,
    startAt: new Date(Date.now() - (i + 10) * 24 * 60 * 60 * 1000), // 10-19 days ago
    endAt: new Date(Date.now() - (i + 9) * 24 * 60 * 60 * 1000),
    viewersCurrent: 0,
    viewersTotal: Math.floor(Math.random() * 3000) + 500,
    reportCount: Math.floor(Math.random() * 30),
    likeCount: Math.floor(Math.random() * 800) + 100,
    revenueTotal: Math.floor(Math.random() * 8000000) + 2000000,
    sellerName: `판매자${i + 46}`,
    sanctionsViewerCount: Math.floor(Math.random() * 8),
    sanctionsSellerCount: 0,
  })),

  // VOD with PRIVATE visibility (5)
  ...Array.from({ length: 5 }, (_, i) => ({
    id: `vod-private-${i + 1}`,
    title: `VOD 비공개 방송 ${i + 1}`,
    category: ["가구", "전자기기"][i % 2],
    thumbnailUrl: `/vod-replay.jpg`,
    status: "VOD" as BroadcastStatus,
    vodVisibility: "PRIVATE" as VodVisibility,
    startAt: new Date(Date.now() - (i + 5) * 24 * 60 * 60 * 1000),
    endAt: new Date(Date.now() - (i + 4) * 24 * 60 * 60 * 1000),
    viewersCurrent: 0,
    viewersTotal: Math.floor(Math.random() * 1000) + 200,
    reportCount: Math.floor(Math.random() * 15),
    likeCount: Math.floor(Math.random() * 200),
    revenueTotal: Math.floor(Math.random() * 5000000) + 1000000,
    sellerName: `판매자${i + 56}`,
    sanctionsViewerCount: Math.floor(Math.random() * 3),
    sanctionsSellerCount: 0,
  })),

  // ENCODING broadcasts (8) - VOD being processed
  ...Array.from({ length: 8 }, (_, i) => ({
    id: `encoding-${i + 1}`,
    title: `인코딩 중인 방송 ${i + 1}`,
    category: ["가구", "전자기기", "악세사리", "패션"][i % 4],
    thumbnailUrl: `/vod-replay.jpg`,
    status: "ENCODING" as BroadcastStatus,
    vodVisibility: null,
    startAt: new Date(Date.now() - (i + 1) * 12 * 60 * 60 * 1000), // 12-96 hours ago
    endAt: new Date(Date.now() - (i + 1) * 10 * 60 * 60 * 1000),
    viewersCurrent: 0,
    viewersTotal: Math.floor(Math.random() * 1000) + 300,
    reportCount: Math.floor(Math.random() * 10),
    likeCount: Math.floor(Math.random() * 150),
    revenueTotal: Math.floor(Math.random() * 3000000) + 800000,
    sellerName: `판매자${i + 71}`,
    sanctionsViewerCount: Math.floor(Math.random() * 2),
    sanctionsSellerCount: 0,
  })),

  // STOPED broadcasts (admin stopped) (5)
  ...Array.from({ length: 5 }, (_, i) => ({
    id: `stopped-${i + 1}`,
    title: `송출 중지된 방송 ${i + 1}`,
    category: ["가구", "전자기기", "악세사리"][i % 3],
    thumbnailUrl: `/vod-replay.jpg`,
    status: "STOPED" as BroadcastStatus,
    vodVisibility: "PRIVATE" as VodVisibility,
    startAt: new Date(Date.now() - (i + 3) * 24 * 60 * 60 * 1000),
    endAt: new Date(Date.now() - (i + 2) * 24 * 60 * 60 * 1000 - 30 * 60 * 1000),
    viewersCurrent: 0,
    viewersTotal: Math.floor(Math.random() * 500) + 100,
    reportCount: Math.floor(Math.random() * 80) + 20,
    likeCount: Math.floor(Math.random() * 50),
    revenueTotal: Math.floor(Math.random() * 2000000) + 500000,
    sellerName: `판매자${i + 61}`,
    stopReason: i === 4 ? "기타: 심각한 운영 정책 위반" : "부적절한 상품 판매",
    sanctionsViewerCount: Math.floor(Math.random() * 10) + 5,
    sanctionsSellerCount: 1,
  })),
]

export const mockBroadcasts = normalizeBroadcastData(rawMockBroadcasts) as Broadcast[]

export const mockProducts: Product[] = Array.from({ length: 10 }, (_, i) => ({
  id: `product-${i + 1}`,
  name: `상품 ${i + 1}`,
  price: Math.floor(Math.random() * 100000) + 10000,
  soldCount: Math.floor(Math.random() * 100) + 10,
  revenue: Math.floor(Math.random() * 5000000) + 500000,
  imageUrl: `/placeholder.svg?height=100&width=100`,
}))

export const mockRevenueData = {
  daily: Array.from({ length: 30 }, (_, i) => ({
    date: `${i + 1}일`,
    value: Math.floor(Math.random() * 10000000) + 2000000,
  })),
  monthly: Array.from({ length: 12 }, (_, i) => ({
    date: `${i + 1}월`,
    value: Math.floor(Math.random() * 100000000) + 50000000,
  })),
  yearly: Array.from({ length: 5 }, (_, i) => ({
    date: `${2020 + i}년`,
    value: Math.floor(Math.random() * 1000000000) + 500000000,
  })),
}

export const mockRevenuePerViewerData = {
  daily: Array.from({ length: 30 }, (_, i) => ({
    date: `${i + 1}일`,
    value: Math.floor(Math.random() * 50000) + 10000,
  })),
  monthly: Array.from({ length: 12 }, (_, i) => ({
    date: `${i + 1}월`,
    value: Math.floor(Math.random() * 100000) + 30000,
  })),
  yearly: Array.from({ length: 5 }, (_, i) => ({
    date: `${2020 + i}년`,
    value: Math.floor(Math.random() * 150000) + 50000,
  })),
}

export const mockStopCountData = {
  daily: Array.from({ length: 30 }, (_, i) => ({
    date: `${i + 1}일`,
    value: Math.floor(Math.random() * 10),
  })),
  monthly: Array.from({ length: 12 }, (_, i) => ({
    date: `${i + 1}월`,
    value: Math.floor(Math.random() * 50) + 5,
  })),
  yearly: Array.from({ length: 5 }, (_, i) => ({
    date: `${2020 + i}년`,
    value: Math.floor(Math.random() * 200) + 50,
  })),
}

export const mockViewerSanctionData = {
  daily: Array.from({ length: 30 }, (_, i) => ({
    date: `${i + 1}일`,
    value: Math.floor(Math.random() * 20),
  })),
  monthly: Array.from({ length: 12 }, (_, i) => ({
    date: `${i + 1}월`,
    value: Math.floor(Math.random() * 100) + 10,
  })),
  yearly: Array.from({ length: 5 }, (_, i) => ({
    date: `${2020 + i}년`,
    value: Math.floor(Math.random() * 500) + 100,
  })),
}

export const topSellersBySanctions = mockBroadcasts
  .reduce(
    (acc, b) => {
      const existing = acc.find((s) => s.name === b.sellerName)
      if (existing) {
        existing.count += b.sanctionsSellerCount
      } else {
        acc.push({ name: b.sellerName, count: b.sanctionsSellerCount })
      }
      return acc
    },
    [] as { name: string; count: number }[],
  )
  .sort((a, b) => b.count - a.count)
  .slice(0, 5)

export const topViewersBySanctions = [
  { name: "시청자A", count: 15 },
  { name: "시청자B", count: 12 },
  { name: "시청자C", count: 10 },
  { name: "시청자D", count: 8 },
  { name: "시청자E", count: 6 },
]

export const topBroadcastsByRevenue = mockBroadcasts
  .filter((b) => b.revenueTotal > 0)
  .sort((a, b) => b.revenueTotal - a.revenueTotal)
  .slice(0, 5)
  .map((b, i) => ({ rank: i + 1, title: b.title, value: b.revenueTotal }))

export const worstBroadcastsByRevenue = mockBroadcasts
  .filter((b) => b.revenueTotal > 0)
  .sort((a, b) => a.revenueTotal - b.revenueTotal)
  .slice(0, 5)
  .map((b, i) => ({ rank: i + 1, title: b.title, value: b.revenueTotal }))

export const topProductsByRevenue = mockProducts
  .sort((a, b) => b.revenue - a.revenue)
  .slice(0, 5)
  .map((p, i) => ({ rank: i + 1, title: p.name, value: p.revenue }))

export const worstProductsByRevenue = mockProducts
  .sort((a, b) => a.revenue - b.revenue)
  .slice(0, 5)
  .map((p, i) => ({ rank: i + 1, title: p.name, value: p.revenue }))
