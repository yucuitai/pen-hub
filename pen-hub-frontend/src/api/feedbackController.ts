// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 提交反馈 POST /feedback/submit */
export async function submitFeedback(
  body: {
    taskId: string
    rating: number
    comment?: string
  },
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseBoolean>('/feedback/submit', {
    method: 'POST',
    data: body,
    ...(options || {}),
  })
}

/** 获取反馈统计 GET /feedback/stats/${taskId} */
export async function getFeedbackStats(taskId: string, options?: { [key: string]: any }) {
  return request<API.BaseResponseMapStringObject>(`/feedback/stats/${taskId}`, {
    method: 'GET',
    ...(options || {}),
  })
}

/** 获取当前用户反馈 GET /feedback/my/${taskId} */
export async function getMyFeedback(taskId: string, options?: { [key: string]: any }) {
  return request<API.BaseResponseArticleFeedback>(`/feedback/my/${taskId}`, {
    method: 'GET',
    ...(options || {}),
  })
}
