"use client"

import type { ReactNode } from "react"

interface FilterBarProps {
  children: ReactNode
  className?: string
}

export function FilterBar({ children, className }: FilterBarProps) {
  return (
    <div className={className || ""}>
      <div className="flex items-center gap-3 flex-wrap">{children}</div>
    </div>
  )
}
