<template>
  <div class="prompt-manage-page">
    <div class="page-header">
      <h1>Prompt 模板管理</h1>
      <a-button type="primary" @click="showCreateModal = true">
        <PlusOutlined /> 新建模板
      </a-button>
    </div>

    <a-table :dataSource="templates" :columns="columns" :loading="loading" rowKey="id" :pagination="pagination"
      @change="handleTableChange">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-tag :color="record.status === 1 ? 'green' : 'red'">{{ record.status === 1 ? '启用' : '禁用' }}</a-tag>
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a-button size="small" @click="handleEdit(record)">编辑</a-button>
            <a-popconfirm title="确认删除？" @confirm="handleDelete(record.id)">
              <a-button size="small" danger>删除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 新建/编辑弹窗 -->
    <a-modal v-model:open="showCreateModal" :title="editingId ? '编辑模板' : '新建模板'" @ok="handleSubmit" :confirmLoading="submitting">
      <a-form layout="vertical">
        <a-form-item label="模板名称" required>
          <a-input v-model:value="form.name" placeholder="如：标题生成Prompt" />
        </a-form-item>
        <a-form-item label="所属智能体" required>
          <a-select v-model:value="form.agentName" placeholder="选择智能体">
            <a-select-option v-for="agent in agentOptions" :key="agent" :value="agent">{{ agent }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="Prompt 内容" required>
          <a-textarea v-model:value="form.promptContent" :auto-size="{ minRows: 10 }" placeholder="输入 Prompt 内容..." />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { listPromptTemplates, createPromptTemplate, updatePromptTemplate, deletePromptTemplate } from '@/api/promptTemplateController'

const loading = ref(false)
const submitting = ref(false)
const showCreateModal = ref(false)
const editingId = ref<number | null>(null)
const templates = ref<any[]>([])

const form = reactive({ name: '', agentName: '', promptContent: '' })

const agentOptions = [
  'TitleGeneratorAgent', 'OutlineGeneratorAgent', 'ContentGeneratorAgent',
  'ImageAnalyzerAgent', 'ContentMergerAgent', 'ReviewerAgent',
  'ScriptHookAgent', 'ScriptOutlineAgent', 'ScriptContentAgent',
  'LiveOutlineAgent', 'LiveScriptAgent', 'InterviewOutlineAgent',
  'EventOutlineAgent', 'DramaCharacterAgent'
]

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
  { title: '名称', dataIndex: 'name', key: 'name' },
  { title: '智能体', dataIndex: 'agentName', key: 'agentName' },
  { title: '版本', dataIndex: 'version', key: 'version', width: 80 },
  { title: '状态', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 150 },
]

const loadData = async () => {
  loading.value = true
  try {
    const res = await listPromptTemplates({ pageNum: pagination.current, pageSize: pagination.pageSize })
    if (res.data.code === 0) {
      templates.value = res.data.data?.records || []
      pagination.total = res.data.data?.total || 0
    }
  } catch { message.error('加载失败') }
  finally { loading.value = false }
}

const handleTableChange = (pag: any) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadData()
}

const handleEdit = (record: any) => {
  editingId.value = record.id
  form.name = record.name
  form.agentName = record.agentName
  form.promptContent = record.promptContent
  showCreateModal.value = true
}

const handleDelete = async (id: number) => {
  try {
    const res = await deletePromptTemplate(id)
    if (res.data.code === 0) { message.success('已删除'); loadData() }
  } catch { message.error('删除失败') }
}

const handleSubmit = async () => {
  if (!form.name || !form.agentName || !form.promptContent) { message.warning('请填写完整'); return }
  submitting.value = true
  try {
    if (editingId.value) {
      await updatePromptTemplate({ id: editingId.value, ...form })
    } else {
      await createPromptTemplate(form)
    }
    message.success(editingId.value ? '已更新' : '已创建')
    showCreateModal.value = false
    editingId.value = null
    form.name = ''; form.agentName = ''; form.promptContent = ''
    loadData()
  } catch { message.error('操作失败') }
  finally { submitting.value = false }
}

onMounted(() => loadData())
</script>

<style scoped>
.prompt-manage-page { max-width: 1200px; margin: 0 auto; padding: 24px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.page-header h1 { margin: 0; font-size: 22px; font-weight: 700; }
</style>
