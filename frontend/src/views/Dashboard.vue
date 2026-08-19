<template>
  <div class="dashboard-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2 class="page-title">工作台</h2>
      <p class="page-subtitle">经营总览</p>
    </div>

    <!-- KPI卡片网格 -->
    <div class="kpi-grid">
      <KpiCard
        title="本月新增报表"
        :value="stats.monthlyNewReports"
        unit="份"
        :trend="0"
        :trend-text="`累计 ${stats.totalReports} 份`"
        icon="el-icon-document"
        color="success"
        clickable
        @click="handleMonthlyReports"
      />
      <KpiCard
        title="待人工复核"
        :value="stats.pendingReview"
        unit="项"
        :trend="0"
        trend-text="需及时完成数据确认"
        icon="el-icon-warning"
        color="danger"
      />
      <KpiCard
        title="OCR识别完成率"
        :value="stats.ocrCompletionRate"
        unit="%"
        :trend="0"
        trend-text="按已归档报表统计"
        icon="el-icon-circle-check"
        color="success"
      />
      <KpiCard
        title="高风险企业"
        :value="stats.highRiskEnterprises"
        unit="家"
        :trend="0"
        :trend-text="`共 ${stats.totalEnterprises} 家企业`"
        icon="el-icon-warning-outline"
        color="danger"
      />
    </div>

    <!-- 图表区域 -->
    <div class="charts-grid">
      <!-- 报表处理量与通过率趋势图 -->
      <LineChart
        title="报表处理量与健康评分趋势"
        :xAxisData="monthData"
        :seriesData="processingSeries"
        :color="['#3d7cf0', '#20a96b']"
        height="380px"
      />

      <!-- 风险等级分布环形图 -->
      <PieChart
        title="风险等级分布"
        :chartData="riskDistribution"
        :colors="['#20a96b', '#9dd99e', '#f3a83b', '#e35d6a', '#ff6b6b']"
        height="380px"
        :donut="true"
      />
    </div>

    <!-- 待处理任务表格 -->
    <div class="task-section">
      <div class="section-header">
        <h3 class="section-title">最近报表</h3>
        <el-button type="text" size="small" @click="$router.push('/enterprise')">
          查看全部 <i class="el-icon-arrow-right"></i>
        </el-button>
      </div>

      <DataTable
        :data="pendingTasks"
        :loading="loadingTasks"
        :total="pendingTasks.length"
        :showPagination="false"
        emptyText="暂无报表记录"
      >
        <el-table-column prop="taskNo" label="归档编号" min-width="140" show-overflow-tooltip />
        <el-table-column prop="enterpriseName" label="企业名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="period" label="报表期间" width="110" />
        <el-table-column prop="taskType" label="数据来源" width="110">
          <template slot-scope="{ row }">
            <StatusTag :type="getTaskTypeTag(row.taskType)" :text="row.taskType" size="small" />
          </template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="90">
          <template slot-scope="{ row }">
            <StatusTag :type="row.priority === '高' ? 'danger' : row.priority === '中' ? 'warning' : 'info'" :text="row.priority" size="mini" />
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160" />
        <el-table-column prop="status" label="状态" width="100">
          <template slot-scope="{ row }">
            <StatusTag :type="getStatusTag(row.status)" :text="row.status" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" size="small" @click="handleViewTask(row)">查看详情</el-button>
            <el-button v-if="row.filingStatus === 'PENDING_REVIEW'" type="primary" size="small" plain round @click="handleProcessTask(row)">立即处理</el-button>
          </template>
        </el-table-column>
      </DataTable>
    </div>
  </div>
</template>

<script>
import {
  getDashboardStats,
  getRecentUploads,
  getRiskDistribution,
  getTrendChartData
} from '@/api/dashboard'
import KpiCard from '@/components/KpiCard.vue'
import LineChart from '@/components/charts/LineChart.vue'
import PieChart from '@/components/charts/PieChart.vue'
import DataTable from '@/components/DataTable.vue'
import StatusTag from '@/components/StatusTag.vue'

