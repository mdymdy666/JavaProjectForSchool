import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from './auth'
import { useNotificationStore } from './notification'

const getMessages = vi.fn()
const getNotifications = vi.fn()

vi.mock('../api/auth', () => ({
  loginApi: vi.fn(),
  logoutApi: vi.fn().mockResolvedValue({ code: 200, message: 'success', data: null })
}))

vi.mock('../api/message', () => ({
  getMessages: (...args: unknown[]) => getMessages(...args),
  getNotifications: (...args: unknown[]) => getNotifications(...args)
}))

describe('notification store', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.clearAllMocks()
    setActivePinia(createPinia())
  })

  it('counts only unread messages received by the current user', async () => {
    useAuthStore().saveSession('token', 1, '计科小李', 'USER')
    getMessages.mockResolvedValue({
      data: [
        { id: 1, senderId: 2, receiverId: 1, readStatus: 'UNREAD' },
        { id: 2, senderId: 1, receiverId: 2, readStatus: 'UNREAD' },
        { id: 3, senderId: 3, receiverId: 1, readStatus: 'READ' }
      ]
    })
    getNotifications.mockResolvedValue({
      data: [
        { id: 1, readStatus: 'UNREAD' },
        { id: 2, readStatus: 'READ' }
      ]
    })

    const store = useNotificationStore()
    await store.refresh()

    expect(store.unreadCount).toBe(2)
  })
})
