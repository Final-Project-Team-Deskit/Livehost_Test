"use client"

import { Card, CardContent } from "@/components/ui/card"
import Image from "next/image"

interface SetupCardProps {
  title: string
  description: string
  imageUrl: string
  onClick?: () => void
}

export function SetupCard({ title, description, imageUrl, onClick }: SetupCardProps) {
  return (
    <Card
      className="overflow-hidden cursor-pointer transition-all hover:scale-[1.02] hover:shadow-lg"
      onClick={onClick}
    >
      <div className="relative aspect-square bg-muted">
        <Image src={imageUrl || "/placeholder.svg"} alt={title} fill className="object-cover" />
      </div>
      <CardContent className="p-4">
        <h4 className="font-semibold mb-1 text-balance">{title}</h4>
        <p className="text-sm text-muted-foreground text-balance">{description}</p>
      </CardContent>
    </Card>
  )
}
