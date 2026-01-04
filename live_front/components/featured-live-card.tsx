"use client"

import { Card } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import Image from "next/image"

interface FeaturedLiveCardProps {
  title: string
  description: string
  thumbnailUrl: string
  isLive: boolean
}

export function FeaturedLiveCard({ title, description, thumbnailUrl, isLive }: FeaturedLiveCardProps) {
  return (
    <Card className="overflow-hidden cursor-pointer transition-all hover:shadow-lg">
      <div className="grid md:grid-cols-2 gap-6 p-6">
        {/* Left: Thumbnail */}
        <div className="relative aspect-[4/5] rounded-lg overflow-hidden bg-muted">
          <Image src={thumbnailUrl || "/placeholder.svg"} alt={title} fill className="object-cover" />
          {isLive && <Badge className="absolute top-3 left-3 bg-red-500 text-white">LIVE</Badge>}
        </div>

        {/* Right: Content */}
        <div className="flex flex-col justify-center gap-4">
          <div className="space-y-2">
            <h3 className="text-2xl font-bold text-balance">{title}</h3>
            <p className="text-muted-foreground text-balance">{description}</p>
          </div>

          <div className="space-y-3">
            <p className="text-sm font-medium text-foreground">{isLive ? "현재 방송 중" : "방송 예정"}</p>
            <Button size="lg" className="w-full md:w-auto bg-orange-500 hover:bg-orange-600 text-white">
              보러가기
            </Button>
          </div>
        </div>
      </div>
    </Card>
  )
}
