// 企业相关服务
const { get, post } = require('./request');

/**
 * 获取企业列表
 * @param {Object} params 查询参数（page, size, keyword等）
 * @returns {Promise}
 */
function getEnterpriseList(params) {
  return get('/enterprises', params);
}

/**
 * 获取企业详情
 * @param {string} id 企业ID
 * @returns {Promise}
 */
function getEnterpriseDetail(id) {
  return get(`/enterprises/${id}`);
}

function getEnterpriseByCreditCode(creditCode) {
  return get('/enterprises/by-credit-code', { creditCode });
}

/**
 * 获取企业的报表列表
 * @param {string} id 企业ID
 * @param {Object} params 查询参数
 * @returns {Promise}
 */
function getEnterpriseReports(id, params) {
  return get(`/enterprises/${id}/reports`, params);
}

function createEnterprise(data) {
  return post('/enterprises', data);
}

module.exports = {
  getEnterpriseList,
  getEnterpriseDetail,
  getEnterpriseByCreditCode,
  getEnterpriseReports,
  createEnterprise
};
