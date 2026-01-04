"use client"

import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"

interface CategoryFilterProps {
  value: string
  onChange: (value: string) => void
  placeholder?: string
}

const categories = ["전체", "가구", "전자기기", "악세사리", "패션", "뷰티"]

export function CategoryFilter({ value, onChange, placeholder = "카테고리" }: CategoryFilterProps) {
  return (
    <Select value={value} onValueChange={onChange}>
      <SelectTrigger className="w-40">
        <SelectValue placeholder={placeholder} />
      </SelectTrigger>
      <SelectContent>
        {categories.map((cat) => (
          <SelectItem key={cat} value={cat === "전체" ? "all" : cat}>
            {cat}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  )
}
