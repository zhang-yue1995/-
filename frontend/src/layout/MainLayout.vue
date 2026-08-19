<template>
  <div class="main-layout" :class="{ 'sidebar-collapsed': sidebarCollapsed }">
    <!-- 侧边栏 -->
    <AppSidebar :is-collapsed="sidebarCollapsed" @toggle-collapse="toggleSidebar" />

    <!-- 主内容区 -->
    <div class="main-container" :style="{ marginLeft: sidebarCollapsed ? '64px' : '210px' }">
      <!-- 顶部栏 -->
      <AppHeader />

      <!-- 内容区域 -->
      <main class="main-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script>
import AppSidebar from '@/components/Layout/AppSidebar'
import AppHeader from '@/components/Layout/AppHeader'

export default {
  name: 'MainLayout',

  components: {
    AppSidebar,
    AppHeader
  },

  data() {
    return {
      sidebarCollapsed: false
    }
  },

  created() {
    // 从store读取侧边栏状态
    this.sidebarCollapsed = this.$store.getters.sidebarCollapsed
  },

  methods: {
    toggleSidebar() {
      this.sidebarCollapsed = !this.sidebarCollapsed
      this.$store.dispatch('app/toggleSidebar')
    }
  }
}
</script>

<style scoped>
.main-layout {
  min-height: 100vh;
  background-color: #eef3f7;
}

.main-container {
  transition: margin-left 0.3s ease;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.main-content {
  flex: 1;
  padding: 20px 24px;
  overflow-y: auto;
}

/* 响应式适配 */
@media screen and (max-width: 1366px) {
  .main-content {
    padding: 16px 20px;
  }
}

@media screen and (max-width: 1440px) {
  .main-content {
    padding: 18px 22px;
  }
}

@media screen and (min-width: 1920px) {
  .main-content {
    padding: 28px 30px;
  }
}
</style>
