"use client"

import { useState } from "react"
import { Card } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { BarChart } from "@/components/admin/bar-chart"
import {
  sellerRevenueData,
  sellerRevenuePerViewerData,
  topBroadcastsByRevenueSeller,
  worstBroadcastsByRevenueSeller,
  topBroadcastsByViewersSeller,
  worstBroadcastsByViewersSeller,
} from "@/lib/seller-mock-data"

export default function SellerStatsPage() {
  const [revenueRange, setRevenueRange] = useState<"daily" | "monthly" | "yearly">("monthly")
  const [revenuePerViewerRange, setRevenuePerViewerRange] = useState<"daily" | "monthly" | "yearly">("monthly")
  const [revenueRankingType, setRevenueRankingType] = useState<"best" | "worst">("best")
  const [viewerRankingType, setViewerRankingType] = useState<"best" | "worst">("best")

  return (
    <div className="container mx-auto px-4 py-8 space-y-8">
      <h1 className="text-3xl font-bold">방송 통계</h1>

      <div className="grid grid-cols-2 gap-6">
        <Card className="p-6 space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="font-semibold">판매자 매출</h3>
            <div className="flex gap-2">
              {(["daily", "monthly", "yearly"] as const).map((range) => (
                <Button
                  key={range}
                  size="sm"
                  variant={revenueRange === range ? "default" : "outline"}
                  onClick={() => setRevenueRange(range)}
                >
                  {range === "daily" ? "일별" : range === "monthly" ? "월별" : "연도별"}
                </Button>
              ))}
            </div>
          </div>
          <BarChart data={sellerRevenueData[revenueRange]} />
        </Card>

        <Card className="p-6 space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="font-semibold">시청자 당 매출액</h3>
            <div className="flex gap-2">
              {(["daily", "monthly", "yearly"] as const).map((range) => (
                <Button
                  key={range}
                  size="sm"
                  variant={revenuePerViewerRange === range ? "default" : "outline"}
                  onClick={() => setRevenuePerViewerRange(range)}
                >
                  {range === "daily" ? "일별" : range === "monthly" ? "월별" : "연도별"}
                </Button>
              ))}
            </div>
          </div>
          <BarChart data={sellerRevenuePerViewerData[revenuePerViewerRange]} />
        </Card>
      </div>

      <div className="grid grid-cols-2 gap-6">
        <Card className="p-6 space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="font-semibold">월별 베스트/워스트 매출 방송 5순위</h3>
            <div className="flex gap-2">
              <Button
                size="sm"
                variant={revenueRankingType === "best" ? "default" : "outline"}
                onClick={() => setRevenueRankingType("best")}
              >
                베스트
              </Button>
              <Button
                size="sm"
                variant={revenueRankingType === "worst" ? "default" : "outline"}
                onClick={() => setRevenueRankingType("worst")}
              >
                워스트
              </Button>
            </div>
          </div>
          <div className="space-y-2">
            {(revenueRankingType === "best" ? topBroadcastsByRevenueSeller : worstBroadcastsByRevenueSeller).map(
              (item) => (
                <div key={item.rank} className="flex items-center justify-between p-2 border rounded">
                  <div className="flex items-center gap-2">
                    <span className="font-bold text-lg w-6">{item.rank}</span>
                    <span className="text-sm">{item.title}</span>
                  </div>
                  <span className="text-sm font-medium">{item.value.toLocaleString()}원</span>
                </div>
              ),
            )}
          </div>
        </Card>

        <Card className="p-6 space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="font-semibold">월별 베스트/워스트 시청자 수 5순위</h3>
            <div className="flex gap-2">
              <Button
                size="sm"
                variant={viewerRankingType === "best" ? "default" : "outline"}
                onClick={() => setViewerRankingType("best")}
              >
                베스트
              </Button>
              <Button
                size="sm"
                variant={viewerRankingType === "worst" ? "default" : "outline"}
                onClick={() => setViewerRankingType("worst")}
              >
                워스트
              </Button>
            </div>
          </div>
          <div className="space-y-2">
            {(viewerRankingType === "best" ? topBroadcastsByViewersSeller : worstBroadcastsByViewersSeller).map(
              (item) => (
                <div key={item.rank} className="flex items-center justify-between p-2 border rounded">
                  <div className="flex items-center gap-2">
                    <span className="font-bold text-lg w-6">{item.rank}</span>
                    <span className="text-sm">{item.title}</span>
                  </div>
                  <span className="text-sm font-medium">{item.value.toLocaleString()}명</span>
                </div>
              ),
            )}
          </div>
        </Card>
      </div>
    </div>
  )
}
