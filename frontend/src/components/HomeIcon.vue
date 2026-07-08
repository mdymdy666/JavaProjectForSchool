<script setup lang="ts">
defineProps<{
  name: 'device' | 'study' | 'book' | 'home' | 'sport' | 'beauty' | 'bag' | 'grid' | 'medal'
}>()
</script>

<template>
  <svg class="home-icon" :data-icon="name" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
    <template v-if="name === 'device'">
      <rect x="5" y="5" width="14" height="10" rx="2" />
      <path d="M9 19h6M12 15v4" />
    </template>
    <template v-else-if="name === 'study'">
      <path d="M5 18V6.8A2.8 2.8 0 0 1 7.8 4H19v14H7.8A2.8 2.8 0 0 0 5 20" />
      <path d="M9 8h6M9 12h5" />
    </template>
    <template v-else-if="name === 'book'">
      <path d="M4 6.5A2.5 2.5 0 0 1 6.5 4H11v16H6.5A2.5 2.5 0 0 0 4 22Z" />
      <path d="M20 6.5A2.5 2.5 0 0 0 17.5 4H13v16h4.5A2.5 2.5 0 0 1 20 22Z" />
      <path d="M7 8h2M15 8h2" />
    </template>
    <template v-else-if="name === 'home'">
      <path d="m4 11 8-7 8 7" />
      <path d="M6.5 10v9h11v-9" />
      <path d="M10 19v-5h4v5" />
    </template>
    <template v-else-if="name === 'sport'">
      <circle cx="12" cy="12" r="8" />
      <path d="M5.5 9.5c3.9 1.1 7 4.2 8.1 8.9" />
      <path d="M18.6 8.5c-4.6.9-7.9 4.1-8.9 8.8" />
      <path d="M12 4c1.9 2.2 3 5 3 8s-1.1 5.8-3 8" />
    </template>
    <template v-else-if="name === 'beauty'">
      <path d="M12 4c1.9 3.1 4.5 5.4 8 6.8-3.5 1.4-6.1 3.7-8 6.8-1.9-3.1-4.5-5.4-8-6.8 3.5-1.4 6.1-3.7 8-6.8Z" />
      <path d="M18 4v4M16 6h4M6 16v3M4.5 17.5h3" />
    </template>
    <template v-else-if="name === 'bag'">
      <path d="M7 9h10l1 11H6Z" />
      <path d="M9 9V7a3 3 0 0 1 6 0v2" />
      <path d="M9 13h6" />
    </template>
    <template v-else-if="name === 'grid'">
      <rect x="4" y="4" width="6" height="6" rx="1.5" />
      <rect x="14" y="4" width="6" height="6" rx="1.5" />
      <rect x="4" y="14" width="6" height="6" rx="1.5" />
      <rect x="14" y="14" width="6" height="6" rx="1.5" />
    </template>
    <template v-else>
      <circle cx="12" cy="8" r="4" />
      <path d="m9.5 12-2 7 4.5-2 4.5 2-2-7" />
      <path d="M10.5 8.2h3" />
    </template>
  </svg>
</template>

<style scoped>
.home-icon {
  width: 24px;
  height: 24px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.9;
  stroke-linecap: round;
  stroke-linejoin: round;
}
</style>
