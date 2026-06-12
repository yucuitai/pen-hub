<template>
  <div class="template-page">
    <div class="page-header">
      <div class="header-container">
        <div>
          <h1 class="page-title">文案模板</h1>
          <p class="page-subtitle">选择模板快速开始创作</p>
        </div>
      </div>
    </div>

    <div class="container">
      <!-- 筛选栏 -->
      <div class="filter-section">
        <div class="filter-group">
          <span class="filter-label">平台：</span>
          <a-radio-group v-model:value="selectedPlatform" button-style="solid" class="filter-radio">
            <a-radio-button value="">全部</a-radio-button>
            <a-radio-button value="wechat">公众号</a-radio-button>
            <a-radio-button value="xiaohongshu">小红书</a-radio-button>
            <a-radio-button value="douyin">抖音</a-radio-button>
            <a-radio-button value="weibo">微博</a-radio-button>
          </a-radio-group>
        </div>
        <div class="filter-group">
          <span class="filter-label">分类：</span>
          <a-radio-group v-model:value="selectedCategory" button-style="solid" class="filter-radio">
            <a-radio-button value="">全部</a-radio-button>
            <a-radio-button value="marketing">营销推广</a-radio-button>
            <a-radio-button value="knowledge">知识科普</a-radio-button>
            <a-radio-button value="story">故事叙述</a-radio-button>
            <a-radio-button value="review">评测种草</a-radio-button>
          </a-radio-group>
        </div>
      </div>

      <!-- 模板列表 -->
      <a-spin :spinning="loading" tip="加载中...">
        <div v-if="templates.length > 0" class="template-grid">
          <div
            v-for="template in templates"
            :key="template.id"
            class="template-card"
            @click="useTemplate(template)"
          >
            <div class="card-header">
              <div class="card-tags">
                <a-tag v-if="template.platform" :color="getPlatformColor(template.platform)" class="platform-tag">
                  {{ getPlatformName(template.platform) }}
                </a-tag>
                <a-tag v-if="template.category" color="default" class="category-tag">
                  {{ getCategoryName(template.category) }}
                </a-tag>
              </div>
            </div>
            <h3 class="card-title">{{ template.name }}</h3>
            <p class="card-desc">{{ template.description }}</p>
            <div v-if="template.topicExample" class="card-example">
              <span class="example-label">示例选题：</span>
              <span class="example-text">{{ template.topicExample }}</span>
            </div>
            <div class="card-footer">
              <a-button type="primary" size="small" class="use-btn">
                使用此模板
              </a-button>
            </div>
          </div>
        </div>

        <a-empty v-else-if="!loading" description="暂无模板" />
      </a-spin>

      <!-- 分页 -->
      <div v-if="total > pageSize" class="pagination-section">
        <a-pagination
          v-model:current="pageNum"
          :total="total"
          :page-size="pageSize"
          show-less-items
          @change="loadTemplates"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { listTemplates } from '@/api/templateController'

const router = useRouter()

const loading = ref(false)
const templates = ref<API.Template[]>([])
const selectedPlatform = ref('')
const selectedCategory = ref('')
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)

// 加载模板列表
const loadTemplates = async () => {
  loading.value = true
  try {
    const res = await listTemplates({
      category: selectedCategory.value || undefined,
      platform: selectedPlatform.value || undefined,
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    })
    if (res.data.code === 0 && res.data.data) {
      templates.value = res.data.data.records || []
      total.value = res.data.data.totalRow || 0
    }
  } catch (error) {
    message.error('加载模板失败')
  } finally {
    loading.value = false
  }
}

// 使用模板（跳转到创作页）
const useTemplate = (template: API.Template) => {
  router.push({
    path: '/create',
    query: {
      topic: template.topicExample || '',
      style: template.style || '',
      templateName: template.name || '',
    },
  })
}

// 平台名称映射
const getPlatformName = (platform: string) => {
  const map: Record<string, string> = {
    wechat: '公众号',
    xiaohongshu: '小红书',
    douyin: '抖音',
    weibo: '微博',
  }
  return map[platform] || platform
}

// 平台颜色映射
const getPlatformColor = (platform: string) => {
  const map: Record<string, string> = {
    wechat: 'green',
    xiaohongshu: 'red',
    douyin: 'purple',
    weibo: 'orange',
  }
  return map[platform] || 'default'
}

// 分类名称映射
const getCategoryName = (category: string) => {
  const map: Record<string, string> = {
    marketing: '营销推广',
    knowledge: '知识科普',
    story: '故事叙述',
    review: '评测种草',
  }
  return map[category] || category
}

// 筛选条件变化时重新加载
watch([selectedPlatform, selectedCategory], () => {
  pageNum.value = 1
  loadTemplates()
})

onMounted(() => {
  loadTemplates()
})
</script>

<style scoped lang="scss">
.template-page {
  background: var(--color-background-secondary);
  min-height: 100vh;
  padding-bottom: 60px;
}

.page-header {
  background: var(--gradient-hero);
  padding: 32px 20px;
  margin-bottom: 24px;
}

.header-container {
  max-width: 1200px;
  margin: 0 auto;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  margin: 0 0 6px;
  letter-spacing: -0.5px;
  color: var(--color-text);
}

.page-subtitle {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin: 0;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

/* 筛选栏 */
.filter-section {
  background: white;
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  padding: 20px;
  margin-bottom: 24px;
}

.filter-group {
  display: flex;
  align-items: center;
  margin-bottom: 12px;

  &:last-child {
    margin-bottom: 0;
  }
}

.filter-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
  margin-right: 12px;
  white-space: nowrap;
}

.filter-radio {
  :deep(.ant-radio-button-wrapper) {
    border-radius: var(--radius-md);
    margin-right: 8px;
    border-left: 1px solid var(--color-border);

    &:first-child {
      border-radius: var(--radius-md);
    }

    &::before {
      display: none;
    }
  }
}

/* 模板卡片网格 */
.template-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}

.template-card {
  background: white;
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  padding: 24px;
  cursor: pointer;
  transition: all var(--transition-normal);

  &:hover {
    border-color: var(--color-primary);
    box-shadow: var(--shadow-md);
    transform: translateY(-2px);
  }
}

.card-header {
  margin-bottom: 12px;
}

.card-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.platform-tag,
.category-tag {
  border-radius: var(--radius-full);
  font-size: 12px;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text);
  margin: 0 0 8px;
  line-height: 1.4;
}

.card-desc {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin: 0 0 12px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-example {
  font-size: 13px;
  color: var(--color-text-muted);
  margin-bottom: 16px;
  padding: 10px 12px;
  background: var(--color-background-secondary);
  border-radius: var(--radius-md);

  .example-label {
    font-weight: 600;
    color: var(--color-text-secondary);
  }

  .example-text {
    color: var(--color-text);
  }
}

.card-footer {
  display: flex;
  justify-content: flex-end;
}

.use-btn {
  background: var(--gradient-primary);
  border: none;
  border-radius: var(--radius-md);
  font-weight: 600;
}

/* 分页 */
.pagination-section {
  display: flex;
  justify-content: center;
  margin-top: 32px;
}

/* 响应式 */
@media (max-width: 768px) {
  .template-grid {
    grid-template-columns: 1fr;
  }

  .filter-group {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .page-title {
    font-size: 22px;
  }
}
</style>
