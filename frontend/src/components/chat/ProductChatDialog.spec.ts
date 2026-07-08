import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import ProductChatDialog from './ProductChatDialog.vue'

const getMessages = vi.fn()
const sendMessage = vi.fn()
const markMessageRead = vi.fn()

vi.mock('../../api/message', () => ({
  getMessages: (...args: unknown[]) => getMessages(...args),
  sendMessage: (...args: unknown[]) => sendMessage(...args),
  markMessageRead: (...args: unknown[]) => markMessageRead(...args),
  getNotifications: vi.fn().mockResolvedValue({ data: [] })
}))

describe('ProductChatDialog', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getMessages.mockResolvedValue({ data: [
      { id: 1, senderId: 2, senderNickname: '软件小陈', receiverId: 1, receiverNickname: '买家', productId: 7, productTitle: '台灯', content: '还在的', readStatus: 'UNREAD', createdAt: '2026-07-06T10:00:00' },
      { id: 2, senderId: 3, senderNickname: '其他卖家', receiverId: 1, receiverNickname: '买家', productId: 9, productTitle: '教材', content: '其他会话', readStatus: 'UNREAD', createdAt: '2026-07-06T10:01:00' }
    ] })
    sendMessage.mockResolvedValue({ code: 200, data: { id: 3 } })
    markMessageRead.mockResolvedValue({ code: 200 })
  })

  it('shows the product conversation and sends to the bound seller', async () => {
    const wrapper = mount(ProductChatDialog, {
      props: {
        open: true,
        productId: 7,
        productTitle: '小米台灯 1S',
        sellerId: 2,
        sellerNickname: '软件小陈',
        currentUserId: 1
      },
      global: { plugins: [createPinia()] }
    })
    await flushPromises()

    expect(wrapper.text()).toContain('软件小陈')
    expect(wrapper.text()).toContain('小米台灯 1S')
    expect(wrapper.text()).toContain('还在的')
    expect(wrapper.text()).not.toContain('其他会话')

    await wrapper.get('textarea').setValue('今晚能自提吗？')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(sendMessage).toHaveBeenCalledWith({
      receiverId: 2,
      productId: 7,
      content: '今晚能自提吗？'
    })
  })
})
