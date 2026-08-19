Page({
  data: {},

  onLoad(options) {
    // 可以接收错误信息参数
    if (options.message) {
      console.log('错误信息:', options.message);
    }
  },

  // 重新上传
  reupload() {
    wx.navigateBack({
      delta: 1,
      fail: () => {
        // 如果无法返回，跳转到首页
        wx.switchTab({
          url: '/pages/index/index'
        });
      }
    });
  },

  // 查看支持的格式
  viewSupportedFormats() {
    wx.showModal({
      title: '支持的报表样式',
      content: '支持的文件类型：\n\n• 图片：JPG、PNG、JPEG\n• 文档：PDF（单页/多页）\n• 表格：XLS、XLSX\n\n支持的报表类型：\n• 资产负债表\n• 利润表\n• 现金流量表\n• 财务指标汇总表\n\n允许只提供其中一张或两张报表，缺少的表及字段将保留为空，供人工复核。\n\n建议：\n• 图片清晰度≥300DPI\n• 避免反光或遮挡\n• 保持表格完整',
      showCancel: false,
      confirmText: '我知道了',
      confirmColor: '#0e8f78'
    });
  }
});
