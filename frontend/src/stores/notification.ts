import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getMessages, getNotifications } from '../api/message'

export const useNotificationStore = defineStore('notification', () => {
  const unreadCount = ref(0)

  async function refresh() {
    try {
      const [mRes, nRes] = await Promise.all([getMessages(), getNotifications()])
      const msgs = mRes.data || []
      const notifs = nRes.data || []
      unreadCount.value = msgs.filter(m => m.readStatus === 'UNREAD').length +
                          notifs.filter(n => n.readStatus === 'UNREAD').length
    } catch { /* */ }
  }

  function setCount(n: number) { unreadCount.value = n }

  return { unreadCount, refresh, setCount }
})
