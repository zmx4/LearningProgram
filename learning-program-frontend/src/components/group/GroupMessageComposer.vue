<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { Flag, Promotion } from '@element-plus/icons-vue'

defineProps<{
  sending: boolean
  isOwner: boolean
}>()

const emit = defineEmits<{
  send: []
  toggleTaskForm: []
}>()

const draft = defineModel<string>('draft', { required: true })
const { t } = useI18n()

function onKeydown(event: KeyboardEvent) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    emit('send')
  }
}
</script>

<template>
  <div class="composer">
    <textarea
        v-model="draft"
        rows="2"
        maxlength="2000"
        :placeholder="t('groups.inputPlaceholder')"
        @keydown="onKeydown"
    ></textarea>
    <div class="composer-actions">
      <button
          v-if="isOwner"
          class="ghost-button"
          type="button"
          @click="emit('toggleTaskForm')"
      >
        <el-icon><Flag /></el-icon>
        {{ t('groups.publishTask') }}
      </button>
      <button class="primary-button" type="button" :disabled="sending" @click="emit('send')">
        <el-icon><Promotion /></el-icon>
        {{ sending ? t('groups.sending') : t('groups.send') }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.composer {
  display: grid;
  gap: 8px;
}

.composer textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--el-border-color);
  border-radius: 9px;
  color: var(--el-text-color-primary);
  background: var(--el-bg-color);
  font: inherit;
  font-size: 14px;
  resize: none;
}

.composer textarea:focus {
  border-color: var(--el-color-primary);
  outline: none;
}

.composer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.ghost-button,
.primary-button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border-radius: 8px;
  font: inherit;
  font-size: 13px;
  cursor: pointer;
}

.ghost-button {
  border: 1px solid var(--el-border-color);
  color: var(--el-text-color-regular);
  background: var(--el-bg-color);
}

.ghost-button:hover:not(:disabled) {
  color: var(--el-color-primary);
  border-color: var(--el-color-primary-light-5);
}

.primary-button {
  border: 0;
  color: var(--el-color-white);
  background: var(--el-color-primary);
  font-weight: 600;
}

.primary-button:hover {
  background: var(--el-color-primary-light-3);
}

.primary-button:disabled {
  opacity: .6;
  cursor: not-allowed;
}
</style>