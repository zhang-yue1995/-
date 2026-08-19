import request from '@/utils/request'

export function getSystemHealth() {
  return request({
    url: '/health',
    method: 'get',
    showLoading: false
  })
}
