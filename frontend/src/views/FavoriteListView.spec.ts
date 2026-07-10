import { describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import FavoriteListView from './FavoriteListView.vue'

vi.mock('../api/user', () => ({
  getMyFavorites: vi.fn().mockResolvedValue({
    code: 200,
    data: [{
      id: 21,
      title: '0.01 元测试商品',
      categoryName: '数码配件',
      sellerNickname: '计科小李',
      price: 0.01,
      status: 'APPROVED',
      viewCount: 1,
      coverUrl: 'https://example.com/item.jpg',
      itemCondition: '九成新',
      createdAt: '2026-07-10T14:41:00'
    }]
  })
}))

vi.mock('../api/product', () => ({
  favoriteProduct: vi.fn().mockResolvedValue({ code: 200, data: { productId: 21, favorite: false } })
}))

describe('FavoriteListView', () => {
  it('renders favorite products with cent-level prices and actions', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/favorites', component: FavoriteListView },
        { path: '/products', component: { template: '<div />' } },
        { path: '/products/:id', component: { template: '<div />' } }
      ]
    })
    await router.push('/favorites')
    await router.isReady()

    const wrapper = mount(FavoriteListView, { global: { plugins: [router] } })
    await flushPromises()

    expect(wrapper.text()).toContain('0.01 元测试商品')
    expect(wrapper.text()).toContain('¥0.01')
    expect(wrapper.text()).toContain('取消收藏')
  })
})