export default {
  name: 'Dashboard',

  components: {
    KpiCard,
    LineChart,
    PieChart,
    DataTable,
    StatusTag
  },

  data() {
    return {
      loadingTasks: false,
      stats: {
        totalEnterprises: 0,
        totalReports: 0,
        monthlyNewReports: 0,
        pendingReview: 0,
        highRiskEnterprises: 0,
        averageHealthScore: 0,
        ocrCompletionRate: 0
      },

      monthData: [],

      processingSeries: [
        { name: '处理量', data: [] },
        { name: '健康评分', data: [] }
      ],

      riskDistribution: [],
      pendingTasks: []
    }
  },

  created() {
    this.fetchDashboardData()
  },

  methods: {
    async fetchDashboardData() {
      this.loadingTasks = true
      try {
        const [stats, recentUploads, riskRows, trend] = await Promise.all([
          getDashboardStats(),
          getRecentUploads({ limit: 5 }),
          getRiskDistribution(),
          getTrendChartData(30)
        ])
        this.stats = { ...this.stats, ...stats }
        this.monthData = trend.labels || []
        this.processingSeries = [
          { name: '处理量', data: trend.reportCounts || [] },
          { name: '健康评分', data: trend.avgScores || [] }
        ]
        this.riskDistribution = (riskRows || []).map(item => ({
          name: item.label,
          value: item.count
        }))
        this.pendingTasks = (recentUploads || []).map(item => ({
          id: item.archiveId,
          enterpriseId: item.enterpriseId,
          taskNo: `REPORT-${String(item.archiveId).padStart(6, '0')}`,
          enterpriseName: item.enterpriseName,
          period: item.reportPeriod,
          taskType: this.getDataSourceLabel(item.dataSource),
          priority: ['DRAFT', 'PENDING_REVIEW'].includes(item.filingStatus) ? '高' : '低',
          createdAt: item.createdTime,
          status: this.getFilingStatusLabel(item.filingStatus),
          filingStatus: item.filingStatus
        }))
      } catch (error) {
        console.error('Failed to fetch dashboard data:', error)
        this.$message.error('工作台数据加载失败')
      } finally {
        this.loadingTasks = false
      }
    },

    getDataSourceLabel(source) {
      const labels = {
        REFERENCE_PDF: '原件入库',
        OCR_COMPLETED: 'OCR识别',
        MANUAL: '手工录入',
        EXCEL_IMPORT: 'Excel导入'
      }
      return labels[source] || '系统入库'
    },

    getFilingStatusLabel(status) {
      const labels = {
        DRAFT: '待处理',
        PENDING_REVIEW: '待复核',
        APPROVED: '已完成',
        REJECTED: '已退回'
      }
      return labels[status] || status || '未知'
    },

    getTaskTypeTag(type) {
      const tagMap = {
        'OCR识别': 'info',
        '人工复核': 'warning',
        '数据分析': 'success',
        '原件入库': 'success',
        '手工录入': 'warning',
        'Excel导入': 'info',
        '系统入库': 'info'
      }
      return tagMap[type] || 'default'
    },

    getStatusTag(status) {
      const tagMap = {
        '待处理': 'warning',
        '待复核': 'warning',
        '进行中': 'warning',
        '已完成': 'success',
        '已取消': 'default'
      }
      return tagMap[status] || 'default'
    },

    handleViewTask(task) {
      this.$router.push({
        path: `/enterprise/${task.enterpriseId}`,
        query: { reportId: task.id }
      })
    },

    handleProcessTask(task) {
      this.$router.push({
        path: `/enterprise/${task.enterpriseId}`,
        query: { reportId: task.id, action: 'approval' }
      })
    },

    handleMonthlyReports() {
      const latest = this.pendingTasks[0]
      const query = { source: 'monthly' }
      if (latest) {
        query.focusReportId = latest.id
        query.focusEnterpriseId = latest.enterpriseId
        query.focusPeriod = latest.period
      }
      this.$router.push({ path: '/enterprise', query })
    }
  }
}
</script>

<style scoped>
.dashboard-page {
  max-width: 1600px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #10212b;
  margin-bottom: 4px;
}

.page-subtitle {
  font-size: 13px;
  color: #6c7d89;
  margin: 0;
}

/* KPI卡片网格 */
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

@media screen and (max-width: 1440px) {
  .kpi-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media screen and (max-width: 1366px) {
  .kpi-grid {
    grid-template-columns: 1fr;
  }
}

/* 图表区域 */
.charts-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

@media screen and (max-width: 1366px) {
  .charts-grid {
    grid-template-columns: 1fr;
  }
}

/* 任务区域 */
.task-section {
  background-color: #ffffff;
  border-radius: 14px;
  padding: 24px;
  box-shadow: 0 5px 18px rgba(27, 61, 78, 0.06);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #10212b;
  margin: 0;
}
</style>
