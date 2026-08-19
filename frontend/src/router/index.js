import Vue from 'vue'
import VueRouter from 'vue-router'
import store from '@/store'

Vue.use(VueRouter)

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import(/* webpackChunkName: "login" */ '@/views/Login.vue'),
    meta: { title: '登录', requiresAuth: false }
  },
  {
    path: '/',
    component: () => import(/* webpackChunkName: "layout" */ '@/layout/MainLayout.vue'),
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import(/* webpackChunkName: "dashboard" */ '@/views/Dashboard.vue'),
        meta: { title: '工作台', icon: 'el-icon-data-analysis' }
      },
      {
        path: 'enterprise',
        name: 'EnterpriseList',
        component: () => import(/* webpackChunkName: "enterprise" */ '@/views/EnterpriseList.vue'),
        meta: { title: '企业与报表列表', icon: 'el-icon-office-building' }
      },
      {
        path: 'enterprise/:id',
        name: 'ReportDetail',
        component: () => import(/* webpackChunkName: "report-detail" */ '@/views/ReportDetail.vue'),
        meta: { title: '报表详情', icon: 'el-icon-document' },
        props: true
      },
      {
        path: 'ocr-tasks',
        name: 'OcrTaskCenter',
        component: () => import(/* webpackChunkName: "ocr-tasks" */ '@/views/OcrTaskCenter.vue'),
        meta: { title: 'OCR任务中心', icon: 'el-icon-picture-outline' }
      },
      {
        path: 'analysis',
        name: 'AnalysisReport',
        component: () => import(/* webpackChunkName: "analysis" */ '@/views/AnalysisReport.vue'),
        meta: { title: '财务分析报告', icon: 'el-icon-data-line' }
      },
      {
        path: 'trend',
        name: 'TrendMonitor',
        component: () => import(/* webpackChunkName: "trend" */ '@/views/TrendMonitor.vue'),
        meta: { title: '历史趋势监控', icon: 'el-icon-trend-charts' }
      },
      {
        path: 'rule-config',
        name: 'RuleConfig',
        component: () => import(/* webpackChunkName: "rule-config" */ '@/views/RuleConfig.vue'),
        meta: { title: '指标与规则配置', icon: 'el-icon-setting' }
      },
      {
        path: 'audit-log',
        name: 'AuditLog',
        component: () => import(/* webpackChunkName: "audit-log" */ '@/views/AuditLog.vue'),
        meta: { title: '审计日志', icon: 'el-icon-document' }
      },
      {
        path: 'system-settings',
        name: 'SystemSettings',
        component: () => import(/* webpackChunkName: "system-settings" */ '@/views/SystemSettings.vue'),
        meta: { title: '系统设置', icon: 'el-icon-tools' }
      }
    ]
  },
  {
    path: '*',
    redirect: '/dashboard'
  }
]

const router = new VueRouter({
  mode: 'hash',
  base: process.env.BASE_URL,
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    } else {
      return { x: 0, y: 0 }
    }
  }
})

async function validateSession() {
  const token = store.getters.token || sessionStorage.getItem('token')
  if (!token) return false

  // 登录成功时用户信息已写入 Vuex，同一会话内无需每次切页重复校验。
  if (store.getters['user/userInfo'] && store.getters['user/userInfo'].id) {
    return true
  }

  try {
    await store.dispatch('user/getUserInfo')
    return true
  } catch (error) {
    await store.dispatch('user/resetState')
    return false
  }
}

// 路由守卫：进入业务页面前先向后端验证会话，避免旧令牌放行业务页。
router.beforeEach(async (to, from, next) => {
  // 设置页面标题
  if (to.meta && to.meta.title) {
    document.title = `${to.meta.title} - 鑫速录`
  }

  // 检查是否需要认证
  if (to.matched.some(record => record.meta.requiresAuth)) {
    if (await validateSession()) {
      next()
    } else {
      next({
        path: '/login',
        query: { redirect: to.fullPath }
      })
    }
  } else {
    // 公开页面
    if (to.path === '/login') {
      if (await validateSession()) {
        next({ path: '/' })
        return
      }
    }

    next()
  }
})

export default router
