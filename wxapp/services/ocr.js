// OCR相关服务
const { get, post } = require('./request');

/**
 * 发起识别请求
 * @param {string} fileId 文件ID
 * @returns {Promise}
 */
function startRecognition(fileId) {
  return post(`/ocr/recognize-async?fileId=${encodeURIComponent(fileId)}`, {});
}

/**
 * 获取识别进度
 * @param {string} taskId 任务ID
 * @returns {Promise}
 */
function getRecognitionStatus(taskId) {
  return get(`/ocr/tasks/${taskId}`, {}, { loading: false, retryTimes: 0 });
}

/**
 * 获取识别结果
 * @param {string} taskId 任务ID
 * @returns {Promise}
 */
function getRecognitionResults(taskId) {
  return get(`/ocr/tasks/${taskId}/results`);
}

function mergeRecognitionTasks(taskIds) {
  return post('/ocr/tasks/merge', taskIds);
}

module.exports = {
  startRecognition,
  getRecognitionStatus,
  getRecognitionResults,
  mergeRecognitionTasks
};
