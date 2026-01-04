"use client"

import { useState } from "react"
import { Card } from "@/components/ui/card"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Button } from "@/components/ui/button"
import { BroadcastCard } from "@/components/admin/broadcast-card"
import { EmptyState } from "@/components/admin/empty-state"
import { mockBroadcasts } from "@/lib/admin-mock-data"

export default function ReservationsPage() {
  const [reservationStatus, setReservationStatus] = useState("all")
  const [category, setCategory] = useState("all")
  const [sort, setSort] = useState("nearest")
  const [visibleCount, setVisibleCount] = useState(12)

  const getFilteredBroadcasts = () => {
    let filtered = mockBroadcasts.filter((b) => b.status === "RESERVED" || b.status === "CANCELED")

    // Apply reservation status filter
    if (reservationStatus === "reserved") {
      filtered = filtered.filter((b) => b.status === "RESERVED")
    } else if (reservationStatus === "canceled") {
      filtered = filtered.filter((b) => b.status === "CANCELED")
    }

    if (category !== "all") {
      filtered = filtered.filter((b) => b.category === category)
    }

    // Apply sorting
    switch (sort) {
      case "latest":
        filtered.sort((a, b) => b.createdAt.getTime() - a.createdAt.getTime())
        break
      case "oldest":
        filtered.sort((a, b) => a.createdAt.getTime() - b.createdAt.getTime())
        break
      case "nearest":
        filtered.sort((a, b) => a.startAt.getTime() - b.startAt.getTime())
        break
    }

    return filtered
  }

  const filteredBroadcasts = getFilteredBroadcasts()

  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-2xl font-bold mb-8">예약 관리</h1>

      {/* Filter Bar */}
      <Card className="p-4 mb-6">
        <div className="flex items-center gap-4">
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
              <SelectItem value="nearest">방송 시간이 가까운 순</SelectItem>
              <SelectItem value="latest">최신순</SelectItem>
              <SelectItem value="oldest">오래된 순</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </Card>

      {/* Grid List */}
      {filteredBroadcasts.length > 0 ? (
        <>
          <div className="grid grid-cols-4 gap-4">
            {filteredBroadcasts.slice(0, visibleCount).map((broadcast) => (
              <BroadcastCard key={broadcast.id} broadcast={broadcast} variant="reservation" />
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
        <EmptyState message="예약 방송이 없습니다." />
      )}
    </div>
  )
}
