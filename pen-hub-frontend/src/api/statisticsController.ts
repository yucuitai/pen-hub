// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 获取系统统计数据 GET /statistics/overview */
export async function getStatistics(options?: { [key: string]: any }) {
  return request<API.BaseResponseStatisticsVO>('/statistics/overview', {
    method: 'GET',
    ...(options || {}),
  })
}

/** 获取 AI 调用统计 GET /statistics/ai-call-stats */
export async function getAiCallStats(options?: { [key: string]: any }) {
  return request<API.BaseResponseMapStringObject>('/statistics/ai-call-stats', {
    method: 'GET',
    ...(options || {}),
  })
}
