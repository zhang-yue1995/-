<template>
  <div v-loading="loading" class="analysis-report-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2 class="page-title">财务分析报告</h2>
      <p class="page-subtitle">{{ reportTitle }}</p>
    </div>

    <!-- 企业与报表双级切换：支持在所有已归档企业、各期报表之间直接切换。 -->
    <div class="report-switcher">
      <div class="switcher-field switcher-enterprise">
        <span class="switcher-label">企业：</span>
        <el-select
          v-model="enterpriseId"
          filterable
          placeholder="请选择企业"
          @change="handleEnterpriseChange"
        >
          <el-option
            v-for="enterprise in enterprises"
            :key="enterprise.id"
            :label="enterprise.name"
            :value="enterprise.id"
          />
        </el-select>
      </div>
      <div class="switcher-field switcher-report">
        <span class="switcher-label">报表：</span>
        <el-select
          v-model="reportId"
          :disabled="!enterpriseId || !reports.length"
          placeholder="请选择报表期"
          @change="handleReportChange"
        >
          <el-option
            v-for="report in reports"
            :key="report.archiveId"
            :label="`${report.reportPeriod || '未知期间'} · REPORT-${String(report.archiveId).padStart(6, '0')}`"
            :value="report.archiveId"
          />
        </el-select>
      </div>
      <div class="switcher-summary">
        {{ selectedEnterpriseName }} · {{ detail.reportPeriod || '暂无报表' }}
      </div>
    </div>

    <!-- 顶部KPI卡片行 -->
    <div class="kpi-grid">
      <div class="kpi-card kpi-card--danger">
        <div class="kpi-card__header">
          <div class="kpi-card__icon" style="background-color: rgba(227, 93, 106, 0.1);">
            <i class="el-icon-data-analysis"></i>
          </div>
        </div>
        <div class="kpi-card__body">
          <div class="kpi-card__value">{{ health.totalScore }}<span class="unit">分</span></div>
          <div class="kpi-card__title">综合健康度</div>
          <div class="health-ring">
            <svg viewBox="0 0 120 120" width="80" height="80">
              <circle cx="60" cy="60" r="50" fill="none" stroke="#eef3f7" stroke-width="10"/>
              <circle cx="60" cy="60" r="50" fill="none" stroke="#e35d6a" stroke-width="10"
                      :stroke-dasharray="`${Number(health.totalScore || 0) * 3.14} 314`" transform="rotate(-90 60 60)"
                      stroke-linecap="round"/>
              <text x="60" y="65" text-anchor="middle" font-size="20" font-weight="bold" fill="#e35d6a">{{ health.totalScore }}</text>
            </svg>
          </div>
        </div>
      </div>

      <div class="kpi-card kpi-card--danger">
        <div class="kpi-card__header">
          <div class="kpi-card__icon" style="background-color: rgba(227, 93, 106, 0.1);">
            <i class="el-icon-coin"></i>
          </div>
          <div class="kpi-card__trend is-down">
            <i class="el-icon-bottom"></i>
            <span>低于警戒线1.0</span>
          </div>
        </div>
        <div class="kpi-card__body">
          <div class="kpi-card__value danger-value">{{ numberValue('currentRatio') }}<span class="unit">x</span></div>
          <div class="kpi-card__title">流动比率</div>
        </div>
      </div>

      <div class="kpi-card kpi-card--danger">
        <div class="kpi-card__header">
          <div class="kpi-card__icon" style="background-color: rgba(227, 93, 106, 0.1);">
            <i class="el-icon-pie-chart"></i>
          </div>
          <div class="kpi-card__trend is-up">
            <i class="el-icon-top"></i>
            <span>超过80%警戒线</span>
          </div>
        </div>
        <div class="kpi-card__body">
          <div class="kpi-card__value danger-value">{{ numberValue('debtToAssetRatio') }}<span class="unit">%</span></div>
          <div class="kpi-card__title">资产负债率</div>
        </div>
      </div>

      <div class="kpi-card kpi-card--danger">
        <div class="kpi-card__header">
          <div class="kpi-card__icon" style="background-color: rgba(227, 93, 106, 0.1);">
            <i class="el-icon-money"></i>
          </div>
          <div class="kpi-card__trend is-danger">
            <i class="el-icon-warning"></i>
            <span>负值，需关注</span>
          </div>
        </div>
        <div class="kpi-card__body">
          <div class="kpi-card__value danger-value">{{ numberValue('operatingCashToRevenue') }}<span class="unit">%</span></div>
          <div class="kpi-card__title">经营现金流/收入</div>
        </div>
      </div>
    </div>

    <!-- 中间2列区域 -->
    <div class="charts-grid">
      <!-- 左侧：五维能力雷达图 -->
      <div class="radar-section">
        <RadarChart
          title="五维能力雷达图"
          :indicators="radarIndicators"
          :seriesData="radarSeriesData"
          height="400px"
        />
      </div>

      <!-- 右侧：关键风险排序 -->
      <div class="risk-list-section">
        <div class="section-header">
          <h3 class="section-title">关键风险排序</h3>
        </div>
        <div class="risk-items">
          <div v-for="(risk, index) in riskList" :key="index" class="risk-item" :class="[`risk-item--${risk.level}`]">
            <div class="risk-content">
              <h4 class="risk-title">{{ risk.title }}</h4>
              <p class="risk-description">{{ risk.description }}</p>
            </div>
            <StatusTag
              :type="getRiskLevelType(risk.level)"
              :text="getRiskLevelText(risk.level)"
              size="small"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- AI分析摘要区域 -->
    <div class="analysis-summary">
      <div class="summary-header">
        <i class="el-icon-document-checked"></i>
        <h3>综合分析结论</h3>
      </div>
      <div class="summary-body">
        <div class="summary-section">
          <h4 class="summary-title">【综合评价】</h4>
          <p class="summary-text pre-line">{{ analysis.overallAssessment || analysis.executiveSummary }}</p>
        </div>

        <div class="summary-section">
          <h4 class="summary-title text-danger">【主要风险】</h4>
          <p class="summary-text pre-line">{{ analysis.riskAnalysis || '当前未识别到可量化风险。' }}</p>
        </div>

        <div class="summary-section">
          <h4 class="summary-title text-success">【积极因素】</h4>
          <p class="summary-text pre-line">{{ analysis.positiveFactors || '暂无。' }}</p>
        </div>

        <div class="summary-section">
          <h4 class="summary-title text-info">【改善建议】</h4>
          <p class="summary-text pre-line">{{ analysis.improvementSuggestions || '暂无。' }}</p>
        </div>
      </div>

      <!-- 免责声明 -->
      <div class="disclaimer">
        本报告基于企业提供的财务数据自动生成，仅供业务分析参考，不构成审计、授信审批或投资建议。
      </div>

      <!-- 导出按钮组 -->
      <div class="export-actions">
        <el-button plain round icon="el-icon-document" @click="handleExportPdf">导出PDF</el-button>
      </div>
    </div>
  </div>
