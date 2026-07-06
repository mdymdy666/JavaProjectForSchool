import { describe, expect, it } from 'vitest'
import type { SiteMessage } from '../../types/domain'
import { buildConversations } from './conversations'

function message(overrides: Partial<SiteMessage> & Pick<SiteMessage, 'id'>): SiteMessage {
  return {
    id: overrides.id,
    senderId: 1,
    senderNickname: '买家',
    receiverId: 2,
    receiverNickname: '卖家',
    productId: 7,
    productTitle: '台灯',
    content: '你好',
    readStatus: 'READ',
    createdAt: '2026-07-06T10:00:00',
    ...overrides
  }
}

describe('buildConversations', () => {
  it('groups by counterpart and product with chronological messages and incoming unread counts', () => {
    const messages = [
      message({ id: 2, senderId: 2, senderNickname: '卖家', receiverId: 1, receiverNickname: '买家', content: '还在', readStatus: 'UNREAD', createdAt: '2026-07-06T10:02:00' }),
      message({ id: 3, receiverId: 3, receiverNickname: '书店卖家', productId: 9, productTitle: '教材', content: '书还有吗', createdAt: '2026-07-06T11:00:00' }),
      message({ id: 1, content: '台灯还在吗', createdAt: '2026-07-06T10:01:00' }),
      message({ id: 4, senderId: 1, receiverId: 2, productId: null, productTitle: null })
    ]

    const conversations = buildConversations(messages, 1)

    expect(conversations).toHaveLength(2)
    expect(conversations[0].key).toBe('3:9')
    expect(conversations[1].key).toBe('2:7')
    expect(conversations[1].counterpartNickname).toBe('卖家')
    expect(conversations[1].unreadCount).toBe(1)
    expect(conversations[1].messages.map(item => item.id)).toEqual([1, 2])
  })
})
