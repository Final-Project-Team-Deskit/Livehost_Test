import { ref } from 'vue'
import { fetchBroadcasts, type BroadcastListItem } from '../api/liveApi'

export const useBroadcastHighlights = () => {
  const items = ref<BroadcastListItem[]>([])
  const loading = ref(false)
  const errorMessage = ref<string | null>(null)

  const fetchHighlights = async () => {
    loading.value = true
    errorMessage.value = null
    try {
      const { content } = await fetchBroadcasts({
        tab: 'LIVE',
        size: 6,
        sortType: 'POPULAR',
      })
      items.value = content
    } catch (error) {
      console.error('Failed to load broadcast highlights', error)
      errorMessage.value = '라이브 정보를 불러오지 못했습니다.'
    } finally {
      loading.value = false
    }
  }

  return {
    items,
    loading,
    errorMessage,
    fetchHighlights,
  }
}
