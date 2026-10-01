<script setup lang="ts">
import { ArrowRight, EditPen, Notebook } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'

const router = useRouter()
const {t} = useI18n()

const entries = [
  {
    name: 'tests-words',
    title: 'tests.word.title',
    description: 'tests.word.description',
    icon: EditPen,
    badge: '',
  },
  {
    name: 'tests-knowledge',
    title: 'tests.knowledge.title',
    description: 'tests.knowledge.description',
    icon: Notebook,
    badge: '',
  },
]
</script>

<template>
  <main class="tests-page">
    <section class="tests-heading">
      <p class="eyebrow">LEARNING TESTS</p>
      <h2>{{ t('tests.title') }}</h2>
      <p>{{ t('tests.description') }}</p>
    </section>

    <section class="test-entries">
      <button
          v-for="entry in entries"
          :key="entry.name"
          class="test-entry"
          type="button"
          @click="router.push({ name: entry.name })"
      >
        <span class="entry-head">
          <span class="entry-icon">
            <el-icon>
              <component :is="entry.icon"/>
            </el-icon>
          </span>
          <span v-if="entry.badge" class="entry-badge">{{ t(entry.badge) }}</span>
        </span>
        <strong>{{ t(entry.title) }}</strong>
        <p>{{ t(entry.description) }}</p>
        <span class="entry-action">
          {{ t('tests.enter') }}
          <el-icon>
            <ArrowRight/>
          </el-icon>
        </span>
      </button>
    </section>
  </main>
</template>

<style scoped>
.tests-page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 42px 5% 60px;
}

.eyebrow {
  margin: 0 0 8px;
  color: var(--el-text-color-placeholder);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: .13em;
}

.tests-heading h2 {
  margin: 0 0 8px;
  font-size: 26px;
}

.tests-heading > p:last-child {
  margin: 0 0 28px;
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.test-entries {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 18px;
}

.test-entry {
  display: grid;
  gap: 10px;
  align-content: start;
  padding: 26px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 16px;
  background: var(--el-bg-color);
  box-shadow: var(--el-box-shadow-lighter);
  font: inherit;
  text-align: left;
  cursor: pointer;
  transition: border-color .2s ease, box-shadow .2s ease, transform .2s ease;
}

.test-entry:hover {
  border-color: var(--el-color-primary-light-5);
  box-shadow: var(--el-box-shadow-light);
  transform: translateY(-2px);
}

.entry-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.entry-icon {
  display: grid;
  width: 40px;
  height: 40px;
  place-items: center;
  border-radius: 12px;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  font-size: 20px;
}

.entry-badge {
  margin-left: auto;
  padding: 3px 9px;
  border-radius: 999px;
  color: var(--el-text-color-secondary);
  background: var(--el-fill-color);
  font-size: 11px;
  font-weight: 600;
}

.test-entry strong {
  font-size: 19px;
}

.test-entry p {
  margin: 0;
  color: var(--el-text-color-secondary);
  font-size: 14px;
  line-height: 1.6;
}

.entry-action {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: 4px;
  color: var(--el-color-primary);
  font-size: 13px;
  font-weight: 600;
}

.test-entry:hover .entry-action {
  color: var(--el-color-primary-light-3);
}
</style>
