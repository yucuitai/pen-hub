<script setup lang="ts">
import { computed } from 'vue'
import { theme } from 'ant-design-vue'
import BasicLayout from '@/layouts/BasicLayout.vue'
import { useThemeStore } from '@/stores/themeStore'

const themeStore = useThemeStore()

const antTheme = computed(() => ({
  token: {
    colorPrimary: '#D97706',
    colorSuccess: '#16A34A',
    colorWarning: '#D97706',
    colorError: '#DC2626',
    colorInfo: '#0284C7',
    borderRadius: 8,
    fontFamily: "'Noto Sans SC', 'Inter', -apple-system, BlinkMacSystemFont, sans-serif",
  },
  algorithm: themeStore.theme === 'dark' ? theme.darkAlgorithm : theme.defaultAlgorithm,
}))
</script>

<template>
  <a-config-provider :theme="antTheme">
    <BasicLayout />
  </a-config-provider>
</template>

<style>
/* 全局样式已在 main.css 中定义 */
</style>
