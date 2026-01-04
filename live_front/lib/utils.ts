import { clsx, type ClassValue } from "clsx"
import { twMerge } from "tailwind-merge"

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export function isWithinReadyWindow(scheduledAt: Date): boolean {
  const now = new Date()
  const thirtyMinsBefore = new Date(scheduledAt.getTime() - 30 * 60 * 1000)
  const tenMinsAfter = new Date(scheduledAt.getTime() + 10 * 60 * 1000) // grace period
  return now >= thirtyMinsBefore && now <= tenMinsAfter
}

export function normalizeBroadcastData(broadcasts: any[]) {
  return broadcasts.map((broadcast) => {
    const normalized = { ...broadcast }

    // A) READY normalization - enforce 30-minute window rule
    if (normalized.scheduledAt || normalized.startAt) {
      const scheduleTime = normalized.scheduledAt || normalized.startAt
      const withinWindow = isWithinReadyWindow(scheduleTime)

      // If stored as READY but outside window, treat as RESERVED
      if (normalized.status === "READY" && !withinWindow) {
        normalized.status = "RESERVED"
      }
    }

    // B) STOPED normalization - force private by default
    if (normalized.status === "STOPED") {
      // If admin hasn't explicitly made it public, force PRIVATE
      if (normalized.vodVisibility !== "PUBLIC") {
        normalized.vodVisibility = "PRIVATE"
      } else {
        // If admin made it public, transition to VOD
        normalized.status = "VOD"
        normalized.vodVisibility = "PUBLIC"
      }

      // Ensure vod_admin_lock exists
      if (!normalized.vodAdminLock) {
        normalized.vodAdminLock = "Y"
      }
    }

    // C) Missing required fields safety
    if (!normalized.scheduledAt && normalized.startAt) {
      normalized.scheduledAt = normalized.startAt
    }
    if (!normalized.thumbnailUrl) {
      normalized.thumbnailUrl = "/placeholder.svg?height=200&width=300"
    }

    return normalized
  })
}
