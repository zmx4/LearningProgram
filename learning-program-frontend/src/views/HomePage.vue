<script setup lang="ts">
import { Reading } from '@element-plus/icons-vue'
</script>

<template>
  <main class="content">
        <div class="hero-card">
          <div>
            <p class="card-label">今日学习计划</p>
            <h2>保持专注，持续进步</h2>
            <p class="hero-copy">安排好今天的学习时间，让每一次积累都离目标更近一步。</p>
            <button class="primary-button" type="button">开始学习</button>
          </div>
          <div class="hero-decoration" aria-hidden="true">
            <div class="decoration-circle circle-large"></div>
            <div class="decoration-circle circle-small"></div>
            <span>✦</span>
          </div>
        </div>

        <div class="section-heading">
          <div>
            <p class="card-label">概览</p>
            <h2>学习概况</h2>
          </div>
          <span class="placeholder-chip">数据即将上线</span>
        </div>

        <div class="stats-grid">
          <article v-for="stat in [
            { label: '进行中的课程', value: '—', hint: '等待课程数据' },
            { label: '本周学习时长', value: '—', hint: '等待学习记录' },
            { label: '完成的课程', value: '—', hint: '等待完成数据' },
          ]" :key="stat.label" class="stat-card">
            <p>{{ stat.label }}</p>
            <strong>{{ stat.value }}</strong>
            <span>{{ stat.hint }}</span>
          </article>
        </div>

        <div class="empty-panel">
          <div class="empty-icon"><el-icon>
              <Reading />
            </el-icon></div>
          <h3>准备好开始你的学习了吗？</h3>
          <p>课程、学习资源和进度信息将在这里展示。</p>
        </div>
  </main>
</template>

<style scoped>
:global(*) {
  box-sizing: border-box;
}

:global(body) {
  background: var(--el-bg-color-page);
  color: var(--el-text-color-primary);
  font-family: Inter, "PingFang SC", "Microsoft YaHei", sans-serif;
}

.app-shell {
  display: flex;
  min-height: 100vh;
  background: var(--el-bg-color-page);
}

.sidebar {
  width: 248px;
  flex: 0 0 248px;
  display: flex;
  flex-direction: column;
  padding: 28px 16px 20px;
  background: var(--el-bg-color);
  border-right: 1px solid var(--el-border-color-light);
}

.brand {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 0 13px;
  color: var(--el-text-color-primary);
  font-size: 18px;
  font-weight: 700;
}

.brand-mark {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  border-radius: 10px;
  color: var(--el-color-white);
  background: var(--el-color-primary);
  box-shadow: var(--el-box-shadow-light);
}

.sidebar-placeholder {
  display: grid;
  gap: 9px;
  margin: 42px 13px 24px;
}

.sidebar-placeholder span {
  width: 100%;
  height: 9px;
  border-radius: 5px;
  background: var(--el-fill-color-light);
}

.sidebar-placeholder span:nth-child(2) {
  width: 72%;
}

.sidebar-placeholder span:nth-child(3) {
  width: 86%;
}

.side-nav {
  display: grid;
  gap: 7px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 14px;
  width: 100%;
  padding: 12px 14px;
  border: 0;
  border-radius: 10px;
  color: var(--el-text-color-secondary);
  background: transparent;
  font: inherit;
  font-size: 14px;
  text-align: left;
  cursor: pointer;
  transition: .2s;
}

