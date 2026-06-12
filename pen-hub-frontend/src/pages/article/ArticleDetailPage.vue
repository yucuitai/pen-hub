<template>
  <div class="article-detail-page">
    <div class="page-header">
      <div class="header-container">
        <div class="header-actions">
          <a-button @click="goBack" class="back-btn">
            <template #icon>
              <ArrowLeftOutlined />
            </template>
            返回
          </a-button>
          <div class="right-actions">
            <a-button
              :class="['favorite-btn', { favorited: article?.isFavorited }]"
              @click="handleToggleFavorite"
            >
              <template #icon>
                <HeartFilled v-if="article?.isFavorited" />
                <HeartOutlined v-else />
              </template>
              {{ article?.isFavorited ? '已收藏' : '收藏' }}
            </a-button>
            <a-button
              v-if="article?.status === 'COMPLETED'"
              :type="isEditing ? 'default' : 'primary'"
              @click="toggleEditMode"
              class="edit-btn"
            >
              <template #icon>
                <EditOutlined v-if="!isEditing" />
                <EyeOutlined v-else />
              </template>
              {{ isEditing ? '预览' : '编辑' }}
            </a-button>
            <a-button
              v-if="article?.status === 'FAILED'"
              type="primary"
              danger
              @click="handleRetry"
              class="retry-btn"
            >
              <template #icon>
                <RedoOutlined />
              </template>
              重新创建
            </a-button>
            <a-dropdown>
              <a-button type="primary" class="export-btn">
                <template #icon>
                  <DownloadOutlined />
                </template>
                导出
                <DownOutlined />
              </a-button>
              <template #overlay>
                <a-menu @click="handleExport">
                  <a-menu-item key="markdown">
                    <FileTextOutlined /> 导出 Markdown
                  </a-menu-item>
                  <a-menu-item key="wechat-html">
                    <WechatOutlined /> 导出公众号 HTML
                  </a-menu-item>
                  <a-menu-item key="xiaohongshu-text">
                    <EditOutlined /> 导出小红书纯文本
                  </a-menu-item>
                  <a-menu-divider />
                  <a-menu-item key="pdf">
                    <FilePdfOutlined /> 导出 PDF
                  </a-menu-item>
                  <a-menu-item key="docx">
                    <FileWordOutlined /> 导出 Word
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </div>
        </div>
      </div>
    </div>

    <div class="container">
      <a-spin :spinning="loading" tip="加载中...">
        <a-card :bordered="false" v-if="article" class="article-card">
          <!-- 标题 -->
          <div class="title-section">
            <h1 class="main-title">{{ article.mainTitle }}</h1>
            <p class="sub-title">{{ article.subTitle }}</p>
            <div class="meta-info">
              <a-tag :color="getStatusColor(article.status ?? '')" class="status-tag">
                {{ getStatusText(article.status ?? '') }}
              </a-tag>
              <span class="time">创建于 {{ article.createTime ? formatDate(article.createTime) : '' }}</span>
            </div>

            <!-- 标签区域 -->
            <div class="tags-section">
              <div class="tags-header">
                <span class="tags-label">标签：</span>
                <a-button type="link" size="small" @click="showTagInput = true" v-if="!showTagInput">
                  <PlusOutlined /> 添加标签
                </a-button>
              </div>
              <div class="tags-content">
                <a-tag
                  v-for="(tag, index) in articleTags"
                  :key="index"
                  closable
                  @close="removeTag(index)"
                  class="article-tag"
                >
                  {{ tag }}
                </a-tag>
                <div v-if="showTagInput" class="tag-input-wrapper">
                  <a-input
                    v-model:value="newTagValue"
                    size="small"
                    placeholder="输入标签"
                    @press-enter="addTag"
                    @blur="addTag"
                    class="tag-input"
                    ref="tagInputRef"
                  />
                </div>
              </div>
            </div>
          </div>

          <a-divider />

          <!-- 执行日志面板 -->
          <div v-if="executionStats && executionStats.logs && executionStats.logs.length > 0" class="execution-logs-section">
            <div class="logs-header" @click="showExecutionLogs = !showExecutionLogs">
              <h2 class="section-title">
                <ClockCircleOutlined class="section-icon" />
                执行日志
                <a-tag :color="getStatusColor(executionStats.overallStatus ?? '')" class="status-tag-small">
                  {{ executionStats.overallStatus ?? '' }}
                </a-tag>
              </h2>
              <ThunderboltOutlined :class="['toggle-icon', { expanded: showExecutionLogs }]" />
            </div>

            <Transition name="expand">
              <div v-show="showExecutionLogs" class="logs-content">
                <!-- 统计概览 -->
                <div class="stats-summary">
                  <div class="stat-item">
                    <span class="label">总耗时</span>
                    <span class="value">{{ executionStats.totalDurationMs ?? 0 }}ms</span>
                  </div>
                  <div class="stat-item">
                    <span class="label">智能体数量</span>
                    <span class="value">{{ executionStats.agentCount ?? 0 }}</span>
                  </div>
                  <div class="stat-item">
                    <span class="label">平均耗时</span>
                    <span class="value">
                      {{ executionStats.agentCount && executionStats.totalDurationMs ? Math.round(executionStats.totalDurationMs / executionStats.agentCount) : 0 }}ms
                    </span>
                  </div>
                </div>

                <!-- 智能体时间线 -->
                <div class="agent-timeline">
                  <div
                    v-for="log in executionStats.logs"
                    :key="log.id"
                    :class="['timeline-item', log.status?.toLowerCase()]"
                  >
                    <div class="timeline-indicator">
                      <CheckCircleOutlined v-if="log.status === 'SUCCESS'" class="icon success" />
                      <CloseCircleOutlined v-else-if="log.status === 'FAILED'" class="icon failed" />
                      <LoadingOutlined v-else class="icon running" />
                    </div>
                    <div class="timeline-content">
                      <div class="timeline-header">
                        <span class="agent-name">{{ getAgentDisplayName(log.agentName ?? '') }}</span>
                        <span class="duration">{{ log.durationMs ?? 0 }}ms</span>
                      </div>
                      <div class="timeline-time">
                        {{ log.startTime ? formatDate(log.startTime) : '' }}
                      </div>
                      <div v-if="log.errorMessage" class="error-message">
                        <CloseCircleOutlined /> {{ log.errorMessage }}
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </Transition>
          </div>

          <a-divider v-if="executionStats && executionStats.logs && executionStats.logs.length > 0" />

          <!-- 质量评分 -->
          <div v-if="article.reviewScore != null" class="review-section">
            <h2 class="section-title">
              <StarOutlined class="section-icon" />
              内容质量评分
            </h2>
            <div class="review-score-card">
              <div class="score-circle" :style="{ borderColor: getScoreColor(article.reviewScore) }">
                <span class="score-number" :style="{ color: getScoreColor(article.reviewScore) }">{{ article.reviewScore }}</span>
                <span class="score-label">综合评分</span>
              </div>
              <div class="score-details">
                <div v-if="article.reviewSuggestions && article.reviewSuggestions.length > 0" class="suggestions">
                  <div class="suggestions-title">改进建议：</div>
                  <ul class="suggestions-list">
                    <li v-for="(suggestion, idx) in article.reviewSuggestions" :key="idx">{{ suggestion }}</li>
                  </ul>
                </div>
              </div>
            </div>
          </div>

          <a-divider v-if="article.reviewScore != null" />

          <!-- 大纲 -->
          <div v-if="article.outline && article.outline.length > 0" class="outline-section">
            <h2 class="section-title">
              <OrderedListOutlined class="section-icon" />
              文章大纲
            </h2>
            <div class="outline-list">
              <div v-for="item in article.outline" :key="item.section" class="outline-item">
                <div class="outline-title">{{ item.section }}. {{ item.title }}</div>
                <ul class="outline-points">
                  <li v-for="(point, idx) in item.points" :key="idx">{{ point }}</li>
                </ul>
              </div>
            </div>
          </div>

          <a-divider v-if="article.outline && article.outline.length > 0" />

          <!-- 编辑模式 -->
          <div v-if="isEditing" class="edit-mode-section">
            <div class="edit-toolbar">
              <a-button type="primary" @click="saveContent" :loading="saving">保存</a-button>
              <a-button @click="cancelEdit" style="margin-left: 8px;">取消</a-button>
              <a-button @click="toggleEditorMode" style="margin-left: 8px;">{{ editorMode === 'wysiwyg' ? '切换源码' : '切换富文本' }}</a-button>
            </div>
            <div v-if="editorMode === 'wysiwyg'" class="wysiwyg-container">
              <TiptapEditor v-model="editHtmlContent" />
            </div>
            <div v-else class="edit-container">
              <div class="edit-left">
                <div class="edit-label">Markdown 源码</div>
                <a-textarea v-model:value="editContent" :auto-size="{ minRows: 20 }" class="edit-textarea" />
              </div>
              <div class="edit-right">
                <div class="edit-label">实时预览</div>
                <div v-html="markdownToHtml(editContent)" class="markdown-content edit-preview"></div>
              </div>
            </div>
          </div>

          <!-- 完整图文（优先展示） -->
          <div v-else-if="article.fullContent" class="content-section" @click="handleContentClick">
            <h2 class="section-title">
              <FileTextOutlined class="section-icon" />
              完整图文
            </h2>
            <div v-html="markdownToHtml(article.fullContent)" class="markdown-content"></div>
          </div>

          <!-- 普通正文（无 fullContent 时展示） -->
          <div v-else-if="article.content" class="content-section" @click="handleContentClick">
            <h2 class="section-title">
              <FileTextOutlined class="section-icon" />
              文章正文
            </h2>
            <div v-html="markdownToHtml(article.content)" class="markdown-content"></div>
          </div>

          <!-- 配图（仅在没有 fullContent 时单独展示） -->
          <div v-if="!article.fullContent && article.images && article.images.length > 0" class="images-section">
            <h2 class="section-title">
              <PictureOutlined class="section-icon" />
              文章配图
            </h2>
            <div class="images-grid">
              <div v-for="image in article.images" :key="image.position" class="image-item">
                <a-image
                  :src="image.url"
                  :alt="image.description"
                  :preview="{ src: image.url }"
                  class="preview-image"
                />
                <div class="image-info">
                  <span class="badge">{{ image.method }}</span>
                  <span class="keywords">{{ image.keywords }}</span>
                </div>
              </div>
            </div>
          </div>
        </a-card>
      </a-spin>
    </div>

    <!-- 用户反馈区域 -->
    <div v-if="article?.status === 'COMPLETED'" class="feedback-section">
      <a-card :bordered="false" class="feedback-card">
        <div class="feedback-content">
          <div class="feedback-question">这篇文章对您有帮助吗？</div>
          <div class="feedback-actions">
            <a-button
              :type="myFeedback?.rating === 1 ? 'primary' : 'default'"
              @click="submitFeedbackRating(1)"
              :loading="feedbackLoading"
            >
              👍 有帮助
            </a-button>
            <a-button
              :type="myFeedback?.rating === 0 ? 'primary' : 'default'"
              danger
              @click="submitFeedbackRating(0)"
              :loading="feedbackLoading"
              style="margin-left: 12px;"
            >
              👎 需改进
            </a-button>
          </div>
          <div v-if="feedbackStats && feedbackStats.total > 0" class="feedback-stats">
            {{ feedbackStats.total }} 人反馈 · {{ Math.round((feedbackStats.satisfied / feedbackStats.total) * 100) }}% 满意
          </div>
        </div>
      </a-card>
    </div>

    <!-- 图片预览 Modal -->
    <a-modal
      v-model:open="previewVisible"
      :footer="null"
      :width="'80vw'"
      :bodyStyle="{ padding: 0, textAlign: 'center' }"
      :centered="true"
      :zIndex="1000"
    >
      <img :src="previewImageUrl" alt="预览图片" style="max-width: 100%; max-height: 80vh; object-fit: contain;" />
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  ArrowLeftOutlined,
  DownloadOutlined,
  OrderedListOutlined,
  FileTextOutlined,
  PictureOutlined,
  ClockCircleOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  LoadingOutlined,
  RedoOutlined,
  ThunderboltOutlined,
  HeartOutlined,
  HeartFilled,
  EyeOutlined,
  StarOutlined,
  DownOutlined,
  WechatOutlined,
  EditOutlined,
  PlusOutlined,
  FilePdfOutlined,
  FileWordOutlined
} from '@ant-design/icons-vue'
import { getArticle, getExecutionLogs, toggleFavorite, updateArticleTags, exportArticle, updateArticleContent } from '@/api/articleController'
import { submitFeedback, getFeedbackStats, getMyFeedback } from '@/api/feedbackController'
import TiptapEditor from '@/components/TiptapEditor.vue'
import { marked } from 'marked'
import dayjs from 'dayjs'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const article = ref<API.ArticleVO | null>(null)
const executionStats = ref<API.AgentExecutionStats | null>(null)
const logsLoading = ref(false)
const showExecutionLogs = ref(false)

