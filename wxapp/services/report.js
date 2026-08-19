// 报表相关服务
const { get, post, put } = require('./request');

/**
 * 报表建档
 * @param {Object} data 报表信息
 * @returns {Promise}
 */
function archiveReport(data) {
  return post('/reports/archive', data);
}

/**
 * 提交复核结果
 * @param {string} reportId 报表ID
 * @param {Array} fields 字段列表
 * @returns {Promise}
 */
function submitReview(reportId, fields) {
  return put(`/reports/${reportId}/review`, fields);
}

/**
 * 获取三大报表
 * @param {string} reportId 报表ID
 * @returns {Promise}
 */
function getStatements(reportId) {
  return get(`/reports/${reportId}/statements`);
}

function getReportDetail(reportId) {
  return get(`/reports/${reportId}`);
}

/**
 * 获取财务指标
 * @param {string} reportId 报表ID
 * @returns {Promise}
 */
function getIndicators(reportId) {
  return get(`/indicators/report/${reportId}`);
}

/**
 * 获取健康评分
 * @param {string} reportId 报表ID
 * @returns {Promise}
 */
function getHealthScore(reportId) {
  return get(`/indicators/report/${reportId}/health-score`);
}

/**
 * 获取分析报告
 * @param {string} reportId 报表ID
 * @returns {Promise}
 */
function getAnalysisReport(reportId) {
  return get(`/analysis-reports/report/${reportId}`);
}

function generateAnalysisReport(reportId) {
  return post(`/analysis-reports/generate/${reportId}`, {});
}

function submitAnalysisApproval(reportId) {
  return post(`/analysis-reports/${reportId}/submit-approval`, {});
}

function getValidations(reportId) {
  return get(`/reports/${reportId}/validations`);
}

/**
 * 获取报表列表
 * @param {Object} params 查询参数
 * @returns {Promise}
 */
function getReportList(params) {
  return get('/reports', params);
}

module.exports = {
  archiveReport,
  submitReview,
  getReportDetail,
  getStatements,
  getIndicators,
  getHealthScore,
  getAnalysisReport,
  generateAnalysisReport,
  submitAnalysisApproval,
  getValidations,
  getReportList
};
