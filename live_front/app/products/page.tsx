"use client"

import { useState, useMemo } from "react"
import { useSearchParams } from "next/navigation"
import { Header } from "@/components/header"
import { Breadcrumb } from "@/components/breadcrumb"
import { AdBannerCard } from "@/components/ad-banner-card"
import { SortTabs, type SortOption } from "@/components/sort-tabs"
import { TagChipsFilter } from "@/components/tag-chips-filter"
import { ProductCardList } from "@/components/product-card-list"
import { products } from "@/lib/products-data"

export default function ProductsPage() {
  const searchParams = useSearchParams()
  const categoryParam = searchParams.get("category")

  const [sortBy, setSortBy] = useState<SortOption>("ranking")
  const [selectedTags, setSelectedTags] = useState<string[]>([])

  // Extract all unique tags
  const allTags = useMemo(() => {
    const tagSet = new Set<string>()
    products.forEach((product) => {
      product.tags.forEach((tag) => tagSet.add(tag))
    })
    return Array.from(tagSet)
  }, [])

  const handleToggleTag = (tag: string) => {
    setSelectedTags((prev) => (prev.includes(tag) ? prev.filter((t) => t !== tag) : [...prev, tag]))
  }

  // Filter and sort products
  const filteredProducts = useMemo(() => {
    let filtered = products

    // Filter by category if provided
    if (categoryParam) {
      filtered = filtered.filter((p) => p.category === categoryParam)
    }

    // Filter by tags
    if (selectedTags.length > 0) {
      filtered = filtered.filter((p) => selectedTags.some((tag) => p.tags.includes(tag)))
    }

    // Sort
    const sorted = [...filtered]
    switch (sortBy) {
      case "price-low":
        sorted.sort((a, b) => a.price - b.price)
        break
      case "price-high":
        sorted.sort((a, b) => b.price - a.price)
        break
      case "sales":
        sorted.sort((a, b) => (b.reviewCount || 0) - (a.reviewCount || 0))
        break
      case "latest":
        sorted.reverse()
        break
      case "ranking":
      default:
        sorted.sort((a, b) => (b.rating || 0) - (a.rating || 0))
        break
    }

    return sorted
  }, [categoryParam, selectedTags, sortBy])

  const breadcrumbItems = [
    { label: "상품", href: "/products" },
    ...(categoryParam ? [{ label: categoryParam, href: `/products?category=${categoryParam}` }] : []),
  ]

  return (
    <div className="min-h-screen bg-background">
      <Header />

      <main className="container mx-auto px-4 py-8 space-y-6">
        <Breadcrumb items={breadcrumbItems} />

        <AdBannerCard />

        <div className="space-y-4">
          <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
            <h1 className="text-2xl font-bold">
              {categoryParam || "전체 상품"} ({filteredProducts.length})
            </h1>
            <SortTabs selected={sortBy} onSelect={setSortBy} />
          </div>

          <TagChipsFilter tags={allTags} selectedTags={selectedTags} onToggleTag={handleToggleTag} />
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
          {filteredProducts.map((product) => (
            <ProductCardList key={product.id} product={product} />
          ))}
        </div>

        {filteredProducts.length === 0 && (
          <div className="text-center py-12">
            <p className="text-muted-foreground">선택한 조건에 맞는 상품이 없습니다.</p>
          </div>
        )}
      </main>
    </div>
  )
}
