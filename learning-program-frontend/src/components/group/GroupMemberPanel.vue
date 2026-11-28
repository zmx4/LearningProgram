<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { User, UserFilled } from '@element-plus/icons-vue'
import type { GroupMember } from '@/types/group'

const props = defineProps<{
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
          class="member-item"
          :class="{ owner: member.role === 'owner', 'is-me': member.accountId === myId }"
      >
        <button class="member-open" type="button" @click="emit('openMember', member)">
          <span class="member-avatar" aria-hidden="true">
            <el-icon><UserFilled /></el-icon>
          </span>
          <span class="member-info">
            <span class="member-name">
              <span>{{ member.username || t('groups.unknownUser') }}</span>
              <span v-if="member.role === 'owner'" class="sender-badge">{{ t('groups.roleOwner') }}</span>
            </span>
            <small>{{ t('groups.joinedAt', { time: formatDate(member.joinedAt) }) }}</small>
          </span>
        </button>
        <button
            v-if="props.isOwner && member.accountId !== props.myId"
            class="remove-button"
            type="button"
            :title="t('groups.removeMember')"
            :aria-label="`${member.username || t('groups.unknownUser')} ${t('groups.removeMember')}`"
            @click="emit('removeMember', member)"
        >
          ×
        </button>
      </li>
    </ul>
  </aside>
</template>

<style scoped>
.member-panel {
  display: flex;
  width: 264px;
  flex: 0 0 264px;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid var(--el-border-color-light);
  border-radius: 18px;
  background: var(--el-bg-color);
  box-shadow: var(--el-box-shadow-lighter);
}

.member-panel h3 {
  display: flex;
  align-items: center;
  gap: 7px;
  margin: 0;
  padding: 16px 17px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  font-size: 13px;
  font-weight: 600;
}

.member-panel h3 .el-icon {
  color: var(--el-color-primary);
}

.member-count {
  display: grid;
  min-width: 24px;
  height: 22px;
  margin-left: auto;
  place-items: center;
  border-radius: 999px;
  color: var(--el-color-primary-dark-2);
  background: var(--el-color-primary-light-9);
  font-size: 11px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.member-list {
  display: flex;
  flex: 1;
  min-height: 0;
  flex-direction: column;
  gap: 4px;
  margin: 0;
  padding: 9px;
  overflow-y: auto;
  list-style: none;
  scrollbar-color: var(--el-border-color-dark) transparent;
  scrollbar-width: thin;
}

.member-item {
  display: flex;
  align-items: center;
  gap: 4px;
  border-radius: 12px;
  transition: background-color .2s, transform .2s;
}

.member-item.owner {
  background: var(--el-color-primary-light-9);
}

.member-item:hover,
.member-item:focus-within {
  background: var(--el-fill-color-light);
  transform: translateX(2px);
}

.member-item.owner:hover,
.member-item.owner:focus-within {
  background: var(--el-color-primary-light-8);
}

.member-open {
  display: flex;
  min-width: 0;
  flex: 1;
  align-items: center;
  gap: 10px;
  padding: 9px 8px;
  border: 0;
  border-radius: inherit;
  color: inherit;
  background: transparent;
  font: inherit;
  text-align: left;
  cursor: pointer;
}

.member-open:focus-visible {
  outline: 2px solid var(--el-color-primary);
  outline-offset: -2px;
}

.member-avatar {
  display: grid;
  width: 32px;
  height: 32px;
  flex: 0 0 32px;
  place-items: center;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 50%;
  color: var(--el-color-white);
  background: var(--el-color-info-dark-2);
  font-size: 13px;
}

.member-item.owner .member-avatar {
  border-color: var(--el-color-primary-light-5);
  background: var(--el-color-primary-dark-2);
}

.member-info {
  display: grid;
  min-width: 0;
  flex: 1;
  gap: 3px;
}

.member-name {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
}

.member-name > span:first-child {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.member-info small {
  color: var(--el-text-color-secondary);
  font-size: 10px;
  font-variant-numeric: tabular-nums;
}

.sender-badge {
  flex: 0 0 auto;
  padding: 1px 6px;
  border-radius: 999px;
  color: var(--el-color-primary-dark-2);
  background: var(--el-color-primary-light-7);
  font-size: 9px;
  font-weight: 700;
}

.remove-button {
  display: grid;
  width: 22px;
  height: 22px;
  flex: 0 0 22px;
  margin-right: 4px;
  place-items: center;
  border: 0;
  border-radius: 50%;
  color: var(--el-text-color-secondary);
  background: transparent;
  font-size: 15px;
  line-height: 1;
  cursor: pointer;
  opacity: 0;
  transition: opacity .15s, color .15s, background-color .15s;
}

.member-item:hover .remove-button,
.member-item:focus-within .remove-button {
  opacity: 1;
}

.remove-button:hover {
  color: var(--el-color-white);
  background: var(--el-color-danger);
}

.remove-button:focus-visible {
  outline: 2px solid var(--el-color-primary);
  outline-offset: 2px;
}

@media (max-width: 900px) {
  .member-panel {
    width: 100%;
    flex-basis: auto;
    max-height: 250px;
    border-radius: 15px;
  }
}

@media (hover: none) {
  .remove-button {
    opacity: 1;
  }
}

@media (prefers-reduced-motion: reduce) {
  .member-item,
  .remove-button {
    transition-duration: .01ms;
  }
}
</style>
