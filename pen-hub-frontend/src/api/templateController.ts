// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 获取模板列表 GET /template/list */
export async function listTemplates(
  params: {
    category?: string
    platform?: string
    pageNum?: number
    pageSize?: number
  },
  options?: { [key: string]: any },
) {
  return request<API.BaseResponsePageTemplate>('/template/list', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 获取模板详情 GET /template/${param0} */
export async function getTemplate(id: number, options?: { [key: string]: any }) {
  return request<API.BaseResponseTemplate>(`/template/${id}`, {
    method: 'GET',
    ...(options || {}),
  })
}
