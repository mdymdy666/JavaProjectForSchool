import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'

import App from './App.vue'

describe('App', () => {
  it('renders the campus trade dashboard shell', () => {
    const wrapper = mount(App)

    expect(wrapper.text()).toContain('校园二手交易平台')
    expect(wrapper.text()).toContain('商品市场')
    expect(wrapper.text()).toContain('订单交易')
    expect(wrapper.findAll('[data-test="product-card"]')).toHaveLength(2)
  })

  it('creates an order from an approved product', async () => {
    const wrapper = mount(App)

    await wrapper.find('[data-test="product-card"] button').trigger('click')

    expect(wrapper.text()).toContain('订单已创建，等待模拟支付')
    expect(wrapper.findAll('[data-test="order-row"]').length).toBeGreaterThanOrEqual(3)
  })
})
