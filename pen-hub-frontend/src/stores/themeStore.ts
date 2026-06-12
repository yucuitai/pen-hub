import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

export type ThemeMode = 'light' | 'dark'

/**
 * 主题管理 Store
 * 持久化到 localStorage，刷新后保持主题选择
 */
export const useThemeStore = defineStore(
  'theme',
  () => {
    const theme = ref<ThemeMode>('light')

    /** 切换主题 */
    function toggleTheme() {
      theme.value = theme.value === 'light' ? 'dark' : 'light'
    }

    /** 设置指定主题 */
    function setTheme(mode: ThemeMode) {
      theme.value = mode
    }

    /** 应用主题到 DOM */
    function applyTheme(mode: ThemeMode) {
      document.documentElement.setAttribute('data-theme', mode)
    }

    // 监听变化并应用到 DOM
    watch(
      theme,
      (newTheme) => {
        applyTheme(newTheme)
      },
      { immediate: true },
    )

    return { theme, toggleTheme, setTheme }
  },
  {
    persist: true,
  },
)
