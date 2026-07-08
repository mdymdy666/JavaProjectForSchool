<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getMessages, getNotifications, markMessageRead, markNotificationRead, sendMessage } from '../api/message'
import ChatPanel from '../components/chat/ChatPanel.vue'
import { buildConversations } from '../features/chat/conversations'
import { useAuthStore } from '../stores/auth'
import { useNotificationStore } from '../stores/notification'
import type { Notification, SiteMessage } from '../types/domain'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const notificationStore = useNotificationStore()
const activeTab = ref<'messages' | 'notifications'>('messages')
const messages = ref<SiteMessage[]>([])
const notifications = ref<Notification[]>([])
const activeKey = ref('')
const mobileChatOpen = ref(false)
const loading = ref(true)
const sending = ref(false)
const error = ref('')
const chatPanel = ref<{ clearDraft: () => void } | null>(null)
let pollTimer: ReturnType<typeof setInterval> | null = null

const currentUserId = computed(() => auth.userId || 0)
const conversations = computed(() => buildConversations(messages.value, currentUserId.value))
const activeConversation = computed(() =>
  conversations.value.find(conversation => conversation.key === activeKey.value) || null)

function apiError(cause: unknown, fallback: string) {
  return (cause as { response?: { data?: { message?: string } } })?.response?.data?.message || fallback
}

function desiredConversationKey() {
  const counterpartId = Number(route.query.counterpartId)
  const productId = Number(route.query.productId)
  return counterpartId && productId ? `${counterpartId}:${productId}` : ''
}

function ensureActiveConversation() {
  const desired = desiredConversationKey()
  if (desired && conversations.value.some(conversation => conversation.key === desired)) {
    activeKey.value = desired
    mobileChatOpen.value = true
    return
  }
  if (!conversations.value.some(conversation => conversation.key === activeKey.value)) {
    activeKey.value = conversations.value[0]?.key || ''
  }
}

async function load(silent = false) {
  if (!silent) loading.value = true
  try {
    const [messageResponse, notificationResponse] = await Promise.all([getMessages(), getNotifications()])
    messages.value = messageResponse.data || []
    notifications.value = notificationResponse.data || []
    ensureActiveConversation()
    error.value = ''
  } catch (cause) {
    if (!silent) error.value = apiError(cause, '消息加载失败，请重试')
  } finally {
    if (!silent) loading.value = false
  }
}

async function markConversationRead() {
  const conversation = activeConversation.value
  if (!conversation) return
  const unread = conversation.messages.filter(message =>
    message.receiverId === currentUserId.value && message.readStatus === 'UNREAD')
  if (!unread.length) return
  try {
    await Promise.all(unread.map(message => markMessageRead(message.id)))
    const ids = new Set(unread.map(message => message.id))
    messages.value = messages.value.map(message =>
      ids.has(message.id) ? { ...message, readStatus: 'READ' } : message)
    await notificationStore.refresh()
  } catch (cause) {
    error.value = apiError(cause, '已读状态更新失败')
  }
}

async function selectConversation(key: string) {
  activeKey.value = key
  mobileChatOpen.value = true
  const conversation = activeConversation.value
  if (conversation) {
    await router.replace({ query: { counterpartId: conversation.counterpartId, productId: conversation.productId } })
  }
  await markConversationRead()
}

async function handleSend(content: string) {
  const conversation = activeConversation.value
  if (!conversation) return
  sending.value = true
  error.value = ''
  try {
    await sendMessage({ receiverId: conversation.counterpartId, productId: conversation.productId, content })
    chatPanel.value?.clearDraft()
    await load(true)
    await notificationStore.refresh()
  } catch (cause) {
    error.value = apiError(cause, '发送失败，请稍后重试')
  } finally {
    sending.value = false
  }
}

async function readNotification(id: number) {
  try {
    await markNotificationRead(id)
    notifications.value = notifications.value.map(item => item.id === id ? { ...item, readStatus: 'READ' } : item)
    await notificationStore.refresh()
  } catch (cause) {
    error.value = apiError(cause, '通知状态更新失败')
  }
}

async function readAllNotifications() {
  const unread = notifications.value.filter(item => item.readStatus === 'UNREAD')
  await Promise.all(unread.map(item => markNotificationRead(item.id)))
  notifications.value = notifications.value.map(item => ({ ...item, readStatus: 'READ' }))
  await notificationStore.refresh()
}