// 标签相关
const articleTags = ref<string[]>([])
const showTagInput = ref(false)
const newTagValue = ref('')
const tagInputRef = ref()

// 编辑模式相关
const isEditing = ref(false)
const editContent = ref('')
const editHtmlContent = ref('')
const editorMode = ref<'wysiwyg' | 'source'>('wysiwyg')

const toggleEditorMode = () => {
  editorMode.value = editorMode.value === 'wysiwyg' ? 'source' : 'wysiwyg'
}
const saving = ref(false)

// 图片预览相关
const previewVisible = ref(false)
const previewImageUrl = ref('')

// 反馈相关
const feedbackLoading = ref(false)
const myFeedback = ref<API.ArticleFeedback | null>(null)
const feedbackStats = ref<{ satisfied: number; unsatisfied: number; total: number } | null>(null)

// Markdown 转 HTML
const markdownToHtml = (markdown: string) => {
  return marked(markdown)
}

// 加载文章
const loadArticle = async () => {
  const taskId = route.params.taskId as string
  if (!taskId) {
    message.error('文章ID不存在')
    return
  }

  loading.value = true
  try {
    const res = await getArticle({ taskId })
    article.value = res.data.data || null
    // 解析标签
    if (article.value?.tags) {
      try {
        articleTags.value = typeof article.value.tags === 'string'
          ? JSON.parse(article.value.tags)
          : article.value.tags || []
      } catch {
        articleTags.value = []
      }
    }
    // 自动加载执行日志
    await loadExecutionLogs(taskId)
    // 加载反馈数据
    await loadFeedbackData(taskId)
  } catch (error) {
    message.error((error as Error).message || '加载失败')
  } finally {
    loading.value = false
  }
}

