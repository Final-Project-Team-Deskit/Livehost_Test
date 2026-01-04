"use client"

import { useState, useEffect, useRef, useCallback, useMemo, memo } from "react"
import { useRouter } from "next/navigation"
import {
  Pin,
  MessageSquare,
  Settings,
  Power,
  Mic,
  MicOff,
  Video,
  VideoOff,
  Volume2,
  FileText,
  Maximize,
  Minimize,
  X,
  Heart,
  ShoppingBag,
  Users,
  Clock,
} from "lucide-react"
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Slider } from "@/components/ui/slider"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { ConfirmModal } from "@/components/admin/confirm-modal"
import { QCardModal } from "@/components/seller/qcard-modal"
import { BasicInfoEditModal } from "@/components/seller/basic-info-edit-modal"
import { ContextMenu, ContextMenuContent, ContextMenuItem, ContextMenuTrigger } from "@/components/ui/context-menu"
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { sellerProducts, sellerBroadcasts } from "@/lib/seller-mock-data"
import { sellerRoutes } from "@/lib/routes"
import { apiClient } from "@/lib/api-client"
import Image from "next/image"
import { Badge } from "@/components/ui/badge"

const ChatMessage = memo(({ index, onSanction }: { index: number; onSanction: (username: string) => void }) => {
  const username = `시청자${index + 1}`
  const messages = ["안녕하세요!", "상품 정보 알려주세요", "가격이 어떻게 되나요?", "배송은 언제?", "좋아요!"]
  const message = messages[index % 5]

  const handleContextMenu = useCallback(() => {
    onSanction(username)
  }, [username, onSanction])

  return (
    <ContextMenu>
      <ContextMenuTrigger asChild>
        <div className="text-xs cursor-pointer hover:bg-accent p-1 rounded">
          <span className="font-medium text-primary">{username}:</span>{" "}
          <span className="text-muted-foreground">{message}</span>
        </div>
      </ContextMenuTrigger>
      <ContextMenuContent>
        <ContextMenuItem onClick={handleContextMenu}>시청자 제재하기</ContextMenuItem>
      </ContextMenuContent>
    </ContextMenu>
  )
})
ChatMessage.displayName = "ChatMessage"

