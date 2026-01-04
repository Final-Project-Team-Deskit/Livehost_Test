"use client"

import { useState } from "react"
import { useRouter, useParams } from "next/navigation"
import { ArrowLeft, FileText, List } from "lucide-react"
import Image from "next/image"
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { ReasonSelectModal } from "@/components/admin/reason-select-modal"
import { mockBroadcasts, mockProducts } from "@/lib/admin-mock-data"

export default function ReservationDetailPage() {
  const router = useRouter()
  const params = useParams()
  const broadcast = mockBroadcasts.find((b) => b.id === params.id)

  const [showQueueModal, setShowQueueModal] = useState(false)
  const [showCancelModal, setShowCancelModal] = useState(false)

  if (!broadcast) {
    return <div className="container mx-auto px-4 py-8">예약을 찾을 수 없습니다.</div>
  }

  const handleCancel = (reason: string) => {
    router.back()
  }

  const isCanceled = broadcast.status === "CANCELED"

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="flex items-center justify-between mb-6">
        <Button variant="ghost" onClick={() => router.back()}>
          <ArrowLeft className="h-4 w-4 mr-2" />
          뒤로 가기
        </Button>
        <div className="flex items-center gap-2">
          <Button variant="outline" onClick={() => router.push("/admin/broadcasts")}>
            <List className="h-4 w-4 mr-2" />
            목록으로
          </Button>
          <Button variant="outline" size="sm" onClick={() => setShowQueueModal(true)}>
            <FileText className="h-4 w-4 mr-2" />큐 카드 보기
          </Button>
        </div>
      </div>

      <h1 className="text-2xl font-bold mb-6">예약 상세</h1>

      <div className="max-w-6xl mx-auto space-y-6">
        <Card className="p-6">
          <h2 className="text-lg font-semibold mb-4">방송 기본 정보</h2>
          <div className="space-y-6">
            <div className="flex gap-6">
              <div className="relative w-80 h-48 flex-shrink-0 rounded-lg overflow-hidden">
                <Image
                  src={broadcast.thumbnailUrl || "/placeholder.svg"}
                  alt={broadcast.title}
                  fill
                  className="object-cover"
                />
              </div>
              <div className="flex-1 space-y-4">
                <div>
                  <h3 className="text-xl font-bold mb-2">{broadcast.title}</h3>
                </div>
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
                    <span className="font-semibold w-32 text-muted-foreground">판매자:</span>
                    <span>{broadcast.sellerName}</span>
                  </div>
                  <div className="flex gap-2">
                    <span className="font-semibold w-32 text-muted-foreground">상태:</span>
                    {isCanceled ? (
                      <span className="text-red-600 font-semibold">취소됨</span>
                    ) : (
                      <span className="text-green-600 font-semibold">예약 중</span>
                    )}
                  </div>
                  {isCanceled && broadcast.cancelReason && (
                    <div className="flex gap-2">
                      <span className="font-semibold w-32 text-muted-foreground">취소 사유:</span>
                      <span className="text-red-600">{broadcast.cancelReason}</span>
                    </div>
                  )}
                </div>
              </div>
            </div>

            <div className="pt-4 border-t">
              <h3 className="font-semibold text-sm text-muted-foreground mb-2">공지사항</h3>
              <p className="text-sm leading-relaxed">
                이번 라이브 방송에서는 특별 할인 혜택과 함께 선착순 사은품을 제공합니다. 방송 시작 전 미리 입장하셔서
                알림을 받아보세요!
              </p>
            </div>
          </div>
        </Card>

        <Card className="p-6">
          <h2 className="text-lg font-semibold mb-4">썸네일 & 대기 화면</h2>
          <div className="grid md:grid-cols-2 gap-6">
            <div>
              <h3 className="text-sm font-semibold text-muted-foreground mb-2">썸네일</h3>
              <div className="relative w-full aspect-video bg-gray-100 rounded-lg overflow-hidden">
                <Image src={broadcast.thumbnailUrl || "/placeholder.svg"} alt="썸네일" fill className="object-cover" />
              </div>
            </div>
            <div>
              <h3 className="text-sm font-semibold text-muted-foreground mb-2">대기 화면</h3>
              <div className="relative w-full aspect-video bg-gray-100 rounded-lg overflow-hidden">
                <div className="absolute inset-0 flex items-center justify-center text-muted-foreground">
                  [방송 대기 화면]
                </div>
              </div>
            </div>
          </div>
        </Card>

        <Card className="p-6">
          <h2 className="text-lg font-semibold mb-4">판매 상품 리스트</h2>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>상품명</TableHead>
                <TableHead className="text-right">실재고</TableHead>
                <TableHead className="text-right">판매가</TableHead>
                <TableHead className="text-right">방송 할인가</TableHead>
                <TableHead className="text-right">방송 수량</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {mockProducts.slice(0, 5).map((product) => (
                <TableRow key={product.id}>
                  <TableCell className="font-medium">{product.name}</TableCell>
                  <TableCell className="text-right">{Math.floor(Math.random() * 500) + 100}</TableCell>
                  <TableCell className="text-right">{product.price.toLocaleString()}원</TableCell>
                  <TableCell className="text-right text-red-600 font-semibold">
                    {Math.floor(product.price * 0.8).toLocaleString()}원
                  </TableCell>
                  <TableCell className="text-right">{product.soldCount}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Card>

        {!isCanceled && (
          <div className="flex gap-4">
            <Button variant="destructive" className="flex-1" onClick={() => setShowCancelModal(true)}>
              예약 취소
            </Button>
          </div>
        )}
      </div>

      <Dialog open={showQueueModal} onOpenChange={setShowQueueModal}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle>큐 카드</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div className="text-center text-muted-foreground p-12 border-2 border-dashed rounded-lg">
              [큐 카드 내용 영역]
            </div>
          </div>
        </DialogContent>
      </Dialog>

      <ReasonSelectModal
        open={showCancelModal}
        onOpenChange={setShowCancelModal}
        title="예약 취소"
        description="예약을 취소하시겠습니까?"
        reasons={["판매자 요청", "상품 준비 미비", "일정 변경", "기타"]}
        onConfirm={handleCancel}
      />
    </div>
  )
}
