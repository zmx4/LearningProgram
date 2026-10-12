<script setup lang="ts">
</script>

<template>
  <div class="welcome-shell">
    <div class="welcome-hero">
      <el-image style="width: 100%;height: 100%" fit="cover"
                src="https://img1.baidu.com/it/u=4097856652,4033702227&fm=253&fmt=auto&app=120&f=JPEG?w=1422&h=800"/>
    </div>
    <div class="welcome-title">
      <div style="font-size: 30px; font-weight: bold">{{ $t('welcome.title') }}</div>
      <div style="margin-top: 10px">.....</div>
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
  height: 100vh;
  overflow: hidden;
  display: flex;
}

.welcome-hero {
  flex: 1;
  background: aqua;
}

.right-card {
  width: 400px;
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
    height: 100%;
    overflow-y: auto;
  }
}
</style>
