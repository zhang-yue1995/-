<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2>审计日志</h2>
        <p>追踪关键新增、修改、删除、登录与导出操作</p>
      </div>
      <el-button icon="el-icon-download" @click="handleExport">导出CSV</el-button>
    </div>

    <div class="filter-card">
      <el-select v-model="filters.operationType" clearable placeholder="操作类型">
        <el-option label="新增" value="CREATE" />
        <el-option label="修改" value="UPDATE" />
        <el-option label="删除" value="DELETE" />
        <el-option label="导出" value="EXPORT" />
      </el-select>
      <el-input v-model.trim="filters.operator" clearable placeholder="操作人" />
      <el-button type="primary" icon="el-icon-search" @click="handleSearch">查询</el-button>
    </div>

    <div class="table-card">
      <DataTable
        :data="logs"
        :loading="loading"
        :total="total"
        :current-page="page"
        :page-size="pageSize"
        :show-index="true"
        empty-text="暂无审计日志"
        @pagination-change="handlePagination"
      >
        <el-table-column prop="action" label="操作" width="90">
          <template slot-scope="{ row }">
            <el-tag :type="actionType(row.action)" size="small">{{ row.action }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="module" label="模块" width="130" />
        <el-table-column prop="description" label="操作描述" min-width="220" show-overflow-tooltip />
        <el-table-column prop="username" label="操作人" width="110" />
        <el-table-column prop="requestMethod" label="请求" width="80" />
        <el-table-column prop="responseStatus" label="响应" width="80" />
        <el-table-column prop="executionTime" label="耗时" width="90">
          <template slot-scope="{ row }">{{ row.executionTime == null ? '—' : `${row.executionTime} ms` }}</template>
        </el-table-column>
        <el-table-column prop="ipAddress" label="IP地址" width="130" />
        <el-table-column prop="createdTime" label="操作时间" min-width="165" />
      </DataTable>
    </div>
  </div>
</template>

<script>
import DataTable from '@/components/DataTable.vue'
import { getAuditLogList, exportAuditLogs } from '@/api/audit'

export default {
  name: 'AuditLog',
  components: { DataTable },
  data() {
    return {
      loading: false,
      logs: [],
      total: 0,
      page: 1,
      pageSize: 20,
      filters: { operationType: '', operator: '' }
    }
  },
  created() {
    this.fetchLogs()
  },
  methods: {
    async fetchLogs() {
      this.loading = true
      try {
        const result = await getAuditLogList({
          ...this.filters,
          page: this.page,
          size: this.pageSize
        })
        this.logs = result.content || []
        this.total = result.totalElements || 0
      } catch (error) {
        this.$message.error('审计日志加载失败')
      } finally {
        this.loading = false
      }
    },
    handleSearch() {
      this.page = 1
      this.fetchLogs()
    },
    handlePagination({ page, pageSize }) {
      this.page = page
      this.pageSize = pageSize
      this.fetchLogs()
    },
    actionType(action) {
      return { CREATE: 'success', UPDATE: 'warning', DELETE: 'danger', EXPORT: 'info' }[action] || 'info'
    },
    async handleExport() {
      const response = await exportAuditLogs(this.filters)
      const url = URL.createObjectURL(response.data)
      const link = document.createElement('a')
      link.href = url
      link.download = `审计日志_${new Date().toISOString().slice(0, 10)}.csv`
      link.click()
      URL.revokeObjectURL(url)
      this.$message.success('导出成功')
    }
  }
}
</script>

<style scoped>
.page-header { display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:18px; }
.page-header h2 { margin:0 0 6px; color:#10212b; font-size:24px; }
.page-header p { margin:0; color:#6c7d89; }
.filter-card, .table-card { background:#fff; border-radius:12px; padding:18px; box-shadow:0 4px 16px rgba(27,61,78,.05); }
.filter-card { display:flex; gap:12px; margin-bottom:16px; }
.filter-card .el-select, .filter-card .el-input { width:190px; }
</style>
