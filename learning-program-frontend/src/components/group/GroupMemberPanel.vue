<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { User, UserFilled } from '@element-plus/icons-vue'
import type { GroupMember } from '@/types/group'

defineProps<{
  members: GroupMember[]
  isOwner: boolean
  myId: number | null
}>()

const emit = defineEmits<{
  openMember: [member: GroupMember]
  removeMember: [member: GroupMember]
}>()

const { t } = useI18n()

function formatDate(value: string) {
  if (!value) return ''
  return value.slice(0, 10)
}
</script>

<template>
  <aside class="member-panel">
    <h3>
      <el-icon><User /></el-icon>
      {{ t('groups.members') }}
      <span class="member-count">{{ members.length }}</span>
    </h3>
    <ul class="member-list">
      <li
          v-for="member in members"
          :key="member.accountId"
          class="member-item clickable"
          @click="emit('openMember', member)"
      >
        <div class="member-avatar">
          <el-icon><UserFilled /></el-icon>
        </div>
        <div class="member-info">
          <div class="member-name">
            <span>{{ member.username || t('groups.unknownUser') }}</span>
            <span v-if="member.role === 'owner'" class="sender-badge">{{ t('groups.roleOwner') }}</span>
          </div>
          <small>{{ t('groups.joinedAt', { time: formatDate(member.joinedAt) }) }}</small>
        </div>
        <button
            v-if="isOwner && member.accountId !== myId"
            class="remove-button"
            type="button"
            :title="t('groups.removeMember')"
            @click.stop="emit('removeMember', member)"
        >
          ×
        </button>
      </li>
    </ul>
  </aside>
</template>

<style scoped>
.member-panel {
  width: 240px;
  flex: 0 0 240px;
  min-height: 0;
  display: flex;
  flex-direction: column;
  border: 1px solid var(--el-border-color-light);
  border-radius: 14px;
  background: var(--el-bg-color);
  box-shadow: var(--el-box-shadow-lighter);
  overflow: hidden;
}

.member-panel h3 {
  display: flex;
  align-items: center;
  gap: 7px;
  margin: 0;
  padding: 14px 16px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  font-size: 14px;
}

.member-count {
  margin-left: auto;
  color: var(--el-text-color-placeholder);
  font-size: 12px;
  font-weight: 400;
}

.member-list {
  flex: 1;
  min-height: 0;
  margin: 0;
  padding: 8px;
  overflow-y: auto;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.member-item {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 8px;
  border-radius: 9px;
}

.member-item.clickable {
  cursor: pointer;
}

.member-item:hover {
  background: var(--el-fill-color-light);
}

.member-avatar {
  display: grid;
  width: 30px;
  height: 30px;
  flex: 0 0 30px;
  place-items: center;
  border-radius: 50%;
  color: var(--el-color-white);
  background: var(--el-color-info);
  font-size: 14px;
}

.member-info {
  min-width: 0;
  flex: 1;
}

.member-name {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}

.member-name > span:first-child {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.member-info small {
  color: var(--el-text-color-placeholder);
  font-size: 11px;
}

.sender-badge {
  padding: 1px 7px;
  border-radius: 999px;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  font-size: 10px;
  font-weight: 600;
}

.remove-button {
  display: grid;
  width: 20px;
  height: 20px;
  place-items: center;
  border: 0;
  border-radius: 50%;
  color: var(--el-text-color-placeholder);
  background: transparent;
  font-size: 14px;
  cursor: pointer;
  opacity: 0;
  transition: opacity .15s;
}

.member-item:hover .remove-button {
  opacity: 1;
}

.remove-button:hover {
  color: var(--el-color-white);
  background: var(--el-color-danger);
}

@media (max-width: 900px) {
  .member-panel {
    width: 100%;
    flex-basis: auto;
    max-height: 260px;
  }
}
</style>