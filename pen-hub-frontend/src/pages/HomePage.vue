<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import ContentTypeSelector from '@/components/ContentTypeSelector.vue'
import {
  RocketOutlined,
  FileTextOutlined,
  OrderedListOutlined,
  EditOutlined,
  PictureOutlined,
  ThunderboltOutlined,
  ArrowDownOutlined,
} from '@ant-design/icons-vue'

const router = useRouter()
const loginUserStore = useLoginUserStore()
const topic = ref('')
const currentPage = ref(0)
const showContentTypeSelector = ref(false)

const scrollToFeatures = () => {
  document.querySelector('.features-section')?.scrollIntoView({ behavior: 'smooth' })
}

const scrollToPage = (page: number) => {
  if (page === 0) {
    window.scrollTo({ top: 0, behavior: 'smooth' })
  } else {
    scrollToFeatures()
  }
}

const handleScroll = () => {
  const scrollY = window.scrollY
  const halfViewport = window.innerHeight / 2
  currentPage.value = scrollY > halfViewport ? 1 : 0
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})

const goToCreate = () => {
  if (!loginUserStore.loginUser.id) {
    message.warning('请先登录后再进行创作')
    router.push('/user/login')
    return
  }
  showContentTypeSelector.value = true
}

const handleContentTypeSelect = (contentType: string) => {
  const query: Record<string, string> = { contentType }
  if (topic.value.trim()) {
    query.topic = topic.value
  }
  router.push({ path: '/create', query })
}

const features = [
  {
    icon: FileTextOutlined,
    title: '智能生成标题',
    description: 'AI 自动分析选题，生成吸引眼球的爆款标题',
    color: '#D97706'
  },
  {
    icon: OrderedListOutlined,
    title: '自动生成大纲',
    description: '智能规划文章结构，确保逻辑清晰完整',
    color: '#3B82F6'
  },
  {
    icon: EditOutlined,
    title: '流式生成正文',
    description: '实时展示创作过程，体验打字机般的流畅输出',
    color: '#8B5CF6'
  },
  {
    icon: PictureOutlined,
    title: '智能配图',
    description: '自动检索高质量无版权图片，完美匹配内容',
    color: '#F59E0B'
  },
  {
    icon: ThunderboltOutlined,
    title: '快速高效',
    description: '5-10分钟完成全文创作，效率提升10倍',
    color: '#EF4444'
  },
  {
    icon: RocketOutlined,
    title: '多平台适配',
    description: '支持公众号、小红书、抖音等多平台风格',
    color: '#06B6D4'
  }
]
</script>

<template>
  <div id="homePage">
    <!-- 上半部分：AI 驱动的内容创作平台 -->
    <section class="hero-section">
      <div class="hero-content">
        <div class="hero-badge">
          <ThunderboltOutlined />
          <span>AI 驱动的内容创作平台</span>
        </div>
        <h1 class="hero-title">笔枢</h1>
        <p class="hero-subtitle">让每个人都能写出 10万+ 文章</p>

        <div class="hero-input-wrapper">
          <a-input
            v-model:value="topic"
            placeholder="输入您想创作的文章选题，例如：2026年AI如何改变职场"
            size="large"
            class="hero-input"
            @pressEnter="goToCreate"
          >
            <template #prefix>
              <EditOutlined class="input-icon" />
            </template>
          </a-input>
          <a-button type="primary" size="large" @click="goToCreate" class="hero-btn">
            <RocketOutlined />
            开始创作
          </a-button>
        </div>

        <p class="hero-tip">工作总结、心得体会、演讲稿、分析报告... 一键生成</p>
      </div>

      <!-- 向下滚动提示 -->
      <div class="scroll-hint" @click="scrollToFeatures">
        <span class="scroll-text">向下滚动探索更多</span>
        <div class="scroll-arrow">
          <ArrowDownOutlined />
        </div>
      </div>
    </section>

    <!-- 右侧页面指示器 -->
    <div class="page-indicator">
      <div
        v-for="page in 2"
        :key="page"
        :class="['indicator-dot', { active: currentPage === page - 1 }]"
        @click="scrollToPage(page - 1)"
      >
        <span class="dot-label">{{ String(page).padStart(2, '0') }}</span>
      </div>
    </div>

    <!-- 下半部分：核心能力 -->
    <section class="features-section">
      <div class="section-container">
        <div class="section-header">
          <span class="section-badge">核心能力</span>
          <h2 class="section-title">专业人士的一站式AI写作工具</h2>
          <p class="section-subtitle">强大的 AI 能力，让创作变得简单高效</p>
        </div>

        <div class="features-grid">
          <div
            v-for="(feature, index) in features"
            :key="index"
            class="feature-card"
          >
            <div class="feature-icon" :style="{ background: `${feature.color}15`, color: feature.color }">
              <component :is="feature.icon" />
            </div>
            <div class="feature-info">
              <h3 class="feature-title">{{ feature.title }}</h3>
              <p class="feature-desc">{{ feature.description }}</p>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>

  <ContentTypeSelector
    v-model:open="showContentTypeSelector"
    @select="handleContentTypeSelect"
  />
</template>

<style scoped>
#homePage {
  width: 100%;
}

