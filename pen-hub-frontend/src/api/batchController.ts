// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 批量创建文章 */
export async function batchCreateArticle(body: {
  topics: string[]
  style?: string
  contentType?: string
}) {
  return request<API.BaseResponseListString>('/batch/create', { method: 'POST', data: body })
}
