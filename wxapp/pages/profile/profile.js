const { login, logout, getUserInfo } = require('../../services/auth');
const { getDashboardStats } = require('../../services/dashboard');

Page({
  data: {
    userInfo: {},
    loggedIn: false,
    username: '',
    password: '',
    loggingIn: false,
    kpiData: {
      pendingReview: 0,
      pendingExceptions: 0
    }
  },

  onShow() {
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({
        selected: 3
      });
    }

    this.loadUserInfo();
  },

  // 加载用户信息
  loadUserInfo() {
    const raw = getUserInfo();
    if (!raw || !wx.getStorageSync('token')) {
      this.setData({ loggedIn: false, userInfo: {} });
      return;
    }
    this.setData({
      loggedIn: true,
      userInfo: {
        ...raw,
        name: raw.realName || raw.username,
        department: '鑫速录',
        role: raw.role === 'ADMIN' ? '系统管理员' : '客户经理',
        avatar: String(raw.realName || raw.username || '鑫').slice(0, 1)
      }
    });
    getDashboardStats().then(res => {
      const data = res.data || {};
      this.setData({
        kpiData: {
          pendingReview: data.pendingReview || 0,
          pendingExceptions: data.highRiskEnterprises || 0
        }
      });
    }).catch(() => {});
  },

  onLoginInput(e) {
    this.setData({ [e.currentTarget.dataset.field]: e.detail.value });
  },

  async submitLogin() {
    if (!this.data.username || !this.data.password) {
      wx.showToast({ title: '请输入用户名和密码', icon: 'none' });
      return;
    }
    this.setData({ loggingIn: true });
    try {
      await login(this.data.username.trim(), this.data.password);
      this.setData({ password: '' });
      this.loadUserInfo();
      wx.showToast({ title: '登录成功', icon: 'success' });
    } catch (error) {
      wx.showToast({ title: error.message || '登录失败', icon: 'none' });
    } finally {
      this.setData({ loggingIn: false });
    }
  },

  logoutAccount() {
    logout();
    this.setData({ loggedIn: false, userInfo: {}, username: '', password: '' });
  },

  // 跳转到待复核列表
  goToReviewList() {
    wx.navigateTo({
      url: '/pages/upload-history/upload-history?status=pending'
    });
  },

  // 跳转到异常列表
  goToExceptionList() {
    wx.navigateTo({
      url: '/pages/validation/validation'
    });
  },

  // 上传记录
  goToUploadHistory() {
    wx.navigateTo({ url: '/pages/upload-history/upload-history' });
  },

  // 报告导出
  goToExportReport() {
    wx.navigateTo({ url: '/pages/analysis-select/analysis-select' });
  },

  // OCR设置
  goToOCRSettings() {
    wx.showModal({
      title: 'OCR识别设置',
      content: '识别引擎由服务端统一配置。正式环境请在部署配置中启用经授权的OCR服务。',
      showCancel: false,
      confirmText: '我知道了'
    });
  },

  // 安全设置
  goToSecuritySettings() {
    wx.showModal({
      title: '数据安全与隐私',
      content: '• 所有数据传输采用HTTPS加密\n• 敏感信息本地加密存储\n• 符合金融行业数据安全标准\n• 支持数据导出和删除\n\n您的数据安全是我们的首要任务。',
      showCancel: false,
      confirmText: '我知道了',
      confirmColor: '#0e8f78'
    });
  },

  // 分享
  onShareAppMessage() {
    return {
      title: '鑫速录 - 企业财务报表智能填报',
      path: '/pages/index/index'
    };
  }
});
