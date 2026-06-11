// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 创建兑换码 POST /redemption/code */
export async function createRedemptionCode(
  body: API.RedemptionCodeCreateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListRedemptionCodeVO>('/redemption/code', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 查询所有兑换码 GET /redemption/codes */
export async function getAllRedemptionCodes(options?: { [key: string]: any }) {
  return request<API.BaseResponseListRedemptionCodeVO>('/redemption/codes', {
    method: 'GET',
    ...(options || {}),
  })
}

/** 查询我的兑换记录 GET /redemption/records */
export async function getMyRedemptionRecords(options?: { [key: string]: any }) {
  return request<API.BaseResponseListRedemptionRecordVO>('/redemption/records', {
    method: 'GET',
    ...(options || {}),
  })
}

/** 查询所有兑换记录 GET /redemption/records/all */
export async function getAllRedemptionRecords(options?: { [key: string]: any }) {
  return request<API.BaseResponseListRedemptionRecordVO>('/redemption/records/all', {
    method: 'GET',
    ...(options || {}),
  })
}

/** 兑换会员 POST /redemption/redeem */
export async function redeemCode(body: API.RedemptionRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseRedemptionRecordVO>('/redemption/redeem', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}
