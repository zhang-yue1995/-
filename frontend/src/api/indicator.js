import request from '@/utils/request'

/**
 * 获取报表的指标数据
 * @param {String|Number} reportId - 报表ID
 */
export function getReportIndicators(reportId) {
  return request({
    url: `/indicators/report/${reportId}`,
    method: 'get'
  })
}

/**
 * 获取健康度评分
 * @param {String|Number} reportId - 报表ID
 */
export function getHealthScore(reportId) {
  return request({
    url: `/indicators/report/${reportId}/health-score`,
    method: 'get'
  })
}

/**
 * 获取指标定义列表
 */
export function getIndicatorDefinitions() {
  return request({
    url: '/indicators/definitions',
    method: 'get'
  })
}
