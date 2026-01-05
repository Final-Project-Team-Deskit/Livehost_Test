interface EmptyStateProps {
  message: string
}

export function EmptyState({ message }: EmptyStateProps) {
  return (
    <div className="grid w-full place-items-center py-16 text-center text-muted-foreground">
      <p>{message}</p>
    </div>
  )
}
