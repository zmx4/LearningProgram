<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Plus, Key, User, UserFilled } from '@element-plus/icons-vue'
import { get, post } from '@/net'

interface GroupSummary {
  id: number
  name: string
  ownerId: number
  ownerName: string
  inviteCode: string
  memberCount: number
  role: string
  createdAt: string
}

const router = useRouter()
const { t } = useI18n()

const groups = ref<GroupSummary[]>([])
const loading = ref(true)
const loadFailed = ref(false)
const creating = ref(false)
const joining = ref(false)
const createForm = ref({ name: '' })
const joinForm = ref({ inviteCode: '' })

function loadGroups() {
  loading.value = true
  loadFailed.value = false
  get<GroupSummary[]>('/api/groups', data => {
    groups.value = data
    loading.value = false
  }, () => {
    loading.value = false
    loadFailed.value = true
  })
}

function submitCreate() {
  const name = createForm.value.name.trim()
  if (!name) {
    ElMessage.warning(t('groups.nameRequired'))
    return
  }
  creating.value = true
  post<GroupSummary>('/api/groups', { name }, group => {
    creating.value = false
    createForm.value.name = ''
    groups.value = [group, ...groups.value]
    ElMessage.success(t('groups.created'))
  }, () => {
    creating.value = false
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
    joining.value = false
    joinForm.value.inviteCode = ''
    groups.value = [group, ...groups.value]
    ElMessage.success(t('groups.joined'))
  }, () => {
    joining.value = false
  })
}

function openGroup(item: GroupSummary) {
  void router.push({ name: 'group-detail', params: { id: item.id } })
}

function formatDate(value: string) {
  if (!value) return ''
  return value.replace('T', ' ').slice(0, 16)
}

onMounted(loadGroups)
</script>

<template>
  <main class="groups-page">
    <section class="groups-heading">
      <p class="eyebrow">{{ t('groups.eyebrow') }}</p>
      <h2>{{ t('groups.title') }}</h2>
      <p>{{ t('groups.description') }}</p>
    </section>

    <section class="action-grid">
      <div class="action-card">
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
      </div>

      <div class="action-card">
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
      </div>
    </section>

    <div class="list-heading">
      <h3>{{ t('groups.myGroups') }}</h3>
      <span>{{ t('groups.total', { count: groups.length }) }}</span>
    </div>

    <section v-if="loading" class="group-list" aria-busy="true">
      <div v-for="index in 2" :key="index" class="group-card skeleton-card">
        <div class="skeleton line short"></div>
        <div class="skeleton line long"></div>
      </div>
    </section>

    <section v-else-if="loadFailed" class="empty-panel">
      <h3>{{ t('groups.loadFailed') }}</h3>
      <button class="ghost-button" type="button" @click="loadGroups">{{ t('groups.retry') }}</button>
    </section>

    <section v-else-if="groups.length" class="group-list">
      <article v-for="item in groups" :key="item.id" class="group-card" @click="openGroup(item)">
        <div class="group-card-title">
          <h4>{{ item.name }}</h4>
          <span class="role-badge" :class="{ owner: item.role === 'owner' }">
            {{ item.role === 'owner' ? t('groups.roleOwner') : t('groups.roleMember') }}
          </span>
        </div>
        <div class="group-meta">
          <span class="meta-item">
            <el-icon><UserFilled /></el-icon>
            {{ t('groups.ownerLabel', { name: item.ownerName || t('groups.unknownUser') }) }}
          </span>
          <span class="meta-item">
            <el-icon><User /></el-icon>
            {{ t('groups.memberCount', { count: item.memberCount }) }}
          </span>
          <span class="meta-item">{{ formatDate(item.createdAt) }}</span>
        </div>
      </article>
    </section>

    <section v-else class="empty-panel">
      <span class="empty-icon"><el-icon><User /></el-icon></span>
      <h3>{{ t('groups.empty') }}</h3>
    </section>
  </main>
</template>

<style scoped>
.groups-page {
  max-width: 860px;
  margin: 0 auto;
  padding: 42px 5% 60px;
}

.eyebrow {
  margin: 0 0 6px;
  color: var(--el-text-color-placeholder);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: .13em;
}

.groups-heading h2 {
  margin: 0 0 6px;
  font-size: 24px;
}

.groups-heading p {
  margin: 0 0 22px;
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 14px;
  margin-bottom: 26px;
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
  border-radius: 9px;
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
  border-radius: 9px;
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

.list-heading {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 14px;
}

.list-heading h3 {
  margin: 0;
  font-size: 17px;
}

.list-heading span {
  color: var(--el-text-color-placeholder);
  font-size: 13px;
}

.group-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.group-card {
  padding: 18px 20px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 14px;
  background: var(--el-bg-color);
  box-shadow: var(--el-box-shadow-lighter);
  cursor: pointer;
  transition: border-color .2s, box-shadow .2s, transform .2s;
}

.group-card:hover {
  border-color: var(--el-color-primary-light-5);
  box-shadow: var(--el-box-shadow-light);
  transform: translateY(-1px);
}

.group-card-title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.group-card-title h4 {
  margin: 0;
  font-size: 16px;
}

.role-badge {
  padding: 2px 9px;
  border-radius: 999px;
  color: var(--el-color-info);
  background: var(--el-color-info-light-9);
  font-size: 11px;
  font-weight: 600;
}

.role-badge.owner {
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.group-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  color: var(--el-text-color-placeholder);
  font-size: 12px;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.empty-panel {
  display: grid;
  justify-items: center;
  gap: 12px;
  padding: 48px 20px;
  border: 1px dashed var(--el-border-color);
  border-radius: 14px;
  background: var(--el-fill-color-lighter);
  text-align: center;
}

.empty-panel h3 {
  margin: 0;
  font-size: 16px;
}

.empty-icon {
  color: var(--el-text-color-placeholder);
  font-size: 40px;
}

.ghost-button {
  padding: 8px 16px;
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  color: var(--el-text-color-regular);
  background: var(--el-bg-color);
  font: inherit;
  font-size: 13px;
  cursor: pointer;
}

.ghost-button:hover:not(:disabled) {
  color: var(--el-color-primary);
  border-color: var(--el-color-primary-light-5);
}

.skeleton-card {
  display: grid;
  gap: 10px;
  cursor: default;
}

.skeleton.line {
  height: 14px;
  border-radius: 7px;
}

.skeleton.line.short {
  width: 30%;
}

.skeleton.line.long {
  width: 85%;
}

.skeleton {
  background: linear-gradient(90deg, var(--el-fill-color-light) 25%, var(--el-fill-color-lighter) 50%, var(--el-fill-color-light) 75%);
  background-size: 200% 100%;
  animation: skeleton-wave 1.4s ease infinite;
}

@keyframes skeleton-wave {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>
