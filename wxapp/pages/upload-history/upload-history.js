const { getUploadRecords, deleteUploadRecord } = require('../../services/upload-record');

const STATUS_LABELS = {
  DRAFT: '待复核',
  PENDING_REVIEW: '待审批',
  APPROVED: '已入库',
  REVIEWED: '待提交',
  REJECTED: '已退回'
};

Page({
  data: {
    pendingOnly: false,
    enterpriseName: '',
    uploadDate: '',
    records: [],
    loading: false
  },

  onLoad(options) {
    this.setData({ pendingOnly: options.status === 'pending' });
  },

  onShow() {
    if (wx.getStorageSync('token')) this.loadRecords();
  },

  onPullDownRefresh() {
    this.loadRecords().finally(() => wx.stopPullDownRefresh());
  },

  onCompanyInput(e) {
    this.setData({ enterpriseName: e.detail.value });
  },

  onDateChange(e) {
    this.setData({ uploadDate: e.detail.value });
    this.loadRecords();
  },

  clearDate() {
    this.setData({ uploadDate: '' });
    this.loadRecords();
  },

  search() {
    this.loadRecords();
  },

  async loadRecords() {
    this.setData({ loading: true });
    try {
      const res = await getUploadRecords({
        pageNum: 1,
        pageSize: 50,
        enterpriseName: this.data.enterpriseName.trim(),
        uploadDate: this.data.uploadDate,
        status: this.data.pendingOnly ? 'PENDING' : ''
      });
      const list = res.data && res.data.list ? res.data.list : [];
      const records = list.map(item => ({
        ...item,
        enterpriseName: item.enterpriseName || '尚未完成归档',
        reportPeriod: item.reportPeriod || '-',
        statusText: STATUS_LABELS[item.filingStatus] || '处理中',
        pending: ['DRAFT', 'REVIEWED', 'REJECTED'].includes(item.filingStatus),
        fileSummary: Number(item.fileCount || 0) > 1
          ? `同一次上传 · ${item.fileCount}个原件`
          : item.fileName
      }));
      this.setData({ records });
    } catch (error) {
      wx.showToast({ title: error.message || '记录加载失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  openRecord(e) {
    const item = e.currentTarget.dataset.item;
    if (item.pending) {
      if (!item.archiveId || !item.ocrTaskId) {
        wx.showToast({ title: '识别任务尚未准备完成', icon: 'none' });
        return;
      }
      wx.navigateTo({
        url: `/pages/review/review?reportId=${item.archiveId}&taskId=${item.ocrTaskId}&fileId=${item.fileId}`
      });
      return;
    }
    if (item.archiveId) {
      wx.navigateTo({ url: `/pages/statements/statements?id=${item.archiveId}` });
    } else {
      wx.showToast({ title: '该文件尚未完成归档', icon: 'none' });
    }
  },

  removeRecord(e) {
    const fileId = e.currentTarget.dataset.id;
    wx.showModal({
      title: '删除上传记录',
      content: '删除后，本次上传的全部原件、对应报表和待复核任务将不再展示。是否继续？',
      confirmColor: '#e35d6a',
      success: async result => {
        if (!result.confirm) return;
        try {
          await deleteUploadRecord(fileId);
          wx.showToast({ title: '已删除', icon: 'success' });
          this.loadRecords();
        } catch (error) {
          wx.showToast({ title: error.message || '删除失败', icon: 'none' });
        }
      }
    });
  }
});
