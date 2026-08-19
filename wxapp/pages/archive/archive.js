const { archiveReport } = require('../../services/report');
const { getEnterpriseList, getEnterpriseByCreditCode, createEnterprise } = require('../../services/enterprise');
const { getRecognitionResults } = require('../../services/ocr');
const { required, validateCreditCode, validatePeriod } = require('../../utils/validate');

Page({
  data: {
    fileId: '',
    fileType: '',
    taskId: '',
    sourceReportDate: '',
    formData: {
      enterpriseName: '',
      creditCode: '',
      period: '',
      reportType: 'MONTHLY',
      unit: '万元',
      currency: 'CNY',
      managerName: '',
      remark: ''
    },
    reportTypes: [
      { name: '月报', value: 'MONTHLY' },
      { name: '季报', value: 'QUARTERLY' },
      { name: '年报', value: 'ANNUAL' }
    ],
    reportTypeIndex: 0,
    units: ['元', '千元', '万元'],
    unitIndex: 2,
    currencies: ['人民币(CNY)', '美元(USD)', '欧元(EUR)'],
    currencyIndex: 0,
    submitting: false,
    existingEnterpriseNotice: ''
  },

  onLoad(options) {
    if (options.fileId) {
      this.setData({ fileId: options.fileId });
    }
    if (options.type) {
      this.setData({ fileType: options.type });
    }
    if (options.taskId) {
      this.setData({ taskId: options.taskId });
    }
    if (options.draft) {
      try {
        const draft = JSON.parse(decodeURIComponent(options.draft));
        const updates = { formData: { ...this.data.formData, ...draft } };
        const reportTypeIndex = this.data.reportTypes.findIndex(item => item.value === draft.reportType);
        const unitIndex = this.data.units.indexOf(draft.unit);
        const currencyIndex = ['CNY', 'USD', 'EUR'].indexOf(draft.currency);
        if (reportTypeIndex >= 0) updates.reportTypeIndex = reportTypeIndex;
        if (unitIndex >= 0) updates.unitIndex = unitIndex;
        if (currencyIndex >= 0) updates.currencyIndex = currencyIndex;
        this.setData(updates);
      } catch (error) { console.warn('新增报表草稿解析失败', error); }
    }
    const userInfo = wx.getStorageSync('userInfo') || {};
    if (userInfo.realName || userInfo.username) {
      this.setData({ 'formData.managerName': userInfo.realName || userInfo.username });
    }
    this.prefillFromRecognition();
  },

  async prefillFromRecognition() {
    if (!this.data.taskId) return;
    try {
      const res = await getRecognitionResults(this.data.taskId);
      const metadata = res.data || {};
      const updates = {};
      if (metadata.enterpriseName) updates['formData.enterpriseName'] = metadata.enterpriseName;
      if (metadata.reportPeriod) updates['formData.period'] = metadata.reportPeriod.slice(0, 7);
      if (metadata.reportDate) updates.sourceReportDate = String(metadata.reportDate).slice(0, 10);
      if (metadata.unit) {
        const unitIndex = this.data.units.indexOf(metadata.unit);
        if (unitIndex >= 0) {
          updates.unitIndex = unitIndex;
          updates['formData.unit'] = metadata.unit;
        }
      }
      if (Object.keys(updates).length) this.setData(updates);
    } catch (error) {
      console.warn('报表表头自动读取失败，可继续手工填写：', error);
    }
  },

  // 输入框值变化
  onInputChange(e) {
    const field = e.currentTarget.dataset.field;
    const value = e.detail.value;
    this.setData({
      [`formData.${field}`]: value
    });
    if (field === 'creditCode') {
      clearTimeout(this._creditTimer);
      this._creditTimer = setTimeout(() => this.checkArchivedEnterprise(value), 350);
    }
  },

  async checkArchivedEnterprise(rawCode) {
    const creditCode = String(rawCode || '').trim().toUpperCase();
    this.setData({ 'formData.creditCode': creditCode, existingEnterpriseNotice: '' });
    if (creditCode.length !== 18) return;
    try {
      const response = await getEnterpriseByCreditCode(creditCode);
      if (response.data) {
        this.setData({
          'formData.enterpriseName': response.data.name,
          existingEnterpriseNotice: '当前企业已归档，无需重复操作'
        });
        if (this._lastDuplicateCode !== creditCode) {
          this._lastDuplicateCode = creditCode;
          wx.showModal({
            title: '企业已归档',
            content: '当前企业已归档，无需重复操作',
            showCancel: false,
            confirmText: '知道了'
          });
        }
      }
    } catch (error) {
      console.warn('企业唯一性校验失败', error);
    }
  },

  // 期间选择变化
  onPeriodChange(e) {
    this.setData({
      'formData.period': e.detail.value
    });
  },

  // 报表口径选择变化
  onReportTypeChange(e) {
    const index = parseInt(e.detail.value);
    this.setData({
      reportTypeIndex: index,
      'formData.reportType': this.data.reportTypes[index].value
    });
  },

  // 单位选择变化
  onUnitChange(e) {
    const index = parseInt(e.detail.value);
    this.setData({
      unitIndex: index,
      'formData.unit': this.data.units[index]
    });
  },

  // 币种选择变化
  onCurrencyChange(e) {
    const index = parseInt(e.detail.value);
    this.setData({
      currencyIndex: index,
      'formData.currency': ['CNY', 'USD', 'EUR'][index]
    });
  },

  // 表单验证
  validateForm() {
    const { formData } = this.data;

    // 验证必填项
    let error = required(formData.enterpriseName, '企业名称');
    if (error) return error;

    error = required(formData.creditCode, '统一社会信用代码');
    if (error) return error;

    error = validateCreditCode(formData.creditCode);
    if (error) return error;

    error = required(formData.period, '报表期间');
    if (error) return error;

    error = validatePeriod(formData.period);
    if (error) return error;

    return null;
  },

  // 提交表单
  async submitForm() {
    // 表单验证
    const error = this.validateForm();
    if (error) {
      wx.showToast({ title: error, icon: 'none' });
      return;
    }

    this.setData({ submitting: true });

    try {
      const app = getApp();
      const enterpriseId = await this.resolveEnterpriseId();
      const periodParts = this.data.formData.period.split('-');
      const year = Number(periodParts[0]);
      const month = Number(periodParts[1]);
      const lastDay = new Date(year, month, 0).getDate();
      const submitData = {
        enterpriseId,
        reportPeriod: this.data.formData.period,
        reportDate: this.data.sourceReportDate || `${this.data.formData.period}-${String(lastDay).padStart(2, '0')}`,
        reportType: this.data.formData.reportType,
        year,
        month,
        quarter: this.data.formData.reportType === 'QUARTERLY' ? Math.ceil(month / 3) : null,
        dataSource: 'OCR_AUTO',
        filingStatus: 'DRAFT',
        managerName: this.data.formData.managerName,
        remark: this.data.formData.remark,
        fileId: this.data.fileId,
        ocrTaskId: Number(this.data.taskId)
      };

      const res = await archiveReport(submitData);
      const reportId = res.data;
      app.globalData.currentReport = {
        id: reportId,
        fileId: this.data.fileId,
        taskId: this.data.taskId,
        enterpriseId,
        ...this.data.formData
      };

      wx.showToast({
        title: '保存成功',
        icon: 'success'
      });

      // 跳转到复核页面
      setTimeout(() => {
        wx.redirectTo({
          url: `/pages/review/review?reportId=${reportId}&taskId=${this.data.taskId}&fileId=${this.data.fileId}`
        });
      }, 1500);

    } catch (error) {
      console.error('提交失败:', error);
      wx.showToast({
        title: error.message || '提交失败',
        icon: 'none'
      });
    } finally {
      this.setData({ submitting: false });
    }
  },

  async resolveEnterpriseId() {
    const { enterpriseName, creditCode } = this.data.formData;
    const listRes = await getEnterpriseList({
      pageNum: 1,
      pageSize: 20,
      keyword: creditCode
    });
    const enterpriseList = listRes.data && listRes.data.list ? listRes.data.list : [];
    const existing = enterpriseList.find(item => item.creditCode === creditCode);
    if (existing) return existing.id;

    const createRes = await createEnterprise({
      name: enterpriseName,
      creditCode,
      managerName: this.data.formData.managerName,
      remark: '由小程序OCR建档自动创建'
    });
    return createRes.data.id;
  },

  onUnload() {
    clearTimeout(this._creditTimer);
  }
});
