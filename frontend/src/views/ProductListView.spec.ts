import { describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import ProductListView from './ProductListView.vue'
import listSource from './ProductListView.vue?raw'

vi.mock('../api/product', () => ({
  searchProducts: vi.fn().mockResolvedValue({
    code: 200,
    data: { records: [], total: 0, page: 1, size: 12 }
  })
}))

describe('ProductListView', () => {
  it('uses the clean market layout with readable sort controls', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/products', component: ProductListView }]
    })
    await router.push('/products')
    await router.isReady()

    const wrapper = mount(ProductListView, { global: { plugins: [router] } })
    await flushPromises()

    expect(wrapper.get('[data-test="market-shell"]').text()).toContain('校园市集')
    const sortButtons = wrapper.findAll('.sort-bar button')
    expect(sortButtons).toHaveLength(4)
    expect(sortButtons.map(button => button.text())).toEqual(['最新', '热度', '价格↑', '价格↓'])
    expect(sortButtons[0].classes()).toContain('active')
    expect(listSource).toMatch(/\.market-layout \{[^}]*grid-template-columns:\s*minmax\(0,\s*1fr\)\s*300px/s)
    expect(listSource).toMatch(/\.sort-bar button\.active \{[^}]*background:\s*var\(--brand-blue\)/s)
  })
})
