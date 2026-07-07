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
  it('keeps sort controls readable in normal and active states', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/products', component: ProductListView }]
    })
    await router.push('/products')
    await router.isReady()

    const wrapper = mount(ProductListView, { global: { plugins: [router] } })
    await flushPromises()

    const sortButtons = wrapper.findAll('.sort-bar button')
    expect(sortButtons).toHaveLength(4)
    expect(sortButtons.map(button => button.text())).toEqual(['最新', '热度', '价格↑', '价格↓'])
    expect(sortButtons[0].classes()).toContain('active')
    expect(listSource).toMatch(/\.sort-bar button \{[^}]*background:\s*#fff;/s)
    expect(listSource).toMatch(/\.sort-bar button \{[^}]*color:\s*#253044;/s)
    expect(listSource).toMatch(/\.sort-bar button\.active \{[^}]*background:\s*#1677ff;[^}]*color:\s*#fff;/s)
  })
})
