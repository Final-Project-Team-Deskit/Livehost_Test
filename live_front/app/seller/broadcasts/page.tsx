"use client"

import { Select, SelectTrigger, SelectValue, SelectContent, SelectItem } from "@/components/ui/select"

import { useState, useEffect } from "react"
import { useRouter } from "next/navigation"
import { Plus, Clock, Users, DollarSign, Heart, Eye, TrendingUp, Package } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { BroadcastCarousel } from "@/components/admin/broadcast-carousel"
import { BroadcastCard } from "@/components/admin/broadcast-card"
import { DeviceSetupModal } from "@/components/seller/device-setup-modal"
import { FilterTabs } from "@/components/admin/filter-tabs"
import { FilterBar } from "@/components/admin/filter-bar"
import { DateRangeFilter } from "@/components/admin/date-range-filter"
import { CategoryFilter } from "@/components/admin/category-filter"
import { VisibilityFilter } from "@/components/admin/visibility-filter"
import { SortSelect } from "@/components/admin/sort-select"
import { sellerBroadcasts } from "@/lib/seller-mock-data"
import { sellerRoutes } from "@/lib/routes"
import { isWithinReadyWindow, normalizeBroadcastData } from "@/lib/utils"
import { useToast } from "@/hooks/use-toast"
import Image from "next/image"
import Link from "next/link"
import type { Product } from "@/lib/admin-mock-data"

interface BroadcastProduct extends Product {
  broadcastPrice: number
  quantity: number
  soldCount: number
  isPinned: boolean
  status: "판매중" | "품절" | "삭제됨"
}

type TabType = "전체" | "예약" | "방송 중" | "VOD"

