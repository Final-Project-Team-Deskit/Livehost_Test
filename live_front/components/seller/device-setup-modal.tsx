"use client"

import { useState } from "react"
import { useRouter } from "next/navigation"
import { Camera, Mic, Volume2 } from "lucide-react"
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "@/components/ui/dialog"
import { Button } from "@/components/ui/button"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"

interface DeviceSetupModalProps {
  open: boolean
  onClose: () => void
  broadcastId: string
}

export function DeviceSetupModal({ open, onClose, broadcastId }: DeviceSetupModalProps) {
  const router = useRouter()
  const [selectedCamera, setSelectedCamera] = useState("camera-1")
  const [selectedMic, setSelectedMic] = useState("mic-1")

  const handleStart = () => {
    router.push(`/seller/broadcasts/live/${broadcastId}`)
  }

  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle>방송 장치 설정</DialogTitle>
        </DialogHeader>

        <div className="space-y-6">
          <div className="aspect-video bg-gray-900 rounded-lg flex items-center justify-center">
            <Camera className="h-16 w-16 text-white/50" />
            <div className="absolute text-white/70 text-sm">카메라 미리보기</div>
          </div>

          <div className="space-y-4">
            <div className="space-y-2">
              <label className="text-sm font-medium flex items-center gap-2">
                <Camera className="h-4 w-4" />
                카메라 선택
              </label>
              <Select value={selectedCamera} onValueChange={setSelectedCamera}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="camera-1">기본 웹캠</SelectItem>
                  <SelectItem value="camera-2">외부 카메라 1</SelectItem>
                  <SelectItem value="camera-3">외부 카메라 2</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <label className="text-sm font-medium flex items-center gap-2">
                <Mic className="h-4 w-4" />
                마이크 선택
              </label>
              <Select value={selectedMic} onValueChange={setSelectedMic}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="mic-1">기본 마이크</SelectItem>
                  <SelectItem value="mic-2">외부 마이크 1</SelectItem>
                  <SelectItem value="mic-3">외부 마이크 2</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <label className="text-sm font-medium flex items-center gap-2">
                <Volume2 className="h-4 w-4" />
                볼륨 미터
              </label>
              <div className="h-4 bg-muted rounded-full overflow-hidden">
                <div className="h-full bg-green-500 w-3/4 transition-all" />
              </div>
            </div>
          </div>
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={onClose}>
            취소
          </Button>
          <Button onClick={handleStart}>방송 스튜디오 입장</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
