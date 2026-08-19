<template>
  <div class="page">
    <div class="page-header">
      <h2>系统设置</h2>
      <p>部署状态与生产参数说明</p>
    </div>

    <div class="status-card">
      <div class="status-title">
        <span>后端服务</span>
        <el-tag :type="healthStatus === 'UP' ? 'success' : 'danger'">
          {{ healthStatus === 'UP' ? '运行正常' : '连接异常' }}
        </el-tag>
      </div>
      <el-button size="small" icon="el-icon-refresh" @click="checkHealth">重新检测</el-button>
    </div>

    <div class="settings-grid">
      <section>
        <h3>账号与安全</h3>
        <p>当前用户：{{ username || '—' }}</p>
        <p>登录令牌有效期、管理员密码与客户经理密码均由服务器环境变量管理。</p>
        <p class="tip">生产部署必须修改默认密码，并使用 HTTPS。</p>
      </section>
      <section>
        <h3>文件与OCR</h3>
        <p>单文件上限：30 MB</p>
        <p>生产环境必须使用 HTTP OCR 提供方；MOCK_OCR 仅用于流程验收。</p>
        <p class="tip">可在审计日志中核对上传、复核、修改和导出操作。</p>
      </section>
      <section>
        <h3>数据与备份</h3>
        <p>报表金额单位：元</p>
        <p>Docker 部署将数据库与上传原件统一保存在持久化数据卷。</p>
        <p class="tip">备份恢复与 OCR 协议详见 DEPLOYMENT.md。</p>
      </section>
    </div>
  </div>
</template>

<script>
import { getSystemHealth } from '@/api/system'

export default {
  name: 'SystemSettings',
  data() {
    return { healthStatus: 'UNKNOWN' }
  },
  computed: {
    username() {
      const user = this.$store.state.user.userInfo || {}
      return user.realName || user.username || ''
    }
  },
  created() {
    this.checkHealth()
  },
  methods: {
    async checkHealth() {
      try {
        const result = await getSystemHealth()
        this.healthStatus = result.status
      } catch (error) {
        this.healthStatus = 'DOWN'
      }
    }
  }
}
</script>

<style scoped>
.page-header { margin-bottom:18px; }
.page-header h2 { margin:0 0 6px; color:#10212b; font-size:24px; }
.page-header p { margin:0; color:#6c7d89; }
.status-card { display:flex; justify-content:space-between; align-items:center; background:#fff; padding:18px 22px; border-radius:12px; margin-bottom:18px; box-shadow:0 4px 16px rgba(27,61,78,.05); }
.status-title { display:flex; align-items:center; gap:14px; font-weight:600; color:#10212b; }
.settings-grid { display:grid; grid-template-columns:repeat(3,1fr); gap:18px; }
.settings-grid section { background:#fff; border-radius:12px; padding:22px; box-shadow:0 4px 16px rgba(27,61,78,.05); }
.settings-grid h3 { margin:0 0 16px; color:#10212b; }
.settings-grid p { color:#526572; line-height:1.7; }
.settings-grid .tip { color:#0e8f78; }
@media (max-width:1200px) { .settings-grid { grid-template-columns:1fr; } }
</style>
