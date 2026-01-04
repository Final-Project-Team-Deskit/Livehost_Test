"use client"

import { useState } from "react"
import { Search } from "lucide-react"
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Checkbox } from "@/components/ui/checkbox"
import { sellerProducts } from "@/lib/seller-mock-data"
import type { Product } from "@/lib/admin-mock-data"
import Image from "next/image"

interface ProductPickerModalProps {
  open: boolean
  onClose: () => void
  onSave?: (products: Product[]) => void
  currentlySelected?: Product[]
}

export function ProductPickerModal({ open, onClose, onSave, currentlySelected = [] }: ProductPickerModalProps) {
  const [searchQuery, setSearchQuery] = useState("")
  const [selectedIds, setSelectedIds] = useState<string[]>(currentlySelected.map((p) => p.id))

  const filteredProducts = sellerProducts.filter((p) => p.name.toLowerCase().includes(searchQuery.toLowerCase()))

  const handleToggle = (id: string) => {
    setSelectedIds((prev) => {
      if (prev.includes(id)) {
        return prev.filter((pid) => pid !== id)
      } else {
        if (prev.length >= 10) {
          alert("최대 10개까지만 선택할 수 있습니다.")
          return prev
        }
        return [...prev, id]
      }
    })
  }

  const handleSave = () => {
    if (selectedIds.length < 1) {
      alert("최소 1개 이상의 상품을 선택해주세요.")
      return
    }
    if (selectedIds.length > 10) {
      alert("최대 10개까지만 선택할 수 있습니다.")
      return
    }
    const selected = sellerProducts.filter((p) => selectedIds.includes(p.id))
    onSave?.(selected)
    onClose()
  }

  const handleClose = () => {
    setSearchQuery("")
    setSelectedIds(currentlySelected.map((p) => p.id))
    onClose()
  }

  return (
    <Dialog open={open} onOpenChange={handleClose}>
      <DialogContent className="max-w-2xl max-h-[80vh] flex flex-col">
        <DialogHeader>
          <DialogTitle>판매상품 불러오기</DialogTitle>
        </DialogHeader>

        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="상품 검색"
            className="pl-9"
          />
        </div>

        <div className="flex-1 overflow-y-auto space-y-2 border rounded-lg p-4">
          {filteredProducts.map((product) => (
            <div
              key={product.id}
              className="flex items-center gap-3 p-3 border rounded-lg hover:bg-muted/50 transition-colors"
            >
              <Checkbox checked={selectedIds.includes(product.id)} onCheckedChange={() => handleToggle(product.id)} />
              <div className="w-12 h-12 bg-muted rounded flex-shrink-0 relative overflow-hidden">
                {product.imageUrl && (
                  <Image
                    src={product.imageUrl || "/placeholder.svg"}
                    alt={product.name}
                    fill
                    className="object-cover"
                  />
                )}
              </div>
              <div className="flex-1 min-w-0">
                <p className="font-medium truncate">{product.name}</p>
                <div className="flex gap-4 text-sm text-muted-foreground">
                  <span>판매가: {product.price.toLocaleString()}원</span>
                  <span>실재고: {product.stock}</span>
                </div>
              </div>
            </div>
          ))}
        </div>

        <DialogFooter className="flex items-center justify-between">
          <p className="text-sm text-muted-foreground">{selectedIds.length}개 선택됨 (최소 1개, 최대 10개)</p>
          <div className="flex gap-2">
            <Button variant="outline" onClick={handleClose}>
              취소
            </Button>
            <Button onClick={handleSave}>저장</Button>
          </div>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
