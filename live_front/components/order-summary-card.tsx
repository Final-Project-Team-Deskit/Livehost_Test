"use client"

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"

type OrderSummaryCardProps = {
  totalProductPrice: number
  shippingFee: number
  grandTotal: number
  selectedCount: number
  onCheckout: () => void
}

export function OrderSummaryCard({
  totalProductPrice,
  shippingFee,
  grandTotal,
  selectedCount,
  onCheckout,
}: OrderSummaryCardProps) {
  return (
    <Card className="sticky top-20">
      <CardHeader>
        <CardTitle>주문 예상 금액</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="space-y-2">
          <div className="flex justify-between text-sm">
            <span>총 상품 가격</span>
            <span>{totalProductPrice.toLocaleString()}원</span>
          </div>
          <div className="flex justify-between text-sm">
            <span>배송비</span>
            <span>{shippingFee === 0 ? "무료" : `${shippingFee.toLocaleString()}원`}</span>
          </div>
          {shippingFee > 0 && <p className="text-xs text-muted-foreground">50,000원 이상 구매 시 무료배송</p>}
        </div>

        <div className="border-t pt-4">
          <div className="flex justify-between items-center text-lg font-bold">
            <span>총 결제 금액</span>
            <span className="text-primary">{grandTotal.toLocaleString()}원</span>
          </div>
        </div>

        <Button className="w-full" size="lg" disabled={selectedCount === 0} onClick={onCheckout}>
          총 {selectedCount}개 상품 구매하기
        </Button>
      </CardContent>
    </Card>
  )
}
