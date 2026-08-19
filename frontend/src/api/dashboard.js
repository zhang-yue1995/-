import request from '@/utils/request'

/**
 * 获取仪表盘统计数据
 */
export function getDashboardStats(options = {}) {
  return request({
    url: '/dashboard/stats',
    method: 'get',
    showLoading: options.showLoading !== false
  })
}

/**
 * 获取最近上传记录
 * @param {Object} params - 查询参数 {limit}
 */
export function getRecentUploads(params) {
  return request({
    url: '/dashboard/recent-uploads',
    method: 'get',
    params
  })
}

/**
 * 获取风险分布数据
 */
export function getRiskDistribution() {
  return request({
    url: '/dashboard/risk-distribution',
    method: 'get'
  })
}

/**
 * 获取图表数据（处理量、通过率等）
 * @param {String} type - 图表类型 (processing/risk/trend)
 */
export function getTrendChartData(days = 30) {
  return request({
    url: '/dashboard/trend-chart',
    method: 'get',
    params: { days }
  })
}
