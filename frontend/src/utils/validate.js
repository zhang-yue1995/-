/**
 * 用户名验证规则
 * 支持：字母、数字、下划线、中文，4-20位
 */
export const usernameRule = [
  { required: true, message: '请输入用户名', trigger: 'blur' },
  { min: 4, max: 20, message: '长度在 4 到 20 个字符', trigger: 'blur' },
  { pattern: /^[a-zA-Z0-9_\u4e00-\u9fa5]+$/, message: '只允许字母、数字、下划线和中文', trigger: 'blur' }
]

/**
 * 密码验证规则
 * 至少6位，包含字母和数字
 */
export const passwordRule = [
  { required: true, message: '请输入密码', trigger: 'blur' },
  { min: 6, max: 30, message: '长度在 6 到 30 个字符', trigger: 'blur' },
  { pattern: /^(?=.*[a-zA-Z])(?=.*\d).+$/, message: '密码必须包含字母和数字', trigger: 'blur' }
]

/**
 * 企业名称验证规则
 */
export const enterpriseNameRule = [
  { required: true, message: '请输入企业名称', trigger: 'blur' },
  { min: 2, max: 100, message: '长度在 2 到 100 个字符', trigger: 'blur' }
]

/**
 * 统一社会信用代码验证规则
 * 18位字符，由数字和大写字母组成
 */
export const creditCodeRule = [
  { required: true, message: '请输入统一社会信用代码', trigger: 'blur' },
  { pattern: /^[0-9A-Z]{18}$/, message: '请输入正确的18位统一社会信用代码', trigger: 'blur' }
]

/**
 * 手机号验证规则
 */
export const phoneRule = [
  { required: true, message: '请输入手机号', trigger: 'blur' },
  { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
]

/**
 * 邮箱验证规则
 */
export const emailRule = [
  { required: false, message: '请输入邮箱地址', trigger: 'blur' },
  { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
]

/**
 * 金额验证规则
 * 正数，最多2位小数
 */
export const moneyRule = [
  { required: true, message: '请输入金额', trigger: 'blur' },
  { pattern: /^(0|[1-9]\d*)(\.\d{1,2})?$/, message: '请输入有效的金额（正数，最多2位小数）', trigger: 'blur' }
]

/**
 * 百分比验证规则
 * 0-100之间的数值
 */
export const percentRule = [
  { required: true, message: '请输入百分比', trigger: 'blur' },
  { pattern: /^(100(\.0+)?|[1-9]?\d(\.\d+)?)$/, message: '请输入0-100之间的数值', trigger: 'blur' }
]

/**
 * 必填字段验证
 * @param {string} fieldName - 字段名称
 * @returns {Array}
 */
export function requiredRule(fieldName) {
  return [{ required: true, message: `请输入${fieldName}`, trigger: 'blur' }]
}

/**
 * 验证统一社会信用代码校验码
 * @param {string} code - 18位统一社会信用代码
 * @returns {boolean}
 */
export function validateCreditCode(code) {
  if (!code || code.length !== 18) return false

  const weights = [1, 3, 9, 27, 19, 26, 16, 17, 20, 29, 25, 13, 8, 24, 10, 30, 28]
  const codeChars = '0123456789ABCDEFGHJKLMNPQRTUWXY'
  const codeValues = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35]

  let sum = 0
  for (let i = 0; i < 17; i++) {
    const charIndex = codeChars.indexOf(code[i])
    if (charIndex === -1) return false
    sum += codeValues[charIndex] * weights[i]
  }

  const checkIndex = 31 - (sum % 31)
  return code[17] === codeChars[checkIndex]
}
