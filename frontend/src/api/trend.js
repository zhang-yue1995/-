import request from '@/utils/request'

/**
 * 获取趋势数据
 * @param {String|Number} enterpriseId - 企业ID
 * @param {Object} params - 查询参数 {granularity, startDate, endDate}
 */
export function getTrendData(enterpriseId, params) {
  return request({
    url: `/trends/enterprise/${enterpriseId}`,
    method: 'get',
    params
  })
}

/**
 * 获取多指标趋势对比
 * @param {String|Number} enterpriseId - 企业ID
 * @param {Object} params - 查询参数 {indicators, granularity}
 */
export function getMultiIndicatorTrend(enterpriseId, params) {
  return request({
    url: `/trends/enterprise/${enterpriseId}/indicators`,
    method: 'get',
    params
  })
}

/**
 * 获取趋势摘要信息
 * @param {String|Number} enterpriseId - 企业ID
 */
export function getTrendSummary(enterpriseId) {
  return request({
    url: `/trends/enterprise/${enterpriseId}/summary`,
    method: 'get'
  })
}
