"use client"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { Header } from "@/components/header"
import { CartItemRow } from "@/components/cart-item-row"
import { OrderSummaryCard } from "@/components/order-summary-card"
import { Button } from "@/components/ui/button"
import { Checkbox } from "@/components/ui/checkbox"
import { useCart } from "@/contexts/cart-context"
import { ChevronLeft, ShoppingBag } from "lucide-react"

export default function CartPage() {
  const router = useRouter()
  const {
    items,
    removeFromCart,
    updateQuantity,
    toggleSelect,
    selectAll,
    unselectAll,
    removeSelected,
    clearAll,
    getTotals,
  } = useCart()

  const totals = getTotals()
  const allSelected = items.length > 0 && items.every((item) => item.selected)

  const handleSelectAll = () => {
    if (allSelected) {
      unselectAll()
    } else {
      selectAll()
    }
  }

  const handleCheckout = () => {
    alert("주문/결제 기능은 준비 중입니다.")
  }

  if (items.length === 0) {
    return (
      <div className="min-h-screen bg-background">
        <Header />
        <main className="container mx-auto px-4 py-12">
          <div className="text-center space-y-6 py-12">
            <ShoppingBag className="h-24 w-24 mx-auto text-muted-foreground" />
            <div>
              <h2 className="text-2xl font-bold mb-2">장바구니가 비어있습니다</h2>
              <p className="text-muted-foreground">원하는 상품을 담아보세요!</p>
            </div>
            <Link href="/products">
              <Button size="lg">상품 보러가기</Button>
            </Link>
          </div>
        </main>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-background">
      <Header />

      <main className="container mx-auto px-4 py-8 space-y-6">
        {/* Page Title */}
        <div className="flex items-center gap-2">
          <Button variant="ghost" size="icon" onClick={() => router.back()}>
            <ChevronLeft className="h-5 w-5" />
          </Button>
          <h1 className="text-2xl font-bold">장바구니 ({items.length})</h1>
        </div>

        {/* Step Indicator */}
        <div className="flex items-center justify-center gap-2 text-sm">
          <span className="font-bold text-primary">01 장바구니</span>
          <span className="text-muted-foreground">&gt;</span>
          <span className="text-muted-foreground">02 주문/결제</span>
          <span className="text-muted-foreground">&gt;</span>
          <span className="text-muted-foreground">03 주문 완료</span>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Left Column: Cart Items */}
          <div className="lg:col-span-2 space-y-4">
            <div className="space-y-4">
              {items.map((item) => (
                <CartItemRow
                  key={item.productId}
                  item={item}
                  onToggleSelect={() => toggleSelect(item.productId)}
                  onUpdateQuantity={(qty) => updateQuantity(item.productId, qty)}
                  onRemove={() => removeFromCart(item.productId)}
                />
              ))}
            </div>

            {/* Bottom Controls */}
            <div className="flex items-center justify-between pt-4 border-t">
              <div className="flex items-center gap-2">
                <Checkbox checked={allSelected} onCheckedChange={handleSelectAll} />
                <span className="text-sm font-medium">전체 선택 ({items.filter((i) => i.selected).length})</span>
              </div>

              <div className="flex gap-2">
                <Button variant="outline" onClick={removeSelected}>
                  선택 삭제
                </Button>
                <Button variant="outline" onClick={clearAll}>
                  전체 삭제
                </Button>
              </div>
            </div>
          </div>

          {/* Right Column: Order Summary */}
          <div>
            <OrderSummaryCard
              totalProductPrice={totals.totalProductPrice}
              shippingFee={totals.shippingFee}
              grandTotal={totals.grandTotal}
              selectedCount={totals.selectedCount}
              onCheckout={handleCheckout}
            />
          </div>
        </div>
      </main>
    </div>
  )
}
