import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getLoginUser } from '@/api/yonghuguanli'
import { DEFAULT_USERNAME } from '@/constants/user'

/**
 * 登录用户信息
 */
export const useLoginUserStore = defineStore('loginUser', () => {
  // 默认值
  const loginUser = ref<API.LoginUserVO>({
    userNickname: DEFAULT_USERNAME,
  })

  // 获取登录用户信息
  async function fetchLoginUser() {
    try {
      const res = await getLoginUser()
      if (res.data.code === 0 && res.data.data) {
        loginUser.value = res.data.data
      }
    } catch (error) {
      // 网络错误或后端未启动，保持未登录状态
      console.warn('获取用户信息失败:', error)
    }
  }

  // 更新登录用户信息
  function setLoginUser(newLoginUser: Partial<API.LoginUserVO>) {
    loginUser.value = { ...loginUser.value, ...newLoginUser }
  }

  return { loginUser, fetchLoginUser, setLoginUser }
})
