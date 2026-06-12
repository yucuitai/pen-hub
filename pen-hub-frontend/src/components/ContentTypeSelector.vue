<template>
  <a-modal
    v-model:open="visible"
    title="选择创作类型"
    :footer="null"
    :width="600"
    centered
    @cancel="handleCancel"
  >
    <div class="content-type-grid">
      <div
        v-for="item in contentTypes"
        :key="item.value"
        class="content-type-card"
        @click="handleSelect(item.value)"
      >
        <div class="card-icon" :style="{ background: item.gradient }">
          <component :is="item.icon" />
        </div>
        <div class="card-info">
          <h3>{{ item.label }}</h3>
          <p>{{ item.description }}</p>
          <div class="card-tags">
            <a-tag v-for="tag in item.tags" :key="tag" size="small">{{ tag }}</a-tag>
          </div>
        </div>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import {
  FileTextOutlined,
  VideoCameraOutlined,
  PlayCircleOutlined,
  AudioOutlined,
  CommentOutlined,
  ReadOutlined,
} from '@ant-design/icons-vue'

interface Props {
  open: boolean
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'update:open', value: boolean): void
  (e: 'select', contentType: string): void
}>()

const visible = computed({
  get: () => props.open,
  set: (val) => emit('update:open', val),
})

const contentTypes = [
  {
    value: 'ARTICLE',
    label: '新媒体文案',
    description: '公众号、小红书、抖音等平台的文章和文案',
    icon: FileTextOutlined,
    gradient: 'linear-gradient(135deg, #D97706 0%, #F59E0B 100%)',
    tags: ['公众号', '小红书', '抖音', '微博'],
  },
  {
    value: 'SHORT_VIDEO_SCRIPT',
    label: '短视频脚本',
    description: '包含分镜、台词、画面描述的短视频拍摄脚本',
    icon: VideoCameraOutlined,
    gradient: 'linear-gradient(135deg, #7C3AED 0%, #A78BFA 100%)',
    tags: ['抖音', 'B站', '视频号'],
  },
  {
    value: 'LIVE_SCRIPT',
    label: '直播台本',
    description: '电商带货、知识分享等直播的完整台本',
    icon: PlayCircleOutlined,
    gradient: 'linear-gradient(135deg, #DC2626 0%, #F87171 100%)',
    tags: ['电商带货', '知识分享', '活动直播'],
  },
  {
    value: 'INTERVIEW_SCRIPT',
    label: '访谈脚本',
    description: '播客、访谈节目的对话脚本',
    icon: AudioOutlined,
    gradient: 'linear-gradient(135deg, #0891B2 0%, #22D3EE 100%)',
    tags: ['播客', '访谈', '对话'],
  },
  {
    value: 'EVENT_SCRIPT',
    label: '活动台本',
    description: '发布会、会议、典礼等活动的完整台本',
    icon: CommentOutlined,
    gradient: 'linear-gradient(135deg, #059669 0%, #34D399 100%)',
    tags: ['发布会', '会议', '典礼'],
  },
  {
    value: 'DRAMA_SCRIPT',
    label: '剧本',
    description: '短剧、微电影的故事剧本',
    icon: ReadOutlined,
    gradient: 'linear-gradient(135deg, #7C3AED 0%, #A78BFA 100%)',
    tags: ['短剧', '微电影', '故事'],
  },
]

const handleSelect = (value: string) => {
  emit('select', value)
  visible.value = false
}

const handleCancel = () => {}
</script>

<style scoped>
.content-type-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 8px 0;
}

.content-type-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  border: 2px solid #e5e5e5;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.content-type-card:hover {
  border-color: #D97706;
  box-shadow: 0 4px 12px rgba(217, 119, 6, 0.15);
  transform: translateY(-2px);
}

.card-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: white;
  flex-shrink: 0;
}

.card-info {
  flex: 1;
}

.card-info h3 {
  margin: 0 0 4px;
  font-size: 16px;
  font-weight: 600;
}

.card-info p {
  margin: 0 0 8px;
  font-size: 13px;
  color: #666;
}

.card-tags {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}
</style>
