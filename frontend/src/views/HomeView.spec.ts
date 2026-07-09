import { describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import HomeView from './HomeView.vue'
import homeSource from './HomeView.vue?raw'
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
  apiGet: vi.fn().mockResolvedValue({
    code: 200,
    message: 'success',
    data: [{ id: 1, title: '欢迎使用校园二手交易平台', content: '请在校内公共区域交易', createdAt: '2026-07-09T09:00:00' }]
  })
}))

describe('HomeView', () => {
  function createHomeRouter() {
    return createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/', component: HomeView },
        { path: '/products', component: { template: '<div />' } },
        { path: '/products/:id', component: { template: '<div />' } },
        { path: '/announcements', component: { template: '<div />' } }
      ]
    })
  }

  it('renders a product image carousel from loaded products', async () => {
    localStorage.clear()
    const router = createHomeRouter()
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
    expect(wrapper.find('.hot-label').exists()).toBe(true)
    expect(wrapper.findAll('.rank-medal')).toHaveLength(3)
    expect(wrapper.findAll('.notice-list i')).toHaveLength(1)
    expect(wrapper.find('.motion-ring').exists()).toBe(true)
    expect(wrapper.findAll('.hero-stats .stat-pill')).toHaveLength(3)
    expect(wrapper.get('.notice-list').text()).toContain('欢迎使用校园二手交易平台')
    expect(wrapper.get('.recommendation-section').text()).toContain('猜你喜欢')
    expect(wrapper.get('.recommendation-section').text()).toContain('匹配你搜索过的“键盘”')

    expect(homeSource).toContain('@keyframes soft-rise')
    expect(homeSource).toMatch(/\.home-grid > \* \{[^}]*animation:\s*soft-rise/s)
    expect(homeSource).toMatch(/\.motion-ring \{[^}]*animation:\s*ring-drift/s)
    expect(homeSource).toMatch(/\.stat-pill \{[^}]*backdrop-filter:\s*blur\(14px\);/s)
    expect(wrapper.find('.recommend-head .eyebrow').exists()).toBe(false)
    expect(homeSource).toMatch(/\.section-kicker \{[^}]*background:\s*#eaf3ff;/s)
    expect(homeSource).toMatch(/\.recommend-title \{[^}]*border-left:\s*4px solid var\(--brand-blue\);/s)
    expect(homeSource).toMatch(/\.section-tabs \.more-link \{[^}]*border:\s*1px solid #cfe1ff;/s)

    await wrapper.get('.slide-nav.next').trigger('click')
    expect(wrapper.get('.carousel-track').attributes('style')).toContain('translateX(calc(-100%')
    expect(wrapper.findAll('.slide-dots button')[1].attributes('aria-pressed')).toBe('true')

    await wrapper.get('.notice-list button').trigger('click')
    await flushPromises()
    expect(wrapper.findAll('.notice-list i')).toHaveLength(0)
    expect(router.currentRoute.value.path).toBe('/announcements')

    wrapper.unmount()
  })

  it('opens the carousel product detail when a press ends without swiping', async () => {
    localStorage.clear()
    const router = createHomeRouter()
    await router.push('/')
    await router.isReady()

    const wrapper = mount(HomeView, { global: { plugins: [createPinia(), router] } })
    await flushPromises()

    const viewport = wrapper.get('.carousel-viewport')
    const slide = wrapper.get('.slide-main')
    await slide.trigger('pointerdown', { clientX: 120, pointerId: 1 })
    await viewport.trigger('pointerup', { clientX: 120, pointerId: 1 })
    await flushPromises()

    expect(router.currentRoute.value.path).toBe('/products/1')

    wrapper.unmount()
  })

  it('keeps the home product preview short so the market owns full browsing', async () => {
    localStorage.clear()
    const router = createHomeRouter()
    await router.push('/')
    await router.isReady()

    const wrapper = mount(HomeView, { global: { plugins: [createPinia(), router] } })
    await flushPromises()

    expect(wrapper.findAll('.product-section .product-card')).toHaveLength(2)
    expect(homeSource).toMatch(/visibleProducts = computed\(\(\) => \(activeTab\.value === 'hot' \? hotProducts\.value : newProducts\.value\)\.slice\(0, 2\)/)

    wrapper.unmount()
  })
})
