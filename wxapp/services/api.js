// API基础配置：开发版连接本机，体验版/正式版读取固定配置或 extConfig。
const environment = require('../config');

function normalizeBaseUrl(url) {
  return String(url || '').trim().replace(/\/+$/, '');
}

function getBaseUrl() {
  let envVersion = 'develop';
  try {
    envVersion = wx.getAccountInfoSync().miniProgram.envVersion || 'develop';
  } catch (error) {
    console.warn('无法读取小程序运行环境，按开发环境处理', error);
  }
  if (envVersion === 'develop') {
    // 开发环境只使用代码中的显式配置，避免真机存储或 extConfig 的旧值覆盖。
    return normalizeBaseUrl(environment.developmentBaseUrl);
  }

  if (typeof wx.getExtConfigSync === 'function') {
    const extConfig = wx.getExtConfigSync() || {};
    const extBaseUrl = normalizeBaseUrl(extConfig.apiBaseUrl);
    if (extBaseUrl) return extBaseUrl;
  }

  const productionBaseUrl = normalizeBaseUrl(environment.productionBaseUrl);
  if (productionBaseUrl) return productionBaseUrl;

  throw new Error('未配置服务地址，请在 config.js 或 extConfig 中设置 apiBaseUrl');
}

module.exports = {
  getBaseUrl,
  TIMEOUT: 30000
};
