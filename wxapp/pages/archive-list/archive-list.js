const { getEnterpriseList, getEnterpriseReports, getEnterpriseByCreditCode } = require('../../services/enterprise');
const { getDashboardStats } = require('../../services/dashboard');
const { getReportList } = require('../../services/report');
const { uploadImage, uploadDocument } = require('../../services/upload');
const { startRecognition, getRecognitionStatus, mergeRecognitionTasks } = require('../../services/ocr');

const RISK_LABELS = {
  NORMAL: '基本健康',
  HEALTHY: '健康',
  ATTENTION: '需关注',
  WARNING: '需关注',
  DANGEROUS: '高风险',
  CRITICAL: '高风险'
};

Page({
  data: {
    searchKeyword: '',
    enterprises: [],
    filteredEnterprises: [],
    loading: false,
    refreshing: false,
    stats: {
      newReports: 0,
      pendingReview: 0
    },
    showAddModal: false,
    addStep: 'info',
    addProcessing: false,
    duplicateNotice: '',
    addDraft: {
      enterpriseName: '', creditCode: '', period: '', reportType: 'MONTHLY',
      unit: '万元', currency: 'CNY', managerName: ''
    }
  },

  onShow() {
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({
        selected: 1
      });
    }
    this.loadEnterprises();
  },

  // 加载企业列表
  async loadEnterprises() {
    this.setData({ loading: true });

    try {
      const [enterpriseRes, statsRes, reportRes] = await Promise.all([
        getEnterpriseList({
          pageNum: 1,
          pageSize: 50,
          keyword: this.data.searchKeyword
        }),
        getDashboardStats(),
        getReportList({
          pageNum: 1,
          pageSize: 500,
          keyword: this.data.searchKeyword
        }).catch(error => {
          console.warn('加载报表期列表失败，先展示企业摘要:', error);
          return { data: { list: [] } };
        })
      ]);
      const enterpriseList = enterpriseRes.data && enterpriseRes.data.list
        ? enterpriseRes.data.list
        : [];
      const reportList = reportRes.data && Array.isArray(reportRes.data.list)
        ? reportRes.data.list
        : [];
      const reportsByEnterprise = reportList.reduce((grouped, report) => {
        const enterpriseId = String(report.enterpriseId || '');
        if (!enterpriseId) return grouped;
        if (!grouped[enterpriseId]) grouped[enterpriseId] = [];
        grouped[enterpriseId].push(report);
        return grouped;
      }, {});
      const enterprises = enterpriseList.map(item => {
        const seenPeriods = new Set();
        const periodReports = (reportsByEnterprise[String(item.id)] || [])
          .slice()
          .sort((left, right) => {
            const periodCompare = String(right.reportPeriod || '').localeCompare(String(left.reportPeriod || ''));
            if (periodCompare !== 0) return periodCompare;
            return String(right.createdTime || '').localeCompare(String(left.createdTime || ''));
          })
          .filter(report => {
            const period = String(report.reportPeriod || '');
            if (!period || seenPeriods.has(period)) return false;
            seenPeriods.add(period);
            return true;
          })
          .map(report => ({
            reportId: report.archiveId,
            period: report.reportPeriod
          }));

        return {
          ...item,
          riskLevel: item.latestRiskLevel || 'UNSCORED',
          riskLevelText: RISK_LABELS[item.latestRiskLevel] || '未评分',
          latestReport: item.latestReportPeriod || item.lastReportDate || '-',
          reportCount: Math.max(Number(item.reportCount || 0), periodReports.length),
          periodReports,
          healthScore: item.latestHealthScore,
          hasHealthScore: item.latestHealthScore !== null && item.latestHealthScore !== undefined,
          managerName: item.managerName || '-'
        };
      });
      const dashboard = statsRes.data || {};

      this.setData({
        enterprises,
        filteredEnterprises: enterprises,
        stats: {
          newReports: dashboard.monthlyNewReports || 0,
          pendingReview: dashboard.pendingReview || 0
        },
        loading: false
      });

    } catch (error) {
      console.error('加载企业列表失败:', error);
      this.setData({ loading: false });
      wx.showToast({
        title: '加载失败',
        icon: 'none'
      });
    }
  },

  // 搜索输入
  onSearchInput(e) {
    const keyword = e.detail.value;
    this.setData({ searchKeyword: keyword });

    // 实时过滤
    if (!keyword.trim()) {
      this.setData({ filteredEnterprises: this.data.enterprises });
    } else {
      const filtered = this.data.enterprises.filter(item =>
        item.name.includes(keyword) ||
        item.creditCode.includes(keyword)
      );
      this.setData({ filteredEnterprises: filtered });
    }
  },

  // 执行搜索
  doSearch() {
    this.loadEnterprises();
  },

  // 下拉刷新
  onRefresh() {
    this.setData({ refreshing: true });
    this.loadEnterprises().then(() => {
      this.setData({ refreshing: false });
    });
  },

  // 跳转到新增页面
  goToAdd() {
    const userInfo = wx.getStorageSync('userInfo') || {};
    this.setData({
      showAddModal: true,
      addStep: 'info',
      duplicateNotice: '',
      addDraft: {
        enterpriseName: '', creditCode: '', period: '', reportType: 'MONTHLY',
        unit: '万元', currency: 'CNY', managerName: userInfo.realName || userInfo.username || ''
      }
    });
  },

  closeAddModal() {
    if (!this.data.addProcessing) this.setData({ showAddModal: false, addStep: 'info' });
  },

  preventModalClose() {},

  onAddInput(e) {
    const field = e.currentTarget.dataset.field;
    this.setData({ [`addDraft.${field}`]: e.detail.value });
    if (field === 'creditCode') {
      clearTimeout(this._addCreditTimer);
      this._addCreditTimer = setTimeout(() => this.checkAddCreditCode(e.detail.value), 350);
    }
  },

  onAddPeriodChange(e) { this.setData({ 'addDraft.period': e.detail.value }); },

  async checkAddCreditCode(rawCode) {
    const creditCode = String(rawCode || '').trim().toUpperCase();
    this.setData({ 'addDraft.creditCode': creditCode, duplicateNotice: '' });
    if (creditCode.length !== 18) return;
    try {
      const response = await getEnterpriseByCreditCode(creditCode);
      if (response.data) {
        this.setData({
          'addDraft.enterpriseName': response.data.name,
          duplicateNotice: '当前企业已归档，将沿用库内企业名称并新增报表期'
        });
      }
    } catch (error) { console.warn('企业唯一性校验失败', error); }
  },

  showUploadStep() {
    const draft = this.data.addDraft;
    if (!String(draft.enterpriseName || '').trim()) return wx.showToast({ title: '请输入企业名称', icon: 'none' });
    if (!/^[0-9A-Z]{18}$/.test(String(draft.creditCode || '').trim().toUpperCase())) {
      return wx.showToast({ title: '请输入18位统一社会信用代码', icon: 'none' });
    }
    if (!/^\d{4}-\d{2}$/.test(String(draft.period || ''))) {
      return wx.showToast({ title: '请选择报表期间', icon: 'none' });
    }
    this.setData({ addStep: 'upload' });
  },

  backToAddInfo() { if (!this.data.addProcessing) this.setData({ addStep: 'info' }); },

  chooseAddAlbum() {
    wx.chooseMedia({ count: 9, mediaType: ['image'], sourceType: ['album'],
      success: res => this.processAddImages((res.tempFiles || []).map(item => item.tempFilePath)) });
  },

  startAddCamera() { this.scanAddCamera([]); },

  scanAddCamera(paths) {
    wx.chooseMedia({
      count: 1, mediaType: ['image'], sourceType: ['camera'],
      success: res => {
        const nextPaths = paths.concat((res.tempFiles || []).map(item => item.tempFilePath));
        wx.showModal({ title: '本张扫描完成', content: '是否继续扫描其他报表？',
          confirmText: '继续扫描', cancelText: '扫描完成',
          success: modal => modal.confirm ? this.scanAddCamera(nextPaths) : this.processAddImages(nextPaths) });
      },
      fail: () => { if (paths.length) this.processAddImages(paths); }
    });
  },

  chooseAddDocument() {
    wx.chooseMessageFile({ count: 1, type: 'file', extension: ['pdf', 'xls', 'xlsx'], success: res => {
      const selected = res.tempFiles && res.tempFiles[0];
      if (!selected || !selected.path) return;
      if (selected.size > 30 * 1024 * 1024) return wx.showToast({ title: '文件不能超过30MB', icon: 'none' });
      const type = String(selected.name || selected.path).split('.').pop().toLowerCase();
      this.processAddDocument(selected.path, type);
    }});
  },

  async processAddImages(paths) {
    if (!paths.length || this.data.addProcessing) return;
    this.setData({ addProcessing: true });
    try {
      const uploads = [], tasks = [];
      for (let index = 0; index < paths.length; index++) {
        wx.showLoading({ title: `识别 ${index + 1}/${paths.length}`, mask: true });
        const upload = await uploadImage(paths[index]);
        uploads.push(upload.data);
        const started = await startRecognition(upload.data.id);
        tasks.push(await this.waitForAddRecognition(started.data.id));
      }
      const taskId = tasks.length > 1 ? (await mergeRecognitionTasks(tasks.map(task => task.id))).data.id : tasks[0].id;
      this.finishAddRecognition(uploads[0].id, taskId, 'image');
    } catch (error) { wx.showToast({ title: error.message || '图片识别失败', icon: 'none' }); }
    finally { wx.hideLoading(); this.setData({ addProcessing: false }); }
  },

  async processAddDocument(path, type) {
    if (this.data.addProcessing) return;
    this.setData({ addProcessing: true });
    wx.showLoading({ title: '上传并识别...', mask: true });
    try {
      const upload = await uploadDocument(path, type);
      const started = await startRecognition(upload.data.id);
      const task = await this.waitForAddRecognition(started.data.id);
      this.finishAddRecognition(upload.data.id, task.id, type);
    } catch (error) { wx.showToast({ title: error.message || '文件识别失败', icon: 'none' }); }
    finally { wx.hideLoading(); this.setData({ addProcessing: false }); }
  },

  waitForAddRecognition(taskId) {
    return new Promise((resolve, reject) => {
      const poll = async () => {
        try {
          const response = await getRecognitionStatus(taskId), task = response.data || {};
          if (task.taskStatus === 'COMPLETED') return resolve(task);
          if (['FAILED', 'CANCELLED'].includes(task.taskStatus)) return reject(new Error(task.errorMessage || '识别失败'));
          setTimeout(poll, 800);
        } catch (error) { reject(error); }
      };
      poll();
    });
  },

  finishAddRecognition(fileId, taskId, type) {
    const draft = encodeURIComponent(JSON.stringify(this.data.addDraft));
    this.setData({ showAddModal: false });
    wx.navigateTo({ url: `/pages/archive/archive?fileId=${fileId}&taskId=${taskId}&type=${type}&draft=${draft}` });
  },

  // 查看企业详情
  async viewEnterprise(e) {
    const { id } = e.currentTarget.dataset;
    try {
      const res = await getEnterpriseReports(id, { pageNum: 1, pageSize: 1 });
      const reportList = res.data && res.data.list ? res.data.list : [];
      const latest = reportList[0];
      if (!latest) {
        wx.showToast({ title: '该企业暂无报表', icon: 'none' });
        return;
      }
      wx.navigateTo({
        url: `/pages/statements/statements?id=${latest.archiveId}&enterpriseId=${id}`
      });
    } catch (error) {
      wx.showToast({ title: error.message || '报表加载失败', icon: 'none' });
    }
  },

  // 从档案卡片直接打开指定报表期
  viewReportPeriod(e) {
    const { reportId, enterpriseId } = e.currentTarget.dataset;
    if (!reportId) return;
    wx.navigateTo({
      url: `/pages/statements/statements?id=${reportId}&enterpriseId=${enterpriseId}`
    });
  },

  onUnload() { clearTimeout(this._addCreditTimer); }
});
