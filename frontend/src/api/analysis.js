import request from '@/utils/request'

/**
 * 获取分析报告
 * @param {String|Number} reportId - 报表ID
 */
export function getAnalysisReport(reportId) {
  return request({
    url: `/analysis-reports/report/${reportId}`,
    method: 'get'
  })
}

/**
 * 生成分析报告
 * @param {String|Number} reportId - 报表ID
 */
export function generateReport(reportId) {
  return request({
    url: `/analysis-reports/generate/${reportId}`,
    method: 'post'
  })
}

/**
 * 导出分析报告
 * @param {String|Number} reportId - 报表ID
 * @param {String} format - 导出格式 (pdf/word/excel)
 */
export function exportReport(reportId, format = 'pdf') {
  return request({
    url: `/analysis-reports/export/${reportId}`,
    method: 'get',
    params: { format },
    responseType: 'blob'
  })
}
