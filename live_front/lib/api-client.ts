/**
 * API Client - Backend contract layer for NCP deployment with OpenVidu v2
 *
 * This file defines TypeScript types aligned with backend DTOs and provides
 * a thin client with mock implementations for frontend development.
 * Later, these mocks will be replaced with actual API calls.
 */

// ============================================================================
// TYPES & INTERFACES (Backend DTO contracts)
// ============================================================================

export type BroadcastStatus =
  | "RESERVED" // 예약됨
  | "READY" // 방송 준비 완료 (디바이스 설정 완료)
  | "ON_AIR" // 방송 중
  | "ENDED" // 송출 종료 (채팅/판매는 계속)
  | "ENCODING" // VOD 인코딩 중
  | "VOD" // VOD 공개
  | "CANCELED" // 예약 취소
  | "STOPED" // 송출 중지됨 (관리자)
  | "DELETED" // 삭제됨

export interface Broadcast {
  id: string
  title: string
  category: string
  sellerName: string
  sellerId: string
  status: BroadcastStatus
  thumbnailUrl: string
  waitingScreenUrl: string
  vodUrl?: string
  vodVisibility?: "PUBLIC" | "PRIVATE"
  startAt: Date
  endAt?: Date
  notice?: string
  reportCount: number
  viewersCurrent: number
  viewersTotal: number
  likeCount: number
  revenueTotal: number
  qCards: string[]
  products: string[]
  terminationReason?: string
}

export interface Product {
  id: string
  name: string
  price: number
  imageUrl: string
  stock: number
  soldCount: number
  revenue: number
}

export interface ChatMessage {
  id: string
  userId: string
  userName: string
  message: string
  timestamp: Date
}

// ============================================================================
// VIEWER COUNTING DESIGN (Redis-based)
// ============================================================================
/**
 * Viewer Counting Logic:
 *
 * Frontend:
 * - Generate viewer_uuid in localStorage on first visit
 * - Send viewer_uuid when joining broadcast
 *
 * Backend (Redis):
 * - SADD broadcast:{id}:viewers {viewer_uuid}  // Add viewer to set
 * - SCARD broadcast:{id}:viewers               // Get unique count
 * - SREM broadcast:{id}:viewers {viewer_uuid}  // Remove on leave
 *
 * IP is used for rate limiting only, NOT for counting.
 */

let viewerUuid: string | null = null

