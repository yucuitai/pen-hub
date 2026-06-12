<template>
  <div class="dashboard-page">
    <div class="page-header">
      <h1 class="page-title">数据驾驶舱</h1>
    </div>

    <div class="container">
      <!-- 统计卡片 -->
      <div class="stats-cards">
        <div class="stat-card">
          <div class="stat-icon total">
            <FileTextOutlined />
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.totalCount ?? 0 }}</div>
            <div class="stat-label">总创作数</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon week">
            <CalendarOutlined />
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.weekCount ?? 0 }}</div>
            <div class="stat-label">本周创作</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon month">
            <RiseOutlined />
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.monthCount ?? 0 }}</div>
            <div class="stat-label">本月创作</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon success">
            <CheckCircleOutlined />
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.successRate ?? 0 }}%</div>
            <div class="stat-label">成功率</div>
          </div>
        </div>
      </div>

      <!-- 图表区域 -->
      <div class="charts-grid">
        <a-card :bordered="false" class="chart-card">
          <template #title>创作趋势</template>
          <div ref="trendChartRef" class="chart-container"></div>
        </a-card>
        <a-card :bordered="false" class="chart-card">
          <template #title>最近创作</template>
          <div class="recent-list">
            <div v-for="article in recentArticles" :key="article.taskId" class="recent-item" @click="goToDetail(article.taskId)">
              <div class="recent-title">{{ article.mainTitle }}</div>
              <div class="recent-meta">
                <a-tag :color="getStatusColor(article.status ?? '')" size="small">
                  {{ getStatusText(article.status ?? '') }}
                </a-tag>
                <span class="recent-time">{{ article.createTime ? formatDate(article.createTime) : '' }}</span>
              </div>
            </div>
            <a-empty v-if="recentArticles.length === 0" description="暂无创作记录" />
          </div>
        </a-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import {
  FileTextOutlined,
  CalendarOutlined,
  RiseOutlined,
  CheckCircleOutlined
} from '@ant-design/icons-vue'
import { listArticle } from '@/api/articleController'
import { getStatistics } from '@/api/statisticsController'
import * as echarts from 'echarts/core'
import { BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import dayjs from 'dayjs'

echarts.use([BarChart, GridComponent, TooltipComponent, CanvasRenderer])

const router = useRouter()

const stats = ref<API.StatisticsVO>({})
const recentArticles = ref<API.ArticleVO[]>([])
const trendChartRef = ref<HTMLElement>()
let trendChart: echarts.ECharts | null = null

const loadStats = async () => {
  try {
    const res = await getStatistics()
    stats.value = res.data.data || {}
  } catch {
    // 静默处理
  }
}

const loadRecentArticles = async () => {
  try {
    const res = await listArticle({ pageNum: 1, pageSize: 5 })
    recentArticles.value = res.data.data?.records || []
  } catch {
    // 静默处理
  }
}

const initTrendChart = () => {
  if (!trendChartRef.value) return
  trendChart = echarts.init(trendChartRef.value)
  const option = {
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
      axisLine: { lineStyle: { color: '#d9d9d9' } },
      axisLabel: { color: '#666' }
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      splitLine: { lineStyle: { color: '#f0f0f0' } },
      axisLabel: { color: '#666' }
    },
    series: [{
      data: [0, 0, 0, 0, 0, 0, 0],
      type: 'bar',
      barWidth: '40%',
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#D97706' },
          { offset: 1, color: '#FDE68A' }
        ]),
        borderRadius: [4, 4, 0, 0]
      }
    }]
  }
  trendChart.setOption(option)
}

const getStatusColor = (status: string) => {
  const map: Record<string, string> = {
    COMPLETED: 'success',
    PROCESSING: 'processing',
    PENDING: 'default',
    FAILED: 'error'
  }
  return map[status] || 'default'
}

const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    COMPLETED: '已完成',
    PROCESSING: '生成中',
    PENDING: '待处理',
    FAILED: '失败'
  }
  return map[status] || status
}

const formatDate = (date: string) => {
  return dayjs(date).format('MM-DD HH:mm')
}

const goToDetail = (taskId?: string) => {
  if (taskId) {
    router.push(`/article/${taskId}`)
  }
}

onMounted(async () => {
  await Promise.all([loadStats(), loadRecentArticles()])
  await nextTick()
  initTrendChart()
  window.addEventListener('resize', () => trendChart?.resize())
})

onUnmounted(() => {
  trendChart?.dispose()
})
</script>

<style scoped>
.dashboard-page {
  min-height: 100vh;
  background: var(--color-background);
}

.page-header {
  padding: 24px 0 16px;
  text-align: center;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--color-text);
  margin: 0;
}

.container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 0 24px 48px;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: var(--color-background);
  border: 1px solid var(--color-border);
  border-radius: 12px;
  transition: box-shadow 0.3s;
}

.stat-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
}

.stat-icon.total {
  background: #FFF7ED;
  color: #D97706;
}

.stat-icon.week {
  background: #EFF6FF;
  color: #2563EB;
}

.stat-icon.month {
  background: #F0FDF4;
  color: #16A34A;
}

.stat-icon.success {
  background: #FDF2F8;
  color: #DB2777;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--color-text);
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.charts-grid {
  display: grid;
  grid-template-columns: 1.5fr 1fr;
  gap: 16px;
}

.chart-card {
  border-radius: 12px;
}

.chart-container {
  height: 280px;
}

.recent-list {
  max-height: 280px;
  overflow-y: auto;
}

.recent-item {
  padding: 12px 0;
  border-bottom: 1px solid var(--color-border);
  cursor: pointer;
  transition: background 0.2s;
}

.recent-item:hover {
  background: var(--color-background-secondary);
}

.recent-item:last-child {
  border-bottom: none;
}

.recent-title {
  font-weight: 500;
  color: var(--color-text);
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recent-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.recent-time {
  font-size: 12px;
  color: var(--color-text-secondary);
}

@media (max-width: 768px) {
  .stats-cards {
    grid-template-columns: repeat(2, 1fr);
  }
  .charts-grid {
    grid-template-columns: 1fr;
  }
}
</style>
