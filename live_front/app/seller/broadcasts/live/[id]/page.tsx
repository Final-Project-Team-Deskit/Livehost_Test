"use client"

import { useState } from "react"
import { useRouter } from "next/navigation"
import { Pin, MessageSquare, Settings, Power } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import { ConfirmModal } from "@/components/admin/confirm-modal"
import { sellerProducts } from "@/lib/seller-mock-data"

export default function SellerStudioPage() {
  const router = useRouter()
  const [endModalOpen, setEndModalOpen] = useState(false)
  const [pinnedProduct, setPinnedProduct] = useState<string | null>(null)

  const handleEndBroadcast = () => {
    alert("방송이 종료되었습니다.")
    router.push("/seller/broadcasts")
  }

  return (
    <div className="h-screen flex flex-col bg-black">
      <div className="bg-background border-b p-4 flex items-center justify-between">
        <h1 className="font-semibold">방송 스튜디오</h1>
        <div className="flex gap-2">
          <Button variant="outline" size="sm">
            <Settings className="mr-2 h-4 w-4" />
            기본 정보 수정
          </Button>
          <Button variant="outline" size="sm">
            큐 카드 보기
          </Button>
          <Button variant="destructive" size="sm" onClick={() => setEndModalOpen(true)}>
            <Power className="mr-2 h-4 w-4" />
            방송 종료
          </Button>
        </div>
      </div>

      <div className="flex-1 flex overflow-hidden">
        <Card className="w-80 m-4 p-4 space-y-4 overflow-y-auto">
          <h3 className="font-semibold">상품 관리</h3>
          {sellerProducts.map((product) => (
            <div key={product.id} className="flex items-center gap-2 p-2 border rounded">
              <div className="w-12 h-12 bg-muted rounded" />
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium truncate">{product.name}</p>
                <p className="text-xs text-muted-foreground">{product.price.toLocaleString()}원</p>
              </div>
              <Button
                size="sm"
                variant={pinnedProduct === product.id ? "default" : "outline"}
                onClick={() => setPinnedProduct(product.id === pinnedProduct ? null : product.id)}
              >
                <Pin className="h-3 w-3" />
              </Button>
            </div>
          ))}
        </Card>

        <div className="flex-1 flex items-center justify-center m-4">
          <div className="aspect-video w-full max-w-4xl bg-gray-900 rounded-lg flex items-center justify-center">
            <p className="text-white">송출 화면</p>
          </div>
        </div>

        <Card className="w-80 m-4 p-4 space-y-4 flex flex-col">
          <h3 className="font-semibold flex items-center gap-2">
            <MessageSquare className="h-4 w-4" />
            실시간 채팅
          </h3>
          <div className="flex-1 space-y-2 overflow-y-auto">
            {Array.from({ length: 20 }, (_, i) => (
              <div key={i} className="text-sm">
                <span className="font-medium">시청자{i + 1}:</span> 안녕하세요!
              </div>
            ))}
          </div>
        </Card>
      </div>

      <ConfirmModal
        open={endModalOpen}
        onClose={() => setEndModalOpen(false)}
        onConfirm={handleEndBroadcast}
        title="방송 종료"
        message="방송 종료 시 송출이 중단되며, 시청자 화면은 대기화면으로 전환됩니다."
      />
    </div>
  )
}
