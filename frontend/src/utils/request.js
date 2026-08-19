import axios from 'axios'
import { Message } from 'element-ui'
import store from '@/store'

let redirectingToLogin = false

function isLoginRequest(config) {
  return Boolean(config && String(config.url || '').includes('/auth/login'))
}

function handleUnauthorized(config) {
  if (isLoginRequest(config)) return false
  if (redirectingToLogin) return true
  redirectingToLogin = true

  store.dispatch('user/resetState')
  Message.closeAll()
  Message.warning({
    message: '登录状态已失效，请重新登录',
    duration: 2200
  })

  // request.js 不能直接依赖 router，否则会与 store/api 形成循环依赖。
  // 使用 hash 跳转可立即终止业务页继续请求，并回到初始化登录页。
  if (window.location.hash !== '#/login') {
    window.location.replace(`${window.location.pathname}${window.location.search}#/login`)
  }

  window.setTimeout(() => {
    redirectingToLogin = false
  }, 1000)
  return true
}

const service = axios.create({
  baseURL: process.env.VUE_APP_BASE_API || '/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
    'X-Requested-With': 'XMLHttpRequest'
  }
})

// 请求拦截器
service.interceptors.request.use(
  config => {
    const token = store.getters.token || sessionStorage.getItem('token')

    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }

    if (config.method === 'get') {
      config.params = {
        ...config.params,
        _t: Date.now()
      }
    }

    if (config.showLoading !== false) {
      store.dispatch('app/setLoading', true)
    }

    return config
  },
  error => {
    console.error('Request error:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  response => {
    if (response.config.showLoading !== false) {
      store.dispatch('app/setLoading', false)
    }

    // 文件下载接口返回二进制内容，不走统一 JSON 业务码判断
    if (response.config.responseType === 'blob' || response.config.responseType === 'arraybuffer') {
      return response
    }

    const res = response.data

    // 业务成功
    if (res.code === 200 || res.code === 0) {
      return res.data
    }

    // Token过期或无效
    if (res.code === 401) {
      if (!handleUnauthorized(response.config)) {
        Message.error(res.message || '用户名或密码错误')
      }
      return Promise.reject(new Error(res.message || 'Token过期'))
    }

    // 其他业务错误
    Message({
      message: res.message || '请求失败',
      type: 'error',
      duration: 3000
    })

    return Promise.reject(new Error(res.message || '错误'))
  },
  error => {
    if (error.config && error.config.showLoading !== false) {
      store.dispatch('app/setLoading', false)
    }

    if (error.response) {
      switch (error.response.status) {
        case 401:
          if (!handleUnauthorized(error.config)) {
            Message.error((error.response.data && error.response.data.message) || '用户名或密码错误')
          }
          break
        case 400:
          Message.error('请求参数错误')
          break
        case 403:
          Message.error('没有权限访问')
          break
        case 404:
          Message.error('请求的资源不存在')
          break
        case 500:
          Message.error('服务器内部错误')
          break
        default:
          Message.error(`连接错误 ${error.response.status}`)
      }
    } else if (error.message.includes('timeout')) {
      Message.error('请求超时，请稍后重试')
    } else {
      Message.error('网络异常，请检查网络连接')
    }

    return Promise.reject(error)
  }
)

export default service
