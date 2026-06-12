import { createRouter, createWebHistory } from 'vue-router'
import HomePage from '@/pages/HomePage.vue'
import UserLoginPage from '@/pages/user/UserLoginPage.vue'
import UserRegisterPage from '@/pages/user/UserRegisterPage.vue'
import UserManagePage from '@/pages/admin/UserManagePage.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: '主页',
      component: HomePage,
    },
    {
      path: '/user/login',
      name: '用户登录',
      component: UserLoginPage,
    },
    {
      path: '/user/register',
      name: '用户注册',
      component: UserRegisterPage,
    },
    {
      path: '/user/profile',
      name: '个人中心',
      component: () => import('@/pages/user/UserProfilePage.vue'),
    },
    {
      path: '/admin/userManage',
      name: '用户管理',
      component: () => import('@/pages/admin/UserManagePage.vue'),
    },
    {
      path: '/admin/statistics',
      name: '数据分析',
      component: () => import('@/pages/admin/StatisticsPage.vue'),
    },
    {
      path: '/admin/prompts',
      name: 'Prompt管理',
      component: () => import('@/pages/admin/PromptManagePage.vue'),
    },
    {
      path: '/vip',
      name: '会员兑换',
      component: () => import('@/pages/VipPage.vue'),
    },
    {
      path: '/create',
      name: '文章创作',
      component: () => import('@/pages/article/ArticleCreatePage.vue'),
    },
    {
      path: '/template',
      name: '文案模板',
      component: () => import('@/pages/template/TemplatePage.vue'),
    },
    {
      path: '/batch',
      name: '批量创作',
      component: () => import('@/pages/article/BatchCreatePage.vue'),
    },
    {
      path: '/article/list',
      name: '文章列表',
      component: () => import('@/pages/article/ArticleListPage.vue'),
    },
    {
      path: '/article/:taskId',
      name: '文章详情',
      component: () => import('@/pages/article/ArticleDetailPage.vue'),
    },
  ],
})

export default router
