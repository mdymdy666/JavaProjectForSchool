import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import ChatPanel from './ChatPanel.vue'
import type { SiteMessage } from '../../types/domain'

const messages: SiteMessage[] = [
  {
    id: 1, senderId: 2, senderNickname: '卖家', receiverId: 1, receiverNickname: '买家',
    productId: 7, productTitle: '台灯', content: '还在的', readStatus: 'UNREAD',
    createdAt: '2026-07-06T10:00:00'
  },
  {
    id: 2, senderId: 1, senderNickname: '买家', receiverId: 2, receiverNickname: '卖家',
    productId: 7, productTitle: '台灯', content: '今晚能自提吗', readStatus: 'READ',
    createdAt: '2026-07-06T10:01:00'
  }
]

describe('ChatPanel', () => {
  it('renders message sides and emits trimmed text from the composer', async () => {
    const wrapper = mount(ChatPanel, {
      props: { messages, currentUserId: 1, sending: false, error: '' }
    })

    const bubbles = wrapper.findAll('[data-test="chat-bubble"]')
    expect(bubbles[0].attributes('data-side')).toBe('incoming')
    expect(bubbles[1].attributes('data-side')).toBe('outgoing')

    const textarea = wrapper.get('textarea')
    await textarea.setValue('  还可以便宜吗？  ')
    await textarea.trigger('keydown', { key: 'Enter', shiftKey: true })
    expect(wrapper.emitted('send')).toBeUndefined()
    await textarea.trigger('keydown', { key: 'Enter', shiftKey: false })
    expect(wrapper.emitted('send')?.[0]).toEqual(['还可以便宜吗？'])
  })
})
