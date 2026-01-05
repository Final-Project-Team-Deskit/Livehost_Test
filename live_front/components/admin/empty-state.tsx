interface EmptyStateProps {
  message: string
}

export function EmptyState({ message }: EmptyStateProps) {
  return (
    <div className="flex w-full flex-1 items-center justify-center py-16 text-center text-muted-foreground">
      <p>{message}</p>
    </div>
  )
}
