/**
 * Route Helpers - Centralized routing for Admin and Seller areas
 * Prevents cross-namespace navigation bugs
 */

// Admin Routes
export const adminRoutes = {
  broadcasts: {
    list: () => "/admin/broadcasts",
    reservations: (id: string) => `/admin/broadcasts/reservations/${id}`,
    live: (id: string) => `/admin/broadcasts/live/${id}/monitor`,
    reports: (id: string) => `/admin/broadcasts/reports/${id}`,
    stats: () => "/admin/broadcasts/stats",
    sanctions: () => "/admin/broadcasts/sanctions",
  },
}

// Seller Routes
export const sellerRoutes = {
  broadcasts: {
    list: () => "/seller/broadcasts",
    create: () => "/seller/broadcasts/create",
    reservations: (id: string) => `/seller/broadcasts/reservations/${id}`,
    reservationsEdit: (id: string) => `/seller/broadcasts/reservations/${id}/edit`,
    studio: (id: string) => `/seller/broadcasts/live/${id}/studio`,
    reports: (id: string) => `/seller/broadcasts/reports/${id}`,
    stats: () => "/seller/broadcasts/stats",
  },
}

/**
 * Runtime guard: Prevents seller pages from navigating to admin routes
 */
export function validateNavigation(currentPath: string, targetPath: string): boolean {
  const isSellerContext = currentPath.startsWith("/seller")
  const isAdminTarget = targetPath.startsWith("/admin")

  if (isSellerContext && isAdminTarget) {
    console.error(`[Route Guard] Blocked navigation from seller to admin: ${currentPath} → ${targetPath}`)
    return false
  }

  return true
}