export default function SellerStudioPage({ params }: { params: { id: string } }) {
  const router = useRouter()

  const [endModalOpen, setEndModalOpen] = useState(false)
  const [qCardModalOpen, setQCardModalOpen] = useState(false)
  const [basicInfoModalOpen, setBasicInfoModalOpen] = useState(false)

  const [sanctionModalOpen, setSanctionModalOpen] = useState(false)
  const [selectedChatUser, setSelectedChatUser] = useState<string | null>(null)
  const [sanctionType, setSanctionType] = useState("")
  const [sanctionReason, setSanctionReason] = useState("")

  const [pinnedProduct, setPinnedProduct] = useState<string | null>(null)
  const [micEnabled, setMicEnabled] = useState(true)
  const [cameraEnabled, setCameraEnabled] = useState(true)
  const [volume, setVolume] = useState([70])
  const [chatMessage, setChatMessage] = useState("")
  const [isFullscreen, setIsFullscreen] = useState(false)
  const videoContainerRef = useRef<HTMLDivElement>(null)

  const [showProductPanel, setShowProductPanel] = useState(true)
  const [showChatPanel, setShowChatPanel] = useState(true)
  const [showControlPanel, setShowControlPanel] = useState(true)

  const [currentLikes, setCurrentLikes] = useState(0)
  const [currentViewers, setCurrentViewers] = useState(0)
  const [elapsedTime, setElapsedTime] = useState("00:00:00")

  const [selectedMic, setSelectedMic] = useState("default")
  const [selectedCamera, setSelectedCamera] = useState("default")
  const [volumeMeter, setVolumeMeter] = useState(50)

  const chatEndRef = useRef<HTMLDivElement>(null)

  const mockQCards = ["오늘의 특별 할인 상품은 무엇인가요?", "배송은 언제 시작되나요?"]

  const broadcast = sellerBroadcasts.find((b) => b.id === params.id)

  useEffect(() => {
    setCurrentLikes(broadcast?.likeCount || 0)
    setCurrentViewers(broadcast?.viewersCurrent || 0)

    const timeInterval = setInterval(() => {
      if (broadcast) {
        const now = new Date()
        const diff = Math.floor((now.getTime() - broadcast.startAt.getTime()) / 1000)
        const hours = Math.floor(diff / 3600)
        const minutes = Math.floor((diff % 3600) / 60)
        const seconds = diff % 60
        setElapsedTime(
          `${String(hours).padStart(2, "0")}:${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`,
        )
      }
    }, 1000)

    const viewersInterval = setInterval(() => {
      setCurrentViewers((prev) => Math.max(50, prev + Math.floor(Math.random() * 20) - 10))
    }, 1500)

    const likesInterval = setInterval(() => {
      setCurrentLikes((prev) => prev + Math.floor(Math.random() * 5))
    }, 2000)

    return () => {
      clearInterval(timeInterval)
      clearInterval(viewersInterval)
      clearInterval(likesInterval)
    }
  }, [broadcast])

  useEffect(() => {
    const interval = setInterval(() => {
      if (micEnabled) {
        setVolumeMeter(Math.random() * 100)
      } else {
        setVolumeMeter(0)
      }
    }, 100)
    return () => clearInterval(interval)
  }, [micEnabled])

  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: "smooth" })
  }, [])

  const handleEndBroadcast = async () => {
    await apiClient.endBroadcast(params.id)
    alert("방송이 종료되었습니다.")
    router.push(sellerRoutes.broadcasts.list())
  }

  const handlePinProduct = async (productId: string) => {
    if (pinnedProduct && pinnedProduct !== productId) {
      const confirmed = confirm("PIN 상품을 변경하시겠습니까?")
      if (!confirmed) return
    }

    const newPinnedId = productId === pinnedProduct ? null : productId
    setPinnedProduct(newPinnedId)

    if (newPinnedId) {
      await apiClient.pinProduct(params.id, productId)
    }
  }

  const handleSendChat = () => {
    if (!chatMessage.trim()) return
    setChatMessage("")
  }

  const toggleFullscreen = () => {
    if (!isFullscreen) {
      if (videoContainerRef.current?.requestFullscreen) {
        videoContainerRef.current.requestFullscreen()
        setIsFullscreen(true)
      }
    } else {
      if (document.exitFullscreen) {
        document.exitFullscreen()
        setIsFullscreen(false)
      }
    }
  }

  useEffect(() => {
    const handleFullscreenChange = () => {
      setIsFullscreen(!!document.fullscreenElement)
    }
    document.addEventListener("fullscreenchange", handleFullscreenChange)
    return () => document.removeEventListener("fullscreenchange", handleFullscreenChange)
  }, [])

  const handleChatRightClick = useCallback((username: string) => {
    setSelectedChatUser(username)
    setSanctionModalOpen(true)
  }, [])

  const handleSanctionSubmit = () => {
    if (!sanctionType) {
      alert("제재 유형을 선택해주세요.")
      return
    }
    alert(`${selectedChatUser}님에게 제재가 적용되었습니다.`)
    setSanctionModalOpen(false)
    setSanctionType("")
    setSanctionReason("")
  }

  const sortedProducts = useMemo(() => {
    return sellerProducts.slice(0, 8).sort((a, b) => {
      if (a.id === pinnedProduct) return -1
      if (b.id === pinnedProduct) return 1
      return 0
    })
  }, [pinnedProduct])

  const chatMessages = useMemo(() => Array.from({ length: 30 }, (_, i) => i), [])

  return (
    <div className="h-screen flex flex-col bg-black">
      <div className="bg-background border-b p-4">
        <div className="flex items-center gap-4">
          <div className="relative w-16 h-16 rounded overflow-hidden flex-shrink-0">
            {broadcast && (
              <Image src={broadcast.thumbnailUrl || "/placeholder.svg"} alt="썸네일" fill className="object-cover" />
            )}
          </div>
          <div className="flex-1">
            {broadcast && (
              <>
                <h1 className="font-semibold">{broadcast.title}</h1>
                <p className="text-sm text-muted-foreground">카테고리: {broadcast.category}</p>
                <p className="text-xs text-muted-foreground">공지: 판매 상품 외 다른 상품 문의는 받지 않습니다.</p>
              </>
            )}
          </div>
          <div className="flex gap-2">
            <Button variant="outline" size="sm" onClick={() => setBasicInfoModalOpen(true)}>
              <Settings className="mr-2 h-4 w-4" />
              기본정보 수정
            </Button>
            <Button variant="outline" size="sm" onClick={() => setQCardModalOpen(true)}>
              <FileText className="mr-2 h-4 w-4" />큐 카드 보기
            </Button>
            <Button variant="destructive" size="sm" onClick={() => setEndModalOpen(true)}>
              <Power className="mr-2 h-4 w-4" />
              방송 종료
            </Button>
          </div>
        </div>
      </div>

      <div className="flex-1 flex overflow-hidden">
        {showProductPanel && (
          <Card
            className={`w-72 m-3 p-4 space-y-3 overflow-y-auto relative ${isFullscreen ? "fixed top-20 left-4 z-50 h-[calc(100vh-120px)] shadow-2xl" : ""}`}
          >
            <Button
              size="sm"
              variant="ghost"
              className="absolute top-2 right-2"
              onClick={() => setShowProductPanel(false)}
            >
              <X className="h-4 w-4" />
            </Button>
            <h3 className="font-semibold text-sm flex items-center gap-2">
              <Pin className="h-4 w-4" />
              상품 관리
            </h3>
            <div className="space-y-2">
              {sortedProducts.map((product) => (
                <div
                  key={product.id}
                  className={`flex items-center gap-2 p-2 border rounded transition-colors ${
                    pinnedProduct === product.id ? "bg-primary/10 border-primary shadow-md" : "hover:bg-accent"
                  }`}
                >
                  <div className="w-12 h-12 bg-muted rounded flex-shrink-0 relative overflow-hidden">
                    {product.imageUrl && (
                      <Image
                        src={product.imageUrl || "/placeholder.svg"}
                        alt={product.name}
                        fill
                        className="object-cover rounded"
                      />
                    )}
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-1">
                      <p className="text-xs font-medium truncate">{product.name}</p>
                      {pinnedProduct === product.id && (
                        <Badge variant="default" className="text-[10px] px-1 py-0 h-4">
                          PIN
                        </Badge>
                      )}
                    </div>
                    <p className="text-xs text-muted-foreground">{product.price.toLocaleString()}원</p>
                    <p className="text-xs text-muted-foreground">재고: {product.stock}</p>
                  </div>
                  <Button
                    size="sm"
                    variant={pinnedProduct === product.id ? "default" : "outline"}
                    onClick={() => handlePinProduct(product.id)}
                    className="flex-shrink-0"
                  >
                    <Pin className="h-3 w-3" />
                  </Button>
                </div>
              ))}
            </div>
          </Card>
        )}

        <div className="flex-1 flex flex-col m-3 gap-3">
          <div
            ref={videoContainerRef}
            className={`flex-1 bg-gray-900 rounded-lg flex items-center justify-center relative ${isFullscreen ? "bg-black" : ""}`}
          >
            <p className="text-white text-lg">송출 화면 (WebRTC Stream)</p>

            <div className="absolute top-4 left-4 bg-black/70 text-white px-4 py-2 rounded space-y-1">
              <div className="flex items-center gap-2">
                <Users className="h-4 w-4" />
                <span className="text-sm font-semibold">{currentViewers.toLocaleString()}</span>
              </div>
              <div className="flex items-center gap-2">
                <Clock className="h-4 w-4" />
                <span className="text-sm font-mono font-semibold">{elapsedTime}</span>
              </div>
            </div>

            <div className="absolute top-4 right-4 flex items-center gap-2 bg-black/70 px-3 py-1 rounded-full">
              <Heart className="h-4 w-4 text-red-500 fill-red-500" />
              <span className="text-white text-sm font-semibold">{currentLikes.toLocaleString()}</span>
            </div>

            <div className="absolute bottom-4 right-4 flex flex-col gap-2 z-[100]">
              <Button
                size="icon"
                variant={showProductPanel ? "default" : "secondary"}
                className="rounded-full w-12 h-12 shadow-lg"
                onClick={() => setShowProductPanel(!showProductPanel)}
                title="상품"
              >
                <ShoppingBag className="h-5 w-5" />
              </Button>
              <Button
                size="icon"
                variant={showChatPanel ? "default" : "secondary"}
                className="rounded-full w-12 h-12 shadow-lg"
                onClick={() => setShowChatPanel(!showChatPanel)}
                title="채팅"
              >
                <MessageSquare className="h-5 w-5" />
              </Button>
              <Button
                size="icon"
                variant={showControlPanel ? "default" : "secondary"}
                className="rounded-full w-12 h-12 shadow-lg"
                onClick={() => setShowControlPanel(!showControlPanel)}
                title="제어"
              >
                <Settings className="h-5 w-5" />
              </Button>
              <Button
                size="icon"
                variant="secondary"
                className="rounded-full w-12 h-12 shadow-lg"
                onClick={toggleFullscreen}
                title={isFullscreen ? "전체화면 해제" : "전체화면"}
              >
                {isFullscreen ? <Minimize className="h-5 w-5" /> : <Maximize className="h-5 w-5" />}
              </Button>
            </div>
          </div>

          {showControlPanel && (
            <Card className={`p-4 relative ${isFullscreen ? "fixed bottom-4 left-4 right-4 z-50 shadow-2xl" : ""}`}>
              <Button
                size="sm"
                variant="ghost"
                className="absolute top-2 right-2"
                onClick={() => setShowControlPanel(false)}
              >
                <X className="h-4 w-4" />
              </Button>
              <div className="space-y-4">
                <div className="flex items-center justify-between gap-4">
                  <div className="flex items-center gap-2">
                    <Button
                      size="sm"
                      variant={micEnabled ? "default" : "destructive"}
                      onClick={() => setMicEnabled(!micEnabled)}
                    >
                      {micEnabled ? <Mic className="h-4 w-4" /> : <MicOff className="h-4 w-4" />}
                    </Button>
                    <Button
                      size="sm"
                      variant={cameraEnabled ? "default" : "destructive"}
                      onClick={() => setCameraEnabled(!cameraEnabled)}
                    >
                      {cameraEnabled ? <Video className="h-4 w-4" /> : <VideoOff className="h-4 w-4" />}
                    </Button>
                  </div>

                  <div className="flex items-center gap-2 flex-1 max-w-xs">
                    <Volume2 className="h-4 w-4" />
                    <Slider value={volume} onValueChange={setVolume} max={100} step={1} className="flex-1" />
                    <span className="text-sm text-muted-foreground w-10">{volume[0]}%</span>
                  </div>

                  <div className="text-sm text-muted-foreground">
                    시청자: <span className="font-semibold">{currentViewers.toLocaleString()}</span>
                  </div>
                </div>

                <div className="grid grid-cols-3 gap-4 pt-4 border-t">
                  <div className="space-y-2">
                    <label className="text-xs text-muted-foreground">마이크 선택</label>
                    <Select value={selectedMic} onValueChange={setSelectedMic}>
                      <SelectTrigger className="h-9">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="default">기본 마이크</SelectItem>
                        <SelectItem value="usb">USB 마이크</SelectItem>
                        <SelectItem value="bluetooth">블루투스 마이크</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>

                  <div className="space-y-2">
                    <label className="text-xs text-muted-foreground">카메라 선택</label>
                    <Select value={selectedCamera} onValueChange={setSelectedCamera}>
                      <SelectTrigger className="h-9">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="default">기본 카메라</SelectItem>
                        <SelectItem value="usb">USB 카메라</SelectItem>
                        <SelectItem value="external">외부 카메라</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>

                  <div className="space-y-2">
                    <label className="text-xs text-muted-foreground">볼륨 미터</label>
                    <div className="h-9 flex items-center">
                      <div className="flex-1 h-2 bg-muted rounded-full overflow-hidden">
                        <div
                          className="h-full bg-green-500 transition-all duration-100"
                          style={{ width: `${volumeMeter}%` }}
                        />
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </Card>
          )}
        </div>

        {showChatPanel && (
          <Card
            className={`w-80 m-3 p-4 flex flex-col relative ${isFullscreen ? "fixed top-20 right-4 z-50 h-[calc(100vh-120px)] shadow-2xl" : ""}`}
          >
            <Button
              size="sm"
              variant="ghost"
              className="absolute top-2 right-2"
              onClick={() => setShowChatPanel(false)}
            >
              <X className="h-4 w-4" />
            </Button>
            <h3 className="font-semibold text-sm flex items-center gap-2 mb-3">
              <MessageSquare className="h-4 w-4" />
              실시간 채팅
            </h3>

            <div className="flex-1 space-y-2 overflow-y-auto mb-3">
              {chatMessages.map((index) => (
                <ChatMessage key={index} index={index} onSanction={handleChatRightClick} />
              ))}
              <div ref={chatEndRef} />
            </div>

            <div className="flex gap-2">
              <Input
                placeholder="메시지 입력..."
                value={chatMessage}
                onChange={(e) => setChatMessage(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && handleSendChat()}
                className="flex-1"
              />
              <Button size="sm" onClick={handleSendChat}>
                전송
              </Button>
            </div>
          </Card>
        )}
      </div>

      <ConfirmModal
        open={endModalOpen}
        onOpenChange={setEndModalOpen}
        title="방송 종료"
        description="방송 종료 시 송출이 중단되며, 시청자 화면은 대기화면으로 전환됩니다. VOD 인코딩이 자동으로 시작됩니다."
        onConfirm={handleEndBroadcast}
        confirmText="종료"
      />

      <QCardModal open={qCardModalOpen} onClose={() => setQCardModalOpen(false)} qCards={mockQCards} />

      <BasicInfoEditModal
        open={basicInfoModalOpen}
        onClose={() => setBasicInfoModalOpen(false)}
        broadcast={broadcast}
      />

      <Dialog open={sanctionModalOpen} onOpenChange={setSanctionModalOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>채팅 관리</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div>
              <Label>제재 대상</Label>
              <Input value={selectedChatUser || ""} disabled className="mt-1" />
            </div>
            <div>
              <Label>
                제재 유형 <span className="text-red-500">*</span>
              </Label>
              <Select value={sanctionType} onValueChange={setSanctionType}>
                <SelectTrigger className="mt-1">
                  <SelectValue placeholder="제재 유형을 선택해주세요" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="CHAT_BAN">채팅 금지</SelectItem>
                  <SelectItem value="FORCE_EXIT">강제 퇴장</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div>
              <Label>사유</Label>
              <Textarea
                value={sanctionReason}
                onChange={(e) => setSanctionReason(e.target.value)}
                placeholder="사유를 입력할 수 있습니다. (선택)"
                className="mt-1"
                rows={3}
              />
            </div>
            <div className="flex gap-2 pt-4">
              <Button variant="outline" className="flex-1 bg-transparent" onClick={() => setSanctionModalOpen(false)}>
                취소
              </Button>
              <Button className="flex-1" onClick={handleSanctionSubmit}>
                저장
              </Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  )
}
