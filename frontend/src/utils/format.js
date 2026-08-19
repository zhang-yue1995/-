/**
 * 格式化金额（千分位）
 * @param {number} value - 数值
 * @param {number} decimals - 小数位数，默认2位
 * @returns {string} 格式化后的金额字符串
 */
export function formatMoney(value, decimals = 2) {
  if (value === null || value === undefined || isNaN(value)) {
    return '-'
  }

  const num = Number(value)

  if (num === 0) {
    return '0.00'
  }

  const parts = Math.abs(num).toFixed(decimals).split('.')
  parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ',')

  let result = parts.join('.')

  // 负数显示红色标记（返回带样式的值）
  if (num < 0) {
    result = `-${result}`
  }

  return result
}

/**
 * 判断金额是否为负数
 * @param {number} value - 数值
 * @returns {boolean}
 */
export function isNegative(value) {
  return value < 0
}

/**
 * 格式化百分比
 * @param {number} value - 数值（如 0.1234 表示 12.34%）
 * @param {number} decimals - 小数位数，默认2位
 * @returns {string} 格式化后的百分比字符串
 */
export function formatPercent(value, decimals = 2) {
  if (value === null || value === undefined || isNaN(value)) {
    return '-'
  }

  const num = Number(value) * 100

  return `${num.toFixed(decimals)}%`
}

/**
 * 格式化日期
 * @param {Date|string|number} date - 日期对象或时间戳
 * @param {string} format - 格式模板，默认 YYYY-MM-DD
 * @returns {string}
 */
export function formatDate(date, format = 'YYYY-MM-DD') {
  if (!date) return '-'

  const d = new Date(date)

  if (isNaN(d.getTime())) return '-'

  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  const seconds = String(d.getSeconds()).padStart(2, '0')

  return format
    .replace('YYYY', year)
    .replace('MM', month)
    .replace('DD', day)
    .replace('HH', hours)
    .replace('mm', minutes)
    .replace('ss', seconds)
}

/**
 * 格式化期间（季度）
 * @param {string} period - 如 "2025Q1"
 * @returns {string}
 */
export function formatPeriod(period) {
  if (!period) return '-'

  const quarterMap = {
    Q1: '第一季度',
    Q2: '第二季度',
    Q3: '第三季度',
    Q4: '第四季度'
  }

  const match = period.match(/(\d{4})(Q\d)/)
  if (match) {
    return `${match[1]}年${quarterMap[match[2]] || match[2]}`
  }

  return period
}

/**
 * 格式化文件大小
 * @param {number} bytes - 字节数
 * @returns {string}
 */
export function formatFileSize(bytes) {
  if (!bytes || bytes === 0) return '0 B'

  const units = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(1024))

  return `${(bytes / Math.pow(1024, i)).toFixed(2)} ${units[i]}`
}

/**
 * 截断文本
 * @param {string} text - 原始文本
 * @param {number} maxLength - 最大长度
 * @returns {string}
 */
export function truncateText(text, maxLength = 20) {
  if (!text) return '-'

  if (text.length <= maxLength) return text

  return text.substring(0, maxLength) + '...'
}
