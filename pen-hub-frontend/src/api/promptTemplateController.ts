// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 获取Prompt模板列表 */
export async function listPromptTemplates(params: {
  agentName?: string
  pageNum?: number
  pageSize?: number
}) {
  return request('/admin/prompt/list', { method: 'GET', params })
}

/** 获取Prompt模板详情 */
export async function getPromptTemplate(id: number) {
  return request(`/admin/prompt/${id}`, { method: 'GET' })
}

/** 创建Prompt模板 */
export async function createPromptTemplate(data: {
  name: string
  agentName: string
  promptContent: string
}) {
  return request('/admin/prompt/create', { method: 'POST', data })
}

/** 更新Prompt模板 */
export async function updatePromptTemplate(data: {
  id: number
  name?: string
  agentName?: string
  promptContent?: string
  status?: number
}) {
  return request('/admin/prompt/update', { method: 'PUT', data })
}

/** 删除Prompt模板 */
export async function deletePromptTemplate(id: number) {
  return request(`/admin/prompt/${id}`, { method: 'DELETE' })
}
