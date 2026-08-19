import request from '@/utils/request'

export function getAuditLogList(params) {
  return request({
    url: '/audit-logs',
    method: 'get',
    params
  })
}

export function exportAuditLogs(params) {
  return request({
    url: '/audit-logs/export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}
