<script setup lang="ts">
import { useRoute } from 'vue-router'

const route = useRoute()
</script>

<template>
  <main class="feature-page">
    <p class="card-label">LEARNING SPACE</p>
    <h2>{{ route.meta.title }}</h2>
    <p>该功能正在建设中，敬请期待。</p>
  </main>
</template>

<style scoped>
.feature-page {
  max-width: 1180px;
  margin: 0 auto;
  padding: 42px 5% 60px;
}
.card-label {
  margin: 0 0 7px;
  color: var(--el-text-color-placeholder);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: .13em;
}
h2 { margin: 0 0 12px; font-size: 24px; }
p:last-child { color: var(--el-text-color-secondary); font-size: 14px; }
</style>
