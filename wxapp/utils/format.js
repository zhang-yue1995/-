/**
 * 格式化工具函数
 */

// 金额格式化：1234567.89 → "1,234,567.89"
function formatMoney(amount, unit = '元') {
  if (amount === null || amount === undefined || amount === '') return '-';
  const num = parseFloat(amount);
  if (isNaN(num)) return '-';
  const formatted = Math.abs(num).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
  return num < 0 ? `(${formatted})` : formatted;
}

// 百分比格式化：0.978 → "97.8%"
function formatPercent(value, decimals = 1) {
  if (value === null || value === undefined) return '-';
  const num = parseFloat(value);
  if (isNaN(num)) return '-';
  return (num * 100).toFixed(decimals) + '%';
}

// 日期格式化：2024-09-15T10:30:00 → "2024-09-15 10:30"
function formatDateTime(dateStr) {
  if (!dateStr) return '-';
  const date = new Date(dateStr);
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  const h = String(date.getHours()).padStart(2, '0');
  const min = String(date.getMinutes()).padStart(2, '0');
  return `${y}-${m}-${d} ${h}:${min}`;
}

// 风险等级颜色映射
function getRiskColor(score) {
  if (score >= 80) return '#20a96b'; // 绿色-健康
  if (score >= 60) return '#3d7cf0'; // 蓝色-基本健康
  if (score >= 40) return '#f3a83b'; // 黄色-需关注
  if (score >= 20) return '#e35d6a'; // 橙色-高风险
  return '#c0392b'; // 深红-严重风险
}

// 风险等级文本
function getRiskText(score) {
  if (score >= 80) return '健康';
  if (score >= 60) return '基本健康';
  if (score >= 40) return '需关注';
  if (score >= 20) return '高风险';
  return '严重风险';
}

// 置信度颜色
function getConfidenceColor(confidence) {
  if (confidence >= 95) return '#20a96b'; // 绿色
  if (confidence >= 70) return '#f3a83b'; // 黄色
  return '#e35d6a'; // 红色
}

module.exports = {
  formatMoney,
  formatPercent,
  formatDateTime,
  getRiskColor,
  getRiskText,
  getConfidenceColor
};
