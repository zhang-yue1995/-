import request from '@/utils/request'

export function uploadReportFile(file) {
  const data = new FormData()
  data.append('file', file)
  return request({
    url: '/files/upload',
    method: 'post',
    data,
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000
  })
}
