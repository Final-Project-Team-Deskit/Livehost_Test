"use client"

import { useState } from "react"
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "@/components/ui/dialog"
import { Button } from "@/components/ui/button"
import { Textarea } from "@/components/ui/textarea"
import { Label } from "@/components/ui/label"
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group"
import { Input } from "@/components/ui/input"

interface SanctionModalProps {
  open: boolean
  onClose: () => void
  username: string
}

export function SanctionModal({ open, onClose, username }: SanctionModalProps) {
  const [sanctionType, setSanctionType] = useState("")
  const [customReason, setCustomReason] = useState("")

  const handleSubmit = () => {
    if (!sanctionType) {
      alert("제재 유형을 선택해주세요.")
      return
    }

    alert(`${username}님에게 제재가 적용되었습니다.`)
    onClose()
    setSanctionType("")
    setCustomReason("")
  }

  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>채팅 관리</DialogTitle>
        </DialogHeader>

        <div className="space-y-4 py-4">
          <div>
            <Label>시청자</Label>
            <Input value={username} disabled className="mt-1" />
          </div>

          <div className="space-y-3">
            <Label>
              제재 유형 <span className="text-red-500">*</span>
            </Label>
            <RadioGroup value={sanctionType} onValueChange={setSanctionType}>
              <div className="flex items-center space-x-2">
                <RadioGroupItem value="CHAT_BAN" id="chat-ban" />
                <label htmlFor="chat-ban" className="text-sm cursor-pointer">
                  채팅 금지
                </label>
              </div>
              <div className="flex items-center space-x-2">
                <RadioGroupItem value="FORCE_EXIT" id="force-exit" />
                <label htmlFor="force-exit" className="text-sm cursor-pointer">
                  강제 퇴장
                </label>
              </div>
            </RadioGroup>
          </div>

          <div className="space-y-2">
            <Label>사유 (선택)</Label>
            <Textarea
              value={customReason}
              onChange={(e) => setCustomReason(e.target.value)}
              placeholder="제재 사유를 입력하세요"
              rows={3}
            />
          </div>
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={onClose}>
            취소
          </Button>
          <Button variant="destructive" onClick={handleSubmit}>
            제재하기
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
