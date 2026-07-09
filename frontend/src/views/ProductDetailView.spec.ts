import { describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import ProductDetailView from './ProductDetailView.vue'
import detailSource from './ProductDetailView.vue?raw'

vi.mock('../api/product', () => ({
  getProductDetail: vi.fn().mockResolvedValue({ code: 200, data: {
    id: 7, sellerId: 2, sellerNickname: '卖家', sellerProductCount: 1,
    categoryId: 1, categoryName: '教材', title: '软件工程教材', description: '正版教材',
    price: 30, itemCondition: '九成新', status: 'APPROVED', viewCount: 8,
    images: ['/book.jpg'], favorite: false, createdAt: '2026-07-06T12:00:00'
  }}),
  favoriteProduct: vi.fn(), offShelfProduct: vi.fn(), relistProduct: vi.fn(), deleteProduct: vi.fn()
}))
vi.mock('../api/message', () => ({
  getMessages: vi.fn().mockResolvedValue({ data: [] }),
  sendMessage: vi.fn().mockResolvedValue({ code: 200, data: null }),
  markMessageRead: vi.fn().mockResolvedValue({ code: 200 }),
  getNotifications: vi.fn().mockResolvedValue({ data: [] })
}))

describe('ProductDetailView', () => {
  it('shows an identifiable favorite control', async () => {
    localStorage.setItem('campus-token', 'token')
    localStorage.setItem('campus-user-id', '1')
    localStorage.setItem('campus-role', 'USER')
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/products/:id', component: ProductDetailView },
        { path: '/', component: { template: '<div />' } },
        { path: '/products', component: { template: '<div />' } },
        { path: '/login', name: 'login', component: { template: '<div />' } }
      ]
    })
    await router.push('/products/7')
    await router.isReady()
    const wrapper = mount(ProductDetailView, { global: { plugins: [createPinia(), router] } })
    await flushPromises()

    const favorite = wrapper.get('button.btn-fav')
    expect(favorite.attributes('aria-label')).toBe('收藏商品')
    expect(favorite.text()).toContain('收藏')
    expect(favorite.find('[data-icon="heart"]').exists()).toBe(true)
    expect(detailSource).toMatch(/\.btn-fav \{[^}]*color:\s*#333;/)
    expect(detailSource).toMatch(/\.meta-item label \{[^}]*color:\s*#475569;/)
    expect(detailSource).toMatch(/\.meta-item label \{[^}]*font-weight:\s*700;/)

    await wrapper.get('button.btn-contact').trigger('click')
    await flushPromises()
    expect(wrapper.find('[role="dialog"]').exists()).toBe(true)
    expect(router.currentRoute.value.path).toBe('/products/7')
  })
})
