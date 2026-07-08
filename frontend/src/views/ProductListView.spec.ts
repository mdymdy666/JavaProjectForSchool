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
  it('uses the dynamic market layout with readable controls', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/products', component: ProductListView }]
    })
    await router.push('/products')
    await router.isReady()

    const wrapper = mount(ProductListView, { global: { plugins: [createPinia(), router] } })
    await flushPromises()

    expect(wrapper.get('[data-test="market-shell"]').text()).toContain('校园市场')
    expect(wrapper.get('.hero-panel').text()).toContain('校园二手交易')
    expect(wrapper.get('.safe-panel').text()).toContain('交易安全提示')
    expect(wrapper.findAll('.category-tile')).toHaveLength(7)
    const sortButtons = wrapper.findAll('.sort-bar button')
    expect(sortButtons).toHaveLength(5)
    expect(sortButtons.map(button => button.text())).toEqual(['最新', '热度', '价格↑', '价格↓', '重置'])
    expect(sortButtons[0].classes()).toContain('active')
    expect(listSource).toMatch(/\.market-content \{[^}]*grid-template-columns:\s*minmax\(0,\s*1fr\)\s*340px/s)
    expect(listSource).toContain('campus-market-hero.png')
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

    const bookButton = wrapper.findAll('.category-tile').find(button => button.text().includes('图书教材'))
    expect(bookButton).toBeTruthy()
    await bookButton!.trigger('click')
    await flushPromises()

    expect(searchProducts).toHaveBeenLastCalledWith(
      expect.objectContaining({ categoryId: 2, page: 1, size: 12 })
    )
    expect(bookButton!.classes()).toContain('active')
  })
})
