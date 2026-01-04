"use client"

import { useState } from "react"
import { useRouter } from "next/navigation"
import {
  ArrowLeft,
  Users,
  Heart,
  Download,
  Play,
  Loader2,
  AlertTriangle,
  Eye,
  EyeOff,
  Trash2,
  List,
} from "lucide-react"
import Image from "next/image"
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import { Switch } from "@/components/ui/switch"
import { ConfirmModal } from "@/components/admin/confirm-modal"
import { sellerBroadcasts, sellerProducts } from "@/lib/seller-mock-data"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { useToast } from "@/hooks/use-toast"
import { sellerRoutes } from "@/lib/routes"

export default function SellerReportPage({ params }: { params: { id: string } }) {
  const router = useRouter()
  const broadcast = sellerBroadcasts.find((b) => b.id === params.id)
  const { toast } = useToast()

  const [vodVisibility, setVodVisibility] = useState(broadcast?.vodVisibility === "PUBLIC")
  const [showDeleteModal, setShowDeleteModal] = useState(false)
  const [showVodPlayer, setShowVodPlayer] = useState(false)
  const [vodDeleted, setVodDeleted] = useState(false)

  if (!broadcast) {
    return <div className="container mx-auto px-4 py-8">방송을 찾을 수 없습니다.</div>
  }

  const isAdminStopped = broadcast.status === "STOPED"
  const vodState = vodDeleted ? "deleted" : broadcast.status === "ENCODING" ? "encoding" : "normal"

  const handleVodToggle = (checked: boolean) => {
    if (isAdminStopped) {
      toast({
        title: "공개 불가",
        description: "송출 중지된 방송은 관리자만 공개 처리할 수 있습니다.",
        variant: "destructive",
      })
      return
    }
    setVodVisibility(checked)
  }

  const handleDelete = () => {
    setVodDeleted(true)
    setShowDeleteModal(false)
  }

  const handleDownload = () => {
    toast({
      title: "다운로드를 시작합니다.",
      description: "VOD 파일 다운로드가 시작되었습니다.",
    })
  }

  const getStatusDisplay = () => {
    if (vodDeleted) return { label: "삭제됨", color: "text-red-600" }

    switch (broadcast.status) {
      case "ON_AIR":
        return { label: "방송 중", color: "text-red-600" }
      case "ENDED":
        return { label: "송출 종료", color: "text-amber-600" }
      case "ENCODING":
        return { label: "인코딩 중", color: "text-amber-600" }
      case "VOD":
        return { label: "VOD", color: "text-green-600" }
      case "RESERVED":
        return { label: "예약", color: "text-blue-600" }
      case "CANCELED":
        return { label: "취소됨", color: "text-gray-600" }
      case "STOPED":
        return { label: "송출 중지", color: "text-red-600" }
      default:
        return { label: "정상 종료", color: "text-green-600" }
    }
  }

  const statusDisplay = getStatusDisplay()
  const endTime = broadcast.endAt || new Date(broadcast.startAt.getTime() + 2 * 60 * 60 * 1000)

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="flex items-center justify-between mb-6">
        <Button variant="ghost" onClick={() => router.back()}>
          <ArrowLeft className="h-4 w-4 mr-2" />
          뒤로 가기
        </Button>
        <Button variant="outline" onClick={() => router.push(sellerRoutes.broadcasts.list())}>
          <List className="h-4 w-4 mr-2" />
          목록으로
        </Button>
      </div>

      <div className="max-w-6xl mx-auto space-y-6">
        <h1 className="text-2xl font-bold">방송 결과 리포트</h1>

        {/* Same header block as Admin */}
        <Card className="p-6">
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
              <h2 className="text-xl font-bold">{broadcast.title}</h2>
              <div className="space-y-2 text-sm">
                <div className="flex gap-2">
                  <span className="font-semibold w-32 text-muted-foreground">방송 시작 시간:</span>
                  <span>
                    {broadcast.startAt.toLocaleDateString("ko-KR")}{" "}
                    {broadcast.startAt.toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit" })}
                  </span>
                </div>
                <div className="flex gap-2">
                  <span className="font-semibold w-32 text-muted-foreground">방송 종료 시간:</span>
                  <span>
                    {endTime.toLocaleDateString("ko-KR")}{" "}
                    {endTime.toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit" })}
                  </span>
                </div>
                <div className="flex gap-2">
                  <span className="font-semibold w-32 text-muted-foreground">상태:</span>
                  <span className={`font-semibold ${statusDisplay.color}`}>{statusDisplay.label}</span>
                </div>
                {broadcast.status === "STOPED" && broadcast.terminationReason && (
                  <div className="flex gap-2">
                    <span className="font-semibold w-32 text-muted-foreground">사유:</span>
                    <span className="text-red-600">{broadcast.terminationReason}</span>
                  </div>
                )}
              </div>
            </div>
          </div>
        </Card>

        {/* KPI grid - same as Admin */}
        <div className="grid grid-cols-5 gap-4">
          <Card className="p-4">
            <div className="flex items-center gap-2 text-muted-foreground text-sm mb-2">
              <Users className="h-4 w-4" />
              <span>최대 시청자 수</span>
            </div>
            <div className="text-2xl font-bold">{broadcast.viewersTotal.toLocaleString()}</div>
          </Card>
          <Card className="p-4">
            <div className="flex items-center gap-2 text-muted-foreground text-sm mb-2">
              <AlertTriangle className="h-4 w-4" />
              <span>신고 건수</span>
            </div>
            <div className="text-2xl font-bold text-red-500">
              {broadcast.reportCount || Math.floor(Math.random() * 10)}
            </div>
          </Card>
          <Card className="p-4 border-amber-200 bg-amber-50">
            <div className="flex items-center gap-2 text-amber-700 text-sm mb-2">
              <AlertTriangle className="h-4 w-4" />
              <span>시청자 제재 건수</span>
            </div>
            <div className="text-2xl font-bold text-amber-700">{Math.floor(Math.random() * 15) + 3}</div>
          </Card>
          <Card className="p-4">
            <div className="flex items-center gap-2 text-muted-foreground text-sm mb-2">
              <Heart className="h-4 w-4" />
              <span>좋아요</span>
            </div>
            <div className="text-2xl font-bold">{broadcast.likeCount.toLocaleString()}</div>
          </Card>
          <Card className="p-4">
            <div className="text-muted-foreground text-sm mb-2">총 매출</div>
            <div className="text-2xl font-bold">{broadcast.revenueTotal.toLocaleString()}원</div>
          </Card>
        </div>

        {isAdminStopped && (
          <Card className="p-4 bg-amber-50 border-amber-200">
            <p className="text-sm text-amber-800">
              송출 중지된 방송은 기본적으로 비공개 처리됩니다. 관리자만 공개 처리할 수 있습니다.
            </p>
          </Card>
        )}

        {/* VOD Section - same as Admin */}
        {(broadcast.status === "VOD" || broadcast.status === "ENDED" || broadcast.status === "STOPED") && (
          <Card className="p-6">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-lg font-semibold">VOD</h2>
              {vodState === "normal" && !vodDeleted && (
                <div className="flex items-center gap-2">
                  <div className="flex items-center gap-2 px-3 py-1 border rounded-md">
                    <EyeOff className="h-4 w-4 text-muted-foreground" />
                    <span className="text-sm text-muted-foreground">비공개</span>
                    <Switch
                      checked={vodVisibility}
                      onCheckedChange={handleVodToggle}
                      disabled={isAdminStopped}
                      className="scale-75"
                    />
                    <span className="text-sm text-muted-foreground">공개</span>
                    <Eye className="h-4 w-4 text-muted-foreground" />
                  </div>
                  <Button variant="outline" size="icon" onClick={handleDownload} title="다운로드">
                    <Download className="h-4 w-4" />
                  </Button>
                  <Button variant="outline" size="icon" onClick={() => setShowDeleteModal(true)} title="삭제">
                    <Trash2 className="h-4 w-4 text-destructive" />
                  </Button>
                </div>
              )}
            </div>

            <div className="relative w-full aspect-video bg-black rounded-lg overflow-hidden">
              {vodState === "normal" && (
                <>
                  <Image
                    src={broadcast.thumbnailUrl || "/placeholder.svg"}
                    alt={broadcast.title}
                    fill
                    className="object-cover"
                  />
                  {!showVodPlayer && (
                    <div className="absolute inset-0 flex items-center justify-center bg-black/50">
                      <Button size="lg" className="rounded-full w-16 h-16" onClick={() => setShowVodPlayer(true)}>
                        <Play className="h-8 w-8" />
                      </Button>
                    </div>
                  )}
                  {showVodPlayer && (
                    <div className="absolute inset-0 flex items-center justify-center text-white">[VOD 재생 중]</div>
                  )}
                </>
              )}

              {vodState === "encoding" && (
                <div className="absolute inset-0 flex flex-col items-center justify-center text-white gap-4">
                  <Loader2 className="h-12 w-12 animate-spin" />
                  <p className="text-lg">현재 VOD 인코딩 중입니다.</p>
                </div>
              )}

              {vodState === "deleted" && (
                <div className="absolute inset-0 flex items-center justify-center text-white">
                  <p className="text-lg">VOD가 삭제되었습니다.</p>
                </div>
              )}
            </div>
          </Card>
        )}

        {/* Product Performance Table - same as Admin */}
        <Card className="p-6">
          <h2 className="text-lg font-semibold mb-4">상품별 성과</h2>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>상품명</TableHead>
                <TableHead className="text-right">가격</TableHead>
                <TableHead className="text-right">판매 수량</TableHead>
                <TableHead className="text-right">매출</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {sellerProducts.slice(0, 5).map((product) => (
                <TableRow key={product.id}>
                  <TableCell className="font-medium">{product.name}</TableCell>
                  <TableCell className="text-right">{product.price.toLocaleString()}원</TableCell>
                  <TableCell className="text-right">{product.soldCount}</TableCell>
                  <TableCell className="text-right font-semibold">{product.revenue.toLocaleString()}원</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Card>
      </div>

      <ConfirmModal
        open={showDeleteModal}
        onOpenChange={setShowDeleteModal}
        title="VOD 삭제"
        description="VOD를 삭제하시겠습니까? 영구 삭제되어 복구할 수 없습니다."
        onConfirm={handleDelete}
        confirmText="삭제"
      />
    </div>
  )
}
