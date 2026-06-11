<template>
  <div class="profile-page">
    <div class="container">
      <div class="page-header">
        <h1 class="page-title">个人中心</h1>
        <p class="page-subtitle">管理您的账户信息和会员状态</p>
      </div>

      <a-spin :spinning="loading">
        <div class="profile-grid">
          <!-- 基本信息 + 编辑表单 -->
          <div class="main-section">
            <div class="info-card">
              <div class="card-header">
                <UserOutlined class="card-icon" />
                <span>基本信息</span>
                <a-button type="link" size="small" @click="editing = !editing" class="edit-toggle">
                  {{ editing ? '取消' : '编辑' }}
                </a-button>
              </div>

              <!-- 展示模式 -->
              <div v-if="!editing" class="avatar-section">
                <a-avatar :size="80" :src="loginUser.userAvatar" class="avatar">
                  {{ loginUser.userNickname?.charAt(0) || 'U' }}
                </a-avatar>
                <div class="avatar-info">
                  <h2 class="nickname">{{ loginUser.userNickname || '默认用户' }}</h2>
                  <span :class="['role-tag', isVipUser ? 'vip' : 'normal']">
                    {{ isAdminUser ? '管理员' : isVipUser ? 'VIP会员' : '普通用户' }}
                  </span>
                </div>
              </div>
              <a-divider v-if="!editing" />
              <div v-if="!editing" class="info-list">
                <div class="info-item">
                  <span class="info-label"><IdcardOutlined /> 用户ID</span>
                  <span class="info-value">{{ loginUser.id || '-' }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label"><UserOutlined /> 账号</span>
                  <span class="info-value">{{ loginUser.userAccount || '-' }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label"><ProfileOutlined /> 简介</span>
                  <span class="info-value">{{ loginUser.userProfile || '暂无简介' }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label"><CalendarOutlined /> 注册时间</span>
                  <span class="info-value">{{ loginUser.createTime ? formatDate(loginUser.createTime) : '-' }}</span>
                </div>
              </div>

              <!-- 编辑模式 -->
              <a-form v-else layout="vertical" :model="profileForm" class="profile-form">
                <a-form-item label="昵称">
                  <a-input v-model:value="profileForm.userNickname" placeholder="输入新昵称" />
                </a-form-item>
                <a-form-item label="头像URL">
                  <a-input v-model:value="profileForm.userAvatar" placeholder="输入头像图片URL" />
                </a-form-item>
                <a-form-item label="个人简介">
                  <a-textarea v-model:value="profileForm.userProfile" placeholder="介绍一下自己..." :rows="3" />
                </a-form-item>
                <a-form-item>
                  <a-button type="primary" :loading="saving" @click="handleSaveProfile" block>
                    保存修改
                  </a-button>
                </a-form-item>
              </a-form>
            </div>

            <!-- 修改密码 -->
            <div class="info-card">
              <div class="card-header">
                <LockOutlined class="card-icon" />
                <span>修改密码</span>
              </div>
              <a-form layout="vertical" :model="passwordForm" class="profile-form">
                <a-form-item label="旧密码">
                  <a-input-password v-model:value="passwordForm.oldPassword" placeholder="输入旧密码" />
                </a-form-item>
                <a-form-item label="新密码">
                  <a-input-password v-model:value="passwordForm.newPassword" placeholder="输入新密码（至少8位）" />
                </a-form-item>
                <a-form-item label="确认新密码">
                  <a-input-password v-model:value="passwordForm.checkNewPassword" placeholder="再次输入新密码" />
                </a-form-item>
                <a-form-item>
                  <a-button :loading="changingPwd" @click="handleChangePassword" block>
                    修改密码
                  </a-button>
                </a-form-item>
              </a-form>
            </div>
          </div>

          <!-- 右侧：会员状态 & 操作 -->
          <div class="actions-section">
            <div class="action-card member-card">
              <div class="card-header">
                <CrownOutlined class="card-icon vip-icon" />
                <span>会员状态</span>
              </div>
              <div v-if="isVipUser" class="member-status vip-status">
                <CheckCircleFilled class="status-icon" />
                <span class="status-text">您是永久会员，畅享全部功能</span>
                <div class="member-perks">
                  <div class="perk">✓ 无限创作配额</div>
                  <div class="perk">✓ AI 智能生图</div>
                  <div class="perk">✓ SVG 图表生成</div>
                  <div class="perk">✓ 优先生成队列</div>
                </div>
              </div>
              <div v-else class="member-status normal-status">
                <InfoCircleOutlined class="status-icon" />
                <span class="status-text">您还不是会员</span>
                <p class="member-hint">升级会员解锁全部功能</p>
                <a-button type="primary" size="large" class="upgrade-btn" @click="$router.push('/vip')">
                  <CrownOutlined /> 去升级会员
                </a-button>
              </div>
            </div>

            <div class="action-card">
              <div class="card-header"><ToolOutlined /><span>快捷操作</span></div>
              <div class="action-list">
                <a-button block @click="$router.push('/create')"><EditOutlined />创作文章</a-button>
                <a-button block @click="$router.push('/article/list')"><FileTextOutlined />我的文章</a-button>
                <a-button block @click="$router.push('/vip')"><GiftOutlined />兑换会员</a-button>
              </div>
            </div>
          </div>
        </div>
      </a-spin>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { isAdmin, isVip } from '@/utils/permission'
import { updateMyProfile, changePassword } from '@/api/yonghuguanli'
import dayjs from 'dayjs'
import {
  UserOutlined, IdcardOutlined, ProfileOutlined, CalendarOutlined, LockOutlined,
  CrownOutlined, CheckCircleFilled, InfoCircleOutlined,
  ToolOutlined, EditOutlined, FileTextOutlined, GiftOutlined,
} from '@ant-design/icons-vue'

const loginUserStore = useLoginUserStore()
const loading = ref(false)
const editing = ref(false)
const saving = ref(false)
const changingPwd = ref(false)

const loginUser = computed(() => loginUserStore.loginUser)
const isAdminUser = computed(() => isAdmin(loginUser.value))
const isVipUser = computed(() => isVip(loginUser.value))
const formatDate = (d: string) => dayjs(d).format('YYYY-MM-DD HH:mm')

const profileForm = reactive({
  userNickname: '',
  userAvatar: '',
  userProfile: '',
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  checkNewPassword: '',
})

const initProfileForm = () => {
  profileForm.userNickname = loginUser.value.userNickname || ''
  profileForm.userAvatar = loginUser.value.userAvatar || ''
  profileForm.userProfile = loginUser.value.userProfile || ''
}

// 保存个人信息
const handleSaveProfile = async () => {
  saving.value = true
  try {
    const res = await updateMyProfile({
      userNickname: profileForm.userNickname || undefined,
      userAvatar: profileForm.userAvatar || undefined,
      userProfile: profileForm.userProfile || undefined,
    })
    if (res.data.code === 0 && res.data.data) {
      loginUserStore.setLoginUser(res.data.data)
      message.success('修改成功')
      editing.value = false
    } else {
      message.error(res.data.message || '修改失败')
    }
  } catch {
    message.error('修改失败')
  } finally {
    saving.value = false
  }
}

// 修改密码
const handleChangePassword = async () => {
  if (!passwordForm.oldPassword) { message.warning('请输入旧密码'); return }
  if (passwordForm.newPassword.length < 8) { message.warning('新密码至少8位'); return }
  if (passwordForm.newPassword !== passwordForm.checkNewPassword) { message.warning('两次密码不一致'); return }

  changingPwd.value = true
  try {
    const res = await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
      checkNewPassword: passwordForm.checkNewPassword,
    })
    if (res.data.code === 0) {
      message.success('密码修改成功，请重新登录')
      passwordForm.oldPassword = ''
      passwordForm.newPassword = ''
      passwordForm.checkNewPassword = ''
    } else {
      message.error(res.data.message || '修改失败')
    }
  } catch {
    message.error('修改失败')
  } finally {
    changingPwd.value = false
  }
}

onMounted(async () => {
  loading.value = true
  await loginUserStore.fetchLoginUser()
  initProfileForm()
  loading.value = false
})
</script>

<style scoped lang="scss">
.profile-page { min-height: calc(100vh - 64px); background: var(--color-background-secondary); padding: 40px 24px 80px; }
.container { max-width: 1000px; margin: 0 auto; }
.page-header { text-align: center; margin-bottom: 40px; }
.page-title { font-size: 28px; font-weight: 700; color: var(--color-text); margin: 0 0 8px; }
.page-subtitle { font-size: 14px; color: var(--color-text-secondary); margin: 0; }
.profile-grid { display: grid; grid-template-columns: 1fr 360px; gap: 24px; }
.main-section { display: flex; flex-direction: column; gap: 24px; }
.info-card, .action-card { background: white; border-radius: var(--radius-lg); border: 1px solid var(--color-border); padding: 28px; }
.card-header { display: flex; align-items: center; gap: 10px; font-size: 16px; font-weight: 600; color: var(--color-text); margin-bottom: 24px;
  .card-icon { color: var(--color-primary); font-size: 18px; &.vip-icon { color: #EAB308; } } }
.edit-toggle { margin-left: auto; }
.avatar-section { display: flex; align-items: center; gap: 20px; }
.avatar { background: var(--gradient-primary); color: white; font-weight: 600; font-size: 28px; }
.avatar-info { flex: 1; }
.nickname { font-size: 22px; font-weight: 600; margin: 0 0 8px; color: var(--color-text); }
.role-tag { display: inline-block; padding: 4px 12px; border-radius: var(--radius-full); font-size: 12px; font-weight: 600;
  &.vip { background: var(--gradient-primary); color: white; } &.normal { background: var(--color-background-tertiary); color: var(--color-text-secondary); } }
.info-list { display: flex; flex-direction: column; gap: 16px; }
.info-item { display: flex; justify-content: space-between; align-items: center; padding: 12px 16px; background: var(--color-background-secondary); border-radius: var(--radius-md); }
.info-label { display: flex; align-items: center; gap: 8px; font-size: 13px; color: var(--color-text-secondary); }
.info-value { font-size: 14px; color: var(--color-text); font-weight: 500; max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.profile-form { .ant-form-item:last-child { margin-bottom: 0; } }
.actions-section { display: flex; flex-direction: column; gap: 24px; }
.member-card { background: linear-gradient(135deg, #FFFBEB 0%, white 100%); border-color: rgba(234, 179, 8, 0.2); }
.member-status { text-align: center; padding: 16px 0; .status-icon { font-size: 40px; margin-bottom: 12px; } .status-text { display: block; font-size: 16px; font-weight: 600; margin-bottom: 12px; } }
.vip-status { .status-icon { color: var(--color-primary); } .status-text { color: var(--color-primary-dark); } }
.normal-status { .status-icon { color: var(--color-text-muted); } .status-text { color: var(--color-text-secondary); } }
.member-perks { text-align: left; max-width: 200px; margin: 0 auto; .perk { font-size: 13px; color: var(--color-text-secondary); padding: 4px 0; } }
.member-hint { font-size: 13px; color: var(--color-text-muted); margin-bottom: 16px; }
.upgrade-btn { width: 100%; height: 44px; font-weight: 600; border-radius: var(--radius-md); background: var(--gradient-primary) !important; border: none !important; color: white !important; }
.action-list { display: flex; flex-direction: column; gap: 12px; .ant-btn { height: 42px; font-size: 14px; border-radius: var(--radius-md); display: flex; align-items: center; justify-content: center; gap: 8px; } }
@media (max-width: 768px) { .profile-grid { grid-template-columns: 1fr; } .page-title { font-size: 22px; } }
</style>
