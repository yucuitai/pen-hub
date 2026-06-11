import { useLoginUserStore } from '@/stores/loginUser'
import { message } from 'ant-design-vue'
import router from '@/router'
import { USER_ROLE_ADMIN } from '@/constants/user'

// 是否为首次获取登录用户
let firstFetchLoginUser = true

/**
 * 全局权限校验
 */
router.beforeEach(async (to, from, next) => {
  const loginUserStore = useLoginUserStore()
  let loginUser = loginUserStore.loginUser
  // 确保页面刷新，首次加载时，能够等后端返回用户信息后再校验权限
  if (firstFetchLoginUser) {
    try {
      await loginUserStore.fetchLoginUser()
      loginUser = loginUserStore.loginUser
    } catch (error) {
      // 后端未连接或网络错误，使用默认用户信息继续
      console.warn('获取登录用户信息失败，使用未登录状态:', error)
    }
    firstFetchLoginUser = false
  }
  const toUrl = to.fullPath
  // 需要登录才能访问的路由
  const loginRequiredPaths = ['/create', '/article/list', '/user/profile']
  const needLogin = loginRequiredPaths.some((path) => toUrl.startsWith(path))
  if (needLogin) {
    if (!loginUser || !loginUser.id) {
      message.warning('请先登录')
      next(`/user/login?redirect=${to.fullPath}`)
      return
    }
  }
  if (toUrl.startsWith('/admin')) {
    if (!loginUser || loginUser.userRole !== USER_ROLE_ADMIN) {
      message.error('没有权限')
      next(`/user/login?redirect=${to.fullPath}`)
      return
    }
  }
  next()
})
