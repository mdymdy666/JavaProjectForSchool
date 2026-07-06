# Cart And Favorite Icon Fix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace corrupted cart and favorite marker characters with stable SVG icons and readable labels.

**Architecture:** Add one focused `UiIcon` Vue component that owns the two inline SVG paths. Consume it in the existing header and product detail buttons without changing routing, cart, or favorite behavior.

**Tech Stack:** Vue 3, TypeScript, Vue Test Utils, Vitest, Vite

---

### Task 1: Lock The Button Markers With Tests

**Files:**
- Create: `frontend/src/components/AppHeader.spec.ts`
- Create: `frontend/src/components/UiIcon.spec.ts`
- Modify: `frontend/src/views/ProductDetailView.spec.ts`

- [ ] **Step 1: Write failing icon tests**

Test that `UiIcon` renders a named SVG, the header cart button has `aria-label="购物车"` and a `cart` icon, and the product favorite button has `aria-label="收藏商品"` with a `heart` icon.

- [ ] **Step 2: Run tests to verify they fail**

Run: `pnpm test -- src/components/UiIcon.spec.ts src/components/AppHeader.spec.ts src/views/ProductDetailView.spec.ts`

Expected: FAIL because `UiIcon` and the required accessible markers do not exist.

### Task 2: Implement Stable SVG Markers

**Files:**
- Create: `frontend/src/components/UiIcon.vue`
- Modify: `frontend/src/components/AppHeader.vue`
- Modify: `frontend/src/views/ProductDetailView.vue`

- [ ] **Step 1: Implement `UiIcon`**

Create a 16px SVG component accepting `name: 'cart' | 'heart'` and optional `filled`, using `currentColor`, `aria-hidden="true"`, and `data-icon` for deterministic tests.

- [ ] **Step 2: Replace corrupted button text**

Render `UiIcon name="cart"` beside `购物车` in the header. Render `UiIcon name="heart"` beside `收藏`, `已收藏`, or `自己的商品` in the detail button; bind `filled` to `product.favorite`.

- [ ] **Step 3: Stabilize button layout**

Use `inline-flex`, center alignment, and a 6px gap for both controls while preserving the existing badge and favorite-state colors.

- [ ] **Step 4: Run targeted tests**

Run: `pnpm test -- src/components/UiIcon.spec.ts src/components/AppHeader.spec.ts src/views/ProductDetailView.spec.ts`

Expected: all targeted tests PASS.

### Task 3: Verify The Frontend

**Files:**
- No production file changes expected.

- [ ] **Step 1: Run all frontend tests**

Run: `pnpm test`

Expected: all test files PASS.

- [ ] **Step 2: Build production assets**

Run: `pnpm build`

Expected: TypeScript and Vite build complete successfully.

- [ ] **Step 3: Review the diff**

Run: `git diff --check` and `git status --short`.

Expected: no whitespace errors; the unrelated modified `backend/src/main/resources/schema-h2.sql` remains untouched.