async function readAllMessages() {
  const unread = messages.value.filter(message =>
    message.receiverId === currentUserId.value && message.readStatus === 'UNREAD')
  await Promise.all(unread.map(message => markMessageRead(message.id)))
  messages.value = messages.value.map(message =>
    message.receiverId === currentUserId.value ? { ...message, readStatus: 'READ' } : message)
  await notificationStore.refresh()
}

function typeLabel(type: string) {
  return ({ ORDER_STATUS: '订单', AUDIT: '审核', SYSTEM: '系统' } as Record<string, string>)[type] || type
}

function formatTime(value: string) {
  if (!value) return ''
  return new Date(value).toLocaleString('zh-CN', {
    month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hour12: false
  })
}

function startPolling() {
  pollTimer = setInterval(() => load(true), 5000)
}

onMounted(async () => {
  await load()
  await markConversationRead()
  startPolling()
})

onBeforeUnmount(() => {
  if (pollTimer) clearInterval(pollTimer)
})
</script>

<template>
  <main class="message-page" data-test="message-community">
    <header class="page-header">
      <div>
        <p class="eyebrow">消息中心</p>
        <h1>边聊边交易，记录都留在平台里</h1>
        <p>查看商品咨询、交易进度和系统通知。</p>
      </div>
      <nav class="tabs" aria-label="消息分类">
        <button :class="{ active: activeTab === 'messages' }" @click="activeTab = 'messages'">交易会话</button>
        <button :class="{ active: activeTab === 'notifications' }" @click="activeTab = 'notifications'">系统通知</button>
      </nav>
    </header>

    <p v-if="error" class="page-error" role="alert">{{ error }}</p>

    <section v-if="activeTab === 'messages'" :class="['community-layout', { 'mobile-chat-open': mobileChatOpen }]">
      <aside class="conversation-sidebar">
        <div class="sidebar-header">
          <div><strong>正在聊</strong><span>{{ conversations.length }} 个会话</span></div>
          <button v-if="conversations.some(item => item.unreadCount)" @click="readAllMessages">全部已读</button>
        </div>
        <div v-if="loading" class="state">正在加载...</div>
        <div v-else-if="conversations.length" class="conversation-list">
          <button
            v-for="conversation in conversations"
            :key="conversation.key"
            data-test="conversation-item"
            :class="['conversation-item', { active: conversation.key === activeKey }]"
            @click="selectConversation(conversation.key)"
          >
            <span class="avatar">{{ conversation.counterpartNickname.slice(0, 1) }}</span>
            <span class="conversation-copy">
              <span class="conversation-line"><strong>{{ conversation.counterpartNickname }}</strong><time>{{ formatTime(conversation.lastMessage.createdAt) }}</time></span>
              <span class="product-name">{{ conversation.productTitle }}</span>
              <span class="last-message">{{ conversation.lastMessage.content }}</span>
            </span>
            <span v-if="conversation.unreadCount" class="unread-badge">{{ conversation.unreadCount }}</span>
          </button>
        </div>
        <div v-else class="state">暂无交易会话</div>
      </aside>

      <section class="chat-workspace">
        <template v-if="activeConversation">
          <header class="chat-header">
            <button class="back-button" type="button" aria-label="返回会话列表" @click="mobileChatOpen = false">‹</button>
            <div class="chat-avatar">{{ activeConversation.counterpartNickname.slice(0, 1) }}</div>
            <div><strong>{{ activeConversation.counterpartNickname }}</strong><p>{{ activeConversation.productTitle }} · 同校认证</p></div>
            <button class="product-link" type="button" @click="router.push(`/products/${activeConversation.productId}`)">查看商品</button>
          </header>
          <ChatPanel
            ref="chatPanel"
            :messages="activeConversation.messages"
            :current-user-id="currentUserId"
            :sending="sending"
            :error="error"
            @send="handleSend"
          />
        </template>
        <div v-else class="state large">选择一个会话开始聊天</div>
      </section>

      <aside class="community-side">
        <section class="side-card">
          <div class="side-head"><h3>校园动态</h3><button>更多</button></div>
          <article class="feed-item"><strong>毕业季闲置市集活动开始啦</strong><p>6月12日-6月15日，南区广场集中交易。</p></article>
          <article class="feed-item"><strong>环保协会旧物新生计划</strong><p>让闲置用品找到新主人，一起环保传递爱心。</p></article>
        </section>
        <section class="side-card safe">
          <div class="side-head"><h3>安全提醒</h3><button>规则</button></div>
          <ul>
            <li>优先选择同校认证卖家交易</li>
            <li>线下交易请在公共场所见面</li>
            <li>不要提前转账，谨防诈骗</li>
          </ul>
        </section>
      </aside>
    </section>

    <section v-else class="notification-panel">
      <div class="notification-toolbar">
        <strong>系统通知</strong>
        <button v-if="notifications.some(item => item.readStatus === 'UNREAD')" @click="readAllNotifications">全部已读</button>
      </div>
      <div v-if="notifications.length" class="notification-list">
        <article v-for="notification in notifications" :key="notification.id" :class="['notification-item', { unread: notification.readStatus === 'UNREAD' }]">
          <div class="notification-head"><span :class="['type-tag', notification.type]">{{ typeLabel(notification.type) }}</span><strong>{{ notification.title }}</strong><span v-if="notification.readStatus === 'UNREAD'" class="unread-dot" /></div>
          <p>{{ notification.content }}</p>
          <footer><time>{{ formatTime(notification.createdAt) }}</time><button v-if="notification.readStatus === 'UNREAD'" @click="readNotification(notification.id)">标为已读</button></footer>
        </article>
      </div>
      <div v-else class="state large">暂无系统通知</div>
    </section>
  </main>
