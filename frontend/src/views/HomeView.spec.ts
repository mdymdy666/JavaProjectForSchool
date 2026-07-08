import { describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import HomeView from './HomeView.vue'
import { searchProducts } from '../api/product'
import type { ProductCard } from '../types/domain'

const hotProduct: ProductCard = {
  id: 1,
  title: 'AirPods Pro 二代 充电盒版',
  categoryName: '数码配件',
  sellerNickname: '小陈',
  price: 699,
  status: 'APPROVED',
  viewCount: 376,
  coverUrl: 'https://example.com/airpods.jpg',
  itemCondition: '八成新',
  createdAt: '2026-07-08 09:00'
}

const newProduct: ProductCard = {
  ...hotProduct,
  id: 2,
  title: '考研数学复习全书',
  categoryName: '图书教材',
  price: 35,
  coverUrl: 'https://example.com/book.jpg'
}
const secondHotProduct: ProductCard = {
  ...hotProduct,
  id: 3,
  title: '宿舍小冰箱 42L',
  categoryName: '生活用品',
  price: 260,
  coverUrl: 'https://example.com/fridge.jpg'
}
const thirdHotProduct: ProductCard = {
  ...hotProduct,
  id: 4,
  title: '小米台灯 1S 护眼版',
  categoryName: '生活用品',
  price: 65,
  coverUrl: 'https://example.com/lamp.jpg'
}

vi.mock('../api/product', () => ({
  searchProducts: vi.fn((query: { sort?: string }) => Promise.resolve({
    code: 200,
    message: 'success',
    data: {
      records: query.sort === 'hot' ? [hotProduct, secondHotProduct, thirdHotProduct] : [newProduct],
      total: 1,
      page: 1,
      size: 4
    }
  })),
  getRecommendedProducts: vi.fn().mockResolvedValue({
    code: 200,
    message: 'success',
    data: [{
      id: 5,
      title: '蓝牙机械键盘',
      categoryName: '数码配件',
      price: 99,
      imageUrl: 'https://example.com/keyboard.jpg',
      viewCount: 88,
      score: 72.4,
      reason: '匹配你搜索过的“键盘”'
    }]
  })
}))

vi.mock('../api/http', () => ({
  apiGet: vi.fn().mockResolvedValue({ code: 200, message: 'success', data: [] })
}))

describe('HomeView', () => {
  it('renders a product image carousel from loaded products', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/', component: HomeView },
        { path: '/products', component: { template: '<div />' } },
        { path: '/products/:id', component: { template: '<div />' } }
      ]
    })
    await router.push('/')
    await router.isReady()

    const wrapper = mount(HomeView, { global: { plugins: [createPinia(), router] } })
    await flushPromises()

    expect(searchProducts).toHaveBeenCalledWith({ sort: 'hot', size: 4 })
    expect(searchProducts).toHaveBeenCalledWith({ sort: 'newest', size: 4 })
    expect(wrapper.get('.product-carousel').text()).toContain('正在校园里流转的好物')
    expect(wrapper.get('.slide-main img').attributes('src')).toBe(hotProduct.coverUrl)
    expect(wrapper.findAll('.slide-dots button')).toHaveLength(4)
    expect(wrapper.find('[data-icon="device"]').exists()).toBe(true)
    expect(wrapper.findAll('.rank-medal')).toHaveLength(3)
    expect(wrapper.get('.recommendation-section').text()).toContain('猜你喜欢')
    expect(wrapper.get('.recommendation-section').text()).toContain('匹配你搜索过的“键盘”')

    await wrapper.get('.slide-nav.next').trigger('click')
    expect(wrapper.get('.carousel-track').attributes('style')).toContain('translateX(calc(-100%')
    expect(wrapper.findAll('.slide-dots button')[1].attributes('aria-pressed')).toBe('true')

    wrapper.unmount()
  })
})
