import Link from "next/link"
import Image from "next/image"
import { Card, CardContent } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Star } from "lucide-react"
import type { Product } from "@/lib/products-data"

type ProductCardListProps = {
  product: Product
}

export function ProductCardList({ product }: ProductCardListProps) {
  return (
    <Link href={`/products/${product.id}`}>
      <Card className={`overflow-hidden hover:shadow-lg transition-all ${product.isSoldOut ? "opacity-60" : ""}`}>
        <div className="relative aspect-square">
          <Image src={product.imageUrl || "/placeholder.svg"} alt={product.name} fill className="object-cover" />
          {product.discountRate && <Badge className="absolute top-2 left-2 bg-red-500">{product.discountRate}%</Badge>}
          {product.isSoldOut && <Badge className="absolute top-2 right-2 bg-gray-500">품절</Badge>}
        </div>
        <CardContent className="p-4">
          <h3 className="font-semibold text-lg mb-1 line-clamp-1">{product.name}</h3>
          <p className="text-sm text-muted-foreground mb-2 line-clamp-1">{product.shortDescription}</p>

          <div className="flex items-center gap-2 mb-2">
            <div className="flex items-center gap-1">
              <Star className="h-4 w-4 fill-yellow-400 text-yellow-400" />
              <span className="text-sm font-medium">{product.rating}</span>
            </div>
            <span className="text-xs text-muted-foreground">({product.reviewCount?.toLocaleString()})</span>
          </div>

          <div className="flex items-center gap-2">
            {product.originalPrice && (
              <span className="text-sm text-muted-foreground line-through">
                {product.originalPrice.toLocaleString()}원
              </span>
            )}
            <span className="text-xl font-bold">{product.price.toLocaleString()}원</span>
          </div>
        </CardContent>
      </Card>
    </Link>
  )
}
