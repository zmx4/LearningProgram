<script setup lang="ts">

</script>

<template>
  <div style="width: 100vw;height: 100vh;overflow: hidden;display: flex">
    <div style="flex: 1;background: aqua">
      <el-image style="width: 100%;height: 100%" fit="cover"
                src="https://img1.baidu.com/it/u=4097856652,4033702227&fm=253&fmt=auto&app=120&f=JPEG?w=1422&h=800"/>
    </div>
    <div class="welcome-title">
      <div style="font-size: 30px; font-weight: bold">欢迎来到我们的学习平台</div>
      <div style="margin-top: 10px">.....</div>
    </div>
    <div class="right-card">
      <router-view />
    </div>
  </div>
</template>
  
<style scoped>
.right-card{
  width: 400px;
  z-index: 1;
  background: wheat;
}
.welcome-title {
  position: absolute;
  bottom: 30px;
  left: 30px;
  color: white;
  text-shadow: 0 0 10px black;
}
</style>
