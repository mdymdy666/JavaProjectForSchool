import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getMessages, getNotifications } from '../api/message'
import { useAuthStore } from './auth'

export const useNotificationStore = defineStore('notification', () => {
  const unreadCount = ref(0)

  async function refresh() {
    try {
      const auth = useAuthStore()
      const [mRes, nRes] = await Promise.all([getMessages(), getNotifications()])
      const msgs = mRes.data || []
      const notifs = nRes.data || []
      unreadCount.value = msgs.filter(m =>
        m.receiverId === auth.userId && m.readStatus === 'UNREAD').length +
        notifs.filter(n => n.readStatus === 'UNREAD').length
    } catch { /* */ }
  }

  function setCount(n: number) { unreadCount.value = n }

  return { unreadCount, refresh, setCount }
})
