export type BroadcastTab = 'ALL' | 'RESERVED' | 'LIVE' | 'VOD';

export type BroadcastSort =
  | 'LATEST'
  | 'POPULAR'
  | 'SALES'
  | 'REPORT'
  | 'START_ASC';

export interface BroadcastSearch {
  tab?: BroadcastTab;
  keyword?: string;
  sortType?: BroadcastSort;
  categoryId?: number;
  isPublic?: boolean;
  statusFilter?: string;
  startDate?: string;
  endDate?: string;
  page?: number;
  size?: number;
}

export interface BroadcastProductSummary {
  name: string;
  stock: number;
  isSoldOut: boolean;
}

export type BroadcastStatus =
  | 'RESERVED'
  | 'READY'
  | 'ON_AIR'
  | 'ENDED'
  | 'VOD'
  | 'DELETED'
  | 'CANCELED'
  | 'STOPPED';

export interface BroadcastListResponse {
  broadcastId: number;
  title: string;
  notice: string;
  sellerName: string;
  categoryName: string;
  thumbnailUrl: string;
  status: BroadcastStatus;
  startAt: string;
  endAt: string | null;
  viewerCount: number;
  liveViewerCount: number;
  reportCount: number;
  totalSales: number;
  totalLikes: number;
  isPublic: boolean;
  adminLock: boolean;
  products?: BroadcastProductSummary[];
}

export interface BroadcastProductResponse {
  bpId: number;
  productId: number;
  name: string;
  imageUrl: string;
  originalPrice: number;
  bpPrice: number;
  bpQuantity: number;
  displayOrder: number;
  isPinned: boolean;
  status: string;
}

export interface QcardResponse {
  qcardId: number;
  question: string;
  answer: string;
}

export interface BroadcastResponse {
  broadcastId: number;
  sellerId: number;
  sellerName: string;
  sellerProfileUrl: string;
  title: string;
  notice: string;
  status: BroadcastStatus;
  layout: string;
  categoryName: string;
  scheduledAt: string;
  startedAt: string | null;
  thumbnailUrl: string;
  waitScreenUrl: string | null;
  streamKey: string | null;
  vodUrl: string | null;
  totalViews: number;
  totalLikes: number;
  totalReports: number;
  products: BroadcastProductResponse[];
  qcards: QcardResponse[];
}

export interface ApiErrorResponse {
  status: string;
  code: string;
  message: string;
  errors?: Array<{ field: string; value: string; reason: string }>;
}

export interface ApiResult<T> {
  success: boolean;
  data: T;
  error?: ApiErrorResponse;
}

export interface SliceResponse<T> {
  content: T[];
  empty: boolean;
  first: boolean;
  last: boolean;
  number: number;
  numberOfElements: number;
  size: number;
  sort?: unknown;
}
