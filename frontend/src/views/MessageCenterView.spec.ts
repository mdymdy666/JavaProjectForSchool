import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import MessageCenterView from './MessageCenterView.vue'

const getMessages = vi.fn()
const sendMessage = vi.fn()

vi.mock('../api/message', () => ({
  getMessages: (...args: unknown[]) => getMessages(...args),
  sendMessage: (...args: unknown[]) => sendMessage(...args),
  markMessageRead: vi.fn().mockResolvedValue({ code: 200 }),
  getNotifications: vi.fn().mockResolvedValue({ data: [] }),
  markNotificationRead: vi.fn().mockResolvedValue({ code: 200 })
}))

describe('MessageCenterView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getMessages.mockResolvedValue({ data: [
      { id: 1, senderId: 1, senderNickname: '买家', receiverId: 2, receiverNickname: '台灯卖家', productId: 7, productTitle: '台灯', content: '台灯还在吗', readStatus: 'READ', createdAt: '2026-07-06T10:00:00' },
      { id: 2, senderId: 3, senderNickname: '教材卖家', receiverId: 1, receiverNickname: '买家', productId: 9, productTitle: '教材', content: '教材还在', readStatus: 'UNREAD', createdAt: '2026-07-06T11:00:00' }
    ] })
    sendMessage.mockResolvedValue({ code: 200, data: { id: 3 } })
  })

  it('shows grouped conversations and sends through the selected conversation', async () => {
    const pinia = createPinia()
    useAuthStore(pinia).saveSession('token', 1, '买家', 'USER')
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/messages', component: MessageCenterView }]
    })
    await router.push('/messages')
    await router.isReady()
    const wrapper = mount(MessageCenterView, { global: { plugins: [pinia, router] } })
    await flushPromises()

    expect(wrapper.findAll('[data-test="conversation-item"]')).toHaveLength(2)
    expect(wrapper.find('input[placeholder="输入用户ID"]').exists()).toBe(false)
    expect(wrapper.text()).toContain('教材卖家')

    await wrapper.get('textarea').setValue('请问在哪里自提？')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(sendMessage).toHaveBeenCalledWith({
      receiverId: 3,
      productId: 9,
      content: '请问在哪里自提？'
    })
  })
})
