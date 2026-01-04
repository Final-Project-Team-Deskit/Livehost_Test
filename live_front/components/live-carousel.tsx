"use client"

import { useState, useEffect, useRef } from "react"
import { LiveCard } from "@/components/live-card"
import { Button } from "@/components/ui/button"
import { ChevronLeft, ChevronRight } from "lucide-react"

interface LiveItem {
  id: string
  title: string
  description: string
  thumbnailUrl: string
  isLive: boolean
  viewerCount: number
  liveStartedAt: Date
}

interface LiveCarouselProps {
  items: LiveItem[]
}

export function LiveCarousel({ items }: LiveCarouselProps) {
  const [currentIndex, setCurrentIndex] = useState(0)
  const carouselRef = useRef<HTMLDivElement>(null)

  const goToNext = () => {
    setCurrentIndex((prev) => (prev + 1) % items.length)
  }

  const goToPrev = () => {
    setCurrentIndex((prev) => (prev - 1 + items.length) % items.length)
  }

  // Keyboard navigation
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "ArrowLeft") {
        goToPrev()
      } else if (e.key === "ArrowRight") {
        goToNext()
      }
    }

    window.addEventListener("keydown", handleKeyDown)
    return () => window.removeEventListener("keydown", handleKeyDown)
  }, [])

  // Auto-scroll to center active card
  useEffect(() => {
    if (carouselRef.current) {
      const container = carouselRef.current
      const cardWidth = container.scrollWidth / items.length
      const scrollPosition = currentIndex * cardWidth - container.clientWidth / 2 + cardWidth / 2

      container.scrollTo({
        left: scrollPosition,
        behavior: "smooth",
      })
    }
  }, [currentIndex, items.length])

  return (
    <div className="relative">
      {/* Carousel Container */}
      <div className="relative overflow-hidden">
        <div ref={carouselRef} className="flex gap-6 overflow-x-auto snap-x snap-mandatory scrollbar-hide px-4 md:px-8">
          {items.map((item, index) => (
            <div
              key={item.id}
              className="w-full md:w-[calc(100%-120px)] snap-center flex-shrink-0"
              style={{
                minWidth: "280px",
              }}
            >
              <LiveCard
                title={item.title}
                description={item.description}
                thumbnailUrl={item.thumbnailUrl}
                isLive={item.isLive}
                viewerCount={item.viewerCount}
                liveStartedAt={item.liveStartedAt}
                isActive={index === currentIndex}
              />
            </div>
          ))}
        </div>
      </div>

      {/* Navigation Arrows */}
      <Button
        variant="outline"
        size="icon"
        className="absolute left-2 top-1/2 -translate-y-1/2 z-10 bg-background/90 backdrop-blur-sm shadow-lg hidden md:flex"
        onClick={goToPrev}
        aria-label="Previous slide"
      >
        <ChevronLeft className="h-5 w-5" />
      </Button>

      <Button
        variant="outline"
        size="icon"
        className="absolute right-2 top-1/2 -translate-y-1/2 z-10 bg-background/90 backdrop-blur-sm shadow-lg hidden md:flex"
        onClick={goToNext}
        aria-label="Next slide"
      >
        <ChevronRight className="h-5 w-5" />
      </Button>

      {/* Pagination Dots */}
      <div className="flex justify-center gap-2 mt-6">
        {items.map((_, index) => (
          <button
            key={index}
            className={`w-2 h-2 rounded-full transition-all ${
              index === currentIndex ? "bg-orange-500 w-8" : "bg-muted-foreground/30"
            }`}
            onClick={() => setCurrentIndex(index)}
            aria-label={`Go to slide ${index + 1}`}
          />
        ))}
      </div>
    </div>
  )
}