// 切换收藏状态
const handleToggleFavorite = async () => {
  if (!article.value?.taskId) return
  try {
    const res = await toggleFavorite(article.value.taskId)
    if (res.data.code === 0) {
      article.value.isFavorited = res.data.data ? 1 : 0
      message.success(res.data.data ? '已收藏' : '已取消收藏')
    }
  } catch (error) {
    message.error('操作失败')
  }
}

// 添加标签
const addTag = async () => {
  const value = newTagValue.value.trim()
  if (!value) {
    showTagInput.value = false
    return
  }
  if (articleTags.value.includes(value)) {
    message.warning('标签已存在')
    newTagValue.value = ''
    return
  }
  const newTags = [...articleTags.value, value]
  try {
    await updateArticleTags(article.value?.taskId || '', newTags)
    articleTags.value = newTags
    newTagValue.value = ''
    showTagInput.value = false
    message.success('标签已添加')
  } catch (error) {
    message.error('添加标签失败')
  }
}

// 删除标签
const removeTag = async (index: number) => {
  const newTags = articleTags.value.filter((_, i) => i !== index)
  try {
    await updateArticleTags(article.value?.taskId || '', newTags)
    articleTags.value = newTags
    message.success('标签已删除')
  } catch (error) {
    message.error('删除标签失败')
  }
}

