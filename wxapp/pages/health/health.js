const { getHealthScore, getIndicators } = require('../../services/report');

Page({
  data: {
    reportId: '',
    healthData: {},
    dimensions: [],
    keyMetrics: [],
    totalIndicators: 0,
    riskLevelType: 'NORMAL',
    riskLevelText: '基本健康'
  },

  onLoad(options) {
    if (options.id) {
      this.setData({ reportId: options.id });
      this.loadHealthData();
    }
  },

  // 加载健康度数据
  async loadHealthData() {
    try {
      const [healthRes, indicatorRes] = await Promise.all([
        getHealthScore(this.data.reportId),
        getIndicators(this.data.reportId)
      ]);
      const healthData = healthRes.data || {};
      const indicators = indicatorRes.data || {};

      // 设置风险等级
      let riskLevelType = 'NORMAL';
      let riskLevelText = '基本健康';
      if (healthData.totalScore >= 80) {
        riskLevelType = 'HEALTH';
        riskLevelText = '健康';
      } else if (healthData.totalScore >= 60) {
        riskLevelType = 'NORMAL';
        riskLevelText = '基本健康';
      } else if (healthData.totalScore >= 40) {
        riskLevelType = 'WARNING';
        riskLevelText = '需关注';
      } else {
        riskLevelType = 'DANGER';
        riskLevelText = '高风险';
      }

      const dimensionDefinitions = [
        ['偿债能力', healthData.solvencyScore, healthData.solvencyWeight],
        ['盈利能力', healthData.profitabilityScore, healthData.profitabilityWeight],
        ['运营效率', healthData.operationScore, healthData.operationWeight],
        ['现金流能力', healthData.cashFlowScore, healthData.cashFlowWeight],
        ['成长能力', healthData.growthScore, healthData.growthWeight]
      ];
      const dimensions = dimensionDefinitions.map(item => {
        const score = Number(item[1] || 0);
        const status = score >= 80 ? 'GOOD' : score >= 60 ? 'AVERAGE'
          : score >= 40 ? 'BELOW_AVERAGE' : 'WEAK';
        return {
          name: item[0],
          score: score.toFixed(2),
          maxScore: 100,
          percent: score,
          weight: item[2],
          status,
          statusClass: this.getStatusClass(status),
          statusText: this.getStatusText(status)
        };
      });
      const keyMetrics = [
        this.buildMetric('资产负债率', indicators.debtToAssetRatio, '%', 60, true),
        this.buildMetric('流动比率', indicators.currentRatio, 'x', 2, false),
        this.buildMetric('销售净利率', indicators.netProfitMargin, '%', 10, false),
        this.buildMetric(
          '经营现金/收入',
          indicators.operatingCashToRevenue == null
            ? null
            : Number(indicators.operatingCashToRevenue) * 100,
          '%',
          10,
          false
        )
      ];

      this.setData({
        healthData,
        dimensions,
        keyMetrics,
        totalIndicators: Object.keys(indicators).filter(key => indicators[key] != null).length,
        riskLevelType,
        riskLevelText
      });

    } catch (error) {
      console.error('加载健康数据失败:', error);
      wx.showToast({
        title: '加载失败',
        icon: 'none'
      });
    }
  },

  // 获取状态样式类
  getStatusClass(status) {
    const map = {
      'WEAK': 'WEAK',
      'BELOW_AVERAGE': 'BELOW_AVERAGE',
      'AVERAGE': 'AVERAGE',
      'GOOD': 'GOOD',
      'EXCELLENT': 'GOOD'
    };
    return map[status] || 'AVERAGE';
  },

  // 获取状态文本
  getStatusText(status) {
    const map = {
      'WEAK': '弱',
      'BELOW_AVERAGE': '偏低',
      'AVERAGE': '一般',
      'GOOD': '良好',
      'EXCELLENT': '优秀'
    };
    return map[status] || '一般';
  },

  buildMetric(name, rawValue, unit, target, lowerIsBetter) {
    const value = rawValue == null ? null : Number(rawValue);
    const favorable = value != null && (lowerIsBetter ? value <= target : value >= target);
    const dangerous = value == null || (lowerIsBetter ? value >= target * 1.5 : value <= 0);
    return {
      name,
      value: value == null ? '不可计算' : `${value.toFixed(2)}${unit}`,
      percent: value == null ? 0 : Math.max(0, Math.min(100, lowerIsBetter
        ? 100 - value / Math.max(target * 1.5, 1) * 100
        : value / Math.max(target, 1) * 70)),
      color: favorable ? '#20a96b' : dangerous ? '#e35d6a' : '#f3a83b',
      status: favorable ? '正常' : dangerous ? '高风险' : '需关注'
    };
  },

  // 查看分析报告
  viewReport() {
    wx.navigateTo({
      url: `/pages/report/report?id=${this.data.reportId}`
    });
  },

  // 下拉刷新
  onPullDownRefresh() {
    this.loadHealthData().then(() => {
      wx.stopPullDownRefresh();
    });
  }
});