.nav-item:hover {
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.nav-item.active {
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  font-weight: 600;
}

.nav-item .el-icon {
  font-size: 18px;
}

.sidebar-footer {
  margin-top: auto;
  padding-top: 20px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.page {
  min-width: 0;
  flex: 1;
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 100px;
  padding: 22px 5%;
  background: var(--el-bg-color);
  border-bottom: 1px solid var(--el-border-color-light);
}

.eyebrow,
.card-label {
  margin: 0 0 7px;
  color: var(--el-text-color-placeholder);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: .13em;
}

h1,
h2,
h3,
p {
  margin-top: 0;
}

h1 {
  margin-bottom: 0;
  font-size: 24px;
}

.topbar-actions {
  display: flex;
  align-items: center;
  gap: 22px;
}

.icon-button,
.logout-button {
  border: 0;
  background: transparent;
  cursor: pointer;
}

.icon-button {
  position: relative;
  display: grid;
  place-items: center;
  color: var(--el-text-color-secondary);
  font-size: 20px;
}

.notification-dot {
  position: absolute;
  top: -1px;
  right: -2px;
  width: 6px;
  height: 6px;
  border: 1px solid var(--el-color-white);
  border-radius: 50%;
  background: var(--el-color-danger);
}

.profile {
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  border-radius: 50%;
  color: var(--el-color-white);
  background: var(--el-color-info);
  font-size: 18px;
}

.profile-name {
  color: var(--el-text-color-regular);
  font-size: 14px;
}

.logout-button {
  padding: 9px 14px;
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  color: var(--el-text-color-regular);
  font-size: 13px;
}

.logout-button:hover {
  color: var(--el-color-primary);
  border-color: var(--el-color-primary-light-5);
  background: var(--el-color-primary-light-9);
}

.content {
  max-width: 1180px;
  margin: 0 auto;
  padding: 42px 5% 60px;
}

.hero-card {
  position: relative;
  display: flex;
  min-height: 224px;
  align-items: center;
  justify-content: space-between;
  overflow: hidden;
  padding: 34px 42px;
  border-radius: 18px;
  color: var(--el-color-white);
  background: linear-gradient(120deg, var(--el-color-primary), var(--el-color-primary-light-3));
  box-shadow: var(--el-box-shadow);
}

.hero-card h2 {
  margin-bottom: 10px;
  font-size: 28px;
}

.hero-copy {
  max-width: 470px;
  margin-bottom: 24px;
  color: var(--el-color-primary-light-9);
  font-size: 14px;
  line-height: 1.7;
}

.primary-button {
  padding: 11px 20px;
  border: 0;
  border-radius: 8px;
  color: var(--el-color-primary);
  background: var(--el-color-white);
  font: inherit;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.hero-decoration {
  position: relative;
  width: 190px;
  height: 160px;
  margin-right: 5%;
  opacity: .8;
}

.decoration-circle {
  position: absolute;
  border: 1px solid color-mix(in srgb, var(--el-color-white) 30%, transparent);
  border-radius: 50%;
}

.circle-large {
  inset: -20px -15px 0 15px;
}

.circle-small {
  inset: 25px 30px 35px 55px;
}

.hero-decoration span {
  position: absolute;
  top: 55px;
  left: 96px;
  font-size: 35px;
}

.section-heading {
  display: flex;
  align-items: end;
  justify-content: space-between;
  margin: 42px 0 18px;
}

.section-heading h2 {
  margin-bottom: 0;
  font-size: 20px;
}

.placeholder-chip {
  padding: 7px 11px;
  border-radius: 20px;
  color: var(--el-text-color-secondary);
  background: var(--el-fill-color-light);
  font-size: 12px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 18px;
}

.stat-card {
  padding: 22px 24px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 14px;
  background: var(--el-bg-color);
}

.stat-card p {
  margin-bottom: 18px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.stat-card strong {
  display: block;
  margin-bottom: 9px;
  color: var(--el-text-color-primary);
  font-size: 30px;
}

.stat-card span {
  color: var(--el-text-color-placeholder);
  font-size: 12px;
}

.empty-panel {
  display: grid;
  justify-items: center;
  margin-top: 18px;
  padding: 48px 20px;
  border: 1px dashed var(--el-border-color);
  border-radius: 14px;
  background: var(--el-fill-color-lighter);
  text-align: center;
}

.empty-icon {
  display: grid;
  width: 50px;
  height: 50px;
  place-items: center;
  margin-bottom: 16px;
  border-radius: 14px;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  font-size: 24px;
}

.empty-panel h3 {
  margin-bottom: 8px;
  font-size: 16px;
}

.empty-panel p {
  margin-bottom: 0;
  color: var(--el-text-color-placeholder);
  font-size: 13px;
}

@media (max-width: 760px) {
  .sidebar {
    width: 70px;
    flex-basis: 70px;
    padding: 22px 10px;
  }

  .brand {
    justify-content: center;
    padding: 0;
  }

  .brand span,
  .nav-item span,
  .sidebar-placeholder {
    display: none;
  }

  .nav-item {
    justify-content: center;
    padding: 13px 0;
  }

  .topbar {
    align-items: flex-start;
    gap: 16px;
  }

  .profile-name {
    display: none;
  }

  .topbar-actions {
    gap: 12px;
  }

  .hero-card {
    padding: 28px;
  }

  .hero-decoration {
    display: none;
  }

  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>
