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
  <div class="composer" :class="{ 'is-composing': draft.length > 0 }">
    <div class="composer-main">
      <textarea
          v-model="draft"
          rows="2"
          maxlength="2000"
          :aria-label="t('groups.inputPlaceholder')"
          :placeholder="t('groups.inputPlaceholder')"
          @keydown="onKeydown"
      ></textarea>
      <span class="composer-count" aria-hidden="true">{{ draft.length }}<small>/2000</small></span>
    </div>
    <div class="composer-actions">
      <button
          v-if="isOwner"
          class="task-button"
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
  gap: 9px;
  padding: 8px;
  border: 1px solid var(--el-border-color);
  border-radius: 14px;
  background: var(--el-fill-color-lighter);
  transition: border-color .2s, box-shadow .25s, background-color .2s;
}

.composer:focus-within {
  border-color: var(--el-color-primary);
  background: var(--el-bg-color);
  box-shadow: 0 12px 30px -24px var(--el-color-primary-dark-2);
}

.composer-main {
  position: relative;
}

.composer textarea {
  display: block;
  width: 100%;
  min-height: 50px;
  padding: 5px 58px 5px 6px;
  border: 0;
  outline: 0;
  color: var(--el-text-color-primary);
  background: transparent;
  font: inherit;
  font-size: 14px;
  line-height: 1.55;
  resize: none;
}

.composer textarea::placeholder {
  color: var(--el-text-color-secondary);
}

.composer-count {
  position: absolute;
  right: 7px;
  bottom: 8px;
  color: var(--el-text-color-secondary);
  font-size: 10px;
  font-variant-numeric: tabular-nums;
  opacity: 0;
  transition: opacity .2s;
}

.composer-count small {
  font-size: inherit;
}

.composer:focus-within .composer-count,
.composer.is-composing .composer-count {
  opacity: 1;
}

.composer-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.task-button,
.primary-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-height: 34px;
  padding: 7px 13px;
  border-radius: 9px;
  font: inherit;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: color .2s, border-color .2s, background-color .2s, transform .2s;
}

.task-button {
  border: 1px solid var(--el-color-warning-light-5);
  color: var(--el-color-warning-dark-2);
  background: var(--el-color-warning-light-9);
}

.task-button:hover {
  border-color: var(--el-color-warning);
  transform: translateY(-1px);
}

.primary-button {
  margin-left: auto;
  border: 1px solid transparent;
  color: var(--el-color-white);
  background: color-mix(in srgb, var(--el-color-primary-dark-2) 78%, #071624);
}

.primary-button .el-icon {
  transition: transform .2s;
}

.primary-button:hover:not(:disabled) {
  background: color-mix(in srgb, var(--el-color-primary-dark-2) 88%, #071624);
  transform: translateY(-1px);
}

.primary-button:hover:not(:disabled) .el-icon {
  transform: translate(2px, -2px);
}

.primary-button:disabled {
  opacity: .58;
  cursor: not-allowed;
}

.task-button:focus-visible,
.primary-button:focus-visible {
  outline: 2px solid var(--el-color-primary);
  outline-offset: 2px;
}

@media (max-width: 560px) {
  .composer textarea {
    font-size: 13px;
  }

  .task-button,
  .primary-button {
    padding-inline: 11px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .composer,
  .composer-count,
  .task-button,
  .primary-button,
  .primary-button .el-icon {
    transition-duration: .01ms;
  }
}
</style>
