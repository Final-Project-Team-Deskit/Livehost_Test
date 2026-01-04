"use client"

import { Button } from "@/components/ui/button"

export type SortOption = "ranking" | "price-low" | "price-high" | "sales" | "latest"

type SortTabsProps = {
  selected: SortOption
  onSelect: (sort: SortOption) => void
}

const sortOptions: { value: SortOption; label: string }[] = [
  { value: "ranking", label: "DESKIT 랭킹순" },
  { value: "price-low", label: "낮은 가격순" },
  { value: "price-high", label: "높은 가격순" },
  { value: "sales", label: "판매량순" },
  { value: "latest", label: "최신순" },
]

export function SortTabs({ selected, onSelect }: SortTabsProps) {
  return (
    <div className="flex gap-2 flex-wrap">
      {sortOptions.map((option) => (
        <Button
          key={option.value}
          variant={selected === option.value ? "default" : "outline"}
          size="sm"
          onClick={() => onSelect(option.value)}
        >
          {option.label}
        </Button>
      ))}
    </div>
  )
}
