interface RankListProps {
  items: { rank: number; title: string; value: number }[]
}

export function RankList({ items }: RankListProps) {
  return (
    <div className="space-y-3">
      {items.map((item) => (
        <div key={item.rank} className="flex items-center gap-4 p-3 bg-muted rounded-lg">
          <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary text-primary-foreground font-bold text-sm">
            {item.rank}
          </div>
          <div className="flex-1">
            <div className="font-medium text-sm">{item.title}</div>
          </div>
          <div className="text-sm font-semibold">{item.value.toLocaleString()}원</div>
        </div>
      ))}
    </div>
  )
}
