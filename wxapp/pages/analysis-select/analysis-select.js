const { getReportList, generateAnalysisReport } = require('../../services/report');

const RISK_LABELS = {
  HEALTHY: '健康',
  NORMAL: '基本健康',
  ATTENTION: '需关注',
  WARNING: '需关注',
  DANGEROUS: '高风险',
  CRITICAL: '严重风险'
};

Page({
  data: {
    reports: [],
    filteredReports: [],
    selectedId: '',
    keyword: '',
    loading: false,
    generating: false
  },

  onLoad() {
    this.loadReports();
  },

  onPullDownRefresh() {
    this.loadReports().finally(() => wx.stopPullDownRefresh());
  },

  async loadReports() {
    this.setData({ loading: true });
    try {
      const res = await getReportList({ pageNum: 1, pageSize: 100 });
      const list = res.data && res.data.list ? res.data.list : [];
      const reports = list.map(item => ({
        ...item,
        riskText: RISK_LABELS[item.riskLevel] || '待评估',
        scoreText: item.healthScore === null || item.healthScore === undefined ? '—' : item.healthScore,
        createdDate: String(item.createdTime || '').slice(0, 10),
        approvalText: item.approvalStatus === 'approved'
          ? '已审批'
          : item.approvalStatus === 'pending_approval' ? '待审批' : '可生成'
      }));
      this.setData({ reports, filteredReports: reports });
      this.applyFilter();
    } catch (error) {
      wx.showToast({ title: error.message || '报表加载失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  onKeywordInput(e) {
    this.setData({ keyword: e.detail.value });
    this.applyFilter();
  },

  applyFilter() {
    const keyword = this.data.keyword.trim().toLowerCase();
    const filteredReports = keyword
      ? this.data.reports.filter(item =>
        String(item.enterpriseName || '').toLowerCase().includes(keyword) ||
        String(item.reportPeriod || '').toLowerCase().includes(keyword))
      : this.data.reports;
    this.setData({ filteredReports });
  },

  selectReport(e) {
    this.setData({ selectedId: String(e.currentTarget.dataset.id) });
  },

  async generatePreview() {
    if (!this.data.selectedId) {
      wx.showToast({ title: '请先选择一份报表', icon: 'none' });
      return;
    }
    this.setData({ generating: true });
    try {
      await generateAnalysisReport(this.data.selectedId);
      wx.navigateTo({ url: `/pages/report/report?id=${this.data.selectedId}&source=analysis-select` });
    } catch (error) {
      wx.showToast({ title: error.message || '报告生成失败', icon: 'none' });
    } finally {
      this.setData({ generating: false });
    }
  }
});
