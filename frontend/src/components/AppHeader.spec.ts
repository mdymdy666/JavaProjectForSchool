import { describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import AppHeader from './AppHeader.vue'

vi.mock('../api/message', () => ({
  getMessages: vi.fn().mockResolvedValue({ data: [] }),
  getNotifications: vi.fn().mockResolvedValue({ data: [] })
}))

describe('AppHeader', () => {
  it('shows an identifiable shopping cart control', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div />' } }, { path: '/cart', component: { template: '<div />' } }]
    })
    await router.push('/')
    await router.isReady()
    const wrapper = mount(AppHeader, { global: { plugins: [createPinia(), router] } })

    const cart = wrapper.get('button.cart')
    expect(cart.attributes('aria-label')).toBe('购物车')
    expect(cart.text()).toContain('购物车')
    expect(cart.find('[data-icon="cart"]').exists()).toBe(true)
  })
})
