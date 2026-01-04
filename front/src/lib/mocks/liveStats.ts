import { getAdminVodDetail, getAdminVodSummaries } from './adminVods'
import { getSellerVodDetail, sellerVodSummaries } from './sellerVods'

export type StatsRange = 'daily' | 'monthly' | 'yearly'

export type ChartDatum = { label: string; value: number }

export type RankItem = { rank: number; title: string; value: number }

export type RankGroup = { best: RankItem[]; worst: RankItem[] }

const mapRange = (length: number, unit: string, base: number, increment: number): ChartDatum[] =>
  Array.from({ length }, (_, index) => ({
    label: `${index + 1}${unit}`,
    value: base + index * increment,
  }))

export const revenueData: Record<StatsRange, ChartDatum[]> = {
  daily: mapRange(14, '일', 3_400_000, 180_000),
  monthly: mapRange(12, '월', 68_000_000, 3_200_000),
  yearly: mapRange(5, '년', 740_000_000, 86_000_000),
}

export const revenuePerViewerData: Record<StatsRange, ChartDatum[]> = {
  daily: mapRange(14, '일', 14_000, 900),
  monthly: mapRange(12, '월', 32_000, 1_300),
  yearly: mapRange(5, '년', 48_000, 2_400),
}

const createRankGroup = (items: Array<{ title: string; value: number }>): RankGroup => {
  const sortedDesc = [...items].sort((a, b) => b.value - a.value)
  const sortedAsc = [...items].sort((a, b) => a.value - b.value)

  return {
    best: sortedDesc.slice(0, 5).map((item, index) => ({ ...item, rank: index + 1 })),
    worst: sortedAsc.slice(0, 5).map((item, index) => ({ ...item, rank: index + 1 })),
  }
}

const readAdminVodDetails = () => {
  const summaries = getAdminVodSummaries()
  return summaries.map((summary) => getAdminVodDetail(summary.id))
}

export const getAdminRevenueRankings = (): RankGroup => {
  const vodDetails = readAdminVodDetails()
  const revenueItems = vodDetails.map((vod) => ({
    title: `${vod.sellerName} · ${vod.title}`,
    value: vod.metrics.totalRevenue,
  }))
  return createRankGroup(revenueItems)
}

export const getAdminProductRevenueRankings = (): RankGroup => {
  const vodDetails = readAdminVodDetails()
  const productItems = vodDetails.flatMap((vod) =>
    vod.productResults.map((product) => ({
      title: product.name,
      value: product.revenue,
    })),
  )
  return createRankGroup(productItems)
}

const readSellerVodDetails = () => {
  return sellerVodSummaries.map((summary) => getSellerVodDetail(summary.id))
}

export const getSellerRevenueRankings = (): RankGroup => {
  const vodDetails = readSellerVodDetails()
  const revenueItems = vodDetails.map((vod) => ({
    title: vod.title,
    value: vod.metrics.totalRevenue,
  }))
  return createRankGroup(revenueItems)
}

export const getSellerViewerRankings = (): RankGroup => {
  const vodDetails = readSellerVodDetails()
  const viewerItems = vodDetails.map((vod) => ({
    title: vod.title,
    value: vod.metrics.maxViewers,
  }))
  return createRankGroup(viewerItems)
}
