const { getAnalysisReport } = require('../../services/report');
const { getBaseUrl } = require('../../services/api');

Page({
  data: {
    reportId: '',
    reportData: {}
  },

  onLoad(options) {
    if (options.id) {
      this.setData({ reportId: options.id });
      this.loadReport();
    }
  },

  // 加载分析报告
  async loadReport() {
    try {
      const res = await getAnalysisReport(this.data.reportId);
      const raw = res.data || {};
      const splitLines = text => String(text || '').split(/\r?\n|；/)
        .map(item => item.replace(/^[-•\d.、\s]+/, '').trim())
        .filter(Boolean);
      const reportData = {
        ...raw,
        generatedAt: raw.createdTime,
        overallConclusion: raw.overallAssessment || raw.executiveSummary,
        mainRisks: splitLines(raw.riskAnalysis).map((desc, index) => ({
          level: index === 0 ? '严重' : '关注',
          title: `风险提示 ${index + 1}`,
          desc
        })),
        suggestions: splitLines(raw.improvementSuggestions),
        disclaimer: '本报告基于企业提供的财务数据自动生成，仅供业务分析参考，不构成审计、授信审批或投资建议。'
      };

      this.setData({ reportData });

    } catch (error) {
      console.error('加载报告失败:', error);
      wx.showToast({
        title: '加载失败',
        icon: 'none'
      });
    }
  },

  // 下载服务器生成的真实 PDF，并以业务可识别的文件名保存到小程序目录。
  exportPdf() {
    wx.showModal({
      title: '导出报告',
      content: '确认下载当前智能分析报告 PDF？',
      success: (res) => {
        if (res.confirm) {
          const token = wx.getStorageSync('token');
          wx.showLoading({ title: '正在生成...' });
          wx.downloadFile({
            url: `${getBaseUrl()}/analysis-reports/export/${this.data.reportId}`,
            header: token ? { Authorization: `Bearer ${token}` } : {},
            success: downloadRes => {
              wx.hideLoading();
              if (downloadRes.statusCode !== 200) {
                wx.showToast({ title: '导出失败', icon: 'none' });
                return;
              }
              const safe = value => String(value || '').replace(/[\\/:*?"<>|]/g, '_');
              const filename = `${safe(this.data.reportData.enterpriseName || '企业')}_${safe(this.data.reportData.reportPeriod || '未标注期间')}_报表分析.pdf`;
              const targetPath = `${wx.env.USER_DATA_PATH}/${filename}`;
              const openPdf = filePath => wx.openDocument({
                filePath,
                fileType: 'pdf',
                showMenu: true,
                fail: () => wx.showToast({ title: '无法打开PDF文件', icon: 'none' })
              });
              const fileSystem = wx.getFileSystemManager();
              const copyPdf = () => fileSystem.copyFile({
                srcPath: downloadRes.tempFilePath,
                destPath: targetPath,
                success: () => openPdf(targetPath),
                fail: copyError => {
                  const exists = String(copyError && copyError.errMsg || '').includes('exist');
                  if (!exists) {
                    openPdf(downloadRes.tempFilePath);
                    return;
                  }
                  fileSystem.unlink({
                    filePath: targetPath,
                    complete: () => fileSystem.copyFile({
                      srcPath: downloadRes.tempFilePath,
                      destPath: targetPath,
                      success: () => openPdf(targetPath),
                      fail: () => openPdf(downloadRes.tempFilePath)
                    })
                  });
                }
              });
              copyPdf();
            },
            fail: () => {
              wx.hideLoading();
              wx.showToast({ title: '导出失败', icon: 'none' });
            }
          });
        }
      }
    });
  },

  // 分享
  onShareAppMessage() {
    return {
      title: `财务分析报告 - ${this.data.reportData.enterpriseName || ''}`,
      path: `/pages/report/report?id=${this.data.reportId}`
    };
  },

  // 下拉刷新
  onPullDownRefresh() {
    this.loadReport().then(() => {
      wx.stopPullDownRefresh();
    });
  }
});