// 编辑模式
const toggleEditMode = () => {
  if (isEditing.value) {
    isEditing.value = false
    return
  }
  editContent.value = article.value?.fullContent || article.value?.content || ''
  isEditing.value = true
}

const cancelEdit = () => {
  isEditing.value = false
  editContent.value = ''
}

const saveContent = async () => {
  if (!article.value?.taskId) return
  saving.value = true
  try {
    await updateArticleContent(article.value.taskId, editContent.value)
    article.value.content = editContent.value
    article.value.fullContent = editContent.value
    isEditing.value = false
    message.success('保存成功')
  } catch (error) {
    message.error('保存失败')
  } finally {
    saving.value = false
  }
}

// 质量评分颜色
const getScoreColor = (score: number) => {
  if (score >= 80) return '#52c41a'
  if (score >= 60) return '#faad14'
  return '#ff4d4f'
}

// 加载反馈数据
const loadFeedbackData = async (taskId: string) => {
  try {
    const [statsRes, myRes] = await Promise.all([
      getFeedbackStats(taskId),
      getMyFeedback(taskId),
    ])
    if (statsRes.data.code === 0 && statsRes.data.data) {
      feedbackStats.value = statsRes.data.data as { satisfied: number; unsatisfied: number; total: number }
    }
    if (myRes.data.code === 0 && myRes.data.data) {
      myFeedback.value = myRes.data.data
    }
  } catch {
    // 反馈加载失败不影响主流程
  }
}

// 提交反馈
const submitFeedbackRating = async (rating: number) => {
  if (!article.value?.taskId) return
  feedbackLoading.value = true
  try {
    const res = await submitFeedback({ taskId: article.value.taskId, rating })
    if (res.data.code === 0) {
      myFeedback.value = { ...myFeedback.value, rating } as API.ArticleFeedback
      message.success(rating === 1 ? '感谢您的正面反馈！' : '感谢反馈，我们会持续改进')
      // 刷新统计
      const statsRes = await getFeedbackStats(article.value.taskId)
      if (statsRes.data.code === 0 && statsRes.data.data) {
        feedbackStats.value = statsRes.data.data as { satisfied: number; unsatisfied: number; total: number }
      }
    }
  } catch {
    message.error('反馈提交失败')
  } finally {
    feedbackLoading.value = false
  }
}

// 导出处理
const handleExport = async ({ key }: { key: string }) => {
  if (!article.value) return
  switch (key) {
    case 'markdown':
      exportMarkdown()
      break
    case 'wechat-html':
      exportWechatHtml()
      break
    case 'xiaohongshu-text':
      exportXiaohongshuText()
      break
    case 'pdf':
    case 'docx':
      await exportServerFile(key as 'pdf' | 'docx')
      break
  }
}

