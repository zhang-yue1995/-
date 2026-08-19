const state = {
  loading: false,
  sidebarCollapsed: false,
  device: 'desktop',
  errorLog: []
}

const mutations = {
  SET_LOADING(state, status) {
    state.loading = status
  },

  TOGGLE_SIDEBAR(state) {
    state.sidebarCollapsed = !state.sidebarCollapsed
  },

  SET_SIDEBAR(state, collapsed) {
    state.sidebarCollapsed = collapsed
  },

  SET_DEVICE(state, device) {
    state.device = device
  },

  ADD_ERROR_LOG(state, log) {
    state.errorLog.push(log)
  },

  CLEAR_ERROR_LOG(state) {
    state.errorLog = []
  }
}

const actions = {
  setLoading({ commit }, status) {
    commit('SET_LOADING', status)
  },

  toggleSidebar({ commit }) {
    commit('TOGGLE_SIDEBAR')
  },

  setSidebar({ commit }, collapsed) {
    commit('SET_SIDEBAR', collapsed)
  },

  addErrorLog({ commit }, log) {
    commit('ADD_ERROR_LOG', log)
  },

  clearErrorLog({ commit }) {
    commit('CLEAR_ERROR_LOG')
  }
}

const getters = {
  loading: state => state.loading,
  sidebarCollapsed: state => state.sidebarCollapsed,
  device: state => state.device,
  isMobile: state => state.device === 'mobile',
  errorLog: state => state.errorLog
}

export default {
  namespaced: true,
  state,
  mutations,
  actions,
  getters
}
