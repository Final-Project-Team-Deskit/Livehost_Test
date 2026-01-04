import { Header } from "@/components/header"
import { LiveCarousel } from "@/components/live-carousel"
import { SetupCarousel } from "@/components/setup-carousel"
import { ProductCarousel } from "@/components/product-carousel"

const liveItems = [
  {
    id: "1",
    title: "프리미엄 데스크 셋업 라이브",
    description: "인기 유튜버와 함께하는 특별한 데스크 셋업 쇼핑",
    thumbnailUrl: "/modern-desk-setup-live-stream.jpg",
    isLive: true,
    viewerCount: 1284,
    liveStartedAt: new Date(Date.now() - 1 * 60 * 60 * 1000 - 23 * 60 * 1000 - 45 * 1000), // 1:23:45 ago
  },
  {
    id: "2",
    title: "게이밍 셋업 특가 쇼핑",
    description: "최고 성능의 게이밍 장비를 특별 가격으로",
    thumbnailUrl: "/gaming-rgb-desk-setup.jpg",
    isLive: true,
    viewerCount: 892,
    liveStartedAt: new Date(Date.now() - 45 * 60 * 1000), // 45 minutes ago
  },
  {
    id: "3",
    title: "미니멀 오피스 꾸미기",
    description: "심플하고 세련된 업무 공간 만들기",
    thumbnailUrl: "/minimal-white-desk-setup.jpg",
    isLive: true,
    viewerCount: 2156,
    liveStartedAt: new Date(Date.now() - 2 * 60 * 60 * 1000 - 15 * 60 * 1000), // 2:15:00 ago
  },
  {
    id: "4",
    title: "우드 인테리어 데스크",
    description: "따뜻한 원목 소재로 꾸미는 자연스러운 공간",
    thumbnailUrl: "/wooden-natural-desk-setup.jpg",
    isLive: true,
    viewerCount: 1547,
    liveStartedAt: new Date(Date.now() - 30 * 60 * 1000), // 30 minutes ago
  },
]

// Dummy data for setup recommendations
const popularSetups = [
  {
    id: "1",
    title: "미니멀 화이트 셋업",
    description: "깔끔한 화이트 톤의 심플한 책상 구성",
    imageUrl: "/minimal-white-desk-setup.jpg",
  },
  {
    id: "2",
    title: "게이밍 RGB 셋업",
    description: "화려한 RGB 조명의 게이밍 환경",
    imageUrl: "/gaming-rgb-desk-setup.jpg",
  },
  {
    id: "3",
    title: "우드 내추럴 셋업",
    description: "따뜻한 원목 느낌의 자연스러운 공간",
    imageUrl: "/wooden-natural-desk-setup.jpg",
  },
  {
    id: "4",
    title: "프로페셔널 오피스",
    description: "효율적인 업무 환경을 위한 전문가 셋업",
    imageUrl: "/professional-office-desk-setup.jpg",
  },
  {
    id: "5",
    title: "스튜디오 크리에이터",
    description: "콘텐츠 제작을 위한 완벽한 작업 공간",
    imageUrl: "/content-creator-studio-desk.jpg",
  },
  {
    id: "6",
    title: "컴팩트 원룸 셋업",
    description: "작은 공간을 효율적으로 활용한 구성",
    imageUrl: "/compact-small-desk-setup.jpg",
  },
  {
    id: "7",
    title: "듀얼 모니터 셋업",
    description: "생산성을 높이는 멀티 모니터 환경",
    imageUrl: "/dual-monitor-desk-setup.jpg",
  },
  {
    id: "8",
    title: "스탠딩 데스크 셋업",
    description: "건강을 생각한 높낮이 조절 책상",
    imageUrl: "/standing-desk-setup.jpg",
  },
]

const popularProducts = [
  {
    id: "1",
    name: "에르고노믹 메쉬 체어 프리미엄",
    imageUrl: "/ergonomic-mesh-chair.jpg",
    price: 289000,
    originalPrice: 359000,
    discountRate: 20,
    rating: 4.8,
    reviewCount: 1247,
  },
  {
    id: "2",
    name: "USB-C 도킹 스테이션 11포트",
    imageUrl: "/usb-c-docking-station.jpg",
    price: 129000,
    originalPrice: 169000,
    discountRate: 24,
    rating: 4.6,
    reviewCount: 892,
  },
  {
    id: "3",
    name: "기계식 키보드 RGB 청축",
    imageUrl: "/mechanical-keyboard-rgb.jpg",
    price: 159000,
    rating: 4.9,
    reviewCount: 2156,
  },
  {
    id: "4",
    name: "무선 마우스 게이밍 프로",
    imageUrl: "/wireless-gaming-mouse.jpg",
    price: 89000,
    originalPrice: 119000,
    discountRate: 25,
    rating: 4.7,
    reviewCount: 1523,
  },
  {
    id: "5",
    name: "27인치 4K 모니터 IPS",
    imageUrl: "/27-inch-4k-monitor.jpg",
    price: 449000,
    originalPrice: 599000,
    discountRate: 25,
    rating: 4.8,
    reviewCount: 756,
  },
  {
    id: "6",
    name: "LED 데스크 라이트 조명",
    imageUrl: "/led-desk-lamp.jpg",
    price: 59000,
    rating: 4.5,
    reviewCount: 2341,
  },
  {
    id: "7",
    name: "스탠딩 데스크 전동 높이조절",
    imageUrl: "/electric-standing-desk.jpg",
    price: 589000,
    originalPrice: 799000,
    discountRate: 26,
    rating: 4.9,
    reviewCount: 412,
  },
  {
    id: "8",
    name: "노트북 거치대 알루미늄",
    imageUrl: "/aluminum-laptop-stand.jpg",
    price: 45000,
    rating: 4.6,
    reviewCount: 1876,
  },
  {
    id: "9",
    name: "케이블 정리 박스 세트",
    imageUrl: "/cable-management-box.jpg",
    price: 29000,
    originalPrice: 39000,
    discountRate: 26,
    rating: 4.4,
    reviewCount: 3214,
  },
  {
    id: "10",
    name: "웹캠 4K 스트리밍 프로",
    imageUrl: "/4k-streaming-webcam.jpg",
    price: 189000,
    rating: 4.7,
    reviewCount: 634,
    isSoldOut: true,
  },
  {
    id: "11",
    name: "블루투스 스피커 데스크용",
    imageUrl: "/bluetooth-desk-speaker.jpg",
    price: 79000,
    originalPrice: 99000,
    discountRate: 20,
    rating: 4.5,
    reviewCount: 1128,
  },
  {
    id: "12",
    name: "모니터 암 듀얼 거치대",
    imageUrl: "/dual-monitor-arm.jpg",
    price: 129000,
    rating: 4.8,
    reviewCount: 987,
  },
]

export default function Home() {
  return (
    <div className="min-h-screen bg-background">
      <Header />

      <main className="container mx-auto px-4 py-12 space-y-16">
        {/* Live Section */}
        <section id="live">
          <h2 className="text-3xl font-bold mb-6">라이브 방송</h2>
          <LiveCarousel items={liveItems} />
        </section>

        <section id="setup">
          <h2 className="text-3xl font-bold mb-6">인기 셋업</h2>
          <SetupCarousel setups={popularSetups} />
        </section>

        <section>
          <h2 className="text-3xl font-bold mb-6">인기 상품</h2>
          <ProductCarousel products={popularProducts} />
        </section>
      </main>
    </div>
  )
}
