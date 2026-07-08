import type { SiteMessage } from '../../types/domain'

export interface Conversation {
  key: string
  counterpartId: number
  counterpartNickname: string
  productId: number
  productTitle: string
  messages: SiteMessage[]
  lastMessage: SiteMessage
  unreadCount: number
}

export function buildConversations(messages: SiteMessage[], currentUserId: number): Conversation[] {
  const groups = new Map<string, SiteMessage[]>()

  for (const message of messages) {
    if (message.productId == null) continue
    const counterpartId = message.senderId === currentUserId ? message.receiverId : message.senderId
    const key = `${counterpartId}:${message.productId}`
    const group = groups.get(key) || []
    group.push(message)
    groups.set(key, group)
  }

  return [...groups.entries()].map(([key, groupedMessages]) => {
    const sorted = [...groupedMessages].sort((left, right) =>
      left.createdAt.localeCompare(right.createdAt) || left.id - right.id)
    const lastMessage = sorted[sorted.length - 1]
    const counterpartIsReceiver = lastMessage.senderId === currentUserId
    return {
      key,
      counterpartId: counterpartIsReceiver ? lastMessage.receiverId : lastMessage.senderId,
      counterpartNickname: counterpartIsReceiver
        ? lastMessage.receiverNickname
        : lastMessage.senderNickname,
      productId: lastMessage.productId!,
      productTitle: lastMessage.productTitle || '商品已删除',
      messages: sorted,
      lastMessage,
      unreadCount: sorted.filter(message =>
        message.receiverId === currentUserId && message.readStatus === 'UNREAD').length
    }
  }).sort((left, right) =>
    right.lastMessage.createdAt.localeCompare(left.lastMessage.createdAt)
      || right.lastMessage.id - left.lastMessage.id)
}