// 服务器端导出（PDF/Word）
const exportServerFile = async (format: 'pdf' | 'docx') => {
  if (!article.value?.taskId) return
  try {
    message.loading(`正在生成 ${format.toUpperCase()} 文件...`, 0)
    const res = await exportArticle(article.value.taskId, format)
    // 下载文件
    const blob = new Blob([res.data], {
      type: format === 'pdf' ? 'application/pdf' : 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
    })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${article.value.mainTitle}.${format}`
    a.click()
    URL.revokeObjectURL(url)
    message.destroy()
    message.success(`导出 ${format.toUpperCase()} 成功`)
  } catch (error) {
    message.destroy()
    message.error(`导出 ${format.toUpperCase()} 失败`)
  }
}

// 导出 Markdown（原有逻辑）
const exportMarkdown = () => {
  if (!article.value) return

  let markdown = `# ${article.value.mainTitle}\n\n`
  markdown += `> ${article.value.subTitle}\n\n`

  if (article.value.fullContent) {
    markdown += article.value.fullContent
  } else {
    if (article.value.outline && article.value.outline.length > 0) {
      markdown += `## 目录\n\n`
      article.value.outline.forEach(item => {
        markdown += `${item.section}. ${item.title}\n`
      })
      markdown += `\n---\n\n`
    }
    markdown += article.value.content || ''
    if (article.value.images && article.value.images.length > 0) {
      markdown += `\n\n## 配图\n\n`
      article.value.images.forEach(image => {
        markdown += `![${image.description}](${image.url})\n\n`
      })
    }
  }

  downloadFile(`${article.value.mainTitle}.md`, markdown, 'text/markdown')
  message.success('导出 Markdown 成功')
}

// HTML 转义函数
const escapeHtml = (text: string) => {
  if (!text) return ''
  return text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;')
}

// 导出公众号 HTML
const exportWechatHtml = () => {
  if (!article.value) return

  const content = article.value.fullContent || article.value.content || ''
  const htmlContent = `<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>${escapeHtml(article.value.mainTitle || '')}</title>
  <style>
    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; max-width: 677px; margin: 0 auto; padding: 20px; color: #333; line-height: 1.8; }
    h1 { font-size: 22px; font-weight: 700; text-align: center; margin-bottom: 10px; }
    h2 { font-size: 18px; font-weight: 700; margin: 24px 0 12px; border-bottom: 1px solid #eee; padding-bottom: 8px; }
    h3 { font-size: 16px; font-weight: 600; margin: 18px 0 8px; }
    p { margin-bottom: 14px; text-indent: 0; }
    img { max-width: 100%; display: block; margin: 16px auto; border-radius: 4px; }
    blockquote { border-left: 3px solid #D97706; padding-left: 12px; color: #666; margin: 16px 0; }
    ul, ol { padding-left: 2em; margin-bottom: 14px; }
    li { margin-bottom: 6px; }
    code { background: #f5f5f5; padding: 2px 6px; border-radius: 3px; font-size: 13px; }
    pre { background: #1e1e1e; color: #d4d4d4; padding: 16px; border-radius: 4px; overflow-x: auto; }
    pre code { background: transparent; color: inherit; }
  /* 编辑模式样式 */
.edit-mode-section {
  margin-bottom: 24px;
}

.edit-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 16px;
}

.edit-container {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.edit-label {
  font-weight: 600;
  margin-bottom: 8px;
  color: var(--color-text);
}

.edit-textarea {
  font-family: 'Fira Code', 'Consolas', monospace;
  font-size: 14px;
}

.edit-preview {
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 16px;
  min-height: 400px;
  max-height: 600px;
  overflow-y: auto;
  background: var(--color-background);
}

@media (max-width: 768px) {
  .edit-container {
    grid-template-columns: 1fr;
  }
}

/* 质量评分样式 */
.review-section {
  margin-bottom: 24px;
}

.review-score-card {
  display: flex;
  align-items: flex-start;
  gap: 32px;
  padding: 20px;
  background: var(--color-background);
  border-radius: 12px;
  border: 1px solid var(--color-border);
}

.score-circle {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100px;
  height: 100px;
  border-radius: 50%;
  border: 4px solid;
  flex-shrink: 0;
}

.score-number {
  font-size: 32px;
  font-weight: 700;
  line-height: 1;
}

.score-label {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-top: 4px;
}

.score-details {
  flex: 1;
}

.suggestions-title {
  font-weight: 600;
  margin-bottom: 8px;
  color: var(--color-text);
}

.suggestions-list {
  padding-left: 20px;
  margin: 0;
}

.suggestions-list li {
  margin-bottom: 6px;
  color: var(--color-text-secondary);
  line-height: 1.6;
}

</style>
</head>
<body>
  <h1>${escapeHtml(article.value.mainTitle || '')}</h1>
  <blockquote>${escapeHtml(article.value.subTitle || '')}</blockquote>
  ${markdownToHtml(content)}
</body>
</html>`

  downloadFile(`${article.value.mainTitle}_公众号.html`, htmlContent, 'text/html')
  message.success('导出公众号 HTML 成功')
}

// 导出小红书纯文本
const exportXiaohongshuText = () => {
  if (!article.value) return

  const content = article.value.fullContent || article.value.content || ''
  // 移除 Markdown 图片语法，保留纯文本
  const plainText = content
    .replace(/!\[.*?\]\(.*?\)/g, '')
    .replace(/#{1,6}\s/g, '')
    .replace(/\*\*(.*?)\*\*/g, '$1')
    .replace(/\*(.*?)\*/g, '$1')
    .replace(/`(.*?)`/g, '$1')
    .replace(/```[\s\S]*?```/g, '')
    .replace(/\[([^\]]+)\]\([^)]+\)/g, '$1')
    .replace(/^\s*[-*+]\s/gm, '• ')
    .replace(/^\s*\d+\.\s/gm, '')
    .replace(/\n{3,}/g, '\n\n')
    .trim()

  const xiaohongshuText = `📝 ${article.value.mainTitle}\n\n${plainText}\n\n#${article.value.topic || '分享'}`

  downloadFile(`${article.value.mainTitle}_小红书.txt`, xiaohongshuText, 'text/plain')
  message.success('导出小红书纯文本成功')
}

