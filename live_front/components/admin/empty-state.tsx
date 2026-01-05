interface EmptyStateProps {
  message: string
}

export function EmptyState({ message }: EmptyStateProps) {
  return (
    <div className="flex w-full items-center justify-center py-16 text-muted-foreground">
      <p className="text-center">{message}</p>
    </div>
  )
}
