import Vue from 'vue'
import Vuex from 'vuex'
import user from './modules/user'
import app from './modules/app'

Vue.use(Vuex)

export default new Vuex.Store({
  modules: {
    user,
    app
  },

  getters: {
    token: state => state.user.token || sessionStorage.getItem('token'),
    isLoggedIn: state => !!state.user.token,
    sidebarCollapsed: state => state.app.sidebarCollapsed,
    loading: state => state.app.loading
  }
})
