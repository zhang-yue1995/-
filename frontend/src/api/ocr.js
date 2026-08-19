import request from '@/utils/request'

/**
 * 上传文件进行OCR识别
 * @param {File} file - 要识别的文件
 */
export function recognizeFile(fileId) {
  return request({
    url: '/ocr/recognize',
    method: 'post',
    params: { fileId },
    timeout: 60000 // OCR任务可能需要更长时间
  })
}

/**
 * 获取OCR任务状态
 * @param {String} taskId - 任务ID
 */
export function getTaskStatus(taskId) {
  return request({
    url: `/ocr/tasks/${taskId}`,
    method: 'get'
  })
}

/**
 * 获取OCR任务结果
 * @param {String} taskId - 任务ID
 */
export function getTaskResults(taskId) {
  return request({
    url: `/ocr/tasks/${taskId}/results`,
    method: 'get'
  })
}

export function mergeOcrTasks(taskIds) {
  return request({ url: '/ocr/tasks/merge', method: 'post', data: taskIds, timeout: 60000 })
}

export function getOcrTaskList(params) {
  return request({
    url: '/ocr/tasks',
    method: 'get',
    params
  })
}
