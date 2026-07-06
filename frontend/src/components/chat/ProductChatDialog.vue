<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { getMessages, markMessageRead, sendMessage } from '../../api/message'
import { useNotificationStore } from '../../stores/notification'
import type { SiteMessage } from '../../types/domain'
import ChatPanel from './ChatPanel.vue'

const props = defineProps<{
  open: boolean
  productId: number
  productTitle: string
  sellerId: number
  sellerNickname: string
  currentUserId: number
}>()

const emit = defineEmits<{ close: []; openCenter: [] }>()
const notificationStore = useNotificationStore()
const allMessages = ref<SiteMessage[]>([])
const loading = ref(false)
const sending = ref(false)
const error = ref('')
const chatPanel = ref<{ clearDraft: () => void } | null>(null)
let pollTimer: ReturnType<typeof setInterval> | null = null

const conversationMessages = computed(() => allMessages.value
  .filter(message => message.productId === props.productId
    && ((message.senderId === props.currentUserId && message.receiverId === props.sellerId)
      || (message.senderId === props.sellerId && message.receiverId === props.currentUserId)))
  .sort((left, right) => left.createdAt.localeCompare(right.createdAt) || left.id - right.id))

function errorMessage(cause: unknown, fallback: string) {
  return (cause as { response?: { data?: { message?: string } } })?.response?.data?.message || fallback
}

async function markIncomingRead() {
  const unread = conversationMessages.value.filter(message =>
    message.receiverId === props.currentUserId && message.readStatus === 'UNREAD')
  if (!unread.length) return
  await Promise.all(unread.map(message => markMessageRead(message.id)))
  const unreadIds = new Set(unread.map(message => message.id))
  allMessages.value = allMessages.value.map(message =>
    unreadIds.has(message.id) ? { ...message, readStatus: 'READ' } : message)
  await notificationStore.refresh()
}

async function loadMessages(silent = false) {
  if (!props.open) return
  if (!silent) loading.value = true
  try {
    const response = await getMessages()
    allMessages.value = response.data || []
    error.value = ''
    await markIncomingRead()
  } catch (cause) {
    if (!silent) error.value = errorMessage(cause, '消息加载失败，请重试')
  } finally {
    if (!silent) loading.value = false
  }
}

async function handleSend(content: string) {
  sending.value = true
  error.value = ''
  try {
    await sendMessage({ receiverId: props.sellerId, productId: props.productId, content })
    chatPanel.value?.clearDraft()
    await loadMessages(true)
    await notificationStore.refresh()
  } catch (cause) {
    error.value = errorMessage(cause, '发送失败，请稍后重试')
  } finally {
    sending.value = false
  }
}

function startPolling() {
  stopPolling()
  pollTimer = setInterval(() => loadMessages(true), 5000)
}

function stopPolling() {
  if (pollTimer) clearInterval(pollTimer)
  pollTimer = null
}

function handleEscape(event: KeyboardEvent) {
  if (event.key === 'Escape' && props.open) emit('close')
}

watch(() => props.open, open => {
  if (open) {
    loadMessages()
    startPolling()
    window.addEventListener('keydown', handleEscape)
  } else {
    stopPolling()
    window.removeEventListener('keydown', handleEscape)
  }
}, { immediate: true })

onBeforeUnmount(() => {
  stopPolling()
  window.removeEventListener('keydown', handleEscape)
})
</script>

<template>
  <div v-if="open" class="chat-dialog" role="dialog" aria-modal="true" aria-labelledby="chat-dialog-title" @click.self="$emit('close')">
    <section class="dialog-surface">
      <header class="dialog-header">
        <div class="seller-avatar">{{ sellerNickname.slice(0, 1) }}</div>
        <div class="dialog-title">
          <h2 id="chat-dialog-title">{{ sellerNickname }}</h2>
          <p>{{ productTitle }}</p>
        </div>
        <button class="center-link" type="button" @click="$emit('openCenter')">消息中心</button>
        <button class="close-button" type="button" aria-label="关闭聊天" @click="$emit('close')">×</button>
      </header>

      <div v-if="loading" class="dialog-state">正在加载消息...</div>
      <ChatPanel
        v-else
        ref="chatPanel"
        :messages="conversationMessages"
        :current-user-id="currentUserId"
        :sending="sending"
        :error="error"
        empty-hint="还没有消息，向卖家问问商品细节吧"
        @send="handleSend"
      />
    </section>
  </div>
</template>

<style scoped>
.chat-dialog { position: fixed; inset: 0; z-index: 300; display: grid; place-items: center; padding: 20px; background: rgba(15, 23, 42, 0.48); }
.dialog-surface { display: grid; grid-template-rows: auto minmax(0, 1fr); width: min(640px, 100%); height: min(680px, calc(100vh - 40px)); overflow: hidden; border: 1px solid #dbe2ea; border-radius: 8px; background: #fff; box-shadow: 0 18px 50px rgba(15, 23, 42, 0.24); }
.dialog-header { display: grid; grid-template-columns: 42px minmax(0, 1fr) auto auto; gap: 10px; align-items: center; padding: 13px 16px; border-bottom: 1px solid #e6ebf0; }
.seller-avatar { display: grid; place-items: center; width: 42px; height: 42px; border-radius: 50%; color: #fff; background: #1677ff; font-weight: 700; }
.dialog-title { min-width: 0; }
.dialog-title h2 { margin: 0; overflow: hidden; color: #17212b; font-size: 16px; line-height: 1.4; text-overflow: ellipsis; white-space: nowrap; }
.dialog-title p { margin: 2px 0 0; overflow: hidden; color: #71808f; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.center-link, .close-button { border: 0; color: #1677ff; background: transparent; cursor: pointer; }
.center-link { padding: 7px 8px; font-size: 13px; }
.close-button { width: 34px; height: 34px; padding: 0; color: #52606d; font-size: 24px; line-height: 1; }
.dialog-state { display: grid; place-items: center; color: #71808f; background: #f5f7fa; }
@media (max-width: 700px) {
  .chat-dialog { padding: 0; }
  .dialog-surface { width: 100%; height: 100vh; border: 0; border-radius: 0; }
  .center-link { display: none; }
  .dialog-header { grid-template-columns: 40px minmax(0, 1fr) auto; }
}
</style>
