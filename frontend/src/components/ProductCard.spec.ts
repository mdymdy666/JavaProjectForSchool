import { describe, expect, it, vi, beforeEach } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import ProductCard from './ProductCard.vue'
import { useAuthStore } from '../stores/auth'
import { favoriteProduct } from '../api/product'
import type { ProductCard as ProductCardType } from '../types/domain'

vi.mock('../api/product', () => ({
  favoriteProduct: vi.fn().mockResolvedValue({
    code: 200,
    data: { productId: 1, favorite: true }
  })
}))

const product: ProductCardType = {
  id: 1,
  title: '宜家收纳盒 三件套',
  categoryName: '生活用品',
  sellerNickname: '软件小陈',
  price: 25,
  status: 'APPROVED',
  viewCount: 74,
  coverUrl: null,
  itemCondition: '八成新',
  createdAt: '2026-07-07T12:00:00'
}

async function mountCard(loggedIn = false) {
  localStorage.clear()
  const pinia = createPinia()
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: { template: '<div />' } },
      { path: '/login', component: { template: '<div />' } },
      { path: '/products', component: { template: '<div />' } }
    ]
  })
  await router.push('/products')
  await router.isReady()
  if (loggedIn) {
    useAuthStore(pinia).saveSession('token', 1, '计科小李', 'USER')
  }
  const wrapper = mount(ProductCard, {
    props: { product },
    global: { plugins: [pinia, router] }
  })
  return { wrapper, router }
}

describe('ProductCard', () => {
  beforeEach(() => {
    vi.mocked(favoriteProduct).mockClear()
  })

  it('sends guests to login when they click favorite', async () => {
    const { wrapper, router } = await mountCard(false)

    await wrapper.get('button.favorite').trigger('click')
    await flushPromises()
    await router.isReady()

    expect(favoriteProduct).not.toHaveBeenCalled()
    expect(router.currentRoute.value.path).toBe('/login')
  })

  it('toggles the favorite state for logged-in users', async () => {
    const { wrapper } = await mountCard(true)

    const favorite = wrapper.get('button.favorite')
    await favorite.trigger('click')
    await flushPromises()

    expect(favoriteProduct).toHaveBeenCalledWith(1)
    expect(favorite.classes()).toContain('active')
    expect(favorite.attributes('aria-pressed')).toBe('true')
    expect(favorite.find('[data-icon="heart"]').exists()).toBe(true)
  })

  it('keeps cent-level prices visible', async () => {
    const centProduct = { ...product, price: 0.01 }
    const { wrapper } = await mountCard(false)
    await wrapper.setProps({ product: centProduct })

    expect(wrapper.text()).toContain('¥0.01')
    expect(wrapper.text()).not.toContain('¥0 ')
  })
})
