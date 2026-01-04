"use client"

import { SetupCard } from "./setup-card"

interface Setup {
  id: string
  title: string
  description: string
  imageUrl: string
}

interface SetupGridProps {
  setups: Setup[]
}

export function SetupGrid({ setups }: SetupGridProps) {
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
      {setups.map((setup) => (
        <SetupCard
          key={setup.id}
          title={setup.title}
          description={setup.description}
          imageUrl={setup.imageUrl}
          onClick={() => console.log(`Navigate to setup ${setup.id}`)}
        />
      ))}
    </div>
  )
}