// 文件下载工具函数
const downloadFile = (filename: string, content: string, mimeType: string) => {
  const blob = new Blob([content], { type: mimeType })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}

// 处理 Markdown 内容中图片点击
const handleContentClick = (event: MouseEvent) => {
  const target = event.target as HTMLElement
  if (target.tagName === 'IMG' && target.closest('.markdown-content')) {
    const imgSrc = (target as HTMLImageElement).src
    if (imgSrc) {
      previewImageUrl.value = imgSrc
      previewVisible.value = true
    }
  }
}

// 加载执行日志
const loadExecutionLogs = async (taskId: string) => {
  logsLoading.value = true
  try {
    const res = await getExecutionLogs({ taskId })
    executionStats.value = res.data.data || null
  } catch (error) {
    console.error('加载执行日志失败:', error)
  } finally {
    logsLoading.value = false
  }
}

// 返回
const goBack = () => {
  router.back()
}

// 格式化日期
const formatDate = (date: string) => {
  return dayjs(date).format('YYYY-MM-DD HH:mm:ss')
}

// 获取状态颜色
const getStatusColor = (status: string) => {
  const colorMap: Record<string, string> = {
    PENDING: 'default',
    PROCESSING: 'processing',
    COMPLETED: 'success',
    FAILED: 'error',
  }
  return colorMap[status] || 'default'
}

// 获取状态文本
const getStatusText = (status: string) => {
  const textMap: Record<string, string> = {
    PENDING: '等待中',
    PROCESSING: '生成中',
    COMPLETED: '已完成',
    FAILED: '失败',
  }
  return textMap[status] || status
}

// 获取智能体显示名称
const getAgentDisplayName = (agentName: string) => {
  const nameMap: Record<string, string> = {
    'agent1_generate_titles': '生成标题',
    'agent2_generate_outline': '生成大纲',
    'agent3_generate_content': '生成正文',
    'agent4_analyze_image_requirements': '分析配图需求',
    'agent5_generate_images': '生成配图',
    'agent6_merge_content': '图文合成',
    'ai_modify_outline': 'AI修改大纲'
  }
  return nameMap[agentName] || agentName
}

// 重试（重新创建文章）
const handleRetry = () => {
  if (!article.value) return

  Modal.confirm({
    title: '确认重试',
    content: '将使用相同的选题和配置重新创建文章，是否继续？',
    okText: '确认',
    cancelText: '取消',
    onOk: () => {
      router.push({
        path: '/create',
        query: {
          topic: article.value?.topic
        }
      })
    }
  })
}

onMounted(() => {
  loadArticle()
})
</script>

