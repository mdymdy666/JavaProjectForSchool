import { describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { searchProducts } from '../api/product'
import ProductListView from './ProductListView.vue'
import listSource from './ProductListView.vue?raw'

vi.mock('../api/product', () => ({
  searchProducts: vi.fn().mockResolvedValue({
    code: 200,
    data: { records: [], total: 0, page: 1, size: 12 }
  })
}))

describe('ProductListView', () => {
  it('uses a compact catalog layout that is visually different from the home page', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/products', component: ProductListView }]
    })
    await router.push('/products')
    await router.isReady()

    const wrapper = mount(ProductListView, { global: { plugins: [createPinia(), router] } })
    await flushPromises()

    expect(wrapper.get('[data-test="market-shell"]').text()).toContain('商品市场')
    expect(wrapper.find('.hero-panel').exists()).toBe(false)
    expect(wrapper.find('.market-console').exists()).toBe(true)
    expect(wrapper.find('.console-metrics').exists()).toBe(true)
    expect(wrapper.find('.catalog-layout').exists()).toBe(true)
    expect(wrapper.find('.result-toolbar').exists()).toBe(true)
    expect(wrapper.findAll('.category-filter')).toHaveLength(7)
    const sortButtons = wrapper.findAll('.sort-bar button')
    expect(sortButtons).toHaveLength(5)
    expect(sortButtons.map(button => button.text())).toEqual(['最新', '热度', '价格↑', '价格↓', '重置'])
    expect(sortButtons[0].classes()).toContain('active')
    expect(listSource).toMatch(/\.catalog-layout \{[^}]*grid-template-columns:\s*220px\s+minmax\(0,\s*1fr\)/s)
    expect(listSource).not.toContain('campus-market-hero.png')
    expect(listSource).toMatch(/\.market-console::before \{[^}]*background:/s)
    expect(listSource).toMatch(/\.metric-card \{[^}]*box-shadow:/s)
    expect(listSource).toMatch(/\.filter-card::before \{[^}]*background:/s)
    expect(listSource).toMatch(/\.sort-bar button\.active \{[^}]*background:\s*var\(--brand-blue\)/s)
  })

  it('filters products by category when a category pill is selected', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/products', component: ProductListView }]
    })
    await router.push('/products')
    await router.isReady()

    const wrapper = mount(ProductListView, { global: { plugins: [createPinia(), router] } })
    await flushPromises()

    const bookButton = wrapper.findAll('.category-filter').find(button => button.text().includes('图书教材'))
    expect(bookButton).toBeTruthy()
    await bookButton!.trigger('click')
    await flushPromises()

    expect(searchProducts).toHaveBeenLastCalledWith(
      expect.objectContaining({ categoryId: 2, page: 1, size: 12 })
    )
    expect(bookButton!.classes()).toContain('active')
  })

  it('switches to newest sorting when the latest feed tab is clicked', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/products', component: ProductListView }]
    })
    await router.push('/products?sort=hot&page=2')
    await router.isReady()

    const wrapper = mount(ProductListView, { global: { plugins: [createPinia(), router] } })
    await flushPromises()

    await wrapper.findAll('.section-tabs button')[1].trigger('click')
    await flushPromises()

    expect(searchProducts).toHaveBeenLastCalledWith(
      expect.objectContaining({ sort: 'newest', page: 1, size: 12 })
    )
    expect(router.currentRoute.value.query.sort).toBe('newest')
    expect(router.currentRoute.value.query.page).toBeUndefined()
  })

  it('starts a global fuzzy search when a hot keyword is clicked', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/products', component: ProductListView }]
    })
    await router.push('/products?sort=newest&categoryId=2')
    await router.isReady()

    const wrapper = mount(ProductListView, { global: { plugins: [createPinia(), router] } })
    await flushPromises()

    await wrapper.find('.quick-keywords button').trigger('click')
    await flushPromises()

    const lastCall = vi.mocked(searchProducts).mock.calls.at(-1)?.[0]
    expect(lastCall).toEqual(expect.objectContaining({ page: 1, size: 12 }))
    expect(lastCall?.keyword).toBeTruthy()
    expect(lastCall?.categoryId).toBeUndefined()
    expect(router.currentRoute.value.query.categoryId).toBeUndefined()
  })
})
