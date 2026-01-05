"use client"

import type { ReactNode } from "react"

interface FilterBarProps {
  children: ReactNode
  className?: string
}

export function FilterBar({ children, className }: FilterBarProps) {
  return (
    <div className={className ? `w-full ${className}` : "w-full"}>
      <div className="flex w-full items-center gap-3 flex-wrap">{children}</div>
    </div>
  )
}
