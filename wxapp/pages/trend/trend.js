const { getTrendData } = require('../../services/trend');
const { getEnterpriseList } = require('../../services/enterprise');

Page({
  data: {
    enterpriseId: '',
    enterprises: [],
    enterpriseNames: [],
    enterpriseIndex: 0,
    currentYearIndex: 0,
    years: [],
    periodsLength: 0,
    trendData: {},
    trendCards: [],
    summaryCards: [],
    warnings: [],
    loading: false
  },

  onLoad(options) {
    if (options.enterpriseId) {
      this.setData({ enterpriseId: String(options.enterpriseId) });
    }
  },

  // Tab 页面只会触发一次 onLoad。每次进入都刷新，确保新上传的报表立即进入趋势序列。
  onShow() {
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({ selected: 2 });
    }
    if (!wx.getStorageSync('token')) {
      this.resetTrendData();
      return;
    }
    this.loadEnterprisesAndTrend();
  },

  async loadEnterprisesAndTrend() {
    if (this.data.loading) return;
    this.setData({ loading: true });
    try {
      const response = await getEnterpriseList({ pageNum: 1, pageSize: 100 });
      const list = response.data && response.data.list ? response.data.list : [];
      // 优先展示已有报表的企业；多期企业排在前面，同时仍允许用户手动切换。
      const enterprises = list
        .filter(item => Number(item.reportCount || 0) > 0)
        .sort((left, right) => {
          const countDiff = Number(right.reportCount || 0) - Number(left.reportCount || 0);
          if (countDiff !== 0) return countDiff;
          return String(right.latestReportPeriod || '').localeCompare(String(left.latestReportPeriod || ''));
        });

      if (!enterprises.length) {
        this.setData({
          enterprises: [],
          enterpriseNames: [],
          enterpriseId: '',
          enterpriseIndex: 0
        });
        this.resetTrendData();
        return;
      }

      let enterpriseIndex = enterprises.findIndex(
        item => String(item.id) === String(this.data.enterpriseId)
      );
      if (enterpriseIndex < 0) enterpriseIndex = 0;
      const enterpriseId = String(enterprises[enterpriseIndex].id);
      this.setData({
        enterprises,
        enterpriseNames: enterprises.map(item => item.name),
        enterpriseIndex,
        enterpriseId
      });
      await this.loadTrendData();
    } catch (error) {
      console.error('加载企业及趋势数据失败:', error);
      this.resetTrendData();
      wx.showToast({ title: error.message || '趋势数据加载失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  async loadTrendData() {
    const enterpriseId = this.data.enterpriseId;
    if (!enterpriseId) {
      this.resetTrendData();
      return;
    }

    try {
      const [debtRes, grossMarginRes] = await Promise.all([
        getTrendData(enterpriseId, { indicatorCode: 'debtToAssetRatio', periods: 36 }),
        getTrendData(enterpriseId, { indicatorCode: 'grossProfitMargin', periods: 36 })
      ]);
      // 用户快速切换企业时，丢弃上一个企业较晚返回的数据。
      if (String(this.data.enterpriseId) !== String(enterpriseId)) return;

      this._debtItems = debtRes.data && debtRes.data.dataList ? debtRes.data.dataList : [];
      this._grossItems = grossMarginRes.data && grossMarginRes.data.dataList
        ? grossMarginRes.data.dataList
        : [];

      const allItems = this._debtItems.concat(this._grossItems);
      const yearValues = Array.from(new Set(allItems.map(item =>
        String(item.reportDate || item.reportPeriod || '').slice(0, 4)
      ).filter(Boolean))).sort().reverse();
      const years = yearValues.length > 1 ? ['全部'].concat(yearValues) : yearValues;
      const currentYear = this.data.years[this.data.currentYearIndex];
      let currentYearIndex = years.indexOf(currentYear);
      if (currentYearIndex < 0) currentYearIndex = 0;
      this.setData({ years, currentYearIndex });
      this.applyTrendView();
    } catch (error) {
      console.error('加载趋势数据失败:', error);
      this.resetTrendData();
      wx.showToast({ title: error.message || '趋势数据加载失败', icon: 'none' });
    }
  },

  applyTrendView() {
    const selectedYear = this.data.years[this.data.currentYearIndex];
    const filterByYear = item => !selectedYear || selectedYear === '全部'
      || String(item.reportDate || item.reportPeriod || '').startsWith(selectedYear);
    const debtItems = (this._debtItems || []).filter(filterByYear);
    const grossItems = (this._grossItems || []).filter(filterByYear);
    const periods = Array.from(new Set(debtItems.concat(grossItems)
      .map(item => item.reportPeriod).filter(Boolean)));

    if (!periods.length) {
      this.setData({
        trendData: { periods: [] },
        periodsLength: 0,
        trendCards: [],
        summaryCards: [],
        warnings: []
      });
      return;
    }

    const debtCard = this.buildTrendCard('资产负债率', debtItems, '#e35d6a');
    const grossCard = this.buildTrendCard('销售毛利率', grossItems, '#20a96b');
    const trendCards = [debtCard, grossCard];
    const latestDebtItem = this.latestValidItem(debtItems);
    const latestDebt = latestDebtItem ? Number(latestDebtItem.value) : null;
    const warnings = latestDebt != null && latestDebt >= 70 ? [{
      time: latestDebtItem.reportPeriod,
      metric: '资产负债率',
      current: `${latestDebt.toFixed(2)}%`,
      threshold: '70%',
      trend: '偿债压力偏高',
      level: latestDebt >= 100 ? 'HIGH' : 'MEDIUM'
    }] : [];

    this.setData({
      trendData: { periods },
      periodsLength: periods.length,
      trendCards,
      summaryCards: trendCards.map(item => ({
        name: item.name,
        value: item.value,
        change: item.change,
        color: item.color
      })),
      warnings
    });
  },

  buildTrendCard(name, items, color) {
    const validItems = items.filter(item => item.value !== null && item.value !== undefined
      && item.value !== '' && Number.isFinite(Number(item.value)));
    const latest = validItems.length ? validItems[validItems.length - 1] : null;
    const previous = validItems.length > 1 ? validItems[validItems.length - 2] : null;
    const latestValue = latest ? Number(latest.value) : null;
    const previousValue = previous ? Number(previous.value) : null;
    const change = latest && latest.changeRate !== null && latest.changeRate !== undefined
      ? `${Number(latest.changeRate) >= 0 ? '+' : ''}${Number(latest.changeRate).toFixed(1)}%`
      : '暂无环比';
    return {
      name,
      value: latestValue == null ? '-' : `${latestValue.toFixed(1)}%`,
      chartData: validItems.map(item => Number(item.value)),
      change,
      direction: latestValue != null && previousValue != null && latestValue < previousValue
        ? 'down'
        : 'up',
      firstPeriod: validItems.length ? validItems[0].reportPeriod : '',
      latestPeriod: latest ? latest.reportPeriod : '',
      color
    };
  },

  latestValidItem(items) {
    const valid = items.filter(item => item.value !== null && item.value !== undefined
      && item.value !== '' && Number.isFinite(Number(item.value)));
    return valid.length ? valid[valid.length - 1] : null;
  },

  resetTrendData() {
    this._debtItems = [];
    this._grossItems = [];
    this.setData({
      years: [],
      currentYearIndex: 0,
      periodsLength: 0,
      trendData: { periods: [] },
      trendCards: [],
      summaryCards: [],
      warnings: []
    });
  },

  onEnterpriseChange(e) {
    const enterpriseIndex = Number(e.detail.value);
    const enterprise = this.data.enterprises[enterpriseIndex];
    if (!enterprise) return;
    this.setData({
      enterpriseIndex,
      enterpriseId: String(enterprise.id),
      years: [],
      currentYearIndex: 0
    });
    this.loadTrendData();
  },

  onYearChange(e) {
    this.setData({ currentYearIndex: Number(e.detail.value) });
    this.applyTrendView();
  },

  onPullDownRefresh() {
    this.loadEnterprisesAndTrend().then(() => wx.stopPullDownRefresh());
  },

  onShareAppMessage() {
    return {
      title: '财务指标趋势分析',
      path: `/pages/trend/trend?enterpriseId=${this.data.enterpriseId || ''}`
    };
  }
});
