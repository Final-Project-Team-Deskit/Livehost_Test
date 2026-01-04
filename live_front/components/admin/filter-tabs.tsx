"use client"

interface FilterTabsProps {
  tabs: { label: string; value: string }[]
  activeTab: string
  onChange: (value: string) => void
}

export function FilterTabs({ tabs, activeTab, onChange }: FilterTabsProps) {
  return (
    <div className="flex justify-center gap-8 border-b">
      {tabs.map((tab) => (
        <button
          key={tab.value}
          onClick={() => onChange(tab.value)}
          className={`
            relative px-4 py-3 text-sm font-medium transition-colors
            ${activeTab === tab.value ? "text-foreground" : "text-muted-foreground hover:text-foreground"}
          `}
        >
          {tab.label}
          {activeTab === tab.value && <span className="absolute bottom-0 left-0 right-0 h-0.5 bg-primary" />}
        </button>
      ))}
    </div>
  )
}
