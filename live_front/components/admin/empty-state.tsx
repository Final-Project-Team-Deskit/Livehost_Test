interface EmptyStateProps {
  message: string
}

export function EmptyState({ message }: EmptyStateProps) {
  return (
    <div className="grid w-full max-w-screen-xl place-items-center mx-auto py-16 text-center text-muted-foreground">
      <p>{message}</p>
    </div>
  )
}
