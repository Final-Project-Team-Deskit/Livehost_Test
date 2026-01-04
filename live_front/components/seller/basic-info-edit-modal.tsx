"use client"

import type React from "react"

import { useState, useEffect } from "react"
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "@/components/ui/dialog"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Upload } from "lucide-react"
import type { Broadcast } from "@/lib/seller-mock-data"

interface BasicInfoEditModalProps {
  open: boolean
  onClose: () => void
  broadcast: Broadcast
}

export function BasicInfoEditModal({ open, onClose, broadcast }: BasicInfoEditModalProps) {
  const [title, setTitle] = useState(broadcast.title)
  const [category, setCategory] = useState(broadcast.category)
  const [notice, setNotice] = useState("판매 상품 외 다른 상품 문의는 받지 않습니다.")
  const [thumbnailPreview, setThumbnailPreview] = useState(broadcast.thumbnailUrl)
  const [waitingPreview, setWaitingPreview] = useState(broadcast.waitingScreenUrl || "")

  // Update state when broadcast prop changes or modal opens
  useEffect(() => {
    if (open) {
      setTitle(broadcast.title)
      setCategory(broadcast.category)
      setNotice("판매 상품 외 다른 상품 문의는 받지 않습니다.")
      setThumbnailPreview(broadcast.thumbnailUrl)
      setWaitingPreview(broadcast.waitingScreenUrl || "")
    }
  }, [open, broadcast])
  // </CHANGE>

  const handleSave = () => {
    alert("기본정보가 수정되었습니다.")
    onClose()
  }

  const handleThumbnailUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (file) {
      const reader = new FileReader()
      reader.onloadend = () => setThumbnailPreview(reader.result as string)
      reader.readAsDataURL(file)
    }
  }

  const handleWaitingUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (file) {
      const reader = new FileReader()
      reader.onloadend = () => setWaitingPreview(reader.result as string)
      reader.readAsDataURL(file)
    }
  }

  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle>기본정보 수정</DialogTitle>
        </DialogHeader>

        <div className="space-y-4 py-4">
          <div className="space-y-2">
            <Label>방송 제목</Label>
            <Input
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              maxLength={30}
              placeholder="방송 제목을 입력하세요"
            />
            <p className="text-xs text-muted-foreground">{title.length}/30</p>
          </div>

          <div className="space-y-2">
            <Label>카테고리</Label>
            <Select value={category} onValueChange={setCategory}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="가구">가구</SelectItem>
                <SelectItem value="전자기기">전자기기</SelectItem>
                <SelectItem value="패션">패션</SelectItem>
                <SelectItem value="뷰티">뷰티</SelectItem>
                <SelectItem value="악세사리">악세사리</SelectItem>
              </SelectContent>
            </Select>
          </div>
          {/* </CHANGE> */}

          <div className="space-y-2">
            <Label>공지사항</Label>
            <Textarea
              value={notice}
              onChange={(e) => setNotice(e.target.value)}
              maxLength={50}
              placeholder="공지사항을 입력하세요"
              rows={3}
            />
            <p className="text-xs text-muted-foreground">{notice.length}/50</p>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label>썸네일</Label>
              <label className="block">
                <input type="file" accept="image/*" className="hidden" onChange={handleThumbnailUpload} />
                <div className="border-2 border-dashed rounded-lg p-4 text-center cursor-pointer hover:border-primary transition-colors aspect-video">
                  {thumbnailPreview ? (
                    <img
                      src={thumbnailPreview || "/placeholder.svg"}
                      alt="썸네일"
                      className="w-full h-full object-cover rounded"
                    />
                  ) : (
                    <div className="flex flex-col items-center justify-center h-full gap-2">
                      <Upload className="h-8 w-8 text-muted-foreground" />
                      <p className="text-sm text-muted-foreground">클릭하여 업로드</p>
                    </div>
                  )}
                </div>
              </label>
            </div>

            <div className="space-y-2">
              <Label>대기화면</Label>
              <label className="block">
                <input type="file" accept="image/*" className="hidden" onChange={handleWaitingUpload} />
                <div className="border-2 border-dashed rounded-lg p-4 text-center cursor-pointer hover:border-primary transition-colors aspect-video">
                  {waitingPreview ? (
                    <img
                      src={waitingPreview || "/placeholder.svg"}
                      alt="대기화면"
                      className="w-full h-full object-cover rounded"
                    />
                  ) : (
                    <div className="flex flex-col items-center justify-center h-full gap-2">
                      <Upload className="h-8 w-8 text-muted-foreground" />
                      <p className="text-sm text-muted-foreground">클릭하여 업로드</p>
                    </div>
                  )}
                </div>
              </label>
            </div>
          </div>
          {/* </CHANGE> */}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={onClose}>
            취소
          </Button>
          <Button onClick={handleSave}>저장</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