</template>

<script>
import { getAnalysisReport, exportReport } from '@/api/analysis'
import { getReportIndicators, getHealthScore } from '@/api/indicator'
import { getEnterpriseList, getEnterpriseReports } from '@/api/enterprise'
import { getReportDetail } from '@/api/report'
import RadarChart from '@/components/charts/RadarChart.vue'
import StatusTag from '@/components/StatusTag.vue'

export default {
  name: 'AnalysisReport',

  components: {
    RadarChart,
    StatusTag
  },

  data() {
    return {
      loading: false,
      enterpriseId: null,
      enterprises: [],
      reports: [],
      reportId: null,
      analysis: {},
      detail: {},
      indicators: {},
      health: {
        totalScore: 0,
        solvencyScore: 0,
        profitabilityScore: 0,
        operationScore: 0,
        cashFlowScore: 0,
        growthScore: 0
      },
      // 雷达图指标
      radarIndicators: [
        { name: '偿债能力', max: 100 },
        { name: '盈利能力', max: 100 },
        { name: '运营效率', max: 100 },
        { name: '现金流质量', max: 100 },
        { name: '成长性', max: 100 }
      ],

      // 雷达图数据
      radarSeriesData: [
        {
          name: '当前评分',
          value: [0, 0, 0, 0, 0]
        }
      ],

      riskList: []
    }
  },

  computed: {
    selectedEnterpriseName() {
      const enterprise = this.enterprises.find(item => Number(item.id) === Number(this.enterpriseId))
      return enterprise ? enterprise.name : (this.analysis.enterpriseName || '请选择企业')
    },

    reportTitle() {
      return this.analysis.reportTitle ||
        `${this.analysis.enterpriseName || '企业'} - ${this.detail.reportPeriod || '暂无报表'}`
    }
  },

  async created() {
    await this.initializeSwitcher()
  },

  methods: {
    async initializeSwitcher() {
      this.loading = true
      try {
        const page = await getEnterpriseList({ pageNum: 1, pageSize: 500, activeReportsOnly: true })
        this.enterprises = page.list || []

        const requestedReportId = this.$route.query.reportId
          ? Number(this.$route.query.reportId)
          : null
        const requestedEnterpriseId = this.$route.query.enterpriseId
          ? Number(this.$route.query.enterpriseId)
          : null

        if (requestedReportId) {
          const detail = await getReportDetail(requestedReportId)
          this.enterpriseId = Number(detail.enterpriseId)
          await this.loadReports(this.enterpriseId, requestedReportId)
        } else {
          this.enterpriseId = requestedEnterpriseId || (this.enterprises[0] && this.enterprises[0].id)
          await this.loadReports(this.enterpriseId)
        }

        await this.fetchAnalysisReport()
      } catch (error) {
        console.error('Failed to initialize analysis switcher:', error)
        this.$message.error('企业及报表列表加载失败')
      } finally {
        this.loading = false
      }
    },

    async loadReports(enterpriseId, preferredReportId) {
      if (!enterpriseId) {
        this.reports = []
        this.reportId = null
        return
      }
      const page = await getEnterpriseReports(enterpriseId, {
        pageNum: 1,
        pageSize: 500,
        sortBy: 'reportDate',
        sortOrder: 'desc'
      })
      this.reports = page.list || []
      const preferred = this.reports.find(item => Number(item.archiveId) === Number(preferredReportId))
      this.reportId = preferred
        ? preferred.archiveId
        : (this.reports[0] && this.reports[0].archiveId)
    },

    async handleEnterpriseChange(enterpriseId) {
      this.loading = true
      try {
        await this.loadReports(enterpriseId)
        await this.fetchAnalysisReport()
      } catch (error) {
        console.error('Failed to switch enterprise:', error)
        this.$message.error('切换企业失败')
      } finally {
        this.loading = false
      }
    },

    async handleReportChange(reportId) {
      this.reportId = reportId
      await this.fetchAnalysisReport()
    },

    async fetchAnalysisReport() {
      if (!this.reportId) {
        this.analysis = {}
        this.detail = {}
        this.indicators = {}
        this.riskList = []
        return
      }
      this.loading = true
      try {
        const [analysis, indicators, health, detail] = await Promise.all([
          getAnalysisReport(this.reportId),
          getReportIndicators(this.reportId),
          getHealthScore(this.reportId),
          getReportDetail(this.reportId)
        ])
        this.analysis = analysis || {}
        this.indicators = indicators || {}
        this.health = { ...this.health, ...(health || {}) }
        this.detail = detail || {}
        this.enterpriseId = Number(this.detail.enterpriseId || this.enterpriseId)
        this.syncRoute()
        this.radarSeriesData = [{
          name: '当前评分',
          value: [
            Number(this.health.solvencyScore || 0),
            Number(this.health.profitabilityScore || 0),
            Number(this.health.operationScore || 0),
            Number(this.health.cashFlowScore || 0),
            Number(this.health.growthScore || 0)
          ]
        }]
        this.riskList = this.buildRiskList()
      } catch (error) {
        console.error('Failed to fetch analysis report:', error)
        this.$message.error('获取分析报告失败')
      } finally {
        this.loading = false
      }
    },

    syncRoute() {
      const reportId = String(this.reportId || '')
      const enterpriseId = String(this.enterpriseId || '')
      if (String(this.$route.query.reportId || '') === reportId &&
          String(this.$route.query.enterpriseId || '') === enterpriseId) return
      this.$router.replace({
        path: '/analysis',
        query: { reportId, enterpriseId }
      }).catch(() => {})
    },

    numberValue(code) {
      const value = this.indicators[code]
      const multiplier = code === 'operatingCashToRevenue' ? 100 : 1
      return value === null || value === undefined
        ? '—'
        : (Number(value) * multiplier).toFixed(2)
    },

    buildRiskList() {
      const rows = []
      const debt = Number(this.indicators.debtToAssetRatio)
      const current = Number(this.indicators.currentRatio)
      const cash = Number(this.indicators.operatingCashToRevenue)
      const margin = Number(this.indicators.netProfitMargin)
      if (Number.isFinite(debt) && debt >= 80) {
        rows.push({
          title: '资产负债率过高',
          description: `当前 ${debt.toFixed(2)}%，超过 80% 风险阈值`,
          level: 'critical'
        })
      }
      if (Number.isFinite(current) && current < 1.5) {
        rows.push({
          title: '流动比率偏低',
          description: `当前 ${current.toFixed(2)} 倍，低于 1.5 警戒线`,
          level: 'high'
        })
      }
      if (Number.isFinite(cash) && cash < 0) {
        rows.push({
          title: '经营现金流为负',
          description: `经营现金流/营业收入为 ${(cash * 100).toFixed(2)}%`,
          level: 'high'
        })
      }
      if (Number.isFinite(margin) && margin < 3) {
        rows.push({
          title: '盈利能力薄弱',
          description: `销售净利率为 ${margin.toFixed(2)}%，低于 3% 关注阈值`,
          level: 'medium'
        })
      }
      return rows
    },

    handlePrint() {
      window.print()
    },

    async handleExportPdf() {
      if (!this.reportId) return
      try {
        const response = await exportReport(this.reportId, 'pdf')
        const url = URL.createObjectURL(new Blob([response.data], { type: 'application/pdf' }))
        const link = document.createElement('a')
        link.href = url
        const safe = value => String(value || '').replace(/[\\/:*?"<>|]/g, '_')
        link.download = `${safe(this.selectedEnterpriseName)}_${safe(this.detail.reportPeriod)}_报表分析.pdf`
        link.click()
        URL.revokeObjectURL(url)
        this.$message.success('PDF智能分析报告已生成')
      } catch (error) {
        this.$message.error((error && error.message) || '报告导出失败')
      }
    },

    getRiskLevelType(level) {
      const map = {
        critical: 'danger',
        high: 'danger',
        medium: 'warning',
        low: 'success'
      }
      return map[level] || 'default'
    },

    getRiskLevelText(level) {
      const map = {
        critical: '严重',
        high: '高',
        medium: '中',
        low: '低'
      }
      return map[level] || '未知'
    }
  }
}
</script>

<style scoped>
.analysis-report-page {
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

.report-switcher {
  display: flex;
  align-items: center;
  gap: 24px;
  margin-bottom: 24px;
  padding: 20px 24px;
  background: #fff;
  border: 1px solid #e1e9ed;
  border-radius: 14px;
  box-shadow: 0 5px 18px rgba(27, 61, 78, 0.05);
}

.switcher-field {
  display: flex;
  align-items: center;
  gap: 10px;
}

.switcher-enterprise .el-select { width: 320px; }
.switcher-report .el-select { width: 280px; }

.switcher-label {
  color: #526b78;
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
}

.switcher-summary {
  min-width: 0;
  margin-left: auto;
  overflow: hidden;
  color: #718691;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media screen and (max-width: 1200px) {
  .report-switcher {
    flex-wrap: wrap;
  }

  .switcher-summary {
    width: 100%;
    margin-left: 0;
  }
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

.kpi-card {
  background-color: #ffffff;
  border-radius: 14px;
  padding: 24px;
  box-shadow: 0 5px 18px rgba(27, 61, 78, 0.06);
  transition: all 0.3s ease;
}

.kpi-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 25px rgba(27, 61, 78, 0.12);
}

.kpi-card--danger::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, #e35d6a, #f07a85);
  border-radius: 14px 14px 0 0;
}

.kpi-card {
  position: relative;
}

.kpi-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.kpi-card__icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  color: #e35d6a;
}

.kpi-card__trend {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 999px;
}

.kpi-card__trend.is-up,
.kpi-card__trend.is-down,
.kpi-card__trend.is-danger {
  color: #e35d6a;
  background-color: rgba(227, 93, 106, 0.08);
}

.kpi-card__body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.kpi-card__value {
  font-size: 28px;
  font-weight: 700;
  color: #10212b;
  line-height: 1.2;
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.danger-value {
  color: #e35d6a;
}

.unit {
  font-size: 14px;
  font-weight: 500;
  color: #6c7d89;
}

.kpi-card__title {
  font-size: 13px;
  color: #6c7d89;
  font-weight: 500;
}

.health-ring {
  margin-top: 8px;
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

.radar-section,
.risk-list-section {
  background-color: #ffffff;
  border-radius: 14px;
  box-shadow: 0 5px 18px rgba(27, 61, 78, 0.06);
}

.risk-list-section {
  padding: 24px;
}

.section-header {
  margin-bottom: 20px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #10212b;
  margin: 0;
}

.risk-items {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.risk-item {
  padding: 16px 18px;
  border-left: 4px solid #e35d6a;
  border-radius: 0 10px 10px 0;
  background-color: #fafafa;
  transition: all 0.3s;
}

.risk-item:hover {
  background-color: #fef5f5;
  box-shadow: 0 2px 8px rgba(227, 93, 106, 0.08);
}

.risk-item--critical {
  border-left-color: #e35d6a;
}

.risk-item--high {
  border-left-color: #ff6b6b;
}

.risk-item--medium {
  border-left-color: #f3a83b;
}

.risk-item--low {
  border-left-color: #9dd99e;
}

.risk-content {
  flex: 1;
}

.risk-title {
  font-size: 14px;
  font-weight: 600;
  color: #10212b;
  margin: 0 0 8px 0;
}

.risk-description {
  font-size: 13px;
  color: #6c7d89;
  line-height: 1.6;
  margin: 0;
}

/* AI分析摘要区域 */
.analysis-summary {
  background-color: #ffffff;
  border-radius: 14px;
  padding: 28px;
  box-shadow: 0 5px 18px rgba(27, 61, 78, 0.06);
  margin-bottom: 24px;
}

.summary-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 2px solid #eef3f7;
}

.summary-header i {
  font-size: 24px;
  color: #0e8f78;
}

.summary-header h3 {
  font-size: 18px;
  font-weight: 600;
  color: #10212b;
  margin: 0;
}

.summary-body {
  line-height: 1.8;
}

.summary-section {
  margin-bottom: 24px;
}

.summary-section:last-of-type {
  margin-bottom: 0;
}

.summary-title {
  font-size: 15px;
  font-weight: 600;
  color: #10212b;
  margin: 0 0 12px 0;
}

.summary-text {
  font-size: 14px;
  color: #3d5563;
  margin: 0;
  line-height: 1.8;
}

.pre-line {
  white-space: pre-line;
}

@media print {
  .export-actions {
    display: none;
  }
}

.summary-list {
  margin: 0;
  padding-left: 20px;
  font-size: 14px;
  color: #3d5563;
  line-height: 1.9;
}

.summary-list li {
  margin-bottom: 8px;
}

.text-success {
  color: #20a96b !important;
}

.text-warning {
  color: #f3a83b !important;
}

.text-danger {
  color: #e35d6a !important;
}

.text-info {
  color: #3d7cf0 !important;
}

/* 免责声明 */
.disclaimer {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #eef3f7;
  font-size: 12px;
  color: #999;
  line-height: 1.6;
  text-align: center;
}

/* 导出按钮组 */
.export-actions {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