</template>

<style scoped>
.message-page { max-width: 1320px; margin: 0 auto; }
.page-header {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 16px;
  padding: 22px;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.06);
}
.eyebrow { margin: 0 0 6px; color: var(--brand-blue); font-weight: 900; }
.page-header h1 { margin: 0; color: #17212b; font-size: 28px; letter-spacing: 0; }
.page-header p { margin: 6px 0 0; color: #71808f; }
.tabs { display: flex; gap: 8px; }
.tabs button {
  height: 38px;
  padding: 0 16px;
  border: 1px solid var(--line);
  border-radius: 999px;
  color: #52606d;
  background: #fff;
  cursor: pointer;
  font-weight: 800;
}
.tabs button.active { color: #fff; border-color: var(--brand-blue); background: var(--brand-blue); }
.page-error { padding: 10px 12px; color: #b42318; background: #fff1f0; border-left: 3px solid #ff4d4f; }
.community-layout {
  display: grid;
  grid-template-columns: 270px minmax(0, 1fr) 280px;
  height: min(720px, calc(100vh - 165px));
  min-height: 560px;
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.06);
}
.conversation-sidebar {
  min-width: 0;
  overflow-y: auto;
  border-right: 1px solid #e6ebf0;
  background: #fbfdff;
}
.sidebar-header, .notification-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 16px;
  border-bottom: 1px solid #e6ebf0;
}
.sidebar-header strong { display: block; font-size: 18px; }
.sidebar-header span { color: #94a3b8; font-size: 12px; }
.sidebar-header button, .notification-toolbar button, .side-head button {
  padding: 0;
  border: 0;
  color: var(--brand-blue);
  background: transparent;
  cursor: pointer;
  font-size: 12px;
  font-weight: 800;
}
.conversation-list { display: grid; }
.conversation-item {
  position: relative;
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr);
  gap: 10px;
  width: 100%;
  padding: 13px;
  border: 0;
  border-bottom: 1px solid #edf0f3;
  border-radius: 0;
  color: #27313b;
  background: transparent;
  text-align: left;
  cursor: pointer;
}
.conversation-item:hover { background: #f2f7ff; }
.conversation-item.active { background: #eaf3ff; box-shadow: inset 3px 0 var(--brand-blue); }
.avatar, .chat-avatar {
  display: grid;
  place-items: center;
  border-radius: 50%;
  color: #fff;
  background: linear-gradient(135deg, var(--brand-blue), #22c55e);
  font-weight: 900;
}
.avatar { width: 42px; height: 42px; }
.chat-avatar { width: 38px; height: 38px; }
.conversation-copy, .conversation-line { min-width: 0; }
.conversation-line { display: flex; align-items: center; justify-content: space-between; gap: 6px; }
.conversation-line strong, .conversation-line time, .product-name, .last-message { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.conversation-line strong { font-size: 14px; }
.conversation-line time { color: #9aa4af; font-size: 10px; }
.product-name { display: block; margin-top: 2px; color: var(--brand-blue); font-size: 12px; }
.last-message { display: block; margin-top: 3px; color: #71808f; font-size: 12px; }
.unread-badge { position: absolute; right: 10px; bottom: 10px; min-width: 18px; height: 18px; padding: 0 5px; border-radius: 9px; color: #fff; background: var(--danger); font-size: 10px; line-height: 18px; text-align: center; }
.chat-workspace { display: grid; grid-template-rows: auto minmax(0, 1fr); min-width: 0; min-height: 0; background: #f8fbff; }
.chat-header { display: flex; align-items: center; gap: 10px; padding: 12px 16px; border-bottom: 1px solid #e6ebf0; background: #fff; }
.chat-header > div:last-of-type { min-width: 0; flex: 1; }
.chat-header strong, .chat-header p { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.chat-header p { margin: 2px 0 0; color: #71808f; font-size: 12px; }
.back-button { display: none; border: 0; color: #52606d; background: transparent; font-size: 25px; }
.product-link { padding: 7px 12px; border: 1px solid var(--brand-blue); border-radius: 8px; color: var(--brand-blue); background: #fff; cursor: pointer; font-weight: 800; }
.community-side {
  display: grid;
  gap: 14px;
  align-content: start;
  padding: 14px;
  border-left: 1px solid #e6ebf0;
  background: #fbfdff;
}
.side-card {
  border: 1px solid #e6edf5;
  border-radius: 12px;
  background: #fff;
  padding: 14px;
}
.side-head { display: flex; align-items: center; justify-content: space-between; gap: 10px; margin-bottom: 10px; }
.side-head h3 { margin: 0; font-size: 17px; }
.feed-item { padding: 10px 0; border-top: 1px solid #edf2f7; }
.feed-item:first-of-type { border-top: 0; }
.feed-item strong { font-size: 14px; }
.feed-item p { margin: 5px 0 0; color: #64748b; line-height: 1.55; }
.safe ul { display: grid; gap: 9px; margin: 0; padding: 0; list-style: none; color: #475569; line-height: 1.55; }
.state { padding: 34px 14px; color: #8792a0; text-align: center; }
.state.large { display: grid; place-items: center; min-height: 360px; }
.notification-panel { overflow: hidden; border: 1px solid var(--line); border-radius: 12px; background: #fff; box-shadow: 0 14px 34px rgba(15, 23, 42, 0.06); }
.notification-list { display: grid; }
.notification-item { padding: 15px 18px; border-bottom: 1px solid #edf0f3; }
.notification-item.unread { box-shadow: inset 3px 0 var(--brand-blue); background: #fafcff; }
.notification-head { display: flex; align-items: center; gap: 8px; }
.notification-head strong { flex: 1; }
.type-tag { padding: 2px 7px; border-radius: 4px; color: #fff; background: #64748b; font-size: 11px; }
.type-tag.ORDER_STATUS { background: var(--brand-blue); } .type-tag.AUDIT { background: #d48806; } .type-tag.SYSTEM { background: #389e0d; }
.unread-dot { width: 8px; height: 8px; border-radius: 50%; background: var(--brand-blue); }
.notification-item p { margin: 9px 0; color: #52606d; line-height: 1.6; }
.notification-item footer { display: flex; align-items: center; justify-content: space-between; }
.notification-item time { color: #9aa4af; font-size: 12px; }
.notification-item footer button { padding: 3px; border: 0; color: var(--brand-blue); background: transparent; cursor: pointer; }
@media (max-width: 980px) {
  .community-layout { grid-template-columns: 270px minmax(0, 1fr); }
  .community-side { display: none; }
}
@media (max-width: 700px) {
  .message-page { margin: -8px; }
  .page-header { display: grid; padding: 16px; }
  .community-layout { display: block; height: calc(100vh - 180px); min-height: 480px; }
  .conversation-sidebar { height: 100%; border-right: 0; }
  .chat-workspace { display: none; height: 100%; }
  .community-layout.mobile-chat-open .conversation-sidebar { display: none; }
  .community-layout.mobile-chat-open .chat-workspace { display: grid; }
  .back-button { display: block; }
}
</style>
