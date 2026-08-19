const { uploadImage, uploadDocument } = require('../../services/upload');
const { getReportList } = require('../../services/report');
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
    recentReports: [],
    loading: false
  },

  onLoad() {
    // 数据统一在 onShow 中刷新，避免首次进入时 onLoad/onShow 重复请求。
  },

  onShow() {
    // 每次显示时刷新列表
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({
        selected: 0
      });
    }
    if (wx.getStorageSync('token')) {
      this.loadRecentReports();
    } else {
      this.setData({ recentReports: [], loading: false });
    }
  },

  // 下拉刷新
  onPullDownRefresh() {
    this.loadRecentReports().then(() => {
      wx.stopPullDownRefresh();
    });
  },

  // 选择图片进行OCR扫描
  chooseImage() {
    if (!this.ensureLoggedIn()) return;
    wx.showActionSheet({
      itemList: ['从相册多选报表照片', '使用摄像头逐张扫描'],
      success: res => {
        if (res.tapIndex === 0) this.chooseFromAlbum();
        else this.scanWithCamera([]);
      }
    });
  },

  chooseFromAlbum() {
    wx.chooseMedia({
      count: 9,
      mediaType: ['image'],
      sourceType: ['album'],
      success: res => this.processImageBatch((res.tempFiles || []).map(item => item.tempFilePath)),
      fail: err => console.error('选择图片失败', err)
    });
  },

  scanWithCamera(paths) {
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sourceType: ['camera'],
      success: res => {
        const nextPaths = paths.concat((res.tempFiles || []).map(item => item.tempFilePath));
        wx.showModal({
          title: '本张扫描完成',
          content: '是否继续扫描其他报表？',
          confirmText: '继续扫描',
          cancelText: '扫描完成',
          success: modal => modal.confirm ? this.scanWithCamera(nextPaths) : this.processImageBatch(nextPaths)
        });
      },
      fail: err => {
        if (paths.length) this.processImageBatch(paths);
        else console.error('拍摄失败', err);
      }
    });
  },

  async processImageBatch(paths) {
    if (!paths.length) return;
    wx.showLoading({ title: `处理 0/${paths.length}`, mask: true });
    try {
      const uploads = [];
      const tasks = [];
      for (let index = 0; index < paths.length; index++) {
        wx.showLoading({ title: `处理 ${index + 1}/${paths.length}`, mask: true });
        const uploadRes = await uploadImage(paths[index]);
        uploads.push(uploadRes.data);
        const taskRes = await startRecognition(uploadRes.data.id);
        tasks.push(await this.waitForRecognition(taskRes.data.id));
      }
      const taskId = tasks.length > 1
        ? (await mergeRecognitionTasks(tasks.map(item => item.id))).data.id
        : tasks[0].id;
      wx.hideLoading();
      wx.navigateTo({
        url: `/pages/archive/archive?fileId=${uploads[0].id}&taskId=${taskId}&type=image`
      });
    } catch (error) {
      wx.hideLoading();
      wx.showToast({ title: error.message || '图片识别失败', icon: 'none' });
    }
  },

  waitForRecognition(taskId) {
    return new Promise((resolve, reject) => {
      const poll = async () => {
        try {
          const response = await getRecognitionStatus(taskId);
          const task = response.data || {};
          if (task.taskStatus === 'COMPLETED') return resolve(task);
          if (task.taskStatus === 'FAILED' || task.taskStatus === 'CANCELLED') {
            return reject(new Error(task.errorMessage || '识别失败'));
          }
          setTimeout(poll, 800);
        } catch (error) {
          reject(error);
        }
      };
      poll();
    });
  },

  // 选择 PDF 或 Excel 财务报表原件
  chooseDocument() {
    if (!this.ensureLoggedIn()) return;
    wx.chooseMessageFile({
      count: 1,
      type: 'file',
      extension: ['pdf', 'xls', 'xlsx'],
      success: (res) => {
        const selectedFile = res.tempFiles && res.tempFiles[0];
        if (!selectedFile || !selectedFile.path) {
          wx.showToast({ title: '未获取到报表文件', icon: 'none' });
          return;
        }
        const extension = String(selectedFile.name || selectedFile.path)
          .split('.').pop().toLowerCase();
        if (!['pdf', 'xls', 'xlsx'].includes(extension)) {
          wx.showToast({ title: '仅支持PDF、XLS、XLSX', icon: 'none' });
          return;
        }
        if (selectedFile.size > 30 * 1024 * 1024) {
          wx.showToast({ title: '文件不能超过30MB', icon: 'none' });
          return;
        }
        const tempFilePath = selectedFile.path;
        this.uploadAndRecognize(tempFilePath, extension);
      },
      fail: (err) => {
        console.error('选择报表文件失败', err);
      }
    });
  },

  ensureLoggedIn() {
    if (wx.getStorageSync('token')) return true;
    wx.showModal({
      title: '请先登录',
      content: '登录后即可上传并识别财务报表',
      confirmText: '去登录',
      success: (res) => {
        if (res.confirm) {
          wx.switchTab({ url: '/pages/profile/profile' });
        }
      }
    });
    return false;
  },

  // 上传并开始识别
  async uploadAndRecognize(filePath, fileType) {
    wx.showLoading({ title: '正在上传...', mask: true });

    try {
      let uploadRes;
      if (fileType === 'image') {
        uploadRes = await uploadImage(filePath);
      } else {
        uploadRes = await uploadDocument(filePath, fileType);
      }

      wx.hideLoading();

      // 跳转到进度页面
      wx.navigateTo({
        url: `/pages/progress/progress?fileId=${uploadRes.data.id}&type=${fileType}`
      });
    } catch (error) {
      wx.hideLoading();
      wx.showToast({
        title: error.message || '上传失败',
        icon: 'none'
      });
    }
  },

  // 加载最近报表
  async loadRecentReports() {
    this.setData({ loading: true });

    try {
      const res = await getReportList({ pageNum: 1, pageSize: 5 });
      const reportList = res.data && res.data.list ? res.data.list : [];
      const recentReports = reportList.map(item => ({
        id: item.archiveId,
        enterpriseName: item.enterpriseName,
        period: item.reportPeriod,
        healthScore: item.healthScore == null ? '-' : item.healthScore,
        riskLevelType: item.riskLevel || 'NORMAL',
        riskLevelText: RISK_LABELS[item.riskLevel] || '未评分'
      }));
      this.setData({ recentReports });
    } catch (error) {
      console.error('加载报表失败', error);
      wx.showToast({
        title: '加载失败',
        icon: 'none'
      });
    } finally {
      this.setData({ loading: false });
    }
  },

  // 查看报表详情
  viewReport(e) {
    const { id } = e.currentTarget.dataset;
    wx.navigateTo({
      url: `/pages/statements/statements?id=${id}`
    });
  },

  goToArchiveList() {
    wx.switchTab({
      url: '/pages/archive-list/archive-list'
    });
  }
});
