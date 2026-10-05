<script setup lang="ts">
import type { PasswordRequirement } from '@/utils/passwordPolicy'

defineProps<{ requirements: PasswordRequirement[] }>()
</script>

<template>
  <ul v-if="requirements.length" class="password-policy-hint">
    <li v-for="requirement in requirements" :key="requirement.code">{{ requirement.message }}</li>
  </ul>
</template>

<style scoped>
.password-policy-hint {
  margin: 6px 0 0;
  padding-left: 18px;
  color: var(--el-text-color-placeholder);
  font-size: 12px;
  line-height: 1.75;
  text-align: left;
}
</style>
