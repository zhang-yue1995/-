import request from '@/utils/request'

/**
 * 获取企业列表
 * @param {Object} params - 查询参数 {page, pageSize, keyword, period, riskLevel, status}
 */
export function getEnterpriseList(params) {
  return request({
    url: '/enterprises',
    method: 'get',
    params
  })
}

/**
 * 获取企业详情
 * @param {String|Number} id - 企业ID
 */
export function getEnterpriseDetail(id) {
  return request({
    url: `/enterprises/${id}`,
    method: 'get'
  })
}

export function getEnterpriseByCreditCode(creditCode) {
  return request({ url: '/enterprises/by-credit-code', method: 'get', params: { creditCode } })
}

/**
 * 创建新企业
 * @param {Object} data - 企业数据
 */
export function createEnterprise(data) {
  return request({
    url: '/enterprises',
    method: 'post',
    data
  })
}

/**
 * 更新企业信息
 * @param {String|Number} id - 企业ID
 * @param {Object} data - 更新数据
 */
export function updateEnterprise(id, data) {
  return request({
    url: `/enterprises/${id}`,
    method: 'put',
    data
  })
}

export function deleteEnterprise(id) {
  return request({
    url: `/enterprises/${id}`,
    method: 'delete'
  })
}

/**
 * 获取企业的报表列表
 * @param {String|Number} id - 企业ID
 */
export function getEnterpriseReports(id, params) {
  return request({
    url: `/enterprises/${id}/reports`,
    method: 'get',
    params
  })
}

/**
 * 获取企业分析报告
 * @param {String|Number} id - 企业ID
 */
export function getEnterpriseAnalysis(id) {
  return request({
    url: `/enterprises/${id}/analysis`,
    method: 'get'
  })
}

/**
 * 获取企业趋势数据
 * @param {String|Number} id - 企业ID
 * @param {Object} params - 查询参数 {period, granularity}
 */
export function getEnterpriseTrends(id, params) {
  return request({
    url: `/enterprises/${id}/trends`,
    method: 'get',
    params
  })
}
