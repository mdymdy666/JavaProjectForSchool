<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from '../components/AppHeader.vue'

const route = useRoute()
const isCover = computed(() => route.meta.cover === true)
</script>

<template>
  <div class="app-layout" :class="{ 'cover-layout': isCover }">
    <AppHeader v-if="!isCover" />
    <main class="app-main" :class="{ 'cover-main': isCover }">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.app-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}
.app-main {
  flex: 1;
  padding: 24px;
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
}

.cover-main {
  max-width: none;
  padding: 0;
}
</style>
