"use client"

import { useState } from "react"
import { ChevronLeft, ChevronRight } from "lucide-react"
import { Button } from "@/components/ui/button"
import { BroadcastCard } from "./broadcast-card"
import type { Broadcast } from "@/lib/admin-mock-data"

interface BroadcastCarouselProps {
  broadcasts: Broadcast[]
  variant?: "live" | "reservation" | "vod"
  namespace?: "admin" | "seller"
  onReadyStart?: (broadcastId: string) => void
}

export function BroadcastCarousel({ broadcasts, variant, namespace = "admin", onReadyStart }: BroadcastCarouselProps) {
  const [activeIndex, setActiveIndex] = useState(0)

  const handlePrev = () => {
    setActiveIndex((prev) => (prev - 1 + broadcasts.length) % broadcasts.length)
  }

  const handleNext = () => {
    setActiveIndex((prev) => (prev + 1) % broadcasts.length)
  }

  const getVisibleCards = () => {
    const cards = []
    for (let i = -1; i <= 2; i++) {
      const index = (activeIndex + i + broadcasts.length) % broadcasts.length
      cards.push({ broadcast: broadcasts[index], position: i })
    }
    return cards
  }

  const visibleCards = getVisibleCards()

  return (
    <div className="relative overflow-hidden px-16">
      <div
        className="grid grid-cols-4 gap-4 transition-transform duration-500 ease-out"
        style={{
          transform: `translateX(calc(-25% + ${25}%))`,
        }}
      >
        {visibleCards.map(({ broadcast, position }) => (
          <BroadcastCard
            key={`${broadcast.id}-${position}`}
            broadcast={broadcast}
            variant={variant}
            isActive={position === 0}
            namespace={namespace}
            onReadyStart={onReadyStart ? () => onReadyStart(broadcast.id) : undefined}
          />
        ))}
      </div>

      {/* Navigation Arrows */}
      <Button
        variant="outline"
        size="icon"
        className="absolute left-0 top-1/2 -translate-y-1/2 bg-white/90 backdrop-blur-sm shadow-lg hover:bg-white z-10"
        onClick={handlePrev}
      >
        <ChevronLeft className="h-6 w-6" />
      </Button>
      <Button
        variant="outline"
        size="icon"
        className="absolute right-0 top-1/2 -translate-y-1/2 bg-white/90 backdrop-blur-sm shadow-lg hover:bg-white z-10"
        onClick={handleNext}
      >
        <ChevronRight className="h-6 w-6" />
      </Button>
    </div>
  )
}
