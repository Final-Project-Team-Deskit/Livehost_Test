"use client"

import Image from "next/image"
import { Checkbox } from "@/components/ui/checkbox"
import { Button } from "@/components/ui/button"
import { QuantitySelector } from "@/components/quantity-selector"
import { X } from "lucide-react"
import type { CartItem } from "@/contexts/cart-context"

type CartItemRowProps = {
  item: CartItem
  onToggleSelect: () => void
  onUpdateQuantity: (quantity: number) => void
  onRemove: () => void
}

export function CartItemRow({ item, onToggleSelect, onUpdateQuantity, onRemove }: CartItemRowProps) {
  return (
    <div className="flex items-center gap-4 p-4 border rounded-lg">
      <Checkbox checked={item.selected} onCheckedChange={onToggleSelect} />

      <div className="relative w-24 h-24 rounded overflow-hidden flex-shrink-0">
        <Image src={item.imageUrl || "/placeholder.svg"} alt={item.name} fill className="object-cover" />
      </div>

      <div className="flex-1">
        <h3 className="font-semibold mb-1">{item.name}</h3>
        <div className="flex items-center gap-2">
          {item.originalPrice && (
            <span className="text-sm text-muted-foreground line-through">{item.originalPrice.toLocaleString()}원</span>
          )}
          <span className="font-bold">{item.price.toLocaleString()}원</span>
          {item.discountRate && <span className="text-sm text-red-500">({item.discountRate}% 할인)</span>}
        </div>
      </div>

      <QuantitySelector value={item.quantity} onChange={onUpdateQuantity} />

      <Button variant="ghost" size="icon" onClick={onRemove}>
        <X className="h-4 w-4" />
      </Button>
    </div>
  )
}
