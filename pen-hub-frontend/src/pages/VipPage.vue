<template>
  <div class="vip-page">
    <div class="vip-container">
      <!-- 页面头部 -->
      <div class="page-header">
        <div class="header-badge">
          <CrownOutlined />
          <span>会员专属</span>
        </div>
        <h1 class="page-title">升级永久会员</h1>
        <p class="page-subtitle">解锁全部高级功能，无限创作配额，终身有效</p>
      </div>

      <!-- 主内容区：左右布局 -->
      <div class="main-section">
        <!-- 左侧：兑换码卡片 -->
        <div class="redeem-card">
          <div class="redeem-badge">兑换会员</div>
          <div class="redeem-header">
            <div class="plan-icon">
              <CrownOutlined />
            </div>
            <h2 class="plan-name">永久会员</h2>
            <p class="redeem-desc">输入兑换码，立即升级为永久会员</p>
          </div>

          <div class="redeem-divider"></div>

          <div class="redeem-features">
            <div v-for="(item, index) in redeemFeatures" :key="index" class="redeem-feature">
              <CheckCircleOutlined class="feature-check" />
              <span>{{ item }}</span>
            </div>
          </div>

          <div class="redeem-input-section">
            <a-input
              v-model:value="redeemCode"
              placeholder="请输入兑换码"
              size="large"
              class="redeem-input"
              :disabled="isVip"
            />
            <a-button
              type="primary"
              size="large"
              :loading="redeeming"
              :disabled="isVip || !redeemCode.trim()"
              @click="handleRedeem"
              class="redeem-btn"
            >
              <template #icon>
                <ThunderboltOutlined />
              </template>
              {{ isVip ? '您已是永久会员' : '立即兑换' }}
            </a-button>
          </div>

          <div class="redeem-notice">
            <SafetyOutlined />
            <span>兑换码请向管理员获取</span>
          </div>
        </div>

        <!-- 右侧：会员特权 -->
        <div class="features-section">
          <h3 class="features-title">
            <GiftOutlined />
            会员特权
          </h3>
          <div class="features-grid">
            <div v-for="(feature, index) in features" :key="index" class="feature-card">
              <div class="feature-icon-wrapper">
                <component :is="feature.icon" class="feature-icon" />
              </div>
              <div class="feature-content">
                <h4 class="feature-title">{{ feature.title }}</h4>
                <p class="feature-desc">{{ feature.desc }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 常见问题 -->
      <div class="faq-section">
        <div class="section-header">
          <QuestionCircleOutlined class="section-icon" />
          <h2 class="section-title">常见问题</h2>
        </div>
        <div class="faq-grid">
          <div v-for="(faq, index) in faqs" :key="index" class="faq-card">
            <h4 class="faq-question">{{ faq.question }}</h4>
            <p class="faq-answer">{{ faq.answer }}</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  CheckCircleOutlined,
  CrownOutlined,
  SafetyOutlined,
  ThunderboltOutlined,
  RocketOutlined,
  PictureOutlined,
  AppstoreOutlined,
  EditOutlined,
  StarOutlined,
  GiftOutlined,
  QuestionCircleOutlined
} from '@ant-design/icons-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { redeemCode as redeemCodeApi } from '@/api/redemptionController'
import { isVip as checkIsVip } from '@/utils/permission'

const router = useRouter()
const loginUserStore = useLoginUserStore()
const redeeming = ref(false)
const redeemCodeValue = ref('')

// 是否是 VIP（管理员也视为 VIP）
const isVip = computed(() => checkIsVip(loginUserStore.loginUser))

// 兑换码双向绑定
const redeemCode = computed({
  get: () => redeemCodeValue.value,
  set: (val: string) => {
    redeemCodeValue.value = val.toUpperCase()
  }
})

// 兑换特性列表
const redeemFeatures = [
  '无限创作配额',
  '全部高级配图功能',
  'AI 大纲智能编辑',
  '优先生成队列',
  '终身有效'
]

