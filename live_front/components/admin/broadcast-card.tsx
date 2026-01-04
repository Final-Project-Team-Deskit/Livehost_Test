"use client"

import { useEffect, useState } from "react"
import Image from "next/image"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { Clock, Users, Heart } from "lucide-react"
import { Card } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { StatusBadge } from "./status-badge"
import type { Broadcast } from "@/lib/admin-mock-data"
import { adminRoutes, sellerRoutes } from "@/lib/routes"
import { cn } from "@/lib/utils"
import { isWithinReadyWindow } from "@/lib/utils"

interface BroadcastCardProps {
  broadcast: Broadcast
  variant?: "live" | "reservation" | "vod"
  showButton?: boolean
  isActive?: boolean
  namespace?: "admin" | "seller"
  onReadyStart?: () => void
}

export function BroadcastCard({
  broadcast,
  variant = "live",
  showButton = true,
  isActive = false,
  namespace = "admin",
  onReadyStart,
}: BroadcastCardProps) {
  const router = useRouter()
  const [elapsedTime, setElapsedTime] = useState("")
  const [currentViewers, setCurrentViewers] = useState(broadcast.viewersCurrent || 0)

  const routes = namespace === "seller" ? sellerRoutes : adminRoutes

  const isReadyToStart = namespace === "seller" && broadcast.scheduledAt && isWithinReadyWindow(broadcast.scheduledAt)

  const getDDay = () => {
    if (!broadcast.startAt) return ""
    const now = new Date()
    const diff = Math.ceil((broadcast.startAt.getTime() - now.getTime()) / (1000 * 60 * 60 * 24))
    if (diff === 0) return "D-Day"
    if (diff > 0) return `D-${diff}`
    return ""
  }

  useEffect(() => {
    if (broadcast.status === "ON_AIR" || broadcast.status === "READY" || broadcast.status === "ENDED") {
      const timer = setInterval(() => {
        if (!broadcast.startAt) return

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
        setCurrentViewers((prev) => {
          const change = Math.floor(Math.random() * 20) - 10
          return Math.max(0, prev + change)
        })
      }, 2000)

      return () => {
        clearInterval(timer)
        clearInterval(viewerTimer)
      }
    }
  }, [broadcast.status, broadcast.startAt])

  const handleCardClick = () => {
    if (
      variant === "live" &&
      (broadcast.status === "ON_AIR" || broadcast.status === "READY" || broadcast.status === "ENDED")
    ) {
      return
    }

    if (variant === "reservation" && (broadcast.status === "RESERVED" || broadcast.status === "READY")) {
      router.push(routes.broadcasts.reservations(broadcast.id))
    } else if (variant === "reservation" && broadcast.status === "CANCELED") {
      router.push(routes.broadcasts.reservations(broadcast.id))
    } else if (
      variant === "vod" ||
      broadcast.status === "VOD" ||
      broadcast.status === "STOPED" ||
      broadcast.status === "ENCODING"
    ) {
      router.push(routes.broadcasts.reports(broadcast.id))
    }
  }

  const isLiveCard =
    variant === "live" &&
    (broadcast.status === "ON_AIR" || broadcast.status === "READY" || broadcast.status === "ENDED")
  const isClickableCard = !isLiveCard

  const renderBadges = () => {
    const badges = []

    // Priority-based status badge
    if (isReadyToStart) {
      badges.push(
        <div key="ready" className="bg-yellow-600 text-white px-2 py-1 rounded text-xs font-bold">
          방송 대기중
        </div>,
      )
    } else if (broadcast.status === "CANCELED") {
      badges.push(
        <div key="canceled" className="bg-gray-500 text-white px-2 py-1 rounded text-xs font-bold">
          취소됨
        </div>,
      )
    } else if (broadcast.status === "STOPED") {
      badges.push(
        <div key="stoped" className="bg-purple-600 text-white px-2 py-1 rounded text-xs font-bold">
          송출 중지
        </div>,
      )
    } else if (broadcast.status === "ENCODING") {
      badges.push(
        <div key="encoding" className="bg-amber-500 text-white px-2 py-1 rounded text-xs font-bold">
          인코딩 중
        </div>,
      )
    } else if (broadcast.status === "ENDED") {
      badges.push(
        <div key="ended" className="bg-gray-600 text-white px-2 py-1 rounded text-xs font-bold">
          송출 종료
        </div>,
      )
    } else if (broadcast.status === "VOD") {
      badges.push(
        <div key="vod" className="bg-blue-500 text-white px-2 py-1 rounded text-xs font-bold">
          VOD
        </div>,
      )
    } else {
      badges.push(
        <StatusBadge
          key="status"
          status={broadcast.status}
          reportCount={namespace === "admin" ? broadcast.reportCount : undefined}
        />,
      )
    }

    // For VOD variant, ALSO show visibility badge (Admin requirement)
    if (
      variant === "vod" &&
      (broadcast.status === "VOD" || broadcast.status === "STOPED" || broadcast.status === "ENCODING")
    ) {
      if (broadcast.vodVisibility === "PUBLIC") {
        badges.push(
          <div key="visibility" className="bg-green-600 text-white px-2 py-1 rounded text-xs font-bold">
            공개
          </div>,
        )
      } else if (broadcast.vodVisibility === "PRIVATE") {
        badges.push(
          <div key="visibility" className="bg-gray-600 text-white px-2 py-1 rounded text-xs font-bold">
            비공개
          </div>,
        )
      }
    }

    return badges
  }

  return (
    <Card
      className={cn(
        "overflow-hidden transition-all duration-300",
        isClickableCard && "cursor-pointer hover:shadow-lg",
        isActive && "scale-105 shadow-xl opacity-100",
        !isActive && "opacity-80",
      )}
      onClick={isClickableCard ? handleCardClick : undefined}
    >
      <div className="relative aspect-video">
        <Image
          src={broadcast.thumbnailUrl || "/placeholder.svg?height=200&width=300"}
          alt={broadcast.title}
          fill
          className="object-cover"
        />
        <div className="absolute top-2 left-2 flex flex-wrap gap-1">{renderBadges()}</div>
        {variant === "reservation" && (broadcast.status === "RESERVED" || broadcast.status === "READY") && (
          <div className="absolute top-2 right-2 bg-black/70 text-white px-2 py-1 rounded text-sm font-bold">
            {getDDay()}
          </div>
        )}
      </div>
      <div className="p-4 space-y-3">
        <div>
          <h3 className="font-semibold text-sm line-clamp-1">{broadcast.title}</h3>
          <p className="text-xs text-muted-foreground mt-1">
            {broadcast.category} · {broadcast.sellerName}
          </p>
        </div>

        {(broadcast.status === "ON_AIR" || broadcast.status === "READY" || broadcast.status === "ENDED") && (
          <div className="flex items-center gap-4 text-xs text-muted-foreground">
            <div className="flex items-center gap-1">
              <Clock className="h-3 w-3" />
              <span>{elapsedTime}</span>
            </div>
            <div className="flex items-center gap-1">
              <Users className="h-3 w-3" />
              <span>{(currentViewers || 0).toLocaleString()}</span>
            </div>
          </div>
        )}

        {(broadcast.status === "RESERVED" || broadcast.status === "READY") && (
          <div className="text-xs text-muted-foreground">
            {broadcast.startAt ? (
              <>
                {broadcast.startAt.toLocaleDateString("ko-KR")}{" "}
                {broadcast.startAt.toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit" })}
              </>
            ) : (
              "날짜 미정"
            )}
          </div>
        )}

        {(broadcast.status === "VOD" || broadcast.status === "STOPED" || broadcast.status === "ENCODING") && (
          <div className="flex items-center gap-4 text-xs text-muted-foreground">
            <div className="flex items-center gap-1">
              <Users className="h-3 w-3" />
              <span>{(broadcast.viewersTotal || 0).toLocaleString()}</span>
            </div>
            <div className="flex items-center gap-1">
              <Heart className="h-3 w-3" />
              <span>{(broadcast.likeCount || 0).toLocaleString()}</span>
            </div>
          </div>
        )}

        {showButton && isLiveCard && broadcast.status === "ON_AIR" && (
          <Link
            href={
              namespace === "seller" ? routes.broadcasts.studio(broadcast.id) : routes.broadcasts.live(broadcast.id)
            }
            onClick={(e) => e.stopPropagation()}
          >
            <Button size="sm" className="w-full">
              {namespace === "seller" ? "스튜디오 입장" : "방송 입장"}
            </Button>
          </Link>
        )}

        {showButton && isLiveCard && broadcast.status === "READY" && namespace === "admin" && (
          <Link href={routes.broadcasts.live(broadcast.id)} onClick={(e) => e.stopPropagation()}>
            <Button size="sm" className="w-full">
              방송 입장
            </Button>
          </Link>
        )}

        {showButton && isLiveCard && broadcast.status === "ENDED" && (
          <Link
            href={
              namespace === "seller" ? routes.broadcasts.studio(broadcast.id) : routes.broadcasts.live(broadcast.id)
            }
            onClick={(e) => e.stopPropagation()}
          >
            <Button size="sm" className="w-full">
              방송 입장
            </Button>
          </Link>
        )}

        {showButton && namespace === "seller" && isReadyToStart && onReadyStart && (
          <Button
            size="sm"
            className="w-full"
            onClick={(e) => {
              e.stopPropagation()
              onReadyStart()
            }}
          >
            방송 시작
          </Button>
        )}
      </div>
    </Card>
  )
}
