<template>
  <div class="app-sidebar" :class="{ 'is-collapsed': isCollapsed }">
    <!-- Logo区域 -->
    <div class="sidebar-logo">
      <div class="logo-icon">鑫</div>
      <transition name="fade">
        <h1 v-show="!isCollapsed" class="logo-text">鑫速录</h1>
      </transition>
    </div>

    <!-- 菜单区域 -->
    <el-menu
      :default-active="activeMenu"
      class="sidebar-menu"
      :collapse="isCollapsed"
      background-color="#123044"
      text-color="#ffffff"
      active-text-color="#35d0b0"
      router
    >
      <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
        <i :class="item.icon"></i>
        <span slot="title">{{ item.title }}</span>
      </el-menu-item>
    </el-menu>

    <!-- 折叠按钮 -->
    <div class="sidebar-toggle" @click="toggleCollapse">
      <i :class="isCollapsed ? 'el-icon-s-unfold' : 'el-icon-s-fold'"></i>
    </div>
  </div>
</template>

<script>
export default {
  name: 'AppSidebar',

  props: {
    isCollapsed: {
      type: Boolean,
      default: false
    }
  },

  data() {
    return {
      menuItems: [
        { path: '/dashboard', title: '工作台', icon: 'el-icon-data-analysis' },
        { path: '/enterprise', title: '企业与报表', icon: 'el-icon-office-building' },
        { path: '/analysis', title: '财务分析', icon: 'el-icon-data-line' },
        { path: '/trend', title: '历史趋势', icon: 'el-icon-time' },
        { path: '/rule-config', title: '规则配置', icon: 'el-icon-setting' },
        { path: '/ocr-tasks', title: 'OCR任务中心', icon: 'el-icon-picture-outline' },
        { path: '/audit-log', title: '审计日志', icon: 'el-icon-document' },
        { path: '/system-settings', title: '系统设置', icon: 'el-icon-s-tools' }
      ]
    }
  },

  computed: {
    activeMenu() {
      const route = this.$route
      return route.path
    }
  },

  methods: {
    toggleCollapse() {
      this.$emit('toggle-collapse')
    }
  }
}
</script>

<style scoped>
.app-sidebar {
  width: 210px;
  height: 100vh;
  background-color: #123044;
  position: fixed;
  left: 0;
  top: 0;
  z-index: 1001;
  transition: width 0.3s ease;
  display: flex;
  flex-direction: column;
}

.app-sidebar.is-collapsed {
  width: 64px;
}

.sidebar-logo {
  height: 66px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo-icon {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, #35d0b0 0%, #0e8f78 100%);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 18px;
  font-weight: bold;
  flex-shrink: 0;
}

.logo-text {
  margin-left: 12px;
  color: #35d0b0;
  font-size: 20px;
  font-weight: 600;
  white-space: nowrap;
}

.sidebar-menu {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  border-right: none;
}

.sidebar-menu:not(.el-menu--collapse) {
  width: 210px;
}

/* 自定义菜单项样式 */
.sidebar-menu .el-menu-item {
  height: 50px;
  line-height: 50px;
  margin: 4px 12px;
  border-radius: 8px;
}

.sidebar-menu .el-menu-item:hover {
  background-color: rgba(53, 208, 176, 0.1) !important;
}

.sidebar-menu .el-menu-item.is-active {
  background-color: rgba(53, 208, 176, 0.15) !important;
  color: #35d0b0 !important;
}

.sidebar-menu .el-menu-item i {
  font-size: 18px;
  width: 24px;
  margin-right: 8px;
}

.sidebar-toggle {
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  transition: background-color 0.3s;
}

.sidebar-toggle:hover {
  background-color: rgba(255, 255, 255, 0.05);
}

.sidebar-toggle i {
  font-size: 20px;
  color: rgba(255, 255, 255, 0.65);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s;
}

.fade-enter,
.fade-leave-to {
  opacity: 0;
}
</style>
