import { Card } from "@/components/ui/card"

export function AdBannerCard() {
  return (
    <Card className="w-full p-8 bg-gradient-to-r from-orange-50 to-orange-100 border-orange-200">
      <div className="text-center">
        <h3 className="text-2xl font-bold text-orange-900 mb-2">특별 프로모션</h3>
        <p className="text-orange-700">지금 구매하시면 최대 30% 할인 혜택을 받으실 수 있습니다!</p>
      </div>
    </Card>
  )
}
