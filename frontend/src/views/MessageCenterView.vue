<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getMessages, markMessageRead, getNotifications, markNotificationRead } from '../api/message'
import type { SiteMessage, Notification } from '../types/domain'

const activeTab = ref<'messages' | 'notifications'>('messages')
const messages = ref<SiteMessage[]>([])
const notifications = ref<Notification[]>([])

async function fetch() {
  try {
    const [mRes, nRes] = await Promise.all([getMessages(), getNotifications()])
    messages.value = mRes.data || []
    notifications.value = nRes.data || []
  } catch { /* ignore */ }
}

async function readMessage(id: number) {
  try { await markMessageRead(id); await fetch() } catch { /* ignore */ }
}

async function readNotification(id: number) {
  try { await markNotificationRead(id); await fetch() } catch { /* ignore */ }
}

onMounted(fetch)
</script>

<template>
  <div class="message-page">
    <h2>消息中心</h2>

    <div class="tabs">
      <button :class="{ active: activeTab === 'messages' }" @click="activeTab = 'messages'">
        交易留言
      </button>
      <button :class="{ active: activeTab === 'notifications' }" @click="activeTab = 'notifications'">
        系统通知
      </button>
    </div>

    <div v-if="activeTab === 'messages'">
      <div v-if="messages.length" class="msg-list">
        <div v-for="m in messages" :key="m.id" class="msg-card" :class="{ unread: m.readStatus === 'UNREAD' }">
          <div class="msg-head">
            <strong>{{ m.senderNickname }}</strong>
            <small>{{ m.productTitle }}</small>
            <span :class="['dot', m.readStatus === 'UNREAD' ? 'unread-dot' : '']" />
          </div>
          <p>{{ m.content }}</p>
          <div class="msg-foot">
            <small>{{ m.createdAt }}</small>
            <button v-if="m.readStatus === 'UNREAD'" @click="readMessage(m.id)">标为已读</button>
          </div>
        </div>
      </div>
      <p v-else class="empty">暂无留言</p>
    </div>

    <div v-if="activeTab === 'notifications'">
      <div v-if="notifications.length" class="msg-list">
        <div v-for="n in notifications" :key="n.id" class="msg-card" :class="{ unread: n.readStatus === 'UNREAD' }">
          <div class="msg-head">
            <strong>{{ n.title }}</strong>
            <span :class="['dot', n.readStatus === 'UNREAD' ? 'unread-dot' : '']" />
          </div>
          <p>{{ n.content }}</p>
          <div class="msg-foot">
            <small>{{ n.createdAt }}</small>
            <button v-if="n.readStatus === 'UNREAD'" @click="readNotification(n.id)">标为已读</button>
          </div>
        </div>
      </div>
      <p v-else class="empty">暂无通知</p>
    </div>
  </div>
</template>

<style scoped>
.message-page { max-width: 720px; margin: 0 auto; }
.message-page h2 { margin: 0 0 16px; }
.tabs { display: flex; gap: 8px; margin-bottom: 16px; }
.tabs button {
  padding: 6px 20px; border: 1px solid #d9d9d9; border-radius: 6px;
  background: #fff; cursor: pointer; font-size: 14px;
}
.tabs button.active { background: #1677ff; color: #fff; border-color: #1677ff; }
.msg-list { display: flex; flex-direction: column; gap: 10px; }
.msg-card {
  background: #fff; border: 1px solid #e8e8e8; border-radius: 8px; padding: 14px;
}
.msg-card.unread { border-left: 3px solid #1677ff; }
.msg-head { display: flex; align-items: center; gap: 12px; margin-bottom: 6px; }
.msg-head strong { font-size: 15px; }
.msg-head small { color: #888; }
.dot { width: 8px; height: 8px; border-radius: 50%; }
.unread-dot { background: #1677ff; }
.msg-card p { color: #555; font-size: 14px; margin: 0 0 8px; }
.msg-foot { display: flex; justify-content: space-between; align-items: center; }
.msg-foot small { color: #bbb; }
.msg-foot button { background: none; border: none; color: #1677ff; cursor: pointer; font-size: 13px; }
.empty { color: #999; text-align: center; padding: 32px; }
</style>
