<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { getMessages, markMessageRead, sendMessage, getNotifications, markNotificationRead } from '../api/message'
import { useNotificationStore } from '../stores/notification'
import type { SiteMessage, Notification } from '../types/domain'

const notifyStore = useNotificationStore()
const activeTab = ref<'messages' | 'notifications'>('messages')
const messages = ref<SiteMessage[]>([])
const notifications = ref<Notification[]>([])
const showSend = ref(false)

const newMsg = reactive({ receiverId: 0, productId: null as number | null, content: '' })
const sendErr = ref('')
const sending = ref(false)

async function fetch() {
  try {
    const [mRes, nRes] = await Promise.all([getMessages(), getNotifications()])
    messages.value = mRes.data || []
    notifications.value = nRes.data || []
    notifyStore.refresh()
  } catch { /* */ }
}

async function readMsg(id: number) {
  try { await markMessageRead(id); await fetch() } catch { /* */ }
}
async function readNotif(id: number) {
  try { await markNotificationRead(id); await fetch() } catch { /* */ }
}
async function readAllNotifs() {
  for (const n of notifications.value) {
    if (n.readStatus === 'UNREAD') await markNotificationRead(n.id)
  }
  await fetch()
}
async function readAllMsgs() {
  for (const m of messages.value) {
    if (m.readStatus === 'UNREAD') await markMessageRead(m.id)
  }
  await fetch()
}

async function doSend() {
  sendErr.value = ''
  if (!newMsg.receiverId || !newMsg.content.trim()) return
  sending.value = true
  try {
    const res = await sendMessage({
      receiverId: newMsg.receiverId,
      productId: newMsg.productId || undefined as unknown as number,
      content: newMsg.content.trim()
    })
    if (res.code === 200) {
      showSend.value = false
      newMsg.content = ''
      await fetch()
    } else {
      sendErr.value = res.message || '发送失败'
    }
  } catch { sendErr.value = '发送失败' }
  finally { sending.value = false }
}

function typeLabel(t: string) {
  const m: Record<string, string> = { ORDER_STATUS: '订单', AUDIT: '审核', SYSTEM: '系统' }
  return m[t] || t
}

onMounted(fetch)
</script>

<template>
  <div class="msg-page">
    <div class="msg-header">
      <h2>消息中心</h2>
      <div class="tabs">
        <button :class="{ active: activeTab === 'messages' }" @click="activeTab = 'messages'">
          交易留言 ({{ messages.length }})
        </button>
        <button :class="{ active: activeTab === 'notifications' }" @click="activeTab = 'notifications'">
          系统通知 ({{ notifications.length }})
        </button>
      </div>
    </div>

    <!-- 交易留言 -->
    <div v-if="activeTab === 'messages'">
      <div class="toolbar">
        <button class="btn-send" @click="showSend = true">+ 发送留言</button>
        <button v-if="messages.some(m => m.readStatus === 'UNREAD')" class="btn-all" @click="readAllMsgs">全部已读</button>
      </div>

      <div v-if="messages.length" class="list">
        <div v-for="m in messages" :key="m.id" :class="['card', { unread: m.readStatus === 'UNREAD' }]">
          <div class="card-head">
            <div class="card-meta">
              <span class="from">{{ m.senderNickname }}</span>
              <span class="arrow">→</span>
              <span class="to">{{ m.receiverNickname }}</span>
              <span v-if="m.productTitle" class="product-tag">{{ m.productTitle }}</span>
            </div>
            <span v-if="m.readStatus === 'UNREAD'" class="dot" />
          </div>
          <p class="content">{{ m.content }}</p>
          <div class="card-foot">
            <small>{{ m.createdAt?.slice(0, 16) }}</small>
            <button v-if="m.readStatus === 'UNREAD'" @click="readMsg(m.id)">标为已读</button>
          </div>
        </div>
      </div>
      <p v-else class="empty">暂无留言</p>
    </div>

    <!-- 系统通知 -->
    <div v-if="activeTab === 'notifications'">
      <div class="toolbar">
        <button v-if="notifications.some(n => n.readStatus === 'UNREAD')" class="btn-all" @click="readAllNotifs">全部已读</button>
      </div>

      <div v-if="notifications.length" class="list">
        <div v-for="n in notifications" :key="n.id" :class="['card', { unread: n.readStatus === 'UNREAD' }]">
          <div class="card-head">
            <div class="card-meta">
              <span :class="['type-tag', n.type]">{{ typeLabel(n.type) }}</span>
              <strong>{{ n.title }}</strong>
            </div>
            <span v-if="n.readStatus === 'UNREAD'" class="dot" />
          </div>
          <p class="content">{{ n.content }}</p>
          <div class="card-foot">
            <small>{{ n.createdAt?.slice(0, 16) }}</small>
            <button v-if="n.readStatus === 'UNREAD'" @click="readNotif(n.id)">标为已读</button>
          </div>
        </div>
      </div>
      <p v-else class="empty">暂无通知</p>
    </div>

    <!-- 发送留言弹窗 -->
    <div v-if="showSend" class="modal" @click.self="showSend = false">
      <div class="modal-card">
        <h3>发送留言</h3>
        <label>接收方用户 ID <input v-model.number="newMsg.receiverId" type="number" placeholder="输入用户ID" /></label>
        <label>关联商品 ID <input v-model.number="newMsg.productId" type="number" placeholder="可选" /></label>
        <label>内容 <textarea v-model="newMsg.content" rows="3" placeholder="输入留言内容..." /></label>
        <p v-if="sendErr" class="err">{{ sendErr }}</p>
        <div class="modal-actions">
          <button :disabled="sending" @click="doSend">{{ sending ? '发送中...' : '发送' }}</button>
          <button class="ghost" @click="showSend = false">取消</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.msg-page { max-width: 760px; margin: 0 auto; }
