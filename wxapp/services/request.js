// 网络请求统一封装
const { getBaseUrl, TIMEOUT } = require('./api');

// 多个页面接口可能同时返回 401。用一个模块级状态合并为一次提示和一次跳转。
let unauthorizedModalVisible = false;
let unauthorizedRedirecting = false;

/**
 * 请求拦截器
 */
function requestInterceptor(options) {
  // 添加token
  const token = wx.getStorageSync('token');
  if (token) {
    options.header = {
      ...options.header,
      'Authorization': `Bearer ${token}`
    };
  }

  // 显示loading（除非明确禁止）
  if (options.loading !== false) {
    wx.showLoading({
      title: options.loadingText || '加载中...',
      mask: true
    });
  }

  return options;
}

/**
 * 响应拦截器
 */
function responseInterceptor(res, resolve, reject) {
  // 隐藏loading
  if (res.requestOptions && res.requestOptions.loading !== false) {
    wx.hideLoading();
  }

  const { statusCode, data } = res;

  // HTTP状态码处理
  if (statusCode === 200) {
    // 业务状态码处理
    if (data && (data.code === 200 || data.code === 0 || data.success)) {
      resolve(data);
    } else if (data.code === 401) {
      // Token过期或无效
      handleUnauthorized();
      reject(new Error(data.message || '登录已过期'));
    } else {
      reject(new Error(data.message || '请求失败'));
    }
  } else if (statusCode === 401) {
    handleUnauthorized();
    reject(new Error('登录已过期'));
  } else if (statusCode >= 500) {
    reject(new Error('服务器错误，请稍后重试'));
  } else {
    reject(new Error(`请求失败(${statusCode})`));
  }
}

/**
 * 处理未授权
 */
function handleUnauthorized() {
  const app = getApp();
  const hadLoginCredential = Boolean(
    wx.getStorageSync('token') || (app.globalData && app.globalData.token)
  );

  // 先清理登录态；后续并发返回的 401 会因为已无凭证而静默结束。
  wx.removeStorageSync('token');
  wx.removeStorageSync('userInfo');
  app.globalData.token = null;
  app.globalData.userInfo = null;

  if (!hadLoginCredential || unauthorizedModalVisible || unauthorizedRedirecting) {
    return;
  }

  unauthorizedModalVisible = true;
  wx.showModal({
    title: '提示',
    content: '登录已过期，请重新登录',
    showCancel: false,
    success: () => {
      const pages = getCurrentPages();
      const currentPage = pages[pages.length - 1];
      if (currentPage && currentPage.route === 'pages/profile/profile') {
        return;
      }

      unauthorizedRedirecting = true;
      wx.reLaunch({
        url: '/pages/profile/profile',
        complete: () => {
          unauthorizedRedirecting = false;
        }
      });
    },
    complete: () => {
      unauthorizedModalVisible = false;
    }
  });
}

function getRequestHost(requestUrl) {
  const match = String(requestUrl || '').match(/^https?:\/\/([^/]+)/i);
  return match ? match[1] : '未知地址';
}

function createNetworkError(err, requestUrl) {
  const message = String((err && err.errMsg) || '网络请求失败');
  if (message.includes('url not in domain list')) {
    return new Error(`微信拦截了接口地址：${getRequestHost(requestUrl)}`);
  }
  if (message.includes('timeout')) {
    return new Error('连接后端超时，请确认手机与电脑处于同一局域网');
  }
  if (message.includes('connect error') || message.includes('ECONNREFUSED')) {
    return new Error('无法连接后端，请检查局域网地址、服务端口和防火墙');
  }
  return new Error(message);
}

/**
 * 统一请求方法
 * @param {Object} options 请求配置
 * @returns {Promise}
 */
function request(options) {
  return new Promise((resolve, reject) => {
    // 应用请求拦截器
    const requestOptions = requestInterceptor({
      ...options,
      url: `${getBaseUrl()}${options.url}`,
      method: options.method || 'GET',
      data: options.data || {},
      header: {
        'Content-Type': 'application/json',
        ...options.header
      },
      timeout: TIMEOUT,
      requestOptions: options // 保存原始配置供响应拦截器使用
    });

    let retryCount = 0;
    const maxRetries = options.retryTimes || 2;

    function doRequest() {
      wx.request({
        ...requestOptions,
        success(res) {
          res.requestOptions = options; // 传递原始配置
          responseInterceptor(res, resolve, reject);
        },
        fail(err) {
          console.error('[API请求失败]', requestOptions.url, err);
          const message = String((err && err.errMsg) || '');
          const nonRetryable = message.includes('url not in domain list');
          // 自动重试机制
          retryCount++;
          if (!nonRetryable && retryCount <= maxRetries) {
            console.log(`请求重试 ${retryCount}/${maxRetries}:`, options.url);
            setTimeout(doRequest, 1000 * retryCount);
          } else {
            if (options.loading !== false) {
              wx.hideLoading();
            }
            reject(createNetworkError(err, requestOptions.url));
          }
        }
      });
    }

    doRequest();
  });
}

/**
 * GET请求
 */
function get(url, data, options = {}) {
  return request({ url, method: 'GET', data, ...options });
}

/**
 * POST请求
 */
function post(url, data, options = {}) {
  return request({ url, method: 'POST', data, ...options });
}

/**
 * PUT请求
 */
function put(url, data, options = {}) {
  return request({ url, method: 'PUT', data, ...options });
}

/**
 * DELETE请求
 */
function del(url, options = {}) {
  return request({ url, method: 'DELETE', ...options });
}

module.exports = {
  request,
  get,
  post,
  put,
  delete: del
};
