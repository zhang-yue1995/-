<template>
  <header class="app-header">
    <div class="header-left">
      <!-- 面包屑导航 -->
      <el-breadcrumb separator="/">
        <el-breadcrumb-item v-for="(item, index) in breadcrumbs" :key="index" :to="item.path">
          {{ item.title }}
        </el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="header-right">
      <!-- 全屏切换 -->
      <el-tooltip content="全屏" placement="bottom">
        <div class="header-action" @click="toggleFullscreen">
          <i class="el-icon-full-screen"></i>
        </div>
      </el-tooltip>

      <!-- 消息通知 -->
      <el-popover
        v-model="notificationVisible"
        placement="bottom-end"
        width="360"
        trigger="click"
        popper-class="notification-popover"
        @show="loadNotifications"
      >
        <div class="notification-panel">
          <div class="notification-title">消息通知</div>
          <div v-if="notificationLoading" class="notification-loading">
            <i class="el-icon-loading"></i> 正在加载通知...
          </div>
          <div v-else-if="notifications.length" class="notification-list">
            <button
              v-for="item in notifications"
              :key="item.id"
              type="button"
              class="notification-item"
              @click="handleNotification(item)"
            >
              <span :class="['notification-dot', `is-${item.type}`]"></span>
              <span class="notification-content">
                <strong>{{ item.title }}</strong>
                <small>{{ item.description }}</small>
              </span>
              <i class="el-icon-arrow-right"></i>
            </button>
          </div>
          <el-empty v-else description="暂无通知" :image-size="56" />
        </div>
        <el-badge
          slot="reference"
          :value="notifications.length"
          :hidden="notifications.length === 0"
          class="notification-badge"
        >
          <el-tooltip content="消息通知" placement="bottom">
            <div class="header-action" role="button" tabindex="0" aria-label="打开消息通知">
              <i class="el-icon-bell"></i>
            </div>
          </el-tooltip>
        </el-badge>
      </el-popover>

      <!-- 用户信息下拉 -->
      <el-dropdown trigger="click" @command="handleCommand">
        <div class="user-info">
          <el-avatar :size="32" :src="userInfo.avatar || ''" class="user-avatar">
            {{ userInfo.name ? userInfo.name.charAt(0).toUpperCase() : 'U' }}
          </el-avatar>
          <span class="user-name">{{ userInfo.name || userInfo.username || '用户' }}</span>
          <i class="el-icon-arrow-down"></i>
        </div>
        <el-dropdown-menu slot="dropdown">
          <el-dropdown-item command="profile">
            <i class="el-icon-user"></i> 个人中心
          </el-dropdown-item>
          <el-dropdown-item command="settings">
            <i class="el-icon-setting"></i> 系统设置
          </el-dropdown-item>
          <el-dropdown-item divided command="logout">
            <i class="el-icon-switch-button"></i> 退出登录
          </el-dropdown-item>
        </el-dropdown-menu>
      </el-dropdown>
    </div>
  </header>
</template>

<script>
import { getDashboardStats } from '@/api/dashboard'
import { getReportList } from '@/api/report'

