const { submitReview, generateAnalysisReport, submitAnalysisApproval } = require('../../services/report');
const { getRecognitionResults } = require('../../services/ocr');
const { getBaseUrl } = require('../../services/api');
const { getArchiveFiles } = require('../../services/upload-record');

Page({
  data: {
    reportId: '',
    taskId: '',
    fileId: '',
    currentTab: 'BALANCE_SHEET',
    tabs: [
      { label: '资产负债表', value: 'BALANCE_SHEET' },
      { label: '利润表', value: 'INCOME_STATEMENT' },
      { label: '现金流量表', value: 'CASH_FLOW_STATEMENT' }
    ],
    allFields: [],
    filteredFields: [],
    lowConfidenceCount: 0,
    lowConfidenceIndex: -1,
    focusedFieldId: '',
    amountHeaders: ['期末余额', '年初余额'],
    submitting: false,
    reviewCompleted: false,
    approvalSubmitting: false,

    // 编辑相关
    showEditModal: false,
    editingField: {},
    editingValues: { primary: '', secondary: '', tertiary: '' }
  },

  onLoad(options) {
    if (options.reportId) {
      this.setData({ reportId: options.reportId });
    }
    this.setData({
      taskId: options.taskId || '',
      fileId: options.fileId || ''
    });

    if (!this.data.taskId || !this.data.reportId) {
      wx.showToast({ title: '请选择一条待复核记录', icon: 'none' });
      setTimeout(() => {
        wx.redirectTo({ url: '/pages/upload-history/upload-history?status=pending' });
      }, 500);
      return;
    }
    this.loadFields();
  },

  // 加载字段数据
  async loadFields() {
    try {
      const res = await getRecognitionResults(this.data.taskId);
      const fields = (res.data.fieldResults || []).map(field => ({
        id: field.id,
        originalName: field.fieldName,
        originalValue: field.fieldValue,
        originalSecondaryValue: field.secondaryValue || '',
        originalTertiaryValue: field.tertiaryValue || '',
        standardizedValue: field.reviewedValue == null ? field.fieldValue : field.reviewedValue,
        standardizedSecondaryValue: field.secondaryValue || '',
        standardizedTertiaryValue: field.tertiaryValue || '',
        confidence: Number(field.confidenceScore || 0),
        confidenceLevel: this.getConfidenceLevel(Number(field.confidenceScore || 0)),
        sheet: field.fieldType,
        isModified: Number(field.isReviewed || 0) === 1
      }));

      // 统计低置信度字段数量
      const lowConfidenceCount = fields.filter(f => f.confidence < 90).length;

      // 按当前tab过滤
      const filteredFields = fields.filter(f => f.sheet === this.data.currentTab);

      this.setData({
        allFields: fields,
        filteredFields,
        lowConfidenceCount
      });

    } catch (error) {
      console.error('加载字段失败:', error);
      wx.showToast({
        title: '加载数据失败',
        icon: 'none'
      });
    }
  },

  // 切换Tab
  switchTab(e) {
    const tabValue = e.currentTarget.dataset.value;

    // 过滤字段
    const filteredFields = this.data.allFields.filter(f => f.sheet === tabValue);

    this.setData({
      currentTab: tabValue,
      filteredFields,
      amountHeaders: this.getAmountHeaders(tabValue),
      focusedFieldId: ''
    });
  },

  getAmountHeaders(sheet) {
    return sheet === 'BALANCE_SHEET'
      ? ['期末余额', '年初余额']
      : ['本期金额', '上期金额', '本月金额'];
  },

  jumpToNextLowConfidence() {
    const lowFields = this.data.allFields.filter(field => field.confidence < 90);
    if (!lowFields.length) return;
    const nextIndex = (this.data.lowConfidenceIndex + 1) % lowFields.length;
    const target = lowFields[nextIndex];
    this.setData({
      lowConfidenceIndex: nextIndex,
      currentTab: target.sheet,
      amountHeaders: this.getAmountHeaders(target.sheet),
      filteredFields: this.data.allFields.filter(field => field.sheet === target.sheet),
      focusedFieldId: target.id
    }, () => {
      wx.pageScrollTo({ selector: `#field-${target.id}`, offsetTop: -260, duration: 300 });
      clearTimeout(this._focusTimer);
      this._focusTimer = setTimeout(() => this.setData({ focusedFieldId: '' }), 1800);
    });
  },

  // 获取置信度等级
  getConfidenceLevel(confidence) {
    if (confidence >= 95) return 'high';
    if (confidence >= 70) return 'medium';
    return 'low';
  },

  // 编辑字段
  editField(e) {
    const item = e.currentTarget.dataset.item;

    this.setData({
      showEditModal: true,
      editingField: item,
      editingValues: {
        primary: item.standardizedValue || '',
        secondary: item.standardizedSecondaryValue || '',
        tertiary: item.standardizedTertiaryValue || ''
      }
    });
  },

  // 关闭编辑弹窗
  closeEditModal() {
    this.setData({
      showEditModal: false,
      editingField: {},
      editingValues: { primary: '', secondary: '', tertiary: '' }
    });
  },

  // 输入值变化
  onEditInput(e) {
    const key = e.currentTarget.dataset.key;
    this.setData({ [`editingValues.${key}`]: e.detail.value });
  },

  // 确认编辑
  confirmEdit() {
    const { editingField, editingValues, allFields } = this.data;

    // 更新字段值
    const fieldIndex = allFields.findIndex(f => f.id === editingField.id);
    if (fieldIndex > -1) {
      allFields[fieldIndex].standardizedValue = editingValues.primary;
      allFields[fieldIndex].standardizedSecondaryValue = editingValues.secondary;
      allFields[fieldIndex].standardizedTertiaryValue = editingValues.tertiary;
      allFields[fieldIndex].isModified = true;

      // 重新过滤当前tab的字段
      const filteredFields = allFields.filter(f => f.sheet === this.data.currentTab);

      this.setData({
        allFields,
        filteredFields,
        showEditModal: false,
        editingField: {},
        editingValues: { primary: '', secondary: '', tertiary: '' }
      });

      wx.showToast({
        title: '已修改',
        icon: 'success'
      });
    }
  },

  // 查看原图
    async viewOriginalImage() {
      try {
        const response = await getArchiveFiles(this.data.reportId);
        const files = Array.isArray(response.data) ? response.data : [];
        if (!files.length) {
          wx.showToast({ title: '未找到原始文件', icon: 'none' });
          return;
        }
        const imageFiles = files.filter(file => ['JPG', 'JPEG', 'PNG'].includes(String(file.fileType || '').toUpperCase()));
        if (imageFiles.length === files.length) {
          const token = wx.getStorageSync('token');
          wx.showLoading({ title: `加载原图 0/${files.length}`, mask: true });
          const localPaths = [];
          for (let index = 0; index < imageFiles.length; index++) {
            wx.showLoading({ title: `加载原图 ${index + 1}/${files.length}`, mask: true });
            const localPath = await this.downloadOriginal(imageFiles[index].fileId, token);
            localPaths.push(localPath);
          }
          wx.hideLoading();
          wx.previewImage({ current: localPaths[0], urls: localPaths, showmenu: true });
          return;
        }
        const selected = files.length === 1 ? 0 : await this.chooseOriginalFile(files);
        if (selected < 0) return;
        await this.openOriginalDocument(files[selected]);
      } catch (error) {
        wx.hideLoading();
        wx.showToast({ title: error.message || '原件加载失败', icon: 'none' });
      }
    },

    downloadOriginal(fileId, token) {
      return new Promise((resolve, reject) => wx.downloadFile({
        url: `${getBaseUrl()}/files/${fileId}/content`,
        header: token ? { Authorization: `Bearer ${token}` } : {},
        success: res => res.statusCode === 200 ? resolve(res.tempFilePath) : reject(new Error('文件下载失败')),
        fail: err => reject(new Error(err.errMsg || '文件下载失败'))
      }));
    },

    chooseOriginalFile(files) {
      return new Promise(resolve => wx.showActionSheet({
        itemList: files.map((file, index) => `${index + 1}. ${String(file.fileName || '原件').slice(0, 24)}`),
        success: res => resolve(res.tapIndex),
        fail: () => resolve(-1)
      }));
    },

    async openOriginalDocument(file) {
      const token = wx.getStorageSync('token');
      wx.showLoading({ title: '正在打开...', mask: true });
      const localPath = await this.downloadOriginal(file.fileId, token);
      wx.hideLoading();
      const type = String(file.fileType || '').toLowerCase();
      if (['jpg', 'jpeg', 'png'].includes(type)) {
        wx.previewImage({ current: localPath, urls: [localPath], showmenu: true });
      } else {
        wx.openDocument({ filePath: localPath, fileType: type, showMenu: true,
          fail: () => wx.showToast({ title: '暂不支持预览该格式', icon: 'none' }) });
      }
    },

  // 提交复核
  async submitReview() {
    this.setData({ submitting: true });

    try {
      const fields = this.data.allFields.map(field => ({
        fieldResultId: field.id,
        originalValue: field.originalValue,
        correctedValue: String(field.standardizedValue == null ? '' : field.standardizedValue),
        correctedSecondaryValue: String(field.standardizedSecondaryValue == null ? '' : field.standardizedSecondaryValue),
        correctedTertiaryValue: String(field.standardizedTertiaryValue == null ? '' : field.standardizedTertiaryValue),
        isConfirmedCorrect: !field.isModified,
        confidence: field.confidence,
        reviewComment: field.isModified ? '小程序人工修正' : '小程序人工确认'
      }));
      await submitReview(this.data.reportId, fields);

      wx.showToast({
        title: '提交成功',
        icon: 'success'
      });
      this.setData({ reviewCompleted: true });

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

  returnToReview() {
    this.setData({ reviewCompleted: false });
  },

  async submitForApproval() {
    this.setData({ approvalSubmitting: true });
    try {
      await generateAnalysisReport(this.data.reportId);
      await submitAnalysisApproval(this.data.reportId);
      wx.showToast({ title: '已提交后台终审', icon: 'success' });
    } catch (error) {
      wx.showToast({ title: error.message || '提交审批失败', icon: 'none' });
    } finally {
      this.setData({ approvalSubmitting: false });
    }
  },

  finishAndReturnHome() {
    wx.switchTab({ url: '/pages/index/index' });
  },

  viewStoredReport() {
    wx.redirectTo({
      url: `/pages/statements/statements?id=${this.data.reportId}`
    });
  },

  viewValidationResult() {
    wx.redirectTo({
      url: `/pages/validation/validation?id=${this.data.reportId}`
    });
  },

  onUnload() {
    clearTimeout(this._focusTimer);
  }
});
