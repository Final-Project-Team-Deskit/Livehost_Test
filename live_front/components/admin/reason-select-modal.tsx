"use client"

import { useState } from "react"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Button } from "@/components/ui/button"
import { Label } from "@/components/ui/label"
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group"
import { Textarea } from "@/components/ui/textarea"

interface ReasonSelectModalProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  title: string
  description: string
  reasons: string[]
  onConfirm: (reason: string) => void
}

export function ReasonSelectModal({
  open,
  onOpenChange,
  title,
  description,
  reasons,
  onConfirm,
}: ReasonSelectModalProps) {
  const [selectedReason, setSelectedReason] = useState("")
  const [customReason, setCustomReason] = useState("")

  const handleConfirm = () => {
    if (!selectedReason) {
      alert("사유를 선택해주세요.")
      return
    }
    if (selectedReason === "기타" && !customReason.trim()) {
      alert("기타 사유를 입력해주세요.")
      return
    }
    const finalReason = selectedReason === "기타" ? `기타: ${customReason}` : selectedReason
    onConfirm(finalReason)
    onOpenChange(false)
    setSelectedReason("")
    setCustomReason("")
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
          <DialogDescription>{description}</DialogDescription>
        </DialogHeader>
        <div className="space-y-4 py-4">
          <RadioGroup value={selectedReason} onValueChange={setSelectedReason}>
            {reasons.map((reason) => (
              <div key={reason} className="flex items-center space-x-2">
                <RadioGroupItem value={reason} id={reason} />
                <Label htmlFor={reason}>{reason}</Label>
              </div>
            ))}
          </RadioGroup>
          {selectedReason === "기타" && (
            <Textarea
              placeholder="사유를 입력해주세요"
              value={customReason}
              onChange={(e) => setCustomReason(e.target.value)}
              rows={3}
            />
          )}
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            취소
          </Button>
          <Button onClick={handleConfirm}>확인</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
