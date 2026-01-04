"use client"

import Link from "next/link"
import { usePathname } from "next/navigation"
import { ChevronDown, LogOut, User } from "lucide-react"
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from "@/components/ui/dropdown-menu"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"

export function SellerTopbar() {
  const pathname = usePathname()

  const isBroadcastActive = () => {
    return (
      pathname === "/seller/broadcasts" ||
      pathname.startsWith("/seller/broadcasts/reservations") ||
      pathname.startsWith("/seller/broadcasts/reports") ||
      pathname.startsWith("/seller/broadcasts/live") ||
      pathname === "/seller/broadcasts/create" ||
      pathname === "/seller/broadcasts/stats"
    )
  }

  return (
    <header className="border-b bg-background sticky top-0 z-50">
      <div className="container mx-auto px-4 h-16 flex items-center justify-between">
        <div className="flex items-center gap-8">
          <Link href="/seller/broadcasts" className="text-xl font-bold">
            DESKIT
          </Link>

          <nav className="flex items-center gap-6">
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button
                  variant="ghost"
                  className={cn(
                    "text-sm font-medium transition-colors",
                    isBroadcastActive() ? "text-foreground" : "text-muted-foreground hover:text-foreground",
                  )}
                >
                  방송 관리
                  <ChevronDown className="ml-1 h-4 w-4" />
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="start">
                <DropdownMenuItem asChild>
                  <Link href="/seller/broadcasts" className="cursor-pointer">
                    방송 목록
                  </Link>
                </DropdownMenuItem>
                <DropdownMenuItem asChild>
                  <Link href="/seller/broadcasts/create" className="cursor-pointer">
                    방송 등록
                  </Link>
                </DropdownMenuItem>
                <DropdownMenuItem asChild>
                  <Link href="/seller/broadcasts/stats" className="cursor-pointer">
                    방송 통계
                  </Link>
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </nav>
        </div>

        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" size="sm" className="gap-2">
              <User className="h-4 w-4" />
              <span className="text-sm">판매자1</span>
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            <DropdownMenuItem>
              <LogOut className="mr-2 h-4 w-4" />
              로그아웃
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </header>
  )
}
