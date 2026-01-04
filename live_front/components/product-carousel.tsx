"use client"

import { ProductCard } from "./product-card"
import { Button } from "@/components/ui/button"
import { ChevronLeft, ChevronRight } from "lucide-react"
import { useRef } from "react"

interface Product {
  id: string
  name: string
  imageUrl: string
  price: number
  originalPrice?: number
  discountRate?: number
  rating?: number
  reviewCount?: number
  isSoldOut?: boolean
}

interface ProductCarouselProps {
  products: Product[]
}

export function ProductCarousel({ products }: ProductCarouselProps) {
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
        aria-label="Previous products"
      >
        <ChevronLeft className="h-4 w-4" />
      </Button>

      {/* Carousel Container */}
      <div
        ref={scrollRef}
        className="flex gap-4 overflow-x-auto scrollbar-hide snap-x snap-mandatory scroll-smooth"
        style={{ scrollbarWidth: "none", msOverflowStyle: "none" }}
      >
        {products.map((product) => (
          <div key={product.id} className="flex-none w-[320px] snap-start">
            <ProductCard
              name={product.name}
              imageUrl={product.imageUrl}
              price={product.price}
              originalPrice={product.originalPrice}
              discountRate={product.discountRate}
              rating={product.rating}
              reviewCount={product.reviewCount}
              isSoldOut={product.isSoldOut}
              onClick={() => console.log(`Navigate to product ${product.id}`)}
              onAddToCart={() => console.log(`Add product ${product.id} to cart`)}
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
        aria-label="Next products"
      >
        <ChevronRight className="h-4 w-4" />
      </Button>
    </div>
  )
}
