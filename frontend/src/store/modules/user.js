import { login as loginApi, logout as logoutApi, getUserInfo as getUserInfoApi } from '@/api/auth'

// 登录态只在当前浏览器会话中保留。旧版本曾将令牌永久写入
// localStorage，后端重启后该令牌已经失效，必须在启动时清理。
localStorage.removeItem('token')

const state = {
  token: sessionStorage.getItem('token') || '',
  userInfo: {},
  roles: [],
  permissions: []
}

const mutations = {
  SET_TOKEN(state, token) {
    state.token = token
    if (token) {
      sessionStorage.setItem('token', token)
    } else {
      sessionStorage.removeItem('token')
    }
  },

  SET_USER_INFO(state, info) {
    state.userInfo = info
  },

  SET_ROLES(state, roles) {
    state.roles = roles
  },

  SET_PERMISSIONS(state, permissions) {
    state.permissions = permissions
  }
}

const actions = {
  /**
   * 登录
   * @param {Object} context - Vuex context
   * @param {Object} loginData - 登录数据 {username, password}
   */
  async login({ commit }, loginData) {
    try {
      const data = await loginApi(loginData)
      commit('SET_TOKEN', data.token)
      commit('SET_USER_INFO', data)
      commit('SET_ROLES', data.role ? [data.role] : [])

      return data
    } catch (error) {
      throw error
    }
  },

  /**
   * 登出
   */
  async logout({ commit }) {
    try {
      await logoutApi()
    } catch (error) {
      console.error('Logout API error:', error)
    } finally {
      commit('SET_TOKEN', '')
      commit('SET_USER_INFO', {})
      commit('SET_ROLES', [])
      commit('SET_PERMISSIONS', [])
    }
  },

  /**
   * 获取用户信息
   */
  async getUserInfo({ commit }) {
    try {
      const data = await getUserInfoApi()
      commit('SET_USER_INFO', data)

      if (data.roles) {
        commit('SET_ROLES', data.roles)
      }

      if (data.permissions) {
        commit('SET_PERMISSIONS', data.permissions)
      }

      return data
    } catch (error) {
      throw error
    }
  },

  /**
   * 重置用户状态
   */
  resetState({ commit }) {
    commit('SET_TOKEN', '')
    commit('SET_USER_INFO', {})
    commit('SET_ROLES', [])
    commit('SET_PERMISSIONS', [])
  }
}

const getters = {
  token: state => state.token,
  isLoggedIn: state => !!state.token,
  userInfo: state => state.userInfo,
  userName: state => state.userInfo.name || state.userInfo.username || '',
  userAvatar: state => state.userInfo.avatar || '',
  roles: state => state.roles,
  permissions: state => state.permissions,

  /**
   * 检查是否拥有指定角色
   */
  hasRole: state => role => state.roles.includes(role),

  /**
   * 检查是否拥有指定权限
   */
  hasPermission: state => permission => state.permissions.includes(permission)
}

export default {
  namespaced: true,
  state,
  mutations,
  actions,
  getters
}
