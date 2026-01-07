import { getAuthUser } from '../auth'

const parseStoredNumber = (value: string | null): number | null => {
  if (!value) return null
  const parsed = Number.parseInt(value, 10)
  return Number.isNaN(parsed) ? null : parsed
}

export const resolveSellerId = (): number | null => {
  const user = getAuthUser()
  const fromUser = user?.sellerId ?? user?.seller_id
  if (typeof fromUser === 'number') return fromUser
  const stored =
    parseStoredNumber(localStorage.getItem('sellerId')) ??
    parseStoredNumber(localStorage.getItem('seller_id'))
  return stored
}

export const resolveAdminId = (): number | null => {
  const user = getAuthUser()
  const fromUser = user?.id ?? user?.userId ?? user?.user_id
  if (typeof fromUser === 'number') return fromUser
  const stored =
    parseStoredNumber(localStorage.getItem('adminId')) ??
    parseStoredNumber(localStorage.getItem('admin_id'))
  return stored
}
