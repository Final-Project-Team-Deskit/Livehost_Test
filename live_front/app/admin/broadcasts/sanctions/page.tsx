"use client"

import { useState } from "react"
import { Card } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { BarChart } from "@/components/admin/bar-chart"
import {
  mockStopCountData,
  mockViewerSanctionData,
  topSellersBySanctions,
  topViewersBySanctions,
} from "@/lib/admin-mock-data"

type Metric = "daily" | "monthly" | "yearly"

export default function SanctionsPage() {
  const [stopMetric, setStopMetric] = useState<Metric>("daily")
  const [viewerMetric, setViewerMetric] = useState<Metric>("daily")

  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-2xl font-bold mb-8">제재 관리</h1>

      <div className="space-y-8">
        {/* Section A: 송출 중지 횟수 */}
        <Card className="p-6">
          <div className="flex items-center justify-between mb-6">
            <h2 className="text-lg font-semibold">송출 중지 횟수</h2>
            <div className="flex gap-2">
              <Button
                variant={stopMetric === "daily" ? "default" : "outline"}
                size="sm"
                onClick={() => setStopMetric("daily")}
              >
                일별
              </Button>
              <Button
                variant={stopMetric === "monthly" ? "default" : "outline"}
                size="sm"
                onClick={() => setStopMetric("monthly")}
              >
                월별
              </Button>
              <Button
                variant={stopMetric === "yearly" ? "default" : "outline"}
                size="sm"
                onClick={() => setStopMetric("yearly")}
              >
                연도별
              </Button>
            </div>
          </div>
          <BarChart data={mockStopCountData[stopMetric].slice(0, 10)} />
        </Card>

        {/* Section B: 시청자 제재 횟수 */}
        <Card className="p-6">
          <div className="flex items-center justify-between mb-6">
            <h2 className="text-lg font-semibold">시청자 제재 횟수</h2>
            <div className="flex gap-2">
              <Button
                variant={viewerMetric === "daily" ? "default" : "outline"}
                size="sm"
                onClick={() => setViewerMetric("daily")}
              >
                일별
              </Button>
              <Button
                variant={viewerMetric === "monthly" ? "default" : "outline"}
                size="sm"
                onClick={() => setViewerMetric("monthly")}
              >
                월별
              </Button>
              <Button
                variant={viewerMetric === "yearly" ? "default" : "outline"}
                size="sm"
                onClick={() => setViewerMetric("yearly")}
              >
                연도별
              </Button>
            </div>
          </div>
          <BarChart data={mockViewerSanctionData[viewerMetric].slice(0, 10)} />
        </Card>

        {/* Section C & D: Rankings */}
        <div className="grid grid-cols-2 gap-6">
          <Card className="p-6">
            <h2 className="text-lg font-semibold mb-6">송출 중지 횟수 많은 판매자 TOP 5</h2>
            <div className="space-y-3">
              {topSellersBySanctions.map((seller, i) => (
                <div key={i} className="flex items-center gap-4 p-3 bg-muted rounded-lg">
                  <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary text-primary-foreground font-bold text-sm">
                    {i + 1}
                  </div>
                  <div className="flex-1 font-medium">{seller.name}</div>
                  <div className="text-sm font-semibold">{seller.count}회</div>
                </div>
              ))}
            </div>
          </Card>

          <Card className="p-6">
            <h2 className="text-lg font-semibold mb-6">제재 당한 횟수 많은 시청자 TOP 5</h2>
            <div className="space-y-3">
              {topViewersBySanctions.map((viewer, i) => (
                <div key={i} className="flex items-center gap-4 p-3 bg-muted rounded-lg">
                  <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary text-primary-foreground font-bold text-sm">
                    {i + 1}
                  </div>
                  <div className="flex-1 font-medium">{viewer.name}</div>
                  <div className="text-sm font-semibold">{viewer.count}회</div>
                </div>
              ))}
            </div>
          </Card>
        </div>
      </div>
    </div>
  )
}
