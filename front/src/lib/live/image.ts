export const applyImageFallback = (event: Event, fallback: string) => {
  const target = event.target as HTMLImageElement | null
  if (!target) return
  if (target.src === fallback) return
  target.src = fallback
}
