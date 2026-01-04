import type { Broadcast, Product } from "@/lib/admin-mock-data"
import { mockProducts, mockRevenueData, mockRevenuePerViewerData } from "@/lib/admin-mock-data"
import { normalizeBroadcastData } from "./utils"

const rawSellerBroadcasts: Broadcast[] = [
  // 1 ON_AIR broadcast (seller can only have one live)
  {
    id: "seller-live-1",
    title: "겨울 패션 라이브 특가",
    status: "ON_AIR",
    category: "패션",
    thumbnailUrl: "/fashion-live-show.jpg",
    sellerName: "판매자1",
    viewersCurrent: 1250,
    viewersTotal: 3420,
    likeCount: 245,
    reportCount: 0,
    startAt: new Date(Date.now() - 45 * 60 * 1000),
    scheduledAt: new Date(Date.now() - 45 * 60 * 1000),
    endAt: null,
    revenueTotal: 850000,
    vodVisibility: "PUBLIC",
    terminationReason: null,
    sanctionsViewerCount: 0,
    sanctionsSellerCount: 0,
  },
  // 12+ RESERVED/READY/CANCELED broadcasts
  ...Array.from({ length: 15 }, (_, i) => ({
    id: `seller-reserved-${i + 1}`,
    title: `예약 방송 ${i + 1} - ${["뷰티", "가구", "전자기기", "패션", "악세사리"][i % 5]}`,
    status: (i < 8 ? "RESERVED" : i < 12 ? "READY" : "CANCELED") as "RESERVED" | "READY" | "CANCELED",
    category: ["뷰티", "가구", "전자기기", "패션", "악세사리"][i % 5],
    thumbnailUrl: [
      "/beauty-products-collection.png",
      "/cozy-living-room.png",
      "/electronics-sale.jpg",
      "/fashion-live-show.jpg",
      "/luxury-handbags.jpg",
    ][i % 5],
    sellerName: "판매자1",
    viewersCurrent: 0,
    viewersTotal: 0,
    likeCount: 0,
    reportCount: 0,
    startAt: new Date(Date.now() + (i + 1) * 24 * 60 * 60 * 1000),
    scheduledAt: new Date(Date.now() + (i + 1) * 24 * 60 * 60 * 1000),
    endAt: null,
    revenueTotal: 0,
    vodVisibility: "PUBLIC",
    terminationReason: i >= 12 ? "방송자 취소" : null,
    sanctionsViewerCount: 0,
    sanctionsSellerCount: 0,
  })),
  // 15+ VOD broadcasts (ENDED/VOD/STOPED with PUBLIC/PRIVATE mix)
  ...Array.from({ length: 18 }, (_, i) => ({
    id: `seller-vod-${i + 1}`,
    title: `VOD 방송 ${i + 1} - ${["뷰티", "가구", "전자기기", "패션", "악세사리"][i % 5]}`,
    status: (i < 6 ? "ENDED" : i < 13 ? "VOD" : "STOPED") as "ENDED" | "VOD" | "STOPED",
    category: ["뷰티", "가구", "전자기기", "패션", "악세사리"][i % 5],
    thumbnailUrl: [
      "/beauty-products-collection.png",
      "/cozy-living-room.png",
      "/electronics-sale.jpg",
      "/fashion-live-show.jpg",
      "/luxury-handbags.jpg",
    ][i % 5],
    sellerName: "판매자1",
    viewersCurrent: 0,
    viewersTotal: 1200 + i * 100,
    likeCount: 50 + i * 15,
    reportCount: 0,
    startAt: new Date(Date.now() - (i + 1) * 24 * 60 * 60 * 1000),
    scheduledAt: new Date(Date.now() - (i + 1) * 24 * 60 * 60 * 1000),
    endAt: new Date(Date.now() - (i + 1) * 24 * 60 * 60 * 1000 + 90 * 60 * 1000),
    revenueTotal: 500000 + i * 50000,
    vodVisibility: i % 3 === 0 ? "PRIVATE" : "PUBLIC",
    terminationReason: i >= 13 ? ["부적절한 콘텐츠", "시청자 신고", "운영 정책 위반"][i % 3] : null,
    sanctionsViewerCount: 0,
    sanctionsSellerCount: 0,
  })),
]

export const sellerBroadcasts = normalizeBroadcastData(rawSellerBroadcasts) as Broadcast[]

export const sellerProducts: Product[] = mockProducts

// Seller-specific data
export const sellerRevenueData = mockRevenueData
export const sellerRevenuePerViewerData = mockRevenuePerViewerData

export const topBroadcastsByRevenueSeller = sellerBroadcasts
  .filter((b) => b.revenueTotal > 0)
  .sort((a, b) => b.revenueTotal - a.revenueTotal)
  .slice(0, 5)
  .map((b, i) => ({ rank: i + 1, title: b.title, value: b.revenueTotal }))

export const worstBroadcastsByRevenueSeller = sellerBroadcasts
  .filter((b) => b.revenueTotal > 0)
  .sort((a, b) => a.revenueTotal - b.revenueTotal)
  .slice(0, 5)
  .map((b, i) => ({ rank: i + 1, title: b.title, value: b.revenueTotal }))

export const topBroadcastsByViewersSeller = sellerBroadcasts
  .filter((b) => b.viewersTotal > 0)
  .sort((a, b) => b.viewersTotal - a.viewersTotal)
  .slice(0, 5)
  .map((b, i) => ({ rank: i + 1, title: b.title, value: b.viewersTotal }))

export const worstBroadcastsByViewersSeller = sellerBroadcasts
  .filter((b) => b.viewersTotal > 0)
  .sort((a, b) => a.viewersTotal - b.viewersTotal)
  .slice(0, 5)
  .map((b, i) => ({ rank: i + 1, title: b.title, value: b.viewersTotal }))