export default {
  name: 'AppHeader',

  data() {
    return {
      notificationVisible: false,
      notificationLoading: false,
      notificationItems: []
    }
  },

  computed: {
    userInfo() {
      return this.$store.state.user.userInfo || {}
    },

    notifications() {
      return this.notificationItems
    },

    breadcrumbs() {
      const matched = this.$route.matched.filter(item => item.meta && item.meta.title)
      const breadcrumbs = []

      matched.forEach((item, index) => {
        if (item.meta.title) {
          breadcrumbs.push({
            title: item.meta.title,
            path: index === matched.length - 1 ? null : item.path || '/'
          })
        }
      })

      // 如果没有面包屑，添加默认首页
      if (breadcrumbs.length === 0) {
        breadcrumbs.push({ title: '首页', path: '/' })
      }

      return breadcrumbs
    }
  },

  created() {
    this.loadNotifications()
  },

  methods: {
    async loadNotifications() {
      if (this.notificationLoading) return
      this.notificationLoading = true
      try {
        const stats = await getDashboardStats({ showLoading: false })
        const items = []

        if (Number(stats.pendingReview || 0) > 0) {
          const page = await getReportList({
            status: 'PENDING_REVIEW',
            pageNum: 1,
            pageSize: 1,
            sortBy: 'createdTime',
            sortOrder: 'asc'
          }, { showLoading: false })
          const report = page.list && page.list[0]
          if (report) {
            items.push({
              id: `review-${report.archiveId}`,
              type: 'warning',
              title: `待复核报表（${stats.pendingReview}）`,
              description: `${report.enterpriseName} · ${report.reportPeriod}`,
              route: {
                path: `/enterprise/${report.enterpriseId}`,
                query: { reportId: report.archiveId, action: 'review' }
              }
            })
          }
        }

        if (Number(stats.monthlyNewReports || 0) > 0) {
          items.push({
            id: 'monthly',
            type: 'success',
            title: `本月新增报表（${stats.monthlyNewReports}）`,
            description: '查看本月新归档的全部报表',
            route: { path: '/enterprise', query: { source: 'monthly' } }
          })
        }

        this.notificationItems = items
      } catch (error) {
        console.error('消息通知加载失败:', error)
        this.notificationItems = []
      } finally {
        this.notificationLoading = false
      }
    },

    handleNotification(item) {
      this.notificationVisible = false
      if (!item || !item.route) return
      const route = {
        ...item.route,
        query: { ...(item.route.query || {}), noticeAt: Date.now() }
      }
      this.$router.push(route).catch(error => {
        // 重复点击当前通知时 Vue Router 会返回 NavigationDuplicated，页面已处于目标位置。
        if (!error || error.name !== 'NavigationDuplicated') {
          console.error('消息通知跳转失败:', error)
          this.$message.error('页面跳转失败，请稍后重试')
        }
      })
    },

    toggleFullscreen() {
      if (!document.fullscreenElement) {
        document.documentElement.requestFullscreen()
      } else {
        if (document.exitFullscreen) {
          document.exitFullscreen()
        }
      }
    },

    handleCommand(command) {
      switch (command) {
        case 'profile':
          this.$router.push('/profile')
          break
        case 'settings':
          this.$router.push('/system-settings')
          break
        case 'logout':
          this.handleLogout()
          break
      }
    },

    async handleLogout() {
      try {
        await this.$confirm('确定要退出登录吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })

        await this.$store.dispatch('user/logout')
        this.$router.push('/login')
        this.$message.success('已退出登录')
      } catch (error) {
        // 用户取消操作
      }
    }
  }
}
</script>

<style scoped>
.app-header {
  height: 66px;
  background-color: #ffffff;
  border-bottom: 1px solid #dce6eb;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  position: sticky;
  top: 0;
  z-index: 1000;
  box-shadow: 0 2px 8px rgba(27, 61, 78, 0.04);
}

.header-left {
  display: flex;
  align-items: center;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-action {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
  color: #6c7d89;
}

.header-action:hover {
  background-color: #eef3f7;
  color: #0e8f78;
}

.header-action i {
  font-size: 18px;
}

.notification-badge {
  margin-right: 4px;
}

.notification-panel {
  margin: -4px;
}

.notification-title {
  padding: 8px 10px 12px;
  color: #10212b;
  font-size: 15px;
  font-weight: 600;
  border-bottom: 1px solid #edf2f4;
}

.notification-list {
  padding-top: 4px;
}

.notification-loading {
  padding: 24px 12px;
  color: #7b8e98;
  font-size: 13px;
  text-align: center;
}

.notification-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 12px 10px;
  color: #314b59;
  text-align: left;
  background: transparent;
  border: 0;
  border-radius: 8px;
  cursor: pointer;
}

.notification-item:hover {
  background: #f3f8f7;
}

.notification-dot {
  width: 9px;
  height: 9px;
  flex: 0 0 auto;
  border-radius: 50%;
  background: #7c94a0;
}

.notification-dot.is-warning { background: #f3a83b; }
.notification-dot.is-success { background: #20a96b; }
.notification-dot.is-info { background: #3d7cf0; }

.notification-content {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 4px;
}

.notification-content strong {
  color: #15303e;
  font-size: 13px;
}

.notification-content small {
  color: #7b8e98;
  font-size: 12px;
}

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 4px 12px 4px 4px;
  border-radius: 999px;
  transition: background-color 0.3s;
}

.user-info:hover {
  background-color: #eef3f7;
}

.user-avatar {
  background: linear-gradient(135deg, #35d0b0 0%, #0e8f78 100%);
  color: white;
  font-weight: 600;
  flex-shrink: 0;
}

.user-name {
  margin: 0 8px;
  font-size: 14px;
  color: #10212b;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-name + i {
  font-size: 12px;
  color: #6c7d89;
}
</style>
