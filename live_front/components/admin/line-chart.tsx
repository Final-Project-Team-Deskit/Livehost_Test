interface LineChartProps {
  data: { date: string; value: number }[]
}

export function LineChart({ data }: LineChartProps) {
  const maxValue = Math.max(...data.map((d) => d.value))
  const minValue = Math.min(...data.map((d) => d.value))
  const range = maxValue - minValue || 1

  const points = data
    .map((item, i) => {
      const x = (i / (data.length - 1)) * 100
      const y = 100 - ((item.value - minValue) / range) * 100
      return `${x},${y}`
    })
    .join(" ")

  return (
    <div className="space-y-4">
      <svg viewBox="0 0 100 100" className="w-full h-48 border rounded-lg p-4">
        <polyline points={points} fill="none" stroke="currentColor" strokeWidth="2" className="text-primary" />
        {data.map((item, i) => {
          const x = (i / (data.length - 1)) * 100
          const y = 100 - ((item.value - minValue) / range) * 100
          return <circle key={i} cx={x} cy={y} r="2" fill="currentColor" className="text-primary" />
        })}
      </svg>
      <div className="flex justify-between text-xs text-muted-foreground px-4">
        {data.map((item, i) => (
          <div key={i} className="text-center">
            <div>{item.date}</div>
            <div className="font-medium text-foreground">{item.value.toLocaleString()}</div>
          </div>
        ))}
      </div>
    </div>
  )
}
