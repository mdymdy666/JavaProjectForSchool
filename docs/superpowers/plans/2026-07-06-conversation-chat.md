# Conversation Chat Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn flat transaction messages into reusable product conversations with a product-detail chat dialog and a two-pane message center.

**Architecture:** Keep the existing REST API and message table. Add a pure conversation grouping module, a reusable `ChatPanel`, a self-contained `ProductChatDialog`, and make the message center compose those units. Product and counterpart IDs remain explicit in every send request.

**Tech Stack:** Vue 3, TypeScript, Pinia, Vue Router, Vitest, Vue Test Utils

---

### Task 1: Conversation Grouping Model

**Files:**
- Create: `frontend/src/features/chat/conversations.ts`
- Create: `frontend/src/features/chat/conversations.spec.ts`
- Modify: `frontend/src/api/message.ts`

- [ ] **Step 1: Write the failing grouping test**

Create messages where user `1` is alternately sender and receiver. Assert that `buildConversations(messages, 1)` groups by `counterpartId:productId`, sorts each message list oldest-first, sorts conversations newest-first, and counts only incoming unread messages.

```ts
const conversations = buildConversations(messages, 1)
expect(conversations[0].key).toBe('2:7')
expect(conversations[0].counterpartNickname).toBe('卖家')
expect(conversations[0].unreadCount).toBe(1)
expect(conversations[0].messages.map(message => message.id)).toEqual([1, 2])
```

- [ ] **Step 2: Run the test and verify RED**

Run: `pnpm test -- src/features/chat/conversations.spec.ts`

Expected: FAIL because the module does not exist.

- [ ] **Step 3: Implement the model**

Export `Conversation` and `buildConversations(messages, currentUserId)`. Ignore messages with a null product ID, derive the counterpart from sender/receiver, and use the last chronological message for list summaries.

- [ ] **Step 4: Tighten the send contract**

Change `SendMessageRequest.productId` from optional to required because the backend `SendRequest` requires a product.

- [ ] **Step 5: Run the grouping test and verify GREEN**

Run: `pnpm test -- src/features/chat/conversations.spec.ts`

Expected: PASS.

### Task 2: Reusable Chat Panel

**Files:**
- Create: `frontend/src/components/chat/ChatPanel.vue`
- Create: `frontend/src/components/chat/ChatPanel.spec.ts`

- [ ] **Step 1: Write failing component tests**

Mount with one incoming and one outgoing message. Assert `data-side="incoming"` and `data-side="outgoing"`, then enter text and verify `send` emits trimmed content. Trigger plain Enter to send and Shift+Enter to keep a newline.

```ts
expect(wrapper.findAll('[data-test="chat-bubble"]')[0].attributes('data-side')).toBe('incoming')
await wrapper.get('textarea').setValue('  还在吗？  ')
await wrapper.get('form').trigger('submit')
expect(wrapper.emitted('send')?.[0]).toEqual(['还在吗？'])
```

- [ ] **Step 2: Run the test and verify RED**

Run: `pnpm test -- src/components/chat/ChatPanel.spec.ts`

Expected: FAIL because `ChatPanel.vue` does not exist.

- [ ] **Step 3: Implement `ChatPanel`**

Props: `messages`, `currentUserId`, `sending`, `error`, and `emptyHint`. Emit `send(content: string)`. Render message bubbles, empty state, 500-character composer, explicit foreground colors, and scroll the message viewport to the bottom after updates.

- [ ] **Step 4: Run the test and verify GREEN**

Run: `pnpm test -- src/components/chat/ChatPanel.spec.ts`

Expected: PASS.

### Task 3: Product Detail Chat Dialog

**Files:**
- Create: `frontend/src/components/chat/ProductChatDialog.vue`
- Create: `frontend/src/components/chat/ProductChatDialog.spec.ts`
- Modify: `frontend/src/views/ProductDetailView.vue`
- Modify: `frontend/src/views/ProductDetailView.spec.ts`

- [ ] **Step 1: Write failing dialog tests**

Mock message APIs. Mount the dialog open for seller `2`, product `7`, current user `1`; assert seller/product headings, filtered history, and that sending emits the exact REST payload.

```ts
expect(sendMessage).toHaveBeenCalledWith({ receiverId: 2, productId: 7, content: '今晚能自提吗？' })
```

- [ ] **Step 2: Run the dialog test and verify RED**

Run: `pnpm test -- src/components/chat/ProductChatDialog.spec.ts`

Expected: FAIL because the dialog does not exist.

- [ ] **Step 3: Implement `ProductChatDialog`**

Load/filter messages when opened, mark incoming unread messages, poll every five seconds while open, clean up timers on close/unmount, preserve the draft on failure, refresh the notification Store, and emit `close` plus `open-center`.

- [ ] **Step 4: Integrate product detail**

Replace `contactSeller()` navigation with local dialog state. Pass `product.id`, `product.title`, `product.sellerId`, `product.sellerNickname`, and `auth.userId`. Keep unauthenticated redirect behavior.

- [ ] **Step 5: Run detail/dialog tests and verify GREEN**

Run: `pnpm test -- src/components/chat/ProductChatDialog.spec.ts src/views/ProductDetailView.spec.ts`

Expected: PASS.

### Task 4: Conversation-Style Message Center

**Files:**
- Replace: `frontend/src/views/MessageCenterView.vue`
- Create: `frontend/src/views/MessageCenterView.spec.ts`

- [ ] **Step 1: Write the failing message-center test**

Mock APIs with two product conversations. Mount with an authenticated Pinia session and assert two conversation rows, selection displays `ChatPanel`, sending uses the selected counterpart/product, and the old receiver/product ID inputs do not exist.

```ts
expect(wrapper.findAll('[data-test="conversation-item"]')).toHaveLength(2)
expect(wrapper.find('input[placeholder="输入用户ID"]').exists()).toBe(false)
```

- [ ] **Step 2: Run the test and verify RED**

Run: `pnpm test -- src/views/MessageCenterView.spec.ts`

Expected: FAIL because the current page is a flat card list.

- [ ] **Step 3: Implement the two-pane page**

Use `buildConversations`, retain the messages/notifications tabs, select the newest conversation by default, support `counterpartId` and `productId` query parameters, mark selected incoming messages read, poll every five seconds, and reuse `ChatPanel` for sending.

- [ ] **Step 4: Add responsive behavior**

At widths below 700px show either the conversation list or chat panel. Add a back button in the chat header. Desktop remains a stable `280px + minmax(0, 1fr)` work layout.

- [ ] **Step 5: Run the page test and verify GREEN**

Run: `pnpm test -- src/views/MessageCenterView.spec.ts`

Expected: PASS.

### Task 5: Full Verification

**Files:**
- No additional production files expected.

- [ ] **Step 1: Run all frontend tests**

Run: `pnpm test`

Expected: all tests PASS with no Vue warnings.

- [ ] **Step 2: Build production assets**

Run: `pnpm build`

Expected: TypeScript and Vite build successfully; the existing ECharts chunk-size warning is acceptable.

- [ ] **Step 3: Verify live Vite output**

Request `http://127.0.0.1:5173/src/views/MessageCenterView.vue` and `ProductDetailView.vue`; confirm both return HTTP 200 and reference the new chat components.

- [ ] **Step 4: Review repository state**

Run: `git diff --check` and `git status --short`.

Expected: no whitespace errors; the unrelated `backend/src/main/resources/schema-h2.sql` change remains untouched.
