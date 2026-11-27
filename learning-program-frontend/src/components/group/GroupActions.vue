<script setup lang="ts">
import { onUnmounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Key, Plus } from '@element-plus/icons-vue'
import { post } from '@/net'
import type { GroupSummary } from '@/types/group'

const emit = defineEmits<{ changed: [group: GroupSummary] }>()
const { t } = useI18n()

const creating = ref(false)
const joining = ref(false)
const createForm = ref({ name: '' })
const joinForm = ref({ inviteCode: '' })
let disposed = false

onUnmounted(() => {
  disposed = true
})

function submitCreate() {
  const name = createForm.value.name.trim()
  if (!name) {
    ElMessage.warning(t('groups.nameRequired'))
    return
  }
  creating.value = true
  post<GroupSummary>('/api/groups', { name }, group => {
    if (disposed) return
    creating.value = false
    createForm.value.name = ''
    emit('changed', group)
    ElMessage.success(t('groups.created'))
  }, () => {
    if (!disposed) creating.value = false
  })
}

function submitJoin() {
  const inviteCode = joinForm.value.inviteCode.trim().toUpperCase()
  if (!inviteCode) {
    ElMessage.warning(t('groups.codeRequired'))
    return
  }
  joining.value = true
  post<GroupSummary>('/api/groups/join', { inviteCode }, group => {
    if (disposed) return
    joining.value = false
    joinForm.value.inviteCode = ''
    emit('changed', group)
    ElMessage.success(t('groups.joined'))
  }, () => {
    if (!disposed) joining.value = false
  })
}
</script>

<template>
  <section class="group-actions">
    <article class="action-card">
      <h3><el-icon><Plus /></el-icon>{{ t('groups.createTitle') }}</h3>
      <p class="action-hint">{{ t('groups.createDescription') }}</p>
      <label class="field">
        <span>{{ t('groups.nameLabel') }}</span>
        <input
            v-model="createForm.name"
            type="text"
            maxlength="50"
            :placeholder="t('groups.namePlaceholder')"
            @keyup.enter="submitCreate"
        >
      </label>
      <div class="action-actions">
        <button class="primary-button" type="button" :disabled="creating" @click="submitCreate">
          {{ creating ? t('groups.creating') : t('groups.create') }}
        </button>
      </div>
    </article>

    <article class="action-card">
      <h3><el-icon><Key /></el-icon>{{ t('groups.joinTitle') }}</h3>
      <p class="action-hint">{{ t('groups.joinDescription') }}</p>
      <label class="field">
        <span>{{ t('groups.codeLabel') }}</span>
        <input
            v-model="joinForm.inviteCode"
            type="text"
            maxlength="8"
            :placeholder="t('groups.codePlaceholder')"
            @keyup.enter="submitJoin"
        >
      </label>
      <div class="action-actions">
        <button class="primary-button" type="button" :disabled="joining" @click="submitJoin">
          {{ joining ? t('groups.joining') : t('groups.join') }}
        </button>
      </div>
    </article>
  </section>
</template>

<style scoped>
.group-actions {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 14px;
}

.action-card {
  display: grid;
  gap: 12px;
  align-content: start;
  padding: 20px 22px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 14px;
  background: var(--el-bg-color);
  box-shadow: var(--el-box-shadow-lighter);
}

.action-card h3 {
  display: flex;
  align-items: center;
  gap: 7px;
  margin: 0;
  font-size: 16px;
}

.action-hint {
  margin: -6px 0 0;
  color: var(--el-text-color-placeholder);
  font-size: 12px;
}

.field {
  display: grid;
  gap: 6px;
}

.field > span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  font-weight: 600;
}

.field input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  color: var(--el-text-color-primary);
  background: var(--el-bg-color);
  font: inherit;
  font-size: 14px;
}

.field input:focus {
  border-color: var(--el-color-primary);
  outline: none;
}

.action-actions {
  display: flex;
  justify-content: flex-end;
}

.primary-button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 18px;
  border: 0;
  border-radius: 8px;
  color: var(--el-color-white);
  background: var(--el-color-primary);
  font: inherit;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.primary-button:hover {
  background: var(--el-color-primary-light-3);
}

.primary-button:disabled {
  opacity: .6;
  cursor: not-allowed;
}
</style>
