"use client"

import { useState } from "react"
import { Button } from "@/components/ui/button"
import { FilterBar } from "@/components/admin/filter-bar"
import { DateRangeFilter } from "@/components/admin/date-range-filter"
import { CategoryFilter } from "@/components/admin/category-filter"
import { VisibilityFilter } from "@/components/admin/visibility-filter"
import { SortSelect } from "@/components/admin/sort-select"
import { BroadcastCard } from "@/components/admin/broadcast-card"
import { EmptyState } from "@/components/admin/empty-state"
import { mockBroadcasts } from "@/lib/admin-mock-data"

export default function VodPage() {
  const [startDate, setStartDate] = useState("")
  const [endDate, setEndDate] = useState("")
  const [visibility, setVisibility] = useState("all")
  const [category, setCategory] = useState("all")
  const [sort, setSort] = useState("latest")
  const [visibleCount, setVisibleCount] = useState(12)

  const getFilteredBroadcasts = () => {
    let filtered = mockBroadcasts.filter((b) => b.status === "ENCODING" || b.status === "VOD" || b.status === "STOPED")

    if (startDate) {
      filtered = filtered.filter((b) => b.startAt >= new Date(startDate))
    }
    if (endDate) {
      filtered = filtered.filter((b) => b.startAt <= new Date(endDate))
    }

    if (visibility === "public") {
      filtered = filtered.filter((b) => b.vodVisibility === "PUBLIC")
    } else if (visibility === "private") {
      filtered = filtered.filter((b) => b.vodVisibility === "PRIVATE")
    }

    if (category !== "all") {
      filtered = filtered.filter((b) => b.category === category)
    }

    switch (sort) {
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
        filtered.sort((a, b) => b.likes - a.likes)
        break
      case "likes-low":
        filtered.sort((a, b) => a.likes - b.likes)
        break
      case "revenue-high":
        filtered.sort((a, b) => b.revenue - a.revenue)
        break
      case "revenue-low":
        filtered.sort((a, b) => a.revenue - b.revenue)
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

  const filteredBroadcasts = getFilteredBroadcasts()

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
      <h1 className="text-2xl font-bold mb-8">VOD 관리</h1>

      <FilterBar className="mb-6">
        <DateRangeFilter
          startDate={startDate}
          endDate={endDate}
          onStartDateChange={setStartDate}
          onEndDateChange={setEndDate}
        />

        <VisibilityFilter value={visibility} onChange={setVisibility} />

        <CategoryFilter value={category} onChange={setCategory} />

        <SortSelect value={sort} onChange={setSort} options={vodSortOptions} />
      </FilterBar>

      {filteredBroadcasts.length > 0 ? (
        <>
          <div className="grid grid-cols-4 gap-4">
            {filteredBroadcasts.slice(0, visibleCount).map((broadcast) => (
              <BroadcastCard key={broadcast.id} broadcast={broadcast} variant="vod" />
            ))}
          </div>

          {filteredBroadcasts.length > visibleCount && (
            <div className="flex justify-center mt-8">
              <Button variant="outline" size="lg" onClick={() => setVisibleCount((prev) => prev + 12)}>
                더 보기
              </Button>
            </div>
          )}
        </>
      ) : (
        <EmptyState message="VOD가 없습니다." />
      )}
    </div>
  )
}
