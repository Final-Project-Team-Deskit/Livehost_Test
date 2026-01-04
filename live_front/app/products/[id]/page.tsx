"use client"

import { useState } from "react"
import { useRouter } from "next/navigation"
import { Header } from "@/components/header"
import { Breadcrumb } from "@/components/breadcrumb"
import { ProductGallery } from "@/components/product-gallery"
import { QuantitySelector } from "@/components/quantity-selector"
import { AddToCartDialog } from "@/components/add-to-cart-dialog"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Card, CardContent } from "@/components/ui/card"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { useCart } from "@/contexts/cart-context"
import { products } from "@/lib/products-data"
import { Star } from "lucide-react"

export default function ProductDetailPage({ params }: { params: { id: string } }) {
  const router = useRouter()
  const { addToCart } = useCart()

  const product = products.find((p) => p.id === params.id)
  const [quantity, setQuantity] = useState(1)
  const [showDialog, setShowDialog] = useState(false)

  if (!product) {
    return (
      <div className="min-h-screen bg-background">
        <Header />
        <main className="container mx-auto px-4 py-12">
          <p className="text-center">상품을 찾을 수 없습니다.</p>
        </main>
      </div>
    )
  }

  const handleAddToCart = () => {
    addToCart(product, quantity)
    setShowDialog(true)
  }

  const handleBuyNow = () => {
    addToCart(product, quantity)
    router.push("/cart")
  }

  const breadcrumbItems = [
    { label: "상품", href: "/products" },
    { label: product.category, href: `/products?category=${product.category}` },
    { label: product.name, href: `/products/${product.id}` },
  ]

  return (
    <div className="min-h-screen bg-background">
      <Header />

      <main className="container mx-auto px-4 py-8 space-y-8">
        <Breadcrumb items={breadcrumbItems} />

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
          {/* Left: Image Gallery */}
          <ProductGallery images={product.images} productName={product.name} />

          {/* Right: Purchase Panel */}
          <div className="space-y-6">
            <div>
              <p className="text-sm text-muted-foreground mb-2">판매자: {product.seller}</p>
              <h1 className="text-3xl font-bold mb-2">{product.name}</h1>
              <p className="text-muted-foreground mb-4">{product.shortDescription}</p>

              <div className="flex gap-2 mb-4">
                {product.tags.map((tag) => (
                  <Badge key={tag} variant="secondary">
                    {tag}
                  </Badge>
                ))}
              </div>

              {product.rating && (
                <div className="flex items-center gap-2 mb-4">
                  <div className="flex items-center gap-1">
                    <Star className="h-5 w-5 fill-yellow-400 text-yellow-400" />
                    <span className="font-semibold">{product.rating}</span>
                  </div>
                  <span className="text-sm text-muted-foreground">
                    ({product.reviewCount?.toLocaleString()}개 리뷰)
                  </span>
                </div>
              )}

              {product.isSoldOut && (
                <Badge variant="destructive" className="mb-4">
                  품절
                </Badge>
              )}
            </div>

            <Card>
              <CardContent className="pt-6 space-y-4">
                <div className="space-y-2">
                  {product.discountRate && (
                    <div className="flex items-center gap-2">
                      <Badge className="bg-red-500 text-lg px-3 py-1">{product.discountRate}%</Badge>
                      <span className="text-lg text-muted-foreground line-through">
                        {product.originalPrice?.toLocaleString()}원
                      </span>
                    </div>
                  )}
                  <div className="text-3xl font-bold">{product.price.toLocaleString()}원</div>
                </div>

                <div className="space-y-2">
                  <label className="text-sm font-medium">수량</label>
                  <QuantitySelector value={quantity} onChange={setQuantity} />
                </div>

                <div className="flex gap-2 pt-4">
                  <Button
                    variant="outline"
                    className="flex-1 bg-transparent"
                    size="lg"
                    onClick={handleAddToCart}
                    disabled={product.isSoldOut}
                  >
                    장바구니 담기
                  </Button>
                  <Button className="flex-1" size="lg" onClick={handleBuyNow} disabled={product.isSoldOut}>
                    구매하기
                  </Button>
                </div>
              </CardContent>
            </Card>
          </div>
        </div>

        {/* Product Description Section */}
        <Tabs defaultValue="description" className="w-full">
          <TabsList className="w-full justify-start">
            <TabsTrigger value="description">상세 설명</TabsTrigger>
            <TabsTrigger value="info">상품 정보</TabsTrigger>
            <TabsTrigger value="reviews">리뷰</TabsTrigger>
          </TabsList>
          <TabsContent value="description" className="mt-6">
            <Card>
              <CardContent className="pt-6">
                <p className="text-lg leading-relaxed whitespace-pre-line">
                  {product.detailDescription}
                  {"\n\n"}이 제품은 최고 품질의 소재와 정밀한 제작 공정을 거쳐 만들어졌습니다.
                  {"\n"}
                  데스크 환경을 한층 더 업그레이드할 수 있는 완벽한 선택입니다.
                  {"\n\n"}
                  주요 특징:
                  {"\n"}- 고품질 소재 사용
                  {"\n"}- 세련된 디자인
                  {"\n"}- 뛰어난 내구성
                  {"\n"}- 사용자 편의성 고려
                </p>
              </CardContent>
            </Card>
          </TabsContent>
          <TabsContent value="info" className="mt-6">
            <Card>
              <CardContent className="pt-6">
                <dl className="space-y-4">
                  <div className="flex">
                    <dt className="w-32 font-semibold">카테고리</dt>
                    <dd>{product.category}</dd>
                  </div>
                  <div className="flex">
                    <dt className="w-32 font-semibold">태그</dt>
                    <dd>{product.tags.join(", ")}</dd>
                  </div>
                  <div className="flex">
                    <dt className="w-32 font-semibold">판매자</dt>
                    <dd>{product.seller}</dd>
                  </div>
                  <div className="flex">
                    <dt className="w-32 font-semibold">배송비</dt>
                    <dd>3,000원 (50,000원 이상 무료)</dd>
                  </div>
                </dl>
              </CardContent>
            </Card>
          </TabsContent>
          <TabsContent value="reviews" className="mt-6">
            <Card>
              <CardContent className="pt-6">
                <p className="text-muted-foreground text-center py-8">리뷰 기능은 준비 중입니다.</p>
              </CardContent>
            </Card>
          </TabsContent>
        </Tabs>
      </main>

      <AddToCartDialog open={showDialog} onOpenChange={setShowDialog} />
    </div>
  )
}
