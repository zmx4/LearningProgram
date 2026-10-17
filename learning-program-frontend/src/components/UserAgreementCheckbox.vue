<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'

defineProps<{ modelValue: boolean }>()
const emit = defineEmits<{ (e: 'update:modelValue', value: boolean): void }>()
const { t } = useI18n()
const dialogVisible = ref(false)
</script>

<template>
  <div class="user-agreement">
    <el-checkbox
      :model-value="modelValue"
      @update:model-value="emit('update:modelValue', $event)"
    >
      {{ t('welcome.agreementPrefix') }}
      <el-link
        type="primary"
        class="agreement-link"
        @click.stop.prevent="dialogVisible = true"
      >{{ t('welcome.agreementName') }}</el-link>
    </el-checkbox>
    <el-dialog
      v-model="dialogVisible"
      :title="t('welcome.agreementTitle')"
      width="480px"
    >
      <div class="agreement-body">{{ t('welcome.agreementContent') }}</div>
      <template #footer>
        <el-button type="primary" @click="dialogVisible = false">{{ t('welcome.agreementConfirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.user-agreement {
  text-align: left;
}
.agreement-link {
  vertical-align: baseline;
}
.agreement-body {
  max-height: 50vh;
  overflow-y: auto;
  white-space: pre-line;
  font-size: 13px;
  line-height: 1.8;
  color: var(--el-text-color-regular);
}
</style>
