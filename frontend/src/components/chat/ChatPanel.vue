<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import type { SiteMessage } from '../../types/domain'

const props = withDefaults(defineProps<{
  messages: SiteMessage[]
  currentUserId: number
  sending?: boolean
  error?: string
  emptyHint?: string
}>(), {
  sending: false,
  error: '',
  emptyHint: '还没有消息，先打个招呼吧'
})

const emit = defineEmits<{ send: [content: string] }>()
const draft = ref('')
const messageViewport = ref<HTMLElement | null>(null)

function submit() {
  const content = draft.value.trim()
  if (!content || props.sending) return
  emit('send', content)
}

function handleKeydown(event: KeyboardEvent) {
  if (event.key !== 'Enter' || event.shiftKey) return
  event.preventDefault()
  submit()
}

function clearDraft() {
  draft.value = ''
}

function formatTime(value: string) {
  if (!value) return ''
  return new Date(value).toLocaleString('zh-CN', {
    month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hour12: false
  })
}

async function scrollToBottom() {
  await nextTick()
  if (messageViewport.value) {
    messageViewport.value.scrollTop = messageViewport.value.scrollHeight
  }
}

watch(() => props.messages.length, scrollToBottom, { immediate: true })
defineExpose({ clearDraft })
</script>

<template>
  <section class="chat-panel">
    <div ref="messageViewport" class="message-viewport" aria-live="polite">
      <p v-if="!messages.length" class="empty-chat">{{ emptyHint }}</p>
      <div v-else class="bubble-list">
        <article
          v-for="message in messages"
          :key="message.id"
          data-test="chat-bubble"
          :data-side="message.senderId === currentUserId ? 'outgoing' : 'incoming'"
          :class="['bubble-row', message.senderId === currentUserId ? 'outgoing' : 'incoming']"
        >
          <div class="bubble">
            <p>{{ message.content }}</p>
            <time :datetime="message.createdAt">{{ formatTime(message.createdAt) }}</time>
          </div>
        </article>
      </div>
    </div>

    <form class="composer" @submit.prevent="submit">
      <textarea
        v-model="draft"
        rows="3"
        maxlength="500"
        placeholder="输入消息，Enter 发送，Shift+Enter 换行"
        aria-label="消息内容"
        @keydown="handleKeydown"
      />
      <div class="composer-footer">
        <div>
          <span class="counter">{{ draft.length }}/500</span>
          <span v-if="error" class="send-error" role="alert">{{ error }}</span>
        </div>
        <button type="submit" :disabled="sending || !draft.trim()">
          {{ sending ? '发送中...' : '发送' }}
        </button>
      </div>
    </form>
  </section>
</template>

<style scoped>
.chat-panel { display: grid; grid-template-rows: minmax(0, 1fr) auto; min-height: 0; background: #fff; }
.message-viewport { min-height: 260px; overflow-y: auto; padding: 18px; background: #f5f7fa; }
.bubble-list { display: grid; gap: 12px; }
.bubble-row { display: flex; }
.bubble-row.incoming { justify-content: flex-start; }
.bubble-row.outgoing { justify-content: flex-end; }
.bubble { max-width: min(76%, 480px); padding: 9px 12px 6px; border: 1px solid #e3e8ee; border-radius: 8px; color: #27313b; background: #fff; box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04); }
.outgoing .bubble { border-color: #b9d7ff; background: #e8f3ff; }
.bubble p { margin: 0; line-height: 1.55; white-space: pre-wrap; overflow-wrap: anywhere; }
.bubble time { display: block; margin-top: 4px; color: #8792a0; font-size: 11px; text-align: right; }
.empty-chat { margin: 0; padding: 72px 16px; color: #8792a0; text-align: center; }
.composer { padding: 12px 14px; border-top: 1px solid #e6ebf0; background: #fff; }
.composer textarea { display: block; width: 100%; resize: none; padding: 10px 12px; border: 1px solid #cfd8e3; border-radius: 6px; color: #17212b; background: #fff; font: inherit; line-height: 1.5; }
.composer textarea:focus { border-color: #1677ff; outline: 2px solid rgba(22, 119, 255, 0.12); }
.composer-footer { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-top: 8px; }
.composer-footer > div { min-width: 0; }
.counter { color: #8792a0; font-size: 12px; }
.send-error { margin-left: 10px; color: #d4380d; font-size: 12px; }
.composer button { min-width: 84px; padding: 8px 18px; border: 0; border-radius: 6px; color: #fff; background: #1677ff; cursor: pointer; }
.composer button:disabled { color: #8792a0; background: #e5e9ee; cursor: not-allowed; }
@media (max-width: 700px) {
  .message-viewport { min-height: 360px; padding: 14px 10px; }
  .bubble { max-width: 86%; }
}
</style>
