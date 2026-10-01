<script setup lang="ts">
import { ArrowLeft, Notebook } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'

const router = useRouter()
const {t} = useI18n()
</script>

<template>
  <main class="tests-page">
    <button class="back-button" type="button" @click="router.push({ name: 'tests' })">
      <el-icon>
        <ArrowLeft/>
      </el-icon>
      {{ t('tests.back') }}
    </button>

    <section class="tests-heading">
      <p class="eyebrow">LEARNING TESTS</p>
      <h2>{{ t('tests.knowledge.title') }}</h2>
      <p>{{ t('tests.knowledge.description') }}</p>
    </section>

    <section class="placeholder-card">
      <span class="placeholder-icon">
        <el-icon>
          <Notebook/>
        </el-icon>
      </span>
      <h3>{{ t('tests.knowledge.placeholderTitle') }}</h3>
      <p>{{ t('tests.knowledge.placeholder') }}</p>
    </section>
  </main>
</template>

<style scoped>
.tests-page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 42px 5% 60px;
}

.back-button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 28px;
  padding: 0;
  border: 0;
  color: var(--el-text-color-secondary);
  background: transparent;
  font: inherit;
  font-size: 13px;
  cursor: pointer;
}

.back-button:hover {
  color: var(--el-color-primary);
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

.placeholder-card {
  display: grid;
  justify-items: center;
  gap: 10px;
  padding: 56px 26px;
  border: 1px dashed var(--el-border-color);
  border-radius: 16px;
  background: var(--el-bg-color);
  text-align: center;
}

.placeholder-icon {
  display: grid;
  width: 52px;
  height: 52px;
  place-items: center;
  border-radius: 14px;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  font-size: 26px;
}

.placeholder-card h3 {
  margin: 4px 0 0;
  font-size: 20px;
}

.placeholder-card p {
  margin: 0;
  color: var(--el-text-color-secondary);
  font-size: 14px;
}
</style>
