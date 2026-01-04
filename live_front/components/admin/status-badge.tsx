import { cn } from "@/lib/utils"
import type { BroadcastStatus } from "@/lib/admin-mock-data"

interface StatusBadgeProps {
  status: BroadcastStatus
  reportCount?: number
  className?: string
}

export function StatusBadge({ status, reportCount, className }: StatusBadgeProps) {
  const getStatusStyle = () => {
    switch (status) {
      case "ON_AIR":
        return "bg-red-500 text-white"
      case "READY":
        return "bg-orange-500 text-white"
      case "RESERVED":
        return "bg-gray-400 text-white"
      case "CANCELED":
        return "bg-gray-500 text-white"
      case "VOD":
        return "bg-blue-500 text-white"
      case "ENDED":
        return "bg-gray-600 text-white"
      case "STOPED":
        return "bg-purple-600 text-white"
      case "ENCODING":
        return "bg-amber-500 text-white"
      default:
        return "bg-gray-400 text-white"
    }
  }

  const getStatusLabel = () => {
    switch (status) {
      case "ON_AIR":
        return "LIVE"
      case "READY":
        return "송출중"
      case "RESERVED":
        return "예약"
      case "CANCELED":
        return "취소됨"
      case "VOD":
        return "다시보기"
      case "ENDED":
        return "송출 종료"
      case "STOPED":
        return "송출 중지"
      case "ENCODING":
        return "인코딩 중"
      default:
        return status
    }
  }

  return (
    <div className={cn("flex items-center gap-2", className)}>
      <span className={cn("px-2 py-1 text-xs font-bold rounded", getStatusStyle())}>{getStatusLabel()}</span>
      {reportCount !== undefined && reportCount > 0 && (
        <span className="px-2 py-1 text-xs font-bold rounded bg-green-500 text-white">신고: {reportCount}</span>
      )}
    </div>
  )
}
