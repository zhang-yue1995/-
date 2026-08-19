// 认证服务
const { post } = require('./request');
const app = getApp();

/**
 * 登录
 * @param {string} username 用户名
 * @param {string} password 密码
 * @returns {Promise}
 */
function login(username, password) {
  return new Promise((resolve, reject) => {
    post('/auth/login', { username, password }, { loading: false })
      .then(res => {
        const userInfo = res.data || {};
        const { token } = userInfo;
        if (!token) {
          throw new Error('登录响应中缺少访问令牌');
        }

        // 存储token
        setToken(token);
        wx.setStorageSync('userInfo', userInfo);
        app.globalData.token = token;
        app.globalData.userInfo = userInfo;

        resolve(res);
      })
      .catch(reject);
  });
}

/**
 * 登出
 */
function logout() {
  // 清除本地存储
  clearToken();

  // 清除全局数据
  app.globalData.token = null;
  app.globalData.userInfo = null;
  app.globalData.currentReport = null;

  wx.showToast({
    title: '已退出登录',
    icon: 'success'
  });
}

/**
 * 获取用户信息
 * @returns {Object|null}
 */
function getUserInfo() {
  return app.globalData.userInfo || wx.getStorageSync('userInfo');
}

/**
 * 获取Token
 * @returns {string|null}
 */
function getToken() {
  return wx.getStorageSync('token') || app.globalData.token;
}

/**
 * 设置Token
 * @param {string} token
 */
function setToken(token) {
  wx.setStorageSync('token', token);
}

/**
 * 清除Token
 */
function clearToken() {
  wx.removeStorageSync('token');
  wx.removeStorageSync('userInfo');
}

module.exports = {
  login,
  logout,
  getUserInfo,
  getToken,
  setToken,
  clearToken
};