/* ========== 上半部分：Hero ========== */
.hero-section {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: calc(100vh - 64px);
  padding: 24px;
  background: var(--gradient-hero);
  overflow: hidden;
}

.hero-content {
  position: relative;
  z-index: 1;
  text-align: center;
  max-width: 720px;
  width: 100%;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 20px;
  background: rgba(217, 119, 6, 0.1);
  border: 1px solid rgba(217, 119, 6, 0.2);
  border-radius: var(--radius-full);
  font-size: 14px;
  font-weight: 500;
  color: var(--color-primary-dark);
  margin-bottom: 28px;
}

.hero-title {
  font-size: 56px;
  font-weight: 700;
  margin: 0 0 16px;
  letter-spacing: -1.5px;
  line-height: 1.1;
  background: linear-gradient(135deg, var(--color-primary-dark) 0%, var(--color-primary) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-subtitle {
  font-size: 22px;
  margin: 0 0 48px;
  color: var(--color-text-secondary);
  font-weight: 400;
}

.hero-input-wrapper {
  display: flex;
  gap: 12px;
  max-width: 640px;
  margin: 0 auto 20px;
  padding: 8px;
  background: white;
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-lg);
  border: 1px solid var(--color-border);
}

.hero-input {
  flex: 1;
  border: none !important;
  box-shadow: none !important;
  font-size: 16px;
  padding: 8px 16px;
  background: transparent !important;
}

.hero-input:focus {
  box-shadow: none !important;
}

.input-icon {
  color: var(--color-text-muted);
  font-size: 18px;
}

.hero-btn {
  height: 52px !important;
  padding: 0 32px !important;
  font-size: 16px !important;
  font-weight: 600 !important;
  border-radius: var(--radius-lg) !important;
  background: var(--gradient-primary) !important;
  border: none !important;
  color: white !important;
  box-shadow: var(--shadow-warm) !important;
  display: flex !important;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
}

.hero-btn:hover {
  opacity: 0.92 !important;
}

.hero-tip {
  font-size: 14px;
  color: var(--color-text-muted);
  margin: 0;
}

/* 向下滚动提示 */
.scroll-hint {
  position: absolute;
  bottom: 48px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  z-index: 2;
  animation: float 2s ease-in-out infinite;
  color: var(--color-text-muted);
}

.scroll-text {
  font-size: 12px;
  letter-spacing: 1px;
  text-transform: uppercase;
}

.scroll-arrow {
  font-size: 20px;
}

@keyframes float {
  0%, 100% { transform: translateX(-50%) translateY(0); }
  50% { transform: translateX(-50%) translateY(-8px); }
}

/* 右侧页面指示器 */
.page-indicator {
  position: fixed;
  right: 32px;
  top: 50%;
  transform: translateY(-50%);
  z-index: 50;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.indicator-dot {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all var(--transition-fast);
  background: rgba(255, 255, 255, 0.8);
}

.indicator-dot:hover {
  border-color: var(--color-primary);
  background: rgba(217, 119, 6, 0.1);
}

.indicator-dot.active {
  border-color: var(--color-primary);
  background: var(--color-primary);
}

.indicator-dot.active .dot-label {
  color: white;
}

.dot-label {
  font-size: 10px;
  font-weight: 700;
  color: var(--color-text-muted);
  transition: color var(--transition-fast);
}

/* ========== 下半部分：核心能力 ========== */
.features-section {
  padding: 80px 24px 100px;
  background: var(--color-background);
}

.section-container {
  max-width: 1100px;
  margin: 0 auto;
}

.section-header {
  text-align: center;
  margin-bottom: 56px;
}

.section-badge {
  display: inline-block;
  padding: 6px 16px;
  background: rgba(217, 119, 6, 0.1);
  border-radius: var(--radius-full);
  font-size: 13px;
  font-weight: 600;
  color: var(--color-primary-dark);
  margin-bottom: 16px;
}

.section-title {
  font-size: 32px;
  font-weight: 700;
  margin: 0 0 12px;
  color: var(--color-text);
  letter-spacing: -0.5px;
}

.section-subtitle {
  font-size: 16px;
  color: var(--color-text-secondary);
  margin: 0;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

.feature-card {
  background: white;
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  padding: 32px 24px;
  transition: all var(--transition-normal);
}

.feature-card:hover {
  border-color: var(--color-primary-light);
  box-shadow: var(--shadow-card-hover);
  transform: translateY(-2px);
}

.feature-icon {
  width: 52px;
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md);
  font-size: 24px;
  margin-bottom: 20px;
}

.feature-info {
  min-width: 0;
}

.feature-title {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 8px;
  color: var(--color-text);
}

.feature-desc {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin: 0;
  line-height: 1.6;
}

/* ========== 响应式 ========== */
@media (max-width: 992px) {
  .features-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .hero-section {
    padding: 60px 20px 80px;
  }

  .hero-title {
    font-size: 36px;
  }

  .hero-subtitle {
    font-size: 16px;
  }

  .hero-input-wrapper {
    flex-direction: column;
    padding: 12px;
  }

  .hero-btn {
    width: 100%;
    justify-content: center;
  }

  .features-section {
    padding: 60px 20px 80px;
  }

  .features-grid {
    grid-template-columns: 1fr;
  }

  .section-title {
    font-size: 24px;
  }
}
</style>
