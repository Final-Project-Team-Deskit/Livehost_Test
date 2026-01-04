"use client"

import Link from "next/link"
import { usePathname } from "next/navigation"
import { UserCircle, ChevronDown } from "lucide-react"
import { cn } from "@/lib/utils"
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from "@/components/ui/dropdown-menu"

export function AdminTopbar() {
  const pathname = usePathname()

  const isActiveRoute = (path: string) => {
    return pathname.startsWith(path)
  }

  const isBroadcastRoute = pathname.startsWith("/admin/broadcasts") || pathname.startsWith("/admin")

  return (
    <header className="sticky top-0 z-50 w-full border-b bg-white">
      <div className="container mx-auto px-4">
        <div className="flex h-16 items-center justify-between">
          {/* Logo */}
          <Link href="/admin/broadcasts" className="text-xl font-bold">
            DESKIT Admin
          </Link>

          <nav className="flex items-center gap-8">
            <DropdownMenu>
              <DropdownMenuTrigger
                className={cn(
                  "flex items-center gap-1 text-sm font-medium transition-colors hover:text-primary outline-none",
                  isBroadcastRoute ? "text-primary font-bold" : "text-muted-foreground",
                )}
              >
                방송 관리
                <ChevronDown className="h-4 w-4" />
              </DropdownMenuTrigger>
              <DropdownMenuContent align="center">
                <DropdownMenuItem asChild>
                  <Link
                    href="/admin/broadcasts"
                    className={cn(
                      "cursor-pointer",
                      isActiveRoute("/admin/broadcasts") &&
                        !isActiveRoute("/admin/broadcasts/sanctions") &&
                        !isActiveRoute("/admin/broadcasts/stats") &&
                        "bg-accent font-semibold",
                    )}
                  >
                    방송 목록
                  </Link>
                </DropdownMenuItem>
                <DropdownMenuItem asChild>
                  <Link
                    href="/admin/broadcasts/sanctions"
                    className={cn(
                      "cursor-pointer",
                      isActiveRoute("/admin/broadcasts/sanctions") && "bg-accent font-semibold",
                    )}
                  >
                    제재 관리
                  </Link>
                </DropdownMenuItem>
                <DropdownMenuItem asChild>
                  <Link
                    href="/admin/broadcasts/stats"
                    className={cn(
                      "cursor-pointer",
                      isActiveRoute("/admin/broadcasts/stats") && "bg-accent font-semibold",
                    )}
                  >
                    방송 통계
                  </Link>
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </nav>

          {/* User Profile */}
          <div className="flex items-center gap-2">
            <UserCircle className="h-6 w-6 text-muted-foreground" />
            <span className="text-sm">admin</span>
          </div>
        </div>
      </div>
    </header>
  )
}
