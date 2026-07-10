import { describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import PaymentView from './PaymentView.vue'
import { createOrder, getOrderDetail, payOrder } from '../api/order'
import { getProductDetail } from '../api/product'

vi.mock('../api/order', () => ({
  createOrder: vi.fn(),
  getOrderDetail: vi.fn().mockResolvedValue({
    code: 200,
    data: {
      id: 4,
      orderNo: 'CT202607100001',
      productId: 2,
      productTitle: '罗技 G502 游戏鼠标',
      buyerId: 4,
      buyerNickname: '软件小陈',
      sellerId: 2,
      sellerNickname: '计科小李',
      totalAmount: 89,
      status: 'PENDING_PAYMENT',
      logisticsInfo: null,
      version: 0,
      createdAt: '2026-07-10T14:45:11'
    }
  }),
  payOrder: vi.fn().mockResolvedValue({ code: 200, data: { id: 4, status: 'PAID' } })
}))

vi.mock('../api/product', () => ({
  getProductDetail: vi.fn()
}))

describe('PaymentView', () => {
  it('pays an existing order without creating another product order', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/pay/order/:orderId', name: 'pay-order', component: PaymentView },
        { path: '/orders', component: { template: '<div />' } },
        { path: '/home', component: { template: '<div />' } }
      ]
    })
    await router.push('/pay/order/4')
    await router.isReady()

    const wrapper = mount(PaymentView, { global: { plugins: [createPinia(), router] } })
    await flushPromises()

    expect(getOrderDetail).toHaveBeenCalledWith(4)
    expect(getProductDetail).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('罗技 G502 游戏鼠标')
    expect(wrapper.text()).toContain('¥89.00')

    await wrapper.get('button.btn-pay').trigger('click')
    await flushPromises()

    expect(payOrder).toHaveBeenCalledWith(4)
    expect(createOrder).not.toHaveBeenCalled()
  })
})
