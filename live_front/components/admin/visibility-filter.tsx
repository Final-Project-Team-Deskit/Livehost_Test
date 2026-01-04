"use client"

import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"

interface VisibilityFilterProps {
  value: string
  onChange: (value: string) => void
  placeholder?: string
}

export function VisibilityFilter({ value, onChange, placeholder = "공개 여부" }: VisibilityFilterProps) {
  return (
    <Select value={value} onValueChange={onChange}>
      <SelectTrigger className="w-40">
        <SelectValue placeholder={placeholder} />
      </SelectTrigger>
      <SelectContent>
        <SelectItem value="all">전체</SelectItem>
        <SelectItem value="public">공개</SelectItem>
        <SelectItem value="private">비공개</SelectItem>
      </SelectContent>
    </Select>
  )
}
