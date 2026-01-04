"use client"

import { Input } from "@/components/ui/input"

interface DateRangeFilterProps {
  startDate: string
  endDate: string
  onStartDateChange: (date: string) => void
  onEndDateChange: (date: string) => void
  startLabel?: string
  endLabel?: string
}

export function DateRangeFilter({
  startDate,
  endDate,
  onStartDateChange,
  onEndDateChange,
  startLabel = "시작일",
  endLabel = "종료일",
}: DateRangeFilterProps) {
  return (
    <div className="flex items-center gap-2">
      <Input
        type="date"
        value={startDate}
        onChange={(e) => onStartDateChange(e.target.value)}
        placeholder={startLabel}
        className="w-40"
      />
      <span className="text-muted-foreground">~</span>
      <Input
        type="date"
        value={endDate}
        onChange={(e) => onEndDateChange(e.target.value)}
        placeholder={endLabel}
        className="w-40"
      />
    </div>
  )
}