// 会员特权列表
const features = [
  {
    icon: RocketOutlined,
    title: '无限创作配额',
    desc: '无限次使用文章创作功能，告别配额限制'
  },
  {
    icon: PictureOutlined,
    title: 'AI 智能生图',
    desc: '使用 Nano Banana AI 生成独特配图'
  },
  {
    icon: AppstoreOutlined,
    title: 'SVG 图表生成',
    desc: '自动生成精美的概念示意图和思维导图'
  },
  {
    icon: EditOutlined,
    title: 'AI 大纲编辑',
    desc: '使用 AI 助手快速优化文章大纲'
  },
  {
    icon: StarOutlined,
    title: '优先队列',
    desc: '享受更快的生成速度和优先服务'
  },
  {
    icon: GiftOutlined,
    title: '终身有效',
    desc: '一次兑换，永久使用，无需续费'
  }
]

// FAQ 列表
const faqs = [
  {
    question: '兑换后多久生效？',
    answer: '兑换成功后立即生效，您将立即获得永久会员权限，刷新页面即可看到变化。'
  },
  {
    question: '如何获取兑换码？',
    answer: '请联系管理员获取兑换码，或关注我们的官方活动获取免费兑换码。'
  },
  {
    question: '会员是否需要续费？',
    answer: '不需要。永久会员一次兑换，终身有效，无需任何续费。'
  },
  {
    question: '兑换码可以重复使用吗？',
    answer: '每个兑换码只能使用一次，兑换后即失效。'
  }
]

// 兑换处理
const handleRedeem = async () => {
  if (!loginUserStore.loginUser.id) {
    message.warning('请先登录')
    router.push('/user/login')
    return
  }

  if (!redeemCodeValue.value.trim()) {
    message.warning('请输入兑换码')
    return
  }

  redeeming.value = true
  try {
    const res = await redeemCodeApi({ code: redeemCodeValue.value.trim() })
    if (res.data.code === 0 && res.data.data) {
      // 兑换成功，刷新用户信息
      await loginUserStore.fetchLoginUser()
      message.success('兑换成功！您已成为永久会员')
      redeemCodeValue.value = ''
    } else {
      message.error(res.data.message || '兑换失败，请检查兑换码是否正确')
    }
  } catch (error) {
    console.error('兑换失败:', error)
    message.error('兑换失败，请检查兑换码是否正确')
  } finally {
    redeeming.value = false
  }
}
</script>

<style scoped lang="scss">
.vip-page {
  min-height: calc(100vh - 64px);
  background: var(--gradient-hero);
  padding: 48px 24px 80px;
}

.vip-container {
  max-width: 1200px;
  margin: 0 auto;
}

/* 页面头部 */
.page-header {
  text-align: center;
  margin-bottom: 48px;
}

.header-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: rgba(34, 197, 94, 0.1);
  border: 1px solid rgba(34, 197, 94, 0.2);
  border-radius: var(--radius-full);
  font-size: 13px;
  font-weight: 600;
  color: var(--color-primary-dark);
  margin-bottom: 20px;

  .anticon {
    font-size: 14px;
  }
}

.page-title {
  font-size: 36px;
  font-weight: 700;
  margin: 0 0 12px;
  color: var(--color-text);
  letter-spacing: -0.5px;
}

.page-subtitle {
  font-size: 16px;
  color: var(--color-text-secondary);
  margin: 0;
}

/* 主内容区 */
.main-section {
  display: grid;
  grid-template-columns: 400px 1fr;
  gap: 32px;
  margin-bottom: 56px;
}

/* 兑换码卡片 */
.redeem-card {
  background: white;
  border-radius: var(--radius-xl);
  padding: 36px 32px;
  box-shadow: var(--shadow-xl);
  border: 2px solid var(--color-primary);
  position: relative;
  height: fit-content;
  position: sticky;
  top: 88px;
}

.redeem-badge {
  position: absolute;
  top: -12px;
  left: 50%;
  transform: translateX(-50%);
  background: var(--gradient-primary);
  color: white;
  padding: 6px 20px;
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: 600;
  box-shadow: var(--shadow-green);
}

.redeem-header {
  text-align: center;
  padding-bottom: 20px;
}

