/**
 * 表单验证工具
 */

// 校验必填
function required(value, fieldName) {
  if (!value || (typeof value === 'string' && !value.trim())) {
    return `${fieldName}不能为空`;
  }
  return null;
}

// 校验信用代码（18位）
function validateCreditCode(code) {
  if (!code) return '统一社会信用代码不能为空';
  if (!/^[0-9A-HJ-NPQRTUWXY]{2}\d{6}[0-9A-HJ-NPQRTUWXY]{10}$/.test(code)) {
    return '信用代码格式不正确';
  }
  return null;
}

// 校验期间格式（YYYY-MM或YYYY-Qn）
function validatePeriod(period) {
  if (!period) return '报表期间不能为空';
  if (!/^\d{4}-(0[1-9]|1[0-2]|Q[1-4])$/.test(period)) {
    return '期间格式不正确，应为YYYY-MM或YYYY-Qn';
  }
  return null;
}

// 校验文件类型
function validateFileType(fileName, allowedTypes) {
  const ext = fileName.split('.').pop().toLowerCase();
  if (!allowedTypes.includes(ext)) {
    return `不支持${ext}格式，允许的格式：${allowedTypes.join('、')}`;
  }
  return null;
}

// 校验文件大小（MB）
function validateFileSize(size, maxSizeMB = 30) {
  const sizeMB = size / (1024 * 1024);
  if (sizeMB > maxSizeMB) {
    return `文件大小不能超过${maxSizeMB}MB`;
  }
  return null;
}

module.exports = {
  required,
  validateCreditCode,
  validatePeriod,
  validateFileType,
  validateFileSize
};
