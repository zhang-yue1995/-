<template>
  <div v-loading="loading" class="report-detail-page">
    <div class="detail-header">
      <div>
        <h2>{{ reportInfo.enterpriseName || '企业财务报表' }}</h2>
        <p>
          {{ reportInfo.reportPeriod || '—' }} · 单位：元 ·
          归档编号 REPORT-{{ String(archiveId || '').padStart(6, '0') }}
        </p>
      </div>
      <div class="header-actions">
        <el-tag :type="statusType" effect="plain">{{ statusLabel }}</el-tag>
        <el-tag :type="validationPassed ? 'success' : 'warning'" effect="plain">
          {{ validationPassed ? '勾稽校验通过' : '待校验' }}
        </el-tag>
      </div>
    </div>

    <div v-if="!archiveId && !loading" class="empty-card">
      <i class="el-icon-document"></i>
      <h3>暂无财务报表</h3>
      <p>该企业尚未归档可展示的报表。</p>
    </div>

    <template v-else>
      <div v-if="isReviewMode" class="review-task-banner">
        <div>
          <strong><i class="el-icon-warning-outline"></i> 待复核任务</strong>
          <p>请核对当前报表期的三大报表数据、异常项目及勾稽校验结果。</p>
        </div>
        <el-tag v-if="reportInfo.filingStatus === 'PENDING_REVIEW'" type="warning" effect="dark">等待人工复核</el-tag>
        <el-tag v-else type="success" effect="plain">该报表已完成复核</el-tag>
      </div>

      <div v-if="reportInfo.reviewComment && reportInfo.filingStatus === 'REJECTED'" class="rejection-banner">
        <strong><i class="el-icon-circle-close" /> 管理员已驳回</strong>
        <span>{{ reportInfo.reviewComment }}</span>
      </div>

      <el-tabs v-model="activeTab" type="card" class="legacy-tabs">
        <el-tab-pane label="资产负债表" name="balance" />
        <el-tab-pane label="利润表" name="income" />
        <el-tab-pane label="现金流量表(人工录入)" name="cashManual" />
        <el-tab-pane label="现金流量表(自动生成)" name="cashAuto" />
        <el-tab-pane label="主要财务指标" name="indicators" />
        <el-tab-pane label="补充财务数据表" name="supplement" />
      </el-tabs>

      <div class="report-toolbar">
        <div class="breadcrumb">
          {{ reportInfo.enterpriseName }} &gt;&gt; {{ reportInfo.reportPeriod }}
          &gt;&gt; 本部 &gt;&gt; {{ activeTabLabel }} 单位:元
        </div>
        <div>
          <el-button size="mini" icon="el-icon-refresh" @click="loadReport">刷新</el-button>
          <el-button size="mini" icon="el-icon-data-analysis" @click="handleCalculate">测算</el-button>
          <el-button size="mini" type="primary" icon="el-icon-download" @click="handleExport">
            导出
          </el-button>
        </div>
      </div>

      <div v-if="activeTab === 'balance'" class="ledger-card">
        <table class="ledger balance-ledger">
          <thead>
            <tr>
              <th>资产</th>
              <th class="row-no">行次</th>
              <th class="money">期末余额</th>
              <th class="money">年初余额</th>
              <th>负债和所有者权益</th>
              <th class="row-no">行次</th>
              <th class="money">期末余额</th>
              <th class="money">年初余额</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, index) in balanceRows" :key="index">
              <td :class="{ total: isTotal(row.asset) }">{{ row.asset ? row.asset.itemName : '' }}</td>
              <td class="row-no">{{ row.asset ? row.asset.sortOrder : '' }}</td>
              <td class="money" :class="{ negative: isNegative(row.asset && row.asset.endingBalance) }">
                {{ row.asset ? ledgerMoney(row.asset.endingBalance) : '' }}
              </td>
              <td class="money" :class="{ negative: isNegative(row.asset && row.asset.beginningBalance) }">
                {{ row.asset ? ledgerMoney(row.asset.beginningBalance) : '' }}
              </td>
              <td :class="{ total: isTotal(row.liability) }">
                {{ row.liability ? row.liability.itemName : '' }}
              </td>
              <td class="row-no">{{ row.liability ? row.liability.sortOrder : '' }}</td>
              <td class="money" :class="{ negative: isNegative(row.liability && row.liability.endingBalance) }">
                {{ row.liability ? ledgerMoney(row.liability.endingBalance) : '' }}
              </td>
              <td class="money" :class="{ negative: isNegative(row.liability && row.liability.beginningBalance) }">
                {{ row.liability ? ledgerMoney(row.liability.beginningBalance) : '' }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-else-if="activeTab === 'income'" class="ledger-card">
        <table class="ledger">
          <thead>
            <tr>
              <th>项目</th>
              <th class="row-no">行次</th>
              <th class="money">本期金额</th>
              <th class="money">上期金额</th>
              <th class="money">本月金额</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in incomeItems" :key="row.id">
              <td :class="{ total: isTotal(row) }">{{ row.itemName }}</td>
              <td class="row-no">{{ row.sortOrder }}</td>
              <td class="money" :class="{ negative: isNegative(row.currentPeriodAmount) }">
                {{ ledgerMoney(row.currentPeriodAmount) }}
              </td>
              <td class="money" :class="{ negative: isNegative(row.previousPeriodAmount) }">
                {{ ledgerMoney(row.previousPeriodAmount) }}
              </td>
              <td class="money" :class="{ negative: isNegative(row.monthlyAmount) }">
                {{ ledgerMoney(row.monthlyAmount) }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-else-if="activeTab === 'cashManual' || activeTab === 'cashAuto'" class="ledger-card">
        <div v-if="activeTab === 'cashAuto'" class="source-notice">
          自动生成页与已归档现金流量表逐项核对；当前数据来源：{{ dataSourceLabel }}。
        </div>
        <table class="ledger">
          <thead>
            <tr>
              <th>项目</th>
              <th class="row-no">行次</th>
              <th class="money">本期金额</th>
              <th class="money">上期金额</th>
              <th class="money">本月金额</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in cashItems" :key="row.id">
              <td :class="{ total: isTotal(row) }">{{ row.itemName }}</td>
              <td class="row-no">{{ row.rowNumber }}</td>
              <td class="money" :class="{ negative: isNegative(cashCurrentAmount(row)) }">
                {{ ledgerMoney(cashCurrentAmount(row)) }}
              </td>
              <td class="money" :class="{ negative: isNegative(row.previousPeriodAmount) }">
                {{ ledgerMoney(row.previousPeriodAmount) }}
              </td>
              <td class="money" :class="{ negative: isNegative(row.monthlyAmount) }">
                {{ ledgerMoney(row.monthlyAmount) }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-else-if="activeTab === 'indicators'" class="indicator-card">
        <div class="health-summary">
          <div class="score">{{ health.totalScore === null ? '—' : health.totalScore }}</div>
          <div>
            <h3>财务健康度</h3>
            <p>{{ riskLabel(health.riskLevel) }} · 按字段映射与财务指标模型计算</p>
          </div>
        </div>
        <el-table :data="indicatorRows" border stripe>
          <el-table-column prop="name" label="指标名称" min-width="180" />
          <el-table-column prop="formula" label="口径" min-width="260" />
          <el-table-column prop="displayValue" label="本期值" width="160" align="right" />
        </el-table>
      </div>

      <div v-else class="empty-card">
        <i class="el-icon-document-remove"></i>
        <h3>原件未提供补充财务数据</h3>
        <p>系统不会为缺失字段生成或猜测数值。</p>
      </div>

      <div v-if="isApprovalMode" class="approval-bar">
        <div>
          <strong>整份报表终审</strong>
          <p>请确认三张报表、勾稽校验、健康评分和关键风险均已核对。审批通过后正式入库；驳回后提交人可在小程序修订并重新提交。</p>
        </div>
        <div class="approval-actions">
          <el-button type="danger" plain :loading="approvalBusy" @click="handleRejectApproval">拒绝审批</el-button>
          <el-button type="success" :loading="approvalBusy" @click="handleApproveApproval">审批完成</el-button>
        </div>
      </div>
    </template>
  </div>
</template>

<script>
import { getEnterpriseReports } from '@/api/enterprise'
import { getReportDetail, getReportStatements, approveReport, rejectReport } from '@/api/report'
import { getReportIndicators, getHealthScore } from '@/api/indicator'
import { formatMoney } from '@/utils/format'

const INDICATOR_META = {
  currentRatio: ['流动比率', '流动资产 / 流动负债', '倍'],
  quickRatio: ['速动比率', '(流动资产 - 存货) / 流动负债', '倍'],
  cashRatio: ['现金比率', '货币资金 / 流动负债', '倍'],
  debtToAssetRatio: ['资产负债率', '总负债 / 总资产 × 100%', '%'],
  grossProfitMargin: ['销售毛利率', '(营业收入 - 营业成本) / 营业收入 × 100%', '%'],
  operatingProfitMargin: ['营业利润率', '营业利润 / 营业收入 × 100%', '%'],
  netProfitMargin: ['销售净利率', '净利润 / 营业收入 × 100%', '%'],
  roa: ['总资产收益率 ROA', '净利润 / 平均总资产 × 100%', '%'],
  roe: ['净资产收益率 ROE', '净利润 / 平均所有者权益 × 100%', '%'],
  accountsReceivableTurnover: ['应收账款周转率', '营业收入 / 平均应收账款', '次'],
  currentAssetTurnover: ['流动资产周转率', '营业收入 / 平均流动资产', '次'],
  totalAssetTurnover: ['总资产周转率', '营业收入 / 平均总资产', '次'],
  operatingCashToRevenue: ['经营现金净流量与营业收入比率', '经营现金流净额 / 营业收入', '%', 100],
  cashFlowRatio: ['经营现金流与流动负债比率', '经营现金流净额 / 流动负债', '倍'],
  revenueGrowthRate: ['营业收入增长率', '(本期营收 - 上期营收) / 上期营收 × 100%', '%'],
  prepaymentToCurrentAssets: ['预付款项占流动资产比率', '预付款项 / 流动资产 × 100%', '%', 100],
  otherReceivableToCurrentAssets: ['其他应收款占流动资产比率', '其他应收款 / 流动资产 × 100%', '%', 100]
}

export default {
  name: 'ReportDetail',

  props: {
    id: {
      type: [String, Number],
      required: true
    }
  },

  data() {
    return {
      loading: false,
      approvalBusy: false,
      activeTab: 'balance',
      archiveId: null,
      reportInfo: {},
      statements: {
        balanceSheet: { items: [] },
        incomeStatement: { items: [] },
        cashFlowStatement: { items: [] }
      },
      indicators: {},
      health: {
        totalScore: null,
        riskLevel: null
      }
    }
  },

  computed: {
    balanceItems() {
      return (this.statements.balanceSheet && this.statements.balanceSheet.items) || []
    },
    incomeItems() {
      return (this.statements.incomeStatement && this.statements.incomeStatement.items) || []
    },
    cashItems() {
      return (this.statements.cashFlowStatement && this.statements.cashFlowStatement.items) || []
    },
    balanceRows() {
      const assets = this.balanceItems.filter(row => Number(row.sortOrder) <= 29)
      const liabilities = this.balanceItems.filter(row => Number(row.sortOrder) > 29)
      const length = Math.max(assets.length, liabilities.length)
      return Array.from({ length }, (_, index) => ({
        asset: assets[index] || null,
        liability: liabilities[index] || null
      }))
    },
    indicatorRows() {
      return Object.keys(INDICATOR_META).map(code => {
        const meta = INDICATOR_META[code]
        const value = this.indicators[code]
        return {
          code,
          name: meta[0],
          formula: meta[1],
          displayValue: value === null || value === undefined
            ? '—'
            : `${(Number(value) * (meta[3] || 1)).toFixed(2)} ${meta[2]}`
        }
      })
    },
    statusLabel() {
      const labels = {
        DRAFT: '草稿',
        REVIEWED: '已复核待提交',
        PENDING_REVIEW: '待复核',
        APPROVED: '已入库',
        REJECTED: '已退回'
      }
      return labels[this.reportInfo.filingStatus] || this.reportInfo.filingStatus || '未知'
    },
    statusType() {
      return this.reportInfo.filingStatus === 'APPROVED' ? 'success' : 'warning'
    },
    validationPassed() {
      if (this.reportInfo.validationStatus) {
        return ['PASSED', 'VALID'].includes(this.reportInfo.validationStatus)
      }
      return this.reportInfo.balanceDifference !== null &&
        this.reportInfo.balanceDifference !== undefined &&
        Number(this.reportInfo.balanceDifference) === 0
    },
    dataSourceLabel() {
      const labels = {
        REFERENCE_PDF: '随附报表原件',
        OCR_COMPLETED: 'OCR识别',
        MANUAL: '人工录入',
        EXCEL_IMPORT: 'Excel导入'
      }
      return labels[this.reportInfo.dataSource] || '归档数据'
    },
    activeTabLabel() {
      const labels = {
        balance: '资产负债表',
        income: '利润表',
        cashManual: '现金流量表(人工录入)',
        cashAuto: '现金流量表(自动生成)',
        indicators: '主要财务指标',
        supplement: '补充财务数据表'
      }
      return labels[this.activeTab]
    },
    isReviewMode() {
      return ['review', 'approval', 'process'].includes(this.$route.query.action)
    },
    isApprovalMode() {
      return ['approval', 'process'].includes(this.$route.query.action) &&
        this.reportInfo.approvalStatus === 'pending_approval'
    }
  },

  watch: {
    id() {
      this.loadReport()
    },
    '$route.query.reportId'() {
      this.loadReport()
    }
  },

  created() {
    this.loadReport()
  },

  methods: {
    async loadReport() {
      this.loading = true
      try {
        const page = await getEnterpriseReports(this.id, {
          pageNum: 1,
          pageSize: 20,
          sortBy: 'reportDate',
          sortOrder: 'desc'
        })
        const reports = page.list || []
        const requestedId = Number(this.$route.query.reportId || 0)
        const latest = requestedId
          ? reports.find(item => Number(item.archiveId) === requestedId)
          : reports[0]
        if (!latest) {
          if (requestedId) {
            const requestedDetail = await getReportDetail(requestedId)
            if (Number(requestedDetail.enterpriseId) === Number(this.id)) {
              this.archiveId = requestedId
              await this.loadArchiveData(requestedDetail)
              return
            }
          }
          this.archiveId = null
          return
        }
        this.archiveId = latest.archiveId
        await this.loadArchiveData()
      } catch (error) {
        console.error('Failed to load report:', error)
        this.$message.error('报表数据加载失败')
      } finally {
        this.loading = false
      }
    },

    async loadArchiveData(prefetchedDetail) {
      const [detail, statements, indicators, health] = await Promise.all([
          prefetchedDetail || getReportDetail(this.archiveId),
          getReportStatements(this.archiveId),
          getReportIndicators(this.archiveId),
          getHealthScore(this.archiveId)
        ])
      this.reportInfo = detail || {}
      this.statements = statements || this.statements
      this.indicators = indicators || {}
      this.health = health || this.health
    },

    ledgerMoney(value) {
      return value === null || value === undefined ? '0.00' : formatMoney(value)
    },
    isNegative(value) {
      return Number(value) < 0
    },
    cashCurrentAmount(row) {
      return row.currentPeriodAmount === null || row.currentPeriodAmount === undefined
        ? row.amount
        : row.currentPeriodAmount
    },
    isTotal(row) {
      return Boolean(row && (row.isTotalRow === 1 || row.isTotalRow === true))
    },
    riskLabel(level) {
      const labels = {
        HEALTHY: '健康',
        NORMAL: '正常',
        ATTENTION: '关注',
        DANGEROUS: '高风险',
        CRITICAL: '严重风险'
      }
      return labels[level] || '待计算'
    },
    handleCalculate() {
      this.$router.push({
        path: '/analysis',
        query: { enterpriseId: this.id, reportId: this.archiveId }
      })
    },
    handleExport() {
      let headers = []
      let rows = []
      if (this.activeTab === 'balance') {
        headers = ['资产', '行次', '期末余额', '年初余额', '负债和所有者权益', '行次', '期末余额', '年初余额']
        rows = this.balanceRows.map(row => [
          row.asset && row.asset.itemName,
          row.asset && row.asset.sortOrder,
          row.asset && row.asset.endingBalance,
          row.asset && row.asset.beginningBalance,
          row.liability && row.liability.itemName,
          row.liability && row.liability.sortOrder,
          row.liability && row.liability.endingBalance,
          row.liability && row.liability.beginningBalance
        ])
      } else if (this.activeTab === 'income') {
        headers = ['项目', '行次', '本期金额', '上期金额', '本月金额']
        rows = this.incomeItems.map(row => [
          row.itemName, row.sortOrder, row.currentPeriodAmount, row.previousPeriodAmount, row.monthlyAmount
        ])
      } else if (['cashManual', 'cashAuto'].includes(this.activeTab)) {
        headers = ['项目', '行次', '本期金额', '上期金额', '本月金额']
        rows = this.cashItems.map(row => [
          row.itemName, row.rowNumber, this.cashCurrentAmount(row), row.previousPeriodAmount, row.monthlyAmount
        ])
      } else if (this.activeTab === 'indicators') {
        headers = ['指标名称', '计算口径', '本期值']
        rows = this.indicatorRows.map(row => [row.name, row.formula, row.displayValue])
      } else {
        this.$message.info('当前页没有可导出的数据')
        return
      }
      const escape = value => `"${String(value === null || value === undefined ? '' : value).replace(/"/g, '""')}"`
      const csv = [headers, ...rows].map(row => row.map(escape).join(',')).join('\r\n')
      const url = URL.createObjectURL(new Blob(['\ufeff', csv], { type: 'text/csv;charset=utf-8' }))
      const link = document.createElement('a')
      link.href = url
      link.download = `${this.reportInfo.enterpriseName}-${this.reportInfo.reportPeriod}-${this.activeTabLabel}.csv`
      link.click()
      URL.revokeObjectURL(url)
    },
    async handleApproveApproval() {
      try {
        await this.$confirm('确认整份报表数据、勾稽校验与分析结论均无误，并完成最终审批？', '报表终审', {
          confirmButtonText: '确认审批完成', cancelButtonText: '取消', type: 'success'
        })
        this.approvalBusy = true
        await approveReport(this.archiveId)
        await this.loadArchiveData()
        this.$message.success('整份报表已终审通过并正式入库')
      } catch (error) {
        if (error !== 'cancel' && error !== 'close') this.$message.error(error.message || '审批失败')
      } finally {
        this.approvalBusy = false
      }
    },
    async handleRejectApproval() {
      try {
        const result = await this.$prompt('请填写需要提交人修订的具体问题', '拒绝审批', {
          confirmButtonText: '确认驳回', cancelButtonText: '取消', inputType: 'textarea',
          inputValidator: value => (value && value.trim() ? true : '驳回原因不能为空')
        })
        this.approvalBusy = true
        await rejectReport(this.archiveId, result.value.trim())
        await this.loadArchiveData()
        this.$message.success('已驳回，提交人可在小程序修改后重新提交')
      } catch (error) {
        if (error !== 'cancel' && error !== 'close') this.$message.error(error.message || '驳回失败')
      } finally {
        this.approvalBusy = false
      }
    }
  }
}
</script>

<style scoped>
.report-detail-page {
  max-width: 1680px;
  min-height: 480px;
  margin: 0 auto;
}

.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  margin-bottom: 16px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(28, 52, 64, 0.06);
}

.review-task-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 14px 18px;
  margin-bottom: 16px;
  background: #fff8e8;
  border: 1px solid #f5d58b;
  border-radius: 10px;
}

.review-task-banner strong {
  color: #8c5a12;
  font-size: 15px;
}

.review-task-banner p {
  margin: 5px 0 0;
  color: #816d4d;
  font-size: 13px;
}

.rejection-banner { display:flex; gap:14px; align-items:center; padding:12px 16px; margin-bottom:16px; color:#a93743; background:#fff2f3; border:1px solid #f1b6bc; border-radius:10px; }
.approval-bar { position:sticky; z-index:8; bottom:14px; display:flex; justify-content:space-between; align-items:center; gap:24px; padding:16px 20px; margin-top:18px; background:rgba(255,255,255,.97); border:1px solid #c9d9d5; border-left:5px solid #0e8f78; border-radius:12px; box-shadow:0 10px 30px rgba(24,59,72,.18); }
.approval-bar strong { color:#14313d; font-size:16px; }
.approval-bar p { margin:5px 0 0; color:#687b85; font-size:12px; }
.approval-actions { display:flex; flex:none; gap:10px; }

.detail-header h2 {
  margin: 0 0 8px;
  color: #10212b;
  font-size: 21px;
}

.detail-header p {
  margin: 0;
  color: #647681;
  font-size: 13px;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.legacy-tabs {
  padding: 14px 18px 0;
  background: #edf1f4;
  border: 1px solid #c9d2d8;
  border-radius: 8px 8px 0 0;
}

.legacy-tabs >>> .el-tabs__header {
  margin: 0;
}

.legacy-tabs >>> .el-tabs__item {
  color: #223946;
  font-weight: 600;
  background: linear-gradient(#fff, #dce4e8);
  border-color: #aebbc3 !important;
}

.legacy-tabs >>> .el-tabs__item.is-active {
  color: #0b6cae;
  background: #fff;
}

.report-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 42px;
  padding: 8px 14px;
  background: #fff;
  border: 1px solid #c5cfd6;
  border-top: 0;
}

.breadcrumb {
  color: #253b47;
  font-size: 13px;
  font-weight: 600;
}

.ledger-card,
.indicator-card {
  overflow: auto;
  padding: 14px;
  background: #fff;
  border: 1px solid #c5cfd6;
  border-top: 0;
}

.ledger {
  width: 100%;
  min-width: 880px;
  table-layout: fixed;
  border-collapse: collapse;
  color: #172b35;
  font-size: 13px;
}

.ledger th,
.ledger td {
  height: 30px;
  padding: 3px 8px;
  border: 1px solid #6e818d;
}

.ledger th {
  text-align: center;
  background: #e7ecef;
  font-weight: 700;
}

.ledger tr:nth-child(even) td {
  background: #fafcfd;
}

.ledger .row-no {
  width: 56px;
  text-align: center;
}

.ledger .money {
  width: 138px;
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.ledger .total {
  font-weight: 700;
  background: #eef4f7;
}

.ledger .negative {
  color: #c53c47;
}

.balance-ledger th:first-child,
.balance-ledger th:nth-child(5) {
  width: 25%;
}

.source-notice {
  padding: 9px 12px;
  margin-bottom: 10px;
  color: #51636e;
  font-size: 12px;
  background: #f3f7f9;
  border-left: 3px solid #3c8dbc;
}

.health-summary {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 18px;
  margin-bottom: 16px;
  background: #f5f9fb;
  border: 1px solid #d9e3e8;
}

.health-summary .score {
  width: 62px;
  color: #d94f5c;
  font-size: 36px;
  font-weight: 700;
  text-align: center;
}

.health-summary h3 {
  margin: 0 0 5px;
  font-size: 16px;
}

.health-summary p {
  margin: 0;
  color: #60737e;
  font-size: 12px;
}

.empty-card {
  padding: 80px 20px;
  color: #72838d;
  text-align: center;
  background: #fff;
  border: 1px solid #d9e2e7;
}

.empty-card i {
  display: block;
  margin-bottom: 12px;
  color: #b6c4cc;
  font-size: 48px;
}

.empty-card h3 {
  margin: 0 0 8px;
  color: #354b57;
}

.empty-card p {
  margin: 0;
}

@media (max-width: 900px) {
  .detail-header,
  .report-toolbar {
    align-items: flex-start;
    flex-direction: column;
    gap: 12px;
  }
}
</style>
