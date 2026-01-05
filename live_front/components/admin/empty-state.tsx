interface EmptyStateProps {
  message: string
}

export function EmptyState({ message }: EmptyStateProps) {
  return (
    <div className="flex w-full justify-center">
      <div className="py-16 text-center text-muted-foreground">
        <p>{message}</p>
      </div>
    </div>
  )
}
