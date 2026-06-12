<template>
  <div class="batch-create-page">
    <div class="page-header">
      <h1>批量创作</h1>
      <p class="subtitle">每行一个选题，最多10个</p>
    </div>

    <a-card class="batch-card">
      <a-form layout="vertical">
        <a-form-item label="选题列表">
          <a-textarea v-model:value="topicsText" :auto-size="{ minRows: 8 }" placeholder="每行输入一个选题&#10;例如：&#10;如何高效学习编程&#10;2026年AI行业趋势分析&#10;职场新人必知的10个技巧" />
        </a-form-item>

        <a-form-item label="内容类型">
          <a-select v-model:value="contentType" style="width: 200px;">
            <a-select-option value="ARTICLE">新媒体文案</a-select-option>
            <a-select-option value="SHORT_VIDEO_SCRIPT">短视频脚本</a-select-option>
            <a-select-option value="LIVE_SCRIPT">直播台本</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="风格">
          <a-select v-model:value="style" style="width: 200px;" allowClear>
            <a-select-option value="tech">科技风</a-select-option>
            <a-select-option value="emotional">情感风</a-select-option>
            <a-select-option value="educational">教育风</a-select-option>
            <a-select-option value="humorous">幽默风</a-select-option>
          </a-select>
        </a-form-item>

        <a-button type="primary" size="large" :loading="loading" :disabled="!topicsText.trim()" @click="handleBatchCreate">
          批量创建 ({{ topicCount }} 篇)
        </a-button>
      </a-form>
    </a-card>

    <a-card v-if="createdTaskIds.length > 0" class="result-card" title="创建结果">
      <a-list :data-source="createdTaskIds">
        <template #renderItem="{ item, index }">
          <a-list-item>
            <a-list-item-meta :title="`任务 ${index + 1}`" :description="item" />
            <router-link :to="`/article/${item}`">查看</router-link>
          </a-list-item>
        </template>
      </a-list>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { message } from 'ant-design-vue'
import { batchCreateArticle } from '@/api/batchController'

const topicsText = ref('')
const contentType = ref('ARTICLE')
const style = ref<string | undefined>(undefined)
const loading = ref(false)
const createdTaskIds = ref<string[]>([])

const topicCount = computed(() => {
  return topicsText.value.split('\n').filter(t => t.trim()).length
})

const handleBatchCreate = async () => {
  const topics = topicsText.value.split('\n').filter(t => t.trim())
  if (topics.length === 0) { message.warning('请输入选题'); return }
  if (topics.length > 10) { message.warning('单次最多10篇'); return }

  loading.value = true
  try {
    const res = await batchCreateArticle({ topics, style: style.value, contentType: contentType.value })
    if (res.data.code === 0) {
      createdTaskIds.value = res.data.data || []
      message.success(`成功创建 ${createdTaskIds.value.length} 篇`)
    }
  } catch (e: any) {
    message.error(e?.response?.data?.message || '批量创建失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.batch-create-page { max-width: 800px; margin: 0 auto; padding: 24px; }
.page-header { margin-bottom: 24px; }
.page-header h1 { margin: 0 0 4px; font-size: 24px; font-weight: 700; }
.page-header .subtitle { color: var(--color-text-secondary); margin: 0; }
.batch-card { margin-bottom: 24px; border-radius: 12px; }
.result-card { border-radius: 12px; }
</style>
