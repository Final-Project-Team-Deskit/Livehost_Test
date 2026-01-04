"use client"

import { useState } from "react"
import { useRouter } from "next/navigation"
import { ArrowLeft, List, Eye } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import { sellerBroadcasts, sellerProducts } from "@/lib/seller-mock-data"
import { ConfirmModal } from "@/components/admin/confirm-modal"
import { QCardModal } from "@/components/seller/qcard-modal"
import { sellerRoutes } from "@/lib/routes"
import { isWithinReadyWindow } from "@/lib/utils"
import Image from "next/image"

export default function SellerReservationDetailPage({ params }: { params: { id: string } }) {
  const router = useRouter()
  const [cancelModalOpen, setCancelModalOpen] = useState(false)
  const [qCardModalOpen, setQCardModalOpen] = useState(false)
  const broadcast = sellerBroadcasts.find((b) => b.id === params.id)

  if (!broadcast) return <div>방송을 찾을 수 없습니다.</div>

  const isReady = broadcast.scheduledAt && isWithinReadyWindow(broadcast.scheduledAt)

  const mockQCards = [
    "오늘의 특별 할인 상품은 무엇인가요?",
    "배송은 언제 시작되나요?",
    "반품 정책에 대해 설명해주세요.",
  ]

  const handleCancel = () => {
    alert("예약이 취소되었습니다.")
    router.push(sellerRoutes.broadcasts.list())
  }

  const handleStartBroadcast = () => {
    router.push(sellerRoutes.broadcasts.studio(broadcast.id))
  }

  return (
    <div className="container mx-auto px-4 py-8 space-y-6">
      <div className="flex items-center justify-between">
        <Button variant="ghost" onClick={() => router.back()}>
          <ArrowLeft className="h-4 w-4 mr-2" />
          뒤로 가기
        </Button>
        <div className="flex gap-2">
          <Button variant="outline" size="sm" onClick={() => setQCardModalOpen(true)}>
            <Eye className="mr-2 h-4 w-4" />큐 카드 보기
          </Button>
          <Button variant="outline" onClick={() => router.push(sellerRoutes.broadcasts.list())}>
            <List className="mr-2 h-4 w-4" />
            목록으로
          </Button>
        </div>
      </div>

      <h1 className="text-2xl font-bold">예약 상세</h1>

      <Card className="p-6">
        <h2 className="text-lg font-semibold mb-4">방송 기본 정보</h2>
        <div className="flex gap-6">
          <div className="relative w-80 h-48 flex-shrink-0 rounded-lg overflow-hidden">
            <Image
              src={broadcast.thumbnailUrl || "/placeholder.svg"}
              alt={broadcast.title}
              fill
              className="object-cover"
            />
          </div>
          <div className="flex-1 space-y-3">
            <h3 className="text-xl font-bold">{broadcast.title}</h3>
            <div className="space-y-2 text-sm">
              <div className="flex gap-2">
                <span className="font-semibold w-32 text-muted-foreground">방송 예정 시간:</span>
                <span>
                  {broadcast.startAt.toLocaleDateString("ko-KR")}{" "}
                  {broadcast.startAt.toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit" })}
                </span>
              </div>
              <div className="flex gap-2">
                <span className="font-semibold w-32 text-muted-foreground">카테고리:</span>
                <span>{broadcast.category}</span>
              </div>
              <div className="flex gap-2">
                <span className="font-semibold w-32 text-muted-foreground">상태:</span>
                <span className={`font-semibold ${isReady ? "text-yellow-600" : "text-blue-600"}`}>
                  {isReady ? "방송 대기중" : "예약됨"}
                </span>
              </div>
            </div>
          </div>
        </div>

        <div className="pt-4 mt-4 border-t">
          <h3 className="font-semibold text-sm text-muted-foreground mb-2">공지사항</h3>
          <p className="text-sm leading-relaxed">
            판매 상품 외 다른 상품 문의는 받지 않습니다. 방송 중 욕설 및 비방은 제재됩니다.
          </p>
        </div>
      </Card>

      <Card className="p-6">
        <h2 className="text-lg font-semibold mb-4">썸네일 & 대기 화면</h2>
        <div className="grid md:grid-cols-2 gap-6">
          <div>
            <h3 className="text-sm font-semibold text-muted-foreground mb-2">썸네일</h3>
            <div className="relative w-full aspect-video bg-muted rounded-lg overflow-hidden">
              <Image src={broadcast.thumbnailUrl || "/placeholder.svg"} alt="썸네일" fill className="object-cover" />
            </div>
          </div>
          <div>
            <h3 className="text-sm font-semibold text-muted-foreground mb-2">대기 화면</h3>
            <div className="relative w-full aspect-video bg-muted rounded-lg overflow-hidden">
              <Image
                src={broadcast.waitingScreenUrl || "/placeholder.svg"}
                alt="대기화면"
                fill
                className="object-cover"
              />
            </div>
          </div>
        </div>
      </Card>

      <Card className="p-6 space-y-4">
        <h3 className="font-semibold text-lg">판매 상품 리스트</h3>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b">
                <th className="text-left p-2">이미지</th>
                <th className="text-left p-2">상품명</th>
                <th className="text-left p-2">정가</th>
                <th className="text-left p-2">방송 할인가</th>
                <th className="text-left p-2">판매 수량</th>
                <th className="text-left p-2">재고</th>
              </tr>
            </thead>
            <tbody>
              {sellerProducts.slice(0, 5).map((product) => (
                <tr key={product.id} className="border-b">
                  <td className="p-2">
                    <div className="w-12 h-12 bg-muted rounded overflow-hidden">
                      <img
                        src={product.imageUrl || "/placeholder.svg"}
                        alt={product.name}
                        className="w-full h-full object-cover"
                      />
                    </div>
                  </td>
                  <td className="p-2">{product.name}</td>
                  <td className="p-2">{product.price.toLocaleString()}원</td>
                  <td className="p-2 font-semibold text-red-600">
                    {Math.floor(product.price * 0.85).toLocaleString()}원
                  </td>
                  <td className="p-2">100개</td>
                  <td className="p-2">{product.stock}개</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>

      <div className="flex flex-col gap-2">
        {!isReady && <p className="text-sm text-muted-foreground">방송 시작은 시작 30분 전부터 가능합니다.</p>}
        <div className="flex gap-2">
          {isReady && <Button onClick={handleStartBroadcast}>방송 시작</Button>}
          <Button variant="outline" onClick={() => router.push(`/seller/broadcasts/reservations/${broadcast.id}/edit`)}>
            예약 수정
          </Button>
          <Button variant="destructive" onClick={() => setCancelModalOpen(true)}>
            예약 취소
          </Button>
        </div>
      </div>

      <ConfirmModal
        open={cancelModalOpen}
        onOpenChange={setCancelModalOpen}
        title="예약 취소"
        description="예약을 취소하겠습니까? 취소 후 복구할 수 없습니다."
        onConfirm={handleCancel}
        confirmText="취소"
      />

      <QCardModal open={qCardModalOpen} onClose={() => setQCardModalOpen(false)} qCards={mockQCards} />
    </div>
  )
}