export function getViewerUuid(): string {
  if (typeof window === "undefined") return ""

  if (!viewerUuid) {
    viewerUuid = localStorage.getItem("viewer_uuid")
    if (!viewerUuid) {
      viewerUuid = `viewer_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
      localStorage.setItem("viewer_uuid", viewerUuid)
    }
  }
  return viewerUuid
}

// Mock in-memory viewer tracking (replace with Redis in backend)
const mockViewerSets = new Map<string, Set<string>>()

// ============================================================================
// RESERVATION TRAFFIC CONTROL (Transaction + Lock)
// ============================================================================
/**
 * Reservation Concurrency Control:
 *
 * Backend Strategy:
 * - Use database transaction with row-level locking
 * - SELECT ... FOR UPDATE on time-slot row
 * - Enforce: "same 1-hour slot, max 3 broadcasts"
 *
 * Approach:
 * 1. Start transaction
 * 2. Lock slot row: SELECT * FROM time_slots WHERE hour = ? FOR UPDATE
 * 3. Check count: SELECT COUNT(*) FROM broadcasts WHERE time_slot = ?
 * 4. If count >= 3, rollback and return error
 * 5. Otherwise, insert and commit
 *
 * Frontend handles:
 * - Show Korean alert on slot full error
 * - Retry with different time
 */

export interface ReservationError {
  code: "SLOT_FULL" | "INVALID_TIME" | "SELLER_LIMIT"
  message: string
}

// ============================================================================
// OPENVIDU V2 + WEBRTC CONTRACTS
// ============================================================================
/**
 * OpenVidu Integration Endpoints (NCP deployment):
 *
 * POST /api/openvidu/sessions
 * - Creates or gets existing session for broadcast
 * - Returns: { sessionId: string }
 *
 * POST /api/openvidu/tokens
 * - Generates token for publisher/subscriber
 * - Body: { sessionId: string, role: "PUBLISHER" | "SUBSCRIBER" }
 * - Returns: { token: string }
 */

export interface OpenViduSession {
  sessionId: string
}

export interface OpenViduToken {
  token: string
}

// ============================================================================
// API CLIENT FUNCTIONS (Mocked for now)
// ============================================================================

export const apiClient = {
  // ========== Broadcasts ==========

  async getBroadcasts(filters?: {
    status?: BroadcastStatus[]
    sellerId?: string
    category?: string
    sort?: string
  }): Promise<Broadcast[]> {
    // Mock implementation
    await new Promise((resolve) => setTimeout(resolve, 300))

    // Will be replaced with: return fetch('/api/broadcasts', { ...filters })
    return []
  },

  async getBroadcast(id: string): Promise<Broadcast | null> {
    await new Promise((resolve) => setTimeout(resolve, 200))

    // Will be replaced with: return fetch(`/api/broadcasts/${id}`)
    return null
  },

  async createBroadcast(data: {
    title: string
    category: string
    startAt: Date
    products: string[]
    thumbnailUrl: string
    waitingScreenUrl?: string
    notice?: string
    qCards: string[]
  }): Promise<Broadcast | ReservationError> {
    await new Promise((resolve) => setTimeout(resolve, 500))

    // Mock: Simulate slot full error 10% of the time
    if (Math.random() < 0.1) {
      return {
        code: "SLOT_FULL",
        message: "선택한 시간대의 예약 가능 횟수를 초과했습니다. 다른 시간을 선택해주세요.",
      }
    }

    // Will be replaced with: return fetch('/api/broadcasts', { method: 'POST', body: data })
    return {} as Broadcast
  },

  async updateBroadcast(id: string, data: Partial<Broadcast>): Promise<Broadcast> {
    await new Promise((resolve) => setTimeout(resolve, 300))

    // Will be replaced with: return fetch(`/api/broadcasts/${id}`, { method: 'PATCH', body: data })
    return {} as Broadcast
  },

  async startBroadcast(id: string): Promise<Broadcast> {
    await new Promise((resolve) => setTimeout(resolve, 500))

    // Transitions READY/RESERVED → ON_AIR
    // Will be replaced with: return fetch(`/api/broadcasts/${id}/start`, { method: 'POST' })
    return {} as Broadcast
  },

  async endBroadcast(id: string): Promise<Broadcast> {
    await new Promise((resolve) => setTimeout(resolve, 500))

    // Transitions ON_AIR/ENDED → ENDED
    // Will be replaced with: return fetch(`/api/broadcasts/${id}/end`, { method: 'POST' })
    return {} as Broadcast
  },

  // ========== OpenVidu ==========

  async createSession(broadcastId: string): Promise<OpenViduSession> {
    await new Promise((resolve) => setTimeout(resolve, 400))

    // Will be replaced with: return fetch('/api/openvidu/sessions', { method: 'POST', body: { broadcastId } })
    return { sessionId: `session_${broadcastId}` }
  },

  async generateToken(sessionId: string, role: "PUBLISHER" | "SUBSCRIBER"): Promise<OpenViduToken> {
    await new Promise((resolve) => setTimeout(resolve, 300))

    // Will be replaced with: return fetch('/api/openvidu/tokens', { method: 'POST', body: { sessionId, role } })
    return { token: `mock_token_${role.toLowerCase()}_${Date.now()}` }
  },

  // ========== Products ==========

  async getProducts(sellerId?: string): Promise<Product[]> {
    await new Promise((resolve) => setTimeout(resolve, 200))

    // Will be replaced with: return fetch(`/api/products?sellerId=${sellerId}`)
    return []
  },

  async pinProduct(broadcastId: string, productId: string): Promise<void> {
    await new Promise((resolve) => setTimeout(resolve, 200))

    // Will be replaced with: return fetch(`/api/broadcasts/${broadcastId}/products/pin`, { method: 'POST', body: { productId } })
  },

  // ========== Viewer Counting ==========

  async joinBroadcast(broadcastId: string): Promise<number> {
    const uuid = getViewerUuid()

    // Mock implementation with in-memory Set
    if (!mockViewerSets.has(broadcastId)) {
      mockViewerSets.set(broadcastId, new Set())
    }
    mockViewerSets.get(broadcastId)!.add(uuid)

    // Will be replaced with backend Redis:
    // SADD broadcast:{broadcastId}:viewers {uuid}
    // return SCARD broadcast:{broadcastId}:viewers

    return mockViewerSets.get(broadcastId)!.size
  },

  async leaveBroadcast(broadcastId: string): Promise<number> {
    const uuid = getViewerUuid()

    // Mock implementation
    if (mockViewerSets.has(broadcastId)) {
      mockViewerSets.get(broadcastId)!.delete(uuid)
    }

    // Will be replaced with backend Redis:
    // SREM broadcast:{broadcastId}:viewers {uuid}
    // return SCARD broadcast:{broadcastId}:viewers

    return mockViewerSets.get(broadcastId)?.size || 0
  },

  async getViewerCount(broadcastId: string): Promise<number> {
    // Mock implementation
    return mockViewerSets.get(broadcastId)?.size || 0

    // Will be replaced with backend Redis:
    // return SCARD broadcast:{broadcastId}:viewers
  },

  // ========== Chat Moderation ==========

  async sanctionUser(broadcastId: string, userId: string, reason: string, duration: number): Promise<void> {
    await new Promise((resolve) => setTimeout(resolve, 300))

    // Will be replaced with: return fetch('/api/chat/moderations', { method: 'POST', body: { broadcastId, userId, reason, duration } })
  },
}
