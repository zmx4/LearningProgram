<script setup lang="ts">
</script>

<template>
  <div class="welcome-shell">
    <div class="welcome-hero">
      <!-- 首屏 LCP 图片：原生 img + fetchpriority，WebP 版约 43KB，比原来 160KB 的 PNG 小一个量级 -->
      <img
        class="welcome-hero-image"
        src="/background.webp"
        alt=""
        width="799"
        height="533"
        fetchpriority="high"
        decoding="async"
      />
    </div>
    <div class="welcome-title">
      <div style="font-size: 30px; font-weight: bold">{{ $t('welcome.title') }}</div>
      <div style="margin-top: 10px">一个很有意思的学习平台</div>
    </div>
    <div class="right-card">
      <router-view v-slot="{ Component }">
        <transition name="el-fade-in-linear"  mode="out-in">
          <Component :is="Component"/>
        </transition>
      </router-view>
    </div>
  </div>
</template>

<style scoped>
.welcome-shell {
  width: 100vw;
  min-height: 100vh;
  min-height: 100dvh;
  overflow-x: hidden;
  display: flex;
}

.welcome-hero {
  flex: 1;
  background: aqua;
}

.welcome-hero-image {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.right-card {
  box-sizing: border-box;
  width: 400px;
  min-height: 100vh;
  min-height: 100dvh;
  overflow-y: auto;
  z-index: 1;
  background-color: var(--el-bg-color);
}

.welcome-title {
  position: absolute;
  bottom: 30px;
  left: 30px;
  color: white;
  text-shadow: 0 0 10px black;
}

@media (max-width: 760px) {
  /* On phones the decorative image is dropped so the auth card owns the screen. */
  .welcome-hero,
  .welcome-title {
    display: none;
  }

  .right-card {
    width: 100%;
    min-height: 100vh;
    min-height: 100dvh;
  }
}
</style>
