// 趋势相关服务
const { get } = require('./request');

/**
 * 获取历史趋势数据
 * @param {string} enterpriseId 企业ID
 * @param {Object} params 查询参数（periodType, count等）
 * @returns {Promise}
 */
function getTrendData(enterpriseId, params) {
  return get(`/trends/enterprise/${enterpriseId}`, params);
}

/**
 * 获取多指标对比趋势
 * @param {string} enterpriseId 企业ID
 * @param {Array} indicatorCodes 指标代码数组
 * @returns {Promise}
 */
function getMultiIndicatorTrend(enterpriseId, indicatorCodes) {
  return get(`/trends/enterprise/${enterpriseId}/indicators`, {
    periods: indicatorCodes && indicatorCodes.periods ? indicatorCodes.periods : 6
  });
}

module.exports = {
  getTrendData,
  getMultiIndicatorTrend
};
