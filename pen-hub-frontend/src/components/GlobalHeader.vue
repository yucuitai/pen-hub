<template>
  <a-layout-header class="header">
    <div class="header-container">
      <div class="header-left">
        <RouterLink to="/" class="logo-link">
          <div class="logo-wrapper">
            <img src="@/assets/logo.png" alt="Logo" class="logo-img" />
            <h1 class="site-title">笔枢</h1>
          </div>
        </RouterLink>
      </div>

      <!-- 中间：导航菜单 -->
      <nav class="nav-center">
        <RouterLink
          v-for="item in visibleMenuItems"
          :key="item.key"
          :to="item.key"
          :class="['nav-item', { active: selectedKeys.includes(item.key) }]"
        >
          <component :is="item.icon" class="nav-icon" />
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>

      <!-- 右侧：用户操作区域 -->
      <div class="header-right">
        <!-- 主题切换按钮 -->
        <div class="theme-toggle" @click="themeStore.toggleTheme()" :title="themeStore.theme === 'dark' ? '切换到亮色模式' : '切换到暗色模式'">
          <span class="theme-icon">{{ themeStore.theme === 'dark' ? '☀️' : '🌙' }}</span>
        </div>

        <!-- 已登录：显示用户信息 -->
        <div v-if="isLoggedIn" class="user-section">
          <a-dropdown>
            <div class="user-info">
              <a-avatar :size="32" :src="loginUser.userAvatar" class="user-avatar">
                {{ loginUser.userNickname?.charAt(0) || 'U' }}
              </a-avatar>
              <span class="user-name">{{ loginUser.userNickname || '用户' }}</span>
              <span v-if="isAdminUser && loginUser.userNickname !== '管理员'" class="role-badge admin">管理员</span>
              <span v-else-if="isVipUser && !isAdminUser && loginUser.userNickname !== 'VIP'" class="role-badge vip">VIP</span>
            </div>
            <template #overlay>
              <a-menu>
                <a-menu-item key="profile" @click="goToProfile">
                  <UserOutlined />
                  个人中心
                </a-menu-item>
                <a-menu-item key="articles" @click="goToArticles">
                  <FileTextOutlined />
                  我的文章
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item key="logout" @click="handleLogout">
                  <LogoutOutlined />
                  退出登录
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>

        <!-- 未登录：显示登录按钮 -->
        <RouterLink v-else to="/user/login" class="login-btn">登录</RouterLink>
      </div>
    </div>
  </a-layout-header>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  HomeOutlined,
  EditOutlined,
  UnorderedListOutlined,
  SettingOutlined,
  BarChartOutlined,
  CrownOutlined,
  UserOutlined,
  FileTextOutlined,
  LogoutOutlined,
} from '@ant-design/icons-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { useThemeStore } from '@/stores/themeStore'
import { userLogout } from '@/api/yonghuguanli'
import { isAdmin, isVip } from '@/utils/permission'

const router = useRouter()
const loginUserStore = useLoginUserStore()
const themeStore = useThemeStore()

// 用户状态
const loginUser = computed(() => loginUserStore.loginUser)
const isLoggedIn = computed(() => !!loginUser.value.id)
const isAdminUser = computed(() => isAdmin(loginUser.value))
const isVipUser = computed(() => isVip(loginUser.value))

// 当前选中菜单
const selectedKeys = ref<string[]>(['/'])
// 监听路由变化，更新当前选中菜单
router.afterEach((to) => {
  selectedKeys.value = [to.path]
})

// 菜单配置项
const menuItems = [
  {
    key: '/',
    icon: HomeOutlined,
    label: '首页',
    show: true,
  },
  {
    key: '/create',
    icon: EditOutlined,
    label: '创作',
    show: true,
  },
  {
    key: '/article/list',
    icon: UnorderedListOutlined,
    label: '历史',
    show: true,
  },
  {
    key: '/vip',
    icon: CrownOutlined,
    label: '会员',
    show: true,
  },
  {
    key: '/admin/userManage',
    icon: SettingOutlined,
    label: '管理',
    show: computed(() => isAdminUser.value),
  },
  {
    key: '/admin/statistics',
    icon: BarChartOutlined,
    label: '数据',
    show: computed(() => isAdminUser.value),
  },
]