<style scoped lang="scss">
.article-detail-page {
  background: var(--color-background-secondary);
  min-height: 100vh;
  padding-bottom: 60px;

  .page-header {
    background: var(--gradient-hero);
    padding: 20px;
    margin-bottom: 24px;
  }

  .header-container {
    max-width: 1200px;
    margin: 0 auto;
  }

  .header-actions {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .right-actions {
    display: flex;
    gap: 12px;
  }

  .back-btn {
    background: white;
    border: 1px solid var(--color-border);
    color: var(--color-text);
    font-size: 13px;
    transition: all var(--transition-fast);
    border-radius: var(--radius-md);

    &:hover {
      background: var(--color-background-secondary);
      border-color: var(--color-border);
      color: var(--color-text);
    }
  }

  .favorite-btn {
    background: white;
    border: 1px solid var(--color-border);
    color: var(--color-text-secondary);
    font-size: 13px;
    transition: all var(--transition-fast);
    border-radius: var(--radius-md);

    &:hover {
      border-color: #ff4d4f;
      color: #ff4d4f;
    }

    &.favorited {
      background: #fff1f0;
      border-color: #ff4d4f;
      color: #ff4d4f;
    }
  }

  .retry-btn {
    background: #ff4d4f;
    color: white;
    border: none;
    font-weight: 600;
    font-size: 13px;
    transition: all var(--transition-fast);
    border-radius: var(--radius-md);

    &:hover {
      opacity: 0.9;
      transform: translateY(-1px);
    }
  }

  .export-btn {
    background: var(--gradient-primary);
    color: white;
    border: none;
    font-weight: 600;
    font-size: 13px;
    transition: all var(--transition-fast);
    border-radius: var(--radius-md);
    box-shadow: var(--shadow-warm);

    &:hover {
      opacity: 0.9;
      transform: translateY(-1px);
    }
  }

  .container {
    max-width: 1200px;
    margin: 0 auto;
    padding: 0 20px;
  }

  .article-card {
    border-radius: var(--radius-xl);
    border: 1px solid var(--color-border);
    box-shadow: var(--shadow-md);
    background: white;

    :deep(.ant-card-body) {
      padding: 40px;
    }
  }

  .title-section {
    margin-bottom: 28px;
    text-align: center;

    .main-title {
      font-size: 28px;
      font-weight: 700;
      margin: 0 0 10px;
      color: var(--color-text);
      line-height: 1.3;
      letter-spacing: -0.5px;
    }

    .sub-title {
      font-size: 16px;
      color: var(--color-text-secondary);
      margin: 0 0 20px;
    }

    .meta-info {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 12px;
      color: var(--color-text-muted);
      font-size: 13px;
    }

    .tags-section {
      margin-top: 16px;
      text-align: center;

      .tags-header {
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 8px;
        margin-bottom: 8px;

        .tags-label {
          font-size: 13px;
          color: var(--color-text-muted);
          font-weight: 500;
        }
      }

      .tags-content {
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 8px;
        flex-wrap: wrap;
      }

      .article-tag {
        border-radius: var(--radius-full);
        font-size: 12px;
        padding: 2px 12px;
      }

      .tag-input-wrapper {
        display: inline-flex;
      }

      .tag-input {
        width: 80px;
        font-size: 12px;
      }
    }

    .status-tag {
      border-radius: var(--radius-full);
      font-size: 12px;
      padding: 2px 12px;
    }
  }

  .section-title {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 16px;
    font-weight: 600;
    margin-bottom: 16px;
    color: var(--color-text);
  }

  .section-icon {
    font-size: 18px;
    color: var(--color-text-secondary);
  }

  .status-tag-small {
    font-size: 11px;
    padding: 2px 8px;
    margin-left: 8px;
  }

  /* 执行日志部分 */
  .execution-logs-section {
    margin-bottom: 28px;
    background: var(--color-background-secondary);
    border-radius: var(--radius-lg);
    border: 1px solid var(--color-border);
    overflow: hidden;

    .logs-header {
      padding: 16px 20px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      cursor: pointer;
      transition: background var(--transition-fast);

      &:hover {
        background: rgba(0, 0, 0, 0.02);
      }

      .section-title {
        margin: 0;
        display: flex;
        align-items: center;
      }

      .toggle-icon {
        font-size: 14px;
        color: var(--color-text-secondary);
        transition: transform var(--transition-fast);

        &.expanded {
          transform: rotate(180deg);
        }
      }
    }

    .logs-content {
      padding: 0 20px 20px;
    }

    .stats-summary {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 16px;
      margin-bottom: 24px;
      padding: 16px;
      background: white;
      border-radius: var(--radius-md);
      border: 1px solid var(--color-border-light);

      .stat-item {
        text-align: center;

        .label {
          display: block;
          font-size: 12px;
          color: var(--color-text-muted);
          margin-bottom: 4px;
        }

        .value {
          display: block;
          font-size: 20px;
          font-weight: 600;
          color: var(--color-primary);
        }
      }
    }

    .agent-timeline {
      position: relative;

      &::before {
        content: '';
        position: absolute;
        left: 16px;
        top: 12px;
        bottom: 12px;
        width: 2px;
        background: var(--color-border);
      }

      .timeline-item {
        position: relative;
        padding-left: 48px;
        padding-bottom: 20px;

        &:last-child {
          padding-bottom: 0;
        }

        .timeline-indicator {
          position: absolute;
          left: 8px;
          top: 2px;
          width: 20px;
          height: 20px;
          border-radius: 50%;
          background: white;
          display: flex;
          align-items: center;
          justify-content: center;
          border: 2px solid var(--color-border);

          .icon {
            font-size: 12px;

            &.success {
              color: var(--color-success);
            }

            &.failed {
              color: var(--color-error);
            }

            &.running {
              color: var(--color-primary);
            }
          }
        }

        &.success .timeline-indicator {
          border-color: var(--color-success);
        }

        &.failed .timeline-indicator {
          border-color: var(--color-error);
        }

        .timeline-content {
          background: white;
          padding: 12px 16px;
          border-radius: var(--radius-md);
          border: 1px solid var(--color-border-light);

          .timeline-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 4px;

            .agent-name {
              font-size: 14px;
              font-weight: 600;
              color: var(--color-text);
            }

            .duration {
              font-size: 13px;
              font-weight: 600;
              color: var(--color-primary);
            }
          }

          .timeline-time {
            font-size: 12px;
            color: var(--color-text-muted);
          }

          .error-message {
            margin-top: 8px;
            padding: 8px;
            background: rgba(255, 77, 79, 0.1);
            border-radius: var(--radius-md);
            font-size: 12px;
            color: var(--color-error);
            display: flex;
            align-items: flex-start;
            gap: 6px;

            .anticon {
              flex-shrink: 0;
              margin-top: 2px;
            }
          }
        }
      }
    }
  }

  /* 展开/收起动画 */
  .expand-enter-active,
  .expand-leave-active {
    transition: all 0.3s ease;
    overflow: hidden;
  }

  .expand-enter-from,
  .expand-leave-to {
    opacity: 0;
    max-height: 0;
  }

  .expand-enter-to,
  .expand-leave-from {
    opacity: 1;
    max-height: 2000px;
  }

  .outline-section {
    margin-bottom: 28px;

    .outline-list {
      .outline-item {
        margin-bottom: 12px;
        padding: 16px;
        background: var(--color-background-secondary);
        border-radius: var(--radius-md);
        border: 1px solid var(--color-border-light);
        transition: all var(--transition-fast);

        &:hover {
          border-color: var(--color-border);
        }

        .outline-title {
          font-size: 14px;
          font-weight: 600;
          margin-bottom: 8px;
          color: var(--color-text);
        }

        .outline-points {
          margin: 0;
          padding-left: 18px;

          li {
            margin-bottom: 4px;
            color: var(--color-text-secondary);
            line-height: 1.6;
            font-size: 13px;
          }
        }
      }
    }
  }

  .content-section {
    margin-bottom: 28px;

    .markdown-content {
      line-height: 1.8;
      font-size: 15px;
      color: var(--color-text);

      :deep(h2) {
        font-size: 20px;
        font-weight: 600;
        margin: 28px 0 14px;
        padding-bottom: 10px;
        border-bottom: 1px solid var(--color-border);
        color: var(--color-text);
      }

      :deep(h3) {
        font-size: 17px;
        font-weight: 600;
        margin: 22px 0 10px;
        color: var(--color-text);
      }

      :deep(p) {
        margin-bottom: 14px;
        text-indent: 2em;
        color: var(--color-text);
      }

      :deep(ul), :deep(ol) {
        margin-bottom: 14px;
        padding-left: 2em;
      }

      :deep(li) {
        margin-bottom: 6px;
        color: var(--color-text);
      }

      :deep(img) {
        display: block;
        max-width: 100%;
        max-height: 600px;
        width: auto;
        height: auto;
        margin: 20px auto;
        border-radius: var(--radius-md);
        box-shadow: var(--shadow-md);
        object-fit: contain;
      }

      // Mermaid 图表特殊处理（SVG 格式）
      :deep(img[src$=".svg"]) {
        max-width: 800px;
        max-height: 500px;
      }
    }
  }

  .images-section {
    .images-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
      gap: 16px;

      .image-item {
        border-radius: var(--radius-md);
        overflow: hidden;
        border: 1px solid var(--color-border);
        transition: all var(--transition-normal);
        cursor: pointer;

        &:hover {
          border-color: var(--color-text-muted);
          box-shadow: var(--shadow-md);
        }

        img {
          width: 100%;
          height: 160px;
          object-fit: cover;
        }

        .image-info {
          padding: 12px;
          background: white;
          display: flex;
          justify-content: space-between;
          align-items: center;

          .badge {
            padding: 3px 10px;
            background: var(--color-text);
            color: white;
            border-radius: var(--radius-md);
            font-size: 11px;
            font-weight: 500;
          }

          .keywords {
            font-size: 11px;
            color: var(--color-text-muted);
          }
        }
      }
    }
  }
}

