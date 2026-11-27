<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { User } from '@element-plus/icons-vue'
import GroupActions from '@/components/group/GroupActions.vue'
import GroupCard from '@/components/group/GroupCard.vue'
import { get } from '@/net'
import type { GroupSummary } from '@/types/group'

const router = useRouter()
const { t } = useI18n()

const groups = ref<GroupSummary[]>([])
const loading = ref(true)
const loadFailed = ref(false)
let disposed = false

function loadGroups() {
  loading.value = true
  loadFailed.value = false
  get<GroupSummary[]>('/api/groups', data => {
    if (disposed) return
    groups.value = data
    loading.value = false
  }, () => {
    if (disposed) return
    loading.value = false
    loadFailed.value = true
  })
}

function onGroupChanged(group: GroupSummary) {
  groups.value = [group, ...groups.value.filter(item => item.id !== group.id)]
}

function openGroup(group: GroupSummary) {
  void router.push({ name: 'group-detail', params: { id: group.id } })
}

function openLeaderboard(group: GroupSummary) {
  void router.push({ name: 'leaderboard', query: { groupId: group.id } })
}

onMounted(loadGroups)
onUnmounted(() => {
  disposed = true
})
</script>

<template>
  <main class="groups-page">
    <section class="groups-heading">
      <p class="eyebrow">{{ t('groups.eyebrow') }}</p>
      <h2>{{ t('groups.title') }}</h2>
      <p>{{ t('groups.description') }}</p>
    </section>

    <GroupActions @changed="onGroupChanged" />

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
      <GroupCard
          v-for="group in groups"
          :key="group.id"
          :group="group"
          @open="openGroup"
          @leaderboard="openLeaderboard"
      />
    </section>

    <section v-else class="empty-panel">
      <span class="empty-icon"><el-icon><User /></el-icon></span>
      <h3>{{ t('groups.empty') }}</h3>
    </section>
  </main>
</template>

<style scoped>
.groups-page {
  max-width: 1180px;
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

.list-heading {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin: 26px 0 14px;
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
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 14px;
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
  padding: 18px 20px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 14px;
  background: var(--el-bg-color);
}

.skeleton.line {
  height: 14px;
  border-radius: 7px;
}

.skeleton.line.short { width: 30%; }
.skeleton.line.long { width: 85%; }

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
