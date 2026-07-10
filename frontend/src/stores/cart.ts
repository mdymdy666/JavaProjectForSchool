import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

const LEGACY_CART_KEY = 'campus-cart'
const GUEST_CART_KEY = 'campus-cart:guest'

export interface CartItem {
  productId: number
  title: string
  price: number
  coverUrl: string | null
  sellerNickname: string
  quantity: number
}

export const useCartStore = defineStore('cart', () => {
  const storageKey = ref(currentCartKey())
  const items = ref<CartItem[]>(loadCart(storageKey.value))

  function currentCartKey() {
    const userId = localStorage.getItem('campus-user-id')
    return userId ? `campus-cart:${userId}` : GUEST_CART_KEY
  }

  function loadCart(key: string): CartItem[] {
    try {
      const raw = localStorage.getItem(key)
      return raw ? JSON.parse(raw) : []
    } catch { return [] }
  }

  function saveCart() {
    localStorage.setItem(storageKey.value, JSON.stringify(items.value))
    localStorage.removeItem(LEGACY_CART_KEY)
  }

  function switchOwner() {
    const nextKey = currentCartKey()
    if (nextKey === storageKey.value) return
    storageKey.value = nextKey
    items.value = loadCart(nextKey)
    localStorage.removeItem(LEGACY_CART_KEY)
  }

  const totalCount = computed(() =>
    items.value.reduce((s, i) => s + i.quantity, 0)
  )
  const totalAmount = computed(() =>
    items.value.reduce((s, i) => s + i.price * i.quantity, 0)
  )
  const productIds = computed(() => new Set(items.value.map(i => i.productId)))

  function has(productId: number) {
    return productIds.value.has(productId)
  }

  function add(item: Omit<CartItem, 'quantity'>) {
    const exist = items.value.find(i => i.productId === item.productId)
    if (exist) {
      exist.quantity++
    } else {
      items.value.push({ ...item, quantity: 1 })
    }
    saveCart()
  }

  function remove(productId: number) {
    items.value = items.value.filter(i => i.productId !== productId)
    saveCart()
  }

  function updateQuantity(productId: number, qty: number) {
    const item = items.value.find(i => i.productId === productId)
    if (item) {
      item.quantity = Math.max(1, Math.min(qty, 99))
      saveCart()
    }
  }

  function clear() {
    items.value = []
    saveCart()
  }

  return { items, totalCount, totalAmount, has, add, remove, updateQuantity, clear, switchOwner }
})
