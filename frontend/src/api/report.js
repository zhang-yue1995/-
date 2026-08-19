import request from '@/utils/request'

/**
 * 获取报表列表
 * @param {Object} params - 查询参数 {page, pageSize, enterpriseId, status, type}
 */
export function getReportList(params, options = {}) {
  return request({
    url: '/reports',
    method: 'get',
    params,
    showLoading: options.showLoading !== false
  })
}

/**
 * 获取报表详情
 * @param {String|Number} id - 报表ID
 */
export function getReportDetail(id) {
  return request({
    url: `/reports/${id}`,
    method: 'get'
  })
}

/**
 * 归档报表
 * @param {Object} data - 归档数据 {reportId, archiveType}
 */
export function archiveReport(data) {
  return request({
    url: '/reports/archive',
    method: 'post',
    data
  })
}

/**
 * 提交复核
 * @param {String|Number} id - 报表ID
 * @param {Object} data - 复核数据 {reviewer, comments, action}
 */
export function submitReview(id, data) {
  return request({
    url: `/reports/${id}/review`,
    method: 'put',
    data
  })
}

/**
 * 删除报表
 * @param {String|Number} id - 报表ID
 */
export function deleteReport(id) {
  return request({
    url: `/reports/${id}`,
    method: 'delete'
  })
}

/**
 * 获取报表的财务报表数据（资产负债表、利润表、现金流量表）
 * @param {String|Number} id - 报表ID
 */
export function getReportStatements(id) {
  return request({
    url: `/reports/${id}/statements`,
    method: 'get'
  })
}

export function intakeReport(data) {
  return request({
    url: '/reports/intake',
    method: 'post',
    data,
    timeout: 120000
  })
}

export function getReportValidations(id) {
  return request({
    url: `/reports/${id}/validations`,
    method: 'get'
  })
}

export function approveReport(id) {
  return request({
    url: `/reports/${id}/complete-approval`,
    method: 'put'
  })
}

export function rejectReport(id, reason) {
  return request({
    url: `/reports/${id}/reject-approval`,
    method: 'put',
    data: { reason }
  })
}
