"use client"

import { useState } from "react"
import { Card } from "@/components/ui/card"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Button } from "@/components/ui/button"
import { BroadcastCard } from "@/components/admin/broadcast-card"
import { EmptyState } from "@/components/admin/empty-state"
import { mockBroadcasts } from "@/lib/admin-mock-data"

export default function LiveBroadcastsPage() {
  const [category, setCategory] = useState("all")
  const [sort, setSort] = useState("reports")
  const [visibleCount, setVisibleCount] = useState(12)

  const getFilteredBroadcasts = () => {
    let filtered = mockBroadcasts.filter((b) => b.status === "READY" || b.status === "ON_AIR" || b.status === "ENDED")

    if (category !== "all") {
      filtered = filtered.filter((b) => b.category === category)
    }

    switch (sort) {
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

  const filteredBroadcasts = getFilteredBroadcasts()

  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-2xl font-bold mb-8">방송 중 관리</h1>

      {/* Filter Bar */}
      <Card className="p-4 mb-6">
        <div className="flex items-center gap-4">
          <Select value={category} onValueChange={setCategory}>
            <SelectTrigger className="w-40">
              <SelectValue placeholder="카테고리" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">전체</SelectItem>
              <SelectItem value="가구">가구</SelectItem>
              <SelectItem value="전자기기">전자기기</SelectItem>
              <SelectItem value="악세사리">악세사리</SelectItem>
              <SelectItem value="패션">패션</SelectItem>
              <SelectItem value="뷰티">뷰티</SelectItem>
            </SelectContent>
          </Select>

          <Select value={sort} onValueChange={setSort}>
            <SelectTrigger className="w-48">
              <SelectValue placeholder="정렬" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="reports">신고 건수가 많은 순</SelectItem>
              <SelectItem value="viewers-high">시청자가 많은 순</SelectItem>
              <SelectItem value="viewers-low">시청자가 적은 순</SelectItem>
              <SelectItem value="latest">최신순</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </Card>

      {/* Grid List */}
      {filteredBroadcasts.length > 0 ? (
        <>
          <div className="grid grid-cols-4 gap-4">
            {filteredBroadcasts.slice(0, visibleCount).map((broadcast) => (
              <BroadcastCard key={broadcast.id} broadcast={broadcast} variant="live" />
            ))}
          </div>

          {/* Load More Button */}
          {filteredBroadcasts.length > visibleCount && (
            <div className="flex justify-center mt-8">
              <Button variant="outline" size="lg" onClick={() => setVisibleCount((prev) => prev + 12)}>
                더 보기
              </Button>
            </div>
          )}
        </>
      ) : (
        <div className="flex justify-center">
          <EmptyState message="현재 진행 중인 방송이 없습니다." />
        </div>
      )}
    </div>
  )
}
