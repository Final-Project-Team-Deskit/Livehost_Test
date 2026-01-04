import type React from "react"
import { SellerTopbar } from "@/components/seller/seller-topbar"

export default function SellerLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen bg-background">
      <SellerTopbar />
      <main>{children}</main>
    </div>
  )
}
