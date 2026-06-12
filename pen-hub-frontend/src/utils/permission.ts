import { USER_ROLE_ADMIN, USER_ROLE_VIP } from '@/constants/user'

/**
 * 权限判断工具
 */

/**
 * 判断用户是否为管理员
 */
export const isAdmin = (user?: API.LoginUserVO): boolean => {
  return user?.userRole === USER_ROLE_ADMIN
}

/**
 * 判断用户是否为 VIP（包括管理员）
 * 支持新的 vipLevel 分级体系
 */
export const isVip = (user?: API.LoginUserVO): boolean => {
  if (isAdmin(user)) return true
  if (user?.userRole === USER_ROLE_VIP) return true
  return (user?.vipLevel ?? 0) > 0
}

/**
 * 获取 VIP 等级名称
 */
export const getVipLevelName = (user?: API.LoginUserVO): string => {
  if (isAdmin(user)) return '管理员'
  const level = user?.vipLevel ?? 0
  const names: Record<number, string> = { 0: '普通用户', 1: '基础版', 2: '专业版', 3: '旗舰版' }
  return names[level] || '普通用户'
}

/**
 * 判断用户是否有配额
 */
export const hasQuota = (user?: API.LoginUserVO): boolean => {
  if (isAdmin(user) || isVip(user)) {
    return true
  }
  return (user?.quota ?? 0) > 0
}
