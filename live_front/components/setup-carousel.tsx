"use client"

import { SetupCard } from "./setup-card"
import { Button } from "@/components/ui/button"
import { ChevronLeft, ChevronRight } from "lucide-react"
import { useRef } from "react"

interface Setup {
  id: string
  title: string
  description: string
  imageUrl: string
}

interface SetupCarouselProps {
  setups: Setup[]
}

export function SetupCarousel({ setups }: SetupCarouselProps) {
  const scrollRef = useRef<HTMLDivElement>(null)

  const scroll = (direction: "left" | "right") => {
    if (scrollRef.current) {
      const scrollAmount = 360
      scrollRef.current.scrollBy({
        left: direction === "left" ? -scrollAmount : scrollAmount,
        behavior: "smooth",
      })
    }
  }

  return (
    <div className="relative group">
      {/* Left Arrow */}
      <Button
        variant="outline"
        size="icon"
        className="absolute left-0 top-1/2 -translate-y-1/2 z-10 opacity-0 group-hover:opacity-100 transition-opacity bg-background/95 backdrop-blur-sm shadow-lg"
        onClick={() => scroll("left")}
        aria-label="Previous setups"
      >
        <ChevronLeft className="h-4 w-4" />
      </Button>

      {/* Carousel Container */}
      <div
        ref={scrollRef}
        className="flex gap-4 overflow-x-auto scrollbar-hide snap-x snap-mandatory scroll-smooth"
        style={{ scrollbarWidth: "none", msOverflowStyle: "none" }}
      >
        {setups.map((setup) => (
          <div key={setup.id} className="flex-none w-[320px] snap-start">
            <SetupCard
              title={setup.title}
              description={setup.description}
              imageUrl={setup.imageUrl}
              onClick={() => console.log(`Navigate to setup ${setup.id}`)}
            />
          </div>
        ))}
      </div>

      {/* Right Arrow */}
      <Button
        variant="outline"
        size="icon"
        className="absolute right-0 top-1/2 -translate-y-1/2 z-10 opacity-0 group-hover:opacity-100 transition-opacity bg-background/95 backdrop-blur-sm shadow-lg"
        onClick={() => scroll("right")}
        aria-label="Next setups"
      >
        <ChevronRight className="h-4 w-4" />
      </Button>
    </div>
  )
}
