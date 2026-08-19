App({
  globalData: {
    userInfo: null,
    token: null,
    currentReport: null, // 当前处理的报表
  },

  onLaunch() {
    this.configureDeviceDebug();

    // 旧版本允许把接口地址写入本地存储，可能残留 127.0.0.1 并覆盖新配置。
    // 当前版本统一由 config.js 管理地址，启动时清理该遗留项。
    wx.removeStorageSync('apiBaseUrl');

    // 检查登录状态
    this.checkLoginStatus();

    // 获取系统信息
    const systemInfo = wx.getSystemInfoSync();
    this.globalData.systemInfo = systemInfo;

    console.log('鑫速录小程序启动', { systemInfo });
  },

  configureDeviceDebug() {
    try {
      const accountInfo = wx.getAccountInfoSync();
      const envVersion = accountInfo.miniProgram.envVersion;
      if (typeof wx.setEnableDebug === 'function') {
        wx.setEnableDebug({
          // 仅开发版开启；体验版和正式版显式关闭，避免调试状态残留而显示 vConsole。
          enableDebug: envVersion === 'develop',
          fail(error) {
            console.warn('无法配置手机端调试模式', error);
          }
        });
      }
    } catch (error) {
      console.warn('无法识别小程序运行环境', error);
    }
  },

  checkLoginStatus() {
    const token = wx.getStorageSync('token');
    if (token) {
      this.globalData.token = token;
      this.verifyToken(token);
    }
  },

  verifyToken(token) {
    const { get } = require('./services/request');
    get('/auth/userinfo', {}, { loading: false, retryTimes: 0 })
      .then(res => {
        this.globalData.userInfo = res.data;
        wx.setStorageSync('userInfo', res.data);
      })
      .catch(() => {
        wx.removeStorageSync('token');
        wx.removeStorageSync('userInfo');
        this.globalData.token = null;
        this.globalData.userInfo = null;
      });
  },

  // 显示加载中
  showLoading(title = '加载中...') {
    wx.showLoading({
      title,
      mask: true
    });
  },

  // 隐藏加载中
  hideLoading() {
    wx.hideLoading();
  },

  // 显示提示
  showToast(title, icon = 'none') {
    wx.showToast({
      title,
      icon,
      duration: 2000
    });
  }
});
