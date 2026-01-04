"use client"

import { Badge } from "@/components/ui/badge"

type TagChipsFilterProps = {
  tags: string[]
  selectedTags: string[]
  onToggleTag: (tag: string) => void
}

export function TagChipsFilter({ tags, selectedTags, onToggleTag }: TagChipsFilterProps) {
  return (
    <div className="flex gap-2 flex-wrap">
      {tags.map((tag) => (
        <Badge
          key={tag}
          variant={selectedTags.includes(tag) ? "default" : "outline"}
          className="cursor-pointer hover:bg-primary hover:text-primary-foreground transition-colors"
          onClick={() => onToggleTag(tag)}
        >
          {tag}
        </Badge>
      ))}
    </div>
  )
}
