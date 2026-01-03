export const endpoints = {
  products: '/api/products',
  productDetail: (id: string | number) => `/api/products/${id}`,
  setups: '/api/setups',
  setupDetail: (id: string | number) => `/api/setups/${id}`,
  broadcasts: '/api/broadcasts',
  broadcastDetail: (id: string | number) => `/api/broadcasts/${id}`,
  cart: '/cart',
  cartItems: '/cart/items',
  orders: '/orders',
  orderDetail: (id: string | number) => `/orders/${id}`,
}
