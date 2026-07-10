import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useCartStore } from './cart'

describe('cart store', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
  })

  it('keeps cart items isolated by logged-in account', () => {
    localStorage.setItem('campus-user-id', '101')
    const cart = useCartStore()

    cart.add({
      productId: 1,
      title: '账号 A 的商品',
      price: 0.01,
      coverUrl: null,
      sellerNickname: '卖家 A'
    })
    expect(cart.totalCount).toBe(1)
    expect(localStorage.getItem('campus-cart')).toBeNull()
    expect(JSON.parse(localStorage.getItem('campus-cart:101') || '[]')).toHaveLength(1)

    localStorage.setItem('campus-user-id', '202')
    cart.switchOwner()
    expect(cart.items).toHaveLength(0)

    cart.add({
      productId: 2,
      title: '账号 B 的商品',
      price: 9.99,
      coverUrl: null,
      sellerNickname: '卖家 B'
    })
    expect(JSON.parse(localStorage.getItem('campus-cart:202') || '[]')).toHaveLength(1)

    localStorage.setItem('campus-user-id', '101')
    cart.switchOwner()
    expect(cart.items).toHaveLength(1)
    expect(cart.items[0].title).toBe('账号 A 的商品')
  })
})
