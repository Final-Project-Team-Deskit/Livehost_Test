"use client"

import type React from "react"

import { Minus, Plus } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"

type QuantitySelectorProps = {
  value: number
  onChange: (value: number) => void
  min?: number
  max?: number
}

export function QuantitySelector({ value, onChange, min = 1, max = 99 }: QuantitySelectorProps) {
  const handleDecrement = () => {
    if (value > min) onChange(value - 1)
  }

  const handleIncrement = () => {
    if (value < max) onChange(value + 1)
  }

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newValue = Number.parseInt(e.target.value) || min
    onChange(Math.max(min, Math.min(max, newValue)))
  }

  return (
    <div className="flex items-center gap-2">
      <Button type="button" variant="outline" size="icon" onClick={handleDecrement} disabled={value <= min}>
        <Minus className="h-4 w-4" />
      </Button>
      <Input
        type="number"
        value={value}
        onChange={handleInputChange}
        className="w-16 text-center"
        min={min}
        max={max}
      />
      <Button type="button" variant="outline" size="icon" onClick={handleIncrement} disabled={value >= max}>
        <Plus className="h-4 w-4" />
      </Button>
    </div>
  )
}