export default function SellerBroadcastsPage() {
  const router = useRouter()
  const { toast } = useToast()
  const [activeTab, setActiveTab] = useState<TabType>("전체")
  const [reservationStatus, setReservationStatus] = useState("all")
  const [reservationCategory, setReservationCategory] = useState("all")
  const [reservationSort, setReservationSort] = useState("nearest")
  const [vodStartDate, setVodStartDate] = useState("")
  const [vodEndDate, setVodEndDate] = useState("")
  const [vodVisibility, setVodVisibility] = useState("all")
  const [vodCategory, setVodCategory] = useState("all")
  const [vodSort, setVodSort] = useState("latest")
  const [visibleCount, setVisibleCount] = useState(12)
  const [deviceModalOpen, setDeviceModalOpen] = useState(false)
  const [selectedBroadcastId, setSelectedBroadcastId] = useState<string | null>(null)
  const [elapsedTime, setElapsedTime] = useState("")
  const [currentViewers, setCurrentViewers] = useState(0)

  const [realtimeLikes, setRealtimeLikes] = useState(0)
  const [realtimeSales, setRealtimeSales] = useState(0)
  const [broadcastProducts, setBroadcastProducts] = useState<BroadcastProduct[]>([])

  const handleNormalizeData = () => {
    normalizeBroadcastData(sellerBroadcasts)
    toast({
      title: "데이터 정리 완료",
      description: "데이터 정리가 완료되었습니다.",
    })
  }

  const liveBroadcast = sellerBroadcasts.find((b) => b.status === "ON_AIR" || b.status === "ENDED")

  useEffect(() => {
    if (liveBroadcast) {
      // Generate mock broadcast products
      const mockBroadcastProducts: BroadcastProduct[] = [
        {
          id: "bp-1",
          name: "겨울 롱패딩 (블랙)",
          price: 129000,
          imageUrl: "/black-padded-jacket.jpg",
          stock: 45,
          soldCount: 12,
          revenue: 1392000,
          broadcastPrice: 99000,
          quantity: 50,
          isPinned: true,
          status: "판매중",
        },
        {
          id: "bp-2",
          name: "울 니트 스웨터 (베이지)",
          price: 59000,
          imageUrl: "/beige-wool-sweater.jpg",
          stock: 28,
          soldCount: 8,
          revenue: 432000,
          broadcastPrice: 45000,
          quantity: 40,
          isPinned: false,
          status: "판매중",
        },
        {
          id: "bp-3",
          name: "슬림핏 청바지",
          price: 79000,
          imageUrl: "/slim-fit-jeans.jpg",
          stock: 0,
          soldCount: 15,
          revenue: 885000,
          broadcastPrice: 59000,
          quantity: 15,
          isPinned: false,
          status: "품절",
        },
        {
          id: "bp-4",
          name: "캐시미어 머플러 (그레이)",
          price: 89000,
          imageUrl: "/gray-cashmere-scarf.jpg",
          stock: 62,
          soldCount: 5,
          revenue: 370000,
          broadcastPrice: 74000,
          quantity: 70,
          isPinned: false,
          status: "판매중",
        },
        {
          id: "bp-5",
          name: "레더 장갑 세트",
          price: 45000,
          imageUrl: "/leather-gloves.jpg",
          stock: 18,
          soldCount: 3,
          revenue: 105000,
          broadcastPrice: 35000,
          quantity: 25,
          isPinned: false,
          status: "판매중",
        },
      ]

      setBroadcastProducts(mockBroadcastProducts)
      setRealtimeLikes(liveBroadcast.likeCount || 0)
      setRealtimeSales(liveBroadcast.revenueTotal || 0)
    }
  }, [liveBroadcast])

  useEffect(() => {
    if (liveBroadcast && activeTab === "방송 중") {
      setCurrentViewers(liveBroadcast.viewersCurrent || 0)

      const timer = setInterval(() => {
        if (!liveBroadcast.startAt) return

        const now = new Date()
        const diff = Math.floor((now.getTime() - liveBroadcast.startAt.getTime()) / 1000)
        const hours = Math.floor(diff / 3600)
        const minutes = Math.floor((diff % 3600) / 60)
        const seconds = diff % 60
        setElapsedTime(
          `${String(hours).padStart(2, "0")}:${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`,
        )

        // Update viewers
        setCurrentViewers((prev) => {
          const change = Math.floor(Math.random() * 20) - 10
          return Math.max(0, prev + change)
        })

        // Update likes (increase only)
        setRealtimeLikes((prev) => {
          const increase = Math.random() < 0.3 ? Math.floor(Math.random() * 3) + 1 : 0
          return prev + increase
        })

        // Update sales
        setRealtimeSales((prev) => {
          const increase = Math.random() < 0.2 ? Math.floor(Math.random() * 50000) + 10000 : 0
          return prev + increase
        })

        // Update product stock (simulate sales)
        setBroadcastProducts((prev) =>
          prev.map((p) => {
            if (p.status === "판매중" && Math.random() < 0.1) {
              const newStock = Math.max(0, p.stock - 1)
              const newSoldCount = p.soldCount + 1
              return {
                ...p,
                stock: newStock,
                soldCount: newSoldCount,
                status: newStock === 0 ? "품절" : "판매중",
              }
            }
            return p
          }),
        )
      }, 1000)

      return () => clearInterval(timer)
    }
  }, [liveBroadcast, activeTab])

  // Original elapsed time for summary section
  useEffect(() => {
    if (liveBroadcast && activeTab === "전체") {
      setCurrentViewers(liveBroadcast.viewersCurrent || 0)

      const timer = setInterval(() => {
        if (!liveBroadcast.startAt) return

        const now = new Date()
        const diff = Math.floor((now.getTime() - liveBroadcast.startAt.getTime()) / 1000)
        const hours = Math.floor(diff / 3600)
        const minutes = Math.floor((diff % 3600) / 60)
        const seconds = diff % 60
        setElapsedTime(
          `${String(hours).padStart(2, "0")}:${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`,
        )

        setCurrentViewers((prev) => {
          const change = Math.floor(Math.random() * 20) - 10
          return Math.max(0, prev + change)
        })
      }, 1000)

      return () => clearInterval(timer)
    }
  }, [liveBroadcast, activeTab])

  const reservedBroadcastsSummary = sellerBroadcasts
    .filter((b) => {
      if (b.status === "READY" && b.scheduledAt && isWithinReadyWindow(b.scheduledAt)) return true
      if (b.status === "RESERVED" && b.scheduledAt > new Date()) return true
      return false
    })
    .sort((a, b) => {
      // Sort by scheduled_at ascending (earliest first)
      if (a.scheduledAt && b.scheduledAt) {
        return a.scheduledAt.getTime() - b.scheduledAt.getTime()
      }
      return a.startAt.getTime() - b.startAt.getTime()
    })
    .slice(0, 5)

  const vodBroadcastsSummary = sellerBroadcasts
    .filter(
      (b) => (b.status === "ENCODING" || b.status === "VOD" || b.status === "STOPED") && b.vodVisibility === "PUBLIC",
    )
    .sort((a, b) => {
      // Sort by ended_at descending if available, otherwise by startAt descending
      if (a.endedAt && b.endedAt) {
        return b.endedAt.getTime() - a.endedAt.getTime()
      }
      return b.startAt.getTime() - a.startAt.getTime()
    })
    .slice(0, 5)

  const filteredReservations = sellerBroadcasts
    .filter((b) => {
      if (reservationStatus === "reserved") {
        return b.status === "RESERVED" && (isWithinReadyWindow(b.scheduledAt) || b.scheduledAt > new Date())
      }
      if (reservationStatus === "canceled") return b.status === "CANCELED"
      if (reservationStatus === "all") {
        return b.status === "RESERVED" || b.status === "CANCELED"
      }
      return false
    })
    .filter((b) => {
      if (reservationCategory === "all") return true
      return b.category === reservationCategory
    })
    .sort((a, b) => {
      switch (reservationSort) {
        case "latest":
          return b.createdAt.getTime() - a.createdAt.getTime()
        case "oldest":
          return a.createdAt.getTime() - b.createdAt.getTime()
        case "nearest":
        default:
          return a.startAt.getTime() - b.startAt.getTime()
      }
    })

  const filteredVods = sellerBroadcasts
    .filter((b) => {
      if (!["ENCODING", "VOD", "STOPED"].includes(b.status)) return false

      if (vodStartDate && b.startAt < new Date(vodStartDate)) return false
      if (vodEndDate && b.startAt > new Date(vodEndDate)) return false
      if (vodVisibility === "public" && b.vodVisibility !== "PUBLIC") return false
      if (vodVisibility === "private" && b.vodVisibility !== "PRIVATE") return false
      if (vodCategory !== "all" && b.category !== vodCategory) return false

      return true
    })
    .sort((a, b) => {
      switch (vodSort) {
        case "oldest":
          return a.startAt.getTime() - b.startAt.getTime()
        case "reports":
          return b.reportCount - a.reportCount
        case "likes-high":
          return b.likeCount - a.likeCount
        case "likes-low":
          return a.likeCount - b.likeCount
        case "revenue-high":
          return b.revenueTotal - a.revenueTotal
        case "revenue-low":
          return a.revenueTotal - b.revenueTotal
        case "viewers-high":
          return b.viewersTotal - a.viewersTotal
        case "viewers-low":
          return a.viewersTotal - b.viewersTotal
        case "latest":
        default:
          return b.startAt.getTime() - a.startAt.getTime()
      }
    })

  const handleReadyStart = (broadcastId: string) => {
    setSelectedBroadcastId(broadcastId)
    setDeviceModalOpen(true)
  }

  const reservationSortOptions = [
    { value: "nearest", label: "방송 시간이 가까운 순" },
    { value: "latest", label: "최신순" },
    { value: "oldest", label: "오래된 순" },
  ]

  const vodSortOptions = [
    { value: "latest", label: "최신순" },
    { value: "oldest", label: "오래된 순" },
    { value: "reports", label: "신고 많은 순" },
    { value: "likes-high", label: "좋아요가 높은 순" },
    { value: "likes-low", label: "좋아요가 낮은 순" },
    { value: "revenue-high", label: "매출액이 높은 순" },
    { value: "revenue-low", label: "매출액이 낮은 순" },
    { value: "viewers-high", label: "총 시청자 수가 높은 순" },
    { value: "viewers-low", label: "총 시청자 수가 낮은 순" },
  ]

  const getStatusBadge = (status: BroadcastProduct["status"]) => {
    switch (status) {
      case "판매중":
        return <Badge className="bg-green-500">판매중</Badge>
      case "품절":
        return <Badge variant="destructive">품절</Badge>
      case "삭제됨":
        return <Badge variant="secondary">삭제됨</Badge>
    }
  }

  return (
    <div className="container mx-auto px-4 py-8">
      {process.env.NODE_ENV === "development" && (
        <div className="mb-4">
          <Button variant="outline" size="sm" onClick={handleNormalizeData}>
            데이터 정리
          </Button>
        </div>
      )}

      <div className="relative flex items-center mb-10">
        <div className="absolute left-1/2 -translate-x-1/2">
          <FilterTabs
            tabs={[
              { label: "전체", value: "전체" },
              { label: "예약", value: "예약" },
              { label: "방송 중", value: "방송 중" },
              { label: "VOD", value: "VOD" },
            ]}
            activeTab={activeTab}
            onChange={(value) => {
              setActiveTab(value as TabType)
              setVisibleCount(12)
            }}
          />
        </div>
        <Button onClick={() => router.push(sellerRoutes.broadcasts.create())} className="ml-auto">
          <Plus className="h-4 w-4 mr-2" />
          방송 등록
        </Button>
      </div>

      {activeTab === "전체" && (
        <div className="space-y-12">
          {liveBroadcast && (
            <section>
              <div className="flex items-center justify-between mb-6">
                <h2 className="text-2xl font-bold">방송 중</h2>
              </div>
              <div className="flex justify-center">
                <Card className="overflow-hidden max-w-3xl w-full">
                  <div className="flex gap-4 p-4">
                    <div className="relative w-56 aspect-[9/16] flex-shrink-0">
                      <Image
                        src={liveBroadcast.thumbnailUrl || "/placeholder.svg?height=400&width=225"}
                        alt={liveBroadcast.title}
                        fill
                        className="object-cover rounded-lg"
                      />
                      <div className="absolute top-2 left-2">
                        <div className="bg-red-600 text-white px-2.5 py-0.5 rounded-full text-xs font-bold flex items-center gap-1">
                          <span className="w-1.5 h-1.5 bg-white rounded-full animate-pulse" />
                          LIVE
                        </div>
                      </div>
                    </div>

                    <div className="flex-1 flex flex-col justify-between">
                      <div className="space-y-2.5">
                        <div>
                          <h3 className="text-xl font-bold mb-1">{liveBroadcast.title}</h3>
                          <p className="text-sm text-muted-foreground">
                            {liveBroadcast.category} · {liveBroadcast.sellerName}
                          </p>
                        </div>

                        <div className="space-y-1.5">
                          <div className="flex items-center gap-2 text-sm">
                            <Users className="h-4 w-4 text-muted-foreground" />
                            <span className="font-semibold">실시간 시청자:</span>
                            <span className="text-primary font-bold">{currentViewers.toLocaleString()}</span>
                          </div>

                          <div className="flex items-center gap-2 text-sm">
                            <Clock className="h-4 w-4 text-muted-foreground" />
                            <span className="font-semibold">방송 진행 시간:</span>
                            <span className="font-mono text-primary font-bold">{elapsedTime}</span>
                          </div>

                          {liveBroadcast.startAt && (
                            <div className="text-sm text-muted-foreground">
                              시작 시간: {liveBroadcast.startAt.toLocaleDateString("ko-KR")}{" "}
                              {liveBroadcast.startAt.toLocaleTimeString("ko-KR", {
                                hour: "2-digit",
                                minute: "2-digit",
                              })}
                            </div>
                          )}
                        </div>
                      </div>

                      <div className="mt-3">
                        <Link href={sellerRoutes.broadcasts.studio(liveBroadcast.id)}>
                          <Button size="lg" className="w-full text-lg py-6">
                            방송 입장
                          </Button>
                        </Link>
                      </div>
                    </div>
                  </div>
                </Card>
              </div>
            </section>
          )}

          <section>
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-2xl font-bold">예약</h2>
              <Button
                variant="ghost"
                size="sm"
                onClick={() => {
                  setActiveTab("예약")
                  setVisibleCount(12)
                }}
              >
                <Plus className="h-4 w-4 mr-1" />더 보기
              </Button>
            </div>
            {reservedBroadcastsSummary.length > 0 ? (
              <BroadcastCarousel
                broadcasts={reservedBroadcastsSummary}
                variant="reservation"
                namespace="seller"
                onReadyStart={handleReadyStart}
              />
            ) : (
              <Card className="p-8 text-center text-muted-foreground">예약된 방송이 없습니다.</Card>
            )}
          </section>

          <section>
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-2xl font-bold">VOD</h2>
              <Button
                variant="ghost"
                size="sm"
                onClick={() => {
                  setActiveTab("VOD")
                  setVisibleCount(12)
                }}
              >
                <Plus className="h-4 w-4 mr-1" />더 보기
              </Button>
            </div>
            {vodBroadcastsSummary.length > 0 ? (
              <BroadcastCarousel broadcasts={vodBroadcastsSummary} variant="vod" namespace="seller" />
            ) : (
              <Card className="p-8 text-center text-muted-foreground">VOD가 없습니다.</Card>
            )}
          </section>
        </div>
      )}

      {activeTab === "예약" && (
        <>
          <div className="flex justify-between items-center mb-6">
            <h1 className="text-2xl font-bold">예약</h1>
            <FilterBar>
              <Select value={reservationStatus} onValueChange={setReservationStatus}>
                <SelectTrigger className="w-40">
                  <SelectValue placeholder="예약 상태" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">전체</SelectItem>
                  <SelectItem value="reserved">예약 중</SelectItem>
                  <SelectItem value="canceled">취소됨</SelectItem>
                </SelectContent>
              </Select>

              <CategoryFilter value={reservationCategory} onChange={setReservationCategory} />

              <SortSelect
                value={reservationSort}
                onChange={setReservationSort}
                options={reservationSortOptions}
                className="w-48"
              />
            </FilterBar>
          </div>

          {filteredReservations.length > 0 ? (
            <>
              <div className="grid grid-cols-4 gap-4">
                {filteredReservations.slice(0, visibleCount).map((broadcast) => {
                  const isReady = isWithinReadyWindow(broadcast.scheduledAt)

                  return (
                    <BroadcastCard
                      key={broadcast.id}
                      broadcast={broadcast}
                      variant="reservation"
                      namespace="seller"
                      onReadyStart={isReady ? () => handleReadyStart(broadcast.id) : undefined}
                    />
                  )
                })}
              </div>

              {filteredReservations.length > visibleCount && (
                <div className="flex justify-center mt-8">
                  <Button variant="outline" size="lg" onClick={() => setVisibleCount((prev) => prev + 12)}>
                    더 보기
                  </Button>
                </div>
              )}
            </>
          ) : (
            <Card className="p-8 text-center text-muted-foreground">예약 방송이 없습니다.</Card>
          )}
        </>
      )}

      {activeTab === "방송 중" && (
        <div className="space-y-6">
          <div>
            <h1 className="text-2xl font-bold">방송 중</h1>
            <p className="text-sm text-muted-foreground mt-1">진행 중인 방송 정보를 실시간으로 확인할 수 있습니다.</p>
          </div>

          {liveBroadcast ? (
            <div className="grid lg:grid-cols-3 gap-6">
              {/* Left column: Broadcast info + stats (2/3 width) */}
              <div className="lg:col-span-2 space-y-6">
                {/* Broadcast basic info card */}
                <Card className="p-6">
                  <div className="flex gap-6">
                    <div className="relative w-48 aspect-[9/16] flex-shrink-0">
                      <Image
                        src={liveBroadcast.thumbnailUrl || "/placeholder.svg?height=400&width=225"}
                        alt={liveBroadcast.title}
                        fill
                        className="object-cover rounded-lg"
                      />
                      <div className="absolute top-2 left-2">
                        <div className="bg-red-600 text-white px-2.5 py-0.5 rounded-full text-xs font-bold flex items-center gap-1">
                          <span className="w-1.5 h-1.5 bg-white rounded-full animate-pulse" />
                          LIVE
                        </div>
                      </div>
                    </div>

                    <div className="flex-1 space-y-4">
                      <div>
                        <h2 className="text-2xl font-bold mb-1">{liveBroadcast.title}</h2>
                        <p className="text-sm text-muted-foreground">
                          {liveBroadcast.category} · {liveBroadcast.sellerName}
                        </p>
                        <Badge className="mt-2 bg-red-600">
                          {liveBroadcast.status === "ON_AIR" ? "방송 중" : "종료됨"}
                        </Badge>
                      </div>

                      {liveBroadcast.notice && (
                        <div className="bg-muted p-3 rounded-lg">
                          <p className="text-sm font-medium">📢 공지사항</p>
                          <p className="text-sm text-muted-foreground mt-1">{liveBroadcast.notice}</p>
                        </div>
                      )}

                      <div className="text-sm text-muted-foreground">
                        방송 시작: {liveBroadcast.startAt.toLocaleDateString("ko-KR")}{" "}
                        {liveBroadcast.startAt.toLocaleTimeString("ko-KR", {
                          hour: "2-digit",
                          minute: "2-digit",
                        })}
                      </div>

                      <Link href={sellerRoutes.broadcasts.studio(liveBroadcast.id)}>
                        <Button size="lg" className="w-full">
                          방송 입장
                        </Button>
                      </Link>
                    </div>
                  </div>
                </Card>

                {/* Real-time stats cards */}
                <div>
                  <div className="flex items-center justify-between mb-3">
                    <h3 className="text-lg font-semibold">실시간 통계</h3>
                    <span className="text-xs text-muted-foreground flex items-center gap-1">
                      <span className="w-1.5 h-1.5 bg-green-500 rounded-full animate-pulse" />
                      실시간 업데이트 중
                    </span>
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <Card className="p-4">
                      <div className="flex items-center justify-between">
                        <div>
                          <p className="text-sm text-muted-foreground mb-1">현재 방송 상태</p>
                          <p className="text-2xl font-bold">
                            {liveBroadcast.status === "ON_AIR" ? "방송 중" : "종료됨"}
                          </p>
                        </div>
                        <div className="h-12 w-12 rounded-full bg-red-100 flex items-center justify-center">
                          <Eye className="h-6 w-6 text-red-600" />
                        </div>
                      </div>
                      <div className="mt-2 text-xs text-muted-foreground">진행 시간: {elapsedTime}</div>
                    </Card>

                    <Card className="p-4">
                      <div className="flex items-center justify-between">
                        <div>
                          <p className="text-sm text-muted-foreground mb-1">시청자 수</p>
                          <p className="text-2xl font-bold">{currentViewers.toLocaleString()}</p>
                        </div>
                        <div className="h-12 w-12 rounded-full bg-blue-100 flex items-center justify-center">
                          <Users className="h-6 w-6 text-blue-600" />
                        </div>
                      </div>
                      <div className="mt-2 text-xs text-muted-foreground">
                        <TrendingUp className="h-3 w-3 inline mr-1" />
                        실시간
                      </div>
                    </Card>

                    <Card className="p-4">
                      <div className="flex items-center justify-between">
                        <div>
                          <p className="text-sm text-muted-foreground mb-1">좋아요 수</p>
                          <p className="text-2xl font-bold">{realtimeLikes.toLocaleString()}</p>
                        </div>
                        <div className="h-12 w-12 rounded-full bg-pink-100 flex items-center justify-center">
                          <Heart className="h-6 w-6 text-pink-600" />
                        </div>
                      </div>
                      <div className="mt-2 text-xs text-muted-foreground">
                        <TrendingUp className="h-3 w-3 inline mr-1" />
                        실시간
                      </div>
                    </Card>

                    <Card className="p-4">
                      <div className="flex items-center justify-between">
                        <div>
                          <p className="text-sm text-muted-foreground mb-1">현재 매출</p>
                          <p className="text-2xl font-bold">{realtimeSales.toLocaleString()}원</p>
                        </div>
                        <div className="h-12 w-12 rounded-full bg-green-100 flex items-center justify-center">
                          <DollarSign className="h-6 w-6 text-green-600" />
                        </div>
                      </div>
                      <div className="mt-2 text-xs text-muted-foreground">
                        <TrendingUp className="h-3 w-3 inline mr-1" />
                        실시간
                      </div>
                    </Card>
                  </div>
                </div>
              </div>

              {/* Right column: Product list (1/3 width) */}
              <div className="lg:col-span-1">
                <Card className="p-6">
                  <div className="flex items-center justify-between mb-4">
                    <h3 className="text-lg font-semibold flex items-center gap-2">
                      <Package className="h-5 w-5" />
                      판매 상품
                    </h3>
                    <span className="text-xs text-muted-foreground">{broadcastProducts.length}개</span>
                  </div>

                  {broadcastProducts.length > 0 ? (
                    <div className="space-y-3">
                      {/* Sort: pinned first */}
                      {broadcastProducts
                        .sort((a, b) => (a.isPinned === b.isPinned ? 0 : a.isPinned ? -1 : 1))
                        .map((product) => (
                          <div
                            key={product.id}
                            className={`border rounded-lg p-3 ${
                              product.isPinned ? "bg-yellow-50 border-yellow-200" : "bg-background"
                            }`}
                          >
                            <div className="flex gap-3">
                              <div className="relative w-16 h-16 flex-shrink-0 rounded overflow-hidden">
                                <Image
                                  src={product.imageUrl || "/placeholder.svg"}
                                  alt={product.name}
                                  fill
                                  className="object-cover"
                                />
                              </div>

                              <div className="flex-1 min-w-0">
                                <div className="flex items-start justify-between gap-2">
                                  <h4 className="font-medium text-sm truncate">{product.name}</h4>
                                  {product.isPinned && (
                                    <Badge variant="secondary" className="text-xs flex-shrink-0">
                                      PIN
                                    </Badge>
                                  )}
                                </div>

                                <div className="mt-1 space-y-0.5">
                                  <div className="flex items-center justify-between text-xs">
                                    <span className="text-muted-foreground">실재고</span>
                                    <span className="font-medium">{product.stock}개</span>
                                  </div>
                                  <div className="flex items-center justify-between text-xs">
                                    <span className="text-muted-foreground">판매가</span>
                                    <span className="line-through text-muted-foreground">
                                      {product.price.toLocaleString()}원
                                    </span>
                                  </div>
                                  <div className="flex items-center justify-between text-xs">
                                    <span className="text-muted-foreground">방송가</span>
                                    <span className="font-bold text-primary">
                                      {product.broadcastPrice.toLocaleString()}원
                                    </span>
                                  </div>
                                  <div className="flex items-center justify-between text-xs">
                                    <span className="text-muted-foreground">판매수량</span>
                                    <span className="font-medium">
                                      {product.soldCount} / {product.quantity}개
                                    </span>
                                  </div>
                                </div>

                                <div className="mt-2">{getStatusBadge(product.status)}</div>
                              </div>
                            </div>
                          </div>
                        ))}
                    </div>
                  ) : (
                    <div className="text-center py-8 text-sm text-muted-foreground">등록된 판매 상품이 없습니다.</div>
                  )}
                </Card>
              </div>
            </div>
          ) : (
            <Card className="p-12 text-center">
              <p className="text-muted-foreground text-lg">현재 진행 중인 방송이 없습니다.</p>
            </Card>
          )}
        </div>
      )}

      {activeTab === "VOD" && (
        <>
          <div className="flex justify-between items-center mb-6">
            <h1 className="text-2xl font-bold">VOD</h1>
            <FilterBar>
              <DateRangeFilter
                startDate={vodStartDate}
                endDate={vodEndDate}
                onStartDateChange={setVodStartDate}
                onEndDateChange={setVodEndDate}
              />

              <VisibilityFilter value={vodVisibility} onChange={setVodVisibility} />

              <CategoryFilter value={vodCategory} onChange={setVodCategory} />

              <SortSelect value={vodSort} onChange={setVodSort} options={vodSortOptions} />
            </FilterBar>
          </div>

          {filteredVods.length > 0 ? (
            <>
              <div className="grid grid-cols-4 gap-4">
                {filteredVods.slice(0, visibleCount).map((broadcast) => (
                  <BroadcastCard key={broadcast.id} broadcast={broadcast} variant="vod" namespace="seller" />
                ))}
              </div>

              {filteredVods.length > visibleCount && (
                <div className="flex justify-center mt-8">
                  <Button variant="outline" size="lg" onClick={() => setVisibleCount((prev) => prev + 12)}>
                    더 보기
                  </Button>
                </div>
              )}
            </>
          ) : (
            <Card className="p-8 text-center text-muted-foreground">VOD가 없습니다.</Card>
          )}
        </>
      )}

      <DeviceSetupModal
        open={deviceModalOpen}
        onClose={() => {
          setDeviceModalOpen(false)
          setSelectedBroadcastId(null)
        }}
        broadcastId={selectedBroadcastId}
      />
    </div>
  )
}
