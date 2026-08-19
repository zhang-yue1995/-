const { startRecognition, getRecognitionStatus } = require('../../services/ocr');

Page({
  data: {
    fileId: '',
    fileType: '',
    progress: 0,
    currentStepText: '准备中...',
    animating: true,
    steps: [
      { title: '文件上传', desc: '正在上传文件到服务器', status: 'pending' },
      { title: 'OCR识别', desc: '智能识别报表内容', status: 'pending' },
      { title: '字段提取', desc: '提取关键财务数据', status: 'pending' },
      { title: '数据标准化', desc: '转换标准格式', status: 'pending' }
    ],
    taskId: null,
    pollTimer: null
  },

  onLoad(options) {
    const { fileId, type } = options;
    this.setData({
      fileId: fileId || '',
      fileType: type || 'image'
    });

    // 开始识别流程
    this.startRecognitionProcess();
  },

  onUnload() {
    // 清除定时器
    if (this.data.pollTimer) {
      clearInterval(this.data.pollTimer);
    }
  },

  // 开始识别流程
  async startRecognitionProcess() {
    try {
      // 步骤1：开始识别
      this.updateStepStatus(0, 'active');
      this.setData({ currentStepText: '正在上传文件...' });

      const res = await startRecognition(this.data.fileId);
      this.setData({ taskId: res.data.id });

      // 步骤1完成
      this.updateStepStatus(0, 'completed');
      this.updateStepStatus(1, 'active');
      this.setData({ currentStepText: '正在进行OCR识别...', progress: 25 });

      // 开始轮询状态
      this.startPolling();
    } catch (error) {
      console.error('启动识别失败:', error);
      wx.showToast({
        title: error.message || '启动失败',
        icon: 'none'
      });
    }
  },

  // 轮询识别状态
  startPolling() {
    this.data.pollTimer = setInterval(async () => {
      try {
        const res = await getRecognitionStatus(this.data.taskId);
        const { taskStatus, recognizedFields, totalFields } = res.data;
        if (taskStatus === 'PROCESSING') {
          this.updateStepStatus(1, 'completed');
          this.updateStepStatus(2, 'active');
          this.setData({
            currentStepText: '正在提取字段数据...',
            progress: totalFields ? Math.max(50, Math.round(recognizedFields * 100 / totalFields)) : 60
          });
        }

        if (taskStatus === 'COMPLETED') {
          this.updateStepStatus(2, 'completed');
          this.updateStepStatus(3, 'active');
          this.setData({ currentStepText: '正在标准化处理...', progress: 90 });
          this.completeRecognition();
        } else if (taskStatus === 'FAILED' || taskStatus === 'CANCELLED') {
          throw new Error(res.data.errorMessage || '识别失败');
        }
      } catch (error) {
        console.error('轮询失败:', error);
        if (this.data.pollTimer) {
          clearInterval(this.data.pollTimer);
        }
        wx.showToast({
          title: error.message || '识别出错',
          icon: 'none'
        });
      }
    }, 1000);
  },

  // 更新步骤状态
  updateStepStatus(index, status) {
    const steps = this.data.steps;
    if (steps[index]) {
      steps[index].status = status;
      this.setData({ steps });
    }
  },

  // 完成识别，跳转到建档页面
  completeRecognition() {
    // 停止动画和轮询
    this.setData({ animating: false });
    if (this.data.pollTimer) {
      clearInterval(this.data.pollTimer);
    }

    // 标记所有步骤完成
    this.updateStepStatus(3, 'completed');
    this.setData({ progress: 100, currentStepText: '识别完成！' });

    // 延迟跳转
    setTimeout(() => {
      wx.redirectTo({
        url: `/pages/archive/archive?fileId=${this.data.fileId}&type=${this.data.fileType}&taskId=${this.data.taskId}`
      });
    }, 800);
  }
});