.plan-icon {
  width: 52px;
  height: 52px;
  margin: 0 auto 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(34, 197, 94, 0.1);
  border-radius: var(--radius-lg);

  .anticon {
    font-size: 26px;
    color: var(--color-primary);
  }
}

.plan-name {
  font-size: 20px;
  font-weight: 700;
  margin: 0 0 8px;
  color: var(--color-text);
}

.redeem-desc {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin: 0;
}

.redeem-divider {
  height: 1px;
  background: var(--color-border-light);
  margin: 20px 0;
}

.redeem-features {
  margin-bottom: 24px;
}

.redeem-feature {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  font-size: 14px;
  color: var(--color-text);

  .feature-check {
    color: var(--color-primary);
    font-size: 15px;
    flex-shrink: 0;
  }
}

/* 兑换码输入区域 */
.redeem-input-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.redeem-input {
  font-size: 15px;
  border-radius: var(--radius-md);
  text-transform: uppercase;
  letter-spacing: 1px;

  &:focus {
    border-color: var(--color-primary);
    box-shadow: 0 0 0 3px rgba(34, 197, 94, 0.1);
  }
}

.redeem-btn {
  width: 100%;
  height: 48px;
  font-size: 15px;
  font-weight: 600;
  background: var(--gradient-primary) !important;
  border: none !important;
  box-shadow: var(--shadow-green) !important;
  border-radius: var(--radius-md) !important;

  &:hover:not(:disabled) {
    opacity: 0.9;
    transform: translateY(-1px);
  }

  &:disabled {
    background: var(--color-background-tertiary) !important;
    color: var(--color-text-secondary) !important;
    box-shadow: none !important;
  }
}

.redeem-notice {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 14px;
  font-size: 12px;
  color: var(--color-text-secondary);

  .anticon {
    color: var(--color-primary);
    font-size: 13px;
  }
}

/* 会员特权 */
.features-section {
  background: white;
  border-radius: var(--radius-xl);
  padding: 32px;
  border: 1px solid var(--color-border);
}

.features-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 700;
  margin: 0 0 24px;
  color: var(--color-text);

  .anticon {
    color: var(--color-primary);
    font-size: 20px;
  }
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.feature-card {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 20px;
  background: var(--color-background-secondary);
  border-radius: var(--radius-lg);
  transition: all var(--transition-normal);

  &:hover {
    background: rgba(34, 197, 94, 0.06);
  }
}

.feature-icon-wrapper {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(34, 197, 94, 0.1);
  border-radius: var(--radius-md);
}

.feature-icon {
  font-size: 18px;
  color: var(--color-primary);
}

.feature-content {
  flex: 1;
  min-width: 0;
}

.feature-title {
  font-size: 14px;
  font-weight: 600;
  margin: 0 0 4px;
  color: var(--color-text);
}

.feature-desc {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin: 0;
  line-height: 1.5;
}

/* FAQ 部分 */
.faq-section {
  background: white;
  border-radius: var(--radius-xl);
  padding: 32px;
  border: 1px solid var(--color-border);
}

.section-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 24px;
}

.section-icon {
  font-size: 20px;
  color: var(--color-primary);
}

.section-title {
  font-size: 18px;
  font-weight: 700;
  margin: 0;
  color: var(--color-text);
}

.faq-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.faq-card {
  padding: 20px;
  background: var(--color-background-secondary);
  border-radius: var(--radius-lg);
}

.faq-question {
  font-size: 14px;
  font-weight: 600;
  margin: 0 0 8px;
  color: var(--color-text);
}

.faq-answer {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin: 0;
  line-height: 1.6;
}

/* 响应式 */
@media (max-width: 992px) {
  .main-section {
    grid-template-columns: 1fr;
  }

  .redeem-card {
    position: static;
    max-width: 400px;
    margin: 0 auto;
  }

  .features-grid {
    grid-template-columns: 1fr;
  }

  .faq-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .vip-page {
    padding: 32px 16px 60px;
  }

  .page-title {
    font-size: 28px;
  }

  .page-subtitle {
    font-size: 14px;
  }

  .redeem-card {
    padding: 28px 24px;
  }

  .features-section,
  .faq-section {
    padding: 24px;
  }
}
</style>
