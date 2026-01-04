"use client"

import type React from "react"
import { createContext, useContext, useState, useCallback } from "react"

export type CartItem = {
  productId: string
  name: string
  imageUrl: string
  price: number
  originalPrice?: number
  discountRate?: number
  quantity: number
  selected: boolean
}

type CartContextType = {
  items: CartItem[]
  addToCart: (
    product: {
      id: string
      name: string
      imageUrl: string
      price: number
      originalPrice?: number
      discountRate?: number
    },
    quantity?: number,
  ) => void
  removeFromCart: (productId: string) => void
  updateQuantity: (productId: string, quantity: number) => void
  toggleSelect: (productId: string) => void
  selectAll: () => void
  unselectAll: () => void
  removeSelected: () => void
  clearAll: () => void
  getTotals: () => {
    totalProductPrice: number
    shippingFee: number
    grandTotal: number
    selectedCount: number
  }
}

const CartContext = createContext<CartContextType | undefined>(undefined)

export function CartProvider({ children }: { children: React.ReactNode }) {
  const [items, setItems] = useState<CartItem[]>([])

  const addToCart = useCallback(
    (
      product: {
        id: string
        name: string
        imageUrl: string
        price: number
        originalPrice?: number
        discountRate?: number
      },
      quantity = 1,
    ) => {
      setItems((prev) => {
        const existing = prev.find((item) => item.productId === product.id)
        if (existing) {
          return prev.map((item) =>
            item.productId === product.id ? { ...item, quantity: item.quantity + quantity } : item,
          )
        }
        return [
          ...prev,
          {
            productId: product.id,
            name: product.name,
            imageUrl: product.imageUrl,
            price: product.price,
            originalPrice: product.originalPrice,
            discountRate: product.discountRate,
            quantity,
            selected: true,
          },
        ]
      })
    },
    [],
  )

  const removeFromCart = useCallback((productId: string) => {
    setItems((prev) => prev.filter((item) => item.productId !== productId))
  }, [])

  const updateQuantity = useCallback((productId: string, quantity: number) => {
    if (quantity < 1) return
    setItems((prev) => prev.map((item) => (item.productId === productId ? { ...item, quantity } : item)))
  }, [])

  const toggleSelect = useCallback((productId: string) => {
    setItems((prev) =>
      prev.map((item) => (item.productId === productId ? { ...item, selected: !item.selected } : item)),
    )
  }, [])

  const selectAll = useCallback(() => {
    setItems((prev) => prev.map((item) => ({ ...item, selected: true })))
  }, [])

  const unselectAll = useCallback(() => {
    setItems((prev) => prev.map((item) => ({ ...item, selected: false })))
  }, [])

  const removeSelected = useCallback(() => {
    setItems((prev) => prev.filter((item) => !item.selected))
  }, [])

  const clearAll = useCallback(() => {
    setItems([])
  }, [])

  const getTotals = useCallback(() => {
    const selectedItems = items.filter((item) => item.selected)
    const totalProductPrice = selectedItems.reduce((sum, item) => sum + item.price * item.quantity, 0)
    const shippingFee = totalProductPrice > 0 && totalProductPrice < 50000 ? 3000 : 0
    const grandTotal = totalProductPrice + shippingFee
    const selectedCount = selectedItems.reduce((sum, item) => sum + item.quantity, 0)

    return { totalProductPrice, shippingFee, grandTotal, selectedCount }
  }, [items])

  return (
    <CartContext.Provider
      value={{
        items,
        addToCart,
        removeFromCart,
        updateQuantity,
        toggleSelect,
        selectAll,
        unselectAll,
        removeSelected,
        clearAll,
        getTotals,
      }}
    >
      {children}
    </CartContext.Provider>
  )
}

export function useCart() {
  const context = useContext(CartContext)
  if (!context) {
    throw new Error("useCart must be used within CartProvider")
  }
  return context
}
