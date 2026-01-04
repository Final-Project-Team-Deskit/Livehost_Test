interface BarChartProps {
  data: { date: string; value: number }[]
}

export function BarChart({ data }: BarChartProps) {
  const maxValue = Math.max(...data.map((d) => d.value))

  return (
    <div className="space-y-2">
      {data.map((item, i) => (
        <div key={i} className="flex items-center gap-4">
          <div className="w-16 text-sm text-muted-foreground">{item.date}</div>
          <div className="flex-1 bg-muted rounded-full h-8 relative overflow-hidden">
            <div
              className="bg-primary h-full rounded-full transition-all duration-500"
              style={{ width: `${(item.value / maxValue) * 100}%` }}
            />
            <div className="absolute inset-0 flex items-center px-3 text-xs font-medium">
              {item.value.toLocaleString()}
            </div>
          </div>
        </div>
      ))}
    </div>
  )
}
