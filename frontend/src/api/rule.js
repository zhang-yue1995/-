import request from '@/utils/request'

export function getIndicatorRules(params) {
  return request({ url: '/indicator-rules', method: 'get', params })
}

export function createIndicatorRule(data) {
  return request({ url: '/indicator-rules', method: 'post', data })
}

export function updateIndicatorRule(id, data) {
  return request({ url: `/indicator-rules/${id}`, method: 'put', data })
}

export function deleteIndicatorRule(id) {
  return request({ url: `/indicator-rules/${id}`, method: 'delete' })
}

export function getHealthWeights() {
  return request({ url: '/indicator-rules/weights', method: 'get' })
}

export function updateHealthWeights(data) {
  return request({ url: '/indicator-rules/weights', method: 'put', data })
}