h2 { margin: 0; font-size: 22px; }
.msg-header { margin-bottom: 20px; }
.tabs { display: flex; gap: 8px; margin-top: 14px; }
.tabs button {
  padding: 6px 18px; border: 1px solid #d9d9d9; border-radius: 6px;
  background: #fff; cursor: pointer; font-size: 14px;
}
.tabs button.active { background: #1677ff; color: #fff; border-color: #1677ff; }
.toolbar { display: flex; gap: 8px; margin-bottom: 14px; }
.btn-send { padding: 6px 16px; background: #1677ff; color: #fff; border: none; border-radius: 6px; cursor: pointer; font-size: 13px; }
.btn-all { padding: 6px 16px; border: 1px solid #d9d9d9; border-radius: 6px; background: #fff; cursor: pointer; font-size: 13px; color: #666; }
.list { display: flex; flex-direction: column; gap: 10px; }
.card {
  background: #fff; border: 1px solid #e8e8e8; border-radius: 8px; padding: 16px; transition: 0.15s;
}
.card.unread { border-left: 3px solid #1677ff; background: #fafbff; }
.card-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.card-meta { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.from { font-weight: 600; color: #333; }
.arrow { color: #bbb; font-size: 13px; }
.to { color: #888; font-size: 14px; }
.product-tag { background: #f0f0f0; padding: 1px 8px; border-radius: 3px; font-size: 12px; color: #888; }
.type-tag { padding: 1px 8px; border-radius: 3px; font-size: 11px; font-weight: 600; color: #fff; }
.type-tag.ORDER_STATUS { background: #1677ff; }
.type-tag.AUDIT { background: #faad14; }
.type-tag.SYSTEM { background: #52c41a; }
.dot { width: 8px; height: 8px; border-radius: 50%; background: #1677ff; flex-shrink: 0; }
.content { color: #555; font-size: 14px; margin: 0 0 8px; line-height: 1.6; }
.card-foot { display: flex; justify-content: space-between; align-items: center; }
.card-foot small { color: #bbb; font-size: 12px; }
.card-foot button { background: none; border: none; color: #1677ff; cursor: pointer; font-size: 13px; }
.empty { text-align: center; padding: 48px 0; color: #999; }

/* Modal */
.modal { position: fixed; inset: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 200; }
.modal-card { background: #fff; border-radius: 12px; padding: 28px; width: 440px; max-width: 90vw; }
.modal-card h3 { margin: 0 0 16px; }
.modal-card label { display: block; margin-bottom: 12px; font-size: 13px; color: #555; }
.modal-card input, .modal-card textarea { display: block; width: 100%; margin-top: 4px; padding: 8px 10px; border: 1px solid #d9d9d9; border-radius: 6px; font-size: 14px; box-sizing: border-box; resize: vertical; }
.modal-actions { display: flex; gap: 10px; margin-top: 4px; }
.modal-actions button { padding: 8px 24px; background: #1677ff; color: #fff; border: none; border-radius: 6px; cursor: pointer; }
.modal-actions button:disabled { opacity: 0.5; }
.modal-actions button.ghost { background: #fff; color: #666; border: 1px solid #d9d9d9; }
.err { color: #ff4d4f; font-size: 12px; margin: 8px 0 0; }
</style>
