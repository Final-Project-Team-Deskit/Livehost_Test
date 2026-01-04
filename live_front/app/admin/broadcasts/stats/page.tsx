"use client"

import { useState } from "react"
import { Card } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { BarChart } from "@/components/admin/bar-chart"
import { RankList } from "@/components/admin/rank-list"
import {
  mockRevenueData,
  mockRevenuePerViewerData,
  topBroadcastsByRevenue,
  worstBroadcastsByRevenue,
  topProductsByRevenue,
  worstProductsByRevenue,
} from "@/lib/admin-mock-data"

type Metric = "daily" | "monthly" | "yearly"
type RankType = "best" | "worst"

export default function StatsPage() {
  const [revenueMetric, setRevenueMetric] = useState<Metric>("monthly")
  const [revenuePerViewerMetric, setRevenuePerViewerMetric] = useState<Metric>("monthly")
  const [broadcastRank, setBroadcastRank] = useState<RankType>("best")
  const [productRank, setProductRank] = useState<RankType>("best")

  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-2xl font-bold mb-8">방송 통계</h1>

      <div className="space-y-12">
        {/* Group 1: Charts */}
        <div className="space-y-8">
          {/* Section A: 매출 추이 */}
          <Card className="p-6">
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-lg font-semibold">매출 추이</h2>
              <div className="flex gap-2">
                <Button
                  variant={revenueMetric === "daily" ? "default" : "outline"}
                  size="sm"
                  onClick={() => setRevenueMetric("daily")}
                >
                  일별
                </Button>
                <Button
                  variant={revenueMetric === "monthly" ? "default" : "outline"}
                  size="sm"
                  onClick={() => setRevenueMetric("monthly")}
                >
                  월별
                </Button>
                <Button
                  variant={revenueMetric === "yearly" ? "default" : "outline"}
                  size="sm"
                  onClick={() => setRevenueMetric("yearly")}
                >
                  연도별
                </Button>
              </div>
            </div>
            <BarChart data={mockRevenueData[revenueMetric].slice(0, 12)} />
          </Card>

          {/* Section B: 시청자 당 매출 추이 */}
          <Card className="p-6">
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-lg font-semibold">시청자 당 매출 추이</h2>
              <div className="flex gap-2">
                <Button
                  variant={revenuePerViewerMetric === "daily" ? "default" : "outline"}
                  size="sm"
                  onClick={() => setRevenuePerViewerMetric("daily")}
                >
                  일별
                </Button>
                <Button
                  variant={revenuePerViewerMetric === "monthly" ? "default" : "outline"}
                  size="sm"
                  onClick={() => setRevenuePerViewerMetric("monthly")}
                >
                  월별
                </Button>
                <Button
                  variant={revenuePerViewerMetric === "yearly" ? "default" : "outline"}
                  size="sm"
                  onClick={() => setRevenuePerViewerMetric("yearly")}
                >
                  연도별
                </Button>
              </div>
            </div>
            <BarChart data={mockRevenuePerViewerData[revenuePerViewerMetric].slice(0, 12)} />
          </Card>
        </div>

        {/* Group 2: Rankings */}
        <div className="grid grid-cols-2 gap-6">
          {/* Section C: 방송 순위 */}
          <Card className="p-6">
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-lg font-semibold">방송 순위 TOP 5</h2>
              <div className="flex gap-2">
                <Button
                  variant={broadcastRank === "best" ? "default" : "outline"}
                  size="sm"
                  onClick={() => setBroadcastRank("best")}
                >
                  베스트
                </Button>
                <Button
                  variant={broadcastRank === "worst" ? "default" : "outline"}
                  size="sm"
                  onClick={() => setBroadcastRank("worst")}
                >
                  워스트
                </Button>
              </div>
            </div>
            <RankList items={broadcastRank === "best" ? topBroadcastsByRevenue : worstBroadcastsByRevenue} />
          </Card>

          {/* Section D: 상품 순위 */}
          <Card className="p-6">
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-lg font-semibold">상품 순위 TOP 5</h2>
              <div className="flex gap-2">
                <Button
                  variant={productRank === "best" ? "default" : "outline"}
                  size="sm"
                  onClick={() => setProductRank("best")}
                >
                  베스트
                </Button>
                <Button
                  variant={productRank === "worst" ? "default" : "outline"}
                  size="sm"
                  onClick={() => setProductRank("worst")}
                >
                  워스트
                </Button>
              </div>
            </div>
            <RankList items={productRank === "best" ? topProductsByRevenue : worstProductsByRevenue} />
          </Card>
        </div>
      </div>
    </div>
  )
}
