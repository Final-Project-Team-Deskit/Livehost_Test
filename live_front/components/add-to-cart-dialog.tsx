"use client"

import { useRouter } from "next/navigation"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Button } from "@/components/ui/button"

type AddToCartDialogProps = {
  open: boolean
  onOpenChange: (open: boolean) => void
}

export function AddToCartDialog({ open, onOpenChange }: AddToCartDialogProps) {
  const router = useRouter()

  const handleGoToCart = () => {
    onOpenChange(false)
    router.push("/cart")
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>장바구니에 추가되었습니다</DialogTitle>
          <DialogDescription>장바구니로 이동하시겠습니까?</DialogDescription>
        </DialogHeader>
        <DialogFooter className="gap-2">
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            계속 쇼핑
          </Button>
          <Button onClick={handleGoToCart}>장바구니로 이동</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
