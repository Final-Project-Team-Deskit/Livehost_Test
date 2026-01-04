"use client"

import type React from "react"

import { useState, useEffect, useRef } from "react"
import { useRouter, useParams } from "next/navigation"
import { ArrowLeft, Users, Heart, Clock, MessageCircle, X, Maximize, Minimize } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { StatusBadge } from "@/components/admin/status-badge"
import { ReasonSelectModal } from "@/components/admin/reason-select-modal"
import { mockBroadcasts } from "@/lib/admin-mock-data"
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"

const mockChatMessages = [
  { id: 1, user: "시청자A", message: "상품 언제 나오나요?" },
  { id: 2, user: "시청자B", message: "가격이 어떻게 되나요?" },
  { id: 3, user: "시청자C", message: "배송은 언제 되나요?" },
  { id: 4, user: "시청자D", message: "재고 있나요?" },
  { id: 5, user: "시청자E", message: "할인 많이 해주세요!" },
]

export default function MonitorPage() {
  const router = useRouter()
  const params = useParams()
  const broadcast = mockBroadcasts.find((b) => b.id === params.id)

  const [isStopped, setIsStopped] = useState(broadcast?.status === "STOPED")
  const [showStopModal, setShowStopModal] = useState(false)
  const [showChat, setShowChat] = useState(false)
  const [isFullscreen, setIsFullscreen] = useState(false)
  const [elapsedTime, setElapsedTime] = useState("")
  const [currentViewers, setCurrentViewers] = useState(broadcast?.viewersCurrent || 0)
  const [likes, setLikes] = useState(broadcast?.likeCount || 0)
  const [chatInput, setChatInput] = useState("")

  const [contextMenu, setContextMenu] = useState<{ x: number; y: number; messageId: number } | null>(null)
  const [showSanctionModal, setShowSanctionModal] = useState(false)
  const [selectedUser, setSelectedUser] = useState("")
  const [sanctionType, setSanctionType] = useState("")
  const [sanctionReason, setSanctionReason] = useState("")

  const containerRef = useRef<HTMLDivElement>(null)

  // Real-time updates
  useEffect(() => {
    if (!broadcast || isStopped) return

    const timer = setInterval(() => {
      const now = new Date()
      const diff = Math.floor((now.getTime() - broadcast.startAt.getTime()) / 1000)
      const hours = Math.floor(diff / 3600)
      const minutes = Math.floor((diff % 3600) / 60)
      const seconds = diff % 60
      setElapsedTime(
        `${String(hours).padStart(2, "0")}:${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`,
      )
    }, 1000)

    const viewerTimer = setInterval(() => {
      setCurrentViewers((prev) => Math.max(0, prev + Math.floor(Math.random() * 20) - 10))
    }, 2000)

    const likeTimer = setInterval(() => {
      setLikes((prev) => prev + Math.floor(Math.random() * 5))
    }, 3000)

    return () => {
      clearInterval(timer)
      clearInterval(viewerTimer)
      clearInterval(likeTimer)
    }
  }, [broadcast, isStopped])

  useEffect(() => {
    const handleClickOutside = () => setContextMenu(null)
    if (contextMenu) {
      document.addEventListener("click", handleClickOutside)
      return () => document.removeEventListener("click", handleClickOutside)
    }
  }, [contextMenu])

  if (!broadcast) {
    return <div className="container mx-auto px-4 py-8">방송을 찾을 수 없습니다.</div>
  }

  const handleStop = (reason: string) => {
    setIsStopped(true)
  }

  const handleSendChat = () => {
    if (chatInput.trim()) {
      setChatInput("")
    }
  }

  const handleChatRightClick = (e: React.MouseEvent, messageId: number, username: string) => {
    e.preventDefault()
    setContextMenu({ x: e.clientX, y: e.clientY, messageId })
    setSelectedUser(username)
  }

  const handleOpenSanction = () => {
    setShowSanctionModal(true)
    setContextMenu(null)
  }

  const handleSanctionSubmit = () => {
    if (!sanctionType) {
      alert("제재 유형을 선택해주세요.")
      return
    }
    setShowSanctionModal(false)
    setSanctionType("")
    setSanctionReason("")
  }

  return (
    <div className="container mx-auto px-4 py-8">
      <Button variant="ghost" onClick={() => router.back()} className="mb-6">
        <ArrowLeft className="h-4 w-4 mr-2" />
        뒤로 가기
      </Button>

      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold">{broadcast.title}</h1>
        <div className="flex items-center gap-4">
          {!isStopped && (
            <Button variant="destructive" onClick={() => setShowStopModal(true)}>
              방송 송출 중지
            </Button>
          )}
        </div>
      </div>

      <div className={`${isFullscreen ? "" : showChat ? "flex gap-4" : ""}`} ref={containerRef}>
        <Card
          className={`relative bg-black ${isFullscreen ? "h-screen" : showChat ? "flex-1 aspect-video" : "aspect-video"}`}
        >
          <div className="absolute inset-0 flex items-center justify-center text-white">[방송 화면 영역]</div>

          {/* Overlay Stats */}
          <div className="absolute top-4 left-4 right-4 flex items-start justify-between pointer-events-none">
            <div className="space-y-2 pointer-events-auto">
              {isStopped ? (
                <div className="bg-purple-600 text-white px-3 py-1 rounded font-bold">송출중지됨</div>
              ) : (
                <StatusBadge status={broadcast.status} />
              )}
            </div>
            <div className="bg-black/70 text-white px-4 py-2 rounded space-y-1 pointer-events-auto">
              <div className="flex items-center gap-2">
                <Clock className="h-4 w-4" />
                <span className="font-mono font-bold">{elapsedTime}</span>
              </div>
              <div className="flex items-center gap-2">
                <Users className="h-4 w-4" />
                <span>{currentViewers.toLocaleString()}</span>
              </div>
              <div className="flex items-center gap-2">
                <Heart className="h-4 w-4" />
                <span>{likes.toLocaleString()}</span>
              </div>
            </div>
          </div>

          {!showChat && (
            <div className="absolute bottom-4 right-4">
              <Button size="lg" className="rounded-full w-14 h-14 shadow-lg" onClick={() => setShowChat(true)}>
                <MessageCircle className="h-6 w-6" />
              </Button>
            </div>
          )}

          <div className="absolute bottom-4 left-4">
            <Button
              size="icon"
              variant="secondary"
              className="rounded-full"
              onClick={() => setIsFullscreen(!isFullscreen)}
            >
              {isFullscreen ? <Minimize className="h-5 w-5" /> : <Maximize className="h-5 w-5" />}
            </Button>
          </div>

          {isStopped && (
            <div className="absolute inset-0 flex items-center justify-center bg-black/80">
              <div className="text-center text-white space-y-2">
                <div className="text-2xl font-bold">운영 정책 위반</div>
                <div className="text-sm">송출이 중지되었습니다</div>
              </div>
            </div>
          )}

          {showChat && isFullscreen && (
            <Card className="absolute top-0 right-0 w-96 h-full flex flex-col shadow-2xl">
              <div className="p-4 border-b flex items-center justify-between">
                <h3 className="font-semibold">실시간 채팅</h3>
                <Button size="icon" variant="ghost" onClick={() => setShowChat(false)}>
                  <X className="h-4 w-4" />
                </Button>
              </div>
              <div className="flex-1 p-4 overflow-y-auto space-y-3">
                {mockChatMessages.map((msg) => (
                  <div
                    key={msg.id}
                    className="text-sm p-2 rounded hover:bg-accent cursor-context-menu"
                    onContextMenu={(e) => handleChatRightClick(e, msg.id, msg.user)}
                  >
                    <span className="font-semibold text-primary">{msg.user}</span>
                    <p className="text-muted-foreground">{msg.message}</p>
                  </div>
                ))}
              </div>
              <div className="p-4 border-t">
                <div className="flex gap-2">
                  <Input
                    value={chatInput}
                    onChange={(e) => setChatInput(e.target.value)}
                    onKeyDown={(e) => e.key === "Enter" && handleSendChat()}
                    placeholder={isStopped ? "송출 중지로 채팅이 비활성화되었습니다" : "메시지를 입력하세요"}
                    disabled={isStopped}
                  />
                  <Button onClick={handleSendChat} disabled={isStopped}>
                    전송
                  </Button>
                </div>
              </div>
            </Card>
          )}
        </Card>

        {showChat && !isFullscreen && (
          <Card className="w-96 flex flex-col shadow-2xl">
            <div className="p-4 border-b flex items-center justify-between">
              <h3 className="font-semibold">실시간 채팅</h3>
              <Button size="icon" variant="ghost" onClick={() => setShowChat(false)}>
                <X className="h-4 w-4" />
              </Button>
            </div>
            <div className="flex-1 p-4 overflow-y-auto space-y-3 min-h-[500px]">
              {mockChatMessages.map((msg) => (
                <div
                  key={msg.id}
                  className="text-sm p-2 rounded hover:bg-accent cursor-context-menu"
                  onContextMenu={(e) => handleChatRightClick(e, msg.id, msg.user)}
                >
                  <span className="font-semibold text-primary">{msg.user}</span>
                  <p className="text-muted-foreground">{msg.message}</p>
                </div>
              ))}
            </div>
            <div className="p-4 border-t">
              <div className="flex gap-2">
                <Input
                  value={chatInput}
                  onChange={(e) => setChatInput(e.target.value)}
                  onKeyDown={(e) => e.key === "Enter" && handleSendChat()}
                  placeholder={isStopped ? "송출 중지로 채팅이 비활성화되었습니다" : "메시지를 입력하세요"}
                  disabled={isStopped}
                />
                <Button onClick={handleSendChat} disabled={isStopped}>
                  전송
                </Button>
              </div>
            </div>
          </Card>
        )}

        {contextMenu && (
          <div
            className="fixed bg-white border shadow-lg rounded-md py-1 z-50"
            style={{ top: contextMenu.y, left: contextMenu.x }}
          >
            <button className="w-full px-4 py-2 text-sm text-left hover:bg-accent" onClick={handleOpenSanction}>
              시청자 제재하기
            </button>
          </div>
        )}
      </div>

      <Dialog open={showSanctionModal} onOpenChange={setShowSanctionModal}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>채팅 관리</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div>
              <Label>제재 대상</Label>
              <Input value={selectedUser} disabled className="mt-1" />
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
                  <SelectItem value="chat-ban">채팅 금지</SelectItem>
                  <SelectItem value="force-exit">강제 퇴장</SelectItem>
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
              <Button variant="outline" className="flex-1 bg-transparent" onClick={() => setShowSanctionModal(false)}>
                취소
              </Button>
              <Button className="flex-1" onClick={handleSanctionSubmit}>
                저장
              </Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>

      <ReasonSelectModal
        open={showStopModal}
        onOpenChange={setShowStopModal}
        title="방송 송출 중지"
        description="해당 방송의 송출을 중지 하시겠습니까? 시청자에게는 대기 화면이 노출됩니다."
        reasons={["부적절한 상품 판매", "폭력적인 콘텐츠", "혐오 발언"]}
        onConfirm={handleStop}
      />
    </div>
  )
}
