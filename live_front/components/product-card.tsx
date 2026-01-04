"use client"

import { Card, CardContent } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import Image from "next/image"
import { ShoppingCart, Eye } from "lucide-react"

interface ProductCardProps {
  name: string
  imageUrl: string
  price: number
  originalPrice?: number
  discountRate?: number
  rating?: number
  reviewCount?: number
  isSoldOut?: boolean
  onClick?: () => void
  onAddToCart?: () => void
}

export function ProductCard({
  name,
  imageUrl,
  price,
  originalPrice,
  discountRate,
  rating,
  reviewCount,
  isSoldOut,
  onClick,
  onAddToCart,
}: ProductCardProps) {
  return (
    <Card
      className={`overflow-hidden transition-all hover:shadow-lg ${isSoldOut ? "opacity-70" : "hover:scale-[1.02]"}`}
    >
      <div className="relative aspect-square bg-muted">
        <Image src={imageUrl || "/placeholder.svg"} alt={name} fill className="object-cover" />
        {discountRate && discountRate > 0 && (
          <Badge className="absolute top-3 left-3 bg-orange-500 hover:bg-orange-600">{discountRate}%</Badge>
        )}
        {isSoldOut && <Badge className="absolute top-3 right-3 bg-red-500 hover:bg-red-600">품절</Badge>}
      </div>
      <CardContent className="p-4 space-y-3">
        <h4 className="font-semibold text-balance line-clamp-2 min-h-[3rem]">{name}</h4>

        {/* Rating */}
        {rating !== undefined && reviewCount !== undefined && (
          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            <span className="text-yellow-500">★</span>
            <span className="font-medium">{rating.toFixed(1)}</span>
            <span>({reviewCount.toLocaleString()})</span>
          </div>
        )}

        {/* Price */}
        <div className="space-y-1">
          {originalPrice && originalPrice > price && (
            <p className="text-sm text-muted-foreground line-through">{originalPrice.toLocaleString()}원</p>
          )}
          <p className="text-xl font-bold text-orange-600">{price.toLocaleString()}원</p>
        </div>

        {/* Buttons */}
        <div className="flex gap-2 pt-2">
          <Button className="flex-1" variant="default" onClick={onClick}>
            <Eye className="w-4 h-4 mr-2" />
            상품 보기
          </Button>
          <Button
            variant="outline"
            size="icon"
            disabled={isSoldOut}
            onClick={(e) => {
              e.stopPropagation()
              onAddToCart?.()
            }}
            aria-label="Add to cart"
          >
            <ShoppingCart className="w-4 h-4" />
          </Button>
        </div>
      </CardContent>
    </Card>
  )
}
