<template>
  <a-layout class="basic-layout">
    <!-- 顶部导航栏 -->
    <GlobalHeader />

    <!-- 内容区域 -->
    <a-layout-content class="main-content">
      <router-view />
    </a-layout-content>

    <!-- 底部版权信息 -->
    <GlobalFooter />



  </a-layout>
</template>

<script setup lang="ts">

</script>

<style scoped>
.basic-layout {
  height: 100%;
}
</style>