@media (max-width: 768px) {
  .article-detail-page {
    .article-card {
      :deep(.ant-card-body) {
        padding: 24px;
      }
    }

    .title-section {
      .main-title {
        font-size: 22px;
      }

      .sub-title {
        font-size: 14px;
      }
    }
  }
}
/* 编辑模式样式 */
.edit-mode-section {
  margin-bottom: 24px;
}

.edit-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 16px;
}

.edit-container {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.edit-label {
  font-weight: 600;
  margin-bottom: 8px;
  color: var(--color-text);
}

.edit-textarea {
  font-family: 'Fira Code', 'Consolas', monospace;
  font-size: 14px;
}

.edit-preview {
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 16px;
  min-height: 400px;
  max-height: 600px;
  overflow-y: auto;
  background: var(--color-background);
}

@media (max-width: 768px) {
  .edit-container {
    grid-template-columns: 1fr;
  }
}

/* 质量评分样式 */
.review-section {
  margin-bottom: 24px;
}

.review-score-card {
  display: flex;
  align-items: flex-start;
  gap: 32px;
  padding: 20px;
  background: var(--color-background);
  border-radius: 12px;
  border: 1px solid var(--color-border);
}

.score-circle {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100px;
  height: 100px;
  border-radius: 50%;
  border: 4px solid;
  flex-shrink: 0;
}

.score-number {
  font-size: 32px;
  font-weight: 700;
  line-height: 1;
}

.score-label {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-top: 4px;
}

.score-details {
  flex: 1;
}

.suggestions-title {
  font-weight: 600;
  margin-bottom: 8px;
  color: var(--color-text);
}

.wysiwyg-container {
  margin-bottom: 16px;
}


.suggestions-list {
  padding-left: 20px;
  margin: 0;
}

.suggestions-list li {
  margin-bottom: 6px;
  color: var(--color-text-secondary);
  line-height: 1.6;
}
/* 反馈区域样式 */
.feedback-section {
  max-width: 900px;
  margin: 0 auto 24px;
  padding: 0 24px;
}

.feedback-card {
  border-radius: 12px;
  box-shadow: var(--shadow-card);
}

.feedback-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 8px 0;
}

.feedback-question {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
}

.feedback-actions {
  display: flex;
  gap: 12px;
}

.feedback-stats {
  font-size: 13px;
  color: var(--color-text-muted);
}


</style>
