interface EmptyStateProps {
  message: string
}

export function EmptyState({ message }: EmptyStateProps) {
  return (
    <div className="flex w-full max-w-screen-lg justify-center px-4 mx-auto">
      <div className="py-16 text-center text-muted-foreground">
        <p>{message}</p>
      </div>
    </div>
  )
}
