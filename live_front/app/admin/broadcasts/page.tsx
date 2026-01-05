"use client"

import { useState } from "react"
import { Plus } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { FilterTabs } from "@/components/admin/filter-tabs"
import { FilterBar } from "@/components/admin/filter-bar"
import { DateRangeFilter } from "@/components/admin/date-range-filter"
import { CategoryFilter } from "@/components/admin/category-filter"
import { VisibilityFilter } from "@/components/admin/visibility-filter"
import { SortSelect } from "@/components/admin/sort-select"
import { BroadcastCarousel } from "@/components/admin/broadcast-carousel"
import { BroadcastCard } from "@/components/admin/broadcast-card"
import { EmptyState } from "@/components/admin/empty-state"
import { mockBroadcasts } from "@/lib/admin-mock-data"
import { normalizeBroadcastData, isWithinReadyWindow } from "@/lib/utils"
import { useToast } from "@/hooks/use-toast"

type TabType = "all" | "reserved" | "live" | "vod"

export default function BroadcastsPage() {
  const { toast } = useToast()
  const [activeTab, setActiveTab] = useState<TabType>("all")

  const [reservationStatus, setReservationStatus] = useState("all")
  const [reservedCategory, setReservedCategory] = useState("all")
  const [reservedSort, setReservedSort] = useState("nearest")

  const [liveCategory, setLiveCategory] = useState("all")
  const [liveSort, setLiveSort] = useState("reports")

  const [vodStartDate, setVodStartDate] = useState("")
  const [vodEndDate, setVodEndDate] = useState("")
  const [vodVisibility, setVodVisibility] = useState("all")
  const [vodCategory, setVodCategory] = useState("all")
  const [vodSort, setVodSort] = useState("latest")

  const [visibleCount, setVisibleCount] = useState(12)

  const liveBroadcasts = mockBroadcasts
    .filter((b) => {
      if (b.status === "ON_AIR" || b.status === "ENDED") return true
      if (b.status === "READY" && b.startAt && isWithinReadyWindow(b.startAt)) return true
      return false
    })
    .sort((a, b) => b.reportCount - a.reportCount)

  const reservedBroadcasts = mockBroadcasts
    .filter((b) => {
      if (b.status === "RESERVED" && b.startAt) {
        return isWithinReadyWindow(b.startAt) || b.startAt > new Date()
      }
      return false
    })
    .sort((a, b) => {
      return a.startAt.getTime() - b.startAt.getTime()
    })

  const vodBroadcasts = mockBroadcasts
    .filter((b) => b.status === "ENCODING" || b.status === "VOD" || b.status === "STOPED")
    .sort((a, b) => {
      if (a.endAt && b.endAt) {
        return b.endAt.getTime() - a.endAt.getTime()
      }
      return b.startAt.getTime() - a.startAt.getTime()
    })

  const getFilteredReservedBroadcasts = () => {
    let filtered = mockBroadcasts.filter((b) => b.status === "RESERVED" || b.status === "CANCELED")

    if (reservationStatus === "reserved") {
      filtered = filtered.filter((b) => b.status === "RESERVED")
    } else if (reservationStatus === "canceled") {
      filtered = filtered.filter((b) => b.status === "CANCELED")
    }

    if (reservedCategory !== "all") {
      filtered = filtered.filter((b) => b.category === reservedCategory)
    }

    switch (reservedSort) {
      case "latest":
        filtered.sort((a, b) => b.startAt.getTime() - a.startAt.getTime())
        break
      case "oldest":
        filtered.sort((a, b) => a.startAt.getTime() - b.startAt.getTime())
        break
      case "nearest":
        filtered.sort((a, b) => a.startAt.getTime() - b.startAt.getTime())
        break
    }

    return filtered
  }

  const handleVerifySort = () => {
    const filtered = getFilteredReservedBroadcasts()
    const top10 = filtered.slice(0, 10)

    console.log("[v0] 정렬 점검 - 현재 정렬:", reservedSort)
    console.log("[v0] 상위 10개 항목:")
    top10.forEach((b, idx) => {
      console.log(`  ${idx + 1}. ID: ${b.id}, 예약시간: ${b.startAt.toLocaleString("ko-KR")}`)
    })

    toast({
      title: "정렬 점검",
      description: "콘솔에서 정렬 결과를 확인해주세요.",
    })
  }

  const getFilteredLiveBroadcasts = () => {
    let filtered = mockBroadcasts.filter((b) => b.status === "READY" || b.status === "ON_AIR" || b.status === "ENDED")

    if (liveCategory !== "all") {
      filtered = filtered.filter((b) => b.category === liveCategory)
    }

    switch (liveSort) {
      case "latest":
        filtered.sort((a, b) => b.startAt.getTime() - a.startAt.getTime())
        break
      case "reports":
        filtered.sort((a, b) => b.reportCount - a.reportCount)
        break
      case "viewers-high":
        filtered.sort((a, b) => b.viewersCurrent - a.viewersCurrent)
        break
      case "viewers-low":
        filtered.sort((a, b) => a.viewersCurrent - b.viewersCurrent)
        break
    }

    return filtered
  }

  const getFilteredVodBroadcasts = () => {
    let filtered = mockBroadcasts.filter((b) => b.status === "ENCODING" || b.status === "VOD" || b.status === "STOPED")

    if (vodStartDate) {
      filtered = filtered.filter((b) => b.startAt >= new Date(vodStartDate))
    }
    if (vodEndDate) {
      filtered = filtered.filter((b) => b.startAt <= new Date(vodEndDate))
    }

    if (vodVisibility === "public") {
      filtered = filtered.filter((b) => b.vodVisibility === "PUBLIC")
    } else if (vodVisibility === "private") {
      filtered = filtered.filter((b) => b.vodVisibility === "PRIVATE")
    }

    if (vodCategory !== "all") {
      filtered = filtered.filter((b) => b.category === vodCategory)
    }

    switch (vodSort) {
      case "latest":
        filtered.sort((a, b) => b.startAt.getTime() - a.startAt.getTime())
        break
      case "oldest":
        filtered.sort((a, b) => a.startAt.getTime() - b.startAt.getTime())
        break
      case "reports":
        filtered.sort((a, b) => b.reportCount - a.reportCount)
        break
      case "likes-high":
        filtered.sort((a, b) => b.likeCount - a.likeCount)
        break
      case "likes-low":
        filtered.sort((a, b) => a.likeCount - b.likeCount)
        break
      case "revenue-high":
        filtered.sort((a, b) => b.revenueTotal - a.revenueTotal)
        break
      case "revenue-low":
        filtered.sort((a, b) => a.revenueTotal - b.revenueTotal)
        break
      case "viewers-high":
        filtered.sort((a, b) => b.viewersTotal - a.viewersTotal)
        break
      case "viewers-low":
        filtered.sort((a, b) => a.viewersTotal - b.viewersTotal)
        break
    }

    return filtered
  }

  const handleNormalizeData = () => {
    normalizeBroadcastData(mockBroadcasts)
    toast({
      title: "데이터 정리 완료",
      description: "데이터 정리가 완료되었습니다.",
    })
  }

  const reservedSortOptions = [
    { value: "nearest", label: "방송 시간이 가까운 순" },
    { value: "latest", label: "최신순" },
    { value: "oldest", label: "오래된 순" },
  ]

  const liveSortOptions = [
    { value: "reports", label: "신고 건수가 많은 순" },
    { value: "viewers-high", label: "시청자가 많은 순" },
    { value: "viewers-low", label: "시청자가 적은 순" },
    { value: "latest", label: "최신순" },
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

  return (
    <div className="container mx-auto px-4 py-8">
      {process.env.NODE_ENV === "development" && (
        <div className="mb-4">
          <Button variant="outline" size="sm" onClick={handleNormalizeData}>
            데이터 정리
          </Button>
        </div>
      )}

      <div className="mb-10">
        <FilterTabs
          tabs={[
            { label: "전체", value: "all" },
            { label: "예약", value: "reserved" },
            { label: "방송 중", value: "live" },
            { label: "VOD", value: "vod" },
          ]}
          activeTab={activeTab}
          onChange={(value) => {
            setActiveTab(value as TabType)
            setVisibleCount(12)
          }}
        />
      </div>

      {activeTab === "all" && (
        <div className="space-y-12">
          <section>
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-2xl font-bold">방송 중</h2>
              <Button
                variant="ghost"
                size="sm"
                onClick={() => {
                  setActiveTab("live")
                  setVisibleCount(12)
                }}
              >
                <Plus className="h-4 w-4 mr-1" />더 보기
              </Button>
            </div>
            {liveBroadcasts.length > 0 ? (
              <BroadcastCarousel broadcasts={liveBroadcasts.slice(0, 5)} variant="live" />
            ) : (
              <div className="flex w-full justify-center">
                <EmptyState message="현재 진행 중인 방송이 없습니다." />
              </div>
            )}
          </section>

          <section>
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-2xl font-bold">예약</h2>
              <Button
                variant="ghost"
                size="sm"
                onClick={() => {
                  setActiveTab("reserved")
                  setVisibleCount(12)
                }}
              >
                <Plus className="h-4 w-4 mr-1" />더 보기
              </Button>
            </div>
            {reservedBroadcasts.length > 0 ? (
              <BroadcastCarousel broadcasts={reservedBroadcasts.slice(0, 5)} variant="reservation" />
            ) : (
              <div className="flex w-full justify-center">
                <EmptyState message="현재 예약이 존재하지 않습니다." />
              </div>
            )}
          </section>

          <section>
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-2xl font-bold">VOD</h2>
              <Button
                variant="ghost"
                size="sm"
                onClick={() => {
                  setActiveTab("vod")
                  setVisibleCount(12)
                }}
              >
                <Plus className="h-4 w-4 mr-1" />더 보기
              </Button>
            </div>
            {vodBroadcasts.length > 0 ? (
              <BroadcastCarousel broadcasts={vodBroadcasts.slice(0, 5)} variant="vod" />
            ) : (
              <div className="flex w-full justify-center">
                <EmptyState message="현재 VOD가 존재하지 않습니다." />
              </div>
            )}
          </section>
        </div>
      )}

      {activeTab === "reserved" && (
        <>
          <div className="flex items-center justify-between mb-6">
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

              <CategoryFilter value={reservedCategory} onChange={setReservedCategory} />

              <SortSelect value={reservedSort} onChange={setReservedSort} options={reservedSortOptions} />

              {process.env.NODE_ENV === "development" && (
                <Button variant="outline" size="sm" onClick={handleVerifySort}>
                  정렬 점검
                </Button>
              )}
            </FilterBar>
          </div>

          {getFilteredReservedBroadcasts().length > 0 ? (
            <>
              <div className="grid grid-cols-4 gap-4">
                {getFilteredReservedBroadcasts()
                  .slice(0, visibleCount)
                  .map((broadcast) => (
                    <BroadcastCard key={broadcast.id} broadcast={broadcast} variant="reservation" />
                  ))}
              </div>

              {getFilteredReservedBroadcasts().length > visibleCount && (
                <div className="flex justify-center mt-8">
                  <Button variant="outline" size="lg" onClick={() => setVisibleCount((prev) => prev + 12)}>
                    더 보기
                  </Button>
                </div>
              )}
            </>
          ) : (
            <div className="flex w-full justify-center">
              <EmptyState message="예약 방송이 없습니다." />
            </div>
          )}
        </>
      )}

      {activeTab === "live" && (
        <>
          <div className="flex items-center justify-between mb-6">
            <h1 className="text-2xl font-bold">방송 중</h1>
            <FilterBar>
              <CategoryFilter value={liveCategory} onChange={setLiveCategory} />
              <SortSelect value={liveSort} onChange={setLiveSort} options={liveSortOptions} />
            </FilterBar>
          </div>

          {getFilteredLiveBroadcasts().length > 0 ? (
            <>
              <div className="grid grid-cols-4 gap-4">
                {getFilteredLiveBroadcasts()
                  .slice(0, visibleCount)
                  .map((broadcast) => (
                    <BroadcastCard key={broadcast.id} broadcast={broadcast} variant="live" />
                  ))}
              </div>

              {getFilteredLiveBroadcasts().length > visibleCount && (
                <div className="flex justify-center mt-8">
                  <Button variant="outline" size="lg" onClick={() => setVisibleCount((prev) => prev + 12)}>
                    더 보기
                  </Button>
                </div>
              )}
            </>
          ) : (
            <div className="flex w-full justify-center">
              <EmptyState message="현재 진행 중인 방송이 없습니다." />
            </div>
          )}
        </>
      )}

      {activeTab === "vod" && (
        <>
          <div className="flex items-center justify-between mb-6">
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

          {getFilteredVodBroadcasts().length > 0 ? (
            <>
              <div className="grid grid-cols-4 gap-4">
                {getFilteredVodBroadcasts()
                  .slice(0, visibleCount)
                  .map((broadcast) => (
                    <BroadcastCard key={broadcast.id} broadcast={broadcast} variant="vod" />
                  ))}
              </div>

              {getFilteredVodBroadcasts().length > visibleCount && (
                <div className="flex justify-center mt-8">
                  <Button variant="outline" size="lg" onClick={() => setVisibleCount((prev) => prev + 12)}>
                    더 보기
                  </Button>
                </div>
              )}
            </>
          ) : (
            <div className="flex w-full justify-center">
              <EmptyState message="VOD가 없습니다." />
            </div>
          )}
        </>
      )}
    </div>
  )
}
