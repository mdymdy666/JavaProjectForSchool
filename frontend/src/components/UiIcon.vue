<script setup lang="ts">
withDefaults(defineProps<{
  name: 'cart' | 'heart'
  filled?: boolean
}>(), { filled: false })
</script>

<template>
  <svg class="ui-icon" :data-icon="name" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
    <template v-if="name === 'cart'">
      <circle cx="9" cy="20" r="1.5" />
      <circle cx="18" cy="20" r="1.5" />
      <path d="M3 4h2l2.1 10.1a2 2 0 0 0 2 1.6h8.7a2 2 0 0 0 1.9-1.4L21 8H6" />
    </template>
    <path
      v-else
      d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1.1-1.1a5.5 5.5 0 0 0-7.8 7.8l1.1 1.1L12 21l7.8-7.5 1.1-1.1a5.5 5.5 0 0 0-.1-7.8Z"
      :class="{ filled }"
    />
  </svg>
</template>

<style scoped>
.ui-icon {
  width: 16px;
  height: 16px;
  flex: 0 0 16px;
  fill: none;
  stroke: currentColor;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
}
.ui-icon .filled { fill: currentColor; }
</style>
