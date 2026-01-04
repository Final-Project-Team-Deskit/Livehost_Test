"use client"

import { Card } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import Image from "next/image"
import { useEffect, useState } from "react"

interface LiveCardProps {
  title: string
  description: string
  thumbnailUrl: string
  isLive: boolean
  viewerCount: number
  liveStartedAt: Date
  isActive?: boolean
}

export function LiveCard({
  title,
  description,
  thumbnailUrl,
  isLive,
  viewerCount,
  liveStartedAt,
  isActive = false,
}: LiveCardProps) {
  const [elapsedTime, setElapsedTime] = useState("")

  useEffect(() => {
    if (!isLive) return

    const updateTimer = () => {
      const now = new Date().getTime()
      const started = new Date(liveStartedAt).getTime()
      const diff = Math.floor((now - started) / 1000)

      const hours = Math.floor(diff / 3600)
      const minutes = Math.floor((diff % 3600) / 60)
      const seconds = diff % 60

      setElapsedTime(
        `${String(hours).padStart(2, "0")}:${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`,
      )
    }

    updateTimer()
    const interval = setInterval(updateTimer, 1000)

    return () => clearInterval(interval)
  }, [isLive, liveStartedAt])

  return (
    <Card
      className={`overflow-hidden cursor-pointer transition-all duration-300 flex-shrink-0 ${
        isActive ? "shadow-xl scale-100" : "scale-95 opacity-80"
      } hover:shadow-2xl`}
    >
      <div className="flex flex-col md:flex-row gap-6 p-6 h-full">
        {/* Left: Thumbnail */}
        <div className="relative w-full md:w-[320px] aspect-[16/10] rounded-lg overflow-hidden bg-muted flex-shrink-0">
          <Image src={thumbnailUrl || "/placeholder.svg"} alt={title} fill className="object-cover" />

          {/* Live badge and timer overlay */}
          {isLive && (
            <div className="absolute top-3 left-3 flex items-center gap-2">
              <Badge className="bg-red-500 text-white font-semibold px-2 py-1">LIVE</Badge>
              <div className="bg-black/70 backdrop-blur-sm text-white text-xs font-medium px-2 py-1 rounded">
                {elapsedTime} 진행중
              </div>
            </div>
          )}
        </div>

        {/* Right: Content */}
        <div className="flex flex-col justify-center gap-4 flex-1">
          <div className="space-y-2">
            <h3 className="text-2xl font-bold text-balance leading-tight">{title}</h3>
            <p className="text-muted-foreground text-balance">{description}</p>
          </div>

          <div className="space-y-3">
            <p className="text-sm font-medium text-foreground">
              현재 방송 중 · 시청자 {viewerCount.toLocaleString()}명
            </p>
            <Button size="lg" className="w-full md:w-auto bg-orange-500 hover:bg-orange-600 text-white font-semibold">
              보러가기
            </Button>
          </div>
        </div>
      </div>
    </Card>
  )
}
