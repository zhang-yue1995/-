const { getHealthScore, getValidations, generateAnalysisReport } = require('../../services/report');

Page({
  data: {
    reportId: '',
    totalScore: 85,
    riskLevelType: 'NORMAL',
    riskLevelText: '基本健康',
    validationItems: [],
    exceptions: [],
    hasUnhandledExceptions: false
  },

  onLoad(options) {
    if (options.id) {
      this.setData({ reportId: options.id });
      this.loadValidationData();
    }
  },

  // 加载校验数据
  async loadValidationData() {
    try {
      const [healthRes, validationRes] = await Promise.all([
        getHealthScore(this.data.reportId),
        getValidations(this.data.reportId)
      ]);
      const data = healthRes.data || {};
      const validationItems = (validationRes.data || []).map((item, index) => ({
        id: index + 1,
        name: item.checkName,
        desc: item.detail,
        passed: item.passed
      }));
      const exceptions = (validationRes.data || [])
        .filter(item => !item.passed)
        .map((item, index) => ({
          id: index + 1,
          level: 'HIGH',
          levelText: '高',
          name: item.checkName,
          description: `${item.detail}${item.suggestion ? `；${item.suggestion}` : ''}`,
          handled: false
        }));

      let riskLevelType = 'NORMAL';
      let riskLevelText = '基本健康';
      if (data.totalScore >= 80) {
        riskLevelType = 'HEALTH';
        riskLevelText = '健康';
      } else if (data.totalScore >= 60) {
        riskLevelType = 'NORMAL';
        riskLevelText = '基本健康';
      } else if (data.totalScore >= 40) {
        riskLevelType = 'WARNING';
        riskLevelText = '需关注';
      } else {
        riskLevelType = 'DANGER';
        riskLevelText = '高风险';
      }

      this.setData({
        totalScore: data.totalScore || 0,
        riskLevelType,
        riskLevelText,
        validationItems,
        exceptions,
        hasUnhandledExceptions: exceptions.length > 0
      });

    } catch (error) {
      console.error('加载校验数据失败:', error);
      wx.showToast({
        title: '加载数据失败',
        icon: 'none'
      });
    }
  },

  // 处理异常
  handleException(e) {
    const { id } = e.currentTarget.dataset;

    wx.showModal({
      title: '处理异常',
      content: '确认该异常已核实并标记为已处理？',
      success: (res) => {
        if (res.confirm) {
          // 更新异常状态
          const exceptions = this.data.exceptions.map(item => {
            if (item.id === id) {
              return { ...item, handled: true };
            }
            return item;
          });

          // 过滤出未处理的异常
          const unhandled = exceptions.filter(e => !e.handled);

          this.setData({
            exceptions: unhandled,
            hasUnhandledExceptions: unhandled.length > 0
          });

          wx.showToast({
            title: '已标记为已处理',
            icon: 'success'
          });
        }
      }
    });
  },

  // 生成分析报告
  async generateReport() {
    if (this.data.hasUnhandledExceptions) {
      wx.showToast({
        title: '请先处理所有异常',
        icon: 'none'
      });
      return;
    }

    try {
      await generateAnalysisReport(this.data.reportId);
      wx.navigateTo({
        url: `/pages/report/report?id=${this.data.reportId}`
      });
    } catch (error) {
      wx.showToast({ title: error.message || '报告生成失败', icon: 'none' });
    }
  },

  // 下拉刷新
  onPullDownRefresh() {
    this.loadValidationData().then(() => {
      wx.stopPullDownRefresh();
    });
  }
});
