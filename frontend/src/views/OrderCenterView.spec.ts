import { describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import OrderCenterView from './OrderCenterView.vue'
import orderSource from './OrderCenterView.vue?raw'

const { shippedOrder } = vi.hoisted(() => ({
  shippedOrder: {
    id: 6,
    orderNo: 'CT20260706001',
    productId: 3,
    productTitle: 'Cherry 机械键盘 MX3.0S',
    buyerId: 1,
    buyerNickname: '计科小李',
    sellerId: 2,
    sellerNickname: '软件小陈',
    totalAmount: 129,
    status: 'SHIPPED',
    logisticsInfo: '申通快递 7730123456789',
    version: 1,
    createdAt: '2026-07-07T08:40:38'
  }
}))

vi.mock('../api/order', () => ({
  createOrder: vi.fn(),
  payOrder: vi.fn(),
  shipOrder: vi.fn(),
  confirmOrder: vi.fn(),
  cancelOrder: vi.fn(),
  getBuyerOrders: vi.fn().mockResolvedValue({ code: 200, data: [shippedOrder] }),
  getSellerOrders: vi.fn().mockResolvedValue({ code: 200, data: [] })
}))

describe('OrderCenterView', () => {
  it('shows readable buyer and seller tab labels', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/orders', component: OrderCenterView }]
    })
    await router.push('/orders')
    await router.isReady()

    const wrapper = mount(OrderCenterView, { global: { plugins: [router] } })
    await flushPromises()

    const tabs = wrapper.findAll('.tabs button')
    expect(tabs.map(tab => tab.text())).toEqual(['我买的', '我卖的'])
    expect(tabs[0].classes()).toContain('active')
    expect(orderSource).toMatch(/\.tabs button \{[^}]*color:\s*#253044;/s)
    expect(orderSource).toMatch(/\.tabs button\.active \{[^}]*color:\s*#fff;/s)
  })

  it('shows an identifiable close control in the logistics dialog', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/orders', component: OrderCenterView }]
    })
    await router.push('/orders')
    await router.isReady()

    const wrapper = mount(OrderCenterView, { global: { plugins: [router] } })
    await flushPromises()

    await wrapper.get('button.track-btn').trigger('click')
    const close = wrapper.get('button.close-btn')

    expect(close.attributes('aria-label')).toBe('关闭物流信息')
    expect(close.text()).toContain('关闭')
    expect(close.find('[data-icon="close"]').exists()).toBe(true)
  })
})
