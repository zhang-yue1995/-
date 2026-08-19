const { getStatements, getReportDetail } = require('../../services/report');
const { getEnterpriseReports } = require('../../services/enterprise');
const { formatMoney } = require('../../utils/format');
const { generateAnalysisReport, submitAnalysisApproval } = require('../../services/report');

Page({
  data: {
    reportId: '',
    enterpriseId: '',
    taskId: '',
    loading: false,
    periodOptions: [],
    periodIndex: 0,
    periodSwitchable: false,
    currentTab: 'BALANCE_SHEET',
    tabs: [
      { label: '资产负债表', value: 'BALANCE_SHEET' },
      { label: '利润表', value: 'INCOME_STATEMENT' },
      { label: '现金流量表', value: 'CASH_FLOW_STATEMENT' }
    ],
    reportInfo: {},
    riskLevelType: 'NORMAL',
    riskLevelText: '基本健康',
    filingStatus: '',
    approvalStatus: '',
    canSubmitApproval: false,
    approvalSubmitting: false,
    statementData: {
      BALANCE_SHEET: [],
      INCOME_STATEMENT: [],
      CASH_FLOW_STATEMENT: []
    },
    currentData: []
  },

  onLoad(options) {
    if (options.id) {
      this.setData({
        reportId: options.id,
        enterpriseId: options.enterpriseId || ''
      });
      this.loadStatements({ refreshPeriods: true });
    }
  },

  // 加载报表数据
  async loadStatements(options = {}) {
    const reportId = options.reportId || this.data.reportId;
    if (!reportId) return;

    const loadSequence = (this._loadSequence || 0) + 1;
    this._loadSequence = loadSequence;
    this.setData({ loading: true });
    wx.showNavigationBarLoading();

    try {
      const [statementRes, detailRes] = await Promise.all([
        getStatements(reportId),
        getReportDetail(reportId)
      ]);
      if (loadSequence !== this._loadSequence) return;

      const data = statementRes.data || {};
      const detail = detailRes.data || {};
      const mapRow = (name, value1, value2, value3, isTotal) => ({
        name,
        value1,
        value2,
        value3,
        formattedValue1: value1 == null ? '-' : formatMoney(value1),
        formattedValue2: value2 == null ? '-' : formatMoney(value2),
        formattedValue3: value3 == null ? '-' : formatMoney(value3),
        isTotal,
        level: 0
      });
      const balanceItems = data.balanceSheet && data.balanceSheet.items
        ? data.balanceSheet.items
        : [];
      const incomeItems = data.incomeStatement && data.incomeStatement.items
        ? data.incomeStatement.items
        : [];
      const cashItems = data.cashFlowStatement && data.cashFlowStatement.items
        ? data.cashFlowStatement.items
        : [];
      const balanceRows = balanceItems.map(item =>
        mapRow(item.itemName, item.endingBalance, item.beginningBalance, null, item.isTotalRow === 1));
      const incomeRows = incomeItems.map(item =>
        mapRow(item.itemName, item.currentPeriodAmount, item.previousPeriodAmount,
          item.monthlyAmount, item.isTotalRow === 1));
      const cashRows = cashItems.map(item => {
        const currentAmount = item.currentPeriodAmount == null ? item.amount : item.currentPeriodAmount;
        return mapRow(item.itemName, currentAmount, item.previousPeriodAmount,
          item.monthlyAmount, item.isTotalRow === true);
      });

      // 设置风险等级
      let riskLevelType = 'NORMAL';
      let riskLevelText = '基本健康';
      const healthScore = Number(data.healthScore == null ? (detail.healthScore || 0) : data.healthScore);
      if (healthScore >= 80) {
        riskLevelType = 'HEALTH';
        riskLevelText = '健康';
      } else if (healthScore >= 60) {
        riskLevelType = 'NORMAL';
        riskLevelText = '基本健康';
      } else if (healthScore >= 40) {
        riskLevelType = 'WARNING';
        riskLevelText = '需关注';
      } else {
        riskLevelType = 'DANGER';
        riskLevelText = '高风险';
      }

      const statementData = {
        BALANCE_SHEET: balanceRows,
        INCOME_STATEMENT: incomeRows,
        CASH_FLOW_STATEMENT: cashRows
      };
      const currentTab = this.data.currentTab;

      this.setData({
        reportId,
        enterpriseId: detail.enterpriseId || this.data.enterpriseId,
        reportInfo: {
          enterpriseName: data.enterpriseName || detail.enterpriseName,
          period: data.reportPeriod || detail.reportPeriod,
          unit: data.unit || detail.unit
        },
        taskId: detail.ocrTaskId || '',
        filingStatus: detail.filingStatus || '',
        approvalStatus: detail.approvalStatus || '',
        canSubmitApproval: ['REVIEWED', 'REJECTED'].includes(String(detail.filingStatus || '').toUpperCase())
          && String(detail.approvalStatus || '').toLowerCase() !== 'pending_approval',
        riskLevelType,
        riskLevelText,
        statementData,
        currentData: statementData[currentTab] || balanceRows
      });

      if (options.refreshPeriods || this.data.periodOptions.length === 0) {
        await this.loadPeriodOptions(detail.enterpriseId || this.data.enterpriseId, reportId);
      }

    } catch (error) {
      console.error('加载报表失败:', error);
      wx.showToast({
        title: error.message || '加载失败',
        icon: 'none'
      });
    } finally {
      if (loadSequence === this._loadSequence) {
        this.setData({ loading: false });
        wx.hideNavigationBarLoading();
      }
    }
  },

  // 加载当前企业的全部报表期，供详情页直接切换
  async loadPeriodOptions(enterpriseId, currentReportId) {
    if (!enterpriseId) return;

    try {
      const res = await getEnterpriseReports(enterpriseId, {
        pageNum: 1,
        pageSize: 500
      });
      const reportList = res.data && Array.isArray(res.data.list) ? res.data.list : [];
      const seenPeriods = new Set();
      const periodOptions = reportList
        .slice()
        .sort((left, right) => {
          const periodCompare = String(right.reportPeriod || '').localeCompare(String(left.reportPeriod || ''));
          if (periodCompare !== 0) return periodCompare;
          return String(right.createdTime || '').localeCompare(String(left.createdTime || ''));
        })
        .filter(item => {
          const key = `${item.reportPeriod || ''}-${item.archiveId}`;
          if (!item.archiveId || seenPeriods.has(key)) return false;
          seenPeriods.add(key);
          return true;
        })
        .map(item => ({
          reportId: item.archiveId,
          label: item.reportPeriod || '未标注期间'
        }));

      if (periodOptions.length === 0) {
        periodOptions.push({
          reportId: currentReportId,
          label: this.data.reportInfo.period || '当前报表'
        });
      }

      const selectedIndex = periodOptions.findIndex(
        item => String(item.reportId) === String(currentReportId)
      );
      this.setData({
        periodOptions,
        periodIndex: selectedIndex >= 0 ? selectedIndex : 0,
        periodSwitchable: periodOptions.length > 1
      });
    } catch (error) {
      console.warn('加载企业报表期失败:', error);
      this.setData({
        periodOptions: [{
          reportId: currentReportId,
          label: this.data.reportInfo.period || '当前报表'
        }],
        periodIndex: 0,
        periodSwitchable: false
      });
    }
  },

  // 切换同一企业下的报表期
  onPeriodChange(e) {
    const periodIndex = Number(e.detail.value);
    const selected = this.data.periodOptions[periodIndex];
    if (!selected) return;

    this.setData({ periodIndex });
    if (String(selected.reportId) === String(this.data.reportId)) return;

    this.loadStatements({
      reportId: selected.reportId,
      refreshPeriods: false
    });
  },

  // 切换Tab
  switchTab(e) {
    const tabValue = e.currentTarget.dataset.value;

    this.setData({
      currentTab: tabValue,
      currentData: this.data.statementData[tabValue] || []
    });
  },

  // 格式化数值
  formatValue(value) {
    if (value === null || value === undefined) return '-';
    return formatMoney(value);
  },

  // 编辑字段
  editFields() {
    if (!this.data.taskId) {
      wx.showToast({ title: '该报表没有可复核的OCR任务', icon: 'none' });
      return;
    }
    wx.navigateTo({
      url: `/pages/review/review?reportId=${this.data.reportId}&taskId=${this.data.taskId}`
    });
  },

  // 跳转到校验页面
  goToValidation() {
    wx.navigateTo({
      url: `/pages/validation/validation?id=${this.data.reportId}`
    });
  },

  async submitForApproval() {
    if (!this.data.canSubmitApproval || this.data.approvalSubmitting) return;
    this.setData({ approvalSubmitting: true });
    try {
      await generateAnalysisReport(this.data.reportId);
      await submitAnalysisApproval(this.data.reportId);
      wx.showToast({ title: '已提交后台终审', icon: 'success' });
      this.setData({ canSubmitApproval: false, filingStatus: 'PENDING_REVIEW', approvalStatus: 'pending_approval' });
    } catch (error) {
      wx.showToast({ title: error.message || '提交审批失败', icon: 'none' });
    } finally {
      this.setData({ approvalSubmitting: false });
    }
  }
});
