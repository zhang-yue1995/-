// 文件上传封装
const { getBaseUrl, TIMEOUT } = require('./api');

let currentUploadTask = null;

/**
 * 上传文件
 * @param {string} filePath 文件路径
 * @param {Object} formData 额外表单数据
 * @param {Function} onProgress 进度回调
 * @returns {Promise}
 */
function uploadFile(filePath, formData = {}, onProgress) {
  return new Promise((resolve, reject) => {
    const token = wx.getStorageSync('token');

    currentUploadTask = wx.uploadFile({
      url: `${getBaseUrl()}/files/upload`,
      filePath: filePath,
      name: 'file',
      formData: {
        ...formData,
        token: token || ''
      },
      header: {
        'Authorization': token ? `Bearer ${token}` : ''
      },
      timeout: TIMEOUT,
      success(res) {
        try {
          const data = JSON.parse(res.data);
          if (res.statusCode < 200 || res.statusCode >= 300) {
            reject(new Error(data.message || `上传失败（HTTP ${res.statusCode}）`));
            return;
          }
          if (data.code === 200 || data.code === 0 || data.success) {
            resolve(data);
          } else {
            reject(new Error(data.message || '上传失败'));
          }
        } catch (e) {
          reject(new Error(res.statusCode
            ? `服务器响应异常（HTTP ${res.statusCode}）`
            : '解析上传响应失败'));
        }
      },
      fail(err) {
        reject(new Error(err.errMsg || '上传失败'));
      }
    });

    // 监听上传进度
    if (onProgress && currentUploadTask) {
      currentUploadTask.onProgressUpdate((res) => {
        onProgress({
          progress: res.progress,
          bytesSent: res.totalBytesSent,
          totalBytes: res.totalBytesExpectedToSend
        });
      });
    }
  });
}

/**
 * 上传图片并OCR识别
 * @param {string} filePath 图片路径
 * @returns {Promise}
 */
function uploadImage(filePath) {
  return uploadFile(filePath, { type: 'image' });
}

/**
 * 上传财务报表文档（PDF / XLS / XLSX）
 * @param {string} filePath 文档路径
 * @param {string} fileType 文件类型
 * @returns {Promise}
 */
function uploadDocument(filePath, fileType = 'file') {
  return uploadFile(filePath, { type: fileType });
}

/**
 * 取消当前上传任务
 */
function cancelUpload() {
  if (currentUploadTask) {
    currentUploadTask.abort();
    currentUploadTask = null;
    return true;
  }
  return false;
}

module.exports = {
  uploadFile,
  uploadImage,
  uploadDocument,
  cancelUpload
};
