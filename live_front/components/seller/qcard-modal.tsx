import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { ScrollArea } from "@/components/ui/scroll-area"

interface QCardModalProps {
  open: boolean
  onClose: () => void
  qCards: string[]
}

export function QCardModal({ open, onClose, qCards }: QCardModalProps) {
  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent className="max-w-2xl max-h-[80vh]">
        <DialogHeader>
          <DialogTitle>큐 카드</DialogTitle>
        </DialogHeader>

        <ScrollArea className="h-[60vh] pr-4">
          <div className="space-y-4">
            {qCards.map((card, index) => (
              <div key={index} className="p-4 border rounded-lg">
                <p className="text-sm font-medium text-muted-foreground mb-2">질문 {index + 1}</p>
                <p className="text-base">{card || "(내용 없음)"}</p>
              </div>
            ))}

            {qCards.length === 0 && (
              <p className="text-center text-muted-foreground py-8">등록된 큐 카드가 없습니다.</p>
            )}
          </div>
        </ScrollArea>
      </DialogContent>
    </Dialog>
  )
}