// 过滤可见菜单项
const visibleMenuItems = computed(() => {
  return menuItems.filter((item) => {
    if (typeof item.show === 'object' && 'value' in item.show) {
      return item.show.value
    }
    return item.show
  })
})

// 跳转到个人中心
const goToProfile = () => {
  router.push('/user/profile')
}

// 跳转到我的文章
const goToArticles = () => {
  router.push('/article/list')
}

// 退出登录
const handleLogout = async () => {
  try {
    await userLogout()
    // 清除用户信息
    loginUserStore.setLoginUser({
      id: undefined,
      userAccount: undefined,
      userNickname: undefined,
      userAvatar: undefined,
      userProfile: undefined,
      userRole: undefined,
    })
    message.success('已退出登录')
    router.push('/')
  } catch (error) {
    console.error('退出登录失败:', error)
    message.error('退出登录失败')
  }
}
</script>

<style scoped>
.header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: var(--glass-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  padding: 0;
  height: 64px;
  line-height: 64px;
  border-bottom: 1px solid var(--color-border);
  transition: all var(--transition-normal);
  overflow: hidden;
}

.header-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
}

.header-left {
  display: flex;
  align-items: center;
}

.logo-link {
  display: block;
  transition: opacity var(--transition-fast);
}

.logo-link:hover {
  opacity: 0.8;
}

.logo-wrapper {
  display: flex;
  align-items: center;
  gap: 10px;
}

.logo-img {
  width: 36px;
  height: 36px;
  object-fit: contain;
}

.site-title {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: var(--color-text);
  white-space: nowrap;
  letter-spacing: -0.3px;
}

/* 导航菜单 */
.nav-center {
  display: flex;
  align-items: center;
  gap: 8px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-secondary);
  transition: all var(--transition-fast);
  text-decoration: none;
}

.nav-item:hover {
  color: var(--color-text);
  background: var(--color-background-secondary);
}

.nav-item.active {
  color: var(--color-primary-dark);
  background: rgba(217, 119, 6, 0.1);
}

.nav-icon {
  font-size: 16px;
}

/* 用户区域 */
.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

/* 用户信息区域 */
.user-section {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
}

.user-info:hover {
  background: var(--color-background-secondary);
}

.user-avatar {
  background: var(--gradient-primary);
  color: white;
  font-weight: 600;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text);
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.role-badge {
  font-size: 11px;
  padding: 2px 6px;
  border-radius: var(--radius-full);
  font-weight: 600;
}

.role-badge.admin {
  background: linear-gradient(135deg, #0F172A 0%, #1E293B 100%);
  color: white;
}

.role-badge.vip {
  background: var(--gradient-primary);
  color: white;
}

/* 登录按钮 */
.login-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 38px;
  padding: 0 24px;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 600;
  color: white;
  background: var(--gradient-primary);
  border: none;
  box-shadow: var(--shadow-warm);
  transition: all var(--transition-normal);
  text-decoration: none;
}

.login-btn:hover {
  color: white;
  box-shadow: 0 6px 20px rgba(217, 119, 6, 0.35);
}

/* 主题切换按钮 */
.theme-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: var(--radius-full);
  cursor: pointer;
  transition: all var(--transition-fast);
  background: var(--color-background-secondary);
  color: var(--color-text-secondary);
}

.theme-toggle:hover {
  background: var(--color-background-tertiary);
  color: var(--color-primary);
}

.theme-icon {
  font-size: 20px;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 响应式 */
@media (max-width: 768px) {
  .header-container {
    padding: 0 16px;
  }

  .site-title {
    display: none;
  }

  .nav-item span {
    display: none;
  }

  .nav-item {
    padding: 8px 12px;
  }

  .user-name {
    display: none;
  }

  .role-badge {
    display: none;
  }
}
</style>
